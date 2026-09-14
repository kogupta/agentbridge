# Native agent docs

Each fact has one home. If two files seem to say the same thing, the file named here wins and the other must only reference it.

| File | Owns |
|---|---|
| `product.md` | Hypothesis, requirements R1–R6, decision registry, Pi fidelity, invariants I1–I12, tool and Codex contracts, run behavior, roadmap, dogfood gate, exclusions, donor evidence. |
| `workflow.md` | Principles, workflow invariants W1–W8, artifact layout, SPEC_VALID and DESIGN_VALID gates, reviewer contract, failure routing, current slice stages. |
| `lifecycle-admission/spec.json` | Canonical LC-001–LC-012, AC-001–AC-013, type-safety audit, operation matrix, null boundaries, evidence IDs, stage bindings. |
| `lifecycle-admission/design.md` | Java surface, transitions, admission algorithm, precedence rules. References `spec.json` for tables. |
| `lifecycle-admission/review.md` | Append-only review ledger: current state, rounds 1–3, the adversarial review prompt. |

Reading order for implementation work on the lifecycle slice: `lifecycle-admission/review.md` current state → `spec.json` → `design.md` → `workflow.md` current slice. Read `product.md` when a finding touches product scope or Pi fidelity.

## Consolidation record (2026-09-14)

Twenty files became six. Git history keeps every removed file.

| Removed | Content now in |
|---|---|
| `native-agent-mvp/phased-scope.md` | `product.md` |
| `native-agent-mvp/plan.md` | `product.md` (contracts, grammar, stage tests folded into Roadmap) |
| `native-agent-mvp/feedback.md` | `product.md` Decisions, Architecture rules, Dogfood gate, Out of scope |
| `native-agent-mvp/handoff.md` | `product.md` Roadmap (parallel ownership), `workflow.md` Implementation packet (progress report). Branch and Stage 0 instructions were obsolete. |
| `native-agent-mvp/scope-before-revision.md` | Superseded by the revised scope; no unique normative content kept. |
| `native-agent-mvp/review-state.md` | `product.md` Status (author audit only, no independent review). |
| `thought-process.md` | `workflow.md` Goal and principles. The "task for this session" prompt was answered by the workflow plan. |
| `native-agent-workflow/plan.md` | `workflow.md`; its decision table moved to `product.md` Decisions. |
| `native-agent-workflow/lifecycle-admission/review-state.md`, `review-round-2.md`, `review-round-3.md`, `adversarial-review-prompt.md` | `lifecycle-admission/review.md` |
| `native-agent-workflow/lifecycle-admission/review-basis.md` | Deleted. It was a 133 KB concatenation of other files. |
| `*/source-manifest.json`, `*/artifact-check.json` | Deleted. They hashed old paths and donor files that no longer exist. Regenerate `EV-SOURCE` from current paths. |

Conflicts resolved during the merge, each by the later explicit user decision already recorded in the workflow plan:

- Language: Java core with minimal Kotlin, not "Kotlin preferred for new code".
- `run_command`: excluded from the semantic MVP; its contract is kept only as a deferred section.
- Tool results: sealed domain outcomes; the `status/code` envelope only at the LLM boundary.
- Session lifetime: tool-window content owns the session.

Issues found during the merge and resolved afterwards:

- Early donor removal is an intentional owner decision, now recorded in `product.md` DONOR_SUBTRACTION, R5 and roadmap milestone 7.
- `design.md` idempotence rule now matches `spec.json` and the code: `close` and `stop` are idempotent, `finishRun` is not.
- `design.md` uses the real nested type names (`Call.Id`, `Call.Batch`, `Batch.Handle`, `Lifecycle.Snapshot`, `Call.Snapshot`, `Batch.Snapshot`, `Batch.Observation`).

Still open:

- The ignored `.agent-work/archive/` still holds the 2026-09-13 hardened, strip and merged plans. They are historical and not referenced here.
