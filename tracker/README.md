# Tracker — dev-only semantic-reads ledger

Local workflow state for the native-agent semantic-reads preparation plan
(`native-agent-docs/plans/semantic-reads-preparation.md`). **Not product state.** `tracker/ledger.sqlite*` is
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
| `scan-models [FILE ...] --actor A` | implemented | `quint parse` per module (default `native-agent-docs/models/*.qnt`); records every `action`, `var` and `val` with parameters and `file:line` locator; one event per changed module digest, unchanged digests are skipped |
| `obligation add ID --clause C --module M --name N --kind K` / `retire ID --reason R` | implemented | needs a current clause and a declaration in the current model scan; used from Phase 4 |
| `review open SUBJECT --basis DIGEST` / `close ROUND --outcome passed\|failed` | implemented | one open round per subject; `passed` needs every Blocker/Major in the round `verified`, none stale, and evidence with subject `review:ROUND` |
| `finding add --round R --severity S --target-kind K --target-id T --summary TEXT [--category C] [ID]` | implemented | stores the target's current revision; ID defaults to `R<round>-<nnn>` |
| `finding address\|reject\|defer\|reopen ID --reason R`, `finding verify ID` | implemented | transitions are checked; `verify` must come from an actor other than the disposer |
| `scan-sources [FILE ...] --actor A` | implemented | records one `source_field` per listed field (plan Phase 4 source list); unchanged file digests are skipped; a missing listed field fails the scan |
| `source-text [FILE ...]` | implemented | prints current fields as JSON lines (`span`, `length`, `text`) for clause authoring |
| `clause add\|revise ID --kind K --scope S --text T --span FILE#POINTER[@START-END] [--trigger] [--outcome]`, `clause retire ID --reason R` | implemented | spans are checked against the current source field; revise keeps unspecified fields and spans |
| `span exclude FILE#POINTER[@START-END] --reason R`, `span gaps` | implemented | `gaps` excludes uncovered segments that hold only whitespace, punctuation or a list label |
| `apply FILE.jsonl --actor A` | implemented | runs `clause add` / `span exclude` for each line; a span can be `{"at": FILE#POINTER, "quote": TEXT}` (unique occurrence); applied lines are skipped on a rerun |
| `binding set ID --module M --action A --verdict V ...` / `retire ID --reason R` | implemented | active model actions require passing symbol evidence; NEW/DELETE require passing absence evidence; revisions are append-only |
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
| `unbound_model_actions` | — | module, name | every current action except `mut_*` and `step_mut_*` has a current binding (verdict evidence is command-layer) |
| `stale_findings` | — | id, target_kind, target_id, target_rev, status | no finding targets a superseded revision or an older model scan |
| `stale_clauses` | — | clause_id, clause_rev, file, pointer | every current clause cites the current source observation |
| `open_findings` | — | id, severity, category, target_kind, target_id, target_rev, status, summary | nothing open or reopened (severity-ordered listing) |
| `evidence_for` | `subject=k` | id, kind, result, tool_version, params, artifact_sha256, basis_digest | listing |

## Fixtures (`tracker/tests/fixtures/`)

Each fixture declares its expectation in a leading comment; `selftest` enforces it.
`new_without_absence.sql` is PENDING until the `binding set` command layer lands
(verdict-specific evidence rule).

## Finding targets

| `target_kind` | `target_id` | `target_rev` |
|---|---|---|
| `clause`, `obligation`, `binding` | row id | current `rev` |
| `model` | module name | id of the latest `scan-models` event for the module |
| `artifact` | repository path | `0`; the round `basis_digest` pins the content |

## Migrations

`schema.sql` is the canonical latest DDL; a new ledger is created from it. `PRAGMA user_version` holds the number of the last applied migration, and `init` applies every later file in `migrations/` to an existing ledger. Never edit an applied migration; add a new numbered file and update `schema.sql` to match. `002_review_targets.sql` adds the `model` and `artifact` finding targets, `finding.summary`, `review_round_close.outcome` and `model_action.locator`, and makes `current_model_action` select the latest scan of each module.
