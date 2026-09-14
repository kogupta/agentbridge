package com.github.catatafishen.agentbridge.nativeagent.lifecycle;

import org.junit.jupiter.api.Test;

import java.awt.EventQueue;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RunLifecycleTest {
    @Test
    void singleRunOwnership() {
        RunLifecycle lifecycle = new RunLifecycle();
        RunHandle first = startedRun(lifecycle);

        RunLifecycle.StartRunResult busy = lifecycle.startRun();
        assertEquals(new RunLifecycle.StartRunResult.Rejected(RunLifecycle.StartRejection.BUSY), busy);
        assertInstanceOf(RunLifecycle.FinishRunResult.Finished.class, lifecycle.finishRun(first));
        assertInstanceOf(RunLifecycle.StartRunResult.Started.class, lifecycle.startRun());
    }

    @Test
    void validatedImmutableBatch() {
        CallId first = new CallId("first");
        CallId duplicate = new CallId("duplicate");
        assertThrows(IllegalArgumentException.class, () -> CallBatch.of(List.of()));
        assertThrows(NullPointerException.class, () -> CallBatch.of(null));
        assertThrows(IllegalArgumentException.class, () -> CallBatch.of(List.of(duplicate, duplicate)));
        assertThrows(NullPointerException.class, () -> CallBatch.of(Arrays.asList(first, null)));

        var input = new java.util.ArrayList<>(List.of(first));
        CallBatch batch = CallBatch.of(input);
        input.clear();
        assertEquals(List.of(first), batch.calls());
        assertThrows(UnsupportedOperationException.class, () -> batch.calls().clear());
    }

    @Test
    void orderedExclusiveExecutionAndDuplicateAdmission() throws Exception {
        RunLifecycle lifecycle = new RunLifecycle();
        RunHandle run = startedRun(lifecycle);
        CallId first = new CallId("first");
        CallId second = new CallId("second");
        BatchHandle batch = begunBatch(lifecycle, run, first, second);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger firstEntries = new AtomicInteger();

        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<RunLifecycle.ExecutionResult> firstExecution = executor.submit(
                () -> lifecycle.execute(batch, first, () -> {
                    firstEntries.incrementAndGet();
                    entered.countDown();
                    await(release);
                }));
            assertTrue(entered.await(5, TimeUnit.SECONDS));

            RunLifecycle.ExecutionResult duplicate = lifecycle.execute(batch, first, firstEntries::incrementAndGet);
            RunLifecycle.ExecutionResult outOfOrder = lifecycle.execute(batch, second, firstEntries::incrementAndGet);
            assertEquals(new RunLifecycle.ExecutionResult.Rejected(RunLifecycle.ExecutionRejection.ALREADY_EXECUTING), duplicate);
            assertEquals(new RunLifecycle.ExecutionResult.Rejected(RunLifecycle.ExecutionRejection.OUT_OF_ORDER), outOfOrder);

            release.countDown();
            assertInstanceOf(RunLifecycle.ExecutionResult.Executed.class, firstExecution.get(5, TimeUnit.SECONDS));
            assertInstanceOf(RunLifecycle.ExecutionResult.Executed.class,
                lifecycle.execute(batch, second, firstEntries::incrementAndGet));
            assertEquals(2, firstEntries.get());
            assertEquals(new RunLifecycle.ExecutionResult.Rejected(RunLifecycle.ExecutionRejection.ALREADY_TERMINAL),
                lifecycle.execute(batch, first, firstEntries::incrementAndGet));
        }
    }

    @Test
    void stopBeforeQueuedEffect() throws Exception {
        RunLifecycle lifecycle = new RunLifecycle();
        RunHandle run = startedRun(lifecycle);
        CallId call = new CallId("queued");
        BatchHandle batch = begunBatch(lifecycle, run, call);
        CountDownLatch queueGate = new CountDownLatch(1);
        AtomicInteger entries = new AtomicInteger();

        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            executor.submit(() -> await(queueGate));
            Future<RunLifecycle.ExecutionResult> queued = executor.submit(
                () -> lifecycle.execute(batch, call, entries::incrementAndGet));
            RunLifecycle.StopResult stopped = lifecycle.stop(run);
            queueGate.countDown();
            assertEquals(0, entries.get());
            assertEquals(LifecyclePhase.STOPPING, stoppedPhase(stopped));
            assertEquals(new RunLifecycle.ExecutionResult.Rejected(RunLifecycle.ExecutionRejection.RUN_NOT_ACCEPTING_EFFECTS),
                queued.get(5, TimeUnit.SECONDS));
            assertEquals(CallStatus.CANCELLED_BEFORE_START, batchStatus(lifecycle, batch, call));
        }
    }

    @Test
    void stopDuringEffect() throws Exception {
        RunLifecycle lifecycle = new RunLifecycle();
        RunHandle run = startedRun(lifecycle);
        CallId first = new CallId("first");
        CallId second = new CallId("second");
        BatchHandle batch = begunBatch(lifecycle, run, first, second);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<RunLifecycle.ExecutionResult> executing = executor.submit(
                () -> lifecycle.execute(batch, first, () -> {
                    entered.countDown();
                    await(release);
                }));
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            assertInstanceOf(RunLifecycle.StopResult.Acknowledged.class, lifecycle.stop(run));
            assertEquals(new RunLifecycle.StartRunResult.Rejected(RunLifecycle.StartRejection.BUSY), lifecycle.startRun());
            assertEquals(new RunLifecycle.FinishRunResult.Rejected(RunLifecycle.FinishRejection.BATCH_UNSETTLED), lifecycle.finishRun(run));
            release.countDown();
            assertInstanceOf(RunLifecycle.ExecutionResult.Executed.class, executing.get(5, TimeUnit.SECONDS));
            assertEquals(CallStatus.COMPLETED, batchStatus(lifecycle, batch, first));
            assertEquals(CallStatus.CANCELLED_BEFORE_START, batchStatus(lifecycle, batch, second));
            assertInstanceOf(RunLifecycle.FinishRunResult.Finished.class, lifecycle.finishRun(run));
        }
    }

    @Test
    void terminalAccounting() {
        assertTerminalStatus(() -> { });
        RuntimeException runtime = new RuntimeException("runtime");
        assertTerminalStatus(CallStatus.FAILED_AFTER_START, () -> { throw runtime; }, runtime);
        AssertionError error = new AssertionError("error");
        assertTerminalStatus(CallStatus.FAILED_AFTER_START, () -> { throw error; }, error);
    }

    @Test
    void staleHandleIsolation() {
        RunLifecycle firstLifecycle = new RunLifecycle();
        RunLifecycle secondLifecycle = new RunLifecycle();
        RunHandle firstRun = startedRun(firstLifecycle);
        RunHandle secondRun = startedRun(secondLifecycle);
        CallId firstCall = new CallId("first");
        BatchHandle firstBatch = begunBatch(firstLifecycle, firstRun, firstCall);

        assertEquals(new RunLifecycle.BeginBatchResult.Rejected(RunLifecycle.BatchRejection.STALE_RUN),
            secondLifecycle.beginBatch(firstRun, CallBatch.of(List.of(new CallId("foreign")))));
        lifecycleExecute(firstLifecycle, firstBatch, firstCall);
        BatchSnapshot captured = availableSnapshot(firstLifecycle.batchSnapshot(firstBatch));
        CallId secondCall = new CallId("second");
        BatchHandle secondBatch = begunBatch(firstLifecycle, firstRun, secondCall);
        assertEquals(new RunLifecycle.BatchSnapshotResult.Rejected(RunLifecycle.BatchSnapshotRejection.STALE_BATCH),
            firstLifecycle.batchSnapshot(firstBatch));
        assertEquals(List.of(new CallSnapshot(firstCall, CallStatus.COMPLETED)), captured.calls());
        assertNotNull(secondRun);
        assertNotNull(secondBatch);
    }

    @Test
    void closeDrainsWithoutReopening() throws Exception {
        RunLifecycle lifecycle = new RunLifecycle();
        RunHandle run = startedRun(lifecycle);
        CallId active = new CallId("active");
        CallId pending = new CallId("pending");
        BatchHandle batch = begunBatch(lifecycle, run, active, pending);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<RunLifecycle.ExecutionResult> execution = executor.submit(
                () -> lifecycle.execute(batch, active, () -> {
                    entered.countDown();
                    await(release);
                }));
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            lifecycle.stop(run);
            assertEquals(LifecyclePhase.CLOSING, lifecycle.close().phase());
            assertEquals(LifecyclePhase.CLOSING, lifecycle.close().phase());
            release.countDown();
            execution.get(5, TimeUnit.SECONDS);
            assertEquals(LifecyclePhase.CLOSED, finishedPhase(lifecycle.finishRun(run)));
            assertEquals(new RunLifecycle.StartRunResult.Rejected(RunLifecycle.StartRejection.CLOSED), lifecycle.startRun());
        }
    }

    @Test
    void nullInputDoesNotMutate() {
        RunLifecycle lifecycle = new RunLifecycle();
        assertThrows(NullPointerException.class, () -> new CallId(null));
        assertThrows(NullPointerException.class, () -> lifecycle.beginBatch(null, CallBatch.of(List.of(new CallId("x")))));
        assertThrows(NullPointerException.class, () -> lifecycle.execute(null, new CallId("x"), () -> { }));
        assertThrows(NullPointerException.class, () -> lifecycle.stop(null));
        assertThrows(NullPointerException.class, () -> lifecycle.finishRun(null));
        assertThrows(NullPointerException.class, () -> lifecycle.batchSnapshot(null));
        assertEquals(new LifecycleSnapshot.Idle(), lifecycle.snapshot());
    }

    @Test
    void nonblockingStopAndImmutableSnapshots() throws Exception {
        RunLifecycle lifecycle = new RunLifecycle();
        RunHandle run = startedRun(lifecycle);
        CallId call = new CallId("call");
        BatchHandle batch = begunBatch(lifecycle, run, call);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<RunLifecycle.ExecutionResult> execution = executor.submit(() -> lifecycle.execute(batch, call, () -> {
                entered.countDown();
                await(release);
            }));
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            BatchSnapshot before = availableSnapshot(lifecycle.batchSnapshot(batch));
            assertEquals(CallStatus.EXECUTING, before.calls().getFirst().status());
            assertEquals(LifecyclePhase.STOPPING, stoppedPhase(lifecycle.stop(run)));
            release.countDown();
            execution.get(5, TimeUnit.SECONDS);
            assertEquals(CallStatus.EXECUTING, before.calls().getFirst().status());
            assertEquals(CallStatus.COMPLETED, batchStatus(lifecycle, batch, call));
        }
    }

    @Test
    void multipleBatchesPreserveIdentity() {
        RunLifecycle lifecycle = new RunLifecycle();
        RunHandle run = startedRun(lifecycle);
        CallId first = new CallId("first");
        CallId second = new CallId("second");
        CallId fresh = new CallId("fresh");
        BatchHandle firstBatch = begunBatch(lifecycle, run, first, second);
        assertEquals(new RunLifecycle.FinishRunResult.Rejected(RunLifecycle.FinishRejection.BATCH_UNSETTLED), lifecycle.finishRun(run));
        lifecycleExecute(lifecycle, firstBatch, first);
        lifecycleExecute(lifecycle, firstBatch, second);
        assertInstanceOf(RunLifecycle.FinishRunResult.Finished.class, lifecycle.finishRun(run));

        RunHandle nextRun = startedRun(lifecycle);
        BatchHandle nextBatch = begunBatch(lifecycle, nextRun, fresh);
        lifecycleExecute(lifecycle, nextBatch, fresh);
        assertEquals(new RunLifecycle.BeginBatchResult.Rejected(RunLifecycle.BatchRejection.CALL_ID_ALREADY_ACCEPTED),
            lifecycle.beginBatch(nextRun, CallBatch.of(List.of(fresh, first))));
        BatchHandle freshBatch = begunBatch(lifecycle, nextRun, new CallId("new"));
        lifecycleExecute(lifecycle, freshBatch, new CallId("new"));
        assertEquals(LifecyclePhase.IDLE, finishedPhase(lifecycle.finishRun(nextRun)));
        assertNotNull(nextBatch);
    }

    @Test
    void awtAdmissionSmoke() throws Exception {
        RunLifecycle lifecycle = new RunLifecycle();
        RunHandle run = startedRun(lifecycle);
        CallId call = new CallId("awt");
        BatchHandle batch = begunBatch(lifecycle, run, call);
        CountDownLatch edtBlocked = new CountDownLatch(1);
        CountDownLatch releaseEdt = new CountDownLatch(1);
        EventQueue.invokeLater(() -> {
            edtBlocked.countDown();
            await(releaseEdt);
        });
        assertTrue(edtBlocked.await(5, TimeUnit.SECONDS));
        AtomicInteger entries = new AtomicInteger();
        FutureTaskResult queued = new FutureTaskResult();
        EventQueue.invokeLater(() -> queued.set(lifecycle.execute(batch, call, entries::incrementAndGet)));
        RunLifecycle.StopResult stopBefore = lifecycle.stop(run);
        releaseEdt.countDown();
        assertEquals(0, entries.get());
        assertEquals(new RunLifecycle.ExecutionResult.Rejected(RunLifecycle.ExecutionRejection.RUN_NOT_ACCEPTING_EFFECTS), queued.get());
        assertEquals(CallStatus.CANCELLED_BEFORE_START, batchStatus(lifecycle, batch, call));
        assertEquals(LifecyclePhase.STOPPING, stoppedPhase(stopBefore));

        RunLifecycle admittedLifecycle = new RunLifecycle();
        RunHandle admittedRun = startedRun(admittedLifecycle);
        CallId admittedCall = new CallId("admitted");
        BatchHandle admittedBatch = begunBatch(admittedLifecycle, admittedRun, admittedCall);
        CountDownLatch effectEntered = new CountDownLatch(1);
        CountDownLatch releaseEffect = new CountDownLatch(1);
        FutureTaskResult admittedResult = new FutureTaskResult();
        EventQueue.invokeLater(() -> admittedResult.set(admittedLifecycle.execute(admittedBatch, admittedCall, () -> {
            effectEntered.countDown();
            await(releaseEffect);
        })));
        assertTrue(effectEntered.await(5, TimeUnit.SECONDS));
        assertEquals(LifecyclePhase.STOPPING, stoppedPhase(admittedLifecycle.stop(admittedRun)));
        releaseEffect.countDown();
        assertEquals(new RunLifecycle.ExecutionResult.Executed(), admittedResult.get());
        assertEquals(CallStatus.COMPLETED, batchStatus(admittedLifecycle, admittedBatch, admittedCall));
    }

    private static void assertTerminalStatus(Effect effect) {
        assertTerminalStatus(CallStatus.COMPLETED, effect, null);
    }

    private static void assertTerminalStatus(CallStatus expected, Effect effect, Throwable thrown) {
        RunLifecycle lifecycle = new RunLifecycle();
        RunHandle run = startedRun(lifecycle);
        CallId call = new CallId("call");
        BatchHandle batch = begunBatch(lifecycle, run, call);
        if (thrown == null) {
            assertInstanceOf(RunLifecycle.ExecutionResult.Executed.class, lifecycle.execute(batch, call, effect));
        } else {
            Throwable observed = assertThrows(thrown.getClass(), () -> lifecycle.execute(batch, call, effect));
            assertSame(thrown, observed);
        }
        assertEquals(expected, batchStatus(lifecycle, batch, call));
        RunLifecycle.ExecutionResult duplicate = lifecycle.execute(batch, call, () -> { });
        assertEquals(new RunLifecycle.ExecutionResult.Rejected(RunLifecycle.ExecutionRejection.ALREADY_TERMINAL), duplicate);
    }

    private static RunHandle startedRun(RunLifecycle lifecycle) {
        return ((RunLifecycle.StartRunResult.Started) lifecycle.startRun()).run();
    }

    private static BatchHandle begunBatch(RunLifecycle lifecycle, RunHandle run, CallId... calls) {
        return ((RunLifecycle.BeginBatchResult.Begun) lifecycle.beginBatch(run, CallBatch.of(List.of(calls)))).batch();
    }

    private static void lifecycleExecute(RunLifecycle lifecycle, BatchHandle batch, CallId call) {
        assertInstanceOf(RunLifecycle.ExecutionResult.Executed.class, lifecycle.execute(batch, call, () -> { }));
    }

    private static BatchSnapshot availableSnapshot(RunLifecycle.BatchSnapshotResult result) {
        return ((RunLifecycle.BatchSnapshotResult.Available) result).snapshot();
    }

    private static CallStatus batchStatus(RunLifecycle lifecycle, BatchHandle batch, CallId call) {
        return availableSnapshot(lifecycle.batchSnapshot(batch)).calls().stream()
            .filter(snapshot -> snapshot.call().equals(call))
            .findFirst()
            .orElseThrow()
            .status();
    }

    private static LifecyclePhase stoppedPhase(RunLifecycle.StopResult result) {
        return ((RunLifecycle.StopResult.Acknowledged) result).lifecycle().phase();
    }

    private static LifecyclePhase finishedPhase(RunLifecycle.FinishRunResult result) {
        return ((RunLifecycle.FinishRunResult.Finished) result).lifecycle().phase();
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("Latch was not released");
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError(interrupted);
        }
    }

    private static final class FutureTaskResult {
        private final CountDownLatch done = new CountDownLatch(1);
        private volatile RunLifecycle.ExecutionResult result;

        private synchronized void set(RunLifecycle.ExecutionResult result) {
            this.result = result;
            done.countDown();
        }

        private RunLifecycle.ExecutionResult get() throws InterruptedException {
            assertTrue(done.await(5, TimeUnit.SECONDS));
            return result;
        }
    }
}
