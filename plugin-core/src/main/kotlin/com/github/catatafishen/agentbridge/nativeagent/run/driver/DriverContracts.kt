package com.github.catatafishen.agentbridge.nativeagent.run.driver

import com.github.catatafishen.agentbridge.nativeagent.run.cache.CacheRequest
import com.github.catatafishen.agentbridge.nativeagent.run.policy.RetryPolicy
import com.github.catatafishen.agentbridge.nativeagent.run.resources.RunCancellation
import com.github.catatafishen.agentbridge.nativeagent.run.resources.RunResources
import com.github.catatafishen.agentbridge.nativeagent.run.resources.RunTimeSource
import com.github.catatafishen.agentbridge.nativeagent.run.session.CallAdmission
import com.github.catatafishen.agentbridge.nativeagent.run.session.PlannedCall
import com.github.catatafishen.agentbridge.nativeagent.run.session.RunSession
import com.github.catatafishen.agentbridge.nativeagent.run.session.ToolOutcome
import com.github.catatafishen.agentbridge.nativeagent.run.session.ValidatedAssistantTurn
import java.time.Duration
import java.time.Instant

internal data class ProviderRequest(
    val history: RunSession.HistorySnapshot,
    val cache: CacheRequest,
)

internal fun interface ProvisionalSink {
    fun emit(text: String)
}

internal fun interface ProviderTransport {
    suspend fun request(
        request: ProviderRequest,
        sink: ProvisionalSink,
        resources: RunResources,
    ): ProviderAttempt
}

internal sealed interface ProviderAttempt {
    data class Terminal(val turn: ValidatedAssistantTurn) : ProviderAttempt
    data class Retryable(val failure: RetryPolicy.Failure) : ProviderAttempt
    data class Failed(val failure: ProviderFailure) : ProviderAttempt
    data object Stopped : ProviderAttempt
}

internal enum class ProviderFailure {
    AUTHENTICATION_REQUIRED,
    CONFIGURATION,
    PROTOCOL,
    CONTEXT_FULL,
    TRANSPORT,
}

internal fun interface ToolRunner {
    suspend fun execute(
        call: PlannedCall.Executable,
        admission: CallAdmission,
        cancellation: RunCancellation,
    ): ToolOutcome
}

internal interface RunClock : RunTimeSource {
    override fun now(): Instant
    suspend fun delay(duration: Duration)
}

internal sealed interface SubmitResult {
    data class Started(val run: RunSession.ActiveRun) : SubmitResult
    data class Rejected(val reason: com.github.catatafishen.agentbridge.nativeagent.lifecycle.RunLifecycle.StartRejection) : SubmitResult
}

internal sealed interface StopRequestResult {
    data object NoActiveRun : StopRequestResult
    data object Requested : StopRequestResult
}
