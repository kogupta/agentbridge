# .agent-work index

This directory is git-ignored. Nothing here is recoverable from history. Move files instead of deleting them unless a copy is proven redundant.

## Live documents (authorities, in reading order)

| Path | Role |
|---|---|
| `thought-process.md` | Workflow principles. Hash-frozen by `native-agent-workflow/source-manifest.json`. |
| `native-agent-workflow/plan.md` | Constrained implementation workflow (W1–W8, S1–S3). Referenced from the repository `README.md`. |
| `native-agent-workflow/source-manifest.json`, `artifact-check.json` | Frozen source hashes and structure check for the workflow plan. |
| `native-agent-workflow/lifecycle-admission/spec.json` | Canonical LC-001–LC-012 requirements and AC-001–AC-013 acceptance criteria. |
| `native-agent-workflow/lifecycle-admission/design.md` | Java surface, transitions, admission algorithm, operation matrix, type-safety audit. |
| `native-agent-workflow/lifecycle-admission/review-state.md` | Running review ledger for the lifecycle slice. |
| `native-agent-workflow/lifecycle-admission/review-round-2.md`, `review-round-3.md` | Persisted findings per round. Round 3 is the current open set. |
| `native-agent-workflow/lifecycle-admission/review-basis.md` | Frozen basis dump for round 2. Invalidated. Regenerate before the next review, then archive this one. |
| `native-agent-workflow/lifecycle-admission/adversarial-review-prompt.md` | Prompt used for round 3. |

## Frozen inputs to the workflow plan (do not edit)

`native-agent-mvp/` holds the MVP generation from 2026-09-13. The workflow plan and both manifests hash these files, so they stay in place with their current names.

| Path | Role |
|---|---|
| `native-agent-mvp/phased-scope.md` | Authoritative MVP product scope. Older documents call this file `intellij-native-agent-phased-scope.md`. Same content. |
| `native-agent-mvp/plan.md` | MVP implementation plan. **Hash drift:** both manifests record `49a6a970…`, the file now hashes `4faeb2e7…` (edited 2026-09-14 14:52). |
| `native-agent-mvp/feedback.md` | Review feedback and architectural decisions. Sections 4 and 5 define MVP scope and exclusions. |
| `native-agent-mvp/handoff.md` | Implementation-model handoff for the MVP plan. |
| `native-agent-mvp/review-state.md` | Author audit of the MVP plan. Not the lifecycle review state. |
| `native-agent-mvp/scope-before-revision.md` | Original scope, preserved because `phased-scope.md` cites it. |
| `native-agent-mvp/source-manifest.json`, `artifact-check.json` | Frozen hashes for the MVP generation. |

## Archive (historical, superseded)

| Path | What it was |
|---|---|
| `archive/2026-09-13-hardened-plan/` | Greenfield `native-plugin` plan v3.0.0-hardened, its REJECT review, review state, and handoff. Superseded by the merged plan. |
| `archive/2026-09-13-strip-plan/` | Strip-by-subtraction plan, revision 5. Superseded by the merged plan. |
| `archive/2026-09-13-merged-plan/` | Merged plan including "Plan revision 1" appendix, plus its round-1 review. Historical reference only per `phased-scope.md`. |
| `archive/redundant-copies/` | Safe to delete. `intellij-native-agent-phased-scope.md` is byte-identical to `native-agent-mvp/phased-scope.md`. `native-agent-merged-plan.md` is a strict prefix of the archived revision. `design-source-path.txt` repeats the "Design status" paragraph of `design.md`. |

## Name map for old references

| Old name in a document | Current location |
|---|---|
| `intellij-native-agent-phased-scope.md` | `native-agent-mvp/phased-scope.md` |
| `native-agent-merged-plan.md` | `archive/2026-09-13-merged-plan/native-agent-merged-plan.md` |
| `native-agent-merged-plan-review.md` | `archive/2026-09-13-merged-plan/native-agent-merged-plan-review.md` |
| `ij_native_coding_agent_plan.md` | `archive/2026-09-13-hardened-plan/ij_native_coding_agent_plan.md` |
| `ij_native_agent_strip_plan.md` | `archive/2026-09-13-strip-plan/ij_native_agent_strip_plan.md` |
| `plans/` | Removed. Contents are in `archive/`. |
