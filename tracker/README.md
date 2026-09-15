# Tracker — dev-only semantic-reads ledger

Local workflow state for the native-agent semantic-reads preparation plan
(`shiny-questing-crane`). **Not product state.** `tracker/ledger.sqlite*` is
gitignored; the tracked clause export in `tracker/clauses/clauses.sql` is the
only artifact other work consumes.

Run through the repository Python venv:

```bash
~/depot/pyVirtualEnvs/py3.12/bin/python tracker/tracker.py init --actor you:human
~/depot/pyVirtualEnvs/py3.12/bin/python tracker/tracker.py selftest
~/depot/pyVirtualEnvs/py3.12/bin/python tracker/tracker.py --readonly query open_findings
```

Python 3 standard library only. Every connection sets `PRAGMA foreign_keys=ON`.

## Commands

| Command | Status | Notes |
|---|---|---|
| `init [--actor ID[:KIND]]` | implemented | applies schema + migrations, registers the actor |
| `rebuild [--load clauses.sql]` | implemented | fresh database; verifies `PRAGMA foreign_key_check` after load |
| `export-clauses [--out FILE]` | implemented | actors first, then referenced events, then source/clause rows |
| `query NAME [k=v ...]` | implemented | named queries below; `--readonly` opens `mode=ro` |
| `claim acquire/release` | implemented | lease token required for release; double acquire aborts |
| `evidence record` | implemented | hashes the artifact; result/kind validated |
| `selftest` | implemented | runs every fixture; FAIL fails the run, PENDING is reported |
| `scan-sources`, `scan-models` | pending | Phase 2b / Phase 4 |
| `clause`, `span exclude`, `obligation`, `binding`, `review`, `finding` | pending | with their consuming phases |
| `export-roundtrip` | covered by `tests/test_tracker.py` | full-history export/rebuild comparison |

## Tables (14)

`actor`, `event`, `source_field`, `span_exclusion`, `clause`, `clause_source`,
`model_action`, `model_obligation`, `binding`, `evidence`, `review_round`,
`review_round_close`, `finding`, `claim_event`.

Append-only (`UPDATE`/`DELETE` abort): `event`, `evidence`, `review_round`,
`review_round_close`, `claim_event`, plus the observation tables `source_field`,
`clause_source`, `span_exclusion`, `model_action`. Revision tables (key `(id, rev)`,
gap-free by trigger): `clause`, `model_obligation`, `binding`, `finding`.

## Views (8)

`current_clause`, `current_model_obligation`, `current_binding`, `current_finding`,
`current_model_action`, `current_source_field`, `active_claims`, `open_rounds`.

## Queries (`tracker/queries/`)

| Query | Parameters | Output | Zero rows mean |
|---|---|---|---|
| `uncovered_source_spans` | — | file, pointer, start_offset, end_offset | every current source span is clause-covered or explicitly excluded |
| `duplicate_clauses` | — | id_a, id_b, text_sha256 | no two current clauses share text |
| `conflicting_outcomes` | — | id_a, id_b, trigger, outcome_a, outcome_b | no trigger has two current outcomes |
| `interaction_without_obligation` | — | id | every interaction clause has an invariant/refinement obligation |
| `unbound_model_actions` | — | module, name | every current non-mutant action has a current binding (verdict evidence is command-layer) |
| `stale_findings` | — | id, target_kind, target_id, target_rev, status | no finding targets a superseded revision |
| `stale_clauses` | — | clause_id, clause_rev, file, pointer | every current clause cites the current source observation |
| `open_findings` | — | id, severity, category, target_kind, target_id, target_rev | nothing open (severity-ordered listing) |
| `evidence_for` | `subject=k` | id, kind, result, tool_version, params, artifact_sha256, basis_digest | listing |

## Fixtures (`tracker/tests/fixtures/`)

Each fixture declares its expectation in a leading comment; `selftest` enforces it.
`new_without_absence.sql` is PENDING until the `binding set` command layer lands
(verdict-specific evidence rule).

## Migrations

`schema.sql` is the canonical DDL. `migrations/001_init.sql` mirrors it at ledger
creation; later changes are new numbered files and never edit an old migration.
