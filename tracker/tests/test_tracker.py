"""Schema, foreign-key enforcement, and export/rebuild round-trip tests."""

from __future__ import annotations

import importlib.util
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


if __name__ == "__main__":
    unittest.main()
