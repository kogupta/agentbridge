# Lifecycle/admission review ledger

Append-only. Rounds are in chronological order. Add a new round or a Resolution section at the end; never rewrite earlier rounds. File paths inside rounds 1–3 are as they were at review time (`.agent-work/native-agent-workflow/lifecycle-admission/…`, now `native-agent-docs/lifecycle-admission/…`). The round-2 frozen basis dump (`review-basis.md`) was a copy of other files and is removed; regenerate a basis from git when needed.

## Current state

- Review status: FINAL_GATE_PASS
- Latest round: 4, targeted recheck passed on 2026-09-14
- Open Blockers: 0
- Open Majors: 0
- Deferred Minors/Nits: 1 (round 3 N-003)
- Frozen basis: CURRENT
- Next permitted action: S3 implementation qualification

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

## Round 3 — Resolution

Appended after the round-3 ledger and prompt. Round-3 text above is unchanged.

Classification: B-001, M-002, N-001, N-002, N-004 are bounded defects (A). B-002, B-003, M-001, N-005, N-006 are missing proof (B). N-003 is deferred. No finding required replanning (C). The fix does change a normative requirement (LC-006) and one matrix row, so a full review is reopened rather than a targeted gate.

- **B-001 — Resolved.** `RunLifecycle.execute` and `recordFailure` now catch `Throwable` and rethrow the same object through a generic unchecked rethrow. Regression: `RunLifecycleTest#terminalAccounting` throws a checked `IOException` through a sneaky rethrow. Before the fix it failed with `expected: <FAILED_AFTER_START> but was: <EXECUTING>`; after the fix it passes and asserts the same object is rethrown and a second attempt is `ALREADY_TERMINAL`. Spec: LC-006 statement, AC-006 `when`, LC-006 audit residual obligation now say any Throwable. `design.md` State and transitions updated.
- **B-002 — Resolved.** `scripts/native-spec/check_slice.py receipt` produces the S1 receipt: strict structure check (EV-STRUCTURE), `javac --release 21` on the six lifecycle sources (EV-DESIGN-COMPILE), `javap -package` signatures (EV-DESIGN-SOURCE), spec/design/declaration/configuration hashes and evidence-result hashes, and a digest over the `receipt_identity` fields. `check_slice.py verify --receipt <path>` recomputes everything and rejects drift (EV-RECEIPT). The receipt is generated output under the ignored `.agent-work/native-agent-docs/lifecycle-admission/s1-receipt.json`, per `workflow.md` Artifacts; regenerate it rather than trusting a stored copy. Result on the resolved basis: status PASS, javac 25.0.2 with `--release 21`, no compiler output, digest `6a54d476…d5e0`, verify PASS with no mismatches. The spec evidence descriptions now name the commands.
- **B-003 — Resolved.** AC-012 target is `scripts/native-spec/check_slice.py selftest`; AC-013 target is `RunLifecycleTest#awtAdmissionSmoke`; EV-SMOKE describes the JUnit method on the real AWT event dispatch thread. `check` rule T002 now fails if an acceptance target test method or script does not exist. `selftest` result: 7/7 PASS — intact basis passes; removed LC-004 audit row rejected (A001); unchanged basis verifies; altered source rejected (`declaration_hashes`); altered design rejected (`design_hash`); edited receipt rejected (`digest`); receipt carries no behavior evidence IDs.
- **M-001 — Resolved.** Tests now execute the spec text:
  - AC-001 `singleRunOwnership`: old-generation handle gets `STALE_RUN` from stop, finish and beginBatch; the new run stays `RUNNING` and still admits a batch.
  - AC-003 `orderedExclusiveExecution`: B before A is `OUT_OF_ORDER`; two threads released by a `CyclicBarrier` execute A; exactly one `Executed`, one `ALREADY_EXECUTING`, one callback entry; B blocked while A runs, then executes once; A again is `ALREADY_TERMINAL`.
  - AC-007 `staleHandleIsolation`: foreign run and batch handles are rejected by beginBatch, execute, batchSnapshot, stop and finishRun with no callback and unchanged foreign-owner lifecycle and batch snapshots; replaced batch handle is `STALE_BATCH` for execute and query; previous-generation run and batch handles are rejected after a new run starts, with the new run unchanged.
  - AC-008 `closeDrainsWithoutReopening`: while the active call runs, `active` is `EXECUTING`, `pending` is `CANCELLED_BEFORE_START` and finish is `BATCH_UNSETTLED`; after release `active` is `COMPLETED`; closed owner rejects start, execute and stop and stays `CLOSED`.
  - AC-009 `nullInputDoesNotMutate`: `Call.Id`, `Call.Batch.of` (list and element), and every null argument of beginBatch, execute, stop, finishRun and batchSnapshot, both idle and with an active run and batch; lifecycle and batch snapshots are compared after each call; no callback runs; the owner stays usable.
  - AC-011 `multipleBatchesPreserveIdentity`: pending finish rejected; `[freshB, reusedA]` rejected in the same run without changing the settled batch; `[freshB]` then begins and executes; finish returns `IDLE` and a new run starts.
  - Reentrancy (new `reentrantOperationsFromEffect`): from inside an effect, execute is `ALREADY_EXECUTING`/`OUT_OF_ORDER`, beginBatch `PREVIOUS_BATCH_UNSETTLED`, finish `BATCH_UNSETTLED`, start `BUSY`, stop and close succeed; the outer call records `COMPLETED`, the pending call is cancelled and finish reaches `CLOSED`.
- **M-002 — Resolved (documented driver ownership, reviewer option B).** No API change. `Effect` Javadoc, `design.md` Scope and `product.md` Pi fidelity now state that the driver owns a cooperative cancellation token, cancels it together with `stop(run)`, and converts rethrown effect failures into tool error results as Pi does at `agent-loop.ts:708-714`.
- **N-001 — Resolved.** Operation matrix splits the execute row: `STOPPING|CLOSING` → `RUN_NOT_ACCEPTING_EFFECTS`; `IDLE|CLOSED` → `STALE_BATCH`. Covered by `closeDrainsWithoutReopening`.
- **N-002 — Resolved (documented).** `design.md` records that a throwable built with suppression disabled drops an accounting failure.
- **N-003 — Deferred.** Removing `ownerKey` or hiding `Batch.Handle.run()` changes package-internal representation without closing an observable hole; reference identity already rejects forged and foreign handles, as the round-3 probes and `staleHandleIsolation` show. Residual risk: none observable outside the package.
- **N-004 — Resolved.** `Effect` has contract Javadoc: synchronous, outside the monitor, throwable accounting, no detached work, no stop signal.
- **N-005 — Resolved.** `gradlew.bat` no longer exists. The JUnit 4 runtime dependency now has a comment. Verified by removing it: the test executor fails to start with `ServiceConfigurationError: org.junit.platform.launcher.LauncherSessionListener: Provider com.intellij.tests.JUnit5TestSessionListener could not be instantiated` caused by `ClassNotFoundException`.
- **N-006 — Resolved.** The tautological `assertNotNull` calls were replaced by the AC-007 and AC-011 assertions above.

Verification on the resolved basis:

| Check | Result |
|---|---|
| `./gradlew :plugin-core:test --tests '…RunLifecycleTest'` | 13 tests, 0 failures, 0 errors |
| Same, `--rerun` five times | 5/5 BUILD SUCCESSFUL |
| `./gradlew :plugin-core:test :plugin-core:buildPlugin` | BUILD SUCCESSFUL |
| `python3 scripts/native-spec/check_slice.py check` | PASS, no findings |
| `python3 scripts/native-spec/check_slice.py selftest` | 7/7 PASS |
| `check_slice.py receipt` then `verify` | PASS, no mismatches |
| `git diff --check` | clean |

Not run: EV-SURFACE and EV-BUILD as IDE tools (S3 evidence), and no fresh independent review.

- Address status: COMPLETE
- Implementation review status: REOPEN_FULL_REVIEW
- Reviewer checklist: COMPLETE (round 3)
- Frozen basis: INVALIDATED (LC-006, AC-006, AC-012, AC-013, EV-SMOKE, evidence command text, execute matrix row changed)
- Open Blockers: 0
- Open Majors: 0
- Deferred Minors/Nits: 1
- Verification: PASS
- Final gate: FAIL (no review of the changed basis yet)
- Stop reason: normative spec rows changed during address
- Next permitted action: fresh independent review of the current basis

## Round 4

### Peer agreement

- Primary reviewer: `openai-codex/gpt-5.6-sol` (current fresh session).
- Independent peer: none, explicitly selected by user.
- Peer exchanges permitted/used: 0/0.

### Frozen review basis

Basis ID: `CAP-LIFECYCLE-ADMISSION@5b2c73c42f9dc322bb09107e44f4bad06a0ef50a`. Frozen before the exhaustive pass. Rows may move from `PENDING` to `PASS`, `GAP`, or `UNSUPPORTED BY CONTRACT`; no new review dimension may be added without escape analysis or an explicit reopen.

#### Commit and scope manifest

- Branch/head: `native-agent-workflow` at `5b2c73c42f9dc322bb09107e44f4bad06a0ef50a`. Repository state was clean before this Round 4 ledger append.
- Reopened address range: `c84afddea0c8e19089a22d649f8ddc76c9a6c5ae..5b2c73c42f9dc322bb09107e44f4bad06a0ef50a`; production lifecycle history also includes `fde1d58e5a83f6ba3aa54b8312d2df88adeab033` and `457e947b5c543ad6b7019fdbf41331dd8c00f098`.
- Reviewed contract: `native-agent-docs/lifecycle-admission/spec.json`, `design.md`, relevant `product.md`/`workflow.md` clauses, and this append-only ledger.
- Reviewed implementation: six Java files under `plugin-core/src/main/java/com/github/catatafishen/agentbridge/nativeagent/lifecycle/`, `RunLifecycleTest.java`, `plugin-core/build.gradle.kts`, and `scripts/native-spec/check_slice.py`.
- Changed production symbols in scope: `RunLifecycle` lifecycle operations and nested closed outcomes; `Lifecycle` snapshots/phases; `Call` identities, batches, statuses and snapshots; `Batch` handles/snapshots/observations; `Effect`; `RunHandle`.
- Planned boundary: workflow S1 receipt, S2 exact-basis independent review, then S3 lifecycle implementation qualification.
- Excluded: provider loop, tool codec, Kotlin driver, UI/PSI integration, unrelated donor-removal/build/docs commits, and the Round 4 ledger-only mutation.

#### Requirement traceability matrix

| Rows | Contract and implementing evidence | Initial state |
|---|---|---|
| LC-001 / AC-001 | `startRun`, `finishRun`, stale-generation rejection; `singleRunOwnership` | PENDING |
| LC-002 / AC-002 | `Call.Id`, `Call.Batch.of`; `validatedImmutableBatch` | PENDING |
| LC-003 / AC-003 | `beginBatch`, `rejectExecution`, `BatchState`; `orderedExclusiveExecution` | PENDING |
| LC-004 / AC-004, AC-013 | `execute`/`stop` monitor boundary; executor and real-AWT race scenarios | PENDING |
| LC-005 / AC-005 | `stop`, pending cancellation, explicit settlement; `stopDuringEffect` | PENDING |
| LC-006 / AC-006 | `execute`, `recordFailure`, unchecked same-object rethrow; `terminalAccounting` | PENDING |
| LC-007 / AC-007 | owner/generation identity and handle-free snapshots; `staleHandleIsolation` | PENDING |
| LC-008 / AC-008 | `close`, closing drain, permanent closed state; `closeDrainsWithoutReopening` | PENDING |
| LC-009 / AC-009 | closed non-generic API and pre-mutation null checks; signature receipt and `nullInputDoesNotMutate` | PENDING |
| LC-010 / AC-010 | callback outside monitor and immutable snapshots; `nonblockingStopAndImmutableSnapshots` | PENDING |
| LC-011 / AC-011 | run-wide ID set and transactional batch replacement; `multipleBatchesPreserveIdentity` | PENDING |
| LC-012 / AC-012 | strict checker, S1 receipt, altered-basis selftests and exact verification | PENDING |
| Driver obligations | cooperative cancellation, thrown-effect conversion, no detached work, no truncated-call admission | PENDING |

#### Behavior grammar matrix

| Dimension | Frozen cells | Initial state |
|---|---|---|
| Lifecycle phases | IDLE, RUNNING, STOPPING, CLOSING, CLOSED | PENDING |
| Public operations | startRun, beginBatch, execute, stop, finishRun, close, snapshot, batchSnapshot | PENDING |
| Handle identity | current, foreign owner, previous generation, replaced batch, package-forged inert handle | PENDING |
| Batch/call shape | null, blank, empty, duplicate, caller-mutated, fresh, reused, mixed fresh/reused, unknown call | PENDING |
| Call status/order | PENDING, EXECUTING, COMPLETED, FAILED_AFTER_START, CANCELLED_BEFORE_START; next, later, repeated | PENDING |
| Concurrency/reentrancy | two starts, duplicate execute, Stop-before-admission, admission-before-Stop, blocked callback, reentrant operations | PENDING |
| Effect completion | normal return, RuntimeException, Error, checked sneaky throw, accounting failure, non-returning/detached work | PENDING |
| Observation | current/old lifecycle snapshot, current/replaced/foreign batch snapshot, collection mutation | PENDING |
| Evidence failure | malformed spec, missing audit/target, altered source/design/config/receipt, unavailable compiler | PENDING |

#### Change-impact map

| Changed area | Entry points and neighboring contracts to preserve | Initial state |
|---|---|---|
| `RunLifecycle` owner state | every public operation, shared monitor, `BatchState`, all nested result algebras | PENDING |
| `Effect` throwable contract | Java/Kotlin callers, driver cancellation/error conversion, terminal accounting | PENDING |
| Identity/value types | constructors/factories, collection ownership, package visibility, snapshot payloads | PENDING |
| Spec/design | LC/AC bindings, operation/null matrices, product invariants I3/I4/I8, workflow S1-S3 | PENDING |
| `check_slice.py` | strict parsing, target discovery, source/config hashing, javac/javap, tamper rejection | PENDING |
| Test/build wiring | JUnit Platform/IntelliJ listener runtime, focused test executor, module build | PENDING |

#### Per-commit/stage contract audit

| Boundary | Required independent outcome | Initial state |
|---|---|---|
| S1 | current receipt binds exact spec/design/sources/config and structural/compile/signature evidence | PENDING |
| S2 | this exact-basis review covers every LC/AC/matrix/audit row with no open Blocker/Major | PENDING |
| S3 | focused lifecycle tests, real-AWT smoke, IDE build and semantic surface inspection pass | PENDING |
| Address commits | throwable fix, checker/receipt, review dispositions, and final doc corrections each preserve earlier passing rows | PENDING |

#### Test-strength audit

Each AC test must fail at least one plausible wrong implementation: accepting a second/stale owner; aliasing or malformed batches; admitting later/duplicate calls; checking Stop before queueing rather than at execution; cancelling executing work; catching only unchecked throwables or changing throwable identity; accepting foreign/replaced handles; reopening after close; mutating before null rejection; holding the monitor during callback; poisoning a fresh ID on rejected mixed admission; accepting stale/tampered evidence; or treating AWT queue admission as equivalent to pre-enqueue admission. Initial state: PENDING.

#### Weak-implementer traces

1. Fresh run → batch `[A,B]` → A admitted → Stop → B cancelled → A throwable/normal settlement → finish; trace every branch and neighboring no-Stop path.
2. Settled batch A → reject `[freshB,reusedA]` without mutation → accept/execute freshB; compare foreign, replaced and previous-generation handles.
3. Valid basis → receipt → verify; mutate audit, source, design, config and digest separately; require rejection without claiming behavior evidence.

Frozen basis status: CURRENT; exhaustive pass not yet started.

### Executive verdict

**FAIL.** The lifecycle state machine itself satisfies the reviewed LC-001 through LC-011 transitions, including the any-`Throwable` repair. S2 cannot pass. The driver failure policy now conflicts with the product authority, the S1 verifier accepts a tampered evidence payload, the structural checker does not enforce its unknown-field contract, required public outcomes remain untested, and the checker writes temporary trees outside the repository workspace.

### Findings table

| ID | Severity | Frozen-basis row | Summary |
|---|---|---|---|
| R4-B-001 | Blocker | Driver obligations; `Effect` impact | Product says a side-effect failure stops automatic continuation; design says the driver continues; `Effect` leaves the choice open. |
| R4-B-002 | Blocker | LC-012 / AC-012; S1 receipt | `verify` authenticates `evidence_hashes` but never authenticates the stored `evidence` payload those hashes claim to bind. |
| R4-M-001 | Major | LC-012 strict structure | `check` rejects unknown top-level fields only; unknown fields inside normative rows pass despite the workflow contract. |
| R4-M-002 | Major | LC-002, LC-004, operation grammar, test strength | AC-013 and several public invariant/matrix outcomes have no regression assertion. |
| R4-M-003 | Major | Checker impact; repository workspace contract | Receipt compilation and selftest use default system temporary directories instead of `.agent-work/`. |
| R4-N-001 | Minor | Evidence failure grammar | Missing or malformed receipt input escapes as an uncaught exception instead of the documented JSON tool error with exit 2. |

### Escape analysis — R4-B-001

- New material evidence since Round 3: yes. The Round 3 address pass added the driver-owned failure/continuation prose.
- Frozen-basis row that should have caught it: Round 3 M-002 disposition and the product/design parity row; Round 4 `Effect` change-impact row.
- Why Round 3 marked that row complete: it checked cancellation-token ownership and Pi's thrown-error conversion, but did not compare the new prose with the product run-behavior table.
- Classification: FIX_REGRESSION.
- Required workflow correction: treat `product.md` as authority and align `design.md`, `Effect` Javadoc and the resolution record, or reopen the product decision explicitly before choosing Pi-style continuation.

### Escape analysis — R4-B-002

- New material evidence since Round 3: yes. `check_slice.py` and the receipt format were created in the address pass after the Round 3 review.
- Frozen-basis row that should have caught it: LC-012 / EV-RECEIPT tamper rejection and the receipt weak-implementer trace.
- Why Round 3 marked that row complete: the new selftest altered an identity field and checked the digest; it never altered an attached evidence result while preserving the identity fields.
- Classification: FIX_REGRESSION.
- Required workflow correction: add stored-evidence authentication and a payload-tamper selftest before rerunning the targeted gate.

### Escape analysis — R4-M-001

- New material evidence since Round 3: yes. The structural checker was created after the Round 3 review.
- Frozen-basis row that should have caught it: LC-012 strict structure and workflow `Mechanical checks (spec-lint)`.
- Why Round 3 marked that row complete: the selftest removed one audit row; it did not inject an unknown field below the top level.
- Classification: FIX_REGRESSION.
- Required workflow correction: freeze allowed/required fields for every normative record and add one nested unknown-field mutation to selftest.

### Escape analysis — R4-M-002

- New material evidence since Round 3: no for the missing AC-013 settlement assertion; the Round 3 finding already named it. The other uncovered invariant/matrix rows were also readable from the same spec and tests.
- Frozen-basis row that should have caught it: requirement-to-test traceability, operation-by-phase matrix coverage and test-strength audit.
- Why Round 3 marked that row complete: the resolution summarized changed tests without tracing each acceptance sentence and matrix outcome back to an assertion.
- Classification: REVIEW_MISS.
- Required workflow correction: maintain an explicit matrix-outcome-to-assertion checklist in the targeted gate and require each public rejection/transition to fail a plausible wrong implementation.

### Escape analysis — R4-M-003

- New material evidence since Round 3: yes. The checker introduced both default `TemporaryDirectory()` calls after the review.
- Frozen-basis row that should have caught it: checker change-impact and repository workspace compliance.
- Why Round 3 marked that row complete: receipt behavior was checked, but its filesystem boundary was not.
- Classification: FIX_REGRESSION.
- Required workflow correction: add workspace-path inspection for development tools that create temporary files.

### Concrete proof and required corrections

#### R4-B-001 — contradictory failed-effect continuation policy

- `native-agent-docs/product.md:3` declares `product.md` the single product authority.
- `product.md:247` says `FAILED_AFTER_START` has “No automatic continuation; user inspects.”
- `native-agent-docs/lifecycle-admission/design.md:13` assigns the driver conversion **and continuing**, as Pi does.
- `Effect.java:6-8` says the driver decides whether the run continues.

These are three different policies: mandatory stop, mandatory Pi-style continuation, and driver discretion. The lifecycle permits the next call after a failed effect settles, so the future driver needs one answer before it can use the API safely.

Correction: preserve the product-authoritative safety rule unless the product gate is deliberately reopened. State that the driver records/converts the failed result but does not automatically execute later calls after a side-effect failure. Update design and `Effect` Javadoc together. Add a driver acceptance row before integration; this slice can document it because the driver remains out of scope.

#### R4-B-002 — evidence payload is not covered by its hashes

`build_receipt` hashes each evidence object into `evidence_hashes` (`check_slice.py:275-291`). `verify_receipt` checks the digest and compares identity fields with a fresh receipt (`300-314`), but never recomputes hashes from `stored["evidence"]`.

Reproduction against the frozen basis:

```text
receipt.evidence["EV-DESIGN-COMPILE"]["status"] = "FAIL"
receipt.evidence["EV-DESIGN-COMPILE"]["output"] = "forged output"
verify_receipt(...) -> []
reported verification -> PASS
```

The untouched `evidence_hashes`, identity digest and current-basis comparison all pass while the attached compile evidence is forged. This violates LC-012's evidence-result binding and W2's fail-closed exact receipt.

Correction: require exactly the expected evidence keys; recompute `sha256(canonical(stored evidence item))` for every stored payload; compare those hashes with stored `evidence_hashes` before comparing the current basis. Add selftests for changed evidence status/output, missing evidence, extra evidence and mismatched evidence hash.

#### R4-M-001 — nested unknown fields pass the strict checker

`check_spec` computes `unknown = sorted(set(spec) - set(REQUIRED_TOP))` only at `check_slice.py:90-95`. Requirements, acceptance rows, audit rows, evidence rows, stage bindings, operation rows, receipt identity and null-boundary rows have no allowed-field validation. `workflow.md:109` requires unknown fields to be rejected.

Reproduction: add `"unknown_typo_field": true` to LC-001. `check_spec` returns no findings and reports PASS.

Correction: define finite allowed/required keys per record kind and emit a stable diagnostic for every unknown nested field. Selftest at least one normative row and one nested stage binding. Also require `RunLifecycleTest#method` targets to resolve to `@Test` methods, not any `void` helper matched by regex.

#### R4-M-002 — required outcomes can regress without a test failure

- AC-013 says the owner cannot start another run until the admission-winning AWT callback settles (`spec.json:177-180`). `awtAdmissionSmoke` stops at `RunLifecycleTest.java:424`, releases at `425`, and observes completion at `426-427`; it never calls `startRun` or `finishRun` before release. Deleting the busy/unsettled protection would not fail this scenario.
- LC-002 requires nonblank IDs (`spec.json:25-29`), but `validatedImmutableBatch` covers empty batch, nulls, duplicates and aliasing only (`RunLifecycleTest.java:46-60`). Deleting `Call.Id.value().isBlank()` would leave all 13 tests green.
- No test mentions `RUN_NOT_ACCEPTING_BATCHES` or `UNKNOWN_CALL`. A wrong rejection or accidental admission in those public matrix cells would remain green.
- No test invokes `stop` twice with the current handle in STOPPING or CLOSING, although the matrix and design promise idempotent acknowledgement.

Correction: extend the existing cohesive scenarios. In the AWT admission-wins half, assert `startRun -> BUSY` and `finishRun -> BATCH_UNSETTLED` before release. Add empty and whitespace-only `Call.Id` cases. Assert unknown current-batch call rejection with zero callback entry. Assert current-run `beginBatch` rejection in STOPPING and CLOSING. Assert repeated Stop returns acknowledged snapshots without mutation in both phases.

#### R4-M-003 — temporary work escapes the project workspace

`check_slice.py:251` and `:342` call `tempfile.TemporaryDirectory()` with no `dir`. They therefore use the process default, normally `/tmp`, while the repository contract requires all temporary files under `.agent-work/`. The receipt command copies source/config into and compiles classes outside the project-controlled workspace.

Correction: create a stable `.agent-work/native-agent-spec/` parent and pass it as `dir` for both temporary directories. Keep automatic cleanup. Add a selftest assertion that each scratch root resolves under `.agent-work/`.

#### R4-N-001 — tool failures do not use the documented protocol

The module documents exit 2 for tool/environment failure (`check_slice.py:12`). `main` catches only `ToolError` (`403-428`). `verify --receipt .agent-work/does-not-exist.json` raises `FileNotFoundError` from `load_strict` instead of returning JSON `status=ERROR`.

Correction: translate expected `OSError`/parse failures at command boundaries into `ToolError` and add missing/malformed-receipt command tests.

### Frozen-basis disposition

#### Requirement traceability

- PASS: LC-001, LC-003, LC-005 through LC-011 implementation behavior and AC bindings.
- GAP: LC-002 proof omits nonblank IDs; LC-004/AC-013 omits pre-settlement new-run rejection; LC-012 receipt/strict-check enforcement fails R4-B-002 and R4-M-001.
- GAP: driver obligations conflict under R4-B-001.

#### Behavior grammar

- PASS: lifecycle phases/transitions in source; current/foreign/old/replaced identity; ordered status transitions; normal/unchecked/checked throwable accounting; immutable observations; Stop/admission monitor order and reentrancy.
- GAP: blank Call.Id, unknown call, STOPPING/CLOSING batch admission and repeated Stop lack regression proof.
- GAP: evidence grammar accepts a forged stored payload and unknown nested fields.
- UNSUPPORTED BY CONTRACT: unconditional callback termination and discovery of detached work; safety remains truthful while an effect never returns.

#### Change impact and stage audit

- PASS: `RunLifecycle`, identity/value types and throwable settlement source behavior. Text search found no production consumer outside the six declarations; only `RunLifecycleTest` uses the owner in this branch.
- GAP: `Effect`/driver policy, spec/checker enforcement and acceptance strength.
- S1: FAIL because EV-RECEIPT and strict structure are unsound despite the current receipt verifying.
- S2: FAIL with two Blockers and three Majors.
- S3: PARTIAL. The Gradle lifecycle run configuration and IDE build pass, but S2 is not satisfied and the native direct test target failed before discovery.
- Address commits: FAIL; the address pass introduced R4-B-001, R4-B-002, R4-M-001 and R4-M-003, and did not close the known AC-013 assertion gap.

#### Test-strength and weak-implementer traces

- Trace 1 (run/batch/Stop/settlement) and Trace 2 (mixed-ID atomicity and stale identity): PASS in source and focused tests.
- Trace 3 (receipt tamper): FAIL for stored evidence payload tampering.
- Test strength: GAP under R4-M-002.

### Verified invariants

- One owner monitor linearizes run, batch, effect admission, Stop, close and completion mutations. No monitor is held during `Effect.execute`.
- Any callback `Throwable`, including a checked sneaky throw, reaches failure accounting and is rethrown as the same object. Public operations cannot replace the executing batch before accounting.
- Stop/close mutate only PENDING calls; executing and terminal statuses are retained.
- `beginBatch` validates unsettled state and all reused IDs before installing a batch or mutating the run-wide accepted-ID set.
- Run and batch identity checks reject foreign, replaced and previous-generation handles. Snapshots contain immutable values and no handles.
- Public mutating arguments are null-checked before owner mutation. Lifecycle production imports are limited to same-package and `java.util` types.
- Current source receipt digest is `d03e8ab72d9d774f93c09ea6dd5150d82140a2f77e6768ba3160bc1a324ced80`; its six declaration hashes match the frozen tree. This does not cure R4-B-002.

### Verification observed

| Check | Result |
|---|---|
| Repository basis before Round 4 append | branch `native-agent-workflow`, head `5b2c73c…`, clean |
| Current `check_spec` implementation | PASS, no findings |
| Current `selftest` implementation | 7/7 PASS |
| Current receipt rebuild and stored receipt verification | PASS, digest `d03e8ab7…ed80`, no mismatches |
| Adversarial evidence-payload mutation | **Verifier incorrectly PASS**, no mismatches |
| Adversarial nested unknown-field mutation | **Checker incorrectly PASS**, no findings |
| Existing Gradle `RunLifecycleTest` run configuration | exit 0 |
| Native direct class test target | FAIL before discovery: IntelliJ `JUnit5TestSessionListener` could not load `junit.framework.TestCase`; reported separately as a tool/classpath problem |
| IDE incremental build | PASS, no errors |
| IDE diagnostics over six sources, test and checker | complete; 0 errors, one unused-public-method warning for `Batch.Snapshot.isSettled` |
| Consumer search | only lifecycle declarations and `RunLifecycleTest`; semantic reference endpoint unavailable, bounded text search used and limitation recorded |
| Working tree after review | only this append-only `review.md` changed |

The Python gate probes invoked the actual `check_slice.py` functions in-process because the IDE could not create a Python run configuration. Disposable probe state lived under `.agent-work/` and was removed. The checker itself still violates that location rule internally under R4-M-003.

Review status: FINDINGS_READY_FOR_ADDRESS
Reviewer checklist: COMPLETE
Frozen basis: CURRENT
Peer agreement: openai-codex/gpt-5.6-sol + none
Peer exchanges used: 0
Open Blockers: 2
Open Majors: 3
Deferred Minors/Nits: 2 (round-3 N-003; R4-N-001 may be fixed or explicitly deferred)
Verification: FAIL
Review rounds: 4
Stop reason: R4-B-001, R4-B-002, R4-M-001, R4-M-002 and R4-M-003 remain open
Next permitted action: ADDRESS_FINDINGS

## Round 4 — Address resolution and targeted recheck

This section closes only R4-B-003 and R4-M-004 against the existing Round 4 frozen basis. It does not reopen the exhaustive review. The earlier Round 4 peer agreement remains applicable: primary `openai-codex/gpt-5.6-sol`, independent peer none, zero peer exchanges.

- **R4-B-003 — Resolved.** Receipt identity now includes `evidence_input_hashes` for `scripts/native-spec/check_slice.py` and every Java test source read for acceptance-target discovery. Verification compares these hashes before rebuilding evidence. The receipt digest therefore changes when either the validator or a target-discovery input changes, even when the resulting structural evidence remains `PASS`. Selftests alter the copied checker and copied target test source independently and require verification rejection. `spec.json` declares `evidence_input_hashes` in `receipt_identity` and `EV-RECEIPT`.
- **R4-M-004 — Resolved.** The checker now applies finite typed schemas to every top-level and nested normative record before semantic traversal. Wrong record containers, list elements, scalar types and nulls produce deterministic structural findings; semantic checks do not traverse structurally invalid rows. Normative values are no longer accepted through string coercion. Selftests cover a string stage-entry container, non-object stage binding, numeric operation-matrix field and non-string acceptance field.

### Targeted recheck evidence

| Check | Result |
|---|---|
| `python3 scripts/native-spec/check_slice.py check` | PASS, no findings |
| `python3 scripts/native-spec/check_slice.py selftest` | 24/24 PASS, including checker drift, target-source drift and four wrong-type cases |
| `python3 scripts/native-spec/check_slice.py receipt` | PASS; Java 21 declaration compile evidence passed; digest `f29d9268d3fe63bbda1e8dc50c0572a8b61d9a095c6c08d27306a27248321d29` |
| `python3 scripts/native-spec/check_slice.py verify --receipt .agent-work/native-agent-docs/lifecycle-admission/s1-receipt.json` | PASS, no mismatches |
| `RunLifecycleTest` Gradle run configuration | PASS, exit 0 |
| IDE incremental build | PASS, zero build errors |
| IDE lint for changed checker and neighboring lifecycle files | PASS, no reported problems |
| Changed-file boundary | `review.md`, `spec.json`, `check_slice.py`; no untracked files |

The new evidence closes the two failed frozen rows: S1 exact-basis identity and LC-012 malformed-spec grammar. The receipt binds its evidence generator and target-discovery inputs. Typed structural validation rejects malformed nested values before semantic access. No lifecycle behavior, architecture, issue scope or stage boundary changed, so the frozen basis remains current.

Address status: COMPLETE
Implementation review status: FINAL_GATE_PASS
Reviewer checklist: COMPLETE
Frozen basis: CURRENT
Peer agreement: openai-codex/gpt-5.6-sol + none
Peer exchanges used: 0
Open Blockers: 0
Open Majors: 0
Deferred Minors/Nits: 1 (round-3 N-003)
Verification: PASS
Final gate: PASS
Review rounds: 4
Stop reason: all Round 4 Blocker and Major rows pass; deterministic targeted gate reached terminal success
Next permitted action: IMPLEMENT_NEXT_STAGE (S3 implementation qualification)

## Round 4 — Resolution

Appended after the round-4 findings. Round-4 text above is unchanged.

Classification: R4-B-001 bounded defect (A); R4-B-002, R4-M-001, R4-M-003 bounded defects in the checker (A); R4-M-002 missing proof (B); R4-N-001 bounded defect (A). No finding required replanning (C). No architecture, stage boundary or LC behavior grammar changed. `product.md` stayed the authority; `design.md`, `Effect` Javadoc and one `spec.json` assumption were aligned to it. So the targeted gate applies, not a full reopen.

- **R4-B-001 — Resolved (product authority kept).** The driver policy is now one rule in three places: after `FAILED_AFTER_START` the driver records that call's result, starts no later call, makes no further provider request, calls `stop(run)` so remaining calls are `CANCELLED_BEFORE_START`, then `finishRun(run)` after cleanup. This matches `product.md` Run behavior "Tool failure after side effect: No automatic continuation; user inspects". `design.md` Scope records it as a deliberate difference from Pi `agent-loop.ts:708-714`, and notes that expected tool failures are returned values, so only unexpected throwables reach `FAILED_AFTER_START`. `Effect` Javadoc states the same rule. `spec.json` `assumptions` gained the driver obligation. The round-3 M-002 resolution text ("converting them into tool error results and continuing … is the driver's job") is superseded by this entry. The lifecycle code needed no change: after a failed call, `stop` cancels the remaining calls, which `reentrantOperationsFromEffect` and `stopDuringEffect` already exercise.
- **R4-B-002 — Resolved.** `verify_receipt` now requires `evidence` and `evidence_hashes` to hold exactly `EV-STRUCTURE`, `EV-DESIGN-COMPILE`, `EV-DESIGN-SOURCE`; recomputes each stored payload hash and rejects a mismatch; and rejects a receipt whose `status` disagrees with its evidence statuses. It checks these before comparing with the current basis. New selftests: forged compile status/output, missing evidence item, extra evidence item, and a mismatched evidence hash with a recomputed digest. Before the fix, the committed checker's `verify_receipt` returned `[]` for the forged payload (reproduced).
- **R4-M-001 — Resolved.** `check` validates exact required/optional fields for requirements, acceptance, audit, evidence, stages, stage bindings, operation-matrix and null-boundary rows, and `receipt_identity`. Unknown nested fields are rule `S005`; missing fields are `S003`. Decision values, audit dimensions, non-goals and assumptions must be nonempty strings; evidence `kind` must be one of `source`, `command`, `review`, `tool`. Acceptance targets `Class#method` now resolve only to `@Test` methods. New selftests: unknown field in a requirement, unknown field in a stage binding, missing `then`, and `RunLifecycleTest#await` as a target. Before the fix, the committed checker returned no findings for both the nested unknown field and the helper-method target (reproduced).
- **R4-M-002 — Resolved.** Added assertions:
  - AC-013 `awtAdmissionSmoke`: before the admission-winning AWT callback is released, the call is `EXECUTING`, `startRun` is `BUSY` and `finishRun` is `BATCH_UNSETTLED`; after settlement `finishRun` reaches `IDLE` and a new run starts.
  - LC-002 `validatedImmutableBatch`: empty and whitespace-only `Call.Id` are rejected.
  - `orderedExclusiveExecution`: unknown call is `UNKNOWN_CALL` with zero callback entries and unchanged batch snapshot.
  - `stopDuringEffect` (`STOPPING`) and `closeDrainsWithoutReopening` (`CLOSING`): repeated `stop` returns an equal acknowledged result, including an equal batch observation, before and after a rejected `beginBatch`; `beginBatch` is `RUN_NOT_ACCEPTING_BATCHES`.
  - Matrix walk (below) found rows the reviewer did not list; added: `startRun` and `execute` in `CLOSING`; `finishRun` and `beginBatch` in `CLOSED`; `finishRun`, `stop`, `beginBatch` and `execute` with old handles in `IDLE`; `close` from `IDLE` (twice) and directly from `RUNNING` with pending cancellation.
  - Test strength, by deliberate one-line bugs applied and reverted one at a time. Every bug was killed: blank ID accepted (`validatedImmutableBatch`); unknown call reported as out of order (`orderedExclusiveExecution`); no `RUNNING` check in `beginBatch` (`stopDuringEffect`, `closeDrainsWithoutReopening`); repeated stop rejected (same two); start allowed while stopping (`awtAdmissionSmoke`, `stopDuringEffect`); idle close enters `CLOSING`, start while `CLOSING` reports `BUSY`, close from `RUNNING` keeps pending calls (all `closeDrainsWithoutReopening`).
- **R4-M-003 — Resolved.** Both temporary directories go through `scratch_dir()`, which creates them under `.agent-work/native-agent-spec/` and cleans them up. Selftest asserts every scratch root resolves under that directory. After the runs, the directory is empty. Disclosure: the reproduction of R4-B-002 ran the committed (pre-fix) receipt builder once, which compiled in the system temp directory.
- **R4-N-001 — Resolved.** Missing or malformed receipts raise a tool error; `main` also maps `OSError`/`ValueError` to JSON `status: ERROR` with exit 2. Selftest covers a missing and a malformed receipt through `main`. Observed: `verify --receipt .agent-work/nope.json` prints a JSON error and exits 2.

### Operation-matrix outcome to assertion checklist

Maintained for the targeted gate, as R4-M-002's escape analysis required. Rows are `spec.json` `operation_matrix` in order.

| # | Operation / phase / precondition → result | Asserting test(s) |
|---|---|---|
| 1 | startRun IDLE → Started | `singleRunOwnership`, `multipleBatchesPreserveIdentity`, `awtAdmissionSmoke` |
| 2 | startRun RUNNING/STOPPING → BUSY | RUNNING: `singleRunOwnership`, `reentrantOperationsFromEffect`; STOPPING: `stopDuringEffect`, `awtAdmissionSmoke` |
| 3 | startRun CLOSING/CLOSED → CLOSED | CLOSING and CLOSED: `closeDrainsWithoutReopening` |
| 4 | beginBatch RUNNING, valid → Begun | `singleRunOwnership`, `multipleBatchesPreserveIdentity`, `nullInputDoesNotMutate` |
| 5 | beginBatch RUNNING, failed precondition → exact reason | `PREVIOUS_BATCH_UNSETTLED`: `reentrantOperationsFromEffect`; `CALL_ID_ALREADY_ACCEPTED`: `multipleBatchesPreserveIdentity` |
| 6 | beginBatch STOPPING/CLOSING → RUN_NOT_ACCEPTING_BATCHES | STOPPING: `stopDuringEffect`; CLOSING: `closeDrainsWithoutReopening` |
| 7 | beginBatch IDLE/CLOSED or foreign/old run → STALE_RUN | IDLE: `multipleBatchesPreserveIdentity`; CLOSED: `closeDrainsWithoutReopening`; foreign: `staleHandleIsolation`; old: `singleRunOwnership` |
| 8 | execute RUNNING, next pending → Executed or thrown, terminal status | `orderedExclusiveExecution`, `terminalAccounting`, `reentrantOperationsFromEffect` |
| 9 | execute RUNNING, foreign/replaced batch → STALE_BATCH | `staleHandleIsolation` |
| 10 | execute RUNNING, unknown/executing/terminal/out of order → exact reason | `orderedExclusiveExecution`; executing also `reentrantOperationsFromEffect` |
| 11 | execute STOPPING/CLOSING → RUN_NOT_ACCEPTING_EFFECTS | STOPPING: `stopBeforeQueuedEffect`, `awtAdmissionSmoke`; CLOSING: `closeDrainsWithoutReopening` |
| 12 | execute IDLE/CLOSED, no current batch → STALE_BATCH | IDLE: `multipleBatchesPreserveIdentity`; CLOSED: `closeDrainsWithoutReopening`; previous generation: `staleHandleIsolation` |
| 13 | stop RUNNING → Acknowledged, STOPPING, pending cancelled | `stopBeforeQueuedEffect`, `stopDuringEffect`, `closeDrainsWithoutReopening` |
| 14 | stop STOPPING/CLOSING → idempotent Acknowledged | STOPPING: `stopDuringEffect`; CLOSING: `closeDrainsWithoutReopening` |
| 15 | stop IDLE/CLOSED or foreign/old run → STALE_RUN | IDLE: `multipleBatchesPreserveIdentity`; CLOSED: `closeDrainsWithoutReopening`; foreign and old: `staleHandleIsolation`, `singleRunOwnership` |
| 16 | finishRun RUNNING/STOPPING, no or settled batch → Finished, IDLE | RUNNING no batch: `singleRunOwnership`; RUNNING settled: `multipleBatchesPreserveIdentity`; STOPPING: `stopDuringEffect`, `awtAdmissionSmoke` |
| 17 | finishRun CLOSING, settled → Finished, CLOSED | `closeDrainsWithoutReopening`, `reentrantOperationsFromEffect` |
| 18 | finishRun unsettled → BATCH_UNSETTLED | RUNNING: `multipleBatchesPreserveIdentity`, `reentrantOperationsFromEffect`; STOPPING: `stopDuringEffect`, `awtAdmissionSmoke`; CLOSING: `closeDrainsWithoutReopening` |
| 19 | finishRun IDLE/CLOSED or foreign/old run → STALE_RUN | IDLE: `multipleBatchesPreserveIdentity`; CLOSED: `closeDrainsWithoutReopening`; foreign: `staleHandleIsolation`; old: `singleRunOwnership` |
| 20 | close IDLE → CLOSED | `closeDrainsWithoutReopening` |
| 21 | close RUNNING/STOPPING → CLOSING, pending cancelled | RUNNING: `closeDrainsWithoutReopening`; STOPPING: `closeDrainsWithoutReopening`, `reentrantOperationsFromEffect` |
| 22 | close CLOSING/CLOSED → idempotent | `closeDrainsWithoutReopening` |
| 23 | batchSnapshot current handle → Available | all batch tests; during CLOSING: `closeDrainsWithoutReopening` |
| 24 | batchSnapshot foreign/replaced/old handle → STALE_BATCH | `staleHandleIsolation` |

### Verification on the resolved basis

| Check | Result |
|---|---|
| `./gradlew :plugin-core:test --tests '…RunLifecycleTest'` | 13 tests, 0 failures, 0 errors |
| Same, `--rerun` five times | 5/5 BUILD SUCCESSFUL |
| `./gradlew :plugin-core:test :plugin-core:buildPlugin --rerun` | BUILD SUCCESSFUL |
| Deliberate one-line bugs (8) | 8/8 killed; sources restored, no diff |
| `check_slice.py check` | PASS, no findings |
| `check_slice.py selftest` | 18/18 PASS |
| `check_slice.py receipt` then `verify` | PASS, digest `5a888317…0759`, no mismatches |
| `check_slice.py verify --receipt` on a missing file | JSON `status: ERROR`, exit 2 |
| `.agent-work/native-agent-spec/` after runs | empty |
| `git diff --check` | clean |

Not run: IDE semantic surface inspection and IDE build (S3 `EV-SURFACE`, `EV-BUILD`). The reviewer's note that the IDE's direct class test target fails with `junit.framework.TestCase` is an IDE run-configuration issue; the Gradle task, which includes the JUnit 4 runtime dependency, passes.

Changed rows for the targeted gate: driver obligations (`design.md` Scope, `Effect`, `spec.json` assumptions); LC-012 checker rules and receipt verification; AC-002, AC-003, AC-008, AC-011, AC-013 test assertions. Unchanged: `RunLifecycle`, `Call`, `Batch`, `Lifecycle`, `RunHandle` production code; LC requirements; acceptance text; operation matrix.

- Address status: COMPLETE
- Implementation review status: FINAL_GATE_FAIL until the targeted gate runs
- Reviewer checklist: COMPLETE (round 4)
- Frozen basis: CURRENT
- Open Blockers: 0
- Open Majors: 0
- Deferred Minors/Nits: 1
- Verification: PASS
- Final gate: FAIL (targeted gate not yet run)
- Stop reason: awaiting targeted gate
- Next permitted action: TARGETED_GATE

## Round 4 — Targeted final gate

Basis: the clean `native-agent-workflow` tree containing the Round 4 resolution. The Round 4 peer agreement remains applicable: primary `openai-codex/gpt-5.6-sol`, independent peer none, zero peer exchanges. This is the bounded targeted gate, not another exhaustive review.

### Disposition verification

- **R4-B-001 — PASS.** `product.md:247`, `design.md:13`, `Effect.java:6-14` and `spec.json:291-297` now require one policy: an unexpected throwable records `FAILED_AFTER_START`; the driver starts no later call or provider request, stops the run, and finishes after cleanup. The documented Pi difference is explicit.
- **R4-B-002 — PASS for stored-payload authentication.** `verify_receipt` requires the exact three S1 evidence IDs, recomputes every payload hash, checks aggregate status, and compares current identity fields. Selftests cover altered, missing and extra payloads and a forged evidence hash. A separate exact-basis defect remains as R4-B-003.
- **R4-M-001 — PASS for unknown-field and target-kind enforcement.** Fixed field sets cover every current normative record and stage binding; `S005` rejects nested unknown fields; acceptance methods must match an `@Test` declaration. A separate malformed-type defect remains as R4-M-004.
- **R4-M-002 — PASS by source trace.** The operation-matrix checklist maps all 24 rows to assertions. The changed tests cover blank IDs, unknown calls without invocation or mutation, STOPPING/CLOSING batch rejection, repeated Stop, AWT pre-settlement start/finish rejection, and IDLE/CLOSED outcomes. Independent execution was unavailable in this gate as recorded below.
- **R4-M-003 — PASS.** Both receipt compilation and selftest use `scratch_dir`, whose parent is `.agent-work/native-agent-spec/`; the selftest checks every recorded scratch root.
- **R4-N-001 — PASS.** Missing and malformed receipts become `ToolError`; `main` maps expected read/parse errors to JSON `ERROR` and exit 2.

### New findings

#### R4-B-003 — the receipt does not bind the evidence implementation

- [ ] **Severity: Blocker.**
- **Location:** `scripts/native-spec/check_slice.py:32-49`, `:340-356`, `:392-395`, `:409-417`; `spec.json` LC-012.
- **Problem:** `check_slice.py` is copied into selftest bases but is absent from every receipt identity field. `configuration_identity.files` contains only the five Gradle/configuration files. The test sources inspected by `check_spec` are absent too. `EV-STRUCTURE` stores only a command label, findings and status.
- **Concrete trace:** generate a receipt, then change `RECORD_FIELDS`, `TEST_METHOD`, or another checker rule without changing the currently valid spec's empty finding list. `build_receipt` produces the same structure payload, evidence hash, spec/design/declaration hashes and configuration identity. `verify_receipt` compares only those fields, so the old receipt still verifies under a different checker implementation. The same omission applies to acceptance-target input files when their change leaves the discovered target set equivalent.
- **Why it matters:** LC-012 and the frozen S1 row require exact frozen inputs. A receipt that survives a validator change is not an exact-basis receipt. This is the same gate property that made R4-B-002 a Blocker.
- **Expected correction:** add an explicit evidence-input identity to the receipt digest. At minimum bind `scripts/native-spec/check_slice.py` and the test source files consumed for target discovery, alongside every other file read to generate S1 evidence. Verification must compare that identity before rebuilding evidence.
- **Test requirement:** generate a receipt in a copied basis, alter the copied checker and separately alter a target test source without changing current PASS output, then require verification to reject the changed input identity.

### Escape analysis — R4-B-003

- New material evidence since Round 4: no.
- Frozen-basis row that should have caught it: S1 exact-basis receipt; `check_slice.py` change-impact row; receipt weak-implementer trace.
- Why Round 4 marked that row complete: the trace mutated receipt payloads and declared source/configuration files, but treated the checker implementation and target-discovery sources as outside the receipt identity despite listing them in the review scope.
- Classification: REVIEW_MISS.
- Required workflow correction: enumerate and hash every evidence-generator input, then add checker/test-source drift to the receipt trace. Round 4 `Reviewer checklist: COMPLETE` is superseded by `INCOMPLETE` below.

#### R4-M-004 — nested field types are neither rejected nor handled safely

- [ ] **Severity: Major.**
- **Location:** `scripts/native-spec/check_slice.py:132-163`, `:186-266`, `:545-570`; `workflow.md:107-123`.
- **Problem:** the new `fields` helper checks only record shape and key presence. Most field value types are unchecked. Several later semantic loops assume lists, dictionaries, or strings.
- **Concrete traces:** setting `stages[0].entry` to the string `"x"` passes the only container guard at lines 147-151 without a finding; the later stage loop iterates the string and calls `binding.get`, raising `AttributeError`, which `main` does not translate. Setting an operation-matrix `result` to integer `7` passes because `str(7).strip()` is nonempty. Both violate the documented typed, fail-closed schema behavior.
- **Why it matters:** malformed normative input can crash the gate or be accepted with the wrong type instead of producing deterministic diagnostics.
- **Expected correction:** define required value types with the per-record field schema; validate containers and element types before semantic traversal; stop semantic traversal for structurally invalid rows. Do not coerce normative values with `str(...)` for validation.
- **Test requirement:** add selftests for a wrong stage entry container, a non-object stage binding, a numeric matrix field, and a non-string acceptance field. Require deterministic `S003` findings without traceback.

### Escape analysis — R4-M-004

- New material evidence since Round 4: no.
- Frozen-basis row that should have caught it: LC-012 strict structure and behavior grammar `malformed spec`.
- Why Round 4 marked that row complete: it tested unknown and missing keys but did not mutate any nested value type or trace structurally invalid input through the semantic loops.
- Classification: REVIEW_MISS.
- Required workflow correction: add wrong-type mutations to the finite malformed-spec row and keep structural validation ahead of reference/semantic checks. Round 4 `Reviewer checklist: COMPLETE` is superseded by `INCOMPLETE` below.

### Verification observed

| Check | Result |
|---|---|
| Generated S1 receipt supplied by the user | Present; status `PASS`, compile exit 0, digest `5a888317…0759` |
| IDE incremental project build | PASS |
| IDE diagnostics for checker, lifecycle test and `Effect` | No reported warning/error items |
| Focused `RunLifecycleTest` run configuration | Tool failed with HTTP 404 before launch; reported |
| Test target `RunLifecycleTest` fallback | Returned success with `noTestsFound=true`, 0 tests; rejected as evidence and reported |
| `Plugin-Core Tests (Clean)` fallback | Exit 1 with no output or test counts; rejected as evidence and reported |
| Repository state before this ledger append | clean on `native-agent-workflow` |

The user's `receipt` execution and the generated file prove the current strict check, Java 21 declaration compilation and signature extraction passed. They do not execute `selftest` or lifecycle behavior tests. The targeted gate therefore lacks independent focused-test proof in addition to the two open findings.

Address status: COMPLETE for R4-B-001, R4-B-002, R4-M-001, R4-M-002, R4-M-003 and R4-N-001; ADDRESS_FINDINGS for R4-B-003 and R4-M-004
Implementation review status: FINAL_GATE_FAIL
Reviewer checklist: INCOMPLETE
Frozen basis: CURRENT
Peer agreement: openai-codex/gpt-5.6-sol + none
Peer exchanges used: 0
Open Blockers: 1
Open Majors: 1
Deferred Minors/Nits: 1 (round-3 N-003)
Verification: FAIL
Final gate: FAIL
Review rounds: 4
Stop reason: R4-B-003 and R4-M-004 remain open; independent focused tests did not execute
Next permitted action: ADDRESS_FINDINGS

## S3 qualification attempt

Executed after the Round 4 targeted gate passed. This records reachable S3 evidence and the remaining environment boundary; it does not claim DONE while surface inspection and the semantic commit are unavailable.

| Check | Result |
|---|---|
| Focused lifecycle tests | PASS: `RunLifecycleTest`, 13 tests, 0 failures, 0 errors |
| Required AWT smoke | PASS: `RunLifecycleTest#awtAdmissionSmoke` included in the focused run |
| Plugin build | PASS: `Build Plugin ZIP`, `:plugin-core:buildPlugin`, exit 0 |
| Python S1 checks | PASS: `check` no findings; `selftest` 24/24; `receipt` digest `f29d9268d3fe63bbda1e8dc50c0572a8b61d9a095c6c08d27306a27248321d29`; `verify` no mismatches |
| IDE diagnostics | No errors reported; one existing unused `Batch.Snapshot.isSettled` warning was returned |
| IDE semantic surface inspection | BLOCKED: mounted project-outline and symbol routes returned the documented-route error requiring unavailable project-bound `ide_*` navigation; issue reported |
| Semantic Git commit | BLOCKED: no native Git write tool is mounted; the three intended files remain uncommitted |

S3 behavioral and build evidence is green. The feature cannot be marked `DONE` under `workflow.md`: surface evidence and the repository-authorized semantic commit remain outstanding.

S3 status: BLOCKED
Implementation review status: FINAL_GATE_PASS
Reviewer checklist: COMPLETE
Frozen basis: CURRENT
Open Blockers: 0
Open Majors: 0
Deferred Minors/Nits: 1 (round-3 N-003)
Verification: PASS for executed checks; S3 incomplete
Final gate: PASS
Stop reason: S3 surface inspection and semantic commit unavailable in the mounted environment
Next permitted action: COMPLETE_S3_SURFACE_AND_COMMIT
