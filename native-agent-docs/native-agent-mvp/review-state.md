# MVP plan author audit and review state

## Peer agreement

User selected:

- Primary/coordinator: current session model (`openai-codex/gpt-6-astra`).
- Independent peer: **none**.

No independent peer was launched for this plan. The earlier two scout jobs were read-only source research, not review; both failed before producing research because of model configuration/availability errors.

The current session authored the scope and plan. The checks below are therefore an **author audit**, not independent plan approval. No formal FINAL_GATE_PASS is asserted. The historical merged-plan review does not apply to this new basis.

## Frozen scope manifest

- `intellij-native-agent-phased-scope.md`: authoritative revised scope.
- `.agent-work/native-agent-mvp/plan.md`: seven implementation stages, 0 through 6.
- `.agent-work/native-agent-mvp/source-manifest.json`: 13 inspected source hashes.
- `.agent-work/native-agent-mvp/handoff.md`: implementation-model instructions and review limitations.
- `.agent-work/native-agent-mvp/scope-before-revision.md`: preserved original scope.

Requirements: R1 semantic problem-space restriction; R2 source-grounded Pi behavior; R3 ChatGPT subscription; R4 native coding workflow; R5 later measured donor subtraction; R6 executable model handoff.

Behavior basis: completed text/calls; invalid tool/schema; malformed/duplicate calls; LENGTH text/calls/fragments; missing terminal/transient failure; Stop before/after effect admission; failure after effects; capacity exhaustion. These cases are explicitly enumerated in the plan's provider/run grammar.

Change impact: loop/domain contracts, selected donor operation extraction, native catalog/handles, guarded writes/rename, format/diagnostic/process ownership, Codex auth/transport, native UI/startup isolation, manual dogfood. No wholesale deletion or new provider framework in the MVP.

## Requirement coverage

| Requirement | Coverage | Author assessment |
|---|---|---|
| R1 | Stages 2/3/6, identity/range/precondition contracts | Explicit; semantic behavior must be tested, not inferred from tool names. |
| R2 | Pi parity table, Stages 1/4/5, source hashes | Current Pi source traced; old review's LENGTH assertion is not used. |
| R3 | Stage 4 browser PKCE, PasswordSafe, required model setting, live smoke | Source-grounded protocol; actual account/client support remains a manual prerequisite. |
| R4 | Stages 3/5/6, real tool/UI task acceptance | Not satisfied by fixtures or compilation alone. |
| R5 | Stage 2 native-mode startup isolation; Stage 6 separate cutover plan on Go | Explicitly not claimed as a stripped MVP distribution. |
| R6 | Complete stage contracts and handoff | Artifact ready; implementation runner not yet launched. |

## Prior-stage-only traces

| Stage | Literal trace | Defined behavior and evidence boundary |
|---|---|---|
| 0 | Donor package task fails before edits | Record baseline; do not blame new code or delete modules to force a green result. |
| 1 | Fake response requests calls A/B; Stop while A is active | A real outcome retained, B cancelled-not-started, no next request; pure contract test. |
| 2 | Two overloads named `run`; source has unsaved changes | Distinct handles/signatures and current document data; no first-match selection; real platform smoke. |
| 3 | Edit admitted to queue, user changes target before EDT executes | Version/identity conflict; no mutation; new read required. |
| 3 | Rename preparation sees new usages added before commit | Recheck project PSI change state; rediscover/fail before using stale usages. |
| 3 | Formatting is queued when Stop arrives | Native ownership cancels unstarted work; no dependence on a later ACP turn-end callback. |
| 3 | Command does not terminate after five-second cleanup attempt | Visible TERMINATION_INCOMPLETE; Send disabled while tracked process lives; no false idle/success. |
| 4 | Callback port occupied; full callback URL pasted | Same verifier/state, strict callback URL validation, one exchange; manual fallback does not require Stage 5. |
| 4 | Stream has function-call fragments but no terminal | No tools execute; at most one request-local retry; failed provisional output excluded. |
| 4 | Second request after a tool result | Same-model reasoning items and call IDs replayed with matching function_call_output. |
| 5 | User types while response streams and closes content | Draft is separate; late UI updates dropped; effect accounting finishes off EDT. |
| 6 | Native task completes but relies on shell for symbol identity | Record fallback reasons; no automatic claim of semantic advantage; Adjust/Stop remains possible. |

## Test-strength audit

Permanent tests are limited to plausible failures: wrong overload/range, stale snapshot/new usage race, effect admission/cancellation race, undo, detached formatting, schema errors and truncated execution, history replay, OAuth state/refresh/logout races, process flood/cleanup, dirty document vs shell changes, and disposal.

No source-text assertions, model prose pinning, mock-forwarding tests, fixture-only claims of usability, or tests created merely to increase counts are requested. Development actions/smokes demonstrate actual retained tools. Native UI must be exercised through the actual Swing/editor surface.

## Author corrections before handoff

- Preserved the original scope before replacing it.
- Replaced the API-key-only premise with Codex subscription access.
- Recorded the current Pi LENGTH safety rule rather than copying the historical review's contrary statement.
- Named native-only startup isolation before UI, avoiding accidental reliance on the old running bridge.
- Made donor first/nearest-symbol resolution and line-rounded replacement a required extraction/fix, not preserved safety.
- Made native formatting/import queues run-owned.
- Added explicit behavior for process termination failure: STOPPING/TERMINATION_INCOMPLETE rather than idle with a surviving owned process.
- Distinguished source/fixture metadata from actual model availability; generated Pi model data is missing here.
- Distinguished MIT source reuse from OAuth client/service authorization.

## Verification record

Source investigation used real repository/Pi reads and source hashes. Documentation structural/link/hash checks are recorded in `artifact-check.json` after the audit. No production code changed, so no implementation build/test/UI/provider result is claimed.

Formal independent review: NOT RUN. User selected no peer. A fresh implementation model can perform the independent primary preflight before code; do not assert a passed review until that actually occurs under the agreed workflow.

Implementation: NOT STARTED.
