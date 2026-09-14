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
        Call.Id first = new Call.Id("first");
        Call.Id duplicate = new Call.Id("duplicate");
        assertThrows(IllegalArgumentException.class, () -> Call.Batch.of(List.of()));
        assertThrows(NullPointerException.class, () -> Call.Batch.of(null));
        assertThrows(IllegalArgumentException.class, () -> Call.Batch.of(List.of(duplicate, duplicate)));
        assertThrows(NullPointerException.class, () -> Call.Batch.of(Arrays.asList(first, null)));

        var input = new java.util.ArrayList<>(List.of(first));
        Call.Batch batch = Call.Batch.of(input);
        input.clear();
        assertEquals(List.of(first), batch.calls());
        assertThrows(UnsupportedOperationException.class, () -> batch.calls().clear());
    }

    @Test
    void orderedExclusiveExecution() throws Exception {
        RunLifecycle lifecycle = new RunLifecycle();
        RunHandle run = startedRun(lifecycle);
        Call.Id first = new Call.Id("first");
        Call.Id second = new Call.Id("second");
        Batch.Handle batch = begunBatch(lifecycle, run, first, second);
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
        Call.Id call = new Call.Id("queued");
        Batch.Handle batch = begunBatch(lifecycle, run, call);
        CountDownLatch queueGate = new CountDownLatch(1);
        AtomicInteger entries = new AtomicInteger();

        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            executor.submit(() -> await(queueGate));
            Future<RunLifecycle.ExecutionResult> queued = executor.submit(
                () -> lifecycle.execute(batch, call, entries::incrementAndGet));
            RunLifecycle.StopResult stopped = lifecycle.stop(run);
            queueGate.countDown();
            assertEquals(0, entries.get());
            assertEquals(Lifecycle.Phase.STOPPING, stoppedPhase(stopped));
            assertEquals(new RunLifecycle.ExecutionResult.Rejected(RunLifecycle.ExecutionRejection.RUN_NOT_ACCEPTING_EFFECTS),
                queued.get(5, TimeUnit.SECONDS));
            assertEquals(Call.Status.CANCELLED_BEFORE_START, batchStatus(lifecycle, batch, call));
        }
    }

    @Test
    void stopDuringEffect() throws Exception {
        RunLifecycle lifecycle = new RunLifecycle();
        RunHandle run = startedRun(lifecycle);
        Call.Id first = new Call.Id("first");
        Call.Id second = new Call.Id("second");
        Batch.Handle batch = begunBatch(lifecycle, run, first, second);
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
            assertEquals(Call.Status.COMPLETED, batchStatus(lifecycle, batch, first));
            assertEquals(Call.Status.CANCELLED_BEFORE_START, batchStatus(lifecycle, batch, second));
            assertInstanceOf(RunLifecycle.FinishRunResult.Finished.class, lifecycle.finishRun(run));
        }
    }

    @Test
    void terminalAccounting() {
        assertTerminalStatus(() -> { });
        RuntimeException runtime = new RuntimeException("runtime");
        assertTerminalStatus(Call.Status.FAILED_AFTER_START, () -> { throw runtime; }, runtime);
        AssertionError error = new AssertionError("error");
        assertTerminalStatus(Call.Status.FAILED_AFTER_START, () -> { throw error; }, error);
    }

    @Test
    void staleHandleIsolation() {
        RunLifecycle firstLifecycle = new RunLifecycle();
        RunLifecycle secondLifecycle = new RunLifecycle();
        RunHandle firstRun = startedRun(firstLifecycle);
        RunHandle secondRun = startedRun(secondLifecycle);
        Call.Id firstCall = new Call.Id("first");
        Batch.Handle firstBatch = begunBatch(firstLifecycle, firstRun, firstCall);

        assertEquals(new RunLifecycle.BeginBatchResult.Rejected(RunLifecycle.BatchRejection.STALE_RUN),
            secondLifecycle.beginBatch(firstRun, Call.Batch.of(List.of(new Call.Id("foreign")))));
        lifecycleExecute(firstLifecycle, firstBatch, firstCall);
        Batch.Snapshot captured = availableSnapshot(firstLifecycle.batchSnapshot(firstBatch));
        Call.Id secondCall = new Call.Id("second");
        Batch.Handle secondBatch = begunBatch(firstLifecycle, firstRun, secondCall);
        assertEquals(new RunLifecycle.BatchSnapshotResult.Rejected(RunLifecycle.BatchSnapshotRejection.STALE_BATCH),
            firstLifecycle.batchSnapshot(firstBatch));
        assertEquals(List.of(new Call.Snapshot(firstCall, Call.Status.COMPLETED)), captured.calls());
        assertNotNull(secondRun);
        assertNotNull(secondBatch);
    }

    @Test
    void closeDrainsWithoutReopening() throws Exception {
        RunLifecycle lifecycle = new RunLifecycle();
        RunHandle run = startedRun(lifecycle);
        Call.Id active = new Call.Id("active");
        Call.Id pending = new Call.Id("pending");
        Batch.Handle batch = begunBatch(lifecycle, run, active, pending);
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
            assertEquals(Lifecycle.Phase.CLOSING, lifecycle.close().phase());
            assertEquals(Lifecycle.Phase.CLOSING, lifecycle.close().phase());
            release.countDown();
            execution.get(5, TimeUnit.SECONDS);
            assertEquals(Lifecycle.Phase.CLOSED, finishedPhase(lifecycle.finishRun(run)));
            assertEquals(new RunLifecycle.StartRunResult.Rejected(RunLifecycle.StartRejection.CLOSED), lifecycle.startRun());
        }
    }

    @Test
    void nullInputDoesNotMutate() {
        RunLifecycle lifecycle = new RunLifecycle();
        assertThrows(NullPointerException.class, () -> new Call.Id(null));
        assertThrows(NullPointerException.class, () -> lifecycle.beginBatch(null, Call.Batch.of(List.of(new Call.Id("x")))));
        assertThrows(NullPointerException.class, () -> lifecycle.execute(null, new Call.Id("x"), () -> { }));
        assertThrows(NullPointerException.class, () -> lifecycle.stop(null));
        assertThrows(NullPointerException.class, () -> lifecycle.finishRun(null));
        assertThrows(NullPointerException.class, () -> lifecycle.batchSnapshot(null));
        assertEquals(Lifecycle.idle(), lifecycle.snapshot());
    }

    @Test
    void nonblockingStopAndImmutableSnapshots() throws Exception {
        RunLifecycle lifecycle = new RunLifecycle();
        RunHandle run = startedRun(lifecycle);
        Call.Id call = new Call.Id("call");
        Batch.Handle batch = begunBatch(lifecycle, run, call);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<RunLifecycle.ExecutionResult> execution = executor.submit(() -> lifecycle.execute(batch, call, () -> {
                entered.countDown();
                await(release);
            }));
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            Batch.Snapshot before = availableSnapshot(lifecycle.batchSnapshot(batch));
            assertEquals(Call.Status.EXECUTING, before.calls().getFirst().status());
            assertEquals(Lifecycle.Phase.STOPPING, stoppedPhase(lifecycle.stop(run)));
            release.countDown();
            execution.get(5, TimeUnit.SECONDS);
            assertEquals(Call.Status.EXECUTING, before.calls().getFirst().status());
            assertEquals(Call.Status.COMPLETED, batchStatus(lifecycle, batch, call));
        }
    }

    @Test
    void multipleBatchesPreserveIdentity() {
        RunLifecycle lifecycle = new RunLifecycle();
        RunHandle run = startedRun(lifecycle);
        Call.Id first = new Call.Id("first");
        Call.Id second = new Call.Id("second");
        Call.Id fresh = new Call.Id("fresh");
        Batch.Handle firstBatch = begunBatch(lifecycle, run, first, second);
        assertEquals(new RunLifecycle.FinishRunResult.Rejected(RunLifecycle.FinishRejection.BATCH_UNSETTLED), lifecycle.finishRun(run));
        lifecycleExecute(lifecycle, firstBatch, first);
        lifecycleExecute(lifecycle, firstBatch, second);
        assertInstanceOf(RunLifecycle.FinishRunResult.Finished.class, lifecycle.finishRun(run));

        RunHandle nextRun = startedRun(lifecycle);
        Batch.Handle nextBatch = begunBatch(lifecycle, nextRun, fresh);
        lifecycleExecute(lifecycle, nextBatch, fresh);
        assertEquals(new RunLifecycle.BeginBatchResult.Rejected(RunLifecycle.BatchRejection.CALL_ID_ALREADY_ACCEPTED),
            lifecycle.beginBatch(nextRun, Call.Batch.of(List.of(fresh, first))));
        Batch.Handle freshBatch = begunBatch(lifecycle, nextRun, new Call.Id("new"));
        lifecycleExecute(lifecycle, freshBatch, new Call.Id("new"));
        assertEquals(Lifecycle.Phase.IDLE, finishedPhase(lifecycle.finishRun(nextRun)));
        assertNotNull(nextBatch);
    }

    @Test
    void awtAdmissionSmoke() throws Exception {
        RunLifecycle lifecycle = new RunLifecycle();
        RunHandle run = startedRun(lifecycle);
        Call.Id call = new Call.Id("awt");
        Batch.Handle batch = begunBatch(lifecycle, run, call);
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
        assertEquals(Call.Status.CANCELLED_BEFORE_START, batchStatus(lifecycle, batch, call));
        assertEquals(Lifecycle.Phase.STOPPING, stoppedPhase(stopBefore));

        RunLifecycle admittedLifecycle = new RunLifecycle();
        RunHandle admittedRun = startedRun(admittedLifecycle);
        Call.Id admittedCall = new Call.Id("admitted");
        Batch.Handle admittedBatch = begunBatch(admittedLifecycle, admittedRun, admittedCall);
        CountDownLatch effectEntered = new CountDownLatch(1);
        CountDownLatch releaseEffect = new CountDownLatch(1);
        FutureTaskResult admittedResult = new FutureTaskResult();
        EventQueue.invokeLater(() -> admittedResult.set(admittedLifecycle.execute(admittedBatch, admittedCall, () -> {
            effectEntered.countDown();
            await(releaseEffect);
        })));
        assertTrue(effectEntered.await(5, TimeUnit.SECONDS));
        assertEquals(Lifecycle.Phase.STOPPING, stoppedPhase(admittedLifecycle.stop(admittedRun)));
        releaseEffect.countDown();
        assertEquals(new RunLifecycle.ExecutionResult.Executed(), admittedResult.get());
        assertEquals(Call.Status.COMPLETED, batchStatus(admittedLifecycle, admittedBatch, admittedCall));
    }

    private static void assertTerminalStatus(Effect effect) {
        assertTerminalStatus(Call.Status.COMPLETED, effect, null);
    }

    private static void assertTerminalStatus(Call.Status expected, Effect effect, Throwable thrown) {
        RunLifecycle lifecycle = new RunLifecycle();
        RunHandle run = startedRun(lifecycle);
        Call.Id call = new Call.Id("call");
        Batch.Handle batch = begunBatch(lifecycle, run, call);
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

    private static Batch.Handle begunBatch(RunLifecycle lifecycle, RunHandle run, Call.Id... calls) {
        return ((RunLifecycle.BeginBatchResult.Begun) lifecycle.beginBatch(run, Call.Batch.of(List.of(calls)))).batch();
    }

    private static void lifecycleExecute(RunLifecycle lifecycle, Batch.Handle batch, Call.Id call) {
        assertInstanceOf(RunLifecycle.ExecutionResult.Executed.class, lifecycle.execute(batch, call, () -> { }));
    }

    private static Batch.Snapshot availableSnapshot(RunLifecycle.BatchSnapshotResult result) {
        return ((RunLifecycle.BatchSnapshotResult.Available) result).snapshot();
    }

    private static Call.Status batchStatus(RunLifecycle lifecycle, Batch.Handle batch, Call.Id call) {
        return availableSnapshot(lifecycle.batchSnapshot(batch)).calls().stream()
            .filter(snapshot -> snapshot.call().equals(call))
            .findFirst()
            .orElseThrow()
            .status();
    }

    private static Lifecycle.Phase stoppedPhase(RunLifecycle.StopResult result) {
        return ((RunLifecycle.StopResult.Acknowledged) result).lifecycle().phase();
    }

    private static Lifecycle.Phase finishedPhase(RunLifecycle.FinishRunResult result) {
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
