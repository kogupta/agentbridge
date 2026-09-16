package com.github.catatafishen.agentbridge.nativeagent.run.driver

import com.github.catatafishen.agentbridge.nativeagent.lifecycle.Call
import com.github.catatafishen.agentbridge.nativeagent.run.cache.CacheField
import com.github.catatafishen.agentbridge.nativeagent.run.cache.CacheGeneration
import com.github.catatafishen.agentbridge.nativeagent.run.cache.ModelItemRenderer
import com.github.catatafishen.agentbridge.nativeagent.run.session.CallAdmission
import com.github.catatafishen.agentbridge.nativeagent.run.session.PlannedCall
import com.github.catatafishen.agentbridge.nativeagent.run.session.RunMessage
import com.github.catatafishen.agentbridge.nativeagent.run.session.RunSession
import com.github.catatafishen.agentbridge.nativeagent.run.session.ToolOutcome
import com.github.catatafishen.agentbridge.nativeagent.run.session.ValidatedAssistantTurn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import kotlinx.coroutines.withTimeout
import java.time.Duration
import java.time.Instant
import java.util.Collections
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

internal fun fixture(attempts: List<ProviderAttempt>, runner: ToolRunner? = null): Fixture {
    val session = RunSession()
    val clock = FakeClock()
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val queue = ConcurrentLinkedQueue(attempts)
    val historySizes = Collections.synchronizedList(mutableListOf<Int>())
    val transport = ProviderTransport { request, _, _ ->
        historySizes += request.history.messages().size
        queue.poll() ?: error("Unexpected provider request")
    }
    val effects = AtomicInteger()
    val order = Collections.synchronizedList(mutableListOf<String>())
    val selectedRunner = runner ?: ToolRunner { call, admission, _ ->
        val result = admission.execute {
            effects.incrementAndGet()
            order += call.id().value()
        }
        when (result) {
            is CallAdmission.Result.Executed -> ToolOutcome.Completed("ok", emptyList())
            is CallAdmission.Result.Limited -> ToolOutcome.NotStarted(ToolOutcome.Reason.RUN_LIMIT_NOT_STARTED, "limit")
            is CallAdmission.Result.Rejected -> ToolOutcome.NotStarted(ToolOutcome.Reason.CANCELLED_NOT_STARTED, "stopped")
        }
    }
    return Fixture(session,
        NativeRunDriver(scope, session, transport, selectedRunner, clock, cache(), renderer()), scope,
        clock, effects, order, historySizes)
}

internal fun immediateTools() = ToolRunner { _, admission, _ -> mapAdmission(admission, AtomicInteger()) }

internal fun mapAdmission(admission: CallAdmission, effects: AtomicInteger): ToolOutcome = when (admission.execute {
    effects.incrementAndGet()
}) {
    is CallAdmission.Result.Executed -> ToolOutcome.Completed("ok", emptyList())
    is CallAdmission.Result.Limited -> ToolOutcome.NotStarted(ToolOutcome.Reason.RUN_LIMIT_NOT_STARTED, "limit")
    is CallAdmission.Result.Rejected -> ToolOutcome.NotStarted(ToolOutcome.Reason.CANCELLED_NOT_STARTED, "stopped")
}

internal suspend fun awaitPhase(session: RunSession, expected: RunSession.Phase) {
    withTimeout(5_000) {
        while (session.phase() != expected) {
            yield()
            delay(1)
        }
    }
}

internal fun user() = RunMessage.User("request")

internal fun executable(id: String) = PlannedCall.Executable(
    Call.Id(id), PlannedCall.ToolName("tool-$id"), object : PlannedCall.ToolOperation {},
)

internal fun rejected(id: String, code: PlannedCall.CallError.Code) = PlannedCall.Rejected(
    Call.Id(id), PlannedCall.CallError(code, "rejected-$id"),
)

internal fun assistant(text: String, calls: List<PlannedCall>) =
    RunMessage.Assistant(text, RunMessage.Completion.COMPLETE, calls)

internal fun cache() = CacheGeneration.initial(
    CacheGeneration.Id("test-generation"), listOf(CacheField("head", "stable-head".toByteArray())),
)

internal fun renderer() = ModelItemRenderer { message, index ->
    listOf(CacheField("input", "|$index:$message".toByteArray()))
}

internal data class Fixture(
    val session: RunSession,
    val driver: NativeRunDriver,
    val scope: CoroutineScope,
    val clock: FakeClock,
    val effects: AtomicInteger,
    val effectOrder: List<String>,
    val requestHistorySizes: List<Int>,
) {
    fun close() = scope.cancel()
}

internal class FakeClock : RunClock {
    private var instant = Instant.parse("2026-09-14T00:00:00Z")
    val delays = Collections.synchronizedList(mutableListOf<Duration>())
    override fun now(): Instant = instant
    override suspend fun delay(duration: Duration) {
        delays += duration
        instant = instant.plus(duration)
    }
}
