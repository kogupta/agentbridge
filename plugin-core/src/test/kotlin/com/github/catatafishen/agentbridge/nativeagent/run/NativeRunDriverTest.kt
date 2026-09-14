package com.github.catatafishen.agentbridge.nativeagent.run

import com.github.catatafishen.agentbridge.nativeagent.lifecycle.Call
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.util.Collections
import java.util.Optional
import java.util.OptionalInt
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class NativeRunDriverTest {
    @Test
    fun malformedTerminalResponseRejectsWholeTurn() = runBlocking {
        val fixture = fixture(listOf(ProviderAttempt.Terminal(
            ValidatedAssistantTurn.Rejected(ValidatedAssistantTurn.ProtocolFailure.MALFORMED_JSON, "partial"),
        )))
        try {
            fixture.driver.submit(user())
            awaitPhase(fixture.session, RunSession.Phase.IDLE)
            assertEquals(listOf(user()), fixture.session.history().messages())
            assertEquals(0, fixture.effects.get())
        } finally {
            fixture.close()
        }
    }

    @Test
    fun executesValidatedCallsSequentiallyBeforeContinuation() = runBlocking {
        val calls = listOf(
            executable("a"),
            rejected("b", PlannedCall.CallError.Code.UNKNOWN_TOOL),
            rejected("c", PlannedCall.CallError.Code.INVALID_ARGUMENTS),
            executable("d"),
        )
        val fixture = fixture(listOf(
            ProviderAttempt.Terminal(ValidatedAssistantTurn.Complete(assistant("", calls))),
            ProviderAttempt.Terminal(ValidatedAssistantTurn.Complete(assistant("done", emptyList()))),
        ))
        try {
            fixture.driver.submit(user())
            awaitPhase(fixture.session, RunSession.Phase.IDLE)
            assertEquals(listOf("a", "d"), fixture.effectOrder)
            assertEquals(listOf(1, 6), fixture.requestHistorySizes)
            val results = fixture.session.history().messages().filterIsInstance<RunMessage.ToolResult>()
            assertEquals(listOf("a", "b", "c", "d"), results.map { it.callId().value() })
            assertEquals(ToolOutcome.Reason.UNKNOWN_TOOL, (results[1].outcome() as ToolOutcome.NotStarted).reason())
            assertEquals(ToolOutcome.Reason.INVALID_ARGUMENTS, (results[2].outcome() as ToolOutcome.NotStarted).reason())
        } finally {
            fixture.close()
        }
    }

    @Test
    fun appliesLengthOutcomeMatrix() = runBlocking {
        val textOnly = fixture(listOf(ProviderAttempt.Terminal(
            ValidatedAssistantTurn.LengthText(RunMessage.Assistant("partial", RunMessage.Completion.INCOMPLETE, emptyList())),
        )))
        try {
            textOnly.driver.submit(user())
            awaitPhase(textOnly.session, RunSession.Phase.IDLE)
            assertEquals(0, textOnly.effects.get())
            assertEquals(RunMessage.Completion.INCOMPLETE,
                textOnly.session.history().messages().filterIsInstance<RunMessage.Assistant>().single().completion())
        } finally {
            textOnly.close()
        }

        val withCalls = fixture(listOf(ProviderAttempt.Terminal(
            ValidatedAssistantTurn.LengthCalls(
                RunMessage.Assistant("partial", RunMessage.Completion.INCOMPLETE, listOf(executable("a"), executable("b"))),
            ),
        )))
        try {
            withCalls.driver.submit(user())
            awaitPhase(withCalls.session, RunSession.Phase.IDLE)
            assertEquals(0, withCalls.effects.get())
            val outcomes = withCalls.session.history().messages().filterIsInstance<RunMessage.ToolResult>()
                .map { (it.outcome() as ToolOutcome.NotStarted).reason() }
            assertEquals(listOf(ToolOutcome.Reason.TRUNCATED_NOT_EXECUTED, ToolOutcome.Reason.TRUNCATED_NOT_EXECUTED), outcomes)
        } finally {
            withCalls.close()
        }

        val malformed = fixture(listOf(ProviderAttempt.Terminal(
            ValidatedAssistantTurn.Rejected(ValidatedAssistantTurn.ProtocolFailure.MALFORMED_CALL_FRAGMENT, "partial"),
        )))
        try {
            malformed.driver.submit(user())
            awaitPhase(malformed.session, RunSession.Phase.IDLE)
            assertEquals(listOf(user()), malformed.session.history().messages())
        } finally {
            malformed.close()
        }
    }

    @Test
    fun stopDuringRequestDiscardsProvisionalOutput() = runBlocking {
        val requestStarted = CompletableDeferred<Unit>()
        val response = CompletableDeferred<ProviderAttempt>()
        val closed = AtomicInteger()
        val session = RunSession()
        val clock = FakeClock()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val transport = ProviderTransport { _, sink, resources ->
            sink.emit("partial")
            resources.register {
                closed.incrementAndGet()
                response.complete(ProviderAttempt.Stopped)
            }
            requestStarted.complete(Unit)
            response.await()
        }
        val driver = NativeRunDriver(scope, session, transport, immediateTools(), clock, cache(), renderer())
        try {
            driver.submit(user())
            withTimeout(5_000) { requestStarted.await() }
            assertEquals(StopRequestResult.Requested, driver.stop())
            awaitPhase(session, RunSession.Phase.IDLE)
            assertEquals(1, closed.get())
            assertEquals(RunSession.ProvisionalObservation.Absent.INSTANCE, session.provisional())
            assertEquals(listOf(user()), session.history().messages())
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun stopBeforeAdmissionAccountsEveryCall() = runBlocking {
        val queued = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val effects = AtomicInteger()
        val runner = ToolRunner { _, admission, cancellation ->
            queued.complete(Unit)
            cancellation.onCancel { release.complete(Unit) }
            release.await()
            mapAdmission(admission, effects)
        }
        val fixture = fixture(listOf(ProviderAttempt.Terminal(
            ValidatedAssistantTurn.Complete(assistant("", listOf(executable("a"), executable("b")))),
        )), runner)
        try {
            fixture.driver.submit(user())
            withTimeout(5_000) { queued.await() }
            fixture.driver.stop()
            awaitPhase(fixture.session, RunSession.Phase.IDLE)
            assertEquals(0, effects.get())
            val reasons = fixture.session.history().messages().filterIsInstance<RunMessage.ToolResult>()
                .map { (it.outcome() as ToolOutcome.NotStarted).reason() }
            assertEquals(listOf(ToolOutcome.Reason.CANCELLED_NOT_STARTED, ToolOutcome.Reason.CANCELLED_NOT_STARTED), reasons)
            assertEquals(1, fixture.requestHistorySizes.size)
        } finally {
            fixture.close()
        }
    }

    @Test
    fun lateAdmittedResultIsRetainedBeforeIdle() = runBlocking {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val runner = ToolRunner { _, admission, _ ->
            var outcome: ToolOutcome = ToolOutcome.Completed("late", listOf("src/A.java"))
            when (admission.execute {
                entered.countDown()
                assertTrue(release.await(5, TimeUnit.SECONDS))
            }) {
                is CallAdmission.Result.Executed -> outcome
                is CallAdmission.Result.Limited -> ToolOutcome.NotStarted(ToolOutcome.Reason.RUN_LIMIT_NOT_STARTED, "limit")
                is CallAdmission.Result.Rejected -> ToolOutcome.NotStarted(ToolOutcome.Reason.CANCELLED_NOT_STARTED, "stopped")
            }
        }
        val fixture = fixture(listOf(ProviderAttempt.Terminal(
            ValidatedAssistantTurn.Complete(assistant("", listOf(executable("a"), executable("b")))),
        )), runner)
        try {
            fixture.driver.submit(user())
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            fixture.driver.stop()
            assertEquals(RunSession.Phase.STOPPING, fixture.session.phase())
            release.countDown()
            awaitPhase(fixture.session, RunSession.Phase.IDLE)
            val results = fixture.session.history().messages().filterIsInstance<RunMessage.ToolResult>()
            assertInstanceOf(ToolOutcome.Completed::class.java, results[0].outcome())
            assertEquals(ToolOutcome.Reason.CANCELLED_NOT_STARTED, (results[1].outcome() as ToolOutcome.NotStarted).reason())
            assertEquals(1, fixture.requestHistorySizes.size)
        } finally {
            release.countDown()
            fixture.close()
        }
    }

    @Test
    fun failedStartedEffectIsNotReplayed() = runBlocking {
        val runner = ToolRunner { _, admission, _ ->
            when (admission.execute {}) {
                is CallAdmission.Result.Executed -> ToolOutcome.FailedAfterStart("partial failure", listOf("src/A.java"))
                is CallAdmission.Result.Limited -> ToolOutcome.NotStarted(ToolOutcome.Reason.RUN_LIMIT_NOT_STARTED, "limit")
                is CallAdmission.Result.Rejected -> ToolOutcome.NotStarted(ToolOutcome.Reason.CANCELLED_NOT_STARTED, "stopped")
            }
        }
        val fixture = fixture(listOf(ProviderAttempt.Terminal(
            ValidatedAssistantTurn.Complete(assistant("", listOf(executable("a"), executable("b")))),
        )), runner)
        try {
            fixture.driver.submit(user())
            awaitPhase(fixture.session, RunSession.Phase.IDLE)
            val results = fixture.session.history().messages().filterIsInstance<RunMessage.ToolResult>()
            assertInstanceOf(ToolOutcome.FailedAfterStart::class.java, results[0].outcome())
            assertEquals(ToolOutcome.Reason.CANCELLED_NOT_STARTED, (results[1].outcome() as ToolOutcome.NotStarted).reason())
            assertEquals(1, fixture.requestHistorySizes.size)
        } finally {
            fixture.close()
        }
    }

    @Test
    fun retryIsRequestLocalAndBounded() = runBlocking {
        val retry = ProviderAttempt.Retryable(RetryPolicy.Failure(RetryPolicy.FailureKind.EOF, Optional.empty()))
        val fixture = fixture(listOf(
            retry,
            ProviderAttempt.Terminal(ValidatedAssistantTurn.Complete(assistant("done", emptyList()))),
        ))
        try {
            fixture.driver.submit(user())
            awaitPhase(fixture.session, RunSession.Phase.IDLE)
            assertEquals(listOf(Duration.ofSeconds(1)), fixture.clock.delays)
            assertEquals(listOf(1, 1), fixture.requestHistorySizes)
            assertEquals(2, fixture.session.history().messages().size)
            assertEquals(0, fixture.effects.get())
        } finally {
            fixture.close()
        }
    }

    @Test
    fun enforcesRunLimitsBeforeNewWork() = runBlocking {
        val fixture = fixture(listOf(ProviderAttempt.Terminal(
            ValidatedAssistantTurn.Complete(assistant("", listOf(executable("a"), executable("b")))),
        )))
        try {
            fixture.driver.submit(user(), RunLimits(20, 1, Duration.ofMinutes(15)))
            awaitPhase(fixture.session, RunSession.Phase.IDLE)
            assertEquals(1, fixture.effects.get())
            val results = fixture.session.history().messages().filterIsInstance<RunMessage.ToolResult>()
            assertInstanceOf(ToolOutcome.Completed::class.java, results[0].outcome())
            assertEquals(ToolOutcome.Reason.RUN_LIMIT_NOT_STARTED, (results[1].outcome() as ToolOutcome.NotStarted).reason())
            assertEquals(1, fixture.requestHistorySizes.size)
        } finally {
            fixture.close()
        }
    }

    @Test
    fun resourceRegistrationCancellationRaceRetainsLateResult() = runBlocking {
        val executor = Executors.newFixedThreadPool(2)
        try {
            repeat(100) {
                val resources = RunResources()
                val closes = AtomicInteger()
                val barrier = CyclicBarrier(2)
                val registration = executor.submit<RunResources.Registration> {
                    barrier.await(5, TimeUnit.SECONDS)
                    resources.register { closes.incrementAndGet() }
                }
                val cancellation = executor.submit {
                    barrier.await(5, TimeUnit.SECONDS)
                    resources.cancel()
                }
                registration.get(5, TimeUnit.SECONDS)
                cancellation.get(5, TimeUnit.SECONDS)
                assertEquals(1, closes.get())
            }
        } finally {
            executor.shutdownNow()
        }
        lateAdmittedResultIsRetainedBeforeIdle()
    }

    @Test
    fun prefixViolationStopsBeforeTransportAndSettlesIdle() = runBlocking {
        val propagated = CompletableDeferred<Throwable>()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default +
            CoroutineExceptionHandler { _, failure -> propagated.complete(failure) })
        val session = RunSession()
        val transportCalls = AtomicInteger()
        val transport = ProviderTransport { _, _, _ ->
            transportCalls.incrementAndGet()
            ProviderAttempt.Stopped
        }
        val generation = CacheGeneration.Id("violation")
        val divergentRenderer = ModelItemRenderer { _, index ->
            CachePrefixGuard().verify(
                CacheRequest(generation, listOf(CacheRequest.Part(
                    CacheRequest.Component.INPUT, CacheField("item", "|a".toByteArray()), OptionalInt.of(index)))),
                CacheRequest(generation, listOf(CacheRequest.Part(
                    CacheRequest.Component.INPUT, CacheField("item", "|b".toByteArray()), OptionalInt.of(index)))),
            )
            error("Divergent cache requests must violate the prefix")
        }
        val driver = NativeRunDriver(scope, session, transport, immediateTools(), FakeClock(), cache(), divergentRenderer)
        try {
            driver.submit(user())
            val failure = withTimeout(5_000) { propagated.await() }
            assertInstanceOf(CachePrefixViolation::class.java, failure)
            assertEquals(RunSession.Phase.IDLE, session.phase())
            assertEquals(0, transportCalls.get())
            assertEquals(listOf(user()), session.history().messages())
            assertEquals(StopRequestResult.NoActiveRun, driver.stop())
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun submitIntoCancelledScopeSettlesIdle() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scope.cancel()
        val session = RunSession()
        val transportCalls = AtomicInteger()
        val transport = ProviderTransport { _, _, _ ->
            transportCalls.incrementAndGet()
            ProviderAttempt.Stopped
        }
        val driver = NativeRunDriver(scope, session, transport, immediateTools(), FakeClock(), cache(), renderer())

        assertInstanceOf(SubmitResult.Started::class.java, driver.submit(user()))
        awaitPhase(session, RunSession.Phase.IDLE)
        assertEquals(StopRequestResult.NoActiveRun, driver.stop())
        assertInstanceOf(SubmitResult.Started::class.java, driver.submit(RunMessage.User("again")))
        awaitPhase(session, RunSession.Phase.IDLE)
        assertEquals(0, transportCalls.get())
    }

    private fun fixture(attempts: List<ProviderAttempt>, runner: ToolRunner? = null): Fixture {
        val session = RunSession()
        val clock = FakeClock()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val queue = java.util.concurrent.ConcurrentLinkedQueue(attempts)
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

    private fun immediateTools() = ToolRunner { _, admission, _ -> mapAdmission(admission, AtomicInteger()) }

    private fun mapAdmission(admission: CallAdmission, effects: AtomicInteger): ToolOutcome = when (admission.execute {
        effects.incrementAndGet()
    }) {
        is CallAdmission.Result.Executed -> ToolOutcome.Completed("ok", emptyList())
        is CallAdmission.Result.Limited -> ToolOutcome.NotStarted(ToolOutcome.Reason.RUN_LIMIT_NOT_STARTED, "limit")
        is CallAdmission.Result.Rejected -> ToolOutcome.NotStarted(ToolOutcome.Reason.CANCELLED_NOT_STARTED, "stopped")
    }

    private suspend fun awaitPhase(session: RunSession, expected: RunSession.Phase) {
        withTimeout(5_000) {
            while (session.phase() != expected) {
                yield()
                delay(1)
            }
        }
    }

    private fun user() = RunMessage.User("request")

    private fun executable(id: String) = PlannedCall.Executable(
        Call.Id(id),
        PlannedCall.ToolName("tool-$id"),
        object : PlannedCall.ToolOperation {},
    )

    private fun rejected(id: String, code: PlannedCall.CallError.Code) = PlannedCall.Rejected(
        Call.Id(id),
        PlannedCall.CallError(code, "rejected-$id"),
    )

    private fun assistant(text: String, calls: List<PlannedCall>) =
        RunMessage.Assistant(text, RunMessage.Completion.COMPLETE, calls)

    private fun cache() = CacheGeneration.initial(
        CacheGeneration.Id("test-generation"),
        listOf(CacheField("head", "stable-head".toByteArray())),
    )

    private fun renderer() = ModelItemRenderer { message, index ->
        listOf(CacheField("input", "|$index:$message".toByteArray()))
    }

    private data class Fixture(
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

    private class FakeClock : RunClock {
        private var instant = Instant.parse("2026-09-14T00:00:00Z")
        val delays = Collections.synchronizedList(mutableListOf<Duration>())
        override fun now(): Instant = instant
        override suspend fun delay(duration: Duration) {
            delays += duration
            instant = instant.plus(duration)
        }
    }
}
