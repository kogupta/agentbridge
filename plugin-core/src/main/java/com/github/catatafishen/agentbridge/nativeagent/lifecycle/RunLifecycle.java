package com.github.catatafishen.agentbridge.nativeagent.lifecycle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class RunLifecycle {
    private final Object ownerKey = new Object();
    private final Set<CallId> acceptedCallIds = new HashSet<>();

    private LifecyclePhase phase = LifecyclePhase.IDLE;
    private RunHandle currentRun;
    private BatchState currentBatch;

    public synchronized StartRunResult startRun() {
        if (phase != LifecyclePhase.IDLE) {
            return new StartRunResult.Rejected(startRejection());
        }
        currentRun = new RunHandle(ownerKey);
        currentBatch = null;
        acceptedCallIds.clear();
        phase = LifecyclePhase.RUNNING;
        return new StartRunResult.Started(currentRun);
    }

    public synchronized BeginBatchResult beginBatch(RunHandle run, CallBatch batch) {
        Objects.requireNonNull(run, "run");
        Objects.requireNonNull(batch, "batch");
        if (isStaleRun(run)) {
            return new BeginBatchResult.Rejected(BatchRejection.STALE_RUN);
        }
        if (phase != LifecyclePhase.RUNNING) {
            return new BeginBatchResult.Rejected(BatchRejection.RUN_NOT_ACCEPTING_BATCHES);
        }
        if (currentBatch != null && currentBatch.hasUnsettledCalls()) {
            return new BeginBatchResult.Rejected(BatchRejection.PREVIOUS_BATCH_UNSETTLED);
        }
        if (batch.calls().stream().anyMatch(acceptedCallIds::contains)) {
            return new BeginBatchResult.Rejected(BatchRejection.CALL_ID_ALREADY_ACCEPTED);
        }

        currentBatch = new BatchState(new BatchHandle(ownerKey, run), batch.calls());
        acceptedCallIds.addAll(batch.calls());
        return new BeginBatchResult.Begun(currentBatch.handle);
    }

    public ExecutionResult execute(BatchHandle batch, CallId call, Effect effect) {
        Objects.requireNonNull(batch, "batch");
        Objects.requireNonNull(call, "call");
        Objects.requireNonNull(effect, "effect");

        synchronized (this) {
            ExecutionRejection rejection = rejectExecution(batch, call);
            if (rejection != null) {
                return new ExecutionResult.Rejected(rejection);
            }
            currentBatch.markExecuting(call);
        }

        try {
            effect.execute();
            complete(batch, call, CallStatus.COMPLETED);
            return new ExecutionResult.Executed();
        } catch (RuntimeException | Error failure) {
            recordFailure(batch, call, failure);
            throw failure;
        }
    }

    public synchronized StopResult stop(RunHandle run) {
        Objects.requireNonNull(run, "run");
        if (isStaleRun(run)) {
            return new StopResult.Rejected(StopRejection.STALE_RUN);
        }
        if (phase == LifecyclePhase.RUNNING) {
            phase = LifecyclePhase.STOPPING;
            cancelPending();
        } else if (phase != LifecyclePhase.STOPPING && phase != LifecyclePhase.CLOSING) {
            return new StopResult.Rejected(StopRejection.RUN_NOT_ACTIVE);
        }
        return new StopResult.Acknowledged(snapshot(), currentBatchObservation());
    }

    public synchronized FinishRunResult finishRun(RunHandle run) {
        Objects.requireNonNull(run, "run");
        if (isStaleRun(run)) {
            return new FinishRunResult.Rejected(FinishRejection.STALE_RUN);
        }
        if (currentBatch != null && currentBatch.hasUnsettledCalls()) {
            return new FinishRunResult.Rejected(FinishRejection.BATCH_UNSETTLED);
        }

        phase = phase == LifecyclePhase.CLOSING ? LifecyclePhase.CLOSED : LifecyclePhase.IDLE;
        currentRun = null;
        currentBatch = null;
        acceptedCallIds.clear();
        return new FinishRunResult.Finished(snapshot());
    }

    public synchronized LifecycleSnapshot close() {
        if (phase == LifecyclePhase.CLOSED || phase == LifecyclePhase.CLOSING) {
            return snapshot();
        }
        if (phase == LifecyclePhase.IDLE) {
            phase = LifecyclePhase.CLOSED;
        } else {
            phase = LifecyclePhase.CLOSING;
            cancelPending();
        }
        return snapshot();
    }

    public synchronized LifecycleSnapshot snapshot() {
        return switch (phase) {
            case IDLE -> new LifecycleSnapshot.Idle();
            case RUNNING, STOPPING, CLOSING -> new LifecycleSnapshot.Active(phase);
            case CLOSED -> new LifecycleSnapshot.Closed();
        };
    }

    public synchronized BatchSnapshotResult batchSnapshot(BatchHandle handle) {
        Objects.requireNonNull(handle, "handle");
        if (isStaleBatch(handle)) {
            return new BatchSnapshotResult.Rejected(BatchSnapshotRejection.STALE_BATCH);
        }
        return new BatchSnapshotResult.Available(currentBatch.snapshot());
    }

    private void complete(BatchHandle batch, CallId call, CallStatus status) {
        synchronized (this) {
            currentBatch.ensureCurrent(batch, call);
            currentBatch.markTerminal(call, status);
        }
    }

    private void recordFailure(BatchHandle batch, CallId call, Throwable failure) {
        try {
            complete(batch, call, CallStatus.FAILED_AFTER_START);
        } catch (RuntimeException | Error accountingFailure) {
            if (accountingFailure != failure) {
                failure.addSuppressed(accountingFailure);
            }
        }
    }

    private StartRejection startRejection() {
        return switch (phase) {
            case RUNNING, STOPPING -> StartRejection.BUSY;
            case CLOSING, CLOSED -> StartRejection.CLOSED;
            case IDLE -> throw new IllegalStateException("Idle run should have started");
        };
    }

    private ExecutionRejection rejectExecution(BatchHandle batch, CallId call) {
        if (isStaleBatch(batch)) {
            return ExecutionRejection.STALE_BATCH;
        }
        if (phase != LifecyclePhase.RUNNING) {
            return ExecutionRejection.RUN_NOT_ACCEPTING_EFFECTS;
        }
        CallStatus target = currentBatch.status(call);
        if (target == null) {
            return ExecutionRejection.UNKNOWN_CALL;
        }
        if (target == CallStatus.EXECUTING) {
            return ExecutionRejection.ALREADY_EXECUTING;
        }
        if (target.isTerminal()) {
            return ExecutionRejection.ALREADY_TERMINAL;
        }
        CallId next = currentBatch.nextUnsettled();
        if (next == null || !next.equals(call)) {
            return ExecutionRejection.OUT_OF_ORDER;
        }
        return null;
    }

    private boolean isStaleRun(RunHandle run) {
        return !run.belongsTo(ownerKey) || run != currentRun;
    }

    private boolean isStaleBatch(BatchHandle batch) {
        return !batch.belongsTo(ownerKey) || isStaleRun(batch.run())
            || currentBatch == null || currentBatch.handle != batch;
    }

    private void cancelPending() {
        if (currentBatch != null) {
            currentBatch.cancelPending();
        }
    }

    private BatchObservation currentBatchObservation() {
        return currentBatch == null
            ? new BatchObservation.Absent()
            : new BatchObservation.Present(currentBatch.snapshot());
    }

    public enum StartRejection { BUSY, CLOSED }
    public enum BatchRejection { STALE_RUN, RUN_NOT_ACCEPTING_BATCHES, PREVIOUS_BATCH_UNSETTLED, CALL_ID_ALREADY_ACCEPTED }
    public enum ExecutionRejection { STALE_BATCH, RUN_NOT_ACCEPTING_EFFECTS, UNKNOWN_CALL, ALREADY_EXECUTING, ALREADY_TERMINAL, OUT_OF_ORDER }
    public enum StopRejection { STALE_RUN, RUN_NOT_ACTIVE }
    public enum FinishRejection { STALE_RUN, BATCH_UNSETTLED }
    public enum BatchSnapshotRejection { STALE_BATCH }

    public sealed interface StartRunResult {
        record Started(RunHandle run) implements StartRunResult { public Started { Objects.requireNonNull(run); } }
        record Rejected(StartRejection reason) implements StartRunResult { public Rejected { Objects.requireNonNull(reason); } }
    }

    public sealed interface BeginBatchResult {
        record Begun(BatchHandle batch) implements BeginBatchResult { public Begun { Objects.requireNonNull(batch); } }
        record Rejected(BatchRejection reason) implements BeginBatchResult { public Rejected { Objects.requireNonNull(reason); } }
    }

    public sealed interface ExecutionResult {
        record Executed() implements ExecutionResult { }
        record Rejected(ExecutionRejection reason) implements ExecutionResult { public Rejected { Objects.requireNonNull(reason); } }
    }

    public sealed interface StopResult {
        record Acknowledged(LifecycleSnapshot lifecycle, BatchObservation batch) implements StopResult {
            public Acknowledged {
                Objects.requireNonNull(lifecycle);
                Objects.requireNonNull(batch);
            }
        }
        record Rejected(StopRejection reason) implements StopResult { public Rejected { Objects.requireNonNull(reason); } }
    }

    public sealed interface FinishRunResult {
        record Finished(LifecycleSnapshot lifecycle) implements FinishRunResult { public Finished { Objects.requireNonNull(lifecycle); } }
        record Rejected(FinishRejection reason) implements FinishRunResult { public Rejected { Objects.requireNonNull(reason); } }
    }

    public sealed interface BatchSnapshotResult {
        record Available(BatchSnapshot snapshot) implements BatchSnapshotResult {
            public Available { Objects.requireNonNull(snapshot); }
        }
        record Rejected(BatchSnapshotRejection reason) implements BatchSnapshotResult {
            public Rejected { Objects.requireNonNull(reason); }
        }
    }

    private static final class BatchState {
        private final BatchHandle handle;
        private final List<CallId> order;
        private final Map<CallId, CallStatus> statuses;

        private BatchState(BatchHandle handle, List<CallId> calls) {
            this.handle = handle;
            this.order = List.copyOf(calls);
            this.statuses = new LinkedHashMap<>();
            calls.forEach(call -> statuses.put(call, CallStatus.PENDING));
        }

        private CallStatus status(CallId call) {
            return statuses.get(call);
        }

        private void markExecuting(CallId call) {
            mark(call, CallStatus.EXECUTING);
        }

        private void markTerminal(CallId call, CallStatus status) {
            if (!status.isTerminal() || statuses.get(call) != CallStatus.EXECUTING) {
                throw new IllegalStateException("Call is not executing");
            }
            mark(call, status);
        }

        private void ensureCurrent(BatchHandle expectedHandle, CallId call) {
            if (handle != expectedHandle || statuses.get(call) != CallStatus.EXECUTING) {
                throw new IllegalStateException("Executing call is no longer current");
            }
        }

        private void mark(CallId call, CallStatus status) {
            if (!statuses.containsKey(call)) {
                throw new IllegalStateException("Unknown accepted call");
            }
            statuses.put(call, status);
        }

        private CallId nextUnsettled() {
            return order.stream()
                .filter(call -> !statuses.get(call).isTerminal())
                .findFirst()
                .orElse(null);
        }

        private void cancelPending() {
            order.stream()
                .filter(call -> statuses.get(call) == CallStatus.PENDING)
                .forEach(call -> statuses.put(call, CallStatus.CANCELLED_BEFORE_START));
        }

        private boolean hasUnsettledCalls() {
            return statuses.values().stream().anyMatch(status -> !status.isTerminal());
        }

        private BatchSnapshot snapshot() {
            List<CallSnapshot> snapshot = new ArrayList<>(order.size());
            order.forEach(call -> snapshot.add(new CallSnapshot(call, statuses.get(call))));
            return new BatchSnapshot(snapshot);
        }
    }
}
