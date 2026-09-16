package com.github.catatafishen.agentbridge.nativeagent.run.driver

import com.github.catatafishen.agentbridge.nativeagent.model.ItfTrace
import com.github.catatafishen.agentbridge.nativeagent.run.session.ValidatedAssistantTurn
import com.github.catatafishen.agentbridge.nativeagent.run.cache.CacheField
import com.github.catatafishen.agentbridge.nativeagent.run.cache.CacheGeneration
import com.github.catatafishen.agentbridge.nativeagent.run.cache.ModelItemRenderer
import com.github.catatafishen.agentbridge.nativeagent.run.session.RunMessage
import com.github.catatafishen.agentbridge.nativeagent.run.session.RunSession
import com.github.catatafishen.agentbridge.nativeagent.run.session.ToolOutcome
import com.github.catatafishen.agentbridge.nativeagent.run.policy.RunLimits
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
class NativeRunDriverReplayTest {
    @Test
    fun replaysEverySelectedTraceThroughNativeRunDriver() = runBlocking {
        for (index in 0 until 20) {
            val trace = javaClass.getResourceAsStream(
                "/nativeagent/model/traces/agent_coarse_${index}.itf.json"
            )?.use { ItfTrace.read(it) } ?: error("missing trace resource $index")
            assertTrue(trace.states().drop(1).all { it.replayable() },
                "selected trace $index contains excluded read actions")
            val fixture = fixture(attempts(trace))
            try {
                assertTrue(fixture.driver.submit(user()) is SubmitResult.Started)
                withTimeout(5_000) { fixture.driver.awaitCompletion() }
                assertEquals(RunSession.Phase.IDLE, fixture.session.phase(),
                    "trace $index did not settle")
                assertTrue(fixture.requestHistorySizes.isNotEmpty(), "trace $index made no request")
            } finally {
                fixture.close()
            }
        }
    }

    private fun attempts(trace: ItfTrace): List<ProviderAttempt> {
        val attempts = trace.states().filter { it.action() == "providerAttempt" }.map { state ->
            val model = state.values()["s"] as Map<*, *>
            val calls = callIds(model).map(::executable)
            when (model["turn"]) {
                "TEXT" -> ProviderAttempt.Terminal(ValidatedAssistantTurn.Complete(assistant("text", emptyList())))
                "CALLS" -> ProviderAttempt.Terminal(ValidatedAssistantTurn.Complete(assistant("", calls)))
                "LENGTH_CALLS" -> ProviderAttempt.Terminal(
                    ValidatedAssistantTurn.LengthCalls(
                        RunMessage.Assistant("partial", RunMessage.Completion.INCOMPLETE, calls)
                    )
                )
                else -> ProviderAttempt.Stopped
            }
        }.toMutableList()
        repeat(5) { attempts += ProviderAttempt.Stopped }
        return attempts
    }

    private fun callIds(model: Map<*, *>): List<String> =
        listOf("turnId0", "turnId1", "turnId2").mapNotNull { key ->
            val wrapper = model[key] as? Map<*, *> ?: return@mapNotNull null
            val value = (wrapper["#bigint"] as? String)?.toBigIntegerOrNull() ?: return@mapNotNull null
            value.takeIf { it.signum() > 0 }?.toString()
        }

}
