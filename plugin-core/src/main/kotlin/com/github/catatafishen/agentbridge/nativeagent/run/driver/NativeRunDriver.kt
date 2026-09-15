package com.github.catatafishen.agentbridge.nativeagent.run.driver

import com.github.catatafishen.agentbridge.nativeagent.lifecycle.Call
import com.github.catatafishen.agentbridge.nativeagent.run.cache.CacheGeneration
import com.github.catatafishen.agentbridge.nativeagent.run.cache.CachePrefixViolation
import com.github.catatafishen.agentbridge.nativeagent.run.cache.ModelItemRenderer
import com.github.catatafishen.agentbridge.nativeagent.run.policy.RetryPolicy
import com.github.catatafishen.agentbridge.nativeagent.run.policy.RunLimits
import com.github.catatafishen.agentbridge.nativeagent.run.resources.RunCancellation
import com.github.catatafishen.agentbridge.nativeagent.run.session.CallAdmission
import com.github.catatafishen.agentbridge.nativeagent.run.session.PlannedCall
import com.github.catatafishen.agentbridge.nativeagent.run.session.RunMessage
import com.github.catatafishen.agentbridge.nativeagent.run.session.RunSession
import com.github.catatafishen.agentbridge.nativeagent.run.session.ToolOutcome
import com.github.catatafishen.agentbridge.nativeagent.run.session.ValidatedAssistantTurn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicReference

internal class NativeRunDriver(
    private val scope: CoroutineScope,
    private val session: RunSession,
    private val transport: ProviderTransport,
    private val tools: ToolRunner,
    private val clock: RunClock,
    private val cache: CacheGeneration,
    private val renderer: ModelItemRenderer,
    private val retryPolicy: RetryPolicy = RetryPolicy(),
) {
    private val active = AtomicReference<Active?>()

    fun submit(user: RunMessage.User, limits: RunLimits = RunLimits.mvp()): SubmitResult {
        val started = session.start(user, limits, clock.now(), clock)
        if (started is RunSession.StartResult.Rejected) return SubmitResult.Rejected(started.reason())
        val run = (started as RunSession.StartResult.Started).run()
        val holder = Active(run)
        val job = scope.launch(start = CoroutineStart.LAZY) { drive(holder) }
        holder.job = job
        active.set(holder)
        job.invokeOnCompletion {
            active.compareAndSet(holder, null)
            if (!holder.started && session.stopIfCurrent(run)) finishStopped(run)
        }
        job.start()
        return SubmitResult.Started(run)
    }

    fun stop(): StopRequestResult {
        val current = active.get() ?: return StopRequestResult.NoActiveRun
        return if (session.stopIfCurrent(current.run)) {
            StopRequestResult.Requested
        } else {
            StopRequestResult.NoActiveRun
        }
    }

    fun dispose() {
        session.dispose()
        active.get()?.job?.cancel(CancellationException("Session disposed"))
    }

    private suspend fun drive(activeRun: Active) {
        activeRun.started = true
        val run = activeRun.run
        val parentWatcher = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                awaitCancellation()
            } finally {
                session.stopIfCurrent(run)
            }
        }
        try {
            while (true) {
                if (run.cancellation().isCancelled) return finishStopped(run)
                when (session.beginRequest(run, clock.now())) {
                    RunLimits.Admission.ADMITTED -> Unit
                    else -> {
                        session.stop(run)
                        return finishStopped(run)
                    }
                }
                val history = session.history()
                val request = ProviderRequest(history, cache.requestFor(history, renderer))
                val turn = requestWithRetry(run, request)
                if (turn == null) {
                    session.stopIfCurrent(run)
                    return finishStopped(run)
                }
                when (session.acceptTurn(run, turn)) {
                    is RunSession.TurnAcceptance.Ended,
                    is RunSession.TurnAcceptance.ProtocolRejected -> return
                    is RunSession.TurnAcceptance.CallsReady -> if (!executeCalls(run)) return
                }
            }
        } catch (cancelled: CancellationException) {
            stopIfCurrent(run)
            finishStopped(run)
            throw cancelled
        } catch (failure: Throwable) {
            stopIfCurrent(run)
            finishStopped(run)
            throw failure
        } finally {
            parentWatcher.cancel()
        }
    }

    private suspend fun requestWithRetry(
        run: RunSession.ActiveRun,
        request: ProviderRequest,
    ): ValidatedAssistantTurn? {
        var retried = false
        while (true) {
            val attempt = transport.request(
                request,
                ProvisionalSink { text -> session.tryUpdateProvisional(run, text) },
                run.resources(),
            )
            when (attempt) {
                is ProviderAttempt.Terminal -> return attempt.turn
                is ProviderAttempt.Retryable -> when (val decision = retryPolicy.decide(attempt.failure, retried)) {
                    is RetryPolicy.Decision.RetryAfter -> {
                        session.clearProvisional(run)
                        retried = true
                        clock.delay(decision.delay())
                        if (run.cancellation().isCancelled) return null
                    }
                    is RetryPolicy.Decision.NoRetry,
                    is RetryPolicy.Decision.RateLimitWait -> return null
                }
                is ProviderAttempt.Failed,
                ProviderAttempt.Stopped -> return null
            }
        }
    }

    private suspend fun executeCalls(run: RunSession.ActiveRun): Boolean {
        while (true) {
            when (val step = session.nextCall(run)) {
                is RunSession.CallStep.Complete -> return true
                is RunSession.CallStep.Stopped -> {
                    finishStopped(run)
                    return false
                }
                is RunSession.CallStep.AccountRejected -> {
                    val admission = step.admission().execute {}
                    if (admission is CallAdmission.Result.Rejected) {
                        session.stopIfCurrent(run)
                        finishStopped(run)
                        return false
                    }
                    val reason = when (requireNotNull(step.call().error().code())) {
                        PlannedCall.CallError.Code.UNKNOWN_TOOL -> ToolOutcome.Reason.UNKNOWN_TOOL
                        PlannedCall.CallError.Code.INVALID_ARGUMENTS -> ToolOutcome.Reason.INVALID_ARGUMENTS
                    }
                    session.recordToolResult(
                        run,
                        RunMessage.ToolResult(step.call().id(), ToolOutcome.NotStarted(reason, step.call().error().message())),
                    )
                }
                is RunSession.CallStep.Execute -> {
                    val outcome = withContext(NonCancellable) { executeOne(run, step) } ?: continue
                    if (step.admission().status() == Call.Status.CANCELLED_BEFORE_START
                        && session.phase() == RunSession.Phase.STOPPING
                    ) {
                        finishStopped(run)
                        return false
                    }
                    session.recordToolResult(run, RunMessage.ToolResult(step.call().id(), outcome))
                    if (session.phase() == RunSession.Phase.STOPPING) {
                        finishStopped(run)
                        return false
                    }
                    if (outcome is ToolOutcome.FailedAfterStart) {
                        session.stop(run)
                        finishStopped(run)
                        return false
                    }
                    if (outcome is ToolOutcome.NotStarted &&
                        outcome.reason() == ToolOutcome.Reason.RUN_LIMIT_NOT_STARTED
                    ) {
                        session.stopForLimit(run)
                        finishStopped(run)
                        return false
                    }
                }
            }
        }
    }

    private suspend fun executeOne(
        run: RunSession.ActiveRun,
        step: RunSession.CallStep.Execute,
    ): ToolOutcome? {
        return try {
            tools.execute(step.call(), step.admission(), run.cancellation())
        } catch (failure: Throwable) {
            when (step.admission().status()) {
                Call.Status.COMPLETED,
                Call.Status.FAILED_AFTER_START -> ToolOutcome.FailedAfterStart(
                    "Tool execution failed after admission: ${failure.javaClass.simpleName}",
                    emptyList(),
                )
                Call.Status.CANCELLED_BEFORE_START -> null
                else -> throw failure
            }
        }
    }

    private fun stopIfCurrent(run: RunSession.ActiveRun) {
        session.stopIfCurrent(run)
    }

    private fun finishStopped(run: RunSession.ActiveRun) {
        if (session.phase() == RunSession.Phase.STOPPING) session.finish(run)
    }

    private class Active(val run: RunSession.ActiveRun) {
        @Volatile var job: Job? = null
        @Volatile var started = false
    }
}
