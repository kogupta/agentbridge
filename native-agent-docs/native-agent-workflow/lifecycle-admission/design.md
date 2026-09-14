# Lifecycle/admission design

## Design status

Draft pending fresh independent review. The declarations are installed in the real `plugin-core/src/main/java/com/github/catatafishen/agentbridge/nativeagent/lifecycle/` source tree on branch `native-agent-workflow`; IntelliJ must inspect and compile these exact files. The former ignored design-src copy is not part of the basis and is removed to prevent duplicate implementations.

## Scope

`RunLifecycle` owns one session-local lifecycle and one active run. It accepts validated ordered call batches and executes one synchronous effect at a time at the actual effect-entry boundary. It reports immutable snapshots and operation-local rejection outcomes.

It does not own provider streaming, tool argument decoding, PSI, JSON, coroutine scopes, Swing content, process handlers, or platform writes. A later Kotlin adapter may schedule the Java owner from an IntelliJ-owned scope; it must call this API and must not duplicate its state machine.

## Java surface

| Type | Role | Construction/visibility rule |
|---|---|---|
| `Id` | Nonblank call identity | Public record validates non-null/nonblank value. |
| `Batch` | Nonempty immutable distinct ordered calls | Final class; only `of(List<CallId>)`; snapshots input. |
| `RunHandle` | Opaque run capability | Final class; constructor package-private; owner identity private; exposes no observation payload. |
| `Handle` | Opaque batch capability | Final class; constructor package-private; owner/run identity private. |
| `Phase` | Closed finite lifecycle phase set | Enum. |
| `Status` | Closed call status set | Enum; transient and terminal statuses are explicit; only terminal statuses satisfy `isTerminal()`. |
| `Snapshot` | Read-only lifecycle view | Sealed interface; active payload contains phase only and no capability handle. |
| `Snapshot` | Immutable call/status pair | Public record validates both components. |
| `Snapshot` | Read-only ordered status view | Record copies a nonempty ordered list; it exposes no capability handle or owner mutation. |
| `Observation` | Explicit presence/absence of current batch | Sealed variants; no nullable snapshot. |
| `Effect` | Value-free synchronous callback boundary | Functional interface; effect is called only after admission linearizes and cannot return a foreign result object. |
| `RunLifecycle` | Single owner and transition authority | Final class; all state mutations synchronized; no public state mutation or completion operation. |

`RunLifecycle` result algebras are nested sealed interfaces with named variants:

- `StartRunResult.Started` or `.Rejected(StartRejection)`;
- `BeginBatchResult.Begun` or `.Rejected(BatchRejection)`;
- `ExecutionResult.Executed` or `.Rejected(ExecutionRejection)`;
- `StopResult.Acknowledged` or `.Rejected(StopRejection)`;
- `FinishRunResult.Finished` or `.Rejected(FinishRejection)`.

No generic error envelope, status string, nullable success payload or caller-supplied capability token exists in the domain surface.

## State and transitions

The owner stores one `Phase`, current `RunHandle`, at most one current `BatchState`, an owner-private identity key and a run-local accepted-call set.

- `IDLE -> RUNNING`: `startRun()` creates a fresh handle and clears prior accepted IDs.
- `RUNNING -> STOPPING`: `stop(currentRun)` marks all pending calls cancelled while leaving no monitor held across effects.
- `RUNNING -> CLOSING`: `close()` marks pending calls cancelled while an active run exists.
- `STOPPING -> CLOSING`: `close()` preserves the active run and moves it into closing; it can never later become IDLE.
- `RUNNING -> IDLE`: `finishRun(currentRun)` after no batch or a settled current batch.
- `STOPPING -> IDLE`: `finishRun(currentRun)` only after the current batch is settled.
- `CLOSING -> CLOSED`: `finishRun(currentRun)` only after the current batch is settled.
- `IDLE -> CLOSED`: `close()` is immediate because no effect is active.
- `CLOSED`: `startRun`, batch admission, effect execution and later close cannot reopen or mutate the owner.

The public API intentionally does not expose `BatchState`, its cursor, its status map or an effect-completion operation. `Effect.execute()` returns no value; normal return records `COMPLETED`, while `RuntimeException`/`Error` records `FAILED_AFTER_START` and rethrows the same object. Lifecycle state retains only statuses, never callback results or throwables.

## Admission algorithm

`execute(batch, call, effect)` performs these steps:

1. Validate non-null method arguments before acquiring state.
2. Synchronize on the owner.
3. Reject foreign/stale handles, non-running phases, unknown calls, terminal calls, and calls other than the first unsettled call.
4. Change the selected call from `PENDING` to `EXECUTING` while still holding the monitor.
5. Release the monitor before invoking the callback.
6. Invoke the callback immediately; no scheduling or suspension occurs between admission and callback invocation.
7. Reacquire the monitor to record the terminal disposition.

`stop()` uses the same monitor. If it acquires the monitor first, the pending call is cancelled and later `execute()` is rejected. If `execute()` acquires it first, the call is `EXECUTING` before `stop()` can change phase; Stop returns without waiting for the callback. No monitor is held during callback execution.

This establishes safety. It does not assert that an arbitrary callback eventually returns. The driver must call `finishRun()` after owned external work settles; `finishRun()` rejects while an executing call remains unsettled.

## Batch and run rules

`CallBatch.of` establishes nonempty, distinct, ordered immutable calls. `RunLifecycle.beginBatch` rejects a reused `Id` anywhere in the current run, not only in the immediately previous batch. A second batch is rejected while the previous batch is unsettled. The API permits a second batch after the previous batch settles and while the run remains `RUNNING`.

`stop()` and `close()` cancel pending calls in the current batch. They do not invent a result for an executing call. A terminal snapshot is returned as an immutable value; it remains readable after the owner advances or closes. A stale `Handle` cannot query a replaced batch, so the owner retains no historical batch map.

## Operation-by-phase matrix

Handle identity is checked before phase-specific rejection when an operation accepts a handle. A rejected operation never mutates state. `close` is the only idempotent lifecycle operation.

| Operation | Phase/precondition | Result and next phase |
|---|---|---|
| `startRun` | `IDLE` | `Started`; `RUNNING` |
| `startRun` | `RUNNING`/`STOPPING` | `Rejected(BUSY)`; unchanged |
| `startRun` | `CLOSING`/`CLOSED` | `Rejected(CLOSED)`; unchanged |
| `beginBatch` | current run, `RUNNING`, no unsettled current batch, all IDs fresh | `Begun`; `RUNNING` |
| `beginBatch` | current run but any precondition fails | exact rejection (`PREVIOUS_BATCH_UNSETTLED` or `CALL_ID_ALREADY_ACCEPTED`); unchanged |
| `beginBatch` | stale/foreign run or `IDLE`/`CLOSED` | `Rejected(STALE_RUN)`; unchanged |
| `beginBatch` | `STOPPING`/`CLOSING` | `Rejected(RUN_NOT_ACCEPTING_BATCHES)`; unchanged |
| `execute` | current batch, `RUNNING`, first pending call | `Executed` or callback throwable; terminal status recorded |
| `execute` | stale/replaced batch | `Rejected(STALE_BATCH)`; unchanged |
| `execute` | current batch but stopped/closing | `Rejected(RUN_NOT_ACCEPTING_EFFECTS)`; unchanged |
| `execute` | unknown, executing, terminal or later call | exact rejection; unchanged |
| `stop` | current run in `RUNNING` | `Acknowledged`; `STOPPING`, pending calls cancelled |
| `stop` | current run in `STOPPING`/`CLOSING` | idempotent `Acknowledged`; unchanged |
| `stop` | stale/foreign run or no current run | `Rejected(STALE_RUN)`; unchanged |
| `finishRun` | current run, no batch or settled batch, `RUNNING`/`STOPPING` | `Finished`; `IDLE` |
| `finishRun` | current run, no batch or settled batch, `CLOSING` | `Finished`; `CLOSED` |
| `finishRun` | current run with unsettled batch | `Rejected(BATCH_UNSETTLED)`; unchanged |
| `finishRun` | stale/foreign run or no current run | `Rejected(STALE_RUN)`; unchanged |
| `close` | `IDLE` | closed snapshot; `CLOSED` |
| `close` | `RUNNING`/`STOPPING` | closing snapshot; `CLOSING` |
| `close` | `CLOSING`/`CLOSED` | idempotent snapshot; unchanged |
| `batchSnapshot` | current batch handle | `Available(immutable ordered snapshot)`; unchanged |
| `batchSnapshot` | stale/foreign/replaced handle | `Rejected(STALE_BATCH)`; unchanged |

`beginBatch` performs every validation before installing the next `BatchState` or mutating `acceptedCallIds`; a rejected mixed batch cannot poison a later fresh call ID.

## Type-safety audit binding

| Requirement | Invalid representation/operation | Type/API prevention | Residual runtime obligation | Justification |
|---|---|---|---|---|
| LC-001 | Two active runs or start after close | One final owner; no public active-state constructor | Synchronize `startRun`; check phase/handle identity | Java cannot enforce linear ownership statically. |
| LC-002 | Empty/duplicate/null/mutable batch | `Id`, private `Batch` constructor, `List.copyOf`, distinct check | Validate foreign input at construction | A general nonempty unique-list type would add more machinery than this slice needs. |
| LC-003 | Out-of-order or concurrent effects | Private batch cursor/dispositions; no completion mutator | Monitor checks next unsettled call and execution status | Ordering depends on mutable transition history. |
| LC-004 | Stop loses to queued effect or effect starts after Stop | No public admission token; execute owns entry | Same monitor linearizes Stop and actual callback entry | Java has no affine/linear capability type; callback invocation is external. |
| LC-005 | New work after Stop or false settlement | `STOPPING`/`CLOSING` are separate phases; `finishRun` requires settled batch | Driver owns external resource settlement | The owner cannot statically control arbitrary external work. |
| LC-006 | Duplicate completion or result flag combinations | No public completion method; one terminal disposition per call | normal return records COMPLETED; RuntimeException/Error records FAILED_AFTER_START and rethrows the same object; duplicate execute rejects | Callback failure and scheduling are runtime events. |
| LC-007 | Foreign/old capability changes current state or observation grants authority | Distinct final handle types; private owner key; snapshots contain neither handle | Identity checks at every operation; stale snapshot query rejects | Java aliases remain forgeable only through package code; owner key is inaccessible and snapshots are inert. |
| LC-008 | Closed owner reopens or loses active work | `Closed` snapshot and explicit close transitions | Idempotent close and cleanup acknowledgement | External running effect can outlive content; no unconditional liveness claim. |
| LC-009 | JSON/PSI/coroutine/provider objects in core | API contains only Java domain types; no generic envelope | Boundary/dependency inspection; constructor null checks; no static whole-program nullness claim | No JSpecify/NullAway is introduced; Java nullable references remain a stated limitation. |
| LC-010 | Stop blocks on callback or caller mutates state | Snapshot records and private state; monitor scope ends before callback | Concurrency test with blocked callback | Lock duration is a behavioral property, not represented by Java types. |
| LC-011 | Call IDs collide across accepted batches | Run-private accepted ID set and immutable batch | Reject reuse before batch admission | Uniqueness is relative to run history. |
| LC-012 | Gate passes without audit/evidence | Canonical IDs in `spec.json`; S1/S2/S3 evidence bindings | Exact-basis review and evidence checks | Structural tooling cannot judge whether a chosen type/API truly enforces semantics. |

Rejected alternatives: a generic `Result<E,A>`, universal effect token, mutable context bag, generic typestate/effects framework, duplicate Quint/Alloy transition model, and a permanent generated implementation. They add abstraction or duplicate truth without eliminating a residual Java aliasing/external-world obligation in this slice.

## Entry and exit evidence

- **S1 entry:** `EV-SOURCE`; user-selected Java/Kotlin boundary; W1–W8.
- **S1 exit:** all LC requirements present; `EV-STRUCTURE`, `EV-DESIGN-SOURCE`, `EV-DESIGN-COMPILE` and `EV-RECEIPT` from the source-backed declarations and exact-basis receipt.
- **S2 entry:** S1 exit remains valid and exact basis is frozen.
- **S2 exit:** `EV-REVIEW`, `EV-STRUCTURE`, `EV-DESIGN-SOURCE`, `EV-DESIGN-COMPILE` and `EV-RECEIPT`; zero open Blockers/Majors; reviewer confirms every audit row, matrix row and contract.
- **S3 entry:** current S2 receipt, source/design/compile receipt, clean authorized branch and allowed surface.
- **S3 exit:** `EV-FOCUSED`, `EV-SMOKE`, `EV-BUILD`, `EV-SURFACE` and `EV-RECEIPT`; LC-012 retains review/structure/review/VCS evidence.

A design compile is not behavior proof. The source-backed declarations are the only design basis; the temporary ignored declaration copy is deleted before the fresh review. A unit test is not actual AWT/IntelliJ lifecycle proof. A passing S3 does not qualify provider, UI, PSI or donor cutover.
