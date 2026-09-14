# Lifecycle/admission review state

## Peer agreement

- Primary reviewer: `openai-codex/gpt-5.6-sol`, reasoning effort `medium`, configuration role `plan`.
- `omp config get modelRoles --json` confirms `plan = openai-codex/gpt-5.6-sol:medium`.
- Additional independent peer: none, explicitly selected by user.
- Author/coordinator: current session; not eligible to approve its own design.

## Frozen basis

- Basis files: `spec.json`, `design.md`, `design-src/**/*.java`, and the revised plan sections defining W1–W8 and S1–S3.
- Canonical requirements: LC-001 through LC-012.
- Acceptance criteria: AC-001 through AC-012.
- Type-safety audit: one row for each LC requirement; dimensions include sum types, state payloads, validated values, constructor authority, transitions, argument/result correlation, collection invariants, aliasing/ownership, protocol progression, visibility and nullness.
- Evidence IDs: EV-SOURCE, EV-STRUCTURE, EV-DESIGN-COMPILE, EV-REVIEW, EV-VCS, EV-FOCUSED, EV-SMOKE, EV-BUILD, EV-SURFACE.
- Stage contracts: S1 normalize/design, S2 independent review/freeze, S3 implementation/qualification.
- Source basis: current project branch is still `master`; the eight pre-existing untracked entries are unrelated to this lifecycle basis. Design declarations compile with `javac --release 21` in the ignored design workspace. No production source has been installed.

## Review status

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

## Review round 1 — independent findings

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

## Review round 2 — corrected source-backed basis

Reviewer: `openai-codex/gpt-5.6-sol:medium`. Full persisted findings: `review-round-2.md`.

- **B-001 — Open:** current S1 receipt and evidence-result artifacts are absent. Descriptions and hashes alone do not prove structure checks, signature extraction, exact Java 21 compilation, configuration identity or receipt verification.
- **B-002 — Open:** AC-003 target does not match `RunLifecycleTest#orderedExclusiveExecutionAndDuplicateAdmission`; AC-013 names no executable target matching the JUnit AWT method; AC-012 names nonexistent `check_slice.py`.
- **M-001 — Open:** `StopRejection.RUN_NOT_ACTIVE` is public but unreachable because stale-run rejection precedes phase handling.
- **Test gaps — Open:** generation/foreign stale-handle operations, full barrier race, pending/active close assertions, all null boundaries with snapshots, mixed-batch retry and both AWT race orderings need exact evidence.

Review status: FINDINGS_READY_FOR_ADDRESS
Reviewer checklist: COMPLETE
Frozen basis: INVALIDATED
Peer agreement: openai-codex/gpt-5.6-sol:medium + none
Peer exchanges used: 0
Open Blockers: 2
Open Majors: 1
Deferred Minors/Nits: 0
Verification: FAIL (exact-basis evidence absent; plugin build passed; Gradle test executor failed before test execution)
Review rounds: 2
Stop reason: fresh review found unresolved contract/evidence defects
Next permitted action: address round-2 findings; cleanup scope change also invalidates all product gate receipts
