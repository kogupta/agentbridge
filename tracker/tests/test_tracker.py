"""Schema, foreign-key enforcement, and export/rebuild round-trip tests."""

from __future__ import annotations

import importlib.util
import shutil
import sqlite3
import tempfile
import unittest
from argparse import Namespace
from pathlib import Path

TRACKER_DIR = Path(__file__).resolve().parents[1]
WORK_DIR = TRACKER_DIR.parent / ".agent-work" / "tracker-tests"

_spec = importlib.util.spec_from_file_location("tracker_tool", TRACKER_DIR / "tracker.py")
assert _spec is not None and _spec.loader is not None
tracker = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(tracker)

SCHEMA_TEXT = (TRACKER_DIR / "schema.sql").read_text(encoding="utf-8")


def fresh_conn() -> sqlite3.Connection:
    conn = sqlite3.connect(":memory:")
    conn.execute("PRAGMA foreign_keys=ON")
    conn.executescript(SCHEMA_TEXT)
    return conn


class SchemaTests(unittest.TestCase):
    def test_object_counts_match_plan(self) -> None:
        conn = fresh_conn()
        try:
            tables = conn.execute(
                "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'"
            ).fetchone()[0]
            views = conn.execute(
                "SELECT COUNT(*) FROM sqlite_master WHERE type='view'").fetchone()[0]
            self.assertEqual(tables, 14)
            self.assertEqual(views, 8)
        finally:
            conn.close()

    def test_schema_is_idempotent(self) -> None:
        conn = fresh_conn()
        try:
            conn.executescript(SCHEMA_TEXT)
        finally:
            conn.close()

    def test_foreign_keys_are_enforced(self) -> None:
        conn = fresh_conn()
        try:
            with self.assertRaises(sqlite3.IntegrityError):
                conn.execute(
                    "INSERT INTO event(at, actor, command, subject, basis_digest)"
                    " VALUES ('t', 'ghost', 'seed', 's', 'd')")
        finally:
            conn.close()

    def test_double_claim_aborts(self) -> None:
        conn = fresh_conn()
        try:
            conn.execute("INSERT INTO actor(id, kind) VALUES ('t', 'tool')")
            for event_id in (1, 2):
                conn.execute(
                    "INSERT INTO event(id, at, actor, command, subject, basis_digest)"
                    " VALUES (?, 't', 't', 'claim', 'phase-3', 'd')", (event_id,))
            conn.execute(
                "INSERT INTO claim_event(id, subject, actor, kind, lease_token, lease_until, event_id)"
                " VALUES (1, 'phase-3', 't', 'acquire', 'k', '2999-01-01T00:00:00', 1)")
            with self.assertRaises(sqlite3.IntegrityError):
                conn.execute(
                    "INSERT INTO claim_event(id, subject, actor, kind, lease_token, lease_until, event_id)"
                    " VALUES (2, 'phase-3', 't', 'acquire', 'k2', '2999-01-01T00:00:00', 2)")
        finally:
            conn.close()


def seed_export_history(conn: sqlite3.Connection) -> None:
    """Full-history seed: 3 clause revisions, multi-span source, exclusion, A->B sources."""
    conn.execute("INSERT INTO actor(id, kind) VALUES ('tester', 'tool')")
    for event_id, command in ((1, "scan-sources"), (2, "clause add"),
                              (3, "clause revise"), (4, "clause retire"), (5, "span exclude")):
        conn.execute(
            "INSERT INTO event(id, at, actor, command, subject, basis_digest)"
            " VALUES (?, '2026-09-15T00:00:00+00:00', 'tester', ?, 'clauses', 'deadbeef')",
            (event_id, command))
    # two observations for one pointer: digest h1 then h2 (A -> B)
    conn.execute(
        "INSERT INTO source_field(file, pointer, source_event_id, start_offset, end_offset, source_sha256)"
        " VALUES ('product.md', 'I4', 1, 0, 100, 'h1')")
    conn.execute(
        "INSERT INTO source_field(file, pointer, source_event_id, start_offset, end_offset, source_sha256)"
        " VALUES ('product.md', 'I4', 3, 0, 120, 'h2')")
    conn.execute(
        "INSERT INTO clause(id, rev, kind, scope, text, text_sha256, status, event_id)"
        " VALUES ('C1', 1, 'pre', 'local', 'one result per call', 's1', 'active', 2)")
    conn.execute(
        "INSERT INTO clause(id, rev, kind, scope, text, text_sha256, status, event_id)"
        " VALUES ('C1', 2, 'pre', 'local', 'exactly one result per call', 's2', 'active', 3)")
    conn.execute(
        "INSERT INTO clause(id, rev, kind, scope, text, text_sha256, status, reason, event_id)"
        " VALUES ('C1', 3, 'pre', 'local', 'exactly one result per call', 's2', 'retired', 'superseded by C2', 4)")
    # multi-span clause: two spans on the first observation
    conn.execute(
        "INSERT INTO clause_source(clause_id, clause_rev, file, pointer, start_offset, end_offset, source_event_id, source_sha256)"
        " VALUES ('C1', 1, 'product.md', 'I4', 0, 30, 1, 'h1')")
    conn.execute(
        "INSERT INTO clause_source(clause_id, clause_rev, file, pointer, start_offset, end_offset, source_event_id, source_sha256)"
        " VALUES ('C1', 1, 'product.md', 'I4', 40, 60, 1, 'h1')")
    conn.execute(
        "INSERT INTO clause_source(clause_id, clause_rev, file, pointer, start_offset, end_offset, source_event_id, source_sha256)"
        " VALUES ('C1', 2, 'product.md', 'I4', 0, 25, 3, 'h2')")
    conn.execute(
        "INSERT INTO span_exclusion(file, pointer, start_offset, source_event_id, end_offset, reason, source_sha256, event_id)"
        " VALUES ('product.md', 'I4', 60, 1, 100, 'non-normative example text', 'h1', 5)")


EXPORT_TABLES = ("actor", "event", "source_field", "clause", "clause_source", "span_exclusion")


class ExportRoundtripTests(unittest.TestCase):
    def test_export_rebuild_preserves_history(self) -> None:
        with tempfile.TemporaryDirectory(dir=str(WORK_DIR)) as tmp:
            tmp_path = Path(tmp)
            source_db = tmp_path / "source.sqlite"
            conn = tracker.connect(source_db)
            try:
                tracker.apply_schema(conn)  # executescript commits implicitly
                conn.execute("BEGIN IMMEDIATE")
                seed_export_history(conn)
                conn.execute("COMMIT")
            finally:
                conn.close()

            export_file = tmp_path / "clauses.sql"
            tracker.cmd_export_clauses(Namespace(db=str(source_db), out=str(export_file)))

            rebuilt_db = tmp_path / "rebuilt.sqlite"
            tracker.cmd_rebuild(Namespace(db=str(rebuilt_db), load=str(export_file)))

            original = tracker.connect(source_db, readonly=True)
            rebuilt = tracker.connect(rebuilt_db, readonly=True)
            try:
                self.assertEqual(rebuilt.execute("PRAGMA foreign_key_check").fetchall(), [])
                for table in EXPORT_TABLES:
                    before = original.execute(f"SELECT * FROM {table}").fetchall()
                    after = rebuilt.execute(f"SELECT * FROM {table}").fetchall()
                    self.assertEqual(before, after, f"table {table} differs after rebuild")
            finally:
                original.close()
                rebuilt.close()

    def test_export_orders_actors_before_events(self) -> None:
        with tempfile.TemporaryDirectory(dir=str(WORK_DIR)) as tmp:
            tmp_path = Path(tmp)
            source_db = tmp_path / "source.sqlite"
            conn = tracker.connect(source_db)
            try:
                tracker.apply_schema(conn)  # executescript commits implicitly
                conn.execute("BEGIN IMMEDIATE")
                seed_export_history(conn)
                conn.execute("COMMIT")
            finally:
                conn.close()
            export_file = tmp_path / "clauses.sql"
            tracker.cmd_export_clauses(Namespace(db=str(source_db), out=str(export_file)))
            inserts = [line for line in export_file.read_text(encoding="utf-8").splitlines()
                       if line.startswith("INSERT INTO")]
            self.assertTrue(inserts, "export produced no INSERTs")
            self.assertTrue(inserts[0].startswith("INSERT INTO actor("),
                            f"first INSERT is not an actor row: {inserts[0]}")
            self.assertTrue(inserts[1].startswith("INSERT INTO event("),
                            f"second INSERT is not an event row: {inserts[1]}")


def run(db: Path, *argv: str) -> None:
    tracker.main(["--db", str(db), *argv])


def init_db(tmp: Path) -> Path:
    db = tmp / "ledger.sqlite"
    run(db, "init", "--actor", "setup:tool")
    return db


class MigrationTests(unittest.TestCase):
    def test_existing_001_ledger_upgrades_and_keeps_findings(self) -> None:
        with tempfile.TemporaryDirectory(dir=str(WORK_DIR)) as tmp:
            db = Path(tmp) / "old.sqlite"
            conn = sqlite3.connect(db)
            conn.execute("PRAGMA foreign_keys=ON")
            conn.executescript((TRACKER_DIR / "migrations" / "001_init.sql").read_text(encoding="utf-8"))
            conn.executescript(
                "INSERT INTO actor(id, kind) VALUES ('r', 'reviewer');"
                "INSERT INTO event(id, at, actor, command, subject, basis_digest) VALUES (1, 't', 'r', 'c', 's', 'd');"
                "INSERT INTO review_round(id, subject, basis_digest, reviewer, event_id) VALUES (1, 's', 'd', 'r', 1);"
                "INSERT INTO finding(id, rev, round_id, severity, category, target_kind, target_id, target_rev,"
                " status, actor, event_id) VALUES ('F1', 1, 1, 'Minor', 'c', 'clause', 'C1', 1, 'open', 'r', 1);")
            conn.close()

            conn = tracker.connect(db)
            try:
                tracker.apply_schema(conn)
                self.assertEqual(conn.execute("PRAGMA user_version").fetchone()[0], 2)
                row = conn.execute("SELECT id, status, summary FROM current_finding").fetchone()
                self.assertEqual(tuple(row), ("F1", "open", ""))
                conn.execute(
                    "INSERT INTO finding(id, rev, round_id, severity, category, target_kind, target_id,"
                    " target_rev, status, actor, event_id, summary)"
                    " VALUES ('F2', 1, 1, 'Minor', 'c', 'model', 'agent_coarse', 1, 'open', 'r', 1, 'x')")
                with self.assertRaises(sqlite3.IntegrityError):
                    conn.execute("UPDATE finding SET status = 'addressed' WHERE id = 'F1'")
                tracker.apply_schema(conn)  # a second apply is a no-op
                self.assertEqual(conn.execute("PRAGMA user_version").fetchone()[0], 2)
            finally:
                conn.close()

    def test_migrated_columns_match_schema(self) -> None:
        with tempfile.TemporaryDirectory(dir=str(WORK_DIR)) as tmp:
            old = Path(tmp) / "old.sqlite"
            conn = sqlite3.connect(old)
            conn.executescript((TRACKER_DIR / "migrations" / "001_init.sql").read_text(encoding="utf-8"))
            conn.close()
            migrated = tracker.connect(old)
            tracker.apply_schema(migrated)
            fresh = fresh_conn()
            try:
                for table in ("finding", "model_action", "review_round_close"):
                    columns = lambda c: [r[1] for r in c.execute(f"PRAGMA table_info({table})")]
                    self.assertEqual(columns(migrated), columns(fresh), table)
            finally:
                migrated.close()
                fresh.close()


QNT_EMPTY = """module interface_only {
  pure def ok(a: int): bool = a >= 0
}
"""

QNT_A = """module scanned {
  var x: int
  action init = x' = 0
  action step = x' = x + 1
  val INV = x >= 0
}
"""


@unittest.skipUnless(shutil.which("quint"), "quint is not on PATH")
class ScanModelsTests(unittest.TestCase):
    def test_scan_is_idempotent_and_rescan_replaces_current(self) -> None:
        with tempfile.TemporaryDirectory(dir=str(WORK_DIR)) as tmp:
            tmp_path = Path(tmp)
            db = init_db(tmp_path)
            model = tmp_path / "scanned.qnt"
            model.write_text(QNT_A, encoding="utf-8")
            run(db, "scan-models", str(model), "--actor", "scanner:tool")
            run(db, "scan-models", str(model), "--actor", "scanner:tool")  # unchanged digest
            conn = tracker.connect(db, readonly=True)
            try:
                self.assertEqual(conn.execute("SELECT COUNT(*) FROM event WHERE command = 'scan-models'")
                                 .fetchone()[0], 1)
                names = {(r["name"], r["kind"]) for r in conn.execute("SELECT * FROM current_model_action")}
                self.assertEqual(names, {("x", "var"), ("init", "action"), ("step", "action"), ("INV", "val")})
                locator = conn.execute("SELECT locator FROM model_action WHERE name = 'step'").fetchone()[0]
                self.assertTrue(locator.endswith("scanned.qnt:4"), locator)
            finally:
                conn.close()

            interface = tmp_path / "interface_only.qnt"
            interface.write_text(QNT_EMPTY, encoding="utf-8")
            run(db, "scan-models", str(interface), "--actor", "scanner:tool")
            run(db, "scan-models", str(interface), "--actor", "scanner:tool")  # no rows, still idempotent
            conn = tracker.connect(db, readonly=True)
            try:
                self.assertEqual(conn.execute(
                    "SELECT COUNT(*) FROM event WHERE command = 'scan-models' AND subject = 'interface_only'")
                    .fetchone()[0], 1)
            finally:
                conn.close()

            model.write_text(QNT_A.replace("  val INV = x >= 0\n", ""), encoding="utf-8")
            run(db, "scan-models", str(model), "--actor", "scanner:tool")
            conn = tracker.connect(db, readonly=True)
            try:
                names = {r["name"] for r in conn.execute("SELECT * FROM current_model_action")}
                self.assertEqual(names, {"x", "init", "step"})  # INV removed from the module
            finally:
                conn.close()


class ReviewFindingTests(unittest.TestCase):
    def test_passed_close_requires_verified_blockers_and_evidence(self) -> None:
        with tempfile.TemporaryDirectory(dir=str(WORK_DIR)) as tmp:
            tmp_path = Path(tmp)
            db = init_db(tmp_path)
            run(db, "review", "open", "phase-2c-models", "--basis", "abc123", "--actor", "rev:reviewer")
            run(db, "finding", "add", "--round", "1", "--severity", "Major", "--category", "fidelity",
                "--target-kind", "artifact", "--target-id", "tracker/schema.sql",
                "--summary", "example claim", "--actor", "rev:reviewer")
            with self.assertRaises(SystemExit):
                run(db, "review", "close", "1", "--outcome", "passed", "--actor", "rev:reviewer")
            run(db, "finding", "address", "R1-001", "--reason", "fixed", "--actor", "author:agent")
            with self.assertRaises(sqlite3.IntegrityError):  # the disposer cannot verify
                run(db, "finding", "verify", "R1-001", "--actor", "author:agent")
            run(db, "finding", "verify", "R1-001", "--actor", "rev:reviewer")
            with self.assertRaises(SystemExit):  # no review evidence yet
                run(db, "review", "close", "1", "--outcome", "passed", "--actor", "rev:reviewer")
            log = tmp_path / "check-log.txt"
            log.write_text("A-G inspected\n", encoding="utf-8")
            run(db, "evidence", "record", "--kind", "query", "--file", str(log),
                "--subject", "review:1", "--actor", "rev:reviewer")
            run(db, "review", "close", "1", "--outcome", "passed", "--actor", "rev:reviewer")
            conn = tracker.connect(db, readonly=True)
            try:
                self.assertEqual(conn.execute("SELECT outcome FROM review_round_close").fetchone()[0], "passed")
                statuses = [r[0] for r in conn.execute("SELECT status FROM finding ORDER BY rev")]
                self.assertEqual(statuses, ["open", "addressed", "verified"])
            finally:
                conn.close()

    def test_model_finding_becomes_stale_after_rescan_and_bad_transitions_abort(self) -> None:
        with tempfile.TemporaryDirectory(dir=str(WORK_DIR)) as tmp:
            db = init_db(Path(tmp))
            conn = tracker.connect(db)
            conn.executescript(
                "INSERT INTO event(id, at, actor, command, subject, basis_digest)"
                " VALUES (100, 't', 'setup', 'scan-models', 'agent_coarse', 'A');"
                "INSERT INTO model_action(module, name, event_id, kind, parameters, owner, source_sha256)"
                " VALUES ('agent_coarse', 'send', 100, 'action', '[]', 'agent_coarse', 'A');")
            conn.close()
            run(db, "review", "open", "models", "--basis", "A", "--actor", "rev:reviewer")
            run(db, "finding", "add", "--round", "1", "--severity", "Blocker", "--target-kind", "model",
                "--target-id", "agent_coarse", "--summary", "claim", "--actor", "rev:reviewer")
            with self.assertRaises(SystemExit):  # open -> verified is not a transition
                run(db, "finding", "verify", "R1-001", "--actor", "other:reviewer")
            with self.assertRaises(SystemExit):  # unknown module target
                run(db, "finding", "add", "--round", "1", "--severity", "Minor", "--target-kind", "model",
                    "--target-id", "nope", "--summary", "claim", "--actor", "rev:reviewer")
            conn = tracker.connect(db)
            conn.executescript(
                "INSERT INTO event(id, at, actor, command, subject, basis_digest)"
                " VALUES (200, 't', 'setup', 'scan-models', 'agent_coarse', 'B');"
                "INSERT INTO model_action(module, name, event_id, kind, parameters, owner, source_sha256)"
                " VALUES ('agent_coarse', 'send', 200, 'action', '[]', 'agent_coarse', 'B');")
            stale = conn.execute((TRACKER_DIR / "queries" / "stale_findings.sql").read_text()).fetchall()
            conn.close()
            self.assertEqual([r["id"] for r in stale], ["R1-001"])


class ObligationTests(unittest.TestCase):
    def test_add_needs_current_clause_and_model_declaration(self) -> None:
        with tempfile.TemporaryDirectory(dir=str(WORK_DIR)) as tmp:
            db = init_db(Path(tmp))
            conn = tracker.connect(db)
            conn.executescript(
                "INSERT INTO event(id, at, actor, command, subject, basis_digest) VALUES (100, 't', 'setup', 'x', 's', 'A');"
                "INSERT INTO model_action(module, name, event_id, kind, parameters, owner, source_sha256)"
                " VALUES ('agent_coarse', 'INV_C1', 100, 'val', '[]', 'agent_coarse', 'A');"
                "INSERT INTO clause(id, rev, kind, scope, text, text_sha256, status, event_id)"
                " VALUES ('I4', 1, 'invariant', 'interaction', 'one result per call', 'h', 'active', 100);")
            conn.close()
            with self.assertRaises(SystemExit):
                run(db, "obligation", "add", "O1", "--clause", "I9", "--module", "agent_coarse",
                    "--name", "INV_C1", "--kind", "invariant", "--actor", "author:agent")
            with self.assertRaises(SystemExit):
                run(db, "obligation", "add", "O1", "--clause", "I4", "--module", "agent_coarse",
                    "--name", "INV_C9", "--kind", "invariant", "--actor", "author:agent")
            run(db, "obligation", "add", "O1", "--clause", "I4", "--module", "agent_coarse",
                "--name", "INV_C1", "--kind", "invariant", "--actor", "author:agent")
            with self.assertRaises(SystemExit):
                run(db, "obligation", "retire", "O1", "--actor", "author:agent")  # no reason
            run(db, "obligation", "retire", "O1", "--reason", "clause split", "--actor", "author:agent")
            conn = tracker.connect(db, readonly=True)
            try:
                self.assertEqual(conn.execute("SELECT COUNT(*) FROM current_model_obligation").fetchone()[0], 0)
                self.assertEqual(conn.execute("SELECT MAX(rev) FROM model_obligation").fetchone()[0], 2)
            finally:
                conn.close()


if __name__ == "__main__":
    unittest.main()
