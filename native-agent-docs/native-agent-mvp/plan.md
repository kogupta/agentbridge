# Pi-modeled IntelliJ-native agent — MVP implementation plan

## Status and authority

This is a new implementation basis, not an amendment that silently approves the 15-stage merged plan. Scope: `intellij-native-agent-phased-scope.md`. Historical plans/review remain historical.

User requirements:

- R1: restrict the model's source-code problem space through AST/PSI and deterministic IntelliJ operations;
- R2: model agent behavior after `/home/muku/depot/personal/cli-tools/pi`, not an invented orchestration framework;
- R3: use ChatGPT subscription access, not an assumed API key;
- R4: usable native coding workflow with safe mutations, diagnostics, and targeted tests;
- R5: remove irrelevant AgentBridge runtime after proving the native path;
- R6: hand implementation to another model with a bounded executable specification.

The author accepts R1 as a testable hypothesis, not evidence that native placement is intrinsically better than Pi + idea-facade. Index-backed operations reduce identity ambiguity; large usage sets, invalid PSI, and unsupported languages still require explicit scope and errors.

Only documents were changed during planning. No donor build baseline, credentialed provider smoke, UI smoke, or implementation tests have run. Do not convert planned verification into a passed claim.

## Source basis

`source-manifest.json` contains SHA-256 values for 13 inspected donor/Pi files. Repository state observed through the IDE: branch `master`, eight untracked files, no tracked modifications at inspection. Implementation must create a non-default branch `native-agent-mvp` through IDE VCS tooling and preserve all unrelated untracked files. Recheck changed source hashes before using the evidence below; do not assume old line numbers survive.

`AB` below is `plugin-core/src/main/java/com/github/catatafishen/agentbridge/`. `PI` is `/home/muku/depot/personal/cli-tools/pi/`.

| Evidence | Current fact | Consequence |
|---|---|---|
| `AB/psi/PsiBridgeService.java:323-369,399-419,438-464,519-539` | Dispatch logs arguments, invokes bridge permissions, installs UI tracking, and calls `ToolDefinition.execute` under locks. | Do not wrap this method as native dispatch. |
| `AB/psi/tools/Tool.java:22-54` | Each tool owns mutable `argumentsHash`; execute validates required parameters but does not establish the native schema/security contract. | Do not share tool instances with concurrent bridge calls. Validate native arguments at the native boundary. |
| `AB/psi/tools/editing/EditingTool.java:59-79,110-147` | Resolution returns the first same-name match, or nearest line; it stores line-rounded locations. | Native symbol changes must use exact resolved identity and PSI ranges, not this fallback. |
| `AB/psi/tools/editing/ReplaceSymbolBodyTool.java:89-143` | Queues EDT work; writes later; caller times out after 15 seconds; formatting is queued. | Cancellation must reach the actual runnable and formatting; a future timeout alone is not sufficient. |
| `AB/psi/tools/editing/EditingTool.java:52-56` | Formatting goes to `FileTool.queueAutoFormat`. | Native loop must own flush/cancel/completion rather than depend on ACP turn end. |
| `AB/psi/tools/navigation/SearchSymbolsTool.java:69-102,145-170` | Search is paginated; exact name search combines word-index processing with PSI named-element checks. | Do not claim every donor search is a perfect symbol database. Extract typed candidates; retain scope/completeness. |
| `AB/psi/tools/file/EditTextTool.java:58-73` | Donor permits omitted path, replace-all, and case-insensitive matching. | Narrow native schema and enforce the restrictions at execution. |
| `AB/psi/tools/refactoring/RefactorTool.java:103-150,167-210` | Queued EDT refactor; rename finds usages then calls platform refactoring; safe-delete performs a separate reference check and delete. | MVP exposes rename only; move discovery off the write boundary and revalidate usages before commit. |
| `AB/psi/ToolReadinessGate.java:56-69,157-176` | Central readiness gate exists; smart-mode unexpected failure currently returns null. Error hints name tools outside this MVP. | Extract cancellable fail-closed native readiness behavior and native hints; do not reuse permissive failure silently. |
| `AB/psi/tools/infrastructure/RunCommandTool.java:111-156` | Saves documents; routes recognized tests to native testing; blocks some grep; supports donor hidden environment injection. | Preserve toolchain behavior but prohibit hidden model arguments and avoid unsupported tool recommendations. |
| `AB/psi/tools/RunPanelExecutor.java:80-85,141-151` | Output is collected in an unbounded StringBuffer; timeout/interruption destroys the handler. | Bound capture before enabling commands; retain owned process handles and report termination honestly. |
| `plugin-core/src/main/resources/META-INF/plugin.xml:77-80,367-376` | Existing tool window and PSI/custom-MCP/graph startup registrations exist. | Native development mode needs explicit startup isolation, not just an extra tool window. |
| `plugin-core/build.gradle.kts:151-162,381-385,413` | Sandbox/package depend on mcp-server; minimum build is 253. | Physical artifact removal is later; retain Java/platform settings and do not claim native-only packaging during MVP. |
| `PI/packages/agent/src/agent-loop.ts:156-273` | Loop streams an assistant response, executes tools, appends results, and supports steering/follow-up queues. | Port the small loop behavior; defer the queues explicitly. |
| `PI/packages/agent/src/agent-loop.ts:226-233,379-403,409-485,607-718` | Length-truncated calls are not executed; sequential mode exists; unknown tools/schema errors become tool errors; cancellation is passed into tools. | Port sequential mode and validation/error semantics; retain no-truncated-execution invariant. |
| `PI/packages/agent/src/agent-loop.ts:279-370,784-803` | Provider conversion is separate; tool-result messages have their own role and call identity. | Separate wire conversion from loop; retain three message roles. |
| `PI/packages/coding-agent/src/core/system-prompt.ts:80-135,150-165` | Prompt is built from available tool snippets/guidelines and supplied project context. | Build native tool-aware prompt. Do not copy Pi's shell guidance or paths to its own docs. |
| `PI/packages/ai/src/auth/oauth/openai-codex.ts:26-39,149-189,293-394,445-506` | Public client, PKCE browser login, state, localhost:1455 callback, manual input, refresh. | Browser flow is the one MVP login path; preserve verifier and state across manual fallback. |
| `PI/packages/ai/src/api/openai-codex-responses.ts:519-596,637-643,721-758,1587-1639` | Codex-specific instructions/body, endpoint, terminal normalization, account ID, headers. | Port Codex, not direct OpenAI Responses. |
| `PI/packages/ai/src/api/openai-responses-shared.ts:138-353,359-396` | Replays reasoning items and assistant item identity/phase; function_call and function_call_output use matching call IDs. | Retain same-model provider metadata, not a flattened transcript. |
| `PI/packages/ai/test/openai-codex-stream.test.ts:49-96,157-165` | Fixtures are generated inline; text, incomplete, terminal, and header behavior are exemplified. | Derive Java fixtures; do not claim captured fixture files already exist. |
| `PI/packages/ai/src/providers/openai-codex.models.ts:4-8` | Imports generated data absent from this checkout. | No current model-availability list is established. Require explicit model configuration and live account verification. |
| `PI/LICENSE:3-13` | MIT copyright and permission notice must accompany copies/substantial portions. | Preserve attribution for ported code/fixtures. Code license is not authorization to use an OAuth client or service. |

## Pi parity and deliberate deviations

| Behavior | MVP decision |
|---|---|
| Small loop | Port classic `agent-loop.ts` ordering, not `harness/` distributed/durable architecture. |
| Canonical messages | `UserMessage`, `AssistantMessage`, `ToolResultMessage`; no SQLite schema or cross-provider abstraction. |
| Events | Run/message/tool start-update-end events; every event tagged with session/run identity. |
| Tool execution | Select sequential mode; allow a model response to request several calls, but execute in source order. |
| Validation | Unknown tool or invalid schema becomes an error result. Malformed JSON/duplicate wire IDs invalidates the complete provider response before any call executes. |
| Truncation | Like current Pi, no LENGTH tool execution. For MVP, stop after recording rejection results rather than automatically continuing an output-limit loop. |
| Provisional output | Native display state is separate from accepted conversation, unlike Pi's temporary in-place partial-message update. |
| Steering/follow-up | Deferred; active input is draft only. No cancel-and-resend masquerading as steering. |
| Codex | Same-model reasoning/message/call metadata retained; SSE only; uncompressed JSON; no WebSocket cache, grammar tools, or deferred tool search. |
| Authentication | Pi browser PKCE flow; manual full callback URL with mandatory state rather than accepting raw code without state; five-minute native deadline. Device flow deferred. |
| Tool philosophy | Small explicit tools and descriptions; IntelliJ replaces shell source navigation. No generic facade or extension framework. |
| Instructions | Tool-aware native prompt plus explicit per-session user context attachment; automatic Pi project-context discovery is deferred. No silent discovery of host/global instructions. |
| Trust | Explicit native trusted-session opt-in, then no per-call approval; not a sandbox. |

## Global invariants

I1. At most one active native run per project. Loop/domain/provider own no IntelliJ types; platform integration lives in adapters. No Node/ACP/MCP/HTTP hop on the native execution path.

I2. `AgentSession` owns accepted in-memory messages and one `RunId`. A run captures model ID, tool catalog generation, instructions, and limits. No model/catalog change mid-run. New Session/model change is allowed only at idle and invalidates every old handle.

I3. Provider request attempts do not execute tools. Only a terminal validated response may create a batch. Tool invocations have one local CallId and terminal outcome; the harness never retries an executed call. Retrying the next provider request with prior tool results does not replay prior effects.

I4. Each accepted assistant tool call gets exactly one result before a subsequent provider request. Stop records CANCELLED_NOT_STARTED for remaining calls; an already-started effect reports its real terminal outcome. A partial provider response never enters accepted context.

I5. Native symbol handles are generated by IntelliJ resolution, not by the model. Ambiguity, stale document state, unsupported language, unavailable indices, and incomplete search are explicit outcomes, not permission to choose a first/nearest match or emit an authoritative empty result.

I6. Mutation approval and mutation correctness are separate. Within a trusted session, native IDE mutations are restricted to writable project content. All touched files/targets are revalidated under the final mutation boundary. Unsaved editor state is authoritative. No raw offsets survive an intervening document change.

I7. No EDT wait while holding a read action. Do not wrap refactoring processors in a broad read/write action. Read discovery is cancellable and bounded; the actual write command is short, named, and undoable. Check `PlatformApiCompat` and supported IntelliJ APIs before implementation.

I8. Stop wins against not-yet-started side effects through one synchronized admission boundary shared by cancellation and effect start. This is not just an unchecked boolean before scheduling. Once an effect begins, Stop may wait for that effect; the UI remains STOPPING until it and owned cleanup settle. No new run can race it.

I9. Run-owned formatting, saves, diagnostic waits, request bodies, HTTP requests, process handlers, and callbacks are cancellable or tracked to completion. No donor background queue can mutate after the run is reported stopped. Cleanup must not block EDT.

I10. Native prompts/tool arguments, OAuth URLs/codes/verifiers, credentials, and full provider responses do not enter application logs. Sanitize exceptions before logging; do not simply log provider exception bodies. PasswordSafe is the sole credential store. Commands accept no secret injection fields.

I11. Every implementation stage leaves the donor and native code buildable. Legacy registrations/catalog remain intact until a separately qualified cutover; native mode isolates runtime activation without deleting donor code. Selected operation helpers have one implementation used by both frontends where extracted.

## Concrete interfaces and state

Package root: `com.github.catatafishen.agentbridge.nativeagent` within `plugin-core`.

- `domain`: immutable `UserMessage`, `AssistantMessage`, `ToolResultMessage`, `ToolCall`, `Id`, `RunId`, `ToolOutcome`, and bounded event values. Tool arguments are immutable parsed JSON snapshots; mutable Gson trees stay in adapters/wire codecs. Tool results have `status`, `code`, bounded `content`, `details`, and mutation/effect disposition.
- `engine`: `AgentSession`, `AgentLoop`, `Provider`, `ToolInvoker`, `RunCancellation`, and `AgentEventSink` only. No generic hook registry, strategy hierarchy, scheduler framework, or persistence contracts.
- `provider/codex`: request codec, SSE reader/event assembler, accepted same-model replay items, `CodexProvider`.
- `auth`: PasswordSafe-backed account credential source, single-flight refresh, browser login attempt. Expose a credential interface to the provider; never expose credentials to tools or events.
- `tools`: native catalog schemas, selected tool instances/adapters, `NativeExecutionContext`, typed semantic/read results, and session handle registry. Extract reusable operation logic into existing tool packages; never copy PSI algorithms into a second implementation.
- `ui`: tool window, session enablement, login/model controls, editor transcript, input and status. No old chat framework dependency.

Suggested method boundaries (names may follow local conventions; ownership and semantics must not change):

- `Provider.generate(RequestSnapshot, RunCancellation, EventSink) -> CompletedResponse | RejectedResponse`.
- `ToolInvoker.invoke(ToolCall, NativeExecutionContext) -> ToolOutcome`; sequential, terminal outcome required, no detached future presented as success.
- `NativeExecutionContext`: RunId, cancellation/admission gate, deadline, tracked resources, touched-file set; created once for each invocation and explicitly captured by queued work. A ThreadLocal alone is not propagation to EDT/background processors.
- `SessionHandleRegistry`: bounded project/session-local handles; no serialization of PSI pointers to the model; dispose on New Session/content close. Symbol handle maps to a smart pointer plus language/owner/signature and document fingerprint. Read handle maps to file identity/version/hash and inspected ranges. Validate pointers under read access before use.

Run states: `IDLE -> REQUESTING -> EXECUTING_TOOLS -> REQUESTING ... -> IDLE`. Any active state may enter `STOPPING`; it enters `IDLE` only after admitted effects settle, or `DISPOSED` when the content lifetime ends. Error returns to idle with a visible categorized result. DISPOSED never starts new work. Authentication is a separate bounded UI operation, not an agent turn.

Draft text is never part of a request until Send. Double Send is rejected before a second user message is appended. Stop during provider output discards that provisional response; Stop during tools preserves the accepted assistant message and exactly one terminal result for each call. Closing content suppresses further UI updates but still drains terminal effect accounting off EDT.

## Native tool and result contracts

Catalog IDs are exactly the fifteen listed in the scope; no generated aliases. Stage 2 exposes the seven read tools; Stage 3 adds four mutation and four verification/execution tools. Commands are part of trusted execution, not an unrestricted second provider catalog.

Common response envelope: `status = ok|error|pending`, machine `code`, bounded human `content`, typed `details`, and `truncated`. Search/details include `scope`, `complete`, and `nextOffset` when more results may exist. Do not parse donor rendered text to invent typed fields; extract the underlying result-producing logic and render it separately for legacy callers.

### Reads and identity

- `find_file`: explicit name/path query; source/project scope by default; generated/excluded directories omitted. Return at most 50 paths per page, hard cap 100.
- `search_symbols`: named query and scope; project is default, libraries require explicit scope. Return stable session `symbol_id`, language, kind, name, owner/qualified signature when available, file, and exact start/end range. Limit 50/cap 100. Native wildcard listing requires a path/type restriction, not a repository-wide AST dump.
- `get_file_outline`: one explicit file; declarations only, at most 100 entries/page. Each supported declaration has `symbol_id`. Unsupported PSI reports UNSUPPORTED_LANGUAGE, not an empty successful outline.
- `get_symbol_info`: accepts `symbol_id`; returns identity, exact declaration range and bounded source, plus `read_id` for inspected source. Unknown/evicted handles return STALE_HANDLE with rediscovery instructions.
- `find_references`: accepts `symbol_id` and explicit/default project scope. Return actual resolved references and bounded locations; separately report partial search/index readiness. A textual occurrence is not a reference result.
- `read_file`: path plus start/end lines, default 100 and maximum 300 lines, UTF-8 result cap 24 KiB. Use open Document contents; return `read_id` and actual returned ranges. Large/binary files require a smaller supported read or explicit unsupported error, not an unbounded load.
- `search_text`: explicit file/directory scope and literal query; regex is deferred. Cap 50 matches/page, 100 maximum; each excerpt is bounded. No claim that text matches identify declarations. Exclude generated/excluded paths by default.
- Maximum 2,048 live handles per session; LRU eviction is safe because mutations fail on an evicted handle. File edits invalidate all handles for affected files, including command/VFS changes. Do not retain full file copies per handle; share fingerprint metadata per current file version. Cancellable file hashing/size checks must precede large allocation.

### Mutations

- `replace_symbol_body`: native schema accepts `symbol_id`, `read_id`, and `new_body` containing the complete Java method declaration. The source range must have been returned in full by an inspection/read. Reject altered method name, parameter types/arity, return type, modifiers, or type parameters; signature change is not body replacement. Parse candidate code using the supported language PSI factory. Replace the exact PSI element/range, not entire lines. An unsupported declaration kind returns UNSUPPORTED_OPERATION. Reuse/extract donor application logic; do not call its first/nearest-name resolver.
- `edit_text`: native requires path, `read_id`, nonempty `old_str`, and `new_str`; exactly one case-sensitive match wholly within an inspected range. No path alias/active editor, replace-all, regex, or full-file replacement route. Used for imports/config/localized changes; not cross-file rename. Revalidate full document fingerprint and matched range at commit.
- `write_file`: native creates a new UTF-8 text file only; path and content required. Maximum content 128 KiB. Validate normalized real parent and target nonexistence under the final admission/write boundary. Existing target, symlink escape, read-only/unsupported parent, or new existence race yields conflict. Create directories/file through supported VFS/undo behavior; do not overwrite a concurrently created file. Creation can include complete new Java test classes.
- `refactor`: native schema `operation=rename`, `symbol_id`, `read_id`, `new_name`. Discover actual usages and affected files outside the write action; cap at 100 affected files for MVP. Show an actionable TOO_BROAD error above that cap. Revalidate every usage file's identity/document fingerprint and writability before commit. Recheck modification tracking so new usages introduced after discovery cannot be missed; on any project PSI change during preparation, rediscover or fail STALE_TARGET rather than use a partial snapshot. Run supported IntelliJ rename conflict checks; no textual fallback, automatic overwrite, or modal prompt that the agent cannot answer. Unsupported/modal-required/conflicting rename returns a structured error without changes. Keep all changes under the platform's named undoable refactor command.
- All mutation results include actual touched files and terminal effect disposition: NOT_STARTED, COMPLETED, or FAILED_AFTER_START. Do not claim atomic rollback of a platform operation if it partially failed; surface changed files and require inspection before continuing.
- Refuse current files over 2 MiB for native mutation in this MVP. This bounds conflict hashing/validation; an explicit error is preferable to blocking EDT hashing arbitrary files. Fingerprints contain canonical file identity, modification stamp and content hash derived from the editor snapshot; compare stamps/identity at commit and recompute only if needed to establish equivalence. Never wait on an external file/network operation while holding a write action.

### Formatting, diagnostics, build/test, commands

- Disable bridge-owned deferred formatting for native calls. Collect touched files in the native run. Flush formatting once at a successful tool-batch boundary, before the next model request; flush before build/test/command if prior tools in the batch edited files. Import optimization runs with that flush, never between an import-only edit and the subsequent edit that uses the import within the same batch. All formatting/import mutations go through the same admission/precondition mechanism and their completion is awaited. Stop cancels unstarted formatting rather than queueing it beyond run lifetime.
- On edits, commit affected documents and collect focused diagnostics after the corresponding formatting flush. Later edits in a batch may supersede an earlier diagnostic snapshot: tag each result with observed file version; attach batch-final refreshed diagnostics to the final mutation outcome before the next model request. If unavailable after 5 seconds, return `pending`, not zero problems; `get_problems` can query again. Cap 50 problems/file and 200 overall. No whole-project inspection per edit.
- `build_project`/`run_tests`: retain supported native execution and structured exit/failure reporting. Native request must give explicit test/class/file scope; do not infer all tests from an empty selector. Save documents only after rechecking run admission; register the execution handle before start; await real completion. Existing build-in-progress returns BUSY; do not start a duplicate. Stop requests supported cancellation and waits for terminal outcome; no blanket interruption of IntelliJ worker threads. Run panel is the place for detailed output.
- `run_command`: finite, noninteractive command, explicit short title and project working directory. Default 60 seconds, maximum 180 seconds for MVP; no detached jobs or interactive input. Keep existing shell parsing/toolchain environment, but reject all non-schema keys including `_env.*`. Preserve native test routing; schema/help cannot recommend removed terminal tools. Capture continuously into a thread-safe bounded 256 KiB byte ring, return at most 24 KiB plus truncation/exit metadata. No replay-to-paginate: offset does not rerun a command; native schema omits donor offset pagination. Terminate owned root/descendants best-effort, await/rescan for up to 5 seconds, and report survivors; never claim guaranteed process containment. Lingering inherited pipes must not hang the run after that bound. Mark unresolved execution visibly and block automatic continuation, rather than claim successful cancellation.
- If tracked processes survive the five-second termination attempt, keep the run in STOPPING with a visible TERMINATION_INCOMPLETE substatus and the observed PIDs; release pipe readers after bounded drainage, but do not enable Send until tracked processes are observed exited. Tell the user when manual termination is required. During project disposal, report survivors and release IDE resources without blocking EDT; escaped/uncontained OS processes are a disclosed limitation, not a successful cancellation claim.
- Await VFS refresh after commands on success/error/timeout/Stop whenever the process started; invalidate affected handles conservatively for the whole project if touched paths are unknown. Capture project Documents' stamps at launch. If an editor changed while the command also changed its disk file, surface an external-change conflict and do not automatically save/reload over either version; require user resolution before continuation.
- Product prompt says use semantic tools for identity/refactoring, scoped text for literals, native build/test first, command only for genuine escape cases. Commands are not a security-proof way to enforce semantic-first behavior; record fallback reasons during dogfood. Follow project VCS instructions; no automatic git commit/push. This repository's IDE-git implementation rules remain authoritative even though the final product may use commands in other projects.

## Codex-only provider and credential contract

1. Endpoint: `https://chatgpt.com/backend-api/codex/responses`; no custom endpoints or general HTTP tool. OAuth endpoints and callback come from the inspected Pi browser flow. Production URLs are fixed; tests inject a transport endpoint without shipping a configurable network bypass.
2. Browser OAuth: S256 PKCE with fresh verifier/challenge and cryptographic state; loopback-only port 1455, path `/auth/callback`, one successful completion. Five-minute total deadline. Open the system browser via supported platform API. If bind fails or browser redirect cannot reach the IDE, offer full callback URL paste tied to the same attempt. Require exact callback scheme/host/port/path and matching state; reject duplicate code/state parameters and raw code-only input. Cancellation and logout close listener/requests and clear transient code/verifier; do not log URL or response payload.
3. Pi's observed public client ID is `app_EMoamEEZ73f0CkXaXp7hrann`; scopes `openid profile email offline_access`; authorization flags `id_token_add_organizations=true`, `codex_cli_simplified_flow=true`. This source fact does not establish permission to reuse that client. Before real credential use, verify current provider-supported authentication/client policy. If a supported registration/flow is unavailable, mark the live-auth gate blocked rather than impersonate a client or switch to an API key without agreement. No policy/Marketplace approval is claimed by this plan.
4. Exchange and refresh form fields follow Pi `openai-codex.ts:149-189`. Validate nonempty access/refresh token and positive bounded expires_in. Derive account ID from the OAuth response token claim `https://api.openai.com/auth.chatgpt_account_id` as metadata, not as local proof of cryptographic JWT validity. Missing account ID blocks a request. Store account/tokens/expiry together in one PasswordSafe entry. Settings store only nonsecret model ID and UI preferences.
5. One application-level credential owner provides single-flight refresh across projects. Refresh before expiry with a 60-second safety margin; one forced refresh/retry on 401, then require login. Version credential updates so logout/new login cannot be overwritten by late refresh. Run cancellation unsubscribes its wait; logout/disposal cancels the owned credential operation. Do not read external Pi/Codex credential files.
6. Model ID is a required setting entered once by the user. No default availability assertion from unhydrated Pi generated data or this harness's model. Start with one known-to-the-user Codex model during manual qualification; no automatic fallback or silent model switch. Reasoning effort `medium` only after that configured model accepts it; otherwise configuration error, not a hidden request rewrite. Model changes require New Session.
7. Requests: `model`, `store=false`, `stream=true`, `instructions` containing the native prompt, `input` with complete accepted history, `include=["reasoning.encrypted_content"]`, session-stable `prompt_cache_key`, `text.verbosity=low`, `tool_choice=auto`, and flat function tools. Preserve Pi's `parallel_tool_calls=true` wire field while executing calls sequentially locally. Send uncompressed JSON. Omit unsupported temperature/max-output fields, system input item, grammar tools, WebSocket state, hosted tools, and deferred-tool metadata.
8. Headers: Bearer token, chatgpt-account-id, OpenAI-Beta responses=experimental, accept text/event-stream, content-type application/json. Use truthful native product originator/User-Agent rather than claiming to be Pi; verify acceptance in the live smoke. Session/request IDs are nonsecret stable/per-attempt identifiers as appropriate. Never send an x-api-key header for subscription access.
9. Use JDK HttpClient `sendAsync` with `BodyHandlers.ofInputStream`, consumed on the owned turn virtual thread by a strict incremental UTF-8/SSE reader. This deliberately avoids building the merged plan's custom demand queue. Retain request future and opened body atomically; registering a resource after cancellation closes it immediately. Closing body/cancelling request must wake reads; interruption alone is not the cancellation mechanism. Network callbacks do not perform PSI or wait on EDT.
10. SSE: handle LF/CRLF, comments, multiline data, split UTF-8, chunk splits at every byte, and final frame without blank delimiter. [DONE] is not a substitute for a terminal response. Assemble output items by provider index/ID; validate incremental event identity and final items. Support text, reasoning summary/encrypted reasoning, and function call argument events. Normalize response.done/completed/incomplete as Pi does, while honoring status/incomplete reason. Unknown optional events may be ignored; unknown required output item types reject the response rather than disappear from history.
11. Keep accepted Codex replay items (reasoning encrypted_content and IDs, assistant text IDs/phase, function-call item ID/call_id/name/arguments) as immutable validated provider metadata attached to the assistant message. Tool result role remains `toolResult`; codec maps it to function_call_output with the original call_id. No provider IDs constructed from text parsing, and no detached tool result or dropped reasoning item in the next request. Same configured model only; no multi-provider normalization framework.
12. Response limits: 1 MiB per SSE event, 4 MiB decoded stream per request, 128 KiB per tool argument object, 32 calls per response. Request JSON cap 64 KiB including instructions/catalog/history; native transcript cap 1 MiB with explicit oldest-display eviction (accepted context still subject to request cap). These are MVP application limits, not claims about model context-window sizes. Before a request exceeds its cap, stop with CONTEXT_FULL and offer New Session. Provider context-length errors are also actionable and not retried. Do not silently truncate accepted calls/results or claim exact token accounting. Log counts, not bodies.
13. Retry at most once per provider request, and only before that request's response is accepted: transient transport/EOF, 429 excluding quota/billing exhaustion, or 5xx. Delay 1 second unless a valid Retry-After requires longer, capped at 10 seconds; above that report the wait requirement instead of retrying early. Roll back provisional UI attempt before retry. Authentication refresh retry shares the two-attempt request ceiling. No retries for malformed required content, unsupported model, bad schema, billing/quota, context limit, user Stop, or executed tool calls.

### Provider/run behavior grammar

| Input/outcome | Accepted context | Effects | Next step |
|---|---|---|---|
| completed text, no calls | Append assistant | None | Idle |
| completed valid calls | Append assistant and exactly one result/call | Sequential after per-call schema validation | Next request if run active and limits allow |
| completed unknown tool/schema error | Append assistant plus that call's error result | No effect for invalid call; other valid calls follow source order | Model can correct in next request |
| duplicate call IDs, malformed JSON, unsupported required output | No partial assistant | None | Visible protocol error; idle |
| LENGTH text only | Append marked incomplete text | None | Idle; no fabricated success |
| LENGTH with syntactically valid calls | Append assistant plus TRUNCATED_NOT_EXECUTED results | None for the batch | Idle; user may request continuation |
| LENGTH with malformed call fragments | No partial assistant in context; provisional text remains labeled failed display only | None | Idle |
| EOF before terminal / transient failure | No partial assistant | None for this request | One request-local retry, then error |
| Stop during provider request | No partial assistant | None for this request | Cancel/close, then idle |
| Stop before queued effect admission | Accepted call gets CANCELLED_NOT_STARTED | None for that effect | Fill remaining skipped results; idle after cleanup |
| Stop after effect admission | Accepted call gets actual terminal outcome; others cancelled | Existing effect may finish; no next effect | STOPPING until settled |
| Tool failure after side effect | Record FAILED_AFTER_START and touched paths | Do not replay | Stop automatic continuation; require inspection/user action |
| Oversize/context cap | No silently truncated exchange | No new effects | Explicit limit error/New Session |

## Semantic implementation stages

### Stage 0 — Record donor baseline and selected execution closure

**Purpose:** establish executable source and build boundaries before modifying tools.

**Depends on:** None.

**Preconditions:** scope accepted; no implementation on master; preserve unrelated files. Independent review status is recorded honestly before delegating code.

**Discovery / implementation method:** use IDE VCS status/history/branch tools; inspect constructors/factories and semantic references for the fifteen IDs, `Tool`, `ToolDefinition`, `PlatformFacade`, `FileTool`, readiness, format queues, RunPanelExecutor, build/test handlers and startup entries. Record edges to bridge/ActiveAgentManager/renderers/session/MCP. Inspect Pi source hashes and required event branches. Do not delete or implement parallel replacement tools.

**Expected scope:** evidence under `.agent-work/native-agent-mvp/`; no production edits.

**Implementation:** run donor build, unit tests, packaging once; record command, environment and outcome including pre-existing failures. Establish the exact non-default branch/base commit using IDE VCS. Document where selected instances can be constructed without PsiBridgeService and where helper extraction is required. If a missing API/semantic operation blocks a specified contract, report it before code; do not downgrade to text approximation.

**Tests:** baseline only; no new tests asserting source text or wiring.

**Completion contract:** Outcome: measured donor dependency/baseline manifest. Invariants: no deletion or tracked source change. Artifacts: baseline and closure notes. Evidence: IDE build, `:plugin-core:test`, `:plugin-core:buildPlugin` results and selected-source references.

**Commit:** no source commit required; evidence-only stage.

### Stage 1 — Add Pi-modeled loop and explicit effect contracts

**Purpose:** prove state transitions and side-effect admission before platform mutation.

**Depends on:** Stage 0.

**Preconditions:** source parity table checked; Java 21 existing test setup available.

**Discovery / implementation method:** port behavior from Pi runLoop, sequential execution, tool preparation and tool-result construction. Use existing Java/Gson/test conventions; no new build plugins or pure-core module.

**Expected scope:** `nativeagent/domain`, `nativeagent/engine`, focused JUnit tests under corresponding plugin-core test packages.

**Implementation:** immutable message types, accepted/provisional separation, RunId, one run/session, sequential loop, exact result pairing, typed outcomes and cancellation admission. Add bounded request/run limits: 20 model responses, 100 tool invocations, 15-minute run deadline. Exceeding a limit stops before new effects, fills skipped results, and returns an actionable status. Define effect resource registration/terminal accounting for later adapters. Do not add hidden production dev actions or fake production providers; fakes are tests only.

**Tests:** multi-call order, unknown/schema failure, length/malformed rejection, double Send, Stop at each state, resource-registration-vs-cancel race, no replay of a counted non-idempotent tool, late terminal result retained, bounded limits and retry scope using fake clock/transport. Tests assert observable history/events/effects, not interface forwarding.

**Completion contract:** Outcome: fake provider completes multi-turn conversation; cancelled calls cannot cross admission. Invariants: I1-I4/I8 in platform-free code. Artifacts: loop/contracts and regression tests. Evidence: focused `nativeagent.engine` tests plus IDE build.

**Commit:** `feat: add Pi-modeled native agent loop`.

### Stage 2 — Extract native semantic read execution

**Purpose:** prove direct bounded PSI navigation without instantiating bridge runtime.

**Depends on:** Stage 1.

**Preconditions:** exact read-tool construction/dependency closure recorded; references collected before exported-symbol changes.

**Discovery / implementation method:** extract typed operation helpers from selected navigation/refactoring/file tools. Keep legacy output adapters and shared algorithms; introduce no rendered-text parsing. Read `PlatformApiCompat` and platform APIs via IDE navigation/ij-search before calls. Isolate native tool instances to avoid `argumentsHash` races.

**Expected scope:** seven read tools and shared helper classes; native tools/handle registry/readiness adapter; focused platform tests and development-only action registration.

**Implementation:** native catalog with find_file/search_symbols/get_file_outline/get_symbol_info/find_references/read_file/search_text only. Apply limits and identity contracts above. Introduce structured data before rendering. Construct only selected tools/helpers, never `PsiBridgeService.getInstance()` as the native path. Reuse readiness semantics but fail closed on cancellation/unexpected errors and give actionable hints for the actual catalog. Add an off-by-default `agentbridge.native.dev` sandbox mode, selected at IDE launch; in native mode suppress legacy PSI/custom-MCP/graph/external-agent startup and show a dev entry without invoking old tool window setup. Normal mode remains donor behavior. No runtime switch while processes are active.

**Tests:** two same-name overloads; unrelated same-name class; real reference resolution vs text occurrence; unsaved editor read; ambiguity response; paginated/incomplete results; indexing timeout/cancellation; handle eviction/invalidations; constructing native path does not start bridge services. Development smoke invokes a retained real semantic operation without MCP/HTTP.

**Completion contract:** Outcome: direct identity/outline/reference workflow in native dev mode. Invariants: I1/I5/I11; native startup has no old runtime activation. Artifacts: shared read operation extraction and bounded native schemas. Evidence: focused platform tests, native dev action smoke, IDE build, preserved affected donor tests.

**Commit:** `feat: expose bounded native semantic reads`.

### Stage 3 — Add guarded edits, rename, diagnostics and execution

**Purpose:** complete the deterministic coding toolbox before real model execution.

**Depends on:** Stages 1-2.

**Preconditions:** symbol/read handles are real; selected write/format/process closure known; no reliance on future UI approvals.

**Discovery / implementation method:** extract selected application/formatting/process helpers rather than call the bridge wrapper. Trace every EDT runnable and secondary save/format/refactor stage. Use semantic references for all changed exported methods. Refactor donor callers in the same commit; do not leave deprecated shims or clones. Existing donor behavior is preserved except shared correctness fixes, which receive regressions.

**Expected scope:** ReplaceSymbolBodyTool/EditingTool, EditTextTool/WriteFileTool/FileTool, RefactorTool and helper, selected diagnostics/build/test operations, RunCommandTool/RunPanelExecutor, native context/adapter classes and tests. Do not prune unrelated tool families.

**Implementation:** exact native restrictions and preconditions above; all mutation paths get explicit execution context/admission. Native fields are validated even if donor schema ignores unknown keys. Add run-owned format flushing/diagnostics. Wire build/test/process cancellation and bounded output before exposing them. In development mode use a deliberate dev-action consent control for trusted execution; production tool window enablement comes later. No automatic filesystem effect merely from loading the plugin.

**Tests:** exact overload replacement including two declarations on one line; stale read/symbol rejection; queued Stop before EDT commit leaves file unchanged; admitted operation settles before idle; rename all real references without unrelated matches; rename conflict/usage-change race changes nothing; new-file existence/symlink race; undo of edit/create/rename; format cancellation/no deferred mutation; diagnostic failure is not clean; targeted test failure observed; command flood stays bounded; Stop kills owned root and reports survivors; VFS refreshed after failing shell write; dirty-document external conflict surfaced. Keep plausible regressions, not exhaustive plumbing matrices.

**Completion contract:** Outcome: development scenario read → edit/rename/create → diagnostics → test/build is real, undoable and cancellation-aware. Invariants: I1-I11 for selected tool surface. Artifacts: selected native execution adapter, shared correctness fixes and regressions. Evidence: platform/command tests, actual IDE dev-action smoke, affected donor tests and IDE build. No supported-OS claim beyond the exercised Linux target.

**Commit:** `feat: add guarded native coding tools`.

### Stage 4 — Add Codex subscription authentication and SSE transport

**Purpose:** connect the native loop to the user's actual provider access.

**Depends on:** Stage 1 for contracts; Stage 3 for live tool smoke. Auth/codec source work can run concurrently with Stage 2-3 after Stage 1 contracts freeze.

**Preconditions:** source-derived wire/auth rules and attribution accepted; current provider-supported client/auth policy checked before real credentials. No real key/token supplied to an implementation model or written to evidence.

**Discovery / implementation method:** port the exact Pi paths/branches listed above. Derive scrubbed Java event fixtures from inline builders and shared stream handling; record source hashes in fixture provenance. Do not port optional zstd/WebSocket/remote prompt fetching. Configure only one user-supplied model ID, not a model registry.

**Expected scope:** native provider/codex and auth packages, minimal login/model settings/dev dialog, fixture resources and provider/auth tests; third-party notice for substantive ported code.

**Implementation:** browser PKCE/manual full URL flow, PasswordSafe with refresh/logout versioning, Codex request codec, strict incremental SSE, immutable replay metadata and request-local retry. Apply all stated request/response caps. Provide login status, logout, model field, and actionable protocol/account error. No API-key fallback, device login, or direct Responses serializer.

**Tests:** fake OAuth server exercises state mismatch, duplicate callback parameters, port conflict/manual URL same verifier, expiration, malformed token, cancellation/listener cleanup, concurrent refresh and logout race. Fake HTTP replays text/reasoning/function calls/results, identity preservation, split UTF-8/CRLF/multiline/EOF, terminal statuses, duplicate IDs, malformed arguments, oversize and cancellation. Multi-turn fixture asserts reasoning and call/result pairing in the second request; do not assert model-generated prose. Manual smoke: login, use configured account-supported model, resolve a real symbol through native tool and receive continuation. Keep credentials outside logs/artifacts.

**Completion contract:** Outcome: native loop uses real Codex subscription and direct tools. Invariants: provider completion and secret/lifecycle rules hold. Artifacts: one provider/auth path, fixtures and attribution. Evidence: CI-safe fake transport/auth tests plus separately recorded manual live smoke and IDE build. If account/policy access is unavailable, mark this stage blocked; do not call fixtures a usable MVP.

**Commit:** `feat: add native Codex subscription transport`.

### Stage 5 — Add native session UI and complete vertical workflow

**Purpose:** remove dev actions from the normal native workflow and make it usable inside IntelliJ.

**Depends on:** Stages 1-4.

**Preconditions:** login, tools, diagnostics, Stop and provider continuation work without UI glue.

**Discovery / implementation method:** use supported editor/tool-window components and PlatformApiCompat; inspect donor UI only for reusable small utilities, not NativeChatPanel/NativeMarkdownPane inheritance. Native tool window is instantiated only in native dev mode until cutover.

**Expected scope:** native UI and settings integration, plugin.xml native-mode registration/factory behavior, runIde dev-mode configuration, focused UI/lifecycle tests.

**Implementation:** read-only Editor transcript plus separate EditorTextField, Send/Stop/New Session, status, login/logout/model controls, trusted-session enablement. Session/project-scoped state with one active run; editor/listener ownership content-scoped. Login operations and effects use tracked longer-lived ownership while finishing during disposal, then release; callbacks never resurrect content. Coalesce streamed display at 50 ms without unbounded queued deltas. Preserve user draft/caret while streaming, disable Send during a run, support copy and native source links. Show diagnostics pending, tool outcomes, partial answer and cancellation honestly. No approval buttons, branch browser, Markdown engine, persisted transcript, or steering queue. Allow an explicitly selected project instruction file as a per-session attachment capped at 16 KiB and included in the 64 KiB request cap; no automatic global/directory instruction discovery.

**Tests:** authenticated real coding task, typing during streaming, double Send, Stop while request/read/queued mutation/admitted mutation/command active, New Session handle invalidation, close/reopen content and project with no leaked editor/process or stale UI; token-free error display/logs. Test actual surface through native IDE interaction; JCEF browser tests are not verification of this UI.

**Completion contract:** Outcome: the full scope workflow works through native UI; loss of ephemeral history is clearly shown. Invariants: no hidden bridge/UI dependency; all owned resources finish/cancel on disposal. Artifacts: native tool window and usage documentation. Evidence: focused tests, actual runIde interaction transcript, `:plugin-core:test`, `:plugin-core:buildPlugin`, `:plugin-core:verifyPlugin`, IDE build. Record verifier/environment failures rather than suppress them.

**Commit:** `feat: add native semantic agent tool window`.

### Stage 6 — Qualify semantic value and decide cutover

**Purpose:** evaluate the actual hypothesis before expanding features or deleting the donor.

**Depends on:** Stages 0-5.

**Preconditions:** live account access and UI safety gate passed; user supplies/selects real repository tasks, not an implementation model fabricating completed work.

**Discovery / implementation method:** use deterministic Java fixture tasks plus three real tasks described by scope. Compare equivalent clean task bases against Pi + idea-facade. Same model/account/reasoning settings where possible; record differences and provider variability. Use disposable fixture projects under project-local work directories, not destructive resets of the user's work.

**Expected scope:** dogfood evidence and bounded fixes for discovered in-scope defects; no persistence/provider/extension additions.

**Implementation:** record completion, target correctness, semantic vs textual operation reasons, human interventions, token/byte counts when available, diagnostics/test feedback, undo and Stop results. No benchmark claim from a fake provider. Produce Go/Adjust/Stop disposition. Prepare measured deletion plan only after Go, recomputing registration/import/build closure instead of copying historical counts.

**Tests:** execute the scope scenarios; retain a regression only for a plausible discovered failure. No added test merely for a measurement table.

**Completion contract:** Outcome: explicit evidence-backed product decision. Invariants: correctness gates unchanged; no feature expansion used to bypass them. Artifacts: dogfood report and, on Go, separate cutover/deletion plan. Evidence: actual native/comparator task runs, final module tests/build/package, observed UI/process state. Native MVP does not yet claim a stripped distribution.

**Commit:** evidence-only unless a real scoped defect is fixed; such fixes use separate `fix:` commits after build passes.

## Acceptance traceability and risk proof

| Row | Required proof | Stage |
|---|---|---|
| R1 / identity | Overloads/same-name symbols, exact ranges, references not text matches, bounded result completeness | 2-3, 6 |
| R2 / Pi loop | Sequential multi-tool continuation, tool errors, no LENGTH execution, provider conversion isolated | 1, 4 |
| R3 / subscription | PKCE login, refresh/logout race, same-account live model/tool continuation | 4-5 |
| R4 / real task | Inspect/edit/create/rename/diagnostics/test/build/summary inside native UI | 3, 5-6 |
| R5 / subtraction | Native-mode startup isolation now; measured deletion plan after Go, not claimed complete at MVP | 2, 6 |
| R6 / handoff | Frozen contracts, owner boundaries, verification and review status available to other model | This plan + handoff |
| Stop/EDT | Cancellation wins before write admission; after-admission effect accounted before idle | 1, 3, 5 |
| Stale/wrong write | Version/path/identity changes fail; same-line siblings untouched; rename new-usage race rejected | 3 |
| Undo | Edit/create/rename undo restores expected files/content | 3, 5 |
| Deferred mutation | Native formatting/save/diagnostic operations settle/cancel with run; no ACP queue | 3 |
| Process/VFS | Flood bounds, Stop/timeout cleanup, failed command file changes refreshed, dirty-editor conflict | 3, 5 |
| Stream/history | Byte splits, absent terminal, malformed/duplicate call IDs, reasoning replay and paired outputs | 4 |
| Auth/security | Callback state/verifier, PasswordSafe, sanitized errors, logout vs refresh, no hidden env injection | 3-4 |
| Limits | Whole request/frame/output/loop bounds; no silent partial exchanges or exact-token claims | 1, 4 |
| Startup/disposal | Native mode starts no legacy services; normal donor mode still works; closed UI stays closed | 2, 5 |
| Packaging | Donor packaging preserved during MVP; native-only archive explicitly deferred to cutover milestone | 0, 5-6 |

## Author audit and model handoff boundary

The author traced each stage under prior-stage-only assumptions. Stage 3 dev consent does not depend on Stage 5 UI. Stage 4 includes its own minimal credential/model UI, so its live smoke does not depend on Stage 5. Stage 2 isolates native startup before Stage 5 interaction. Stage 1 fakes are test-only; they do not constitute provider or tool implementations. Stage 6 is explicitly a manual gate.

Remaining source discovery inside stages is bounded to platform API identity and measured dependency extraction; it is not permission to replace semantics with approximate text edits. If a required API cannot implement the specified contract, stop that slice and report evidence for a plan correction.

User chose current primary model and no peer. This is an author executability audit, **not independent plan approval**. Independent review has not run. The implementation handoff must report this state, never reuse the old review's approval markers. A different implementation model should perform a fresh-context preflight review before source changes; no second peer is requested.

## Final verification

Planning verification: source trace and content hashes; scope/plan requirement consistency; every stage has purpose/dependency/precondition/method/scope/implementation/tests/outcome/evidence/commit; no source/runtime verification claimed.

Implementation verification, run once after concurrent edits settle and at semantic commit boundaries as required:

1. IDE test runner for focused native engine/provider/tool/lifecycle tests; commands equivalent to `./gradlew :plugin-core:test --tests 'com.github.catatafishen.agentbridge.nativeagent.*'` where supported by the existing test task. Use actual discovered test class patterns, not a fabricated passing command.
2. `./gradlew :plugin-core:test` plus affected donor tests (included in that task unless source-set inspection proves otherwise). Platform/integration-tagged tests must use their actual configured task; the existing test task excludes integration tags, so unit success alone is not platform proof.
3. IDE `build_project` / facade test `op=build` and confirm success before any code-change turn/commit completes.
4. `./gradlew :plugin-core:buildPlugin :plugin-core:verifyPlugin` and root `./gradlew build` at final gate. Record baseline environmental failures; never blanket-suppress checks.
5. Launch actual `runIde` in native dev mode via supervised process/run configuration. Complete login, a real coding task, Stop races, undo, close/reopen. Check normal donor mode once for preservation. Do not use browser/JCEF automation as native Swing proof.
6. Inspect native startup/process/listener behavior and packaged content. Old artifact presence during MVP is expected; old runtime activation in native mode is not. The short-lived OAuth callback is intentional and must be gone after login.
7. Format/optimize imports once after edits settle; use changed-file IDE inspections. Apply only supported API replacements and source fixes, not suppression of safety errors.
8. Inspect complete changes through IDE git diff/status and whitespace facilities, not shell git. Scope should be native packages, selected extracted tool/runtime helpers, registration/dev-mode settings, tests/fixtures, attribution and usage docs; no wholesale ACP/MCP/JCEF deletion in these stages.
9. Remove throwaway scripts/processes after their evidence is recorded. Keep only regression tests that protect observable contracts. All work artifacts remain under `.agent-work/`.

## Out of scope

Everything listed out of MVP in the scope, plus general multi-provider canonical history, external credential-file import, automatic model discovery, safe-delete/move/change-signature, unsupported-language semantic claims, sandbox guarantees, guaranteed process-tree containment, automatic git writes, and redesign of all donor tool families. No new framework/build-plugin stack solely for the MVP.

## Workflow contract

Plan status: COMPLETE_FOR_REVIEW  
Issue: native-agent-mvp — Pi-modeled semantic IntelliJ agent  
Base: observed master; inspected source hashes in source-manifest.json; exact base commit recorded by Stage 0 before code  
Implementation branch: native-agent-mvp (must be created; not created during planning)  
Evidence manifest: COMPLETE for the cited planning basis; runtime baseline NOT RUN  
Stages: 7 (0 through 6)  
Invariants: COMPLETE  
Risk/test matrix: COMPLETE  
Final verification: COMPLETE specification, NOT RUN against an implementation  
Independent review: NOT RUN; user chose no peer  
Next action: fresh-context implementation-model preflight using handoff.md; do not claim historical review approval
