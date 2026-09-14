package com.github.catatafishen.agentbridge.nativeagent.run

import com.github.catatafishen.agentbridge.nativeagent.lifecycle.Call
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class NativeRunDriverCoroutineSmokeTest {
    @Test
    fun driverLifetimeIsBoundToParentScope() = runBlocking {
        cancellationDuringRequestClosesResource()
        cancellationAfterAdmissionRetainsResult()
    }

    private suspend fun cancellationDuringRequestClosesResource() {
        val parent = SupervisorJob()
        val scope = CoroutineScope(parent + Dispatchers.Default)
        val session = RunSession()
        val started = CompletableDeferred<Unit>()
        val response = CompletableDeferred<ProviderAttempt>()
        val closes = AtomicInteger()
        val transport = ProviderTransport { _, sink, resources ->
            sink.emit("partial")
            resources.register {
                closes.incrementAndGet()
                response.complete(ProviderAttempt.Stopped)
            }
            started.complete(Unit)
            response.await()
        }
        NativeRunDriver(scope, session, transport, noTools(), FakeClock(), cache(), renderer())
            .submit(RunMessage.User("request"))
        withTimeout(5_000) { started.await() }
        parent.cancel()
        awaitPhase(session, RunSession.Phase.IDLE)
        assertEquals(1, closes.get())
        assertEquals(listOf(RunMessage.User("request")), session.history().messages())
        assertFalse(parent.children.iterator().hasNext())
    }

    private suspend fun cancellationAfterAdmissionRetainsResult() {
        val parent = SupervisorJob()
        val scope = CoroutineScope(parent + Dispatchers.Default)
        val session = RunSession()
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val call = PlannedCall.Executable(
            Call.Id("a"), PlannedCall.ToolName("tool"), object : PlannedCall.ToolOperation {},
        )
        val transport = ProviderTransport { _, _, _ ->
            ProviderAttempt.Terminal(ValidatedAssistantTurn.Complete(
                RunMessage.Assistant("", RunMessage.Completion.COMPLETE, listOf(call)),
            ))
        }
        val tools = ToolRunner { _, admission, _ ->
            when (admission.execute {
                entered.countDown()
                assertTrue(release.await(5, TimeUnit.SECONDS))
            }) {
                is CallAdmission.Result.Executed -> ToolOutcome.Completed("late", listOf("src/A.java"))
                is CallAdmission.Result.Limited -> ToolOutcome.NotStarted(ToolOutcome.Reason.RUN_LIMIT_NOT_STARTED, "limit")
                is CallAdmission.Result.Rejected -> ToolOutcome.NotStarted(ToolOutcome.Reason.CANCELLED_NOT_STARTED, "stopped")
            }
        }
        NativeRunDriver(scope, session, transport, tools, FakeClock(), cache(), renderer())
            .submit(RunMessage.User("request"))
        assertTrue(entered.await(5, TimeUnit.SECONDS))
        parent.cancel()
        release.countDown()
        awaitPhase(session, RunSession.Phase.IDLE)
        val result = session.history().messages().filterIsInstance<RunMessage.ToolResult>().single()
        assertInstanceOf(ToolOutcome.Completed::class.java, result.outcome())
        assertFalse(parent.children.iterator().hasNext())
    }

    private fun noTools() = ToolRunner { _, admission, _ ->
        when (admission.execute {}) {
            is CallAdmission.Result.Executed -> ToolOutcome.Completed("ok", emptyList())
            is CallAdmission.Result.Limited -> ToolOutcome.NotStarted(ToolOutcome.Reason.RUN_LIMIT_NOT_STARTED, "limit")
            is CallAdmission.Result.Rejected -> ToolOutcome.NotStarted(ToolOutcome.Reason.CANCELLED_NOT_STARTED, "stopped")
        }
    }

    private fun cache() = CacheGeneration.initial(
        CacheGeneration.Id("smoke-generation"),
        listOf(CacheField("head", "stable-head".toByteArray())),
    )

    private fun renderer() = ModelItemRenderer { message, index ->
        listOf(CacheField("input", "|$index:$message".toByteArray()))
    }

    private suspend fun awaitPhase(session: RunSession, expected: RunSession.Phase) {
        withTimeout(5_000) {
            while (session.phase() != expected) yield()
        }
    }

    private class FakeClock : RunClock {
        override fun now(): Instant = Instant.parse("2026-09-14T00:00:00Z")
        override suspend fun delay(duration: Duration) { }
    }
}
