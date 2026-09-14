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
    private final Set<Call.Id> acceptedCallIds = new HashSet<>();

    private Lifecycle.Phase phase = Lifecycle.Phase.IDLE;
    private RunHandle currentRun;
    private BatchState currentBatch;

    public synchronized StartRunResult startRun() {
        if (phase != Lifecycle.Phase.IDLE) {
            return new StartRunResult.Rejected(startRejection());
        }
        currentRun = new RunHandle(ownerKey);
        currentBatch = null;
        acceptedCallIds.clear();
        phase = Lifecycle.Phase.RUNNING;
        return new StartRunResult.Started(currentRun);
    }

    public synchronized BeginBatchResult beginBatch(RunHandle run, Call.Batch batch) {
        Objects.requireNonNull(run, "run");
        Objects.requireNonNull(batch, "batch");
        if (isStaleRun(run)) {
            return new BeginBatchResult.Rejected(BatchRejection.STALE_RUN);
        }
        if (phase != Lifecycle.Phase.RUNNING) {
            return new BeginBatchResult.Rejected(BatchRejection.RUN_NOT_ACCEPTING_BATCHES);
        }
        if (currentBatch != null && currentBatch.hasUnsettledCalls()) {
            return new BeginBatchResult.Rejected(BatchRejection.PREVIOUS_BATCH_UNSETTLED);
        }
        if (batch.calls().stream().anyMatch(acceptedCallIds::contains)) {
            return new BeginBatchResult.Rejected(BatchRejection.CALL_ID_ALREADY_ACCEPTED);
        }

        currentBatch = new BatchState(new Batch.Handle(ownerKey, run), batch.calls());
        acceptedCallIds.addAll(batch.calls());
        return new BeginBatchResult.Begun(currentBatch.handle);
    }

    public ExecutionResult execute(Batch.Handle batch, Call.Id call, Effect effect) {
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
            complete(batch, call, Call.Status.COMPLETED);
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
        if (phase == Lifecycle.Phase.RUNNING) {
            phase = Lifecycle.Phase.STOPPING;
            cancelPending();
        } else if (phase != Lifecycle.Phase.STOPPING && phase != Lifecycle.Phase.CLOSING) {
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

        phase = phase == Lifecycle.Phase.CLOSING ? Lifecycle.Phase.CLOSED : Lifecycle.Phase.IDLE;
        currentRun = null;
        currentBatch = null;
        acceptedCallIds.clear();
        return new FinishRunResult.Finished(snapshot());
    }

    public synchronized Lifecycle.Snapshot close() {
        if (phase == Lifecycle.Phase.CLOSED || phase == Lifecycle.Phase.CLOSING) {
            return snapshot();
        }
        if (phase == Lifecycle.Phase.IDLE) {
            phase = Lifecycle.Phase.CLOSED;
        } else {
            phase = Lifecycle.Phase.CLOSING;
            cancelPending();
        }
        return snapshot();
    }

    public synchronized Lifecycle.Snapshot snapshot() {
        return switch (phase) {
            case IDLE -> Lifecycle.idle();
            case RUNNING, STOPPING, CLOSING -> new Lifecycle.Snapshot.Active(phase);
            case CLOSED -> Lifecycle.closed();
        };
    }

    public synchronized BatchSnapshotResult batchSnapshot(Batch.Handle handle) {
        Objects.requireNonNull(handle, "handle");
        if (isStaleBatch(handle)) {
            return new BatchSnapshotResult.Rejected(BatchSnapshotRejection.STALE_BATCH);
        }
        return new BatchSnapshotResult.Available(currentBatch.snapshot());
    }

    private void complete(Batch.Handle batch, Call.Id call, Call.Status status) {
        synchronized (this) {
            currentBatch.ensureCurrent(batch, call);
            currentBatch.markTerminal(call, status);
        }
    }

    private void recordFailure(Batch.Handle batch, Call.Id call, Throwable failure) {
        try {
            complete(batch, call, Call.Status.FAILED_AFTER_START);
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

    private ExecutionRejection rejectExecution(Batch.Handle batch, Call.Id call) {
        if (isStaleBatch(batch)) return ExecutionRejection.STALE_BATCH;
        if (phase != Lifecycle.Phase.RUNNING) return ExecutionRejection.RUN_NOT_ACCEPTING_EFFECTS;

        Call.Status target = currentBatch.status(call);
        if (target == null) return ExecutionRejection.UNKNOWN_CALL;
        if (target == Call.Status.EXECUTING) return ExecutionRejection.ALREADY_EXECUTING;
        if (target.isTerminal()) return ExecutionRejection.ALREADY_TERMINAL;

        Call.Id next = currentBatch.nextUnsettled();
        return next != null && next.equals(call) ? null : ExecutionRejection.OUT_OF_ORDER;
    }

    private boolean isStaleRun(RunHandle run) {
        return !run.belongsTo(ownerKey) || run != currentRun;
    }

    private boolean isStaleBatch(Batch.Handle batch) {
        return !batch.belongsTo(ownerKey) || isStaleRun(batch.run())
            || currentBatch == null || currentBatch.handle != batch;
    }

    private void cancelPending() {
        if (currentBatch != null) {
            currentBatch.cancelPending();
        }
    }

    private Batch.Observation currentBatchObservation() {
        return currentBatch == null
            ? Batch.Observation.absent()
            : new Batch.Observation.Present(currentBatch.snapshot());
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
        record Begun(Batch.Handle batch) implements BeginBatchResult { public Begun { Objects.requireNonNull(batch); } }
        record Rejected(BatchRejection reason) implements BeginBatchResult { public Rejected { Objects.requireNonNull(reason); } }
    }

    public sealed interface ExecutionResult {
        record Executed() implements ExecutionResult { }
        record Rejected(ExecutionRejection reason) implements ExecutionResult { public Rejected { Objects.requireNonNull(reason); } }
    }

    public sealed interface StopResult {
        record Acknowledged(Lifecycle.Snapshot lifecycle, Batch.Observation batch) implements StopResult {
            public Acknowledged {
                Objects.requireNonNull(lifecycle);
                Objects.requireNonNull(batch);
            }
        }
        record Rejected(StopRejection reason) implements StopResult { public Rejected { Objects.requireNonNull(reason); } }
    }

    public sealed interface FinishRunResult {
        record Finished(Lifecycle.Snapshot lifecycle) implements FinishRunResult { public Finished { Objects.requireNonNull(lifecycle); } }
        record Rejected(FinishRejection reason) implements FinishRunResult { public Rejected { Objects.requireNonNull(reason); } }
    }

    public sealed interface BatchSnapshotResult {
        record Available(Batch.Snapshot snapshot) implements BatchSnapshotResult {
            public Available { Objects.requireNonNull(snapshot); }
        }
        record Rejected(BatchSnapshotRejection reason) implements BatchSnapshotResult {
            public Rejected { Objects.requireNonNull(reason); }
        }
    }

    private static final class BatchState {
        private final Batch.Handle handle;
        private final List<Call.Id> order;
        private final Map<Call.Id, Call.Status> statuses;

        private BatchState(Batch.Handle handle, List<Call.Id> calls) {
            this.handle = handle;
            this.order = List.copyOf(calls);
            this.statuses = new LinkedHashMap<>();
            calls.forEach(call -> statuses.put(call, Call.Status.PENDING));
        }

        private Call.Status status(Call.Id call) {
            return statuses.get(call);
        }

        private void markExecuting(Call.Id call) {
            mark(call, Call.Status.EXECUTING);
        }

        private void markTerminal(Call.Id call, Call.Status status) {
            if (!status.isTerminal() || statuses.get(call) != Call.Status.EXECUTING) {
                throw new IllegalStateException("Call is not executing");
            }
            mark(call, status);
        }

        private void ensureCurrent(Batch.Handle expectedHandle, Call.Id call) {
            if (handle != expectedHandle || statuses.get(call) != Call.Status.EXECUTING) {
                throw new IllegalStateException("Executing call is no longer current");
            }
        }

        private void mark(Call.Id call, Call.Status status) {
            if (!statuses.containsKey(call)) {
                throw new IllegalStateException("Unknown accepted call");
            }
            statuses.put(call, status);
        }

        private Call.Id nextUnsettled() {
            return order.stream()
                .filter(call -> !statuses.get(call).isTerminal())
                .findFirst()
                .orElse(null);
        }

        private void cancelPending() {
            order.stream()
                .filter(call -> statuses.get(call) == Call.Status.PENDING)
                .forEach(call -> statuses.put(call, Call.Status.CANCELLED_BEFORE_START));
        }

        private boolean hasUnsettledCalls() {
            return statuses.values().stream().anyMatch(status -> !status.isTerminal());
        }

        private Batch.Snapshot snapshot() {
            List<Call.Snapshot> snapshot = new ArrayList<>(order.size());
            order.forEach(call -> snapshot.add(new Call.Snapshot(call, statuses.get(call))));
            return new Batch.Snapshot(snapshot);
        }
    }
}
