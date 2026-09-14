# Native Agent Plan Review Feedback

Basis:

- `handoff.md`
- `plan(3).md`
- `phased-scope.md`

This feedback reviews the trio for consistency and overengineering, then records the architectural decisions from the follow-up discussion.

---

## 1. Overall assessment

The current scope/plan/handoff trio is substantially better than the earlier merged 15-stage plan.

The core product direction is coherent:

- Pi-modeled sequential agent loop.
- IntelliJ semantic operations as the default coding surface.
- One Codex subscription provider for MVP.
- Ephemeral session state.
- Small fixed tool catalog.
- Exact PSI identity and stale-target rejection.
- Trusted-session execution rather than a new permission framework.
- No MCP/HTTP hop on the native execution path.
- Dogfood before deleting the donor runtime.
- Persistence, multi-provider support, compaction, extensions, telemetry, and similar infrastructure remain outside MVP.

The main risk is no longer product direction. It is allowing implementation detail and infrastructure to grow before the central hypothesis has been proven.

The scope should remain authoritative. The implementation plan should become smaller, more type-driven, and more explicit about what is intentionally deferred.

---

## 2. Consistency fixes

### 2.1 Review status wording

`handoff.md` begins with wording equivalent to "implement the reviewed MVP", while the plan explicitly records that independent review has not run.

Use:

> Implement the specified MVP after fresh-context review.

Do not imply the current author audit is independent approval.

### 2.2 Provider sequencing

The scope says:

> No speculative provider work before the coding workflow exists.

The plan allows Codex auth/transport implementation to proceed in parallel with tool work after Stage 1 contracts freeze.

The latter is reasonable. Clarify the rule as:

> Provider implementation may proceed in parallel after Stage 1 contracts are stable. Live provider integration cannot qualify before real native tools exist.

### 2.3 Session lifetime

The trio mixes:

- one session per project;
- project-scoped session state;
- content-scoped UI ownership;
- handle invalidation/disposal on content close.

Choose one lifetime:

> At most one native session per project. Its lifetime is the native tool-window content. Closing that content ends the session and invalidates all session handles.

This gives engine state, UI, handles, cancellation resources, listeners, and transcript one unambiguous parent lifetime.

---

## 3. MVP principle

The MVP exists to answer one question:

> Can a small IntelliJ-native agent reliably perform real Java coding tasks better than the current Pi + `idea-facade` path by making semantic IDE operations the default execution model?

Everything in the MVP must contribute directly to answering that question.

A feature should not enter the MVP merely because it would be useful in a finished agent.

The correct post-dogfood result may be:

- **Go** — native path clearly earns further investment;
- **Adjust** — hypothesis is promising but a small number of deficiencies must be fixed;
- **Stop** — native integration does not justify its complexity; retain Pi + `idea-facade`.

Stopping is a valid successful outcome of the experiment.

---

## 4. What is in the MVP

Keep the following. They are directly required to test the hypothesis:

- Pi-style sequential loop;
- one active native run;
- one Codex subscription provider;
- browser OAuth and secure token storage;
- exact symbol identity;
- opaque symbol/read handles;
- stale-handle rejection;
- bounded semantic reads;
- exact PSI mutation;
- no first/nearest-overload fallback;
- mutation revalidation;
- undo;
- cancellation/effect admission;
- focused diagnostics;
- native targeted build/test;
- minimal native Swing/editor UI;
- explicit dogfood tasks;
- minimal local diagnostic logging;
- bounded per-run statistics needed to compare native execution with Pi + `idea-facade`.

These are enough to prove or disprove the native-agent hypothesis.

---

## 5. Explicit MVP exclusions

The following are intentionally **out of scope**. Each item has a reason and a concrete re-entry condition.

| Feature | Why excluded from MVP | When it may return |
|---|---|---|
| Multi-provider support | Does not help prove IntelliJ-native semantics. Creates provider abstraction pressure too early. | After **Go**, if there is a real second provider to support. |
| Persistent conversations/sessions | Adds storage, migration, lifecycle, schema, privacy, and recovery concerns. MVP session can be ephemeral. | After **Go**, only if persistent sessions materially improve actual use. |
| Session database / SQLite | No persistence requirement in MVP. AgentBridge uses SQLite for donor features such as OpenCode session import. | Only if persistence is deliberately added later. |
| Semantic memory / vector search / Lucene | Unrelated to the coding-loop hypothesis. | Only if real dogfood shows repository-local semantic tools are insufficient and a concrete memory use case exists. |
| MCP server/client path | Native agent is specifically testing removal of that integration hop. | Retain only as donor compatibility until cutover; not part of native architecture. |
| ACP/external-agent clients | Same reason as MCP: donor compatibility, not native MVP. | Keep donor path until Go/cutover; remove if no longer needed. |
| JCEF/web chat UI | Native Swing/editor UI is part of the experiment. JCEF adds large runtime/UI complexity. | Do not reintroduce unless native UI demonstrably cannot support a required interaction. |
| Node/TypeScript chat frontend | Exists only for donor web/JCEF UI. | Remove after native cutover. |
| Hook scripting / Rhino | Unrelated to proving semantic coding. Adds a script runtime and hook lifecycle. | Only if a concrete extension use case appears after Go. |
| QR-code generation / ZXing | Not needed for browser OAuth flow. | Probably never. |
| General command runner | Creates process-tree, timeout, VFS-refresh, dirty-editor and cancellation complexity. | Add only if semantic tools + native build/test cannot complete real dogfood tasks. |
| Git tooling | Not required to prove semantic code navigation/edit/build/test. | Add after Go if agent-owned Git operations become a real requirement. |
| Terminal plugin dependency | Not required for native semantic operations. | Only if a real terminal-integrated feature is added. |
| General observability subsystem | Does not help answer MVP hypothesis better than small local measurements. | After Go, and only when specific operational questions require it. |
| OpenTelemetry | Backend/export/dashboard infrastructure is premature. | Only if the agent becomes a maintained product and distributed/aggregate traces become useful. |
| Custom JFR events | Useful for performance analysis, but not required to prove functional value. | Post-Go, when profiling asks questions that ordinary timers/logs cannot answer. |
| Telemetry export | Adds privacy, storage and product-policy concerns. | Only with an explicit product requirement. |
| Generic effects library | Core can express its state machine and outcomes explicitly. | Do not add unless concrete implementation pain demonstrates a missing abstraction. |
| Runtime DI framework | Object graph is small. Constructor wiring and IntelliJ services are enough. | Only if object construction becomes genuinely difficult. |
| Generic plugin/extension framework | Premature API stabilization. | After Go, only in response to a concrete extension need. |
| Tool marketplace/dynamic discovery | Opposes the small closed MVP tool surface. | Only if static tools become a demonstrated limitation. |
| Compaction/context-management framework | Not needed until real sessions prove context pressure. | After Go, based on observed transcript growth. |
| Background autonomy / multiple concurrent runs | Adds scheduling/state complexity and weakens determinism. | Only after the single-run agent is reliable and there is a concrete use case. |
| General performance framework | Premature optimization. | Post-Go when measurements identify actual bottlenecks. |

The plan should resist “we will obviously need this later” reasoning. Later features re-enter only through demonstrated need.

---

## 6. Overengineering review

Keep the product-critical mechanisms:

- Pi loop;
- exact PSI identity;
- semantic handles;
- exact mutation;
- Stop/effect admission;
- Codex OAuth;
- diagnostics/build/test.

Reduce or defer implementation details that do not yet carry product evidence.

| Area | Recommendation |
|---|---|
| 2,048-handle LRU | Keep a bound but do not over-specify the exact number initially. |
| Hash + stamp + identity everywhere | Prefer smart pointer + document/version identity normally. Hash only where crossing filesystem/process boundaries makes it useful. |
| Stage 3 | Split; it currently contains too many independent concerns. |
| `run_command` | Defer from the first semantic MVP slice. |
| Provider byte/size constants | Keep conservative caps but avoid architecture built around arbitrary magic values. |
| Exhaustive race-test matrix | Keep explicit invariants and representative regressions; add tests from observed failures. |
| Parallel implementation-owner structure | Optional; avoid unless there is genuinely independent work. |

### 6.1 Split current Stage 3

#### Stage 3A — Semantic mutation

Implement:

- exact method replacement;
- localized text edit;
- new-file creation;
- semantic rename;
- stale-target detection;
- touched-file reporting;
- undo;
- admission/cancellation around mutations.

#### Stage 3B — Verification

Implement:

- focused diagnostics;
- project build;
- targeted tests;
- formatting/import ownership required by those flows.

#### Stage 3C — Command escape hatch

Add `run_command` only if dogfooding proves it necessary.

It introduces a disproportionate amount of complexity:

- process ownership;
- bounded output buffering;
- timeout;
- descendant termination;
- surviving-PID accounting;
- VFS refresh;
- disk/editor conflict detection;
- lifecycle/disposal behavior;
- Stop semantics for external processes.

The project hypothesis is about semantic IntelliJ-native operations. Prove that path before paying this cost.

---

## 7. Language choice

AgentBridge is already a mixed Java/Kotlin codebase, and Kotlin is particularly prevalent in its UI.

The build already enables Kotlin in `plugin-core`.

Use:

> JVM 21. Existing donor code remains in its current language. New `nativeagent` domain, engine, provider, auth, and UI code should prefer Kotlin. Extracted donor PSI/tool operation code stays in its existing language unless moving it provides a concrete benefit.

This does not require a new module or toolchain.

---

## 8. Core architectural rule

Adopt this as a non-negotiable rule:

> **Unsound shell, typesafe core.**

External reality is unsound and mutable:

- LLM JSON;
- SSE/HTTP;
- OAuth;
- filesystem;
- processes;
- IntelliJ PSI/VFS/Document APIs;
- nullable platform APIs;
- mutable JSON trees.

The core should contain only values that have already passed the boundary checks required to construct them.

Conceptually:

```text
           UNSOUND / UNTRUSTED
────────────────────────────────────
LLM JSON
HTTP / SSE
OAuth
filesystem
processes
IntelliJ PSI/VFS APIs
JSON parser / nullable platform APIs
           │
           │ parse + resolve + validate
           ▼
────────────────────────────────────
             TYPED CORE

UserMessage
AssistantMessage
ValidatedToolCall
ResolvedSymbol
InspectedMethod
DocumentVersion
PreparedRename
RunState
ToolOutcome
ProviderOutcome
...
────────────────────────────────────
           │
           │ execute through adapter
           ▼
────────────────────────────────────
       UNSOUND SIDE EFFECTS
PSI write
refactor
HTTP send
process start
file write
```

### 8.1 Construction is proof

An object entering the core is proof that its construction preconditions succeeded.

Avoid raw core inputs such as:

```kotlin
data class RenameRequest(
    val symbolId: String,
    val readId: String,
    val newName: String,
)
```

Prefer progressive domain values:

```kotlin
@JvmInline
value class SymbolId private constructor(val value: String)

data class ResolvedSymbol internal constructor(
    val id: SymbolId,
    val pointer: SymbolPointer,
    val signature: SymbolSignature,
)

data class InspectedSymbol internal constructor(
    val symbol: ResolvedSymbol,
    val evidence: ReadEvidence,
)

data class PreparedRename internal constructor(
    val target: InspectedSymbol,
    val newName: ValidIdentifier,
    val usages: NonEmptyList<ResolvedUsage>,
)
```

The mutation boundary then accepts something like:

```kotlin
suspend fun rename(
    rename: PreparedRename,
    admission: MutationAdmission,
): RenameOutcome
```

The deep mutation code should not repeatedly rediscover whether a request was syntactically and semantically valid.

It should only re-establish assumptions that may have become invalid because the external world changed.

---

## 9. Runtime reflection policy

Make this an explicit invariant:

> **`nativeagent/**` and its third-party runtime dependencies must not require runtime reflection. Compile-time code generation is allowed and preferred where it removes runtime machinery. IntelliJ Platform internals are outside this rule.**

The target is not “the entire IntelliJ process performs no reflection.” That is not under our control.

The target is:

- no reflective serialization in native-agent code;
- no runtime classpath scanning;
- no dynamic proxy framework;
- no reflective DI;
- no runtime annotation discovery for tool registration;
- no `Class.forName(...)`-style dynamic discovery in the native core;
- no Kotlin reflection requirement.

The native path should prefer:

- Kotlin sealed/data/value types;
- explicit constructors;
- explicit/static registries;
- generated codecs;
- ordinary pattern matching;
- direct IntelliJ APIs.

IntelliJ service/extension instantiation is treated as platform behavior at the unsound boundary.

---

## 10. Core algebra

Use an explicit core algebra.

Do not use generic effect/result abstractions such as:

- `Either<E, A>`;
- `Try<A>`;
- `Raise<E>`;
- universal `Result` wrappers;
- generic monadic effect runtimes.

Without language-level do-notation/for-comprehension syntax, these add composition plumbing without helping this domain.

Prefer named outcomes whose variants belong to the operation whose caller can act on them.

Example:

```kotlin
sealed interface RenameOutcome {
    data class Renamed(
        val symbol: SymbolId,
        val touchedFiles: Set<ProjectFile>,
    ) : RenameOutcome

    data class StaleTarget(
        val symbol: SymbolId,
    ) : RenameOutcome

    data class Conflict(
        val conflicts: List<RenameConflict>,
    ) : RenameOutcome

    data class TooBroad(
        val affectedFiles: Int,
        val limit: Int,
    ) : RenameOutcome

    data class Unsupported(
        val reason: String,
    ) : RenameOutcome
}
```

Call sites remain normal Kotlin:

```kotlin
when (val result = renamer.rename(request)) {
    is RenameOutcome.Renamed -> ...
    is RenameOutcome.StaleTarget -> ...
    is RenameOutcome.Conflict -> ...
    is RenameOutcome.TooBroad -> ...
    is RenameOutcome.Unsupported -> ...
}
```

Do not create one giant `AgentError` hierarchy either.

Keep outcomes local:

```text
SymbolLookupOutcome
ReferenceSearchOutcome
ReadOutcome
ReplaceMethodOutcome
RenameOutcome
DiagnosticsOutcome
BuildOutcome
TestOutcome
ProviderOutcome
LoginOutcome
```

At the LLM boundary they can be rendered into a generic protocol envelope:

```json
{
  "status": "error",
  "code": "STALE_EVIDENCE"
}
```

The core itself should not depend on those strings.

---

## 11. Type safety strategy

The current plan has the right direction with immutable domain values, but it still leaks protocol-style structures into the core.

Avoid a generic internal envelope such as:

```text
status
code
content
details
truncated
```

That is appropriate for the LLM/wire boundary, not the domain.

Likewise, immutable generic JSON inside a `ToolCall` is not enough. It still forces runtime schema interpretation inside the core.

Use:

```text
wire JSON
    ↓ parse + validate
ValidatedToolCall<SearchSymbolsArgs>
    ↓
typed operation
    ↓
SearchSymbolsOutcome
    ↓ render
LLM-facing JSON
```

No core code should branch on:

```text
status == "error"
code == "STALE_HANDLE"
args["symbol_id"]
```

Those conditions should already have become types.

---

## 12. Expected failures vs bugs

Use three categories.

### Expected domain outcomes

Represent as named sealed outcomes:

```text
symbol ambiguity
stale PSI identity
document changed
rename conflict
index unavailable
diagnostics pending
HTTP rate limit
authentication required
context full
```

### Cancellation

Use coroutine / agent-state-machine cancellation semantics.

Cancellation is control flow, not normally a domain result at every layer.

Translate cancellation into a protocol/tool result only where required by the agent transcript contract.

### Bugs / violated invariants

Throw.

Examples:

```text
accepted ToolResult without a CallId
impossible state transition
null in a domain type that cannot be null
internally mismatched call/result identity
```

Do not turn impossible core states into recoverable `InternalError` values.

---

## 13. IntelliJ itself is an unsound boundary

PSI is semantically rich, but it is still mutable external state from the domain's perspective.

Do not leak these into the core:

- `PsiElement`;
- `VirtualFile`;
- `Document`;
- raw smart pointers;
- read/write-action primitives;
- dumb-mode state.

Use adapter-side resolution:

```text
PsiElement?
    ↓ resolve
SymbolResolution
    ↓
ResolvedSymbolHandle
```

The core holds typed evidence and identity.

When moving from the core back into IntelliJ for a side effect, the adapter re-resolves/revalidates because the external world may have changed.

Type safety does not mean pretending prior evidence remains valid forever.

It means:

> The core precisely represents what was proven, and every transition back into mutable reality explicitly re-establishes the assumptions needed for the requested side effect.

---

## 14. Typestate without a typestate framework

Many current runtime checks can become construction boundaries.

For example:

```text
RawSymbolCandidate
        ↓ resolve
ResolvedSymbol
        ↓ inspect
InspectedSymbol
        ↓ prepare
PreparedMutation
        ↓ admission
AdmittedMutation
        ↓ execute
MutationOutcome
```

Each arrow may produce a named failure outcome.

Similarly:

```text
SSE bytes
   ↓
WireEvent
   ↓
ParsedProviderResponse
   ↓
ValidatedAssistantResponse
   ↓
ExecutableToolBatch
```

`AgentLoop` must never receive:

- partial SSE frames;
- malformed call IDs;
- mutable JSON trees;
- nullable wire fields;
- incomplete tool argument fragments.

Only a validated terminal response can produce an executable tool batch.

---

## 15. JSON strategy

Gson is a donor dependency. It should not define the new native-agent architecture.

For `nativeagent/**`:

> JSON parser/binding types exist only at the wire/tool codec boundary. The core has no dependency on Gson, generic JSON trees, or serializer annotations.

### 15.1 Gson

Do not introduce new Gson usage in `nativeagent/**`.

Existing AgentBridge donor code can continue using Gson until cutover. Purging donor Gson before the native path proves itself would be cleanup-driven scope expansion.

After Go/cutover, remove Gson if no surviving donor feature needs it.

### 15.2 Avaje JSONB

Avaje JSONB is a good fit for **fixed wire DTOs** because compile-time generated codecs align with the no-runtime-reflection rule.

Good examples:

```text
OAuth responses
stable Codex request DTOs
stable Codex usage/metadata DTOs
```

Prefer generated Java records or similarly simple wire types where that keeps code generation predictable.

Avaje is a boundary tool, not a domain serialization model.

Core domain types should not acquire serializer annotations merely for convenience.

### 15.3 Helidon JSON

Helidon JSON core is useful if the Codex protocol requires a small dynamic JSON tree/parser surface for:

```text
SSE event payloads
tool argument objects before typed decoding
forward-compatible unknown provider fields
tool schema generation/rendering
```

If used, it stays entirely in codecs/adapters.

Do not add Helidon JSON binding merely because it exists.

### 15.4 Keep one JSON dependency if possible

Do not automatically combine Avaje + Helidon.

Preferred decision sequence:

1. Try Avaje-generated fixed wire DTOs plus minimal hand parsing where necessary.
2. If dynamic JSON handling becomes awkward, evaluate Helidon JSON core.
3. Keep only the smallest combination that materially simplifies the boundary.

The target is not “use a modern JSON stack.” The target is a small reflection-free boundary with no JSON leakage into the core.

---

## 16. Network stack

Use the JDK network stack unless evidence proves it insufficient.

Target:

```text
CodexProvider
    |
    +-- JdkCodexTransport
            |
            +-- java.net.http.HttpClient
            +-- strict SSE decoder
            +-- JSON boundary codec
```

No third-party HTTP client is needed for the MVP.

JDK `HttpClient` already provides what this use case needs:

- async requests;
- streaming response bodies;
- TLS;
- redirects;
- connection reuse;
- proxy support;
- cancellation integration.

Use one reusable client per appropriate provider/session lifetime rather than creating one per request.

### IntelliJ proxy integration

The transport adapter should honor IDE proxy/auth configuration.

This is a platform-boundary concern. It must not leak IntelliJ networking types into the core.

If IntelliJ proxy integration exposes a concrete limitation in raw JDK `HttpClient`, reassess then. Do not add OkHttp/Ktor/Apache HC pre-emptively.

### SSE

Implement a small strict SSE decoder tailored to the Codex protocol.

Do not add an EventSource framework merely to parse a straightforward stream.

---

## 17. Async execution and lifetime

No separate effects runtime is required.

Use Kotlin coroutines as the async/lifetime mechanism:

```kotlin
suspend fun generate(...): ProviderOutcome

suspend fun findReferences(...): ReferenceSearchOutcome

suspend fun replaceMethod(...): ReplaceMethodOutcome
```

Interpretation:

- `suspend` means the operation may wait, perform asynchronous work, and participate in cancellation;
- the return type enumerates meaningful domain outcomes.

Use IntelliJ-owned coroutine scopes so project/content/plugin lifetime controls:

- network requests;
- provider work;
- diagnostics;
- queued tool work;
- UI callbacks.

Do not add another coroutines runtime/version.

Do not turn `CancellationException` into a generic failure value throughout the codebase.

Cancellation normally propagates as structured cancellation. The agent state machine converts it into the transcript semantics required by Stop.

---

## 18. Observability and telemetry

General observability is **not** part of the MVP.

Do not add:

- OpenTelemetry;
- trace exporters;
- metric exporters;
- dashboards;
- event databases;
- general telemetry SDKs;
- custom JFR events;
- telemetry upload;
- a generic event taxonomy.

The MVP does need enough local evidence to evaluate the experiment.

### 18.1 Local logging

Use IntelliJ `Logger` for bounded technical diagnostics.

Logging may include:

- run ID;
- call ID;
- tool name;
- run state transition;
- outcome category;
- elapsed duration;
- cancellation/disposal events;
- high-level provider status.

Never log:

- prompts;
- model responses;
- tool arguments;
- source-code payloads;
- OAuth tokens/secrets;
- raw provider bodies.

Logging exists for local debugging, not product analytics.

### 18.2 `RunStats`

`RunStats` is not an observability framework.

It is a tiny run-owned value whose only purpose is to provide comparable dogfood evidence.

Possible shape:

```kotlin
data class RunStats(
    val providerCalls: Int,
    val toolCalls: Int,
    val toolFailures: Int,
    val semanticResolutions: Int,
    val staleHandleFailures: Int,
    val mutationsAttempted: Int,
    val mutationsCompleted: Int,
    val buildsRun: Int,
    val testsRun: Int,
    val elapsed: Duration,
)
```

Only include fields that directly answer dogfood questions.

Potential additional measurements if needed:

```text
time to first useful tool call
provider wait time
tool execution time
Stop latency
diagnostics pending/ready count
build/test success/failure
```

Do not persist `RunStats` initially unless the dogfood procedure requires it. Printing/rendering a bounded summary is enough.

Do not turn it into:

- a metrics registry;
- an event bus;
- a time-series store;
- a tracing API;
- an extensible telemetry schema.

### 18.3 Why `RunStats` exists

The dogfood comparison needs factual answers such as:

- How many provider turns did native execution require?
- How many semantic operations succeeded?
- How often did evidence become stale?
- Did mutations complete without retries?
- How much time was provider wait vs IDE work?
- Did Stop terminate work promptly?
- Did the native agent finish tasks with fewer integration/tool failures than Pi + `idea-facade`?

Without minimal measurement, the Go/Adjust/Stop decision becomes anecdotal.

That is the full justification for `RunStats`.

### 18.4 When richer o11y may return

After **Go**, richer instrumentation may be considered only in response to a concrete question.

Examples:

- unexpected allocation pressure -> use ordinary JFR profiling first;
- suspected EDT stalls -> profile/read traces;
- unclear coroutine/thread contention -> profile;
- production reliability question -> consider structured tracing;
- aggregate product metrics requirement -> consider telemetry separately with privacy/product review.

Custom JFR events should be added only when standard JFR plus local logs/timers cannot answer a specific profiling question.

---

## 19. Dependency target

The end-state native runtime should be intentionally small.

Target shape:

```text
IntelliJ Platform
  ├─ Kotlin runtime
  ├─ coroutines
  ├─ PSI / VFS / refactor
  ├─ Swing/editor UI
  ├─ PasswordSafe
  ├─ Logger
  └─ build/test/process APIs as actually needed

JDK
  └─ java.net.http.HttpClient

Third party
  └─ JSON codec/parser only if required
       ├─ Avaje JSONB for generated fixed DTO codecs
       └─ optionally Helidon JSON core for dynamic JSON

Our code
  ├─ explicit typed domain algebra
  ├─ agent loop
  ├─ Codex transport/codec/SSE
  └─ semantic IntelliJ adapters
```

Avoid adding runtime dependencies for:

- networking;
- effects;
- DI;
- logging;
- persistence;
- telemetry;
- reflection;
- dynamic proxying;
- plugin discovery.

Every runtime dependency must justify itself against what the JDK/IntelliJ platform already provides.

---

## 20. Dependency cleanup after Go

Do not purge donor dependencies before dogfood merely to make the build aesthetically clean.

After a Go/cutover decision, aggressively remove donor-only runtime/build dependencies that are no longer reachable from the native path.

Likely candidates:

```text
Gson donor usage
Rhino
ZXing
sqlite-jdbc
Lucene-related donor integration
JCEF dependency
Git4Idea if unused
terminal plugin if unused
DVCS/log modules if unused
MCP server packaging
chat-ui/
js-tests/
npm/esbuild/TypeScript/Vitest/happy-dom
SQLite native stripping task
hook-generation tasks/resources
ACP/MCP-specific services and settings
```

Subtraction should happen after the native path proves value, not before.

---

## 21. Recommended Stage 1 change

Stage 1 should explicitly become a **domain algebra + state-transition** stage.

It should establish:

- strongly typed IDs;
- sealed message types;
- sealed run states;
- accepted vs provisional response distinction;
- validated provider response;
- executable tool batch;
- typed tool-call arguments;
- named tool/provider outcomes;
- exact call/result pairing;
- mutation/effect admission capability;
- cancellation state transitions;
- platform-free tests of those invariants.

Do not spend Stage 1 implementing generic framework abstractions.

The purpose is to make invalid agent states difficult or impossible to represent.

---

## 22. `NativeExecutionContext`

The current plan risks turning `NativeExecutionContext` into a giant mutable bag containing:

- `RunId`;
- cancellation;
- admission;
- deadline;
- resources;
- touched files;
- and potentially more over time.

Avoid that.

Prefer capability-oriented values where possible:

```text
RunContext
ReadCapability
MutationAdmission
ProcessOwnership
Deadline
```

A mutation API should receive the minimum capability required to perform that mutation.

In particular, effect admission should produce an explicit capability/token consumed by the operation allowed to cross the side-effect boundary.

This makes the Stop-vs-effect-start invariant easier to reason about than a shared mutable context object.

---

## 23. Revised implementation philosophy

The implementation should optimize for these rules:

1. **Unsound shell, typesafe core.**
2. `nativeagent/**` and its third-party runtime dependencies require no runtime reflection.
3. Compile-time generated code is welcome when it removes runtime machinery.
4. JSON implementation details stay at provider/tool protocol boundaries.
5. IntelliJ classes stay at platform boundaries.
6. Core identifiers are distinct value types.
7. Domain operations return named sealed outcomes.
8. Expected failures are values; invariant violations are bugs.
9. Cancellation remains structured control flow.
10. Kotlin coroutines provide async/lifecycle semantics.
11. No generic effects abstraction in MVP.
12. No generic error registry inside the core.
13. Use construction/progression of types to encode evidence.
14. Revalidate only facts external mutable state can invalidate.
15. Use JDK `HttpClient`; add no HTTP client dependency without evidence.
16. Keep observability to local logging + dogfood `RunStats`.
17. No custom JFR events in MVP.
18. Keep `run_command` out of the first semantic proof unless real dogfooding needs it.
19. Do not clean donor dependencies until the native path earns a Go.
20. Prove semantic value before expanding architecture.

---

## 24. Concrete plan edits

Before implementation, make these changes to the trio:

- Change handoff wording from "reviewed MVP" to "specified MVP after fresh-context review".
- Clarify provider implementation may run in parallel after Stage 1, but cannot qualify before native tools.
- Define one explicit session/content lifetime.
- Change language guidance to JVM 21 with Kotlin preferred for new `nativeagent` code.
- Add **unsound shell / typesafe core** as a global invariant.
- Add the **no runtime reflection in `nativeagent/**` or its third-party runtime dependencies** invariant.
- Explicitly allow/prefer compile-time generated codecs and similar codegen.
- Replace generic internal `status/code/details` error modeling with named sealed outcomes.
- State that the generic result envelope exists only at the LLM/tool protocol boundary.
- Make Stage 1 explicitly responsible for the core algebra and type-state transitions.
- Keep Gson donor-only; add no new Gson dependency to native-agent code.
- Prefer Avaje-generated codecs for stable fixed wire DTOs if code generation is useful.
- Evaluate Helidon JSON core only for genuinely dynamic JSON needs.
- Keep JSON types and serializer annotations out of the core.
- Use JDK `HttpClient` plus a small strict SSE decoder.
- Add no third-party HTTP stack unless IDE proxy/network requirements prove JDK transport insufficient.
- Split Stage 3 into semantic mutation, verification, and optional command execution.
- Defer `run_command` until proven necessary if possible.
- Replace or narrow mutable `NativeExecutionContext` with capability-oriented values.
- Use IntelliJ-owned coroutine scopes for async execution and cancellation.
- Keep MVP observability to bounded local logs and a tiny run-owned `RunStats`.
- State exactly what `RunStats` measures and that it exists only to support Go/Adjust/Stop dogfood evidence.
- Explicitly exclude OpenTelemetry, telemetry export, generic metrics/tracing infrastructure, and custom JFR events from MVP.
- Add the explicit MVP-exclusion table with re-entry conditions.
- Keep donor cleanup after the Go decision, not before.

These changes should reduce implementation size while strengthening correctness and making the MVP boundary materially harder to erode.
