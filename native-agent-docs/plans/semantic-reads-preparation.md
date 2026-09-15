# Interaction models, minimal tracker and clause extraction for native-agent work

Location: this file is the shared copy for all agents (moved from the Claude-local plan `shiny-questing-crane` on 2026-09-15). The Phase 2a model tables describe commit `76f0c9ece`; for later model changes, the files in `native-agent-docs/models/` and their `README.md` are the current truth.

Revision 5.
- **Revision 2 addressed:** round-1 review findings.
- **Revision 4 addressed:** Codex review rounds 2 and 3; dispositions are reproduced below.
- **Repository basis:** `53ddc2d42759316a4cbac62fef5dbf4a0436f9dc`.

## Context

JSON and Markdown review files are passed between models, and reviews repeat because ownership and dependencies exist only as prose. The last semantic-reads DAG review found 5 Blockers and 8 Majors. Most were relational errors that a machine can check.

This plan produces four inputs for a later task breakdown:

1. Layered Quint models of interactions between concurrent owners, checked against existing Java code.
2. A minimal dev-only SQLite ledger for clauses, model obligations, bindings, evidence, reviews and claims.
3. Behavior clauses extracted from the current specs, exported as tracked text.
4. A binding inventory from each model action to existing and Pi reference symbols.

**This plan does not derive implementation tasks.** Task breakdown is a separate follow-up plan.

## Decisions (owner, 2026-09-15)

| Topic | Decision |
|---|---|
| Ledger | Dev-only workflow state, not product state. `tracker/ledger.sqlite*` is gitignored and local-only. |
| Tracked tracker files | `tracker/` at repo root: schema, migration, commands, queries, tests, README, and `tracker/clauses/clauses.sql` (clause export) |
| Tool | `tracker/tracker.py`, Python 3 standard library only |
| Quint | `@informalsystems/quint@0.32.0` (installed). `verify` uses `--backend=tlc` (owner decision 2026-09-15, replaces Apalache 0.56.1; see Phase 2a execution amendments). `run` uses the `rust` backend (default). |
| Model structure | Layered: a coarse composed agent model plus a detailed read module that refines the abstract tool effect |
| Quint scope rule | Model state that two or more independent owners can interleave on. Local value logic gets unit tests and PBT. |
| Test tools | Test-only. jqwik 1.10.1 needs JUnit Platform 1.14.4, so `junitVersion` 5.11.4 → 5.14.4 (only `plugin-core/build.gradle.kts:29` uses it). PIT: plugin `info.solidsoft.pitest` 1.19.0, PIT 1.30.0, `pitest-junit5-plugin` 1.2.3. Kill-rate threshold 80% per task. Survivor triage is scripted. |
| Old specs | Not deleted in this plan |
| Task breakdown | Separate follow-up plan `semantic-reads-breakdown`, which consumes this plan's outputs. Owner decision 2026-09-15. This plan is preparatory by owner choice, not by reduction. |
| Review basis | This Decisions table is owner authority. Reviews check that the plan executes these decisions and do not reopen them. |

## Finding dispositions

Round 1 (SQCR-PR):

| Finding | Disposition | Where |
|---|---|---|
| PR-001 | Keys, foreign keys, revision tables listed, transactions, domain commands only | Phase 3 |
| PR-002 | Dev-only classification. All conflicting `workflow.md` clauses amended. | Phase 1 |
| PR-003 | Pinned version, runnable commands, layered modules, mutant import form, witness per invariant, refinement module, trace replay | Phases 2, 6 |
| PR-004 | No deletion. Full-history clause export with rebuild round-trip. | Phases 3, 4 |
| PR-005 | Outcome renamed. Breakdown is a separate plan. | Context |
| PR-006 | Evidence per acceptance kind. `target_rev` on findings for staleness. | Phase 3, Deferred |
| PR-007 | Scanned source fields, span exclusions, trigger and outcome columns, duplicate and conflict queries | Phases 3, 4 |
| PR-008 | Review rounds, findings with second-actor verify, claims as event rows | Phase 3 |
| PR-009 | Model authoring first. Only tables with a consumer in this plan. | Order, Phase 3 |
| PR-010 | `model_action` table scanned from `quint parse` (rev 3), then bindings | Phases 2b, 5 |

Revision 4 findings:

| Finding | Disposition |
|---|---|
| R4-001 transition relation | Revision 5 adds closed finite domains, initial state, guards/effects, owner assertions, refinement projection and concrete mutants. |
| R4-002 source rescan key | Revision 5 makes source observations revisioned by source digest/event and adds current-source and stale-clause queries. |
| R4-003 product source coverage | Revision 5 scans I2-I4, retry bounds and every scope/non-goal/assumption/type-safety/stage field used downstream. |
| R4-004 binding completeness | Revision 5 derives a complete action census from the checked model digest and requires evidence-backed NEW/DELETE decisions. |
| R4-005 stale clauses | Revision 5 adds `stale_clauses`, its gate and changed-source fixture. |
| R4-006 replay mapping | Revision 5 adds a total action-to-Java mapping and a blocking harness for synchronous `CallAdmission.execute`. |
| R4-007 plan targets | Revision 5 removes `plan` as a polymorphic finding target; plan reviews use immutable basis digests. |
| R4-008 independent review | Revision 5 adds model/tracker and clause/binding independent review gates before conformance. |
| R4-009 omitted authority fields | Revision 5 scans scope, non-goals, assumptions, type-safety audit and stage bindings. |
| R4-010 unavailable citation | Revision 5 removes the unavailable scratchpad citation. |

Revision 2 findings:

| Finding | Disposition | Where |
|---|---|---|
| R2-001 task output deferred | Rejected as a defect. The owner chose a separate breakdown plan. The follow-up plan ID and decision are recorded (reviewer option b). | Decisions |
| R2-002 stale basis and verify identity | `target_rev` plus `verifies_rev`, explicit target revision rows and second-actor trigger | Phase 3 |
| R2-003 action inventory | `model_action` scanned from the exact checked model digest | Phases 2b, 3 |
| R2-004 order contradiction | One order with parallel 2a/3, 2b recording and a merge gate | Order |
| R2-005 model semantics | Bounds, closed action signatures, guard/effect table and refinement relation | Phase 2a |
| R2-006 export fidelity | Full-history export and row-level rebuild comparison including revisions, exclusions and source observations | Phases 3, 4 |
| R2-007 ITF parser and oracle | Strict ITF grammar, total replay mapping and per-step oracle; read traces are not claimed as conformance | Phase 6 |
| R2-008 jqwik/PIT overlap | Proof matrix retains the owner decision but ties each mechanism to one residual; PIT is a task-local gate, not a global closure condition. | Phase 6 |


## Order and preconditions

1. Phase 1 (authority) comes first.
2. Phase 2a (model authoring and checks) and Phase 3 (tracker) run in parallel. The Phase 3 agent owns `tracker/`; the Phase 2a agent owns `native-agent-docs/models/`; these file sets do not overlap. No claim command is required before the claim table exists; each agent records its claim as the first Phase 3/2a tracker operation after migration.
3. Phase 2b records the Phase 2a results into the tracker after Phase 3.
4. **Merge gate before Phase 2c and Phase 4:**
   - `tracker.py selftest` passes.
   - Every Phase 2a check has an `evidence` row whose `basis_digest` equals the current sha256 of its `.qnt` file.
   - `model_action` is current for those exact digests.
5. Phase 2c independently reviews the model, tracker schema/guards and model evidence. It must pass before Phase 4.
6. Phase 4 (clauses), then Phase 5 (bindings).
7. Phase 5b independently reviews the exported clause authority and complete binding inventory. It must pass before Phase 6.
8. **Precondition for Phases 5, 5b and 6:** the other session's uncommitted edits under `plugin-core/.../nativeagent/run/` and `native-agent-docs/domain-run-driver/design.md` are committed. Record the commit ID as evidence of kind `precondition`.
9. A replay failure in Phase 6 becomes a finding that reopens the milestone 0/1 gate. This plan changes no production code; Phase 6 changes only test/build support and conformance tests.

## Phase 1 — Workflow authority (strong model)

Edit `native-agent-docs/workflow.md`:

| Lines | Change |
|---|---|
| `:60-70` Artifacts | Add "Tracker". `tracker/clauses/clauses.sql` is the tracked clause authority for new work. The ledger holds dev-only history, never product state. Frozen milestone 0/1 `spec.json`, `design.md` and `review.md` stay authoritative for their slices. |
| `:196` | Allow a local dev-only ledger. Keep "No workflow server, queue or orchestration service". |
| `:200` | Replace "Adopt at most one, only for a concrete question…" with: Quint only, for interactions between independent owners, as layered models. |
| `:202` | Keep the bounds, backend, reachability and broken-rule requirements. Platform behavior (PSI, dumb mode, write actions) is allowed only as abstract environment actions. Models are tracked evidence whose conformance is checked by trace-replay tests. They are not normative spec text, so "archive the model" and "never keep a permanent normative twin" do not apply to them. |
| `:206` | Add: a tracker binding row records a declaration found through IntelliJ at binding time. It is evidence, not a maintained declaration index. |
| `:210` | Slice state stays in `review.md` for frozen slices. New work records review state in tracker review tables. |
| `:208-221` | Read slices become "clause source until the breakdown plan qualifies". |
| `:248` | Remove "event store" and "graph database" for dev tooling only. |

Edit `native-agent-docs/product.md:60` FORMAL_TOOL_DEFAULT to: "Quint for interactions between independent owners (layered models, `workflow.md` Formal tools); none for local value logic."

Acceptance:
- `git diff --stat -- native-agent-docs/product.md native-agent-docs/workflow.md` lists only these two files.
- `git diff -U0 -- native-agent-docs/product.md native-agent-docs/workflow.md` shows hunks only inside the line ranges above.

## Phase 2a — Layered Quint models (strong model)

Files in `native-agent-docs/models/`:

| Module | Content |
|---|---|
| `agent_interface.qnt` | ToolEffect interface views and the pure relation `legalEffectTransition`. |
| `agent_coarse.qnt` | One `RunSession` with its `RunLifecycle`, the `NativeRunDriver` loop, the provider and tool effects. Derived from `RunSession.java`, `RunLifecycle.java`, `CallAdmission.java` and `NativeRunDriver.kt`. |
| `read_execution.qnt` | One read lifecycle: smart wait with bound, write-action restart, dumb mode, document edit and commit, compute, abort, register, registry disposal, settle. |
| `read_refinement.qnt` | Imports `read_execution` and `agent_interface`. History variables `prevView` and `view` project read state onto ToolEffect views. |

Each base module also defines one mutant step per invariant (`step_mut_<INV>`). There are no separate mutant modules: Quint 0.32.0 fails to flatten `import ... as B` names for TLC and Apalache (`QNT404 Name 'B::BATCHES' not found`), so an importing mutant module crashes and a crash is not a counterexample.

### Coarse model encoding

`agent_coarse` holds its state in one record variable `s`. The variable `prev` holds the pre-state of the last step, and every action, mutants included, sets `prev' = s`. Invariants C2, C5 and C6 are transition properties over `prev` and `s`. A batch has at most three calls, held in three fixed slots with plain fields.

Owners (Java):

| State | Owner |
|---|---|
| `phase`, `active`, `history`, `provisional`, `nextIndex`, `cursor`, per-slot `results` | `RunSession` |
| `lc`, per-slot `status`, `acceptedIds` | `RunLifecycle` |
| `cancelled` | `RunCancellation` |
| `req`, `retried`, `turn`, `turnId0`, `turnId1`, `turns` | `NativeRunDriver` and provider |
| `inEffect`, per-slot `starts`, `startedIds`, `outcome` | `CallAdmission` and ToolEffect |
| `generation`, `lastRequest`, `requestsInGen` | `CacheGeneration` |
| registry, PSI modification count, document state, wait clock, dumb mode, restarts | `read_execution` |

Finite bounds (Quint constants, recorded in evidence `params`):

| Constant | Value |
|---|---|
| `MAX_TURNS` | 2 accepted provider responses per run (`RunLimits`) |
| `MAX_CALLS` | 2 calls per batch (`BATCHES`: `(1,0)`, `(2,0)`, `(1,2)`, `(2,1)`) |
| Retries | 1 per request (`retried`, `RetryPolicy`) |
| `MAX_RUNS` | 2 |
| `MAX_GENERATIONS` | 2 |
| `INDEX_BOUND` | 2 abstract ticks |
| `MAX_RESTARTS` | 2 write-action restarts per read |
| `MAX_EDITS` | 2 document edits per read |
| `MAX_DUMB` | 2 dumb-mode entries per read |

Coarse actions:

| Action | Owner | Guard | Effect |
|---|---|---|---|
| `send` | User | phase `IDLE`, lc `IDLE`, runs below `MAX_RUNS` | new run: `REQUESTING`, lc `RUNNING`, user item appended; per-run state cleared |
| `beginRequest` | Driver | `REQUESTING`, not cancelled, no request or held turn, turns below `MAX_TURNS` | request in flight; `lastRequest` = history; `requestsInGen + 1` |
| `limitStop` | Driver | as `beginRequest`, but turns at `MAX_TURNS` | stop (budget not admitted) |
| `providerAttempt(outcome, ids)` | Provider | request in flight | after stop: request ends. `TERMINAL_TEXT` or `TERMINAL_CALLS`: turn held. First `RETRYABLE`: retry. Second `RETRYABLE` or `FAILED`: stop. |
| `deliverProvisional` | Provider | `REQUESTING`, request in flight, not disposed | provisional text delivered |
| `acceptTurn` | Driver | a turn is held | outside `REQUESTING`: turn dropped. Text: assistant item, finish. Calls with an ID already accepted this run: assistant item, stop. Other calls: batch begun, `EXECUTING_TOOLS`. |
| `nextCall` | Driver | `EXECUTING_TOOLS`, no cursor, no effect | cursor = next call; with no next call, `REQUESTING` (the tool loop) |
| `admit(result)` | Driver | cursor set, not yet admitted, no effect | lc not `RUNNING`: rejected, no effect. `LIMITED`: call `COMPLETED`, no effect. `EXECUTED`: call `EXECUTING`, one effect start. |
| `effectFinish(ok)` | ToolEffect | an effect runs | call `COMPLETED` or `FAILED_AFTER_START` |
| `recordResult` | Driver | admitted cursor call settled | result appended. In `STOPPING`: drain. After `LIMITED` or `FAILED_AFTER_START`: stop. |
| `stop` | User or driver | active; `REQUESTING` or `EXECUTING_TOOLS`, or `STOPPING` with a cancelled result to drain | cancel; lc `RUNNING → STOPPING`; pending calls cancelled; cancelled results drained in order |
| `close` | Content | not disposed | no active run: `DISPOSED`. Active run: `STOPPING`, lc `CLOSING`, pending cancelled, provisional cleared, no drain. |
| `finish` | Driver | `STOPPING`, driver idle, batch settled | `IDLE`, or `DISPOSED` after close |
| `newGeneration` | CacheGeneration | `IDLE`, generation below `MAX_GENERATIONS` | generation `+1`; request prefix cleared |

`stepCorpus` is a scheduler for the replay corpus only. It is a subset of `step`: `close` is enabled only during a tool batch or in the last run. Under `step`, `close` is enabled in almost every state and `DISPOSED` is final, so simulator traces end early.

Read actions (`read_execution`): `startRead`, `refuseRead`, `smartWaitTick`, `dumbEnter`, `dumbExit`, `editDocument`, `commitDocuments`, `writeAction`, `compute`, `abortComputation`, `register`, `disposeRegistry`, `cancel`, `settle`. Computation states: `IDLE`, `RUNNING`, `DONE`, `ABORTED`, `TIMED_OUT`. `smartWaitTick` at the bound in dumb mode sets `TIMED_OUT` (INDEX_NOT_READY). `abortComputation` ends a cancelled or disposed computation. `settle` accepts `DONE`, `ABORTED` or `TIMED_OUT`. The projection maps `TIMED_OUT` and a result to `completed`, and `ABORTED` to `failed_after_start`.

Product invariant coverage (every invariant is assigned, so omissions are visible):

| Product | Mechanism |
|---|---|
| I1 one active run, no IntelliJ types in core | INV-C3, LEM-PHASE. Types: existing `DomainArchitectureTest.java`. |
| I2 session state, no mid-run change | Out of this plan: model and catalog change are idle-only UI actions (milestone 5) |
| I3 no replay | INV-C4 |
| I4 one result per call | INV-C1 |
| I5 identity from resolution | Local: breakdown plan tests (not multi-owner) |
| I6 mutation revalidation | Out of scope: milestone 3A |
| I7 no EDT wait under read | INV-R2 plus Phase 6 thread assertions deferred to the read kernel task |
| I8 Stop admission | INV-C2, INV-C3 |
| I9 tracked completion | INV-C3, INV-R2 |
| I10 no content in logs | Local: breakdown plan tests |
| I11 owned lifetimes | INV-C6, INV-R1 |
| I12 bounds | Local value bounds: breakdown plan tests. Run limits: `limitStop`, LEM-BOUNDS. |
| I13 prefix | INV-C5 |

Invariants, each with a mutant and a witness:

| ID | Statement (model level) | Mutant action | Witness |
|---|---|---|---|
| INV-C1 | Each call has at most one result. While a request is in flight, every call of the previous batch has exactly one result. | `mut_completeEarly`: `nextCall` returns Complete with unrecorded results | W-SUCCESS |
| INV-C2 | If the lifecycle was not `RUNNING` before a step in the same run, the step starts no effect | `mut_admitAfterStop`: admission ignores the lifecycle phase | W-STOP-WAIT |
| INV-C3 | `IDLE` or `DISPOSED` implies no active run and a settled batch | `mut_finishDuringEffect` | W-STOP-WAIT |
| INV-C4 | No effect starts twice, and a started call never becomes `PENDING` again | `mut_acceptReusedIds`: batch begun without the accepted-call-ID check | W-ID-REUSE |
| INV-C5 | Within one generation, request N is a prefix of request N+1 | `mut_compactHistory`: history rewritten without a new generation | W-RETRY |
| INV-C6 | Disposed content has no provisional delivery, and a `DISPOSED` session's history does not change | `mut_provisionalAfterDispose` | W-CLOSE |
| INV-R1 | No registration after cancel or dispose | `mut_registerIgnoringCancel` | W-STOP-WAIT |
| INV-R2 | Effect settles only after the computation terminates (`DONE`, `ABORTED` or `TIMED_OUT`) | `mut_settleEarly` | W-RESTART |
| INV-R3 | Refused admission makes no platform access | `mut_accessAfterRefusal` | W-REFUSED |
| INV-R4 | Wait clock within the bound; INDEX_NOT_READY only at the bound, as the terminal `TIMED_OUT` outcome, with no result | `mut_returnPastBound` | W-INDEX-TIMEOUT |
| INV-R5 | No result from uncommitted documents | `mut_computeUncommitted` | W-RESTART |
| REF-R | `legalEffectTransition(prevView, view)` in every state of `read_refinement` | `mut_unsettleAndProject` | W-SUCCESS |

Consistency lemmas (`agent_coarse`, checked like invariants, no mutant or witness): LEM-EFFECT (the running effect matches the only `EXECUTING` slot), LEM-CURSOR (an admitted cursor is at `nextIndex`), LEM-PHASE (session phase, lifecycle phase and active run agree), LEM-BOUNDS.

Witnesses are `val` predicates. Each is checked as `--invariant="not(W)"` and must be violated:

| Witness | Reachable state |
|---|---|
| W-SUCCESS (`agent_coarse`) | A tool turn completes with one recorded result, then a text turn ends the run |
| W-SUCCESS (`read_refinement`) | A read completes |
| W-RESTART | A write-action restart, then the read settles with a result |
| W-STOP-WAIT (`agent_coarse`) | Stop during a running effect, then the effect fails after start |
| W-STOP-WAIT (`read_execution`) | Cancel after smart-wait ticks, then the read settles as `ABORTED` |
| W-RETRY | A retry, then a terminal response, on the second or later request of a generation |
| W-CLOSE | Close during a tool batch |
| W-ID-REUSE | A reused call ID is rejected and the run stops |
| W-REFUSED | Admission is refused |
| W-INDEX-TIMEOUT | Dumb mode outlasts the bound and gives INDEX_NOT_READY |

Mutated actions are named `mut_*` and mutant steps `step_mut_*`. Both are excluded from `unbound_model_actions`. Model identifiers replace `-` with `_` (INV-R1 → `INV_R1`). The Java mapping reads the same names.

Commands. `native-agent-docs/models/check.sh all` runs every check; it finds its targets in the model files. Each check writes raw output to `.agent-work/evidence/phase2/<name>.txt` and one row to `summary.tsv`. Phase 2b records these files.

```bash
M=native-agent-docs/models
quint typecheck $M/<module>.qnt
quint run    $M/<module>.qnt --main=<module> --invariant=<INV> --max-samples=10000 --max-steps=30 --seed=<recorded> --backend=rust
quint verify $M/<module>.qnt --main=<module> --invariant=<INV> --backend=tlc
quint verify $M/<module>.qnt --main=<module> --invariant="not(<W>)" --backend=tlc                       # expect violation
quint verify $M/<module>.qnt --main=<module> --step=step_mut_<INV> --invariant=<INV> --backend=tlc     # expect violation
quint run    $M/agent_coarse.qnt --main=agent_coarse --step=stepCorpus --max-samples=10000 --max-steps=30 --seed=<recorded> --backend=rust --mbt --out-itf=.agent-work/itf/coarse_{seq}.itf.json --n-traces=50   # replay corpus
```

Results:
- Backend: TLC. All models are finite, so TLC checks the complete reachable state space with no step bound. `agent_coarse` has 137,886 distinct states (depth 39), `read_execution` 7,883, `read_refinement` 14,328. Each check takes about 10 s.
- Apalache is not used. On `agent_coarse`, Apalache took 176 s at 7 steps and did not finish 12 steps in 10 minutes.
- A variable without a bound makes TLC run without end. Every counter needs a guard.
- A `pass` check needs exit 0 and `[ok]`. A witness or mutant check needs a reported violation. A crash also exits non-zero and is not a counterexample.
- Write `x' = (a or b)`, not `x' = a or b`. Quint compiles the second form as `(x' = a) or b` and reports a missing assignment.
- `run` covers only its samples. The seeded simulator missed three witnesses that TLC reaches.

## Phase 2b — Record model evidence (after Phase 3)

- `tracker.py scan-models` runs `quint parse` for each module, records the exact source sha256 and parser output, and fills event-keyed `model_action` observations with every `action`, `var` and `val` name plus parameters, owner module and source locator. A scan is idempotent only when the latest observation already has the same digest; a changed or reverted digest creates a new event-keyed observation and the previous observation is not current.
- `tracker.py evidence record` runs once for each file in `.agent-work/evidence/phase2/`.
- Obligations are not recorded here. `model_obligation.clause_id` needs a current clause, and clauses exist only after Phase 4, so `obligation add` runs in Phase 4 (owner decision 2026-09-15).

## Phase 2c — Independent model and tracker review

A fresh reviewer opens a tracker review round against the exact Phase 2a model digests and Phase 3 schema/tracker commit. The reviewer checks every model action, finite domain, guard/effect row, owner assertion, invariant, witness, mutant, parser result and tracker guard fixture. The reviewer records findings and dispositions through the tracker commands. A targeted gate requires:

- no open Blocker or Major;
- all model files, parser outputs, model actions and evidence rows use the same basis digests;
- `unbound_model_actions`, `interaction_without_obligation` and tracker self-tests pass;
- the model review explicitly confirms that model omission and Java conformance remain separate obligations.

Phase 4 cannot start without this receipt.
## Phase 3 — Minimal tracker (strong model)

Files:
- `tracker/schema.sql`, `tracker/migrations/001_init.sql`
- `tracker/tracker.py`
- `tracker/queries/*.sql`
- `tracker/tests/test_tracker.py`, `tracker/tests/fixtures/*.sql`
- `tracker/README.md` (table: query | parameters | output columns)
- `.gitignore`: add `tracker/ledger.sqlite*`

Table rules:
- **Append-only tables** (`UPDATE` and `DELETE` abort): `event`, `evidence`, `review_round`, `review_round_close`, `claim_event`.
- **Source observations** are append-only and keyed by `(file, pointer, source_event_id)`, so every scan transaction records a new observation even when a file reverts to an earlier digest. `current_source_field` selects the latest observation for each `(file, pointer)` by event sequence. A scan is idempotent only when the latest observation already has the same source digest; otherwise it appends a new event.
- **Revision tables** have key `(id, rev)`, and a trigger requires `rev = 1 + coalesce(max(rev), 0)` for that `id`. They are `clause`, `model_obligation`, `binding` and `finding`. Each has `status`, `reason` and `event_id` (FK to `event`). `UPDATE` and `DELETE` abort.
- `clause_source` is append-only and keyed `(clause_id, clause_rev, file, pointer, start_offset, source_event_id)`, with FKs to `clause(id, rev)` and `source_field(file, pointer, source_event_id)`. It stores the source digest as an immutable observation value.
- The view `current_<table>` selects the max `rev` per `id` where `status <> 'retired'`. A retired row needs a nonempty `reason` (CHECK).
- Uniqueness over current rows is enforced by a `BEFORE INSERT` trigger that queries the view, because SQLite partial indexes cannot express "current".
- Every command runs in `BEGIN IMMEDIATE` together with its checks. Revision and source observations are inserted in one transaction with their event.
- Actor registration is implicit: the first command carrying an `--actor ID[:KIND]` identity auto-registers the actor (id, kind, optional session_ref) in the same transaction before writing its event, and `init --actor` seeds the invoking identity the same way. No other path writes events for an unregistered actor.

DDL closure:
- Binding `java_symbol` and `pi_symbol` are nullable only for `NEW` or `DELETE`; all other verdicts require both applicable symbols. The command layer enforces this verdict-specific rule before insertion.
- Composite foreign keys are exact: `clause_source(clause_id, clause_rev)` references `clause(id, rev)` and `clause_source(file, pointer, source_event_id)` plus `span_exclusion(file, pointer, source_event_id)` reference `source_field(file, pointer, source_event_id)`; `model_obligation(module, name, model_event_id)` and `binding(module, model_action, model_event_id)` reference `model_action(module, name, event_id)`.
- `current_model_action` selects the latest observation for each `(module,name)` by event sequence. `unbound_model_actions` compares bindings only against that view and the exact model digest recorded by the Phase 2a evidence. A binding command or trigger rejects a `model_event_id` that is not the current observation for its `(module,name)`; the A→B→A model-action fixture exercises this rejection.
- `review_round.basis_digest` is immutable. `finding.target_rev` is mandatory for its clause, obligation or binding target, and command-layer target validation rejects a missing composite target before insertion.
- A review close is one append-only `review_round_close` row. A passed close requires the targeted review evidence and zero open Blocker/Major findings.
- A claim acquire/release carries a lease token; release requires the matching actor and token. Expired claims are reclaimable in one `BEGIN IMMEDIATE` transaction.

Tables (14):

| Table | Key | Columns |
|---|---|---|
| actor | id TEXT PK | kind CHECK IN (human, agent, reviewer, tool), model NULLABLE, session_ref NULLABLE |
| event | id INTEGER PK | at, actor FK actor(id), command, subject, basis_digest |
| source_field | (file, pointer, source_event_id) | start_offset, end_offset, source_sha256, source_event_id FK event(id) |
| span_exclusion | (file, pointer, start_offset, source_event_id) | end_offset, reason NOT NULL, source_sha256, source_event_id FK source_field(file, pointer, source_event_id), event_id FK event(id) |
| clause | (id, rev) | kind CHECK IN (pre, post, invariant, outcome, decision), scope CHECK IN (local, interaction), trigger, outcome, text, text_sha256, status, reason, event_id FK event(id) |
| clause_source | see rules | clause_id, clause_rev FK clause(id, rev), file, pointer, start_offset, end_offset, source_event_id FK source_field(file, pointer, source_event_id), source_sha256 |
| model_action | (module, name, event_id) | kind CHECK IN (action, var, val), parameters, owner, source_sha256, locator (file:line), event_id FK event(id) |
| model_obligation | (id, rev) | clause_id, clause_rev FK clause(id, rev), module, name, model_event_id FK model_action(module, name, event_id), kind CHECK IN (invariant, witness, mutant, refinement), status, reason, event_id FK event(id) |
| binding | (id, rev) | module, model_action, model_event_id FK model_action(module, name, event_id), java_symbol NULLABLE, pi_symbol NULLABLE, verdict CHECK IN (KEEP, ADAPT, PORT, REPLACE, DELETE, NEW), note CHECK(length(note) <= 280), status, reason, event_id FK event(id) |
| evidence | id PK | event_id FK event(id), subject, kind CHECK IN (quint_typecheck, quint_run, quint_verify, quint_witness, quint_mutant, quint_refinement, junit, jqwik, replay, pit, query, precondition), tool_version, params, result CHECK IN (pass, fail, counterexample, violation, timeout), artifact_sha256, basis_digest |
| review_round | id PK | subject, basis_digest, reviewer FK actor(id), event_id FK event(id) |
| review_round_close | round_id PK FK review_round(id) | event_id FK event(id), outcome CHECK IN (passed, failed) |
| finding | (id, rev) | round_id FK review_round(id), severity CHECK IN (Blocker, Major, Minor, Nit), category, target_kind CHECK IN (clause, obligation, binding, model, artifact), target_id, target_rev, verifies_rev, status CHECK IN (open, addressed, rejected, deferred, verified, reopened), reason, actor FK actor(id), event_id FK event(id), summary |
| claim_event | id PK | subject, actor FK actor(id), kind CHECK IN (acquire, release), lease_token, lease_until, event_id FK event(id) |

Views: `current_clause`, `current_model_obligation`, `current_binding`, `current_finding`, `current_model_action`, `current_source_field`, `active_claims`, `open_rounds`.

Triggers and derived rules:
- Only one open round (no `review_round_close` row) per subject.
- `finding` status `addressed`, `rejected` or `deferred` needs `reason`.
- A `verified` row carries `verifies_rev`. A trigger checks that the row `(id, verifies_rev)` exists, has status `addressed`, `rejected` or `deferred`, is the latest revision before this one, and has an actor different from the actor that created the verified target revision. The target kind is only `clause`, `obligation` or `binding`; plan reviews are identified by the immutable `review_round.basis_digest`.
- `active_claims` view: the latest `claim_event` per subject has kind `acquire` and `lease_until > now`. A second `acquire` on an active claim aborts; release is idempotent only for the owning actor and lease.
- A current clause row with a `text_sha256` that is already current aborts.
- `stale_clauses` compares every current `clause_source.source_sha256` with `current_source_field.source_sha256` for the same file/pointer. A changed or missing current observation is stale and blocks Phase 4 qualification until revised, retired or explicitly excluded with reason.
- `unbound_model_actions` requires one current binding for every non-mutant `model_action` action from the exact checked model digest. A `NEW` or `DELETE` binding requires a nonempty reason and source/Pi absence evidence; KEEP/ADAPT/PORT/REPLACE requires both applicable symbol evidence and a rationale. Binding to a historical model observation is invalid even when its foreign key exists.
- A phase review cannot close `PASSED` while `open_findings` or `stale_findings` contains a Blocker/Major.

Export and rebuild:
- Every connection (commands, `init`, `rebuild`) opens with `PRAGMA foreign_keys=ON`; no code path runs with foreign-key enforcement off.
- `export-clauses` writes `tracker/clauses/clauses.sql`. It holds the `actor` rows referenced by the exported events (first, before any `event` row), every `event` row referenced by `clause`, `clause_source`, `source_field` or `span_exclusion` rows, then those rows in (event_id, id, rev) order, as INSERTs with explicit values.
- `rebuild` creates an empty database, applies schema and migration, and loads `clauses.sql` in file order with foreign keys enforced. Triggers stay active, and full history in order satisfies the revision and foreign-key rules.
- Evidence, reviews and claims are not exported, because they are local-only.

Commands (no generic insert):

```text
tracker.py init | rebuild
tracker.py scan-sources                         # fills source_field from the Phase 4 source list
tracker.py scan-models                          # fills model_action from quint parse output
tracker.py clause add|revise|retire ...         # writes clause + clause_source rows
tracker.py span exclude FILE POINTER START END --reason R
tracker.py obligation add|retire ...
tracker.py binding set|retire ...
tracker.py evidence record --kind K --file ARTIFACT [--params P] [--result R]
tracker.py review open SUBJECT --basis DIGEST | close ROUND
tracker.py finding add|address|reject|defer|verify|reopen ...   # add stores target_rev
tracker.py claim acquire SUBJECT --lease 2h | release SUBJECT
tracker.py export-clauses
tracker.py query NAME [k=v ...]
tracker.py --readonly query ...                 # file:tracker/ledger.sqlite?mode=ro
tracker.py selftest
```

Queries (`tracker/queries/`):

| Query | Rows mean |
|---|---|
| `uncovered_source_spans` | Character ranges of `source_field` covered by neither `clause_source` nor `span_exclusion` (interval merge in SQL) |
| `duplicate_clauses` | Two current clauses with equal `text_sha256` (defense in depth) |
| `conflicting_outcomes` | Two current `outcome` clauses with equal `trigger` and different `outcome` |
| `interaction_without_obligation` | A current `interaction` clause with no current `invariant` or `refinement` obligation |
| `unbound_model_actions` | A current non-mutant model action with no current binding, or a binding with unsupported NEW/DELETE/KEEP evidence |
| `stale_findings` | An open finding whose target has a current `rev` greater than `target_rev` |
| `stale_clauses` | Current clauses whose source observation is changed or missing |
| `open_findings`, `evidence_for` | Listing |

Named fixtures (seed rows, then the listed result is expected):
| Fixture | Expected |
|---|---|
| `dup_clause.sql` | `clause add` aborts |
| `update_event.sql` | trigger ABORT |
| `gap_rev.sql` | revision trigger ABORT |
| `interaction_no_invariant.sql` | `interaction_without_obligation` returns 1 row |
| `uncovered_span.sql` | `uncovered_source_spans` returns 1 row |
| `conflict.sql` | `conflicting_outcomes` returns 1 row |
| `self_verify.sql` | `finding verify` by the disposer aborts |
| `double_claim.sql` | second `claim acquire` aborts |
| `stale_review.sql` | `stale_findings` returns 1 row |
| `model_action_rescan.sql` | scan model digest A, then B, then A again; each observation has a distinct event ID, `current_model_action` selects final A, and a binding targeting stale B is rejected |
| `retire_no_reason.sql` | CHECK aborts |
| `new_without_absence.sql` | `binding set ... NEW` aborts without absence evidence |
| `export_roundtrip` | The seed holds a clause with 3 revs (add, revise, retire with reason), a multi-span clause, a span exclusion, two source observations for one pointer and changed source hashes. Export/rebuild preserves all rows and IDs byte-for-byte. |

## Phase 4 — Clause extraction (strong model, tracker commands only)

Sources, scanned into `source_field`:

| Source | Fields |
|---|---|
| `native-agent-docs/product.md` | byte ranges of lines `:136-158` (I2-I13), `:179-189` (Reads and identity), and `:229` (retry bound) |
| `native-agent-docs/read-*/spec.json` | `/scope`, `/non_goals/*`, `/assumptions/*`, `/requirements/*/statement`, `/acceptance/*/when`, `/acceptance/*/then`, `/acceptance/*/target`, `/decisions/<key>` (each key is its own field), `/operation_matrix/*/*`, `/null_boundary_matrix/*/*`, `/type_safety_audit/*/invalid`, `/type_safety_audit/*/type_api_prevention`, `/type_safety_audit/*/residual_runtime_obligation`, `/type_safety_audit/*/justification`, `/stages/*/entry`, `/stages/*/exit`, `/stages/*/outcome`, `/evidence/*/description` |

The scan records a stable `source_field` observation for every listed field at the whole-file digest. No field is silently dropped; intentionally non-normative text receives a `span exclude` row with a reason.

Offsets are Unicode code point indices (Python `str` indices), start-inclusive and end-exclusive:
- **JSON:** offsets index the decoded string value (`json.loads`), so escapes are resolved. Keys and punctuation are never normative. Object/array container fields use one field per scalar leaf and a source locator for the parent.
- **Markdown:** offsets index the UTF-8-decoded text of the cited line range, with line terminators included.

`source_sha256` is the sha256 of the whole source file bytes. A changed file digest creates a new event-keyed source observation; reverting to a prior digest creates another observation and makes that digest current again. `stale_clauses` reports clauses tied to prior observations. The source scan fails if a listed field is missing or if a source locator points outside the decoded field.

Steps:
1. A statement with several behaviors becomes several clauses. One clause can cite several spans.
2. Scope, non-goals, assumptions, type-safety and stage-binding fields become clauses or explicit exclusions; their owner is never inferred from a missing row.
3. Text that is not normative (for example `REF:` decision values) gets `span exclude` with a reason.
4. **Gate:** `uncovered_source_spans`, `duplicate_clauses`, `interaction_without_obligation`, `conflicting_outcomes` after disposition and `stale_clauses` each satisfy their stated zero-row/review condition.
5. Run `tracker.py obligation add` for each invariant, witness, mutant and REF-R, linked to its clause and to the current model scan. Then `interaction_without_obligation` must return 0 rows.
6. Record each gate query result as evidence of kind `query`.
7. Run `export-clauses`, then the full-history round-trip check.
8. Delete nothing.

## Phase 5b — Independent clause and binding review

A fresh reviewer opens a review round against the exact Phase 4 export and Phase 5 binding evidence. The reviewer checks source-span completeness, decoded-offset rules, exclusions, duplicate/conflict dispositions, every current model action, every KEEP/ADAPT/PORT/REPLACE/DELETE/NEW rationale and every Java/Pi locator. The reviewer records findings and dispositions through tracker commands. Phase 6 cannot start while a Blocker/Major or stale clause remains.

## Phase 5 — Binding inventory (strong model)

For each current `model_action` row of kind `action` from the exact Phase 2a model digest:
- `scan-models` is the sole action census; no manually added action may satisfy completeness.
- The Java symbol comes from the IntelliJ index (`ide_find_definition` or `ide_find_symbol`), at the precondition commit. A missing Java symbol is explicit absence evidence, not an implicit NEW.
- The Pi reference symbol comes from a `pi-search` agent answer with `file:line`, or explicit absence evidence from the same query scope.
- KEEP, ADAPT, PORT and REPLACE require the applicable Java/Pi evidence and a rationale. NEW and DELETE require bounded absence evidence and a nonempty reason. No binding row with an unsupported verdict can satisfy `unbound_model_actions`.

Action mapping is total across the census: every User, Driver, Provider, ToolEffect, Content, CacheGeneration and read action has one binding row, including actions that remain NEW because they have no current symbol.

Initial expectations:
- Admission maps to `run/session/CallAdmission.execute` (KEEP).
- Cancellation maps to `run/resources/RunCancellation` (KEEP).
- Settlement maps to `lifecycle/Effect` (KEEP).
- Read-module actions have no Java symbol yet (NEW), with absence evidence.

Gate: `unbound_model_actions` returns 0 rows and Phase 5b passes.

## Phase 6 — Conformance (strong model writes the tests)

Build edits:

`gradle.properties`:

```properties
junitVersion=5.14.4
jqwikVersion=1.10.1
pitestVersion=1.30.0
pitestJunit5PluginVersion=1.2.3
```

`plugin-core/build.gradle.kts`:

```kotlin
plugins { id("info.solidsoft.pitest") version "1.19.0" }   // added to existing plugins block
testImplementation("net.jqwik:jqwik:${providers.gradleProperty("jqwikVersion").get()}")
pitest {
    pitestVersion.set(providers.gradleProperty("pitestVersion"))
    junit5PluginVersion.set(providers.gradleProperty("pitestJunit5PluginVersion"))
    outputFormats.set(setOf("XML", "HTML"))
    timestampedReports.set(false)
    fullMutationMatrix.set(true)
    targetClasses.set(providers.gradleProperty("pitTargets").map { it.split(',').toSet() })
}
```

Rules for the build edits:
- The build has no mutation threshold. The tracker applies it only to a named task and its named target classes after that task's proof matrix is reviewed; it is not a global Phase 6 gate.
- First check: run through the repository-required `mtk gradle :plugin-core:test` on JUnit 5.14.4 before any new test is added. On failure, stop and record a finding. Do not downgrade silently.

Model-to-Java action mapping is total. The model action is the row key; each row names the exact Java invocation, intermediate observability and oracle:

| Model action | Java invocation / harness | Observable oracle |
|---|---|---|
| `send` | `RunSession.start(user, limits, now, timeSource)`; the driver replay wraps the same session call | one fresh run; phase REQUESTING; user message appended |
| `beginRequest` | `RunSession.beginRequest(run, now)` returns `ADMITTED`; `cache.requestFor(history, renderer)`; fake `ProviderTransport` receives the request | request history equals `s.lastRequest`; request count in the generation |
| `limitStop` | `RunSession.beginRequest` returns a refusal; the driver calls `RunSession.stop(run)` | phase STOPPING; pending calls cancelled |
| `providerAttempt(outcome, ids)` | fake `ProviderTransport` returns `Terminal` (text or calls with the given IDs), `Retryable` or `Failed` to `NativeRunDriver.requestWithRetry` | held turn and IDs; retry state; STOPPING after a second retryable or a failure |
| `deliverProvisional` | `RunSession.tryUpdateProvisional(run, text)` from the fake transport callback | `provisional()` is `Present` only when not disposed |
| `acceptTurn` | `RunSession.acceptTurn(run, turn)`; outside REQUESTING the call throws and the driver stops | assistant message appended; EXECUTING_TOOLS with the call IDs, IDLE after text, or STOPPING after `CALL_ID_ALREADY_ACCEPTED` |
| `nextCall` | `RunSession.nextCall(run)` | `Execute` for the next call, or `Complete` and phase REQUESTING |
| `admit(result)` | `CallAdmission.execute(effect)` entered on a dedicated thread with a latch in the Effect; `LIMITED` uses a refusing tool budget | `Rejected` after stop or close; `Limited` with call COMPLETED and no effect; `Executed` with EXECUTING observed while the latch is held |
| `effectFinish(ok)` | release the latch, or throw from the Effect, inside the same `CallAdmission.execute` harness | terminal `Call.Status` after the synchronous invocation returns |
| `recordResult` | `RunSession.recordToolResult(run, toolResult)`; then `stop` or `stopForLimit` as `NativeRunDriver.executeCalls` does | one result for the call; cancelled results drained in order when STOPPING |
| `stop` | `RunSession.stop(run)` or `stopIfCurrent(run)` | phase STOPPING; pending calls cancelled; cancelled results drained in order |
| `close` | `RunSession.dispose()` | `DISPOSED` when no run exists; otherwise STOPPING, provisional cleared, pending cancelled, no drain (`RunSession.java:201-223`) |
| `finish` | `RunSession.finish(run)` after the driver is idle and the batch settled | `IDLE` when not disposed; `DISPOSED` after `dispose()` |
| `newGeneration` | `CacheGeneration.replacement(previousSession, reset, id, head)` with `previousSession.phase() == IDLE` and a non-`SessionStart` reset (`CacheGeneration.java:35-46`); later requests use the new generation | new generation ID; the next request does not need the previous prefix; replacement from a non-IDLE session throws |
| `startRead`, `refuseRead`, `smartWaitTick`, `dumbEnter`, `dumbExit`, `editDocument`, `commitDocuments`, `writeAction`, `compute`, `abortComputation`, `register`, `disposeRegistry`, `cancel`, `settle` | no current Java symbol in this plan | trace metadata marks these as `read_adapter_only`; they are excluded from Java replay and retained as breakdown-plan read-kernel inputs |

The reader rejects an action not in this table. For the abstract read actions, Phase 6 filters them before Java replay and records that exclusion as evidence; it never silently skips an action from a selected coarse trace. `admit` and `effectFinish` are distinct model steps but share one blocking synchronous Java invocation, so the replay oracle checks the intermediate latch state and the post-return terminal state.

**ITF reader:** test sources only, `plugin-core/src/test/java/.../nativeagent/model/ItfTrace.java`, JDK-only. Its strict grammar:
- Top level: `#meta` (object), `vars` (array), `states` (array). Any other key is an error.
- Each state: `#meta.index` (int), every name in `vars`, `mbt::actionTaken` (string) and `mbt::nondetPicks` (object). A missing or extra key is an error.
- State 0 is the initial state. Quint labels it with the step action name (`stepCorpus` in the corpus), so the reader checks state 0 against `init` and does not map its label. A label at any other index must be in the mapping table.
- Values: JSON string, boolean, integer or `{"#bigint": "<digits>"}`, `{"#tup": [...]}`, `{"#set": [...]}`, `{"#map": [[k, v], ...]}`, and records as plain objects. Any other `#`-prefixed key is an error.
- An action name missing from the mapping table is an error. An abstract read action is accepted only when the trace metadata marks it `read_adapter_only` and the trace is excluded from Java replay.

**Per-step oracle:** after each replayable model action, compare every mapped field of the ITF record `s` (`phase`, `lc`, `active`, `cancelled`, `provisional`, `history` as message kinds and call IDs, per-slot `status` and `results`, `nextIndex`, `outcome`, `retried`, `generation`, `requestsInGen`) with the Java state. `prev` is not compared. Model call keys `10*run+id` map to provider call IDs `id`. The first mismatch fails with trace file, state index, action, variable, expected and actual values. A selected trace must contain only replayable actions or explicitly excluded read-adapter actions.

**Trace corpus:** copy 20 of the 50 coarse corpus traces (chosen by the lowest seed order), plus, for each coarse witness, one corpus trace in which the witness predicate holds in some state. TLC witness counterexamples carry no action labels and are not replayed. If no labeled trace satisfies a coarse witness, record a finding; do not replay the unlabeled trace. Copy these traces to `plugin-core/src/test/resources/nativeagent/model/traces/`. These files are tracked, and their digests are recorded as evidence of kind `replay`. A zero-trace corpus or unknown action fails.

**Session replay:** `run/session/RunSessionReplayTest.java` replays only the total coarse action mapping through `RunSession` (`start`, `acceptTurn`, `nextCall`, the blocking `CallAdmission.execute` harness, `stop`, `recordToolResult`, `finish`, `dispose`). After each model step it asserts status and phase through the mapping table.

**Driver replay:** first extract the private fakes from `run/cache/NativeRunDriverTest.kt:362-450` to `run/driver/DriverTestFakes.kt` (internal, test sources), with `NativeRunDriverTest.kt` edited to import them. Then `run/driver/NativeRunDriverReplayTest.kt` replays the same coarse traces through `NativeRunDriver` with `runBlocking`.

**PBT:** `run/session/AdmissionPropertyTest.java` uses jqwik to generate interleavings of stop, admit, start, finish and dispose on one deterministic executor. It asserts INV-C1, INV-C2 and INV-C3 after each step. The test names the residual beyond the model bounds; it does not duplicate model checking.

**Commands:** `mtk gradle :plugin-core:test --tests '*ReplayTest' --tests '*AdmissionPropertyTest' --tests '*NativeRunDriverTest'`. Record JUnit XML as evidence of kind `junit`, with discovered and failed counts per named selector.

**Proof matrix (one residual per mechanism, no duplicated claim):**

| Mechanism | Proves | Residual that only it covers |
|---|---|---|
| Quint `verify` | Interaction invariants hold in the model for all interleavings up to recorded bounds | Bounded exhaustive interleavings |
| Trace replay | Java follows the model step by step on stored replayable traces | Model-to-code correspondence |
| jqwik | Java preserves C1-C3 over generated sequences beyond model bounds | Out-of-bound Java sequences |
| PIT | Named tests fail for mutations in the named task classes | Test sensitivity for that task; no global kill-rate gate |
| Existing JUnit | Local value behavior already tested (`RunLifecycleTest`, `RunDomainTest`, `PromptCacheTest`) | Unchanged local behavior |

PIT is run only for a task whose proof matrix names a residual and whose target classes are frozen. Survivors are triaged by the task reviewer; a survivor is not silently treated as equivalent. The task-specific threshold and equivalence disposition are recorded in the breakdown plan, not imposed here.

**PIT toolchain smoke (no gate):** `mtk gradle :plugin-core:pitest -PpitTargets='com.github.catatafishen.agentbridge.nativeagent.lifecycle.*'`. Record the report as evidence of kind `pit` only; this plan does not claim a mutation threshold for unrelated existing code.

## Verification (this plan)

```bash
python3 tracker/tracker.py selftest              # every named fixture gives its expected result
python3 -m unittest discover tracker/tests
quint --version                                  # 0.32.0
# Phase 2a command set for every INV, W, mutant and REF_R; record exact bounds/backend
python3 tracker/tracker.py query uncovered_source_spans          # 0 rows
python3 tracker/tracker.py query duplicate_clauses               # 0 rows
python3 tracker/tracker.py query interaction_without_obligation  # 0 rows
python3 tracker/tracker.py query stale_clauses                   # 0 rows
python3 tracker/tracker.py query unbound_model_actions           # 0 rows
python3 tracker/tracker.py query open_findings                   # 0 Blocker/Major rows at each review gate
mtk gradle :plugin-core:test                                      # executed through mtk
git diff --stat                                                  # only files named in this plan, excluding the other session's files
```

Commit only when the user asks. No AI co-author trailer.

Execution note: run the tracker through the repository Python venv (for example `~/depot/pyVirtualEnvs/py3.12/bin/python tracker/tracker.py selftest`); `python3` above is the human-readable form. The tool itself is Python 3 standard library only.

## Revision 5 address pass

The revision-4 findings have proposed dispositions in this plan:

- R4-001: finite domains, initial state, owner table, exact guards/effects, no-op rule and concrete mutants are specified at lines 166-210.
- R4-002 and R4-005: source observations are keyed by source event, store the source digest, and use current-source selection plus `stale_clauses` at lines 319-371 and 418-447.
- R4-003 and R4-009: product I2-I4, retry bounds, scope, non-goals, assumptions, type-safety and stage fields are scan inputs at lines 418-427.
- R4-004: action census uses the exact model digest and every NEW/DELETE decision requires absence evidence at lines 453-469.
- R4-006: every model action has a replay mapping; synchronous admission uses the blocking harness and active close maps through STOPPING to DISPOSED at lines 503-518.
- R4-007: `plan` is not a polymorphic finding target; review basis is the immutable round digest at lines 342-366.
- R4-008: Phase 2c and Phase 5b require independent semantic review receipts before downstream qualification at lines 299-308 and 449-451.
- R4-010: the unavailable review citation was removed.

Address status: COMPLETE
Plan status: READY_FOR_IMPLEMENTATION_REVIEW
Review status: FINAL_GATE_PASS
Final gate required: NONE
Blocking findings remaining: 0
Deferred findings: 0

## Round 4 (SQCR-R5) address pass

- R5-001: export includes the `actor` rows referenced by exported events before any event row, and every connection enforces `PRAGMA foreign_keys=ON` (Phase 3, export and rebuild).
- R5-002: stage scan fields are `/stages/*/entry`, `/stages/*/exit`, `/stages/*/outcome`, which exist in every read spec; the phantom `/stages/*/contract` is removed (Phase 4 source list).
- R5-003: first-command actor auto-registration rule added (Phase 3 table rules).
- R5-004: `MAX_TURNS` added to the `providerAttempt` guard (Phase 2a guard table).
- R5-005: owner rows added for delivery/batch/request-log/accepted-history/retry (`agent_coarse`) and wait-clock/dumb-mode/document-version/restart-count (`read_execution`).
- R5-006: venv execution note added (Verification section).

## Phase 2a execution amendments (2026-09-15)

Found while the Phase 2a checks ran:
- `newGeneration` was in the model but not in the coarse action, guard or Java mapping tables. The strict reader would reject every corpus trace that has it. Rows added. Java: `CacheGeneration.replacement`. Bound `MAX_GENERATIONS=2` added, because `generation` had no bound.
- `startRead` and `refuseRead` were in the model but not in the read action list. Added.
- The read module had no failure path: a cancelled read never settled. `abortComputation` added. `settle` accepts `ABORTED`. INV-R2 amended. `read_execution` gets its own W-STOP-WAIT.
- `stepCorpus` added for the replay corpus (average trace length 4.08 → 8.76).
- Witnesses use `verify`. Their counterexamples are not replay traces. Coarse witness replay uses labeled corpus traces.
- The ITF reader does not map the state 0 label.

Second pass (invariant strengthening, same day):
- `agent_coarse` was rewritten from the Java code. The old model had no tool loop (a run ended after one batch), did not reset `cancellation` or `recorded` on `send`, and let `finish` drop unrecorded results. C1, C4 and C5 held without testing anything.
- Invariants now state rules over primary state. C2, C5 and C6 compare `prev` with `s`. Lemmas LEM-EFFECT, LEM-CURSOR, LEM-PHASE, LEM-BOUNDS added. New witness W-ID-REUSE.
- INV-C6 now covers provisional delivery. `RunSession.dispose` suppresses provisional output and keeps accepted history, so the old "no accepted result after close" statement did not match the code.
- `read_execution`: INDEX_NOT_READY is the terminal `TIMED_OUT` outcome; INV-R4 strengthened; `MAX_EDITS` and `MAX_DUMB` bound the last two counters.
- Backend change, accepted by the owner: `verify` uses TLC. The models are finite, so TLC checks the complete state space (137,886 coarse states, about 10 s). Apalache did not finish 12 steps on the coarse model.
- Mutant modules removed. Quint 0.32.0 fails to flatten `import ... as B` for TLC and Apalache (QNT404), so every importing mutant crashed, and the old exit-code check counted the crash as a counterexample. Mutants are now `step_mut_<INV>` in the base modules.
- Phase 6 mapping rows now follow the new actions: `beginRequest`, `limitStop`, `deliverProvisional`, `acceptTurn` (replaces `acceptBatch`), `nextCall`.
- Evidence: the check script (now `native-agent-docs/models/check.sh`) gave 59 OK and 0 BAD.

Tracker commands for Phase 2b and 2c (same day):
- Implemented `scan-models`, `obligation add|retire`, `review open|close`, `finding add|address|reject|defer|verify|reopen`.
- Migration `002_review_targets.sql`: finding targets `model` (target_rev = scan event id) and `artifact` (repository path, pinned by the round basis digest), `finding.summary`, `review_round_close.outcome`, `model_action.locator`. Without these, a Phase 2c reviewer could not file a finding against a model, and a finding had no text.
- `current_model_action` now selects the latest scan per module. Per `(module, name)`, a declaration removed from a module stayed current.
- Scan idempotence and model staleness are keyed on the `scan-models` event, because `agent_interface` has no action, var or val and writes no `model_action` row.
- A `passed` close needs every Blocker/Major in the round `verified`, none stale, and evidence with subject `review:<round>`.
- Migrations are versioned by `PRAGMA user_version`; `schema.sql` stays the canonical latest DDL.
- Obligations move from Phase 2b to Phase 4 step 5 (owner decision).
- Evidence: 12 unit tests pass; `selftest` 11 pass, 1 pending (`binding set`). A copy of the real ledger upgraded from version 0 and scanned all four models; a rescan was idempotent.

Address status: COMPLETE (round 4)
Plan status: READY_FOR_IMPLEMENTATION_REVIEW
Final gate required: TARGETED_REVIEW
