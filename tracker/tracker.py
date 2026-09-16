#!/usr/bin/env python3
"""Dev-only SQLite ledger for the native-agent semantic-reads preparation plan.

Python 3 standard library only. Not product state; the ledger file is gitignored.
Every connection enables PRAGMA foreign_keys=ON; no code path runs without it.
"""

from __future__ import annotations

import argparse
import contextlib
import datetime as dt
import hashlib
import json
import re
import secrets
import sqlite3
import subprocess
import sys
import tempfile
from collections.abc import Iterator
from pathlib import Path

ROOT = Path(__file__).resolve().parent
SCHEMA = ROOT / "schema.sql"
MIGRATIONS = ROOT / "migrations"
QUERIES = ROOT / "queries"
FIXTURES = ROOT / "tests" / "fixtures"
DEFAULT_DB = ROOT / "ledger.sqlite"


def connect(path: Path | str, readonly: bool = False) -> sqlite3.Connection:
    if readonly:
        uri = f"file:{Path(path).as_posix()}?mode=ro"
        conn = sqlite3.connect(uri, uri=True)
    else:
        conn = sqlite3.connect(str(path))
    conn.execute("PRAGMA foreign_keys=ON")
    conn.isolation_level = None  # manual BEGIN IMMEDIATE transactions
    conn.row_factory = sqlite3.Row
    return conn


def migration_number(path: Path) -> int:
    return int(path.name.split("_", 1)[0])


def apply_schema(conn: sqlite3.Connection) -> None:
    """Create a new ledger from schema.sql, or upgrade an existing one.

    schema.sql is the canonical latest DDL. `PRAGMA user_version` holds the number of
    the last applied migration; an existing ledger gets every later migration in order.
    """
    migrations = sorted(MIGRATIONS.glob("*.sql"), key=migration_number)
    latest = migration_number(migrations[-1]) if migrations else 0
    empty = conn.execute(
        "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table'").fetchone()[0] == 0
    if empty:
        conn.executescript(SCHEMA.read_text(encoding="utf-8"))
    else:
        version = conn.execute("PRAGMA user_version").fetchone()[0]
        for migration in migrations:
            if migration_number(migration) > version:
                conn.executescript(migration.read_text(encoding="utf-8"))
                conn.execute(f"PRAGMA user_version = {migration_number(migration)}")
    conn.execute(f"PRAGMA user_version = {latest}")


def parse_actor(spec: str) -> tuple[str, str]:
    actor_id, _, kind = spec.partition(":")
    if not actor_id:
        raise SystemExit("Error: --actor must be ID[:KIND] with kind in human, agent, reviewer, tool")
    if kind and kind not in ("human", "agent", "reviewer", "tool"):
        raise SystemExit(f"Error: unknown actor kind {kind!r}")
    return actor_id, kind or "agent"


def ensure_actor(conn: sqlite3.Connection, actor_id: str, kind: str) -> None:
    conn.execute("INSERT OR IGNORE INTO actor(id, kind) VALUES (?, ?)", (actor_id, kind))


def record_event(conn: sqlite3.Connection, actor_id: str, actor_kind: str,
                 command: str, subject: str, basis_digest: str) -> int:
    ensure_actor(conn, actor_id, actor_kind)
    at = dt.datetime.now(dt.timezone.utc).isoformat(timespec="seconds")
    cur = conn.execute(
        "INSERT INTO event(at, actor, command, subject, basis_digest) VALUES (?, ?, ?, ?, ?)",
        (at, actor_id, command, subject, basis_digest),
    )
    if cur.lastrowid is None:
        raise sqlite3.Error("sqlite did not assign an event id")
    return cur.lastrowid


def git_head() -> str:
    try:
        out = subprocess.run(["git", "rev-parse", "HEAD"], capture_output=True,
                             text=True, check=True, cwd=ROOT.parent)
        return out.stdout.strip()
    except (OSError, subprocess.CalledProcessError) as exc:
        raise SystemExit(f"Error: cannot determine repository HEAD for basis digest: {exc}") from exc


def parse_lease(spec: str) -> str:
    match = re.fullmatch(r"(\d+)([hm])", spec)
    if not match:
        raise SystemExit("Error: --lease must look like 2h or 90m")
    amount, unit = int(match.group(1)), match.group(2)
    delta = dt.timedelta(hours=amount) if unit == "h" else dt.timedelta(minutes=amount)
    return (dt.datetime.now(dt.timezone.utc) + delta).isoformat(timespec="seconds")


# --------------------------------------------------------------------------- commands

def cmd_init(args: argparse.Namespace) -> None:
    conn = connect(args.db)
    try:
        apply_schema(conn)  # executescript commits implicitly; run it outside the transaction
        conn.execute("BEGIN IMMEDIATE")
        actor_id, kind = parse_actor(args.actor)
        ensure_actor(conn, actor_id, kind)
        conn.execute("COMMIT")
    except sqlite3.Error:
        conn.execute("ROLLBACK")
        raise
    finally:
        conn.close()
    print(f"initialized {args.db} (actor {args.actor})")


def cmd_rebuild(args: argparse.Namespace) -> None:
    db = Path(args.db)
    if db.exists():
        db.unlink()
    conn = connect(db)
    try:
        apply_schema(conn)
        if args.load:
            script = Path(args.load).read_text(encoding="utf-8")
            conn.executescript("BEGIN IMMEDIATE;\n" + script + "\nCOMMIT;")
            violations = conn.execute("PRAGMA foreign_key_check").fetchall()
            if violations:
                raise SystemExit(f"Error: rebuilt database has foreign-key violations: {violations}")
    finally:
        conn.close()
    print(f"rebuilt {db}" + (f" from {args.load}" if args.load else ""))


def sql_literal(value: object) -> str:
    if value is None:
        return "NULL"
    if isinstance(value, int):
        return str(value)
    text = str(value).replace("'", "''")
    return f"'{text}'"


def cmd_export_clauses(args: argparse.Namespace) -> None:
    conn = connect(args.db, readonly=True)
    lines = [
        "-- tracker/clauses/clauses.sql — generated by `tracker.py export-clauses`.",
        "-- Load order matters: actors, events, source observations, clauses, clause sources,",
        "-- span exclusions. Triggers are active during rebuild and full history satisfies them.",
    ]

    def dump(table: str, order_by: str) -> None:
        rows = conn.execute(f"SELECT * FROM {table} ORDER BY {order_by}").fetchall()
        if not rows:
            return
        cols = [d[0] for d in conn.execute(f"SELECT * FROM {table} LIMIT 0").description or []]
        lines.append(f"-- {table} ({len(rows)} rows)")
        for row in rows:
            values = ", ".join(sql_literal(v) for v in row)
            lines.append(f"INSERT INTO {table}({', '.join(cols)}) VALUES ({values});")

    exported_events = {r[0] for r in conn.execute(
        "SELECT DISTINCT event_id FROM clause UNION "
        "SELECT DISTINCT event_id FROM span_exclusion UNION "
        "SELECT DISTINCT source_event_id FROM clause_source UNION "
        "SELECT DISTINCT source_event_id FROM span_exclusion"
    )}
    if not exported_events:
        lines.append("-- no clause history to export")
    else:
        marks = ", ".join("?" for _ in exported_events)
        actors = conn.execute(
            f"SELECT DISTINCT actor FROM event WHERE id IN ({marks}) ORDER BY actor",
            sorted(exported_events)).fetchall()
        for row in actors:
            a = conn.execute("SELECT * FROM actor WHERE id = ?", (row[0],)).fetchone()
            lines.append(
                f"INSERT INTO actor(id, kind, model, session_ref) VALUES "
                f"({sql_literal(a['id'])}, {sql_literal(a['kind'])}, "
                f"{sql_literal(a['model'])}, {sql_literal(a['session_ref'])});")
        for row in conn.execute(
                f"SELECT * FROM event WHERE id IN ({marks}) ORDER BY id", sorted(exported_events)):
            lines.append(
                f"INSERT INTO event(id, at, actor, command, subject, basis_digest) VALUES "
                f"({row['id']}, {sql_literal(row['at'])}, {sql_literal(row['actor'])}, "
                f"{sql_literal(row['command'])}, {sql_literal(row['subject'])}, "
                f"{sql_literal(row['basis_digest'])});")
        dump("source_field", "source_event_id, file, pointer")
        dump("clause", "event_id, id, rev")
        dump("clause_source", "source_event_id, clause_id, clause_rev, start_offset")
        dump("span_exclusion", "source_event_id, file, start_offset")
    conn.close()
    Path(args.out).write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"exported clause history to {args.out}")


def cmd_query(args: argparse.Namespace) -> None:
    query_file = QUERIES / f"{args.name}.sql"
    if not query_file.is_file():
        available = ", ".join(sorted(p.stem for p in QUERIES.glob("*.sql")))
        raise SystemExit(f"Error: unknown query {args.name!r}; available: {available}")
    params: dict[str, str] = {}
    for pair in args.params:
        key, _, value = pair.partition("=")
        if not value and key not in params:
            raise SystemExit(f"Error: query parameter must look like k=v, got {pair!r}")
        params[key] = value
    conn = connect(args.db, readonly=args.readonly)
    try:
        rows = conn.execute(query_file.read_text(encoding="utf-8"), params).fetchall()
        if rows:
            cols = list(rows[0].keys())
            print("\t".join(cols))
            for row in rows:
                print("\t".join("NULL" if v is None else str(v) for v in row))
        print(f"({len(rows)} rows)")
    finally:
        conn.close()


def cmd_claim(args: argparse.Namespace) -> None:
    actor_id, kind = parse_actor(args.actor)
    conn = connect(args.db)
    try:
        conn.execute("BEGIN IMMEDIATE")
        if args.claim_action == "acquire":
            token = secrets.token_hex(8)
            event_id = record_event(conn, actor_id, kind, "claim acquire", args.subject, git_head())
            conn.execute(
                "INSERT INTO claim_event(subject, actor, kind, lease_token, lease_until, event_id)"
                " VALUES (?, ?, 'acquire', ?, ?, ?)",
                (args.subject, actor_id, token, parse_lease(args.lease), event_id))
            conn.execute("COMMIT")
            print(f"acquired {args.subject} (token {token})")
        else:
            if not args.token:
                raise SystemExit("Error: claim release requires --token from the acquire")
            event_id = record_event(conn, actor_id, kind, "claim release", args.subject, git_head())
            active = conn.execute(
                "SELECT * FROM active_claims WHERE subject = ? AND actor = ? AND lease_token = ?",
                (args.subject, actor_id, args.token)).fetchone()
            if active is None:
                conn.execute("ROLLBACK")
                raise SystemExit(
                    f"Error: no active claim on {args.subject} for actor {actor_id} with that token")
            conn.execute(
                "INSERT INTO claim_event(subject, actor, kind, lease_token, lease_until, event_id)"
                " VALUES (?, ?, 'release', ?, ?, ?)",
                (args.subject, actor_id, args.token, active["lease_until"], event_id))
            conn.execute("COMMIT")
            print(f"released {args.subject}")
    except sqlite3.Error:
        try:
            conn.execute("ROLLBACK")
        except sqlite3.Error:
            pass
        raise
    finally:
        conn.close()


EVIDENCE_KINDS = ("quint_typecheck", "quint_run", "quint_verify", "quint_witness",
                  "quint_mutant", "quint_refinement", "junit", "jqwik", "replay", "pit",
                  "query", "precondition")
EVIDENCE_RESULTS = ("pass", "fail", "counterexample", "violation", "timeout")


def cmd_evidence(args: argparse.Namespace) -> None:
    if args.kind not in EVIDENCE_KINDS:
        raise SystemExit(f"Error: evidence kind must be one of {', '.join(EVIDENCE_KINDS)}")
    if args.result not in EVIDENCE_RESULTS:
        raise SystemExit(f"Error: evidence result must be one of {', '.join(EVIDENCE_RESULTS)}")
    artifact = Path(args.file)
    if not artifact.is_file():
        raise SystemExit(f"Error: artifact {args.file} does not exist")
    artifact_sha = hashlib.sha256(artifact.read_bytes()).hexdigest()
    actor_id, kind = parse_actor(args.actor)
    conn = connect(args.db)
    try:
        conn.execute("BEGIN IMMEDIATE")
        event_id = record_event(conn, actor_id, kind, "evidence record", args.subject, git_head())
        conn.execute(
            "INSERT INTO evidence(event_id, subject, kind, tool_version, params, result,"
            " artifact_sha256, basis_digest) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            (event_id, args.subject, args.kind, args.tool_version, args.params,
             args.result, artifact_sha, git_head()))
        conn.execute("COMMIT")
    except sqlite3.Error:
        try:
            conn.execute("ROLLBACK")
        except sqlite3.Error:
            pass
        raise
    finally:
        conn.close()
    print(f"recorded {args.kind} evidence for {args.subject} ({args.result})")


@contextlib.contextmanager
def write_tx(db: str) -> Iterator[sqlite3.Connection]:
    """One BEGIN IMMEDIATE transaction; rolls back on any error, including SystemExit."""
    conn = connect(db)
    try:
        conn.execute("BEGIN IMMEDIATE")
        yield conn
        conn.execute("COMMIT")
    except BaseException:
        try:
            conn.execute("ROLLBACK")
        except sqlite3.Error:
            pass
        raise
    finally:
        conn.close()


# ------------------------------------------------------------------ scan-models

MODELS_DIR = ROOT.parent / "native-agent-docs" / "models"
DECLARATION_KINDS = {"action": "action", "val": "val", "pureval": "val"}


def quint_declarations(path: Path, quint: str) -> list[dict[str, object]]:
    """Return the action, var and val declarations of the module named like the file."""
    with tempfile.TemporaryDirectory() as tmp:
        out = Path(tmp) / "parse.json"
        proc = subprocess.run([quint, "parse", str(path), "--out", str(out)],
                              capture_output=True, text=True)
        if proc.returncode != 0 or not out.is_file():
            raise SystemExit(f"Error: quint parse failed for {path}:\n{proc.stdout}{proc.stderr}")
        parsed = json.loads(out.read_text(encoding="utf-8"))
    modules = [m for m in parsed.get("modules", []) if m.get("name") == path.stem]
    if len(modules) != 1:
        raise SystemExit(f"Error: {path} must declare exactly one module named {path.stem}")
    source_lines = path.read_text(encoding="utf-8").splitlines()
    declarations: list[dict[str, object]] = []
    for decl in modules[0]["declarations"]:
        if decl["kind"] == "var":
            kind, params = "var", []
        elif decl["kind"] == "def" and decl.get("qualifier") in DECLARATION_KINDS:
            kind = DECLARATION_KINDS[decl["qualifier"]]
            expr = decl.get("expr", {})
            params = [p["name"] for p in expr.get("params", [])] if expr.get("kind") == "lambda" else []
        else:
            continue
        pattern = re.compile(rf"^\s*(?:pure\s+)?(?:action|val|var)\s+{re.escape(decl['name'])}\b")
        line = next((i for i, text in enumerate(source_lines, 1) if pattern.match(text)), None)
        declarations.append({"name": decl["name"], "kind": kind, "parameters": json.dumps(params),
                             "line": line})
    return declarations


def cmd_scan_models(args: argparse.Namespace) -> None:
    files = [Path(f) for f in args.files] if args.files else sorted(MODELS_DIR.glob("*.qnt"))
    if not files:
        raise SystemExit(f"Error: no .qnt files found under {MODELS_DIR}")
    parsed = []
    for path in files:  # parse everything before the transaction opens
        if not path.is_file():
            raise SystemExit(f"Error: model file {path} does not exist")
        digest = hashlib.sha256(path.read_bytes()).hexdigest()
        parsed.append((path, digest, quint_declarations(path, args.quint)))
    actor_id, kind = parse_actor(args.actor)
    with write_tx(args.db) as conn:
        for path, digest, declarations in parsed:
            module = path.stem
            # Keyed on the scan event, not model_action: a module with no action, var or
            # val declarations (for example agent_interface) writes an event and no rows.
            latest = conn.execute(
                "SELECT basis_digest FROM event WHERE command = 'scan-models' AND subject = ?"
                " ORDER BY id DESC LIMIT 1", (module,)).fetchone()
            if latest is not None and latest["basis_digest"] == digest:
                print(f"unchanged {module} ({digest[:12]})")
                continue
            event_id = record_event(conn, actor_id, kind, "scan-models", module, digest)
            rel = path.resolve().relative_to(ROOT.parent) if path.resolve().is_relative_to(ROOT.parent) else path
            for decl in declarations:
                conn.execute(
                    "INSERT INTO model_action(module, name, event_id, kind, parameters, owner,"
                    " source_sha256, locator) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    (module, decl["name"], event_id, decl["kind"], decl["parameters"], module, digest,
                     f"{rel}:{decl['line']}" if decl["line"] else None))
            print(f"scanned {module} ({digest[:12]}): {len(declarations)} declarations, event {event_id}")


# ------------------------------------------------------------------ sources, clauses, exclusions

# Phase 4 source list. Markdown: one field per non-blank line in the cited ranges, pointer
# `L<n>`. JSON: pointer patterns where `*` matches every list index or object key; a
# container expands to one field per scalar leaf.
MARKDOWN_SOURCES = {"native-agent-docs/product.md": [(136, 158), (179, 189), (229, 229)]}
JSON_SOURCE_GLOB = "native-agent-docs/read-*/spec.json"
JSON_POINTERS = (
    "/scope", "/non_goals/*", "/assumptions/*", "/requirements/*/statement",
    "/acceptance/*/when", "/acceptance/*/then", "/acceptance/*/target", "/decisions/*",
    "/operation_matrix/*/*", "/null_boundary_matrix/*/*",
    "/type_safety_audit/*/invalid", "/type_safety_audit/*/type_api_prevention",
    "/type_safety_audit/*/residual_runtime_obligation", "/type_safety_audit/*/justification",
    "/stages/*/entry", "/stages/*/exit", "/stages/*/outcome", "/evidence/*/description",
)
CLAUSE_KINDS = ("pre", "post", "invariant", "outcome", "decision")
CLAUSE_SCOPES = ("local", "interaction")


def escape_pointer(token: str) -> str:
    return token.replace("~", "~0").replace("/", "~1")


def scalar_text(value: object) -> str:
    return value if isinstance(value, str) else json.dumps(value)


def json_leaves(value: object, pointer: str) -> Iterator[tuple[str, str]]:
    if isinstance(value, dict):
        for key, item in value.items():
            yield from json_leaves(item, f"{pointer}/{escape_pointer(key)}")
    elif isinstance(value, list):
        for index, item in enumerate(value):
            yield from json_leaves(item, f"{pointer}/{index}")
    else:
        yield pointer, scalar_text(value)


def expand_pointer(doc: object, pattern: str) -> list[tuple[str, object]]:
    matches: list[tuple[str, object]] = [("", doc)]
    for token in pattern.lstrip("/").split("/"):
        step: list[tuple[str, object]] = []
        for pointer, node in matches:
            if isinstance(node, dict):
                keys = list(node) if token == "*" else ([token] if token in node else [])
                step.extend((f"{pointer}/{escape_pointer(k)}", node[k]) for k in keys)
            elif isinstance(node, list):
                indexes = range(len(node)) if token == "*" else (
                    [int(token)] if token.isdigit() and int(token) < len(node) else [])
                step.extend((f"{pointer}/{i}", node[i]) for i in indexes)
        matches = step
    return matches


def source_fields(rel: str) -> dict[str, str]:
    """Pointer -> decoded field text for one listed source file, in document order."""
    path = ROOT.parent / rel
    if not path.is_file():
        raise SystemExit(f"Error: source {rel} does not exist")
    fields: dict[str, str] = {}
    if rel in MARKDOWN_SOURCES:
        lines = path.read_bytes().decode("utf-8").splitlines(keepends=True)
        for first, last in MARKDOWN_SOURCES[rel]:
            if last > len(lines):
                raise SystemExit(f"Error: {rel} has no line {last}")
            for n in range(first, last + 1):
                if lines[n - 1].strip():
                    fields[f"L{n}"] = lines[n - 1]
        return fields
    doc = json.loads(path.read_bytes().decode("utf-8"))
    for pattern in JSON_POINTERS:
        matches = expand_pointer(doc, pattern)
        if not matches:
            raise SystemExit(f"Error: {rel} has no field matching {pattern}")
        for pointer, node in matches:
            fields.update(json_leaves(node, pointer))
    return fields


def listed_sources() -> list[str]:
    json_files = sorted(str(p.relative_to(ROOT.parent)) for p in ROOT.parent.glob(JSON_SOURCE_GLOB))
    return [*MARKDOWN_SOURCES, *json_files]


def cmd_scan_sources(args: argparse.Namespace) -> None:
    files = args.files or listed_sources()
    scanned = []
    for rel in files:  # read everything before the transaction opens
        digest = hashlib.sha256((ROOT.parent / rel).read_bytes()).hexdigest()
        scanned.append((rel, digest, source_fields(rel)))
    actor_id, kind = parse_actor(args.actor)
    with write_tx(args.db) as conn:
        for rel, digest, fields in scanned:
            latest = conn.execute(
                "SELECT basis_digest FROM event WHERE command = 'scan-sources' AND subject = ?"
                " ORDER BY id DESC LIMIT 1", (rel,)).fetchone()
            if latest is not None and latest["basis_digest"] == digest:
                print(f"unchanged {rel} ({digest[:12]})")
                continue
            event_id = record_event(conn, actor_id, kind, "scan-sources", rel, digest)
            for pointer, text in fields.items():
                conn.execute(
                    "INSERT INTO source_field(file, pointer, source_event_id, start_offset, end_offset,"
                    " source_sha256) VALUES (?, ?, ?, 0, ?, ?)",
                    (rel, pointer, event_id, len(text), digest))
            print(f"scanned {rel} ({digest[:12]}): {len(fields)} fields, event {event_id}")


def current_field(conn: sqlite3.Connection, rel: str, pointer: str, start: int, end: int) -> sqlite3.Row:
    row = conn.execute("SELECT * FROM current_source_field WHERE file = ? AND pointer = ?",
                       (rel, pointer)).fetchone()
    if row is None:
        raise SystemExit(f"Error: {rel}#{pointer} is not a current source field; run scan-sources")
    if not 0 <= start < end <= row["end_offset"]:
        raise SystemExit(f"Error: span {start}-{end} is outside {rel}#{pointer} (0-{row['end_offset']})")
    return row


def parse_span(spec: str) -> tuple[str, str, int, int]:
    """FILE#POINTER[@START-END]; no range means the whole field."""
    match = re.fullmatch(r"([^#]+)#([^@]+)(?:@(\d+)-(\d+))?", spec)
    if not match:
        raise SystemExit(f"Error: span {spec!r} must look like FILE#POINTER or FILE#POINTER@START-END")
    rel, pointer, start, end = match.groups()
    return rel, pointer, int(start) if start else 0, int(end) if end else -1


def resolved_span(conn: sqlite3.Connection, spec: str) -> tuple[sqlite3.Row, int, int]:
    rel, pointer, start, end = parse_span(spec)
    if end == -1:
        row = conn.execute("SELECT end_offset FROM current_source_field WHERE file = ? AND pointer = ?",
                           (rel, pointer)).fetchone()
        end = row["end_offset"] if row is not None else 1
    return current_field(conn, rel, pointer, start, end), start, end


def insert_clause(conn: sqlite3.Connection, args: argparse.Namespace, rev: int, event_id: int,
                  fields: dict[str, object], spans: list[str]) -> None:
    text = str(fields["text"])
    conn.execute(
        'INSERT INTO clause(id, rev, kind, scope, "trigger", outcome, text, text_sha256, status, reason,'
        " event_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        (args.id, rev, fields["kind"], fields["scope"], fields["trigger"], fields["outcome"], text,
         hashlib.sha256(text.encode("utf-8")).hexdigest(), fields["status"], fields["reason"], event_id))
    for spec in spans:
        row, start, end = resolved_span(conn, spec)
        conn.execute(
            "INSERT INTO clause_source(clause_id, clause_rev, file, pointer, start_offset, end_offset,"
            " source_event_id, source_sha256) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            (args.id, rev, row["file"], row["pointer"], start, end, row["source_event_id"],
             row["source_sha256"]))


def cmd_clause(args: argparse.Namespace) -> None:
    actor_id, kind = parse_actor(args.actor)
    with write_tx(args.db) as conn:
        existing = latest_revision(conn, "clause", args.id)
        if args.clause_action in ("add", "revise"):
            if args.clause_action == "add" and existing is not None:
                raise SystemExit(f"Error: clause {args.id} already exists; use revise")
            if args.clause_action == "revise" and (existing is None or existing["status"] == "retired"):
                raise SystemExit(f"Error: no active clause {args.id}")
            base = dict(existing) if existing is not None else {}
            fields: dict[str, object] = {
                name: getattr(args, name) if getattr(args, name) is not None else base.get(name)
                for name in ("kind", "scope", "trigger", "outcome", "text")}
            if fields["kind"] not in CLAUSE_KINDS:
                raise SystemExit(f"Error: clause kind must be one of {', '.join(CLAUSE_KINDS)}")
            if fields["scope"] not in CLAUSE_SCOPES:
                raise SystemExit(f"Error: clause scope must be one of {', '.join(CLAUSE_SCOPES)}")
            if not fields["text"]:
                raise SystemExit("Error: clause needs --text")
            spans = args.span or []
            if not spans and existing is not None:
                spans = [f"{s['file']}#{s['pointer']}@{s['start_offset']}-{s['end_offset']}"
                         for s in conn.execute("SELECT * FROM clause_source WHERE clause_id = ? AND clause_rev = ?",
                                               (args.id, existing["rev"]))]
            if not spans:
                raise SystemExit("Error: clause needs at least one --span FILE#POINTER[@START-END]")
            fields.update(status="active", reason=args.reason)
            rev = 1 if existing is None else existing["rev"] + 1
            event_id = record_event(conn, actor_id, kind, f"clause {args.clause_action}", args.id, git_head())
            insert_clause(conn, args, rev, event_id, fields, spans)
            print(f"{'added' if rev == 1 else 'revised'} clause {args.id} rev {rev}")
        else:
            if existing is None or existing["status"] == "retired":
                raise SystemExit(f"Error: no active clause {args.id}")
            if not args.reason:
                raise SystemExit("Error: clause retire requires --reason")
            event_id = record_event(conn, actor_id, kind, "clause retire", args.id, git_head())
            conn.execute(
                'INSERT INTO clause(id, rev, kind, scope, "trigger", outcome, text, text_sha256, status,'
                " reason, event_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'retired', ?, ?)",
                (args.id, existing["rev"] + 1, existing["kind"], existing["scope"], existing["trigger"],
                 existing["outcome"], existing["text"], existing["text_sha256"], args.reason, event_id))
            print(f"retired clause {args.id}")


def cmd_span(args: argparse.Namespace) -> None:
    if args.span_action == "gaps":
        cmd_span_gaps(args)
        return
    if not args.span:
        raise SystemExit("Error: span exclude needs FILE#POINTER[@START-END]")
    if not args.reason:
        raise SystemExit("Error: span exclude requires --reason")
    actor_id, kind = parse_actor(args.actor)
    with write_tx(args.db) as conn:
        row, start, end = resolved_span(conn, args.span)
        event_id = record_event(conn, actor_id, kind, "span exclude", args.span, git_head())
        conn.execute(
            "INSERT INTO span_exclusion(file, pointer, start_offset, source_event_id, end_offset, reason,"
            " source_sha256, event_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            (row["file"], row["pointer"], start, row["source_event_id"], end, args.reason,
             row["source_sha256"], event_id))
        print(f"excluded {row['file']}#{row['pointer']}@{start}-{end}")


def quoted_span(span: object) -> str:
    """A span is FILE#POINTER[@START-END], or {"at": FILE#POINTER, "quote": TEXT} for the unique occurrence."""
    if isinstance(span, str):
        return span
    if not isinstance(span, dict) or set(span) != {"at", "quote"}:
        raise SystemExit(f"Error: span {span!r} must be a string or {{at, quote}}")
    rel, pointer = str(span["at"]).split("#", 1)
    text = source_fields(rel).get(pointer)
    if text is None:
        raise SystemExit(f"Error: {span['at']} is not a listed source field")
    quote = str(span["quote"])
    start = text.find(quote)
    if start < 0 or text.find(quote, start + 1) >= 0:
        raise SystemExit(f"Error: quote {quote!r} must occur exactly once in {span['at']}")
    return f"{rel}#{pointer}@{start}-{start + len(quote)}"


GAP_TEXT = re.compile(r"^[\s.,;:()\-]*(?:I\d+\.|\d+\.|-)?[\s.,;:()\-]*$")


def cmd_span_gaps(args: argparse.Namespace) -> None:
    """Exclude uncovered segments that hold only whitespace, punctuation or a list label."""
    actor_id, kind = parse_actor(args.actor)
    texts: dict[str, dict[str, str]] = {}
    with write_tx(args.db) as conn:
        uncovered = conn.execute((QUERIES / "uncovered_source_spans.sql").read_text(encoding="utf-8")).fetchall()
        excluded = 0
        for seg in uncovered:
            fields = texts.setdefault(seg["file"], source_fields(seg["file"]))
            piece = fields[seg["pointer"]][seg["start_offset"]:seg["end_offset"]]
            if not GAP_TEXT.match(piece):
                continue
            row = current_field(conn, seg["file"], seg["pointer"], seg["start_offset"], seg["end_offset"])
            spec = f"{seg['file']}#{seg['pointer']}@{seg['start_offset']}-{seg['end_offset']}"
            event_id = record_event(conn, actor_id, kind, "span exclude", spec, git_head())
            conn.execute(
                "INSERT INTO span_exclusion(file, pointer, start_offset, source_event_id, end_offset, reason,"
                " source_sha256, event_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                (row["file"], row["pointer"], seg["start_offset"], row["source_event_id"], seg["end_offset"],
                 "separator: whitespace, punctuation or list label", row["source_sha256"], event_id))
            excluded += 1
        print(f"excluded {excluded} separator segments; {len(uncovered) - excluded} uncovered segments remain")


def cmd_apply(args: argparse.Namespace) -> None:
    """Apply JSON lines of `clause add` and `span exclude` records; lines already applied are skipped.

    {"op": "clause", "id", "kind", "scope", "trigger"?, "outcome"?, "text", "spans": [...]}
    {"op": "exclude", "span", "reason"}
    """
    records = [(n, json.loads(line)) for n, line in
               enumerate(Path(args.file).read_text(encoding="utf-8").splitlines(), 1) if line.strip()]
    applied = skipped = 0
    for n, rec in records:
        conn = connect(args.db, readonly=True)
        try:
            if rec["op"] == "clause":
                done = conn.execute("SELECT 1 FROM clause WHERE id = ?", (rec["id"],)).fetchone() is not None
            elif rec["op"] == "exclude":
                rel, pointer, start, _ = parse_span(quoted_span(rec["span"]))
                done = conn.execute(
                    "SELECT 1 FROM span_exclusion x JOIN current_source_field c ON c.file = x.file"
                    " AND c.pointer = x.pointer AND c.source_event_id = x.source_event_id"
                    " WHERE x.file = ? AND x.pointer = ? AND x.start_offset = ?",
                    (rel, pointer, start)).fetchone() is not None
            else:
                raise SystemExit(f"Error: {args.file}:{n}: unknown op {rec['op']!r}")
        finally:
            conn.close()
        if done:
            skipped += 1
            continue
        if rec["op"] == "clause":
            argv = ["clause", "add", rec["id"], "--kind", rec["kind"], "--scope", rec["scope"],
                    "--text", rec["text"], "--actor", args.actor]
            for name in ("trigger", "outcome"):
                if rec.get(name):
                    argv += [f"--{name}", rec[name]]
            for span in rec["spans"]:
                argv += ["--span", quoted_span(span)]
        else:
            argv = ["span", "exclude", quoted_span(rec["span"]), "--reason", rec["reason"], "--actor", args.actor]
        try:
            with contextlib.redirect_stdout(None):
                main(["--db", args.db, *argv])
        except (SystemExit, sqlite3.Error) as exc:
            raise SystemExit(f"Error: {args.file}:{n}: {exc}") from exc
        applied += 1
    print(f"applied {applied}, skipped {skipped} from {args.file}")


def cmd_source_text(args: argparse.Namespace) -> None:
    """Print current source fields as JSON lines (pointer, length, text) for clause authoring."""
    conn = connect(args.db, readonly=True)
    try:
        for rel in args.files or listed_sources():
            fields = source_fields(rel)
            current = {r["pointer"]: r for r in conn.execute(
                "SELECT * FROM current_source_field WHERE file = ?", (rel,))}
            digest = hashlib.sha256((ROOT.parent / rel).read_bytes()).hexdigest()
            for pointer, text in fields.items():
                row = current.get(pointer)
                if row is None or row["source_sha256"] != digest:
                    raise SystemExit(f"Error: {rel}#{pointer} is not scanned at the file digest; run scan-sources")
                print(json.dumps({"span": f"{rel}#{pointer}", "length": len(text), "text": text}))
    finally:
        conn.close()


# ------------------------------------------------------------------ obligations

OBLIGATION_KINDS = ("invariant", "witness", "mutant", "refinement")


def latest_revision(conn: sqlite3.Connection, table: str, row_id: str) -> sqlite3.Row | None:
    return conn.execute(f"SELECT * FROM {table} WHERE id = ? ORDER BY rev DESC LIMIT 1",
                        (row_id,)).fetchone()


def cmd_obligation(args: argparse.Namespace) -> None:
    actor_id, kind = parse_actor(args.actor)
    with write_tx(args.db) as conn:
        existing = latest_revision(conn, "model_obligation", args.id)
        if args.obligation_action == "add":
            if existing is not None:
                raise SystemExit(f"Error: obligation {args.id} already exists")
            if args.kind not in OBLIGATION_KINDS:
                raise SystemExit(f"Error: obligation kind must be one of {', '.join(OBLIGATION_KINDS)}")
            clause = conn.execute("SELECT id, rev FROM current_clause WHERE id = ?",
                                  (args.clause,)).fetchone()
            if clause is None:
                raise SystemExit(f"Error: no current clause {args.clause}")
            model = conn.execute(
                "SELECT event_id FROM current_model_action WHERE module = ? AND name = ?",
                (args.module, args.name)).fetchone()
            if model is None:
                raise SystemExit(f"Error: {args.module}::{args.name} is not in the current model scan")
            event_id = record_event(conn, actor_id, kind, "obligation add", args.id, git_head())
            conn.execute(
                "INSERT INTO model_obligation(id, rev, clause_id, clause_rev, module, name, model_event_id,"
                " kind, status, event_id) VALUES (?, 1, ?, ?, ?, ?, ?, ?, 'active', ?)",
                (args.id, clause["id"], clause["rev"], args.module, args.name, model["event_id"],
                 args.kind, event_id))
            print(f"added obligation {args.id}")
        else:
            if existing is None or existing["status"] == "retired":
                raise SystemExit(f"Error: no active obligation {args.id}")
            if not args.reason:
                raise SystemExit("Error: obligation retire requires --reason")
            event_id = record_event(conn, actor_id, kind, "obligation retire", args.id, git_head())
            conn.execute(
                "INSERT INTO model_obligation(id, rev, clause_id, clause_rev, module, name, model_event_id,"
                " kind, status, reason, event_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'retired', ?, ?)",
                (args.id, existing["rev"] + 1, existing["clause_id"], existing["clause_rev"],
                 existing["module"], existing["name"], existing["model_event_id"], existing["kind"],
                 args.reason, event_id))
            print(f"retired obligation {args.id}")


# ------------------------------------------------------------------ reviews and findings

BLOCKING_SEVERITIES = ("Blocker", "Major")


def open_round(conn: sqlite3.Connection, round_id: int) -> sqlite3.Row:
    row = conn.execute("SELECT * FROM open_rounds WHERE id = ?", (round_id,)).fetchone()
    if row is None:
        raise SystemExit(f"Error: review round {round_id} is not open")
    return row


def cmd_review(args: argparse.Namespace) -> None:
    actor_id, kind = parse_actor(args.actor)
    with write_tx(args.db) as conn:
        if args.review_action == "open":
            if not args.basis:
                raise SystemExit("Error: review open requires --basis DIGEST")
            event_id = record_event(conn, actor_id, kind, "review open", args.target, args.basis)
            cur = conn.execute(
                "INSERT INTO review_round(subject, basis_digest, reviewer, event_id) VALUES (?, ?, ?, ?)",
                (args.target, args.basis, actor_id, event_id))
            print(f"opened review round {cur.lastrowid} on {args.target}")
            return
        round_id = int(args.target)
        review = open_round(conn, round_id)
        if args.outcome == "passed":
            unverified = conn.execute(
                "SELECT id, severity, status FROM current_finding WHERE round_id = ?"
                " AND severity IN ('Blocker', 'Major') AND status <> 'verified' ORDER BY id",
                (round_id,)).fetchall()
            if unverified:
                listing = ", ".join(f"{r['id']} {r['severity']} {r['status']}" for r in unverified)
                raise SystemExit(f"Error: cannot pass round {round_id}; unverified Blocker/Major: {listing}")
            stale_query = (QUERIES / "stale_findings.sql").read_text(encoding="utf-8")
            stale = [r for r in conn.execute(stale_query).fetchall()
                     if conn.execute("SELECT 1 FROM current_finding WHERE id = ? AND round_id = ?"
                                     " AND severity IN ('Blocker', 'Major')", (r["id"], round_id)).fetchone()]
            if stale:
                raise SystemExit(f"Error: cannot pass round {round_id}; stale Blocker/Major: "
                                 + ", ".join(r["id"] for r in stale))
            evidence = conn.execute("SELECT 1 FROM evidence WHERE subject = ?",
                                    (f"review:{round_id}",)).fetchone()
            if evidence is None:
                raise SystemExit(f"Error: cannot pass round {round_id}; record the review check log first:"
                                 f" evidence record --subject review:{round_id} ...")
        event_id = record_event(conn, actor_id, kind, "review close", review["subject"], review["basis_digest"])
        conn.execute("INSERT INTO review_round_close(round_id, event_id, outcome) VALUES (?, ?, ?)",
                     (round_id, event_id, args.outcome))
        print(f"closed review round {round_id} ({args.outcome})")


def finding_target_rev(conn: sqlite3.Connection, target_kind: str, target_id: str) -> int:
    if target_kind in ("clause", "obligation", "binding"):
        view = {"clause": "current_clause", "obligation": "current_model_obligation",
                "binding": "current_binding"}[target_kind]
        row = conn.execute(f"SELECT rev FROM {view} WHERE id = ?", (target_id,)).fetchone()
        if row is None:
            raise SystemExit(f"Error: no current {target_kind} {target_id}")
        return int(row["rev"])
    if target_kind == "model":
        row = conn.execute("SELECT MAX(id) AS rev FROM event WHERE command = 'scan-models' AND subject = ?",
                           (target_id,)).fetchone()
        if row is None or row["rev"] is None:
            raise SystemExit(f"Error: module {target_id} has no model scan; run scan-models first")
        return int(row["rev"])
    if target_kind == "artifact":
        if not (ROOT.parent / target_id).exists():
            raise SystemExit(f"Error: artifact {target_id} does not exist in the repository")
        return 0
    raise SystemExit("Error: target kind must be clause, obligation, binding, model or artifact")


# Allowed finding transitions: new status -> statuses it may follow.
FINDING_TRANSITIONS = {
    "addressed": ("open", "reopened"),
    "rejected": ("open", "reopened"),
    "deferred": ("open", "reopened"),
    "verified": ("addressed", "rejected", "deferred"),
    "reopened": ("addressed", "rejected", "deferred", "verified"),
}
FINDING_VERBS = {"address": "addressed", "reject": "rejected", "defer": "deferred",
                 "verify": "verified", "reopen": "reopened"}


def cmd_finding(args: argparse.Namespace) -> None:
    actor_id, kind = parse_actor(args.actor)
    with write_tx(args.db) as conn:
        if args.finding_action == "add":
            if args.severity not in ("Blocker", "Major", "Minor", "Nit"):
                raise SystemExit("Error: severity must be Blocker, Major, Minor or Nit")
            if not args.summary:
                raise SystemExit("Error: finding add requires --summary")
            if args.round is None:
                raise SystemExit("Error: finding add requires --round")
            open_round(conn, args.round)
            target_rev = finding_target_rev(conn, args.target_kind, args.target_id)
            finding_id = args.id
            if finding_id is None:
                count = conn.execute("SELECT COUNT(DISTINCT id) FROM finding WHERE round_id = ?",
                                     (args.round,)).fetchone()[0]
                finding_id = f"R{args.round}-{count + 1:03d}"
            if latest_revision(conn, "finding", finding_id) is not None:
                raise SystemExit(f"Error: finding {finding_id} already exists")
            event_id = record_event(conn, actor_id, kind, "finding add", finding_id, git_head())
            conn.execute(
                "INSERT INTO finding(id, rev, round_id, severity, category, target_kind, target_id,"
                " target_rev, status, actor, event_id, summary)"
                " VALUES (?, 1, ?, ?, ?, ?, ?, ?, 'open', ?, ?, ?)",
                (finding_id, args.round, args.severity, args.category, args.target_kind, args.target_id,
                 target_rev, actor_id, event_id, args.summary))
            print(f"added finding {finding_id} ({args.severity}, {args.target_kind} {args.target_id}"
                  f" rev {target_rev})")
            return
        status = FINDING_VERBS[args.finding_action]
        current = latest_revision(conn, "finding", args.id)
        if current is None:
            raise SystemExit(f"Error: no finding {args.id}")
        if current["status"] not in FINDING_TRANSITIONS[status]:
            raise SystemExit(f"Error: finding {args.id} is {current['status']}; cannot mark it {status}")
        if status != "verified" and not args.reason:
            raise SystemExit(f"Error: finding {args.finding_action} requires --reason")
        event_id = record_event(conn, actor_id, kind, f"finding {args.finding_action}", args.id, git_head())
        conn.execute(
            "INSERT INTO finding(id, rev, round_id, severity, category, target_kind, target_id, target_rev,"
            " verifies_rev, status, reason, actor, event_id, summary)"
            " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            (args.id, current["rev"] + 1, current["round_id"], current["severity"], current["category"],
             current["target_kind"], current["target_id"], current["target_rev"],
             current["rev"] if status == "verified" else None, status, args.reason, actor_id, event_id,
             current["summary"]))
        print(f"finding {args.id} is {status}")


# --------------------------------------------------------------------------- selftest

def split_statements(sql_text: str) -> list[str]:
    statements: list[str] = []
    chunk = ""
    for char in sql_text:
        chunk += char
        if char == ";" and sqlite3.complete_statement(chunk):
            statements.append(chunk.strip())
            chunk = ""
    if chunk.strip():
        statements.append(chunk.strip())
    return statements


def strip_comment_lines(text: str) -> str:
    return "\n".join(line for line in text.splitlines() if not line.lstrip().startswith("--"))


def run_fixture(fixture: Path) -> tuple[str, str]:
    """Return (status, detail) with status in PASS, FAIL, PENDING."""
    text = fixture.read_text(encoding="utf-8")
    expect_abort = re.search(r"^-- expect-abort:\s*(.+)$", text, re.MULTILINE)
    expect_query = re.search(r"^-- expect-query:\s*(\w+)\s+(\d+)\s+row", text, re.MULTILINE)
    expect_pending = re.search(r"^-- expect-pending:\s*(.+)$", text, re.MULTILINE)
    if expect_pending and not (expect_abort or expect_query):
        return "PENDING", expect_pending.group(1).strip()
    if not (expect_abort or expect_query):
        return "FAIL", "fixture declares no expectation"

    conn = sqlite3.connect(":memory:")
    conn.execute("PRAGMA foreign_keys=ON")
    try:
        conn.executescript(SCHEMA.read_text(encoding="utf-8"))
        statements = [stripped for s in split_statements(text)
                      if (stripped := strip_comment_lines(s).strip())]
        if expect_abort is not None:
            keyword = expect_abort.group(1).strip().lower()
            if len(statements) < 2:
                return "FAIL", "abort fixture needs a seed and a final statement"
            try:
                for statement in statements[:-1]:
                    conn.execute(statement)
                conn.execute(statements[-1])
            except sqlite3.Error as exc:
                detail = str(exc).lower()
                if keyword in detail:
                    return "PASS", f"aborted as expected: {exc}"
                return "FAIL", f"aborted with unexpected message: {exc}"
            return "FAIL", "final statement did not abort"
        assert expect_query is not None
        query_name, expected_rows = expect_query.group(1), int(expect_query.group(2))
        for statement in statements:
            conn.execute(statement)
        query_file = QUERIES / f"{query_name}.sql"
        rows = conn.execute(query_file.read_text(encoding="utf-8")).fetchall()
        if len(rows) == expected_rows:
            return "PASS", f"{query_name} returned {expected_rows} row(s)"
        return "FAIL", f"{query_name} returned {len(rows)} row(s), expected {expected_rows}"
    finally:
        conn.close()


def cmd_selftest(_args: argparse.Namespace) -> None:
    if not FIXTURES.is_dir() or not list(FIXTURES.glob("*.sql")):
        raise SystemExit("Error: no fixtures found under tracker/tests/fixtures")
    counts = {"PASS": 0, "FAIL": 0, "PENDING": 0}
    for fixture in sorted(FIXTURES.glob("*.sql")):
        status, detail = run_fixture(fixture)
        counts[status] += 1
        print(f"{status:7} {fixture.name}: {detail}")
    print(f"selftest: {counts['PASS']} pass, {counts['FAIL']} fail, {counts['PENDING']} pending")
    if counts["FAIL"]:
        raise SystemExit(1)


# --------------------------------------------------------------------------- CLI

def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--db", default=str(DEFAULT_DB))
    parser.add_argument("--readonly", action="store_true",
                        help="open the ledger read-only (mode=ro URI)")
    sub = parser.add_subparsers(dest="command", required=True)

    p_init = sub.add_parser("init")
    p_init.add_argument("--actor", default="setup:agent")
    p_init.set_defaults(func=cmd_init)

    p_rebuild = sub.add_parser("rebuild")
    p_rebuild.add_argument("--load", default=None, help="clauses.sql to load after schema")
    p_rebuild.set_defaults(func=cmd_rebuild)

    p_export = sub.add_parser("export-clauses")
    p_export.add_argument("--out", default=str(ROOT / "clauses" / "clauses.sql"))
    p_export.set_defaults(func=cmd_export_clauses)

    p_query = sub.add_parser("query")
    p_query.add_argument("name")
    p_query.add_argument("params", nargs="*", help="named parameters as k=v")
    p_query.set_defaults(func=cmd_query)

    p_claim = sub.add_parser("claim")
    p_claim.add_argument("claim_action", choices=["acquire", "release"])
    p_claim.add_argument("subject")
    p_claim.add_argument("--actor", required=True)
    p_claim.add_argument("--lease", default="2h")
    p_claim.add_argument("--token", default=None)
    p_claim.set_defaults(func=cmd_claim)

    p_evidence = sub.add_parser("evidence")
    p_evidence.add_argument("record", choices=["record"])
    p_evidence.add_argument("--kind", required=True)
    p_evidence.add_argument("--file", required=True)
    p_evidence.add_argument("--subject", required=True)
    p_evidence.add_argument("--result", default="pass")
    p_evidence.add_argument("--params", default=None)
    p_evidence.add_argument("--tool-version", default="unrecorded")
    p_evidence.add_argument("--actor", default="tracker:agent")
    p_evidence.set_defaults(func=cmd_evidence)

    p_scan = sub.add_parser("scan-models", help="record model declarations from quint parse")
    p_scan.add_argument("files", nargs="*", help=".qnt files (default: native-agent-docs/models/*.qnt)")
    p_scan.add_argument("--quint", default="quint")
    p_scan.add_argument("--actor", required=True)
    p_scan.set_defaults(func=cmd_scan_models)

    p_sources = sub.add_parser("scan-sources", help="record Phase 4 source fields")
    p_sources.add_argument("files", nargs="*", help="repository-relative listed sources (default: all)")
    p_sources.add_argument("--actor", required=True)
    p_sources.set_defaults(func=cmd_scan_sources)

    p_source_text = sub.add_parser("source-text", help="print current source fields as JSON lines")
    p_source_text.add_argument("files", nargs="*")
    p_source_text.set_defaults(func=cmd_source_text)

    p_clause = sub.add_parser("clause", help="add|revise|retire ID; spans are FILE#POINTER[@START-END]")
    p_clause.add_argument("clause_action", choices=["add", "revise", "retire"])
    p_clause.add_argument("id")
    p_clause.add_argument("--kind")
    p_clause.add_argument("--scope")
    p_clause.add_argument("--trigger")
    p_clause.add_argument("--outcome")
    p_clause.add_argument("--text")
    p_clause.add_argument("--span", action="append")
    p_clause.add_argument("--reason")
    p_clause.add_argument("--actor", required=True)
    p_clause.set_defaults(func=cmd_clause)

    p_span = sub.add_parser("span", help="exclude FILE#POINTER[@START-END] --reason R")
    p_span.add_argument("span_action", choices=["exclude", "gaps"])
    p_span.add_argument("span", nargs="?")
    p_span.add_argument("--reason")
    p_span.add_argument("--actor", required=True)
    p_span.set_defaults(func=cmd_span)

    p_apply = sub.add_parser("apply", help="apply clause/exclude JSON lines; applied lines are skipped")
    p_apply.add_argument("file")
    p_apply.add_argument("--actor", required=True)
    p_apply.set_defaults(func=cmd_apply)

    p_obligation = sub.add_parser("obligation")
    p_obligation.add_argument("obligation_action", choices=["add", "retire"])
    p_obligation.add_argument("id")
    p_obligation.add_argument("--clause")
    p_obligation.add_argument("--module")
    p_obligation.add_argument("--name")
    p_obligation.add_argument("--kind")
    p_obligation.add_argument("--reason")
    p_obligation.add_argument("--actor", required=True)
    p_obligation.set_defaults(func=cmd_obligation)

    p_review = sub.add_parser("review", help="open SUBJECT --basis DIGEST | close ROUND --outcome")
    p_review.add_argument("review_action", choices=["open", "close"])
    p_review.add_argument("target", help="subject for open, round id for close")
    p_review.add_argument("--basis")
    p_review.add_argument("--outcome", choices=["passed", "failed"], default="failed")
    p_review.add_argument("--actor", required=True)
    p_review.set_defaults(func=cmd_review)

    p_finding = sub.add_parser("finding")
    p_finding.add_argument("finding_action", choices=["add", *FINDING_VERBS])
    p_finding.add_argument("id", nargs="?", help="finding id (optional for add)")
    p_finding.add_argument("--round", type=int)
    p_finding.add_argument("--severity")
    p_finding.add_argument("--category", default="general")
    p_finding.add_argument("--target-kind", choices=["clause", "obligation", "binding", "model", "artifact"])
    p_finding.add_argument("--target-id")
    p_finding.add_argument("--summary")
    p_finding.add_argument("--reason")
    p_finding.add_argument("--actor", required=True)
    p_finding.set_defaults(func=cmd_finding)

    sub.add_parser("selftest").set_defaults(func=cmd_selftest)
    return parser


def main(argv: list[str] | None = None) -> int:
    args = build_parser().parse_args(argv)
    if args.command == "query" and args.readonly:
        args.func(args)
        return 0
    if args.readonly and args.command != "query":
        print("Error: --readonly applies to query only", file=sys.stderr)
        return 2
    args.func(args)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
