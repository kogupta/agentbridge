# Lifecycle/admission review ledger

Append-only. Rounds are in chronological order. Add a new round or a Resolution section at the end; never rewrite earlier rounds. File paths inside rounds 1–3 are as they were at review time (`.agent-work/native-agent-workflow/lifecycle-admission/…`, now `native-agent-docs/lifecycle-admission/…`). The round-2 frozen basis dump (`review-basis.md`) was a copy of other files and is removed; regenerate a basis from git when needed.

## Current state

- Review status: FINDINGS_READY_FOR_ADDRESS
- Latest round: 3 (Claude Fable 5.1, adversarial prompt in the appendix)
- Open Blockers: 3 — round 3 B-001 (non-Runtime throwable wedges the owner), B-002 (no S1 receipt/evidence), B-003 (AC-012/AC-013 targets)
- Open Majors: 2 — round 3 M-001 (tests do not run the AC scenarios), M-002 (running effect cannot observe Stop)
- Minors/Nits: 6 (round 3 N-001–N-006)
- Frozen basis: INVALIDATED
- Next permitted action: address round 3 findings, regenerate evidence, obtain a fresh review

## Round 1

### Peer agreement

- Primary reviewer: `openai-codex/gpt-5.6-sol`, reasoning effort `medium`, configuration role `plan`.
- `omp config get modelRoles --json` confirms `plan = openai-codex/gpt-5.6-sol:medium`.
- Additional independent peer: none, explicitly selected by user.
- Author/coordinator: current session; not eligible to approve its own design.

### Frozen basis

- Basis files: `spec.json`, `design.md`, `design-src/**/*.java`, and the revised plan sections defining W1–W8 and S1–S3.
- Canonical requirements: LC-001 through LC-012.
- Acceptance criteria: AC-001 through AC-012.
- Type-safety audit: one row for each LC requirement; dimensions include sum types, state payloads, validated values, constructor authority, transitions, argument/result correlation, collection invariants, aliasing/ownership, protocol progression, visibility and nullness.
- Evidence IDs: EV-SOURCE, EV-STRUCTURE, EV-DESIGN-COMPILE, EV-REVIEW, EV-VCS, EV-FOCUSED, EV-SMOKE, EV-BUILD, EV-SURFACE.
- Stage contracts: S1 normalize/design, S2 independent review/freeze, S3 implementation/qualification.
- Source basis: current project branch is still `master`; the eight pre-existing untracked entries are unrelated to this lifecycle basis. Design declarations compile with `javac --release 21` in the ignored design workspace. No production source has been installed.

### Review status

Review status: IN_PROGRESS
Reviewer checklist: INCOMPLETE
Frozen basis: CURRENT
Peer agreement: openai-codex/gpt-5.6-sol:medium + none
Peer exchanges used: 0
Open Blockers: unknown; independent review running
Open Majors: unknown; independent review running
Deferred Minors/Nits: 0
Verification: PARTIAL (design declarations compile; structural binding check passes; behavioral tests not run)
Review rounds: 1
Stop reason: awaiting independent review output
Next permitted action: address reviewer findings; do not install production source until review passes

### Review round 1 — independent findings

Reviewer: `openai-codex/gpt-5.6-sol:medium` (configured `plan` role). Basis at review launch was stale relative to the later branch/source installation, so B-004 and M-005 are accepted independently of that timing issue. The remaining findings are valid design corrections.

- **B-001 — Addressed:** replace generic `Effect<T>` and `ExecutionResult.Executed<T>` with a value-free `Effect` and non-generic `ExecutionResult.Executed`; lifecycle retains only `Status`. Update LC-006/LC-009, AC-006/AC-009 and the audit.
- **B-002 — Addressed:** remove `RunHandle` from `Snapshot`; snapshots expose phase only. Remove `Handle` from `Snapshot`; use ordered `Snapshot` values. Stale handles cannot query replaced batches.
- **B-003 — Addressed:** document normal `RUNNING -> IDLE` completion and `STOPPING + close -> CLOSING`; add close-after-stop acceptance.
- **B-004 — Addressed:** source-backed declarations now exist under `plugin-core/src/main/java/.../nativeagent/lifecycle/`; compile/surface evidence is reissued after correction.
- **M-001 — Addressed:** add the complete public operation-by-phase matrix and precedence to the design/spec.
- **M-002 — Addressed:** retain transactional `beginBatch` validation before any accepted-ID mutation; add mixed fresh/reused ID regression.
- **M-003 — Addressed:** add same-call concurrent execution regression using barriers.
- **M-004 — Addressed:** normal return and throwable accounting are explicit; completion rechecks current owner/batch/call and preserves the original throwable if accounting itself fails.
- **M-005 — Addressed:** add exact receipt identity fields and current source/design/compile hashes to the frozen basis. Re-review is required after these changes.
- **M-006 — Addressed:** enumerate all nullable public boundaries and before/after snapshot assertions.
- **M-007 — Addressed:** choose minimal semantics: captured snapshots remain readable; stale handles cannot query after replacement.
- **N-001 — Addressed:** rename `CallDisposition` to `Status`.
- **N-002 — Accepted with clarification:** package-private handle constructors can create inert handles, but the private owner key is inaccessible; prose now says owner identity, not package visibility, establishes acceptance.
- **N-003 — Addressed:** `Snapshot` exposes an immutable ordered list of `Snapshot`.
- **N-004 — Addressed:** focused tests and separately launched AWT smoke have distinct evidence IDs and scenarios.

Review status: FINDINGS_READY_FOR_ADDRESS
Reviewer checklist: COMPLETE
Frozen basis: INVALIDATED
Peer agreement: openai-codex/gpt-5.6-sol:medium + none
Peer exchanges used: 0
Open Blockers: 0 after addressed corrections; fresh review required
Open Majors: 0 after addressed corrections; fresh review required
Deferred Minors/Nits: 0
Verification: PARTIAL (source-backed declarations compile before final correction; recompile and focused behavior tests required)
Review rounds: 1
Stop reason: review basis changed; targeted final gate cannot reuse this review
Next permitted action: update canonical spec/design/receipts, recompile, obtain fresh independent review, then implement focused tests

## Round 2

Reviewer: `openai-codex/gpt-5.6-sol:medium`, reasoning effort `medium`, configured `plan` role.
Basis: `.agent-work/native-agent-workflow/lifecycle-admission/review-basis.md`; branch `native-agent-workflow`.

### Summary

S2 review result: FAIL. Core lifecycle source is largely consistent with LC-001–LC-011: Stop and admission share one monitor; callbacks run outside the monitor; pending calls cancel without overwriting executing/terminal calls; batch ID validation precedes mutation; snapshots contain no handles; callback exceptions are accounted and rethrown; public mutating inputs are null-checked; source uses Java-21-compatible constructs and only Java utility imports.

S2 cannot pass because exact S1 evidence is absent, one public rejection variant is unreachable, and acceptance targets/tests do not execute all stated scenarios.

### Blockers

#### B-001 — Exact-basis S1 receipt and evidence results absent

LC-012/S2 require current `EV-STRUCTURE`, `EV-DESIGN-SOURCE`, `EV-DESIGN-COMPILE`, and `EV-RECEIPT` results. The review basis contained descriptions and file hashes, but no receipt with stage/feature/spec/design/declaration/configuration/evidence hashes, no signature extraction, no strict-check output, no expected invalid-copy output, no attached `javac --release 21` result for the exact basis, and no receipt verification result. `review-state.md` was stale: it named `master`, removed `design-src`, omitted AC-013 and newer evidence IDs, and said production source was absent.

Required: reissue current `review-state.md`, create S1 receipt, attach current structure/audit/matrix/null/stage checks, signature hashes, exact Java 21 compile result, configuration identity, evidence-result hashes and receipt verification output.

#### B-002 — Canonical acceptance bindings do not resolve

AC-003 named `RunLifecycleTest#orderedExclusiveExecution`, while the implementation method was `orderedExclusiveExecutionAndDuplicateAdmission`. AC-013 named `lifecycle-admission-awt-smoke`, while the implementation was a JUnit method and no separate launcher/output was supplied. AC-012 named `check_slice.py`, which does not exist.

Required: align AC-003 target; either supply the separate AWT smoke or revise AC-013/EV-SMOKE/design to the JUnit implementation; supply `check_slice.py` evidence or remove/revise AC-012 through the specification gate.

### Majors

#### M-001 — Unreachable Stop rejection

The matrix specifies stale/foreign/no-current-run Stop as `STALE_RUN`, and current STOPPING/CLOSING Stop as acknowledged. `StopRejection.RUN_NOT_ACTIVE` is publicly representable but unreachable because stale-run checking occurs first; its branch is dead.

Required: remove `RUN_NOT_ACTIVE` and the unreachable branch; state Stop/close idempotence explicitly.

### Test and evidence gaps

- AC-001 starts a later run but does not exercise old-generation handles against it.
- AC-003 does not release two competing executions through a barrier; it starts A first, then attempts duplicate A.
- AC-007 lacks previous-generation and foreign-handle operation checks, unchanged-state assertions, and meaningful replacement of tautological `assertNotNull` checks.
- AC-008 does not assert pending cancellation or active terminal retention.
- AC-009 covers only some null inputs, mostly while idle, and lacks before/after snapshots for every mutating boundary.
- AC-011 does not retry the same fresh ID from a rejected mixed `[freshB, reusedA]` candidate.
- AC-013 does not assert new-run rejection before admission-winning callback settlement and is not separately launched.
- Matrix branches for Stop/close, foreign/replaced batch execution, stale Stop/finish, foreign snapshot and closing admission need explicit evidence or a scoped reason they are covered by the acceptance scenarios.

### Verified assumptions

- One synchronized owner monitor can linearize lifecycle state and effect admission while callback execution occurs outside the monitor.
- Admission immediately before callback invocation is a valid defined Stop race ordering.
- An executing callback may never return; the truthful state remains STOPPING/CLOSING and finish rejects until settlement.
- Run-wide CallId uniqueness requires runtime history; Java types alone cannot encode it.
- `List.copyOf` plus immutable CallId values provides the required shallow batch snapshot.
- No Kotlin layer is needed for this synchronous core.
- Production lifecycle declarations use Java-21-compatible records, sealed interfaces, enums and collection APIs.

### State

Review status: FINDINGS_READY_FOR_ADDRESS
Reviewer checklist: COMPLETE
Frozen basis: INVALIDATED
Peer agreement: openai-codex/gpt-5.6-sol:medium + none
Peer exchanges used: 0
Open Blockers: 2
Open Majors: 1
Deferred Minors/Nits: 0
Verification: FAIL (exact-basis evidence absent; local plugin build passed, test executor failed before test execution)
Review rounds: 2
Next permitted action: address B-001, B-002 and M-001; regenerate exact-basis evidence; obtain fresh independent review

This file records the second review result. It is not a passing S2 receipt.

## Round 3

- Reviewer: Claude Fable 5.1, prompt `adversarial-review-prompt.md`.
- Basis: branch `native-agent-workflow`, working tree with the uncommitted diff to `RunLifecycle.java`, `RunLifecycleTest.java`, `plugin-core/build.gradle.kts` (2026-09-14).
- Reference: `pi/packages/agent/src/agent-loop.ts` lines 227–235, 379–404, 442–479, 636–661, 677–718.

### 1. Executive verdict

**FAIL.**

One new Blocker: a checked exception thrown through `Effect.execute()` wedges the owner forever. The state machine has no recovery path. Two round-2 Blockers remain open. The round-2 test gaps are not addressed.

Verified sound: monitor discipline, reentrancy from inside an effect, Stop/complete ordering, handle identity across owners and generations, transactional `beginBatch`, null checks before mutation, snapshot immutability. Gradle run of `RunLifecycleTest`: 12 tests, 0 failures.

### 2. Findings table

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

### 3. Concrete proof

#### B-001 — checked throwable wedges the owner

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

#### B-002 — evidence artifacts absent

`ls .agent-work/native-agent-workflow/lifecycle-admission/` contains only `adversarial-review-prompt.md`, `design.md`, `design-source-path.txt`, `review-basis.md`, `review-round-2.md`, `review-state.md`, `spec.json`. No receipt, no structure-check output, no compile output. `review-state.md` still lists round-2 B-001 as Open.

#### B-003 — acceptance bindings

`find . -name 'check_slice*'` returns nothing. `spec.json` AC-012 target is `check_slice.py`. AC-013 target is `lifecycle-admission-awt-smoke`. The only AWT scenario is the JUnit method `awtAdmissionSmoke`. Only AC-003 was renamed.

#### M-001 — test scenarios do not match spec text

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

#### M-002 — Pi abort signal discrepancy

Pi `agent-loop.ts:677–718` passes `signal` into `tool.execute(...)`, then awaits the tool. A cooperating tool stops early. Pi `agent-loop.ts:476–478` breaks the sequential loop after an aborted call. Pi `agent-loop.ts:708–714` converts a thrown tool error into an error result and continues.

Java lifecycle:

- Nothing new starts after Stop. Matches Pi. Verified by `stopBeforeQueuedEffect` and `awtAdmissionSmoke`.
- The executing effect runs to completion. Stop does not wait. `finishRun` waits. Matches Pi at the driver level.
- The effect receives no stop signal. The only observation path is `lifecycle.snapshot().phase() == STOPPING` polled from inside the callback. Pi tools get the signal as an argument.
- A thrown effect is rethrown to the driver. The next call stays admissible. Matches Pi only if the driver catches and continues. This obligation is not documented on `Effect`.

`spec.json` lists "Unconditional termination of arbitrary callbacks" as a non-goal. Cooperative signal delivery is not unconditional termination. The author can disposition this to the driver layer, but the design must then state where the signal comes from.

Truncated responses (`agent-loop.ts:227–235`, `379–404`): Pi never executes those calls and emits synthetic error results. Layer 1 has no truncation concept. The driver must not call `beginBatch` for a truncated message. Not a lifecycle defect. Record it as a driver obligation.

#### Vectors verified with no finding

- **Vector 1, monitor window.** No monitor is held during `effect.execute()`. Reentrant calls on the same thread take the free monitor. Java monitors are reentrant in any case, so no deadlock is possible. Probe output above.
- **Vector 1, Stop vs complete.** Both run under the monitor. `cancelPending` writes only `PENDING` entries. `markTerminal` requires `EXECUTING`. No interleaving can overwrite a terminal or executing status.
- **Vector 1, detached work.** An effect that spawns a thread returns `COMPLETED` at once. `finishRun` then succeeds. This is the stated assumption "driver calls finishRun only after other owned resources settle". Accepted residual. See N-004.
- **Vector 3.** `isStaleRun` requires `run == currentRun`. `isStaleBatch` requires `batch == currentBatch.handle`. Both are owner-private references. Package code that forges a handle with a random key fails identity. Cross-instance handles fail at both checks. Reflection is out of scope per `decisions.reflection`.
- **Vector 4.** `beginBatch` performs all four checks before `currentBatch` or `acceptedCallIds` change. A rejected mixed batch mutates nothing. `acceptedCallIds` is cleared on `startRun` and `finishRun`. Behavior is correct. The test gap is M-001.
- **Vector 6.** Every public method calls `Objects.requireNonNull` before the monitor. `Call.Batch.of` uses `List.copyOf`, which rejects null elements. `Batch.Snapshot`, `Call.Snapshot`, `Lifecycle.Snapshot.Active` are records over immutable values. `Call.Batch.calls()` returns the `List.copyOf` result. No internal collection leaks.

### 4. Actionable recommendations

#### B-001 — catch every throwable, keep the same object

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

#### B-002 — produce the receipt

Generate and commit under the basis directory: structure-check output, `javac --release 21` output for the exact six sources, signature extraction, source/spec/design/config hashes, and the receipt verification output that LC-012 and EV-RECEIPT require. Update `review-state.md` round-2 B-001 to Addressed with file names.

#### B-003 — align acceptance targets

`spec.json`:

- AC-013 `target`: `RunLifecycleTest#awtAdmissionSmoke`. Update EV-SMOKE to say the scenario runs as a JUnit method on the real `EventQueue` in the same executor, or supply a separate launcher.
- AC-012: supply `check_slice.py` with the described behavior, or remove AC-012 through the specification gate and point EV-STRUCTURE at the real command.

#### M-001 — make the tests execute the spec text

- AC-001: after the second `startRun`, assert `stop(first)`, `finishRun(first)`, `beginBatch(first, ...)` return `STALE_RUN` and `snapshot()` is still `RUNNING`.
- AC-003: start two threads on a `CyclicBarrier(2)` that both call `execute(batch, first, ...)`; assert exactly one `Executed`, one `ALREADY_EXECUTING`, counter 1. Or change the AC-003 text to the deterministic ordering the test already proves.
- AC-007: on `secondLifecycle`, call `execute(firstBatch, firstCall, ...)` and `batchSnapshot(firstBatch)`; assert `STALE_BATCH` and that `secondLifecycle.snapshot()` is unchanged. Replace the two `assertNotNull` lines with those assertions.
- AC-008: before `release.countDown()`, assert `pending` is `CANCELLED_BEFORE_START` and `active` is `EXECUTING`. After `execution.get`, assert `active` is `COMPLETED`.
- AC-009: start a run and a batch, capture `snapshot()` and `batchSnapshot(batch)`, then call `beginBatch(run, null)`, `execute(batch, null, e)`, `execute(batch, call, null)`, `execute(null, call, e)`; assert NPE each time and both snapshots equal the captured values.
- AC-011: rename the ids so the candidate is `[freshB, reusedA]`, then `beginBatch(nextRun, [freshB])` and execute `freshB`.
- New test: reentrant `stop`, `close`, `execute`, `finishRun`, `startRun`, `beginBatch` from inside an effect; assert the probe results listed in section 3.

#### M-002 — decide where the stop signal lives

Option A, lifecycle-owned: change `Effect` to `void execute(StopSignal signal)` where `StopSignal` is a tiny final class with `boolean isRequested()` backed by the owner phase. The owner passes it at admission. Update LC-004/LC-005 and the design table.

Option B, driver-owned: keep `Effect` value-free and argument-free. Add to `design.md` "Scope" and to `Effect` Javadoc: the driver supplies its own cancellation token to tools, and the driver must call `stop(run)` and cancel that token together. Record the deviation from Pi `agent-loop.ts:689` in the parity matrix.

Recommendation: Option B for this slice. It keeps the layer synchronous and value-free. The parity matrix must record it.

#### N-001

`spec.json` operation matrix and `design.md` matrix: change the `execute` row for `CLOSED` to `Rejected(STALE_BATCH)` because no current batch exists after `finishRun`. Keep `STOPPING|CLOSING -> RUN_NOT_ACCEPTING_EFFECTS`.

#### N-002

Accept as a Java limitation, or record the accounting failure through a logger at the driver boundary. Document the limitation in `design.md` next to the rethrow rule.

#### N-003

Optional: drop `ownerKey` from `RunHandle` and `Batch.Handle`, keep reference identity. If kept, state in `design.md` that identity, not the key, is the authority. Make `Batch.Handle.run()` private and pass the `RunHandle` into `isStaleBatch` from `BatchState`.

#### N-004

Add Javadoc to `Effect`:

```java
/**
 * Synchronous callback invoked once, outside the owner monitor, after admission.
 * Return normally to record COMPLETED. Any throwable records FAILED_AFTER_START
 * and is rethrown unchanged. Do not queue or detach work the driver cannot settle
 * before finishRun. Stop is observable only through RunLifecycle.snapshot().
 */
```

#### N-005

Add `gradlew.bat` to `.gitignore` or delete it. Add a comment above `testRuntimeOnly("junit:junit:4.13.2")` that names the platform test framework requirement.

#### N-006

Replace the trailing `assertNotNull` calls with the AC-007 and AC-011 assertions in M-001.

### State

- Review status: FINDINGS_READY_FOR_ADDRESS
- Open Blockers: 3 (B-001 new; B-002, B-003 carried from round 2)
- Open Majors: 2
- Minors/Nits: 6
- Verification: Gradle `:plugin-core:test --tests RunLifecycleTest` passed, 12/12. Standalone probes in the session scratchpad reproduced B-001 and confirmed the safe reentrancy, stale-generation and cross-owner results.
- Next permitted action: fix B-001 with the spec change, produce the receipt for B-002, align AC-012/AC-013 for B-003, close M-001 test gaps, disposition M-002, then obtain a fresh review.

## Appendix — adversarial review prompt

> **Instructions for the Reviewer**:
> You are acting as an **adversarial, zero-trust systems auditor and JVM concurrency expert**. Your objective is **not** to validate or compliment the author's work, but to aggressively probe for race conditions, deadlock vectors, state machine holes, leaky capability abstractions, unhandled throwables, and discrepancies against the reference Pi implementation.

---

### 1. Environment & Exploration Tools

You have access to the repository root at `/home/muku/depot/personal/cli-tools/agentbridge` on branch `native-agent-workflow`.

#### A. Semantic Code Exploration (`idea-facade` MCP)
Connect to the `idea-facade` MCP server to explore the IntelliJ project workspace:
- Uses IntelliJ's semantic index to find symbols, inspect types, resolve references, and read project files with live buffer sync.
- Relevant project source path: `plugin-core/src/main/java/com/github/catatafishen/agentbridge/nativeagent/lifecycle/` and test path: `plugin-core/src/test/java/.../lifecycle/`.

#### B. Fast Codebase Search across Reference Repos (`codeq`)
Use `codeq` from the command line to inspect slow-moving reference repositories:
```bash
# Explore reference Pi codebase (/home/muku/depot/personal/cli-tools/pi)
codeq pi sym runLoop
codeq pi outline packages/agent/src/agent-loop.ts
codeq pi text "isTruncated"

# Explore IntelliJ Community platform sources (/home/muku/depot/personal/cli-tools/intellij-community)
codeq ij sym ApplicationManager
codeq ij sym ProgressIndicator
codeq ij text "JUnit5TestSessionListener"
```

---

### 2. Required Reading List

Before inspecting the code, read the following authoritative documents in order:

1. **The Product Scope & Boundary**:
   - `native-agent-docs/product.md`: Pi fidelity, decisions, invariants, tool catalog.
   - `native-agent-docs/product.md` Out of scope, and `native-agent-docs/workflow.md` gates.
2. **The Formal Specification & Design**:
   - `native-agent-docs/lifecycle-admission/spec.json`: Canonical requirements `LC-001` through `LC-012`, acceptance criteria `AC-001` through `AC-013`.
   - `native-agent-docs/lifecycle-admission/design.md`: Java surface, state transitions, admission algorithm, operation-by-phase matrix, type-safety audit.
3. **Previous Review Audit (Round 2 Findings)**:
   - This file, all earlier rounds: check whether earlier blockers were truly addressed or just masked.

---

### 3. Implementation Code Under Audit

Audit these exact Java 21 files on branch `native-agent-workflow`:
- `plugin-core/src/main/java/com/github/catatafishen/agentbridge/nativeagent/lifecycle/RunLifecycle.java`
- `plugin-core/src/main/java/com/github/catatafishen/agentbridge/nativeagent/lifecycle/Lifecycle.java`
- `plugin-core/src/main/java/com/github/catatafishen/agentbridge/nativeagent/lifecycle/Call.java`
- `plugin-core/src/main/java/com/github/catatafishen/agentbridge/nativeagent/lifecycle/Batch.java`
- `plugin-core/src/main/java/com/github/catatafishen/agentbridge/nativeagent/lifecycle/Effect.java`
- `plugin-core/src/main/java/com/github/catatafishen/agentbridge/nativeagent/lifecycle/RunHandle.java`
- `plugin-core/src/test/java/com/github/catatafishen/agentbridge/nativeagent/lifecycle/RunLifecycleTest.java`
- `plugin-core/build.gradle.kts`

---

### 4. Adversarial Audit Vector Checklist

Conduct your attack across these six specific failure vectors:

#### Vector 1: Concurrency, Monitors & Reentrancy
- **The Execution Lock Window**:
  In `RunLifecycle.java`, `execute(batch, call, effect)` acquires `synchronized (this)`, marks the call as `EXECUTING`, and releases the lock before `effect.execute()`.
  - *Attack*: What happens if `effect.execute()` invokes `RunLifecycle.stop()`, `RunLifecycle.finishRun()`, or `RunLifecycle.execute()` reentrantly on the same thread? Does the JVM reentrant monitor cause deadlock or state corruption?
  - *Attack*: What if `stop(run)` is called concurrently while `complete()` or `recordFailure()` is reacquiring the monitor? Is there any window where a terminal status is overwritten or misreported?
  - *Attack*: What if `effect.execute()` spawns background threads or asynchronous tasks that outlive `execute()`? Can `finishRun()` be tricked into settling early?

#### Vector 2: Exception Accounting & Suppressed Throwables
- In `recordFailure()`, the code catches `RuntimeException | Error`, attempts to record `FAILED_AFTER_START`, and rethrows.
  - *Attack*: What happens if `complete()` throws an unexpected `IllegalStateException` or `Error` during failure recording? Does it swallow the original throwable, or does `failure.addSuppressed()` guarantee the root cause is preserved?
  - *Attack*: What if the callback throws a checked exception wrapped or unsafely cast via `Unsafe` or generic erasure? Does `execute()` catch it, or does it bypass status recording?

#### Vector 3: Capability Token Safety & Forgery
- `RunHandle` and `Batch.Handle` are opaque capability tokens:
  - *Attack*: Are package-private constructors sufficient in Java? Can an attacker in the same package (or via reflection) forge a handle?
  - *Attack*: Check `ownerKey == expectedOwnerKey`. If two `RunLifecycle` instances exist in the same JVM, can a `Batch.Handle` from Instance A be passed to Instance B? Does `isStaleBatch` completely block cross-instance leakage?

#### Vector 4: Batch Atomicity & Mutation Poisoning
- In `beginBatch(RunHandle, Call.Batch)`:
  - *Attack*: If validation fails (e.g. `CALL_ID_ALREADY_ACCEPTED` or `PREVIOUS_BATCH_UNSETTLED`), verify whether `acceptedCallIds` or `currentBatch` mutated at all.
  - *Attack*: If candidate batch `[freshA, reusedB]` is rejected, can `freshA` be admitted in a subsequent batch, or did the rejection poison `freshA`?

#### Vector 5: Discrepancy with Reference Pi Implementation
- Run `codeq pi` on `/home/muku/depot/personal/cli-tools/pi`:
  - Inspect `packages/agent/src/agent-loop.ts:226-240` (truncated calls).
  - Inspect `packages/agent/src/agent-loop.ts:409-485` (sequential execution and abort).
  - *Attack*: Does our Java lifecycle faithfully implement Pi's semantics? Are there cases where Pi aborts but our Java owner would allow subsequent calls to proceed?

#### Vector 6: Residual Type Obligations & Null Boundaries
- The spec claims that invalid states are impossible by construction using Java 21 sealed types and records.
  - *Attack*: Check every public method. What happens if a caller passes `null` for `RunHandle`, `Call.Id`, `Batch.Handle`, or `Effect`? Does any method mutate internal state before throwing `NullPointerException`?
  - *Attack*: Check `Batch.Snapshot` and `Lifecycle.Snapshot`. Are they truly immutable, or do they leak mutable collections or internal state references?

---

### 5. Required Deliverable & Output Format

Deliver your findings strictly using this structure:

1. **Executive Verdict**:
   - `PASS`: Design and implementation are sound, robust, and verified.
   - `FAIL`: Unresolved blockers or majors exist.
2. **Findings Table**:
   - **Blockers (B-XXX)**: Definite race conditions, concurrency deadlocks, unhandled exception leaks, or invariant violations.
   - **Majors (M-XXX)**: State machine omissions, potential API misuse vectors, incomplete test scenarios.
   - **Minors/Nits (N-XXX)**: Documentation sync, test assertion improvements, code style.
3. **Concrete Proof / Reproduction**:
   - For every Blocker and Major, provide a concrete sequence of thread calls or code snippet demonstrating the failure.
4. **Actionable Recommendations**:
   - Exact code diff or design revision required to resolve each finding.
