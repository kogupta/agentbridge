# IntelliJ-Native Coding Agent

## Ordered Scope, MVP Boundary, and Phased Delivery Plan

**Base:** AgentBridge donor codebase  
**Design spirit:** Pi coding-agent  
**Runtime:** IntelliJ Platform

> **Product hypothesis:** an in-process IntelliJ agent with a small loop and a small set of semantic IDE tools can be more effective, simpler, and more pleasant than an external coding harness talking to IntelliJ through MCP/HTTP.

## 1. Guiding principle

- Reuse the expensive IntelliJ-specific work already present in AgentBridge: PSI/VFS access, refactoring, diagnostics, build/test integration, write actions, permissions, undo, and platform compatibility helpers.
- Replace the accidental runtime architecture: JCEF chat, ACP clients, external agent processes, MCP server, localhost HTTP bridge, and broad tool exposure.
- Copy Pi's product philosophy rather than its terminal implementation: small loop, small defaults, explicit extension points, few built-in workflows.
- Validate the product before building a general-purpose agent framework.

## 2. Scope rule

**MVP answers one question:**

Can a user open IntelliJ, give the agent a real coding task, have it navigate semantically, edit safely, inspect diagnostics, run tests, and finish successfully without leaving the IDE?

**Anything not required to answer that question is post-MVP.**

## 3. Ordered feature delivery

| Order | Feature | Deliverable | Scope |
|---:|---|---|---|
| 0 | Donor baseline | Build AgentBridge unchanged; identify retained tool/permission boundaries. | Required |
| 1 | Native agent loop | One session, one provider, messages → model → tool calls → tool results → repeat. | MVP |
| 2 | Native IntelliJ UI | Tool window, transcript, input, Send, Stop, simple status. | MVP |
| 3 | Minimal semantic tool set | Navigation, inspection, editing, diagnostics, build/test, command escape hatch. | MVP |
| 4 | Permissions + safe execution | Reuse existing resolver/store; ask/allow/deny; cancel pending approvals. | MVP |
| 5 | Cancellation + lifecycle | Stop current run; ignore stale callbacks; dispose with project/tool-window lifetime. | MVP |
| 6 | Dogfood + measurement | Use on real Hardwood work; compare against Pi + idea-facade. | MVP exit gate |
| 7 | Native cutover | Make native path default; old bridge path remains physically present but inert. | Post-MVP |
| 8 | Subtractive cleanup | Remove JCEF, ACP, external agents, MCP, HTTP bridge, unused tool families. | Post-MVP |
| 9 | Session persistence | Persist/reopen conversations; branching/compaction only when needed. | Later |
| 10 | Provider expansion | Second provider, then subscription OAuth / compatible APIs. | Later |
| 11 | Extension surface | Skills/prompts/tool packs/hooks after stable internal boundaries emerge. | Later |
| 12 | Hardening | Security, network tools, observability, compatibility matrix, release polish. | Later |

## 4. MVP definition

MVP is complete only when the following real workflow works inside IntelliJ:

- User enters a repository task in the native tool window.
- Agent inspects the code using IntelliJ semantic navigation rather than relying only on text search.
- Agent reads the relevant code and makes a bounded edit through retained IntelliJ tooling.
- IntelliJ reports focused diagnostics after the edit.
- Agent fixes resulting issues if needed.
- Agent runs the relevant build/test action.
- Agent returns a concise completion summary.
- Stop cancels an in-flight model/tool sequence without late mutations.

## 5. MVP tool catalog

**Target: about 10 tools, not 27+.** Prefer semantically strong tools; add another tool only when dogfooding proves a missing capability.

| Capability | Tool | Reason |
|---|---|---|
| Navigate | `search_symbols` | Find classes/methods/fields by semantic symbol. |
| Navigate | `find_references` | Find usages/references through IDE indices. |
| Inspect | `get_symbol_info` | Definition, type/signature, location, relevant metadata. |
| Inspect | `read_file` | Fallback source/context read. |
| Transform | `replace_symbol_body` or `edit_text` | One reliable code mutation path for MVP. |
| Transform | `refactor` | Use IntelliJ refactoring where it clearly beats textual editing. |
| Verify | `get_problems` | Focused diagnostics for touched code. |
| Verify | `build_project` | Compile/build through IDE-supported path. |
| Verify | `run_tests` | Run targeted tests. |
| Escape hatch | `run_command` | Command-line fallback, always permission-gated. |

## 6. Explicitly out of MVP

- SQLite or other durable conversation database.
- Conversation tree, branching, import/export, or long-session compaction.
- Multiple providers.
- ChatGPT/Codex OAuth or other subscription authentication.
- Anthropic signed-thinking preservation or cross-provider canonical history.
- OpenAI-compatible / Ollama compatibility matrix.
- Web search, `fetch_web_page`, raw HTTP, SSRF hardening.
- Large git tool family; use permission-gated `run_command` if git is needed.
- Debug, notebook, database-browser, Qodana/Sonar, memory/graph, browser automation, or other AgentBridge tool families.
- General plugin/extension ecosystem.
- Telemetry/JFR, mutation testing, elaborate persistence migrations, or production release polish.

## 7. Phased implementation plan

### Phase 0 — Establish the donor boundary

**Goal:** prove what is worth keeping before writing new architecture.

- Build/test/package AgentBridge unchanged and record the baseline.
- Identify the exact callable boundary around `ToolRegistry` / `PsiBridgeService` / `ToolDefinition` and permission services.
- Select the initial ~10 tools and their transitive implementation dependencies.
- Do not delete anything yet.

**Exit criterion:** one small test/action can invoke a retained IntelliJ tool directly in-process, without MCP.

### Phase 1 — Native loop spike

**Goal:** prove the smallest in-process agent cycle.

- Add `nativeagent/` package inside `plugin-core`; no separate core module yet.
- Implement `AgentSession`, `AgentLoop`, `Provider`, `ToolInvoker`, and event callbacks.
- Support one provider only.
- Support assistant text + tool calls + tool results + continuation.
- Use fake-provider tests first, then one live provider smoke.

**Exit criterion:** hidden/dev action completes a multi-step model → IntelliJ tool → model turn.

### Phase 2 — Minimal native UI

**Goal:** make the spike usable without JCEF.

- Native IntelliJ tool window.
- Read-only transcript plus separate prompt input.
- Send and Stop.
- Minimal streaming/status display.
- No rich chat framework, markdown engine, branch browser, or session manager.

**Exit criterion:** a user can complete one real task entirely through the native UI.

### Phase 3 — Semantic coding MVP

**Goal:** make the native path useful for actual repository work.

- Expose the selected navigation, inspection, transform, diagnostics, build/test, and command tools.
- Reuse existing permissions instead of creating a second authorization model.
- Ensure edits use existing write/undo/conflict protections.
- Add focused diagnostics after successful edits where existing infrastructure allows it.
- Implement reliable cancellation and stale-result rejection.

**Exit criterion:** the full MVP workflow succeeds on representative Java repository tasks.

### Phase 4 — Dogfood and decide

**Goal:** validate the product hypothesis before further architecture work.

- Use the native agent on several real Hardwood issues.
- Compare task completion, number of turns/tool calls, failed edits, build/test feedback, friction, and subjective usability against Pi + idea-facade.
- Record tools that were never used and missing operations that repeatedly forced `run_command` or raw text editing.
- Do not add features merely because AgentBridge or Pi has them.

**Exit criterion:** explicit **Go / Adjust / Stop** decision. Continue only if the native path is materially better.

### Phase 5 — Cut over and subtract

**Goal:** turn the validated spike into the actual product.

- Make native UI/engine the default registered path.
- Keep old classes temporarily but stop starting JCEF chat, external agents, ACP clients, MCP server, and HTTP bridge.
- Then delete those runtime clusters in build-green slices.
- Prune tool families not justified by dogfooding.

**Exit criterion:** one IntelliJ plugin; no external coding-agent process, MCP bridge, JCEF chat, or localhost PSI HTTP path.

### Phase 6 — Sessions and model ergonomics

**Goal:** add capabilities that repeated use proves necessary.

- Durable session history.
- Resume/reopen.
- Context budgeting/compaction when real sessions hit limits.
- Steering/follow-up queue if needed.
- Only then consider extracting a pure `native-agent-core` module if separation now pays for itself.

**Exit criterion:** longer real-world sessions remain usable without complicating the basic loop.

### Phase 7 — Provider expansion

**Goal:** add models without destabilizing the agent core.

- Add a second provider first to test the provider boundary.
- Then add Codex subscription OAuth if desired and policy/terms are acceptable.
- Add OpenAI-compatible/Ollama/other transports only from concrete usage demand.
- Keep provider-specific wire behavior outside the canonical agent loop.

**Exit criterion:** provider choice is replaceable without provider conditionals spreading through the engine.

### Phase 8 — Extension API and hardening

**Goal:** make the validated product extensible and releasable.

- Stable hooks for prompts/skills/tool packs/events.
- Network/web capability only if needed, with proper security boundaries.
- Compatibility testing, resource lifecycle tests, packaging cleanup, security review, performance/observability.
- Rename/restructure modules only after the final architecture has converged.

**Exit criterion:** maintainable releasable plugin with extension points driven by real usage rather than speculative framework design.

## 8. Go / no-go criteria after MVP

Continue the project only if dogfooding shows clear advantage in several of these areas:

- Fewer failed or ambiguous edits because PSI/refactoring operations replace text patching.
- Faster navigation and evidence gathering through IntelliJ indices.
- Better edit → diagnostics → fix → test feedback loop.
- Lower integration complexity than Pi + MCP/facade for normal work.
- Native UI/lifecycle feels simpler and more reliable than external-agent orchestration.
- A small semantic tool catalog is sufficient for most tasks.

**Stop or reconsider if:** the native agent mostly falls back to `read_file` / `edit_text` / `run_command`, semantic tools provide little measurable value, or IntelliJ threading/lifecycle constraints make the in-process loop substantially more complex than the external harness.

## 9. Target end state

```text
Native Tool Window
        |
        v
   AgentSession
        |
        v
    AgentLoop  ------> Provider
        |
        v
 PermissionGate
        |
        v
Small IntelliJ Semantic Tool Set
 PSI / VFS / Refactor / Build / Test
```

Desired final properties:

- one IntelliJ plugin
- small in-process agent loop
- native UI
- semantic IDE tools
- explicit permissions
- no Node
- no JCEF chat
- no ACP runtime
- no MCP server
- no external coding-agent CLI
- no localhost PSI bridge

Unless a later use case specifically justifies one.

## 10. Immediate next implementation slice

1. Add `nativeagent/` to `plugin-core`.
2. Define `AgentLoop`, `Provider`, `ToolInvoker`, `AgentEvent`, and minimal message/tool-call types.
3. Create a `NativeToolInvoker` that calls the retained AgentBridge tool boundary directly.
4. Wire 3 tools first: `search_symbols`, `read_file`/`get_symbol_info`, and `get_problems`.
5. Use a fake provider to prove one tool round-trip.
6. Add one real provider.
7. Only after that add native UI and expand toward the ~10-tool MVP set.
