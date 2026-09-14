# Domain and run driver design

## Status and dependency

Design draft for `CAP-DOMAIN-RUN-DRIVER`. Canonical behavior is `spec.json`. The only lifecycle authority is the frozen `nativeagent.lifecycle.RunLifecycle`; this feature composes it and does not restate or fork its state machine.

The Java package is `com.github.catatafishen.agentbridge.nativeagent.run`. Kotlin uses the same package so internal driver ports can consume package-private Java capabilities without widening the public plugin API.

## Ownership

`RunSession` is the Java transition owner for one ephemeral accepted conversation and at most one active `RunContext`. It owns one `RunLifecycle`, accepted messages, provisional display state, the current run budget, ordered call/result ledger, and the domain run phase. A successful `start` obtains the lifecycle handle before appending the user message. A rejected start cannot mutate history (DR-001).

`NativeRunDriver` is a Kotlin child job launched in the caller-supplied `CoroutineScope`. It owns the serial request/call loop, request-local retry controller, and suspension. It never owns a second transition model. Every state change goes through `RunSession`; every tool effect crosses a `CallAdmission` that delegates to the current lifecycle batch at the adapter's actual execution entry (DR-004, DR-008, DR-015).

`RunResources` is a Java run-owned registry. One monitor linearizes registration, removal, and cancellation. `register` after cancellation closes the resource before returning. Entries use an atomic closed bit so cancellation, explicit completion, and late registration close each resource at most once (DR-014).
`CacheGeneration` is a Java append-only byte owner composed by the session's Kotlin driver. Its controlled factories distinguish initial `SessionStart` from replacement-session `ModelChange`/`ExplicitContextReset` and check that the replaced session is idle. It owns the stable head fields, one-time rendered accepted-item fields, the previous dispatched `CacheRequest`, and structural-reuse observations (DR-017–DR-020).


## Java surface

Production declarations are installed once under `plugin-core/src/main/java/com/github/catatafishen/agentbridge/nativeagent/run/`.

| Declaration | Frozen role |
|---|---|
| `RunMessage` | Sealed accepted-message algebra: `User`, `Assistant`, `ToolResult`. Constructors reject nulls and snapshot collections. |
| `PlannedCall` | Sealed `Executable` or `Rejected`. Both carry one `Call.Id`; `Executable` carries `ToolName` and an opaque package-owned `ToolOperation`; `Rejected` carries a `CallError`. |
| `ValidatedAssistantTurn` | Closed terminal boundary algebra for complete text/calls, length text/calls, and protocol rejection. Only accepted variants expose an `Assistant` value. |
| `ToolOutcome` | Sealed terminal tool algebra: `Completed`, `NotStarted`, `FailedAfterStart`; only started variants may carry touched paths. |
| `RunLimits` | Validated immutable maxima and deadline duration. `mvp()` is 20 responses, 100 executable-tool admissions, and 15 minutes. |
| `RunLimits.Budget` | Run-local counter/deadline policy. Named request and executable-tool admission outcomes prevent Boolean ambiguity. Accounting-only rejected calls do not increment the 100-invocation cap (review R2-N-001). |
| `RunTimeSource` | Java time capability used by actual-boundary admission; Kotlin `RunClock` supplies it and owns suspend delays. |
| `RetryPolicy` | Pure Java classification and delay selection. It returns `NoRetry`, `RetryAfter(Duration)`, or `RateLimitWait(Duration?)`; no clamping. |
| `RunCancellation` | Idempotent cooperative token. Listener registration after cancellation runs immediately and exactly once. |
| `RunResources` | Race-safe ownership for `AutoCloseable` resources with explicit registration handles. |
| `CallAdmission` | Single-call capability wrapping `RunLifecycle.execute`. It exposes only `execute(Effect)` and the correlated `Call.Id`. |
| `RunSession` | Final Java transition owner described below. Public mutations require its opaque unforgeable current-run capability; observations are immutable records/sealed variants. |
| `CacheField` | Immutable named cache-relevant byte field; input is copied and access returns a copy. |
| `CacheRequest` | Immutable generation-tagged concatenation with component/field/item byte ranges. |
| `CachePrefixGuard` / `CachePrefixViolation` | Pre-dispatch exact-prefix check, reuse observation, and metadata-only first-divergence failure. |
| `CacheGeneration` | Controlled append-only owner with sealed reset reasons and one-time `ModelItemRenderer` calls. |
| `ModelItemRenderer` | Java boundary capability that renders one newly accepted message into ordered immutable fields once. |


Distinct identifiers use existing `Call.Id` for provider call correlation. No second call-ID type or lifecycle alias is introduced. The domain result `CANCELLED_NOT_STARTED` maps from lifecycle `CANCELLED_BEFORE_START`. A tool-limit refusal is checked inside the lifecycle callback before the tool effect: lifecycle records that accounting call `COMPLETED`, while the domain records `RUN_LIMIT_NOT_STARTED` and invokes no tool.

### RunSession operations

The frozen operations are public so Kotlin in the same module can call them without reflection. Mutations require an opaque current `ActiveRun`, which is the authority boundary:

```text
StartResult start(RunMessage.User user, RunLimits limits, Instant now, RunTimeSource timeSource)
HistorySnapshot history()
ProvisionalObservation provisional()
Phase phase()
void updateProvisional(ActiveRun run, String text)
boolean tryUpdateProvisional(ActiveRun run, String text)
void clearProvisional(ActiveRun run)
RunLimits.Admission beginRequest(ActiveRun run, Instant now)
TurnAcceptance acceptTurn(ActiveRun run, ValidatedAssistantTurn turn)
CallStep nextCall(ActiveRun run)
void recordToolResult(ActiveRun run, RunMessage.ToolResult result)
StopResult stop(ActiveRun run)
boolean stopIfCurrent(ActiveRun run)
StopResult stopForLimit(ActiveRun run)
FinishResult finish(ActiveRun run)
void dispose()
```

`ActiveRun` is an opaque capability created only by `start`; it contains the lifecycle `RunHandle`, captured limits, budget, resources, cancellation token, and private owner identity. Callers cannot construct it or mutate its ledger.

`acceptTurn` appends an accepted assistant once, clears provisional state, increments the response budget, and creates a `Call.Batch` containing every accepted call ID. Complete text and length text return an end-run outcome. Complete calls return the first `CallStep`. Length calls settle every call through a synchronous accounting-only lifecycle callback and append `TRUNCATED_NOT_EXECUTED` results without exposing a `CallAdmission` to a tool adapter. Protocol rejection appends nothing.

`nextCall` returns one of `Execute(PlannedCall.Executable, CallAdmission)`, `AccountRejected(PlannedCall.Rejected, CallAdmission)`, `Complete`, or `Stopped`. It never advances past an unsettled lifecycle call. `CallAdmission` checks executable-tool budget inside the admitted lifecycle callback immediately before invoking the tool effect. A budget refusal settles the lifecycle call as accounting-only `COMPLETED`; `ToolRunner` returns `RUN_LIMIT_NOT_STARTED`, and the driver stops remaining calls with the same domain reason. A rejected call is likewise settled through `CallAdmission.execute` with an empty accounting callback and does not increment the tool budget.

`recordToolResult` requires the earliest unsettled call identity and rejects duplicate, foreign, or out-of-order normal results. During Stop, cancelled later calls are retained in lifecycle state but their domain results are appended only after any earlier executing call settles. This keeps accepted tool-result messages in source order while preserving late admitted results. `Complete` is reachable only when both ledgers settle (DR-004, DR-008).

`stop` sets the cooperative token, linearizes frozen lifecycle Stop, records its phase and cancellation reason, closes resources, and converts only the next source-ordered cancelled calls. Cancellation-listener or resource-close failures are aggregated after lifecycle Stop and cannot leave admission open. `stopIfCurrent` makes stale driver callbacks benign. `finish` closes future resource registration and delegates to `finishRun`; `Pending` preserves `STOPPING`. Disposal calls lifecycle `close`, suppresses provisional delivery, and retains accepted history until owned work settles.

## Prompt-cache surface and algorithm

`CacheGeneration.initial(id, headFields)` is the only `SessionStart` path. `CacheGeneration.replacement(previousSession, reset, id, headFields)` accepts only `ModelChange` or `ExplicitContextReset` and rejects unless `previousSession.phase()` is `IDLE`. The previous generation is never mutated or relabeled.

`requestFor(history, renderer)` compares the immutable accepted-message prefix with its stored ownership list. Shortening, replacement, or reordering throws. Only the unseen tail is passed to `ModelItemRenderer`; each returned `CacheField` is copied once and retained with its input index. The generation extends the prior byte array with those stored field bytes. An unchanged history returns the same `CacheRequest` instance, so a retry cannot rerender or change bytes.

`CacheRequest` contains the cache-relevant prefix only: ordered stable-head and input fields that affect provider prefix caching. Provider-specific JSON closing syntax and non-cache suffixes are added by the later transport codec and are not part of this internal prefix. `CachePrefixGuard.verify(previous, candidate)` requires matching generation identity and `candidate.bytes` to start with all `previous.bytes`. It returns an immutable observation of the previous, candidate, and reused byte counts on success; `Observation.structuralReuse()` derives the ratio.

On mismatch, the guard finds the first byte offset and resolves it through immutable component ranges to `HEAD` or `INPUT`, the field name, and optional input index. `CachePrefixViolation` contains only this metadata and lengths. The driver catches it through its existing invariant-failure path, calls `stopIfCurrent` and `finish`, then rethrows after the session reaches `IDLE`; transport is never invoked.

The driver prepares one `ProviderRequest(history, cacheRequest)` before the request-local attempt loop. Retries reuse that object. A terminal accepted response changes history; provisional/rejected output does not. The next loop prepares an append-only request and verifies it immediately before transport.

Structural reuse is total common-prefix bytes divided by total previous-request bytes. The deterministic benchmark uses 20 dispatches, excludes the first two from aggregation, and includes user, assistant, tool-result, truncation, Stop, and rejected-output paths.

## Kotlin surface

Production declarations are under `plugin-core/src/main/kotlin/com/github/catatafishen/agentbridge/nativeagent/run/`.

```text
internal fun interface ProviderTransport {
    suspend fun request(request: ProviderRequest, sink: ProvisionalSink, resources: RunResources): ProviderAttempt
}

internal fun interface ToolRunner {
    suspend fun execute(call: PlannedCall.Executable, admission: CallAdmission,
                        cancellation: RunCancellation): ToolOutcome
}

internal interface RunClock {
    fun now(): Instant
    suspend fun delay(duration: Duration)
}

internal class NativeRunDriver(
    private val scope: CoroutineScope,
    private val session: RunSession,
    private val transport: ProviderTransport,
    private val tools: ToolRunner,
    private val clock: RunClock,
    private val cache: CacheGeneration,
    private val renderer: ModelItemRenderer,
    private val retryPolicy: RetryPolicy = RetryPolicy()
) {
    fun submit(user: RunMessage.User, limits: RunLimits = RunLimits.mvp()): SubmitResult
    fun stop(): StopRequestResult
    fun dispose()
}
```

`ProviderRequest` is an immutable accepted-history snapshot paired with one immutable `CacheRequest`. `ProviderAttempt` is a sealed Kotlin boundary result: `Terminal(ValidatedAssistantTurn)`, `Retryable(RetryPolicy.Failure)`, `Failed(ProviderFailure)`, or `Stopped`. Raw JSON, SDK values, and HTTP resources remain outside this feature. `ProvisionalSink` uses `tryUpdateProvisional`; a late or stale generation returns false without mutation.

`submit` synchronously calls `RunSession.start`; only `Started` launches a child coroutine and returns its handle. `RunSession` remains the sole active-run authority; the driver's atomic holder replaces a completed run without a stale-holder rejection window. The child prepares and verifies one cache request, then performs one provider request and one call at a time. The request-local attempt counter is discarded after a terminal response and retries reuse the prepared request object. Retry clears provisional state before the injected clock delay and never has append authority (DR-010, DR-013, DR-018–DR-020).

For an executable call, the driver passes `CallAdmission` to `ToolRunner`. The adapter invokes it at actual effect entry and maps `Limited` to `RUN_LIMIT_NOT_STARTED`. Returning without a terminal lifecycle status is a contract violation and stops the run. An accounting rejection also stops rather than looping. A `FailedAfterStart` result stops before the next call. A terminal provider failure calls `stopIfCurrent` and `finish` before the child exits.

`stop` never cancels the run child. It calls race-tolerant `stopIfCurrent`; provider resources and the cooperative token wake the child. A sibling watcher in the supplied parent scope executes the same Stop path when parent cancellation begins. Executable-call awaiting and result capture run in `NonCancellable` only for the one already-owned call; lifecycle Stop still rejects a queued call before its effect. The adapter must use the token to end cancellable pre-admission waits and must not detach admitted work. This preserves late results and structured lifetime without `GlobalScope` or `runBlocking` (DR-008, DR-015).

## Retry and limit precedence

At each loop edge:

1. Stop/disposal wins before request or effect admission.
2. Deadline is checked before a provider request and inside lifecycle admission immediately before an executable tool effect.
3. The response cap is checked before a provider request; the twentieth accepted response is allowed.
4. The tool cap is checked inside `CallAdmission`; the hundredth actual tool effect is allowed. A refusal settles lifecycle accounting without invoking the tool.
5. Prevalidated rejection accounting does not consume the tool cap because no tool is invoked.
6. A retry belongs to the current provider request and does not increment accepted-response count.
7. Invalid or above-cap `Retry-After` returns the terminal rate-limit outcome; no delay or second attempt occurs.

If a limit is reached after an assistant with calls is accepted, all not-yet-admitted accepted calls receive `RUN_LIMIT_NOT_STARTED` in source order through accounting-only lifecycle settlement. No continuation follows (DR-012).

## Rejected alternatives

- A second driver state machine mirroring `RunLifecycle`: rejected because it creates two authorities for Stop and admission.
- Suspending inside `Effect.execute`: rejected because the frozen effect is synchronous and forbids detached work. The adapter instead receives `CallAdmission` and invokes it inside the actual queued callback.
- A generic `Result`, provider abstraction hierarchy, mutable execution context, or callback bag: rejected by product decisions and because operation-local variants remove illegal combinations more directly.
- Counting prevalidated rejections as tool invocations: rejected because no tool implementation or external effect is invoked.
- Cancelling the driver job immediately on Stop: rejected because coroutine cancellation could discard an admitted late result before accounting.
- A second coroutine runtime or `GlobalScope`: rejected by ASYNC.

## Allowed implementation surface

Allowed production paths:

- `plugin-core/src/main/java/com/github/catatafishen/agentbridge/nativeagent/run/**`
- `plugin-core/src/main/kotlin/com/github/catatafishen/agentbridge/nativeagent/run/**`

Allowed test paths:

- `plugin-core/src/test/java/com/github/catatafishen/agentbridge/nativeagent/run/**`
- `plugin-core/src/test/kotlin/com/github/catatafishen/agentbridge/nativeagent/run/**`

Allowed support changes:

- `plugin-core/build.gradle.kts` only if the existing IntelliJ-owned coroutine classpath is insufficient for compile/test.
- `scripts/native-spec/check_slice.py` for feature selection, Kotlin target discovery, exact-basis receipts, and preserved lifecycle selftests.
- `native-agent-docs/domain-run-driver/**` and the retry clarification in `native-agent-docs/product.md`.

Forbidden: changes under `nativeagent/lifecycle/**`, new modules, provider/JSON/HTTP/PSI/UI code, reflection, another coroutine runtime, or parallel orchestration.

## Verification bindings

- S1 structure: `python3 scripts/native-spec/check_slice.py check --spec native-agent-docs/domain-run-driver/spec.json --planned-targets`.
- Design compile: user executes `./gradlew :plugin-core:classes` on the exact declaration basis.
- Focused tests: user executes `./gradlew :plugin-core:test --tests '*nativeagent.run.RunDomainTest' --tests '*nativeagent.run.NativeRunDriverTest' --tests '*nativeagent.run.DomainArchitectureTest'` after target discovery is enforced.
- Coroutine lifetime smoke: user executes `./gradlew :plugin-core:test --tests '*nativeagent.run.NativeRunDriverCoroutineSmokeTest.driverLifetimeIsBoundToParentScope'` separately.
- Prompt-cache gate: user executes `./gradlew :plugin-core:test --tests '*nativeagent.run.PromptCacheTest'`; the four tests include the pinned 20-dispatch benchmark.
- Affected build: user executes `./gradlew :plugin-core:build :plugin-core:buildPlugin`.

No test or build is marked passed until its runner reports nonzero discovered tests and terminal counts.
