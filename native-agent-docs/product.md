# IntelliJ-native coding agent — product

This file is the single product authority: hypothesis, decisions, invariants, tool and provider contracts, roadmap, dogfood gate and exclusions. The process that turns this into code is in `workflow.md`.

## Status

- Independent review of this product scope: **not run**. The original authors performed only an author audit. Do not claim approval.
- Implementation: two slices exist, lifecycle/admission (`plugin-core/.../nativeagent/lifecycle/`) and domain and run driver (`plugin-core/.../nativeagent/run/`). Their state is in `workflow.md` Feature slices.
- Donor removal: done intentionally, before dogfood. Branch `native-agent-workflow` removed the donor ACP/JCEF/multi-agent runtime and the PSI tool packages (commit `8791625a9` and later cleanup commits). See DONOR_SUBTRACTION. Donor tool code exists only on `master`; the donor evidence table at the end of this file refers to `master`.

## Hypothesis

Build a Pi-modeled agent whose default working surface is IntelliJ's semantic model, not a shell over repository text.

The model chooses intent and interprets evidence. IntelliJ resolves symbol identity, enumerates references, applies supported refactorings, manages editor documents and undo, and reports diagnostics. Do not send the entire AST to the model or build another index beside IntelliJ.

**Premise accepted as a product hypothesis, not a demonstrated performance result.** PSI provides structure, resolution and scoped operations. It does not make every search small or every edit correct. Indexed text search can also be bounded. Pi + idea-facade already exposes semantic tools. Native placement is valuable only if this product makes semantic operations reliable defaults and removes integration overhead without losing task completion.

MVP question: can a user authenticate with a ChatGPT subscription, give the agent a Java repository task inside IntelliJ, and have it resolve the right symbols, make bounded changes, inspect diagnostics, run targeted tests and finish with verifiable results? A native chat panel that still relies mostly on shell search and text replacement does not establish the hypothesis.

Everything in the MVP must contribute to answering that question. A feature does not enter the MVP because it would be useful in a finished agent. The dogfood result is **Go**, **Adjust** or **Stop**. Stop is a valid successful outcome of the experiment.

## Requirements

| ID | Requirement |
|---|---|
| R1 | Restrict the model's source-code problem space through AST/PSI and deterministic IntelliJ operations. Testable hypothesis, not evidence that native placement is better. |
| R2 | Model agent behavior after the local Pi source (`/home/muku/depot/personal/cli-tools/pi`), not an invented orchestration framework. |
| R3 | Use ChatGPT subscription access, not an assumed API key. |
| R4 | Usable native coding workflow with safe mutations, diagnostics and targeted tests. |
| R5 | Remove irrelevant AgentBridge runtime. Done early by owner decision; see DONOR_SUBTRACTION. |
| R6 | Hand implementation to another model with a bounded executable specification (`workflow.md`). |

## Decisions

This table is the one canonical decision registry until `workflow.md` moves decisions into `decisions.json`. Other documents reference a key. They do not restate or override it.

| Key | Value |
|---|---|
| CORE_LANGUAGE | Java records, sealed interfaces, exhaustive switches and named outcomes for IDs, messages, typed arguments, outcomes, transition policy, PSI helpers and Swing UI. Kotlin only where suspend calls and structured lifetime simplify execution: run driver, service/content scope wiring, cancellable waits, platform dispatch. Supersedes the earlier "Kotlin preferred for all new code" feedback. |
| BYTECODE_BASELINE | Java/JVM 21. Retain `sinceBuild=253` and IntelliJ 2025.3–2026.1 runtime compatibility. No preview features. A 2026.2+/Java 25-only decision reopens compatibility. |
| MODULE_LAYOUT | `plugin-core/.../nativeagent`. No new core module, general framework or mass package rename. |
| QUALIFICATION_TARGET | Local IntelliJ with Java support on Linux. Other languages get labeled file/text fallbacks and no semantic-parity claim. Remote Development and cross-OS qualification are later. |
| SESSION_OWNER | At most one native session per project. Its lifetime is the native tool-window content. Content close ends the session, invalidates all handles, requests cancellation and suppresses UI delivery. Already-started effects still need accounting. History is ephemeral and its loss is shown visibly. |
| RUN_OWNER | One active run per session. New Session and model change only at idle. |
| MODEL | One user-configured model per session. No default availability assumption, no automatic fallback. Model change starts a new session. |
| PROVIDER | ChatGPT Codex subscription only. Browser PKCE OAuth is MVP. No API-key path, no device flow. |
| PROVIDER_SEQUENCING | Provider implementation may proceed after shared contracts freeze. Live integration cannot qualify before real native tools exist. |
| TOOL_EXECUTION | Sequential in source order (an intentional restriction; Pi also supports parallel). Validate complete wire structure and all call schemas before any effect. Mutable-world preconditions stay per-operation checks. |
| TRUNCATION | A length-truncated response never executes calls. Record `TRUNCATED_NOT_EXECUTED` results and end the run at idle. Pi also never executes them but continues the loop. |
| TRUST | Explicit trusted-session opt-in per session, then no per-call approval. Not a sandbox. |
| COMMAND_ESCAPE_HATCH | `run_command` excluded from the semantic MVP. Re-entry requires an observed dogfood task that native operations cannot complete. No unused command API. |
| REFLECTION | `nativeagent/**` and its third-party runtime dependencies require no runtime reflection: no reflective serialization, classpath scanning, dynamic proxies, reflective DI, annotation discovery, `Class.forName` discovery or Kotlin reflection. Compile-time generation is allowed. IntelliJ service instantiation is platform behavior at the boundary. |
| JSON | JSON types exist only in wire/tool codecs. No new Gson in `nativeagent/**`. Prefer Avaje JSONB generated codecs for fixed wire DTOs. Evaluate Helidon JSON core only for genuinely dynamic JSON. Keep one JSON dependency if possible. |
| HTTP | JDK `HttpClient` plus a small strict SSE decoder. Honor IDE proxy settings in the adapter. No OkHttp/Ktor/Apache HC without evidence. |
| ASYNC | IntelliJ-owned coroutine scopes. Content close cancels its child scope only. No `GlobalScope`, no `runBlocking` on EDT, no swallowed cancellation, no second coroutines runtime. Scope cancellation does not suppress an already queued EDT runnable, so the admission boundary stays mandatory. Java does not call raw suspend methods. |
| OUTCOMES | Operation-local sealed outcomes (`SymbolLookupOutcome`, `RenameOutcome`, `ProviderOutcome`, …). No generic `Either`/`Try`/`Result`, no giant `AgentError`. The `status/code/content` envelope exists only at the LLM boundary. Cancellation is control flow. Violated invariants throw. |
| NULLNESS | No JSpecify/NullAway. Existing JetBrains annotations plus constructor/boundary validation. No whole-program null-safety claim. |
| OBSERVABILITY | IntelliJ `Logger` for bounded technical diagnostics plus a tiny run-owned `RunStats`. No OpenTelemetry, exporters, metrics registry, event bus or custom JFR events. |
| FORMAL_TOOL_DEFAULT | Quint for interactions between independent owners (layered models, `workflow.md` Formal tools); none for local value logic. |
| PROMPT_CACHE | Within one cache generation, each provider request is a byte-exact prefix extension of the previous request. Cache resets are explicit typed boundaries. See Prompt-cache stability. |
| DONOR_SUBTRACTION | Done before dogfood, by owner decision. The donor runtime and PSI tool packages are removed on `native-agent-workflow`. Native tools are written fresh from the contracts in this file; donor code on `master` is reference evidence only, never an extraction source or fallback path. Dogfood still compares against Pi + idea-facade. Remove any remaining donor-only dependency or packaging as soon as nothing native reaches it. |

## Pi fidelity

Use the small loop in `packages/agent/src/agent-loop.ts`, the coding-agent's tool-aware prompt construction and the Codex OAuth/Responses implementation. Do not port the monorepo or its newer distributed/durable `harness/` layers. Never implement from an older review's description of Pi without checking current source.

Preserve:

- user message → streamed assistant response → validated tool calls → tool results → continuation;
- distinct user, assistant and tool-result messages;
- run, message and tool start/update/end events, each tagged with session/run identity;
- explicit tool errors that the model can act on;
- tool-aware instructions containing only available capabilities;
- provider-specific history conversion and Codex reasoning-item continuity.

| Behavior | Pi source | Native decision |
|---|---|---|
| Loop | `agent-loop.ts:156-273` streams, executes tools, appends results, supports steering/follow-up queues | Port the loop. Steering/follow-up deferred: the user may draft while a run is active, Send stays disabled until idle. No cancel-and-resend masquerading as steering. |
| Truncation | `agent-loop.ts:227-235,379-404` gives every call a synthetic error result and continues | See TRUNCATION. |
| Sequential execution | `agent-loop.ts:442-479` runs calls in order, breaks after an aborted call | See TOOL_EXECUTION. Whole-response prevalidation and complete Stop accounting are stronger native guarantees, not Pi guarantees. |
| Abort signal | `agent-loop.ts:677-718` passes `signal` into `tool.execute` and awaits the tool | Stop is delivered through the admission boundary. The run driver owns a cooperative cancellation token, passes it to tools and cancels it together with `RunLifecycle.stop`; the lifecycle `Effect` itself takes no signal. |
| Tool errors | `agent-loop.ts:708-714` converts thrown errors into error results | Unknown tool or invalid schema becomes an error result. Malformed JSON or duplicate wire IDs invalidate the whole response before any call executes. |
| Messages | `agent-loop.ts:279-370,784-803` separates provider conversion; tool results have their own role and call identity | `UserMessage`, `AssistantMessage`, `ToolResultMessage`. No SQLite schema, no cross-provider abstraction. |
| Provisional output | Pi updates partial messages in place | Native display state is separate from accepted history. |
| Prompt | `coding-agent/src/core/system-prompt.ts:80-135,150-165` builds from tool snippets and project context | Native tool-aware prompt. Do not copy Pi's shell guidance or doc paths. Automatic project-context discovery deferred; an explicitly selected instruction file may be attached per session (max 16 KiB, inside the request cap). |
| Codex | `ai/src/api/openai-codex-responses.ts`, `openai-responses-shared.ts` | Same-model reasoning/message/call metadata retained. SSE only, uncompressed JSON. No WebSocket cache, grammar tools or deferred tool search. |
| Auth | `ai/src/auth/oauth/openai-codex.ts` browser PKCE | Browser flow with manual full callback URL (state mandatory, raw code rejected). Five-minute deadline. Device flow deferred. |
| Tool philosophy | Small explicit tools | IntelliJ replaces shell source navigation. No generic facade or extension framework. |
| Trust | Pi runs fully trusted | Explicit trusted-session opt-in. |
| License | `pi/LICENSE:3-13` MIT | Preserve attribution for ported code and fixtures. The code license does not authorize use of an OAuth client or service. |

Other intentional deviations: Swing/editor tool window replaces the terminal UI. No durable sessions, compaction, extension runtime, multi-provider conversion or subagents. Native cancellation accounts for queued EDT writes and already-started noninterruptible IDE operations.

## Trust

Before the first run of each session, the user enables the agent after seeing that source goes to the configured provider and that tools can edit files and run builds/tests with the IDE user's privileges. Consent is not persisted. After enablement, catalog tools run without per-call dialogs.

This is not a sandbox. Build scripts and tests can reach the network and files outside the project through normal OS privileges. IDE-native mutations are limited to writable project content; dependency sources are read-only. Never describe that path restriction as containment. Preserve the user's shell/toolchain environment; do not inject PasswordSafe secrets. Untrusted repositories are outside the dogfood contract.

Conflict detection, cancellation, schema validation, output bounds, undo and accurate effect reporting are correctness requirements even in trusted mode.

## Architecture rules

### Unsound shell, typesafe core

External reality is unsound and mutable: LLM JSON, HTTP/SSE, OAuth, filesystem, processes, PSI/VFS/Document APIs, nullable platform APIs, mutable JSON trees. The core holds only values that passed the boundary checks required to construct them. Side effects leave the core through adapters.

```text
provider / JSON / PSI / platform callbacks
             ↓ parse, resolve, validate
Java domain values and transitions
             ↓ explicit adapter effects
IntelliJ platform side effects
```

- **Construction is proof.** A core object proves its construction preconditions. Deep mutation code does not rediscover whether a request was valid. It re-establishes only assumptions the external world can invalidate.
- **Progressive evidence types.** Each arrow can produce a named failure outcome:

```text
RawSymbolCandidate → ResolvedSymbol → InspectedSymbol → PreparedMutation → AdmittedMutation → MutationOutcome
SSE bytes → WireEvent → ParsedProviderResponse → ValidatedAssistantResponse → ExecutableToolBatch
wire JSON → ValidatedToolCall<Args> → typed operation → Outcome → rendered LLM JSON
```

- The loop never receives partial SSE frames, malformed call IDs, mutable JSON trees, nullable wire fields or argument fragments. No core code branches on `status == "error"`, `code == "STALE_HANDLE"` or `args["symbol_id"]`.
- **IntelliJ is an unsound boundary.** `PsiElement`, `VirtualFile`, `Document`, raw smart pointers, read/write-action primitives and dumb-mode state stay in adapters. The adapter re-resolves and revalidates when it goes back into IntelliJ for a side effect.
- **Capabilities, not a context bag.** Prefer small capability values (`RunContext`, `ReadCapability`, `MutationAdmission`, `ProcessOwnership`, `Deadline`) over one growing mutable `NativeExecutionContext`. A mutation API receives the minimum capability it needs. Java references are not affine: single-use admission needs runtime synchronization, not a "consumed" type name.
- **Records are not proofs.** Java records are not deep immutability or non-null proofs. Validate components and snapshot at ownership boundaries. Proof-bearing values that need restricted construction are final classes with controlled factories, or package-private records.
- **Expected failures vs bugs.** Expected domain outcomes (ambiguity, stale identity, document changed, rename conflict, index unavailable, diagnostics pending, rate limit, authentication required, context full) are sealed outcomes. Cancellation is structured control flow, translated to transcript results only where Stop requires it. Violated invariants (result without CallId, impossible transition, mismatched call/result identity) throw; never turn them into a recoverable `InternalError`.

### Invariants

I1. At most one active native run per project. Loop/domain/provider own no IntelliJ types. No Node/ACP/MCP/HTTP hop on the native execution path.

I2. The session owns accepted in-memory messages and one `RunId`. A run captures model ID, tool catalog generation, instructions and limits. No model or catalog change mid-run. New Session or model change happens only at idle and invalidates every old handle.

I3. Provider request attempts do not execute tools. Only a terminal validated response creates a batch. Each invocation has one local CallId and one terminal outcome. The harness never retries an executed call. Retrying the next provider request with prior results does not replay effects.

I4. Each accepted tool call gets exactly one result before the next provider request. Stop records `CANCELLED_NOT_STARTED` for remaining calls. An already-started effect reports its real terminal outcome. A partial provider response never enters accepted context.

I5. Symbol handles come from IntelliJ resolution, not from the model. Ambiguity, stale document state, unsupported language, unavailable indices and incomplete search are explicit outcomes, never permission to choose a first/nearest match or return an authoritative empty result.

I6. Mutation approval and mutation correctness are separate. Native IDE mutations are restricted to writable project content. File identity, document version, symbol identity, writability and cancellation are rechecked at the final mutation boundary for every touched target. Unsaved editor state is authoritative. No raw offsets survive an intervening document change.

I7. No EDT wait while holding a read action. Do not wrap refactoring processors in a broad read/write action. Discovery is cancellable and bounded. The write command is short, named and undoable. Check `PlatformApiCompat` and supported APIs first.

I8. Stop wins against not-yet-started effects through one synchronized admission boundary shared by cancellation and effect start, checked inside the actual queued runnable, not before scheduling. Once an effect begins, Stop does not roll it back; the run stays `STOPPING` until it and owned cleanup settle, and no new run can start. No unconditional "Stop always finishes" claim: safety holds without progress assumptions, liveness assumes the operation eventually returns.

I9. Run-owned formatting, saves, diagnostic waits, request bodies, HTTP requests and callbacks are cancellable or tracked to completion. No background queue mutates after the run reports stopped. Cleanup does not block EDT.

I10. Prompts, tool arguments, source payloads, OAuth URLs/codes/verifiers, credentials and full provider responses never enter logs. Sanitize exceptions before logging. PasswordSafe is the only credential store. No hidden argument (for example donor `_env.*`) is accepted.

I11. Editors, listeners, run resources, callbacks and processes have explicit content/project owners. No blocking disposal on EDT. Closed content never resurrects from a late callback.

I12. Tool arguments/results and provider requests are bounded. A conservative context budget is enforced before dispatch. Context-full ends with an actionable New Session instruction; no compaction.

I13. Within one `CacheGeneration`, the complete cache-relevant serialized request N is an exact prefix of request N+1. Instructions, tool definitions, tool order and serialization never change inside a generation. Mutable IDE or repository state never enters that prefix. A violation outside an explicit reset boundary is a bug and throws before dispatch.

## Tool catalog

Fourteen flat tool IDs for the semantic MVP, plus the deferred `run_command`. The catalog size is a ceiling, not a target. Do not recreate operations behind a generic dispatcher visible to the model. Native schemas narrow donor schemas and never advertise unsupported options. Results carry usable identity and pagination/completeness metadata. Do not parse human-readable donor output to invent typed fields.

| Tools | Contract summary |
|---|---|
| `find_file` | Bounded filename/path discovery, build/output trees excluded. |
| `search_symbols`, `get_file_outline` | Bounded declaration candidates and file structure; no raw AST dump. |
| `get_symbol_info`, `find_references` | Resolve a concrete symbol with signature, owner, location, completeness/scope. Never choose the first overload silently. |
| `read_file` | Explicit path and bounded range; unsaved editor contents win. |
| `search_text` | Scoped literals/configuration/unsupported-language fallback; not symbol identity. |
| `replace_symbol_body` | Replace an inspected Java method declaration, signature preserved, exact PSI range. |
| `edit_text` | Explicit path, current read evidence, exactly one case-sensitive match. |
| `write_file` | Create a new text file only. |
| `refactor` | Semantic rename only. Safe-delete, move, change-signature, inline, extract deferred. |
| `get_problems` | Focused diagnostics with file version and pending/complete status. |
| `build_project`, `run_tests` | Native build and targeted tests with real terminal outcome. |
| `run_command` | Deferred (COMMAND_ESCAPE_HATCH). Contract kept below for re-entry. |

### Reads and identity

- `find_file`: explicit name/path query; source/project scope by default; generated/excluded directories omitted. At most 50 paths per page, hard cap 100.
- `search_symbols`: named query and scope; project default, libraries need explicit scope. Returns session `symbol_id`, language, kind, name, owner/qualified signature when available, file and exact start/end range. Limit 50, cap 100. Wildcard listing requires a path/type restriction.
- `get_file_outline`: one explicit file; declarations only, at most 100 entries/page, each with `symbol_id`. Unsupported PSI reports `UNSUPPORTED_LANGUAGE`, not an empty success.
- `get_symbol_info`: accepts `symbol_id`; returns identity, exact declaration range, bounded source and a `read_id`. Unknown/evicted handles return `STALE_HANDLE` with rediscovery instructions.
- `find_references`: accepts `symbol_id` and scope; returns resolved references and bounded locations, and reports partial search/index readiness separately. A textual occurrence is not a reference.
- `read_file`: path plus start/end lines, default 100, maximum 300 lines, UTF-8 cap 24 KiB. Uses open Document contents; returns `read_id` and actual ranges. Large/binary files get a smaller read or an explicit unsupported error.
- `search_text`: explicit file/directory scope and literal query; regex deferred. Cap 50 matches/page, 100 maximum, bounded excerpts, generated/excluded paths omitted.
- Handles: bounded session-local LRU (initial bound 2,048; tune from dogfood). Eviction is safe because mutations fail on an evicted handle. File edits invalidate handles for affected files. A symbol handle maps to a smart pointer plus language/owner/signature and document identity/version. A read handle maps to file identity/version and inspected ranges. Validate pointers under read access. Prefer smart pointer plus document version; hash content only where filesystem or process boundaries make it useful. No serialization of PSI pointers to the model. Dispose on New Session and content close.
- Readiness: fail closed on cancellation and unexpected errors; give actionable hints naming only catalog tools.

### Mutations

- `replace_symbol_body`: `symbol_id`, `read_id`, `new_body` holding the complete Java method declaration. The range must have been returned in full by an inspection. Reject a changed name, parameter types/arity, return type, modifiers or type parameters. Parse with the language PSI factory; replace the exact element/range, not lines. Other declaration kinds return `UNSUPPORTED_OPERATION`.
- `edit_text`: path, `read_id`, nonempty `old_str`, `new_str`; exactly one case-sensitive match wholly inside an inspected range. No active-editor alias, replace-all, regex or full-file route. For imports, config and localized changes, not cross-file rename. Revalidate document version and matched range at commit.
- `write_file`: new UTF-8 text file only, max 128 KiB. Validate normalized real parent and target nonexistence under the final admission/write boundary. Existing target, symlink escape, read-only parent or creation race is a conflict. Create through VFS with undo. Complete new Java test classes are allowed.
- `refactor`: `operation=rename`, `symbol_id`, `read_id`, `new_name`. Discover usages outside the write action; cap 100 affected files and return `TOO_BROAD` above it. Revalidate every usage file before commit; on any project PSI change during preparation, rediscover or fail `STALE_TARGET`. Run IntelliJ conflict checks. No textual fallback, automatic overwrite or modal prompt. Keep all changes under one named undoable command. A symbol with zero references is a valid rename.
- All mutation results report touched files and terminal disposition `NOT_STARTED`, `COMPLETED` or `FAILED_AFTER_START`. Never claim rollback of a partially failed platform operation; surface changed files and require inspection before continuing.
- Refuse native mutation of files over 2 MiB. Never wait on external file/network work while holding a write action.

### Formatting and verification

- Formatting and imports are run-owned. Flush once at a successful batch boundary before the next model request, and before build/test if earlier tools in the batch edited files. Never optimize imports between an import-only edit and the edit that uses it. Formatting goes through the same admission mechanism and is awaited. Stop cancels unstarted formatting.
- Diagnostics: after the formatting flush, commit documents and collect focused diagnostics tagged with the observed file version. Attach batch-final diagnostics to the final mutation outcome. If unavailable after 5 seconds, return `pending`, never zero problems. Cap 50 problems/file, 200 overall. No whole-project inspection per edit.
- `build_project`/`run_tests`: explicit test/class/file scope; never infer all tests from an empty selector. Save documents only after rechecking admission. Register the execution handle before start and await real completion. Build already running returns `BUSY`. Stop requests supported cancellation and waits for the terminal outcome; no blanket thread interruption.

### Deferred: `run_command` contract

Kept for re-entry only. Complexity it adds: process ownership, bounded buffering, timeout, descendant termination, surviving-PID accounting, VFS refresh, disk/editor conflicts, disposal and Stop semantics for external processes.

- Finite noninteractive command with short title and project working directory. Default 60 s, max 180 s. No detached jobs or input. Reject all non-schema keys including `_env.*`. Preserve native test routing.
- Capture into a thread-safe 256 KiB byte ring; return at most 24 KiB plus truncation/exit metadata. No rerun-to-paginate.
- On Stop/timeout, terminate root and descendants best-effort, await/rescan up to 5 s and report survivors. If tracked processes survive, stay `STOPPING` with `TERMINATION_INCOMPLETE` and PIDs; keep Send disabled until they exit. Never claim guaranteed containment.
- Await VFS refresh after any started command. Invalidate handles project-wide if touched paths are unknown. If an editor and the command both changed a file, surface a conflict and never save/reload over either version.

## Codex provider and credentials

1. Endpoint `https://chatgpt.com/backend-api/codex/responses`. No custom endpoints or general HTTP tool. Tests inject a transport endpoint without shipping a configurable bypass.
2. Browser OAuth: S256 PKCE, fresh verifier/challenge, cryptographic state; loopback port 1455, path `/auth/callback`, one completion, five-minute deadline. System browser via platform API. If bind or redirect fails, accept a pasted full callback URL tied to the same attempt. Require exact scheme/host/port/path and matching state; reject duplicate parameters and raw code-only input. Cancel and logout close the listener and clear code/verifier. The callback is not a PSI/MCP server and closes after login, cancellation or timeout.
3. Pi's observed public client ID is `app_EMoamEEZ73f0CkXaXp7hrann`, scopes `openid profile email offline_access`, flags `id_token_add_organizations=true`, `codex_cli_simplified_flow=true`. This source fact is not permission to reuse that client. Verify current provider-supported client policy before real credentials. If unavailable, mark the live-auth gate blocked; never impersonate a client or switch to an API key.
4. Exchange and refresh fields follow Pi `openai-codex.ts:149-189`. Validate nonempty tokens and bounded positive `expires_in`. Read account ID from claim `https://api.openai.com/auth.chatgpt_account_id` as metadata, not as JWT validity proof. Missing account ID blocks requests. Store account, tokens and expiry in one PasswordSafe entry. Settings hold only the model ID and UI preferences. Never read external Pi/Codex credential files.
5. One application-level credential owner with single-flight refresh across projects. Refresh 60 s before expiry; one forced refresh/retry on 401, then require login. Version credential updates so a late refresh cannot overwrite logout or new login. Run cancellation unsubscribes its wait; logout and disposal cancel the owned operation.
6. Model ID is required user configuration. Reasoning effort `medium` only after the model accepts it; otherwise a configuration error, not a hidden rewrite.
7. Request: `model`, `store=false`, `stream=true`, `instructions` (native prompt), `input` (complete accepted history), `include=["reasoning.encrypted_content"]`, session-stable `prompt_cache_key`, `text.verbosity=low`, `tool_choice=auto`, flat function tools, Pi's `parallel_tool_calls=true` wire field (execution stays sequential). Omit temperature/max-output, system input item, grammar tools, WebSocket state, hosted tools, deferred-tool metadata.
8. Headers: Bearer token, `chatgpt-account-id`, `OpenAI-Beta: responses=experimental`, `accept: text/event-stream`, `content-type: application/json`. Truthful native originator/User-Agent, not Pi's; verify in live smoke. Never send `x-api-key`.
9. `HttpClient.sendAsync` with `BodyHandlers.ofInputStream`, consumed off EDT by a strict incremental UTF-8/SSE reader. Retain request future and opened body atomically; a resource registered after cancellation closes at once. Closing the body must wake reads; interruption is not the cancellation mechanism. Network callbacks never touch PSI or wait on EDT.
10. SSE: LF/CRLF, comments, multiline data, UTF-8 split across chunks at any byte, final frame without blank line. `[DONE]` does not replace a terminal response. Assemble items by provider index/ID and validate identity. Support text, reasoning summary/encrypted reasoning and function-call argument events. Normalize `response.done/completed/incomplete` as Pi does. Unknown optional events may be ignored; unknown required item types reject the response.
11. Accepted replay items (encrypted reasoning and IDs, assistant text IDs/phase, function-call item ID/call_id/name/arguments) stay as immutable provider metadata on the assistant message. Tool results map to `function_call_output` with the original `call_id`. No IDs built from text parsing, no dropped reasoning item.
12. Limits (application limits, not model context claims): 1 MiB per SSE event, 4 MiB decoded stream per request, 128 KiB per argument object, 32 calls per response, 64 KiB request JSON, 1 MiB display transcript with oldest-display eviction. Run limits: 20 model responses, 100 tool invocations, 15-minute deadline; exceeding one stops before new effects and fills skipped results. Above the request cap, stop with `CONTEXT_FULL`. Provider context-length errors are not retried. Log counts, not bodies.
13. Retry at most once per provider request, only before acceptance: transient transport/EOF, 429 excluding quota/billing, or 5xx. Delay 1 s or use a valid `Retry-After` from 0 through 10 s. A missing or malformed value uses 1 s. A negative or above-10-second value is not silently clamped or automatically waited; return the rate-limit outcome with the requested wait when available. Roll back provisional UI before retry. Auth refresh retry shares the two-attempt ceiling. No retry for malformed content, unsupported model, bad schema, billing/quota, context limit, Stop or executed calls.

## Run behavior

Run states: `IDLE → REQUESTING → EXECUTING_TOOLS → REQUESTING … → IDLE`. Any active state may enter `STOPPING`; it reaches `IDLE` only after admitted effects settle, or `DISPOSED` when content ends. Errors return to idle with a categorized visible result. `DISPOSED` never starts work. Authentication is a separate UI operation, not a run.

Draft text is not part of a request until Send. Double Send is rejected before a second user message is appended. Stop during provider output discards the provisional response. Stop during tools keeps the accepted assistant message and exactly one terminal result per call.

| Input / outcome | Accepted context | Effects | Next |
|---|---|---|---|
| Completed text, no calls | Append assistant | None | Idle |
| Completed valid calls | Append assistant and one result per call | Sequential after per-call validation | Next request if active and within limits |
| Completed unknown tool / schema error | Assistant plus that call's error result | None for that call; valid calls follow in order | Model corrects next request |
| Duplicate call IDs, malformed JSON, unsupported required output | No partial assistant | None | Visible protocol error; idle |
| LENGTH, text only | Append marked incomplete text | None | Idle |
| LENGTH with syntactically valid calls | Assistant plus `TRUNCATED_NOT_EXECUTED` results | None | Idle; user may ask to continue |
| LENGTH with malformed fragments | No partial assistant; provisional text shown as failed | None | Idle |
| EOF before terminal / transient failure | No partial assistant | None | One request-local retry, then error |
| Stop during provider request | No partial assistant | None | Cancel/close, idle |
| Stop before effect admission | `CANCELLED_NOT_STARTED` | None for that effect | Fill remaining results; idle after cleanup |
| Stop after effect admission | Real terminal outcome; others cancelled | Started effect may finish; no next effect | `STOPPING` until settled |
| Tool failure after side effect | `FAILED_AFTER_START` and touched paths | No replay | No automatic continuation; user inspects |
| Oversize / context cap | Nothing silently truncated | No new effects | Limit error / New Session |

## Prompt-cache stability

Goal: high prompt-cache reuse follows from the architecture, not from provider tuning. The cache-hit percentage is a metric. The invariant is I13.

### Rules

1. **Append-only accepted context.** Turns and tool rounds only append items. Accepted items are never rewritten, reordered, normalized again, summarized, merged, deleted or regenerated from mutable state. Render each accepted item to its provider wire form once, at acceptance, and keep those bytes immutable. Replay reuses the stored bytes and does not serialize domain values again.
2. **Stable request head.** Instructions, the attached instruction file, tool definitions, tool order, `prompt_cache_key` and every request field under native control are fixed for the generation. Codecs are generated and deterministic: fixed field order, no map iteration order, no locale or clock input. Do not interpolate dynamic data into the head: time, git status, active editor, diagnostics, trust state, plan phase, IDE capabilities, token usage or repository changes. That data reaches the model only as appended tool results.
3. **Stable capability surface.** The flat tool catalog is fixed per session. No capability discovery, MCP server change or IDE state can change the provider-visible tool list. A native `IDE` dispatcher tool is not introduced: the fixed catalog already meets this rule, and the Tool catalog forbids a model-visible generic dispatcher. Reopen that choice only if a later feature needs a changing capability set.
4. **Workflow state is not prompt state.** A future phase change (inspect, plan, implement, verify, review) is a runtime transition. It never swaps instructions or regenerates history. Phase guidance, if needed, is a new tail item.
5. **Incompatible contexts get separate generations.** A different model or a materially different instruction/tool context is a separate session. MODEL already requires this. Future planner/executor splits follow the same rule.
6. **Bounded at insertion, not rewritten later.** Tool results are bounded when first accepted (I12). Large content is retrieved explicitly by ranged tools. Old results are never shrunk to save context.
7. **Rejected output never enters the prefix.** Provisional or rejected responses (Stop during output, malformed or duplicate IDs, EOF) are never appended. A retry sends the identical bytes of the failed attempt. Display-transcript eviction affects only the display, never the request.

### Runtime model

```java
record CacheGeneration(CacheGenerationId id, PrefixFingerprint fingerprint) {}

sealed interface CacheReset permits SessionStart, ModelChange, ExplicitContextReset {}
```

`PrefixFingerprint` digests the canonical request head: model ID, instructions, attached instruction file, tool definitions in order and codec version. Before each dispatch, the run driver compares the new serialized request with the previous request of the same generation. If the previous request is not an exact prefix, the driver throws. A new generation starts only through a `CacheReset` value.

MVP reset boundaries: `SessionStart`, `ModelChange` and `ExplicitContextReset` (New Session). All three already happen only at idle. `Compaction`, `ConversationRewind` and `IncompatibleToolContractChange` are not variants in the MVP, because no MVP feature produces them. Each one enters the sealed type together with the feature that needs it (see Out of scope).

These operations never reset the generation: user turn, tool call, tool result, IDE state change, git state change, diagnostics change, build/test result. Plan-phase change, permission change and capability discovery have no MVP form. When one of them enters, it gets the same rule and a prefix test.

### Violation diagnostic

A failed prefix check reports the first divergence, not only a percentage:

```text
CACHE PREFIX VIOLATION

previous bytes:  83,492
reused bytes:    14,821
reuse:           17.7%

first divergence:
  component: tools
  tool: find_references
  field: description
  byte: 391

previous: "..."
current:  "..."
```

`component` is one of `head`, `instructions`, `tools` or `input[index]`. Excerpts are bounded and follow I10: tests print them, production logs record only component, index, field and byte offsets.

### Telemetry

`RunStats` records, per provider call and per session:

```text
structural prefix reuse: 98.7%
provider reported cache: 96.2%
```

Structural reuse is `reused bytes / previous request bytes` and is always available. Provider-reported cache comes from the Codex terminal response usage when present. Before implementation, check the exact usage field in Pi `openai-codex-responses.ts`. If the provider reports nothing, show only structural reuse. This stays inside OBSERVABILITY: no exporter or registry.

### Regression tests

A fake provider captures the exact serialized request of every call. For every consecutive pair in one generation, the test asserts `commonPrefix(requestN, requestN1) == requestN` and prints the violation diagnostic on failure. Cover:

- ordinary multi-turn conversation;
- several tool rounds in one run;
- semantic reads and symbol queries;
- mutations, including `FAILED_AFTER_START`;
- build and test execution;
- diagnostics that change between calls;
- git or file-system changes outside the agent between calls;
- Stop, `TRUNCATED_NOT_EXECUTED`, rejected response and request retry;
- New Session and model change, which must produce a new generation and never a violation.

Benchmark: a scripted long session through the fake provider must exceed 90% structural reuse after warm-up (first two calls excluded). A reset without a `CacheReset` value fails the test.

### Stretch target

In dogfood with a live Codex account and real repository tasks, target steady-state provider-reported cache hits of at least 95%, and more than 90% after warm-up. Classify every call below 90% as exactly one of:

- expected explicit reset;
- provider-side behavior;
- native prefix-stability bug.

"The harness built the prompt differently" is not a valid fourth category. The stretch target is a measurement, not a Go gate.

### Acceptance

- Requests are append-only within a generation, checked on every dispatch.
- Instructions, tool schemas and tool order are stable across turns and runs of a session.
- Mutable IDE and repository state cannot change the existing prefix.
- Reset boundaries exist as the sealed `CacheReset` type, and no other path starts a generation.
- Tests report the exact first divergence.
- The scripted benchmark exceeds 90% structural reuse after warm-up.

## UI

Read-only Editor transcript plus a separate `EditorTextField`; Send, Stop, New Session; status; login/logout/model controls; trusted-session enablement. Coalesce streamed display at 50 ms without unbounded queued deltas. Preserve draft and caret while streaming, disable Send during a run, support copy and source links. Show pending diagnostics, tool outcomes, partial answers and cancellation honestly. No approval buttons, branch browser, Markdown engine, persisted transcript or steering queue. No `NativeChatPanel`/`NativeMarkdownPane` inheritance. Browser/JCEF screenshots do not verify this UI.

## Roadmap

Each milestone becomes a feature closure that passes SPEC_VALID and DESIGN_VALID (`workflow.md`) before code. This table is not permission to implement from itself.

| # | Milestone | Behavior | Required proof |
|---|---|---|---|
| 0 | Lifecycle/admission slice | Run ownership, sequential admission, Stop, terminal accounting | `lifecycle-admission/` S1–S3. |
| 1 | Domain and run driver | Java algebra, minimal Kotlin driver, sequential continuation, accepted/provisional split, Stop accounting, run limits, `CacheGeneration`/`CacheReset`, dispatch-time prefix check | Platform-free tests: multi-call order, unknown/schema failure, length/malformed rejection, double Send, Stop in each state, resource-registration vs cancel race, no replay, late result retained, limits and retry scope with fake clock/transport. Prefix tests and scripted >90% structural-reuse benchmark (Prompt-cache stability). Coroutine lifetime smoke. `domain-run-driver/` S1–S3. |
| 2 | Native semantic reads | Seven read tools, handles, readiness, no MCP/HTTP | Same-name overloads, unrelated same-name class, reference vs text occurrence, unsaved editor read, ambiguity, pagination/incomplete, indexing timeout, handle eviction/invalidation. Real IDE operations. Prefix test across reads with changing editor/index state. |
| 3A | Semantic mutation | `replace_symbol_body`, `edit_text`, `write_file`, rename, undo, admission | Overload replacement with two declarations on one line; stale read/symbol rejected; queued Stop before EDT commit leaves file unchanged; admitted op settles before idle; rename without unrelated matches; rename usage-change race changes nothing; new-file/symlink race; undo of edit/create/rename. Prefix test across mutations. |
| 3B | Verification | Diagnostics, build, targeted tests, run-owned formatting | Format cancellation without deferred mutation; diagnostics failure is not clean; pending is not clean; targeted failing and passing test; busy/cancel lifecycle. Prefix test across changing diagnostics and build/test results. |
| 4 | Codex auth/transport | Login, PasswordSafe, SSE, replay, retry | Fake OAuth: state mismatch, duplicate params, port conflict with manual URL on same verifier, expiry, malformed token, listener cleanup, concurrent refresh vs logout. Fake HTTP: text/reasoning/calls, identity, split UTF-8/CRLF/multiline/EOF, terminal statuses, duplicate IDs, malformed args, oversize, cancellation; second request replays reasoning and pairs results. Deterministic codec bytes; retry sends identical bytes; provider cache usage parsed when present. Manual live smoke with a real account. If account/policy access is unavailable, the stage is blocked. |
| 5 | Native UI | Tool window workflow and content lifetime | Real task through UI; typing while streaming; double Send; Stop during request, read, queued and admitted mutation; New Session invalidation; close/reopen with no leaked editor or stale UI; token-free logs and errors. |
| 6 | Dogfood decision | Compare against Pi + idea-facade | See Dogfood gate. |
| 7 | Cutover and subtraction | Native registrations only; remove unreachable donor runtime | Done early by owner decision (DONOR_SUBTRACTION). Remaining obligation: startup and plugin archive evidence that no donor runtime is registered or packaged. |
| 3C | Command escape hatch | `run_command` | Only if dogfood proves it necessary. Flood bounded, Stop kills owned root and reports survivors, VFS refreshed after failing write, dirty-document conflict surfaced. |

### Milestone slices

Each milestone passes its gates as a sequence of slices of about 3–8 requirements, one `workflow.md` feature directory each. A slice depends only on earlier slices. Pure domain slices come before IntelliJ adapter slices. Slices without a directory are planned boundaries, not specifications; each boundary is confirmed when its S1 draft starts.

| Milestone | Slices in gate order |
|---|---|
| 2 Native semantic reads | R1 `read-domain` (arguments, bounds, registry, handles, outcomes, ordering) · R2 `read-pipeline` (admission, readiness, cancellation, failures, precedence, paths, `read_file`, `find_file`) · R3 `read-text-search` (`search_text`, paging) and R4 `read-symbols` (`search_symbols`, `get_file_outline`, `get_symbol_info`, handle identity), in either order · R5 `read-references` (`find_references`, milestone prefix test) |
| Codec (no milestone row yet; required before 4 live integration) | Tool schemas and argument parsing into typed read and mutation arguments · LLM status/code/content envelope and outcome rendering into `ToolOutcome` content |
| 3A Semantic mutation | Mutation admission and `edit_text` · `write_file` · `replace_symbol_body` · rename refactor · handle invalidation on edits and mutation prefix test |
| 3B Verification | Run-owned formatting flush · `get_problems` diagnostics · `build_project` and `run_tests` |
| 4 Codex auth/transport | SSE decoder and Responses transport with fake HTTP · OAuth PKCE and PasswordSafe credential owner · manual live smoke |
| 5 Native UI | Session owner and content lifetime (New Session, registry disposal) · transcript and composer streaming · trust opt-in, login and model controls |

After shared contracts freeze, tools and provider work can run in parallel with one integration owner for shared APIs, build files, `plugin.xml` and commits. Sibling workers skip builds while edits are concurrent; the integration owner validates after the wave settles. Persistence, providers and other expansion wait until after subtraction.

## Dogfood gate

Use a disposable Java fixture for deterministic safety checks and at least three real Java tasks:

- modify one overloaded method without touching another overload or an adjacent same-line declaration;
- rename a referenced symbol across files without changing an unrelated same-name symbol;
- fix a real defect, create a regression test file, read diagnostics and run the relevant test/build.

Compare native and Pi + idea-facade on equivalent clean task bases, same account/model/reasoning setting where supported; record differences as confounders. Count human interventions, wrong/stale target attempts, conflicts, relevant diagnostics, completion, tool calls, bounded source returned and text-fallback reasons. Wall time is secondary.

Correctness gates: no wrong-target edit; stale edit rejected; rename references correct; undo works; no queued effect begins after Stop; no leaked session-owned process/editor; test/build outcomes reported accurately. A manual live-provider run is mandatory for usability claims; fixtures prove only protocol handling.

- **Go:** all gates pass, real tasks complete, semantic tools carry identity/refactoring work and show concrete benefit (fewer targeting corrections or manual steps). Raw text reads of code content are expected.
- **Adjust:** semantic tools help, but discovery/identity/result contracts repeatedly force avoidable fallback.
- **Stop:** no semantic advantage on the chosen tasks, or native lifecycle complexity outweighs the benefit. Do not add features to hide this result.

`RunStats` exists only to make this decision factual. Include only fields that answer dogfood questions, for example provider calls, tool calls, tool failures, semantic resolutions, stale-handle failures, mutations attempted/completed, builds and tests run, elapsed time, provider wait vs tool time, Stop latency. Include structural prefix reuse and provider-reported cache per call and per session (Prompt-cache stability). Render a bounded summary; do not persist it initially or grow it into a metrics registry, event bus or tracing API. Logs may include run ID, call ID, tool name, state transition, outcome category, duration and cancellation events.

## Out of scope

Later features re-enter only through demonstrated need, not "we will obviously need this".

| Feature | Why excluded | Re-entry |
|---|---|---|
| Multiple providers, direct API keys, device OAuth | Does not test IntelliJ-native semantics; creates abstraction pressure | After Go, with a real second provider |
| Persistent sessions, SQLite, branching, compaction, import/export | Storage, migration, privacy, recovery | After Go, if real sessions need it. Compaction and rewind enter as `CacheReset` variants, only near real context pressure. |
| Semantic memory / vector search / Lucene | Unrelated to the coding loop | After Go, with a concrete memory use case |
| MCP server/client, ACP/external agents | The experiment removes that hop | Not part of native architecture |
| JCEF/web chat, Node/TypeScript frontend | Native Swing UI is part of the experiment | Only if native UI cannot support a required interaction |
| Hook scripting / Rhino, QR/ZXing | Unrelated | Concrete extension need after Go; ZXing probably never |
| General command runner | See COMMAND_ESCAPE_HATCH | Dogfood evidence |
| Git tooling, terminal plugin | Not needed for semantic proof; follow project VCS rules, no automatic commit/push | After Go |
| Observability subsystem, OpenTelemetry, custom JFR, telemetry export | Local logs and `RunStats` answer the MVP question | Specific operational question after Go; standard JFR first |
| Generic effects library, runtime DI, plugin/extension framework, tool marketplace | Premature abstraction | Demonstrated implementation pain or extension need |
| Background autonomy, multiple concurrent runs, steering/follow-up queue | Scheduling complexity, weaker determinism | After the single-run agent is reliable |
| Web/search tools, debug, notebook, database, memory/graph tools, subagents | Outside the hypothesis | After Go |
| Safe-delete, move, change-signature, inline, extract | Rename proves semantic refactoring | After Go |
| Remote development, cross-OS release, mutation testing, module rename, general performance work | Release qualification | After Go |

Basic safety, secret handling, context/output bounds, lifecycle, focused regression coverage and usable Codex login are **not** deferred hardening.

## Donor evidence (`master` only)

These files were removed on `native-agent-workflow`. The facts describe `master` at planning time and explain what a native reimplementation must not copy. `AB` is `plugin-core/src/main/java/com/github/catatafishen/agentbridge/`.

| Evidence | Fact | Consequence |
|---|---|---|
| `AB/psi/PsiBridgeService.java:323-369,399-419,438-464,519-539` | Dispatch logs arguments, invokes bridge permissions, installs UI tracking and calls `ToolDefinition.execute` under locks | Never wrap `callTool` as native dispatch |
| `AB/psi/tools/Tool.java:22-54` | Each tool owns mutable `argumentsHash`; required-parameter validation only | No shared tool instances; validate at the native boundary |
| `AB/psi/tools/editing/EditingTool.java:52-79,110-147` | Deferred formatting via `FileTool.queueAutoFormat`; first same-name match or nearest line; line-rounded locations | Exact identity and PSI ranges; run-owned formatting |
| `AB/psi/tools/editing/ReplaceSymbolBodyTool.java:85-143` | EDT work queued; line-based replacement; caller times out after 15 s | A timeout is not cancellation; admission at actual effect start |
| `AB/psi/tools/navigation/SearchSymbolsTool.java:69-102,145-170` | Paginated; word-index plus PSI named-element checks | Not a perfect symbol database; typed candidates with completeness |
| `AB/psi/tools/file/EditTextTool.java:58-73` | Omitted path, replace-all, case-insensitive matching allowed | Narrow the native schema and enforce it |
| `AB/psi/tools/refactoring/RefactorTool.java:103-150,167-210` | Queued EDT refactor; rename finds usages then refactors; safe-delete separate check | Rename only; discovery off the write boundary; revalidate usages |
| `AB/psi/ToolReadinessGate.java:56-69,157-176` | Smart-mode unexpected failure returns null; hints name non-MVP tools | Fail-closed native readiness and native hints |
| `AB/psi/tools/infrastructure/RunCommandTool.java:111-156` | Saves documents, routes tests, blocks some grep, supports hidden env injection | No hidden model arguments |
| `AB/psi/tools/RunPanelExecutor.java:80-85,141-151` | Unbounded `StringBuffer`; timeout destroys handler | Bound capture; report termination honestly |
| `plugin-core/src/main/resources/META-INF/plugin.xml:77-80,367-376` | Tool window and PSI/custom-MCP/graph startup registrations | Native mode needs startup isolation |
| `pi/packages/ai/src/providers/openai-codex.models.ts:4-8` | Imports generated model data absent from the checkout | No model-availability list; explicit model configuration |
