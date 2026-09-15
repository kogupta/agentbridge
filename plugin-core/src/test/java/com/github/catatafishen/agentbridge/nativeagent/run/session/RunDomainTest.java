package com.github.catatafishen.agentbridge.nativeagent.run.session;

import com.github.catatafishen.agentbridge.nativeagent.lifecycle.Call;
import com.github.catatafishen.agentbridge.nativeagent.run.policy.RetryPolicy;
import com.github.catatafishen.agentbridge.nativeagent.run.policy.RunLimits;
import com.github.catatafishen.agentbridge.nativeagent.run.resources.RunResources;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RunDomainTest {
    private static final Instant START = Instant.parse("2026-09-14T00:00:00Z");
    private static final PlannedCall.ToolOperation OPERATION = new PlannedCall.ToolOperation() { };

    @Test
    void doubleSendDoesNotAppendOrDispatch() {
        RunSession session = new RunSession();
        RunSession.StartResult first = session.start(user("first"), RunLimits.mvp(), START, () -> START);
        assertInstanceOf(RunSession.StartResult.Started.class, first);

        RunSession.StartResult second = session.start(user("second"), RunLimits.mvp(), START, () -> START);
        RunSession.StartResult.Rejected rejected = assertInstanceOf(RunSession.StartResult.Rejected.class, second);
        assertEquals(com.github.catatafishen.agentbridge.nativeagent.lifecycle.RunLifecycle.StartRejection.BUSY,
            rejected.reason());
        assertEquals(List.of(user("first")), session.history().messages());
    }

    @Test
    void provisionalOutputIsNotAcceptedHistory() {
        RunSession session = new RunSession();
        RunSession.ActiveRun run = started(session);
        session.updateProvisional(run, "partial");

        assertEquals(List.of(user("request")), session.history().messages());
        assertEquals(new RunSession.ProvisionalObservation.Present("partial"), session.provisional());

        RunMessage.Assistant accepted = assistant("done", RunMessage.Completion.COMPLETE, List.of());
        assertInstanceOf(RunSession.TurnAcceptance.Ended.class,
            session.acceptTurn(run, new ValidatedAssistantTurn.Complete(accepted)));
        assertEquals(List.of(user("request"), accepted), session.history().messages());
        assertEquals(RunSession.ProvisionalObservation.Absent.INSTANCE, session.provisional());
    }

    @Test
    void reusedCallIdIsRejectedBeforeAssistantHistoryCommit() {
        RunSession session = new RunSession();
        RunSession.ActiveRun run = started(session);
        PlannedCall.Executable first = executable("same");
        RunMessage.Assistant turn = assistant("", RunMessage.Completion.COMPLETE, List.of(first));

        session.acceptTurn(run, new ValidatedAssistantTurn.Complete(turn));
        RunSession.CallStep.Execute call = assertInstanceOf(RunSession.CallStep.Execute.class, session.nextCall(run));
        assertInstanceOf(CallAdmission.Result.Executed.class, call.admission().execute(() -> { }));
        session.recordToolResult(run, new RunMessage.ToolResult(first.id(),
            new ToolOutcome.Completed("done", List.of("src/A.java"))));
        assertInstanceOf(RunSession.CallStep.Complete.class, session.nextCall(run));

        int historySize = session.history().messages().size();
        assertThrows(IllegalStateException.class,
            () -> session.acceptTurn(run, new ValidatedAssistantTurn.Complete(turn)));
        assertEquals(historySize, session.history().messages().size());
    }

    @Test
    void stopAfterAdmissionRetainsRealResultBeforeCancelledSuccessor() throws Exception {
        RunSession session = new RunSession();
        RunSession.ActiveRun run = started(session);
        PlannedCall.Executable first = executable("a");
        PlannedCall.Executable second = executable("b");
        session.acceptTurn(run, new ValidatedAssistantTurn.Complete(
            assistant("", RunMessage.Completion.COMPLETE, List.of(first, second))));
        RunSession.CallStep.Execute step = assertInstanceOf(RunSession.CallStep.Execute.class, session.nextCall(run));
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        var executor = Executors.newSingleThreadExecutor();
        try {
            var execution = executor.submit(() -> step.admission().execute(() -> {
                entered.countDown();
                await(release);
            }));
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            session.stop(run);
            assertEquals(2, session.history().messages().size(), "later cancellation waits for admitted result");
            release.countDown();
            assertInstanceOf(CallAdmission.Result.Executed.class, execution.get(5, TimeUnit.SECONDS));
            session.recordToolResult(run, new RunMessage.ToolResult(first.id(),
                new ToolOutcome.Completed("first completed", List.of("src/A.java"))));

            List<RunMessage> messages = session.history().messages();
            assertEquals(first.id(), ((RunMessage.ToolResult) messages.get(2)).callId());
            RunMessage.ToolResult cancelled = (RunMessage.ToolResult) messages.get(3);
            assertEquals(second.id(), cancelled.callId());
            assertEquals(ToolOutcome.Reason.CANCELLED_NOT_STARTED,
                ((ToolOutcome.NotStarted) cancelled.outcome()).reason());
            assertInstanceOf(RunSession.FinishResult.Finished.class, session.finish(run));
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void disposeCancelsAndDrainsPendingCallsInOrder() {
        RunSession session = new RunSession();
        RunSession.ActiveRun run = started(session);
        PlannedCall.Executable first = executable("a");
        PlannedCall.Executable second = executable("b");
        session.acceptTurn(run, new ValidatedAssistantTurn.Complete(
            assistant("", RunMessage.Completion.COMPLETE, List.of(first, second))));
        assertInstanceOf(RunSession.CallStep.Execute.class, session.nextCall(run));

        session.dispose();
        assertEquals(RunSession.Phase.STOPPING, session.phase());
        List<RunMessage> messages = session.history().messages();
        assertEquals(4, messages.size(), "dispose records one cancelled result per pending call");
        RunMessage.ToolResult firstResult = (RunMessage.ToolResult) messages.get(2);
        assertEquals(first.id(), firstResult.callId());
        assertEquals(ToolOutcome.Reason.CANCELLED_NOT_STARTED,
            ((ToolOutcome.NotStarted) firstResult.outcome()).reason());
        RunMessage.ToolResult secondResult = (RunMessage.ToolResult) messages.get(3);
        assertEquals(second.id(), secondResult.callId());
        assertEquals(ToolOutcome.Reason.CANCELLED_NOT_STARTED,
            ((ToolOutcome.NotStarted) secondResult.outcome()).reason());

        assertInstanceOf(RunSession.FinishResult.Finished.class, session.finish(run));
        assertEquals(RunSession.Phase.DISPOSED, session.phase());
    }

    @Test
    void runLimitsPermitBoundaryAndRejectNextWork() {
        RunLimits limits = new RunLimits(2, 2, Duration.ofMinutes(1));
        RunLimits.Budget budget = new RunLimits.Budget(limits, START);
        assertEquals(RunLimits.Admission.ADMITTED, budget.admitRequest(START));
        budget.recordAcceptedResponse();
        assertEquals(RunLimits.Admission.ADMITTED, budget.admitRequest(START.plusSeconds(10)));
        budget.recordAcceptedResponse();
        assertEquals(RunLimits.Admission.RESPONSE_LIMIT_REACHED, budget.admitRequest(START.plusSeconds(20)));
        assertEquals(RunLimits.Admission.ADMITTED, budget.admitTool(START));
        assertEquals(RunLimits.Admission.ADMITTED, budget.admitTool(START.plusSeconds(1)));
        assertEquals(RunLimits.Admission.TOOL_LIMIT_REACHED, budget.admitTool(START.plusSeconds(2)));
        assertEquals(RunLimits.Admission.DEADLINE_REACHED, new RunLimits.Budget(limits, START)
            .admitRequest(START.plus(Duration.ofMinutes(1))));
    }

    @Test
    void retryPolicyUsesExactBoundedRequestLocalDelay() {
        RetryPolicy policy = new RetryPolicy();
        assertEquals(new RetryPolicy.Decision.RetryAfter(Duration.ofSeconds(1)),
            policy.decide(failure(RetryPolicy.FailureKind.EOF, Optional.empty()), false));
        assertEquals(new RetryPolicy.Decision.RetryAfter(Duration.ZERO),
            policy.decide(failure(RetryPolicy.FailureKind.RATE_LIMIT, Optional.of(Duration.ZERO)), false));
        assertEquals(new RetryPolicy.Decision.RetryAfter(Duration.ofSeconds(10)),
            policy.decide(failure(RetryPolicy.FailureKind.RATE_LIMIT, Optional.of(Duration.ofSeconds(10))), false));
        assertInstanceOf(RetryPolicy.Decision.RateLimitWait.class,
            policy.decide(failure(RetryPolicy.FailureKind.RATE_LIMIT, Optional.of(Duration.ofSeconds(11))), false));
        assertInstanceOf(RetryPolicy.Decision.RateLimitWait.class,
            policy.decide(failure(RetryPolicy.FailureKind.RATE_LIMIT, Optional.of(Duration.ofSeconds(-1))), false));
        assertInstanceOf(RetryPolicy.Decision.NoRetry.class,
            policy.decide(failure(RetryPolicy.FailureKind.SERVER, Optional.empty()), true));
        assertInstanceOf(RetryPolicy.Decision.NoRetry.class,
            policy.decide(failure(RetryPolicy.FailureKind.BILLING_OR_QUOTA, Optional.empty()), false));
    }

    @Test
    void resourceRegistrationCancellationRaceClosesExactlyOnce() throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        try {
            for (int iteration = 0; iteration < 100; iteration++) {
                RunResources resources = new RunResources();
                AtomicInteger closes = new AtomicInteger();
                CyclicBarrier barrier = new CyclicBarrier(2);
                var registration = executor.submit(() -> {
                    await(barrier);
                    return resources.register(closes::incrementAndGet);
                });
                var cancellation = executor.submit(() -> {
                    await(barrier);
                    resources.cancel();
                });
                registration.get(5, TimeUnit.SECONDS);
                cancellation.get(5, TimeUnit.SECONDS);
                assertEquals(1, closes.get());
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private static RunSession.ActiveRun started(RunSession session) {
        return ((RunSession.StartResult.Started) session.start(user("request"), RunLimits.mvp(), START, () -> START)).run();
    }

    private static RunMessage.User user(String text) { return new RunMessage.User(text); }

    private static PlannedCall.Executable executable(String id) {
        return new PlannedCall.Executable(new Call.Id(id), new PlannedCall.ToolName("tool-" + id), OPERATION);
    }

    private static RunMessage.Assistant assistant(String text, RunMessage.Completion completion,
                                                  List<PlannedCall> calls) {
        return new RunMessage.Assistant(text, completion, calls);
    }

    private static RetryPolicy.Failure failure(RetryPolicy.FailureKind kind, Optional<Duration> delay) {
        return new RetryPolicy.Failure(kind, delay);
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) throw new AssertionError("Timed out waiting for latch");
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new AssertionError(error);
        }
    }

    private static void await(CyclicBarrier barrier) {
        try {
            barrier.await(5, TimeUnit.SECONDS);
        } catch (Exception error) {
            throw new AssertionError(error);
        }
    }
}
