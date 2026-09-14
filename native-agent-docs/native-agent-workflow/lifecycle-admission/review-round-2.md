# Lifecycle/admission review — round 2

Reviewer: `openai-codex/gpt-5.6-sol:medium`, reasoning effort `medium`, configured `plan` role.
Basis: `.agent-work/native-agent-workflow/lifecycle-admission/review-basis.md`; branch `native-agent-workflow`.

## Summary

S2 review result: FAIL. Core lifecycle source is largely consistent with LC-001–LC-011: Stop and admission share one monitor; callbacks run outside the monitor; pending calls cancel without overwriting executing/terminal calls; batch ID validation precedes mutation; snapshots contain no handles; callback exceptions are accounted and rethrown; public mutating inputs are null-checked; source uses Java-21-compatible constructs and only Java utility imports.

S2 cannot pass because exact S1 evidence is absent, one public rejection variant is unreachable, and acceptance targets/tests do not execute all stated scenarios.

## Blockers

### B-001 — Exact-basis S1 receipt and evidence results absent

LC-012/S2 require current `EV-STRUCTURE`, `EV-DESIGN-SOURCE`, `EV-DESIGN-COMPILE`, and `EV-RECEIPT` results. The review basis contained descriptions and file hashes, but no receipt with stage/feature/spec/design/declaration/configuration/evidence hashes, no signature extraction, no strict-check output, no expected invalid-copy output, no attached `javac --release 21` result for the exact basis, and no receipt verification result. `review-state.md` was stale: it named `master`, removed `design-src`, omitted AC-013 and newer evidence IDs, and said production source was absent.

Required: reissue current `review-state.md`, create S1 receipt, attach current structure/audit/matrix/null/stage checks, signature hashes, exact Java 21 compile result, configuration identity, evidence-result hashes and receipt verification output.

### B-002 — Canonical acceptance bindings do not resolve

AC-003 named `RunLifecycleTest#orderedExclusiveExecution`, while the implementation method was `orderedExclusiveExecutionAndDuplicateAdmission`. AC-013 named `lifecycle-admission-awt-smoke`, while the implementation was a JUnit method and no separate launcher/output was supplied. AC-012 named `check_slice.py`, which does not exist.

Required: align AC-003 target; either supply the separate AWT smoke or revise AC-013/EV-SMOKE/design to the JUnit implementation; supply `check_slice.py` evidence or remove/revise AC-012 through the specification gate.

## Majors

### M-001 — Unreachable Stop rejection

The matrix specifies stale/foreign/no-current-run Stop as `STALE_RUN`, and current STOPPING/CLOSING Stop as acknowledged. `StopRejection.RUN_NOT_ACTIVE` is publicly representable but unreachable because stale-run checking occurs first; its branch is dead.

Required: remove `RUN_NOT_ACTIVE` and the unreachable branch; state Stop/close idempotence explicitly.

## Test and evidence gaps

- AC-001 starts a later run but does not exercise old-generation handles against it.
- AC-003 does not release two competing executions through a barrier; it starts A first, then attempts duplicate A.
- AC-007 lacks previous-generation and foreign-handle operation checks, unchanged-state assertions, and meaningful replacement of tautological `assertNotNull` checks.
- AC-008 does not assert pending cancellation or active terminal retention.
- AC-009 covers only some null inputs, mostly while idle, and lacks before/after snapshots for every mutating boundary.
- AC-011 does not retry the same fresh ID from a rejected mixed `[freshB, reusedA]` candidate.
- AC-013 does not assert new-run rejection before admission-winning callback settlement and is not separately launched.
- Matrix branches for Stop/close, foreign/replaced batch execution, stale Stop/finish, foreign snapshot and closing admission need explicit evidence or a scoped reason they are covered by the acceptance scenarios.

## Verified assumptions

- One synchronized owner monitor can linearize lifecycle state and effect admission while callback execution occurs outside the monitor.
- Admission immediately before callback invocation is a valid defined Stop race ordering.
- An executing callback may never return; the truthful state remains STOPPING/CLOSING and finish rejects until settlement.
- Run-wide CallId uniqueness requires runtime history; Java types alone cannot encode it.
- `List.copyOf` plus immutable CallId values provides the required shallow batch snapshot.
- No Kotlin layer is needed for this synchronous core.
- Production lifecycle declarations use Java-21-compatible records, sealed interfaces, enums and collection APIs.

## State

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
