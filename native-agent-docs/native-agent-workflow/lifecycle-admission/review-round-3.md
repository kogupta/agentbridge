# Lifecycle/admission review — round 3 (adversarial)

- Reviewer: Claude Fable 5.1, prompt `adversarial-review-prompt.md`.
- Basis: branch `native-agent-workflow`, working tree with the uncommitted diff to `RunLifecycle.java`, `RunLifecycleTest.java`, `plugin-core/build.gradle.kts` (2026-09-14).
- Reference: `pi/packages/agent/src/agent-loop.ts` lines 227–235, 379–404, 442–479, 636–661, 677–718.

## 1. Executive verdict

**FAIL.**

One new Blocker: a checked exception thrown through `Effect.execute()` wedges the owner forever. The state machine has no recovery path. Two round-2 Blockers remain open. The round-2 test gaps are not addressed.

Verified sound: monitor discipline, reentrancy from inside an effect, Stop/complete ordering, handle identity across owners and generations, transactional `beginBatch`, null checks before mutation, snapshot immutability. Gradle run of `RunLifecycleTest`: 12 tests, 0 failures.

## 2. Findings table

| ID | Severity | Vector | Summary |
|---|---|---|---|
| B-001 | Blocker | 2 | Non-`RuntimeException` throwable (checked exception, Kotlin caller, sneaky throw) leaves the call `EXECUTING` forever. `finishRun` and `close` can never settle. |
| B-002 | Blocker | LC-012 | Round-2 B-001 still open: no S1 receipt, no structure-check output, no compile receipt, no evidence-result hashes exist in the basis directory. |
| B-003 | Blocker | LC-012 | Round-2 B-002 partly open: AC-003 target fixed. AC-012 still names nonexistent `check_slice.py`. AC-013 still names `lifecycle-admission-awt-smoke`. The implementation is `RunLifecycleTest#awtAdmissionSmoke`. |
| M-001 | Major | 1, 3, 4, 6 | Round-2 test gaps are unchanged. AC-001, AC-003, AC-007, AC-008, AC-009, AC-011 do not execute the scenario the spec text states. |
| M-002 | Major | 5 | Pi passes the abort signal into the running tool. `Effect.execute()` receives nothing. An executing effect cannot observe Stop except by polling `snapshot()`. |
| N-001 | Minor | 6 | Operation matrix row `execute` in `CLOSED -> RUN_NOT_ACCEPTING_EFFECTS` is unreachable. `finishRun` nulls `currentBatch`, so the result is always `STALE_BATCH`. |
| N-002 | Minor | 2 | `failure.addSuppressed(accountingFailure)` is a silent no-op when the throwable was built with suppression disabled. The accounting failure is lost. |
| N-003 | Nit | 3 | `ownerKey` checks are redundant. `run != currentRun` and `currentBatch.handle != batch` already require reference identity with owner-private objects. `Batch.Handle.run()` exposes the `RunHandle` to package code. |
| N-004 | Nit | 6 | `Effect.java` has no contract Javadoc. The synchronous, no-detached-work, throwable-accounting rules live only in `spec.json`. |
| N-005 | Nit | build | `gradlew.bat` is untracked again after commit `da8d252b3` removed it. `testRuntimeOnly("junit:junit:4.13.2")` has no comment that states why the platform test framework needs it. |
| N-006 | Nit | tests | `staleHandleIsolation` and `multipleBatchesPreserveIdentity` end with tautological `assertNotNull` on handles. |

## 3. Concrete proof

### B-001 — checked throwable wedges the owner

`execute()` catches only `RuntimeException | Error`. A checked exception reaches the caller without the `recordFailure` path. Kotlin has no checked exceptions, so a Kotlin adapter lambda throws `IOException` or `InterruptedException` directly. Java code reaches the same path with a generic sneaky throw.

Reproduction (compiled against the exact lifecycle sources with `javac --release 21`):

```java
lc.execute(batch, first, () -> Repro.<RuntimeException>sneaky(new IOException("x")));
```

Observed output:

```
caller saw: java.io.IOException: x
batch after throw: [first=EXECUTING, second=PENDING]
execute(second): Rejected[OUT_OF_ORDER]
execute(first) again: Rejected[ALREADY_EXECUTING]
stop: Acknowledged[STOPPING, first=EXECUTING, second=CANCELLED_BEFORE_START]
finishRun: Rejected[BATCH_UNSETTLED]
close: Active[CLOSING]
finishRun after close: Rejected[BATCH_UNSETTLED]
startRun: Rejected[CLOSED]
```

Consequences:

- LC-006 is violated. The call never receives a terminal status.
- LC-008 is violated. The owner can never reach `CLOSED`.
- The session is dead. No public operation can repair it. The driver must discard the `RunLifecycle` instance.
- `spec.json` LC-006, AC-006 and the LC-006 audit row scope the guarantee to `RuntimeException`/`Error`. The specification itself has the hole.

### B-002 — evidence artifacts absent

`ls .agent-work/native-agent-workflow/lifecycle-admission/` contains only `adversarial-review-prompt.md`, `design.md`, `design-source-path.txt`, `review-basis.md`, `review-round-2.md`, `review-state.md`, `spec.json`. No receipt, no structure-check output, no compile output. `review-state.md` still lists round-2 B-001 as Open.

### B-003 — acceptance bindings

`find . -name 'check_slice*'` returns nothing. `spec.json` AC-012 target is `check_slice.py`. AC-013 target is `lifecycle-admission-awt-smoke`. The only AWT scenario is the JUnit method `awtAdmissionSmoke`. Only AC-003 was renamed.

### M-001 — test scenarios do not match spec text

| AC | Spec text | Test reality |
|---|---|---|
| AC-001 | "later start owns a new handle" and round-2 asked for old-generation checks | `singleRunOwnership` never uses `first` against the second run. Probe confirmed correct behavior (`STALE_RUN` for stop/finish/beginBatch), but no test asserts it. |
| AC-003 | "execute A concurrently from two threads using barriers" | No `CyclicBarrier` in the file. The duplicate attempt runs after A is confirmed entered. Deterministic, not a race. |
| AC-007 | "Use handles across owners and after starting a new generation" | Only foreign `RunHandle` on `beginBatch`. No foreign `Batch.Handle` on `execute` or `batchSnapshot`. No unchanged-state assertion after each rejection. |
| AC-008 | "pending calls cancel, active outcome is retained" | `closeDrainsWithoutReopening` asserts phases only. It never reads `pending` or `active` status before `finishRun`. |
| AC-009 | "pass null to ... execute ... recording snapshots before each mutating call" | `execute(batch, null, effect)`, `execute(batch, call, null)`, `beginBatch(run, null)` are untested. All null tests run in `IDLE`. No batch snapshot before/after. |
| AC-011 | "submit [freshB, reusedA]; then submit freshB in a new batch" | Candidate is `[fresh, first]` where `fresh` is reused and `first` is the fresh one. The follow-up batch submits `"new"`, not `first`. Poisoning of `first` is never tested. |
| new | reentrant calls from inside an effect | Untested. Probe confirmed safe: inner `execute` -> `ALREADY_EXECUTING`/`OUT_OF_ORDER`, `beginBatch` -> `PREVIOUS_BATCH_UNSETTLED`, `finishRun` -> `BATCH_UNSETTLED`, `startRun` -> `BUSY`, `stop`/`close` succeed, outer call still records `COMPLETED`. |
| new | non-Runtime throwable | Untested. See B-001. |

### M-002 — Pi abort signal discrepancy

Pi `agent-loop.ts:677–718` passes `signal` into `tool.execute(...)`, then awaits the tool. A cooperating tool stops early. Pi `agent-loop.ts:476–478` breaks the sequential loop after an aborted call. Pi `agent-loop.ts:708–714` converts a thrown tool error into an error result and continues.

Java lifecycle:

- Nothing new starts after Stop. Matches Pi. Verified by `stopBeforeQueuedEffect` and `awtAdmissionSmoke`.
- The executing effect runs to completion. Stop does not wait. `finishRun` waits. Matches Pi at the driver level.
- The effect receives no stop signal. The only observation path is `lifecycle.snapshot().phase() == STOPPING` polled from inside the callback. Pi tools get the signal as an argument.
- A thrown effect is rethrown to the driver. The next call stays admissible. Matches Pi only if the driver catches and continues. This obligation is not documented on `Effect`.

`spec.json` lists "Unconditional termination of arbitrary callbacks" as a non-goal. Cooperative signal delivery is not unconditional termination. The author can disposition this to the driver layer, but the design must then state where the signal comes from.

Truncated responses (`agent-loop.ts:227–235`, `379–404`): Pi never executes those calls and emits synthetic error results. Layer 1 has no truncation concept. The driver must not call `beginBatch` for a truncated message. Not a lifecycle defect. Record it as a driver obligation.

### Vectors verified with no finding

- **Vector 1, monitor window.** No monitor is held during `effect.execute()`. Reentrant calls on the same thread take the free monitor. Java monitors are reentrant in any case, so no deadlock is possible. Probe output above.
- **Vector 1, Stop vs complete.** Both run under the monitor. `cancelPending` writes only `PENDING` entries. `markTerminal` requires `EXECUTING`. No interleaving can overwrite a terminal or executing status.
- **Vector 1, detached work.** An effect that spawns a thread returns `COMPLETED` at once. `finishRun` then succeeds. This is the stated assumption "driver calls finishRun only after other owned resources settle". Accepted residual. See N-004.
- **Vector 3.** `isStaleRun` requires `run == currentRun`. `isStaleBatch` requires `batch == currentBatch.handle`. Both are owner-private references. Package code that forges a handle with a random key fails identity. Cross-instance handles fail at both checks. Reflection is out of scope per `decisions.reflection`.
- **Vector 4.** `beginBatch` performs all four checks before `currentBatch` or `acceptedCallIds` change. A rejected mixed batch mutates nothing. `acceptedCallIds` is cleared on `startRun` and `finishRun`. Behavior is correct. The test gap is M-001.
- **Vector 6.** Every public method calls `Objects.requireNonNull` before the monitor. `Call.Batch.of` uses `List.copyOf`, which rejects null elements. `Batch.Snapshot`, `Call.Snapshot`, `Lifecycle.Snapshot.Active` are records over immutable values. `Call.Batch.calls()` returns the `List.copyOf` result. No internal collection leaks.

## 4. Actionable recommendations

### B-001 — catch every throwable, keep the same object

`RunLifecycle.java`:

```java
public ExecutionResult execute(Batch.Handle batch, Call.Id call, Effect effect) {
    Objects.requireNonNull(batch, "batch");
    Objects.requireNonNull(call, "call");
    Objects.requireNonNull(effect, "effect");

    synchronized (this) {
        ExecutionRejection rejection = rejectExecution(batch, call);
        if (rejection != null) {
            return new ExecutionResult.Rejected(rejection);
        }
        currentBatch.markExecuting(call);
    }

    try {
        effect.execute();
    } catch (Throwable failure) {
        recordFailure(batch, call, failure);
        throw rethrow(failure);
    }
    complete(batch, call, Call.Status.COMPLETED);
    return new ExecutionResult.Executed();
}

private void recordFailure(Batch.Handle batch, Call.Id call, Throwable failure) {
    try {
        complete(batch, call, Call.Status.FAILED_AFTER_START);
    } catch (Throwable accountingFailure) {
        if (accountingFailure != failure) {
            failure.addSuppressed(accountingFailure);
        }
    }
}

@SuppressWarnings("unchecked")
private static <T extends Throwable> RuntimeException rethrow(Throwable failure) throws T {
    throw (T) failure;
}
```

Moving `complete(COMPLETED)` out of the `try` also removes the path where an accounting `IllegalStateException` after a normal return was re-recorded as `FAILED_AFTER_START`.

`spec.json` and `design.md`: change LC-006, AC-006, the LC-006 audit row and the "State and transitions" paragraph from "RuntimeException/Error" to "any Throwable". Add to AC-006: a checked exception thrown through a generic sneaky throw is recorded `FAILED_AFTER_START` and rethrown as the same object.

`RunLifecycleTest.terminalAccounting`: add

```java
IOException checked = new IOException("checked");
assertTerminalStatus(Call.Status.FAILED_AFTER_START, () -> sneaky(checked), checked);
```

with a test-local `static <T extends Throwable> void sneaky(Throwable t) throws T { throw (T) t; }`.

### B-002 — produce the receipt

Generate and commit under the basis directory: structure-check output, `javac --release 21` output for the exact six sources, signature extraction, source/spec/design/config hashes, and the receipt verification output that LC-012 and EV-RECEIPT require. Update `review-state.md` round-2 B-001 to Addressed with file names.

### B-003 — align acceptance targets

`spec.json`:

- AC-013 `target`: `RunLifecycleTest#awtAdmissionSmoke`. Update EV-SMOKE to say the scenario runs as a JUnit method on the real `EventQueue` in the same executor, or supply a separate launcher.
- AC-012: supply `check_slice.py` with the described behavior, or remove AC-012 through the specification gate and point EV-STRUCTURE at the real command.

### M-001 — make the tests execute the spec text

- AC-001: after the second `startRun`, assert `stop(first)`, `finishRun(first)`, `beginBatch(first, ...)` return `STALE_RUN` and `snapshot()` is still `RUNNING`.
- AC-003: start two threads on a `CyclicBarrier(2)` that both call `execute(batch, first, ...)`; assert exactly one `Executed`, one `ALREADY_EXECUTING`, counter 1. Or change the AC-003 text to the deterministic ordering the test already proves.
- AC-007: on `secondLifecycle`, call `execute(firstBatch, firstCall, ...)` and `batchSnapshot(firstBatch)`; assert `STALE_BATCH` and that `secondLifecycle.snapshot()` is unchanged. Replace the two `assertNotNull` lines with those assertions.
- AC-008: before `release.countDown()`, assert `pending` is `CANCELLED_BEFORE_START` and `active` is `EXECUTING`. After `execution.get`, assert `active` is `COMPLETED`.
- AC-009: start a run and a batch, capture `snapshot()` and `batchSnapshot(batch)`, then call `beginBatch(run, null)`, `execute(batch, null, e)`, `execute(batch, call, null)`, `execute(null, call, e)`; assert NPE each time and both snapshots equal the captured values.
- AC-011: rename the ids so the candidate is `[freshB, reusedA]`, then `beginBatch(nextRun, [freshB])` and execute `freshB`.
- New test: reentrant `stop`, `close`, `execute`, `finishRun`, `startRun`, `beginBatch` from inside an effect; assert the probe results listed in section 3.

### M-002 — decide where the stop signal lives

Option A, lifecycle-owned: change `Effect` to `void execute(StopSignal signal)` where `StopSignal` is a tiny final class with `boolean isRequested()` backed by the owner phase. The owner passes it at admission. Update LC-004/LC-005 and the design table.

Option B, driver-owned: keep `Effect` value-free and argument-free. Add to `design.md` "Scope" and to `Effect` Javadoc: the driver supplies its own cancellation token to tools, and the driver must call `stop(run)` and cancel that token together. Record the deviation from Pi `agent-loop.ts:689` in the parity matrix.

Recommendation: Option B for this slice. It keeps the layer synchronous and value-free. The parity matrix must record it.

### N-001

`spec.json` operation matrix and `design.md` matrix: change the `execute` row for `CLOSED` to `Rejected(STALE_BATCH)` because no current batch exists after `finishRun`. Keep `STOPPING|CLOSING -> RUN_NOT_ACCEPTING_EFFECTS`.

### N-002

Accept as a Java limitation, or record the accounting failure through a logger at the driver boundary. Document the limitation in `design.md` next to the rethrow rule.

### N-003

Optional: drop `ownerKey` from `RunHandle` and `Batch.Handle`, keep reference identity. If kept, state in `design.md` that identity, not the key, is the authority. Make `Batch.Handle.run()` private and pass the `RunHandle` into `isStaleBatch` from `BatchState`.

### N-004

Add Javadoc to `Effect`:

```java
/**
 * Synchronous callback invoked once, outside the owner monitor, after admission.
 * Return normally to record COMPLETED. Any throwable records FAILED_AFTER_START
 * and is rethrown unchanged. Do not queue or detach work the driver cannot settle
 * before finishRun. Stop is observable only through RunLifecycle.snapshot().
 */
```

### N-005

Add `gradlew.bat` to `.gitignore` or delete it. Add a comment above `testRuntimeOnly("junit:junit:4.13.2")` that names the platform test framework requirement.

### N-006

Replace the trailing `assertNotNull` calls with the AC-007 and AC-011 assertions in M-001.

## State

- Review status: FINDINGS_READY_FOR_ADDRESS
- Open Blockers: 3 (B-001 new; B-002, B-003 carried from round 2)
- Open Majors: 2
- Minors/Nits: 6
- Verification: Gradle `:plugin-core:test --tests RunLifecycleTest` passed, 12/12. Standalone probes in the session scratchpad reproduced B-001 and confirmed the safe reentrancy, stale-generation and cross-owner results.
- Next permitted action: fix B-001 with the spec change, produce the receipt for B-002, align AC-012/AC-013 for B-003, close M-001 test gaps, disposition M-002, then obtain a fresh review.
