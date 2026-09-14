# IntelliJ-native coding agent — MVP scope

## Decision

Build a Pi-modeled agent whose default working surface is IntelliJ's semantic model, not a shell over repository text.

The model chooses intent and interprets evidence. IntelliJ resolves symbol identity, enumerates references, applies supported refactorings, manages editor documents and undo, and reports diagnostics. Do not send the entire AST to the model or build another index beside IntelliJ.

**Premise accepted as a product hypothesis, not a demonstrated performance result.** PSI provides structure, resolution, and scoped operations; it does not make every search small or every edit correct. Indexed text search can also be bounded. Pi + idea-facade already exposes semantic tools. Native placement is valuable only if this product makes semantic operations reliable defaults and removes integration overhead without sacrificing task completion.

**User priorities:** constrain the model's problem space through AST/PSI and deterministic IntelliJ operations; model the agent after the local Pi coding-agent source; use an existing ChatGPT subscription.

## Authority and revision

This document replaces the previous phased scope. The original is preserved at `.agent-work/native-agent-mvp/scope-before-revision.md`.

- Implementation specification: `.agent-work/native-agent-mvp/plan.md`.
- Source evidence and Pi parity: the plan's evidence table and `.agent-work/native-agent-mvp/source-manifest.json`.
- Implementation handoff: `.agent-work/native-agent-mvp/handoff.md`.
- `native-agent-merged-plan.md` is a historical design reference, not an executable prerequisite chain.
- `native-agent-merged-plan-review.md` is a historical findings ledger. Its addressed markers do not approve this new scope.
- This session selected the current model as review coordinator and **no independent peer**. Do not claim independent review passed.

## MVP question

Can a user authenticate with their ChatGPT subscription, give the agent a Java repository task inside IntelliJ, and have it resolve the right symbols, make bounded changes, inspect diagnostics, run targeted tests, and finish with verifiable results?

A native chat panel that still relies mostly on shell search and text replacement does not establish the product hypothesis.

## Pi fidelity

Source: `/home/muku/depot/personal/cli-tools/pi`.

Use the small loop in `packages/agent/src/agent-loop.ts`, the coding-agent's tool-aware prompt construction, and the Codex OAuth/Responses implementation. Do not port the entire monorepo or its newer distributed/durable harness layers.

Preserve:

- user message → streamed assistant response → validated tool calls → tool results → continuation;
- distinct user, assistant, and tool-result messages;
- lifecycle events for runs, messages, and tool execution;
- explicit tool errors that the model can act on;
- tool-aware instructions containing only available capabilities;
- provider-specific history conversion and Codex reasoning-item continuity.

Choose Pi's **sequential tool execution mode**. Current Pi supports parallel execution too; sequential execution is an intentional IntelliJ MVP restriction, not a claim about Pi's default.

Current Pi rejects execution of tool calls from an output-length-truncated response (`agent-loop.ts:226-233`). Preserve that safety property. Never implement from the older review's description of Pi without checking current source.

Intentional MVP deviations:

- Swing/editor tool window replaces the terminal UI.
- PSI tools replace default shell-oriented source navigation and semantic changes.
- Accepted history is separate from provisional streamed UI output.
- No steering or follow-up queue: the user may draft while a run is active, but Send remains disabled until idle.
- No durable sessions, compaction, extension runtime, multi-provider conversion, or subagents inside the product.
- Native cancellation accounts for queued EDT writes and already-started noninterruptible IDE operations.

## Product and platform boundary

- Java 21; retain the repository's IntelliJ version property and `sinceBuild=253`.
- First qualification target: local IntelliJ with Java support on Linux. Other languages retain explicitly labeled file/text fallbacks; no semantic-parity claim. Remote Development and cross-OS release qualification are later.
- Start under `plugin-core/.../nativeagent`; keep domain/loop/provider packages free of IntelliJ classes. No new core module, general framework, or mass package rename.
- One ephemeral session per project; one active run. New Session clears it. Project/content closure loses conversation history; state this visibly.
- One provider: **ChatGPT Codex subscription**. OAuth is MVP, not later. Direct OpenAI API keys are not an MVP prerequisite.
- One configured model per session. Changing it starts a new session. Do not assume a fixture's model ID is available to the user's account.
- Browser PKCE login with a short-lived loopback callback and manual full-redirect-URL fallback. Device flow is deferred. The OAuth callback is not a PSI/MCP server and must close after login/cancellation/timeout.
- PasswordSafe holds credentials. No reuse of Pi's credential files and no tokens in project configuration, prompts, tool environments, or logs.

## Trust decision

For this local dogfood MVP, use **explicit trusted-session execution**, not a new permission framework.

Before the first run of each session, the user must enable the native agent after seeing that source will be sent to the configured provider and tools can edit files and run commands with the IDE user's privileges. Consent is not persisted between sessions. After enablement, catalog tools execute without per-call approval dialogs. The old bridge's permission settings are neither consulted nor changed.

This is not a sandbox. Build scripts, tests, and commands can access the network and files outside the project through normal OS privileges. IDE-native mutations are limited to writable project content; dependency sources are read-only. Never describe that path restriction as command containment. Preserve the user's shell/toolchain environment for functionality; do not inject PasswordSafe secrets. Untrusted repositories are outside the dogfood contract.

Conflict detection, cancellation, schema validation, output bounds, undo, and accurate effect reporting are correctness requirements even in trusted mode.

## Tool catalog

Use **15 flat tool IDs**, each justified by the coding workflow. Fifteen is a ceiling for the initial scope, not a target to exceed through debug tools or aliases. Do not recreate these operations behind a generic dispatcher visible to the model.

| Tools | Native contract |
|---|---|
| `find_file` | Bounded filename/path discovery, excluding build/output trees by default. |
| `search_symbols`, `get_file_outline` | Bounded declaration candidates and file structure; no raw AST dump. |
| `get_symbol_info`, `find_references` | Resolve a concrete symbol; include signature, owner, location, and completeness/scope. Never choose the first overload silently. |
| `read_file` | Explicit path and bounded range; read unsaved editor contents when present. |
| `search_text` | Scoped literals/configuration/unsupported-language fallback; not a substitute for symbol identity. |
| `replace_symbol_body` | Replace an inspected Java method declaration while preserving its signature; apply to its exact PSI range, not rounded line spans. |
| `edit_text` | Explicit path, current read evidence, exactly one case-sensitive match; localized edits only. No active-editor fallback or replace-all. |
| `write_file` | Create a new text file only. Existing-file overwrite is excluded from the native schema and execution path. |
| `refactor` | Semantic rename only for the MVP. Safe-delete, move, change-signature, inline, and extract are deferred. |
| `get_problems` | Focused diagnostics with file/version and pending/complete status. |
| `build_project`, `run_tests` | Native build and targeted test feedback with real terminal outcome. |
| `run_command` | Finite command escape hatch; bounded capture, owned process lifecycle, save-before and awaited VFS refresh afterward. |

Native catalog schemas may deliberately narrow donor schemas. Never advertise unsupported donor options. Filter/register native-owned selected instances without pruning the old shared catalog before cutover. Reuse implementations by extracting shared operation helpers, not by parsing their human-readable output or copying PSI algorithms.

Semantic results must return usable symbol identity and pagination/completeness metadata. The native session retains opaque symbol/read handles backed by current IDE state; stale handles fail with an actionable conflict, not best-effort retargeting.

## Non-deferrable execution requirements

1. Establish the direct execution boundary before a live model can mutate. Do not wrap `PsiBridgeService.callTool` and assume its timeout, permissions, tracking, or UI are suitable.
2. Validate complete tool batches before side effects; never execute streamed fragments. No automatic tool replay.
3. Keep tool arguments/results and provider requests bounded. Enforce a conservative context budget before dispatch; context-full ends with an actionable New Session instruction. Compaction waits.
4. Stop cancels provider I/O, waits, and queued mutations. No new effect begins after cancellation wins the execution boundary.
5. An effect already executing may finish. Show `Stopping`, retain its actual outcome, and do not start another run until it settles. Stop does not imply rollback.
6. Recheck file identity, document version, symbol identity, writability, and cancellation at the mutation boundary. Include every affected file for rename. Unknown/conflicting mutation scope must fail before changes.
7. Own formatting/import work within the run. Do not leave donor turn-end queues dependent on an ACP callback.
8. After successful edits, attach focused diagnostics. A timeout means diagnostics pending/unavailable, not no errors.
9. Keep provider secrets and full prompt/tool content out of logs. Native commands must not accept hidden donor `_env.*` arguments.
10. Own editors, listeners, run resources, callbacks, and processes through explicit content/project lifetimes; no blocking disposal on EDT.

## Delivery order

| Stage | Deliverable | Exit proof |
|---|---|---|
| 0 | Donor baseline and execution/dependency census | Existing build/test/package results recorded; selected tools and their live dependencies identified. |
| 1 | Minimal Pi-modeled loop and contracts | Fake provider proves ordered continuation, tool errors, Stop, and no truncated execution. |
| 2 | Native semantic reads | Development action resolves overloads and references without MCP/HTTP; bounded typed results. |
| 3 | Guarded semantic mutations and verification | Exact method edit, project rename, conflict rejection, undo, diagnostics, targeted tests, command lifecycle. |
| 4 | Codex login and provider transport | Scrubbed protocol replay; manual subscription login and real tool continuation. |
| 5 | Native tool window | Complete authenticated coding workflow, Stop races, draft preservation, content close/reopen. |
| 6 | Dogfood decision | Same tasks/model/settings compared against Pi + idea-facade; semantic value and correctness measured. |

No deletion-first stage. No speculative provider work before the coding workflow exists.

## MVP acceptance and dogfood gate

Use a disposable Java fixture for deterministic safety checks and at least three real Java tasks for product evaluation:

- modify one overloaded method without touching another overload or an adjacent same-line declaration;
- rename a referenced symbol across files without changing a same-name unrelated symbol;
- fix a real defect, create a regression test file, read diagnostics, and run the relevant test/build.

Compare native and Pi + idea-facade on equivalent clean task bases, using the same account/model/reasoning setting where supported. Record different settings as a confounder. Count human interventions, wrong/stale target attempts, conflicts, relevant diagnostics, task completion, tool calls, bounded source returned, and shell/text fallback reasons. Wall time is secondary because provider latency varies.

MVP correctness gates: no wrong-target edit in these scenarios; stale edit rejected; rename references correct; undo works; no queued effect begins after Stop; no leaked session-owned process/editor; tests/build outcomes are reported accurately. A manual live-provider run is mandatory for usability claims; fixtures alone prove only protocol handling.

**Go:** all correctness gates pass; real tasks complete; semantic tools carry the identity/refactoring work and show a concrete benefit over the comparator, such as fewer targeting corrections or fewer manual steps. Raw text reads for actual code content are expected, not a failure.

**Adjust:** semantic tools are useful but discovery/identity/result contracts repeatedly force avoidable fallback.

**Stop/reconsider:** no semantic advantage appears on the chosen tasks, or native lifecycle complexity outweighs the benefit. Do not expand features to hide this result.

## Next milestone: strip the donor product

After an explicit dogfood Go, cut over and subtract **before** adding persistence or another provider:

1. Native registrations become the only active path; disable old startup/background registrations. Verify native use starts no old runtime.
2. Sever retained-tool dependencies on bridge settings, UI/renderers, trackers, formatting queues, and legacy session services.
3. Delete unreachable ACP, external-agent, JCEF, HTTP/MCP, and unused tool clusters in build-green slices.
4. Remove obsolete modules, packaging edges, CI tasks, and dependencies only after their surviving fixtures/coverage are owned elsewhere.
5. Verify the plugin archive and actual startup contain no old runtime. Rename modules/product identity last.

This is a second delivery milestone, not an excuse to keep two runtimes indefinitely. It needs its own measured deletion plan after the retained closure stabilizes.

## Out of MVP

Durable sessions, branching, compaction, steering/follow-up queues, multiple providers, direct API-key transport, device OAuth, web/search tools, debug catalog, notebook/database/memory/graph tools, extension/skills execution, multi-agent orchestration, telemetry/JFR, mutation testing, remote development, production release qualification, module extraction/renaming.

Basic safety, secret handling, context/output bounds, lifecycle, focused regression coverage, and usable Codex login are **not** deferred hardening.

## Plan revision

- Made the AST/PSI problem-space hypothesis explicit and falsifiable; native placement alone is not the claimed advantage.
- Made Pi source the behavioral reference, with a parity/deviation matrix in the implementation plan.
- Moved Codex subscription authentication into MVP based on the user's available access.
- Chose trusted-session opt-in; removed contradictory resolver/store and per-call approval requirements.
- Expanded the arbitrary ten-tool target to fifteen concrete capabilities, while narrowing dangerous donor options.
- Moved cancellation, stale-target protection, formatting ownership, context limits, and command bounds before live mutation.
- Kept subtraction immediately after the dogfood decision and before optional product expansion.
