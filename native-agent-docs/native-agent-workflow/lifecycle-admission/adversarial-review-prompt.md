# Adversarial Review Prompt: Native Agent Lifecycle & Admission (Layer 1)

> **Instructions for the Reviewer**:
> You are acting as an **adversarial, zero-trust systems auditor and JVM concurrency expert**. Your objective is **not** to validate or compliment the author's work, but to aggressively probe for race conditions, deadlock vectors, state machine holes, leaky capability abstractions, unhandled throwables, and discrepancies against the reference Pi implementation.

---

## 1. Environment & Exploration Tools

You have access to the repository root at `/home/muku/depot/personal/cli-tools/agentbridge` on branch `native-agent-workflow`.

### A. Semantic Code Exploration (`idea-facade` MCP)
Connect to the `idea-facade` MCP server to explore the IntelliJ project workspace:
- Uses IntelliJ's semantic index to find symbols, inspect types, resolve references, and read project files with live buffer sync.
- Relevant project source path: `plugin-core/src/main/java/com/github/catatafishen/agentbridge/nativeagent/lifecycle/` and test path: `plugin-core/src/test/java/.../lifecycle/`.

### B. Fast Codebase Search across Reference Repos (`codeq`)
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

## 2. Required Reading List

Before inspecting the code, read the following authoritative documents in order:

1. **The Product Scope & Boundary**:
   - `.agent-work/native-agent-mvp/phased-scope.md`: Pi fidelity contract, tool catalog, non-deferrable invariants.
   - `.agent-work/native-agent-mvp/feedback.md`: Section 4 (In-scope MVP), Section 5 (24 Explicit Exclusions).
2. **The Formal Specification & Design**:
   - `.agent-work/native-agent-workflow/lifecycle-admission/spec.json`: Canonical requirements `LC-001` through `LC-012`, acceptance criteria `AC-001` through `AC-013`.
   - `.agent-work/native-agent-workflow/lifecycle-admission/design.md`: Java surface, state transitions, admission algorithm, operation-by-phase matrix, type-safety audit.
3. **Previous Review Audit (Round 2 Findings)**:
   - `.agent-work/native-agent-workflow/lifecycle-admission/review-round-2.md`: Check if previous blockers (`B-001`, `B-002`, `M-001`) were truly addressed or just masked.

---

## 3. Implementation Code Under Audit

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

## 4. Adversarial Audit Vector Checklist

Conduct your attack across these six specific failure vectors:

### Vector 1: Concurrency, Monitors & Reentrancy
- **The Execution Lock Window**:
  In `RunLifecycle.java`, `execute(batch, call, effect)` acquires `synchronized (this)`, marks the call as `EXECUTING`, and releases the lock before `effect.execute()`.
  - *Attack*: What happens if `effect.execute()` invokes `RunLifecycle.stop()`, `RunLifecycle.finishRun()`, or `RunLifecycle.execute()` reentrantly on the same thread? Does the JVM reentrant monitor cause deadlock or state corruption?
  - *Attack*: What if `stop(run)` is called concurrently while `complete()` or `recordFailure()` is reacquiring the monitor? Is there any window where a terminal status is overwritten or misreported?
  - *Attack*: What if `effect.execute()` spawns background threads or asynchronous tasks that outlive `execute()`? Can `finishRun()` be tricked into settling early?

### Vector 2: Exception Accounting & Suppressed Throwables
- In `recordFailure()`, the code catches `RuntimeException | Error`, attempts to record `FAILED_AFTER_START`, and rethrows.
  - *Attack*: What happens if `complete()` throws an unexpected `IllegalStateException` or `Error` during failure recording? Does it swallow the original throwable, or does `failure.addSuppressed()` guarantee the root cause is preserved?
  - *Attack*: What if the callback throws a checked exception wrapped or unsafely cast via `Unsafe` or generic erasure? Does `execute()` catch it, or does it bypass status recording?

### Vector 3: Capability Token Safety & Forgery
- `RunHandle` and `Batch.Handle` are opaque capability tokens:
  - *Attack*: Are package-private constructors sufficient in Java? Can an attacker in the same package (or via reflection) forge a handle?
  - *Attack*: Check `ownerKey == expectedOwnerKey`. If two `RunLifecycle` instances exist in the same JVM, can a `Batch.Handle` from Instance A be passed to Instance B? Does `isStaleBatch` completely block cross-instance leakage?

### Vector 4: Batch Atomicity & Mutation Poisoning
- In `beginBatch(RunHandle, Call.Batch)`:
  - *Attack*: If validation fails (e.g. `CALL_ID_ALREADY_ACCEPTED` or `PREVIOUS_BATCH_UNSETTLED`), verify whether `acceptedCallIds` or `currentBatch` mutated at all.
  - *Attack*: If candidate batch `[freshA, reusedB]` is rejected, can `freshA` be admitted in a subsequent batch, or did the rejection poison `freshA`?

### Vector 5: Discrepancy with Reference Pi Implementation
- Run `codeq pi` on `/home/muku/depot/personal/cli-tools/pi`:
  - Inspect `packages/agent/src/agent-loop.ts:226-240` (truncated calls).
  - Inspect `packages/agent/src/agent-loop.ts:409-485` (sequential execution and abort).
  - *Attack*: Does our Java lifecycle faithfully implement Pi's semantics? Are there cases where Pi aborts but our Java owner would allow subsequent calls to proceed?

### Vector 6: Residual Type Obligations & Null Boundaries
- The spec claims that invalid states are impossible by construction using Java 21 sealed types and records.
  - *Attack*: Check every public method. What happens if a caller passes `null` for `RunHandle`, `Call.Id`, `Batch.Handle`, or `Effect`? Does any method mutate internal state before throwing `NullPointerException`?
  - *Attack*: Check `Batch.Snapshot` and `Lifecycle.Snapshot`. Are they truly immutable, or do they leak mutable collections or internal state references?

---

## 5. Required Deliverable & Output Format

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
