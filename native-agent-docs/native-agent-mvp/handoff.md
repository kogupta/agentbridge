# Implementation-model handoff — Pi-modeled IntelliJ native MVP

## Assignment

Implement the reviewed MVP one independently verifiable semantic stage at a time. Do not implement the historical merged plan. Do not start by deleting ACP/MCP/JCEF or building a multi-provider framework.

Read, in order:

1. Repository instructions already supplied by your host.
2. `intellij-native-agent-phased-scope.md` — authoritative product scope.
3. `.agent-work/native-agent-mvp/plan.md` — implementation contracts, source evidence, seven stages, verification.
4. `.agent-work/native-agent-mvp/review-state.md` — actual review state, not presumed approval.
5. `.agent-work/native-agent-mvp/source-manifest.json` — source basis hashes.

Pi source is at `/home/muku/depot/personal/cli-tools/pi`. AgentBridge is the current repository. Use source navigation skills and the plan's exact Pi references; a general memory of Pi is not the specification. Current Pi already prevents output-length-truncated tool execution, unlike the old review's description.

## Review gate before code

The planning model authored this plan. The user selected **no independent peer**. The author audit is not independent plan approval. You are a fresh-context model and did not author it: first review the finite basis using the repository's review-plan workflow, as a primary-only reviewer with no second peer, and record the result. Do not silently claim the author audit or historical merged-plan review was your review.

The recorded user review choice named the current authoring model as coordinator; if your review workflow requires new agreement for a different primary model, obtain it before asserting a formal review gate. You may inspect/preflight the handoff without falsely claiming that agreement.

If you find a Blocker/Major, report exact contract/source evidence and stop before production edits for an address pass. Do not solve a conflicting architecture while implementing. After the independent preflight/review is actually accepted, use impl-plan stage discipline. Never mark a stage complete from fake/provider-only tests when its completion contract requires a real tool or native UI smoke.

## Non-negotiable decisions

- Product hypothesis: bounded PSI identity and deterministic IDE operations, not merely native chat.
- Pi-modeled sequential loop; distinct tool results; same-model Codex reasoning/call metadata preserved.
- Java 21, existing IntelliJ version property, sinceBuild 253; no new core Gradle module.
- ChatGPT subscription authentication is MVP. No API-key assumption or silent fallback.
- One configured model/session, one active run/project; ephemeral history.
- Explicit trusted-session opt-in, then no per-call prompts; commands/build/test have IDE-user privileges. Not a sandbox.
- Fifteen selected flat tool IDs, with native schemas narrowed as specified. No shared mutable donor instances and no PsiBridgeService.callTool wrapper.
- Opaque session symbol/read handles, exact target/range validation, no first/nearest-overload selection.
- Rename only for native refactor; new-file-only write_file; strict localized edit_text.
- Stop participates in effect admission and waits for already-started effects. Never lie about rollback or throw away a completed mutation's result.
- Formatting/process/save/refresh ownership cannot depend on ACP/JCEF turn completion.
- No full prompt/tool/OAuth-content logging. PasswordSafe credentials are never exposed to tools/environment.
- Native development mode suppresses legacy runtime activation, while normal donor mode remains available until cutover.

## First executable stage

Stage 0: use IDE VCS tooling to create `native-agent-mvp` from the actual inspected base; preserve unrelated untracked work. Record donor build/test/package results and exact selected-tool dependency closure. No deletion or new production implementation in this stage.

Stage 1 follows: minimal loop/contracts plus genuine state-transition regressions. Do not build the complete provider/UI/tool implementation in the first commit.

## Ownership and useful concurrency

One implementation owner integrates and commits. Do not spawn agents for every class or delegate a single small cleanup.

After Stage 1 interfaces are frozen, two genuinely independent workstreams are possible:

- **Tools owner:** Stage 2/3 native semantic reads, guarded mutations, platform execution, and their tests. Own selected existing tool/helper classes and native tools package. Do not edit provider/auth/UI.
- **Codex owner:** Stage 4 auth/codec/stream implementation and tests, using the frozen Stage 1 Provider/Credential/event contracts. Own native provider/auth packages and fixture resources. Do not edit shared loop or tool files. Live Stage 4 completion waits for Stage 3 real tools.

The integration owner alone owns loop interface changes, plugin.xml, shared build files, settings/UI integration, source-set wiring, staging/commits, and final validation. Sibling agents skip builds/tests/linters/formatters while edits are concurrent; the integration owner runs focused validation after the wave settles. Shared-file changes must be serialized rather than relying on automatic merging.

This is an optional decomposition for the implementation model, not permission to skip the ordered stage completion contracts or to claim that unmerged work is verified.

## Environment and verification

- Use IDE navigation for identity/references and IDE edit/refactor operations at the strongest applicable level. Read `PlatformApiCompat` before raw platform APIs.
- Use IDE git tools, never shell git; only the integration owner may stage/commit. Build must pass before commits. No push is requested.
- All temporary files, notes and fixtures used only for smoke runs belong under `.agent-work/`. Never create work outside the repository or edit generated build outputs.
- Keep fixture tests CI-safe. Credentialed login/model/tool smoke is manual; user enters credentials only through browser/PasswordSafe flow, never in the agent conversation.
- Verify current provider client/auth support before real credential use. The Pi MIT license does not grant service/client authorization. Do not impersonate Pi to bypass an authentication rejection.
- The generated Pi model data directory is missing in this checkout. Do not invent account-supported models/context windows from fixture metadata. The native model field is explicit.
- Native UI is Swing/editor-based. Browser/JCEF screenshots do not verify this surface.
- Plan final verification includes actual IDE build, module tests, platform tests through their real source set, package/verifier, native-mode startup, login, coding task, Stop, undo and disposal.
- Preserve ported Pi attribution. Do not import Node packages or the Pi runtime into the distribution.

## Required progress report after each semantic stage

- Stage and source changes.
- Observable behavior now implemented.
- Exact tests/commands/scenarios run and their outcomes.
- Relevant donor preservation checks.
- Any manual gate not exercised and why.
- Commit identity if a commit was created through authorized IDE VCS tools.
- Next stage, or exact blocker requiring a plan correction.

Do not say "MVP complete" before Stage 5 live UI proof and Stage 6 dogfood decision. Do not say "stripped native product" until the separate cutover/deletion milestone removes old artifacts and registrations.

## Launch status

This file is the handoff artifact. No implementation model has started production changes. Two earlier read-only scout launches failed before research because their configured models were unavailable; they produced no implementation or review evidence. Select a working supported implementation model/runner before launch. Do not silently substitute another model while claiming to have used the requested one.

The user selected **Separate model session** as the implementation destination. No implementation runner should be launched from this authoring session. In the chosen implementation session, start with:

> Read `.agent-work/native-agent-mvp/handoff.md` and its linked scope, plan, and review state. This is the Pi-modeled IntelliJ native-agent MVP handoff. Perform the fresh-context preflight/review before source changes, then implement one approved semantic stage at a time using the repository's workflow. Preserve unrelated work; do not execute the historical 15-stage plan or delete the donor runtime first.
