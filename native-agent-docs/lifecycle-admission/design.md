# Lifecycle/admission design

## Design status

Installed in production under `plugin-core/src/main/java/com/github/catatafishen/agentbridge/nativeagent/lifecycle/` on branch `native-agent-workflow`. There is no separate design-source copy. Review state: `review.md`.

## Scope

`RunLifecycle` owns one session-local lifecycle and one active run. It accepts validated ordered call batches and executes one synchronous effect at a time at the actual effect-entry boundary. It reports immutable snapshots and operation-local rejection outcomes.

It does not own provider streaming, tool argument decoding, PSI, JSON, coroutine scopes, Swing content, process handlers, or platform writes. A later Kotlin adapter may schedule the Java owner from an IntelliJ-owned scope; it must call this API and must not duplicate its state machine.

The effect receives no stop signal. Pi passes an abort signal into each running tool (`agent-loop.ts:677-718`); here the driver owns any cooperative cancellation token, passes it to tools itself, and cancels it together with `stop(run)`. An effect can also observe `STOPPING`/`CLOSING` through `snapshot()`. The owner catches and rethrows effect failures; converting them into tool error results and continuing, as Pi does (`agent-loop.ts:708-714`), is the driver's job.

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

The public API intentionally does not expose `BatchState`, its cursor, its status map or an effect-completion operation. `Effect.execute()` returns no value; normal return records `COMPLETED`, while any `Throwable` records `FAILED_AFTER_START` and is rethrown as the same object. `Effect` declares no checked exceptions, but Kotlin callers and generic rethrows can raise one; catching only `RuntimeException`/`Error` left such a call `EXECUTING` forever. Lifecycle state retains only statuses, never callback results or throwables. If recording the failure itself throws, that exception is attached with `addSuppressed`; a throwable constructed with suppression disabled drops it, which is an accepted Java limitation.

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

Canonical table: `spec.json` `operation_matrix`. Precedence rules:

Handle identity is checked before phase-specific rejection when an operation accepts a handle. A rejected operation never mutates state. `close` is the only idempotent lifecycle operation.

`beginBatch` performs every validation before installing the next `BatchState` or mutating `acceptedCallIds`; a rejected mixed batch cannot poison a later fresh call ID.

## Type-safety audit binding

Canonical rows: `spec.json` `type_safety_audit`, one per LC requirement, with dimensions in `audit_dimensions`.

Rejected alternatives: a generic `Result<E,A>`, universal effect token, mutable context bag, generic typestate/effects framework, duplicate Quint/Alloy transition model, and a permanent generated implementation. They add abstraction or duplicate truth without eliminating a residual Java aliasing/external-world obligation in this slice.

## Entry and exit evidence

Stage bindings: `spec.json` `stages` S1–S3; evidence definitions: `spec.json` `evidence`. A design compile is not behavior proof. A unit test is not actual AWT/IntelliJ lifecycle proof. A passing S3 does not qualify provider, UI, PSI or donor cutover.
