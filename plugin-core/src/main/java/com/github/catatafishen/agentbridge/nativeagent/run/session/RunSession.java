package com.github.catatafishen.agentbridge.nativeagent.run.session;

import com.github.catatafishen.agentbridge.nativeagent.lifecycle.Batch;
import com.github.catatafishen.agentbridge.nativeagent.lifecycle.Call;
import com.github.catatafishen.agentbridge.nativeagent.lifecycle.RunHandle;
import com.github.catatafishen.agentbridge.nativeagent.lifecycle.RunLifecycle;
import com.github.catatafishen.agentbridge.nativeagent.run.policy.RunLimits;
import com.github.catatafishen.agentbridge.nativeagent.run.resources.RunCancellation;
import com.github.catatafishen.agentbridge.nativeagent.run.resources.RunResources;
import com.github.catatafishen.agentbridge.nativeagent.run.resources.RunTimeSource;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class RunSession {
    private final Object ownerKey = new Object();
    private final RunLifecycle lifecycle = new RunLifecycle();
    private final List<RunMessage> accepted = new ArrayList<>();

    private Phase phase = Phase.IDLE;
    private ActiveRun current;
    private String provisional;
    private boolean disposed;

    public synchronized StartResult start(RunMessage.User user, RunLimits limits, Instant now,
                                          RunTimeSource timeSource) {
        Objects.requireNonNull(user, "user");
        Objects.requireNonNull(limits, "limits");
        Objects.requireNonNull(now, "now");
        Objects.requireNonNull(timeSource, "timeSource");
        RunLifecycle.StartRunResult started = lifecycle.startRun();
        if (started instanceof RunLifecycle.StartRunResult.Rejected(var reason)) {
            return new StartResult.Rejected(reason);
        }
        RunHandle handle = ((RunLifecycle.StartRunResult.Started) started).run();
        ActiveRun run = new ActiveRun(ownerKey, handle, new RunLimits.Budget(limits, now), timeSource);
        current = run;
        accepted.add(user);
        phase = Phase.REQUESTING;
        return new StartResult.Started(run);
    }

    public synchronized HistorySnapshot history() { return new HistorySnapshot(accepted); }

    public synchronized ProvisionalObservation provisional() {
        return provisional == null ? ProvisionalObservation.Absent.INSTANCE
            : new ProvisionalObservation.Present(provisional);
    }

    public synchronized Phase phase() { return phase; }

    public synchronized void updateProvisional(ActiveRun run, String text) {
        requireCurrent(run);
        Objects.requireNonNull(text, "text");
        if (phase != Phase.REQUESTING) throw new IllegalStateException("Provisional output requires REQUESTING");
        provisional = text;
    }

    public synchronized boolean tryUpdateProvisional(ActiveRun run, String text) {
        Objects.requireNonNull(run, "run");
        Objects.requireNonNull(text, "text");
        if (run.ownerKey != ownerKey || run != current || phase != Phase.REQUESTING || disposed) return false;
        provisional = text;
        return true;
    }

    public synchronized void clearProvisional(ActiveRun run) {
        requireCurrent(run);
        provisional = null;
    }

    public synchronized RunLimits.Admission beginRequest(ActiveRun run, Instant now) {
        requireCurrent(run);
        if (phase != Phase.REQUESTING) throw new IllegalStateException("Request admission requires REQUESTING");
        return run.budget.admitRequest(Objects.requireNonNull(now, "now"));
    }

    public synchronized TurnAcceptance acceptTurn(ActiveRun run, ValidatedAssistantTurn turn) {
        requireCurrent(run);
        Objects.requireNonNull(turn, "turn");
        if (phase != Phase.REQUESTING) throw new IllegalStateException("Turn acceptance requires REQUESTING");
        provisional = null;
        if (turn instanceof ValidatedAssistantTurn.Rejected rejected) {
            finishLifecycle(run);
            return new TurnAcceptance.ProtocolRejected(rejected.failure(), history());
        }

        RunMessage.Assistant assistant = assistant(turn);
        if (turn instanceof ValidatedAssistantTurn.LengthCalls) {
            beginBatch(run, assistant.calls());
            run.budget.recordAcceptedResponse();
            accepted.add(assistant);
            for (PlannedCall call : assistant.calls()) {
                CallAdmission admission = admission(run, call, false);
                requireExecuted(admission.execute(() -> {}));
                appendResult(run, new RunMessage.ToolResult(call.id(),
                    new ToolOutcome.NotStarted(ToolOutcome.Reason.TRUNCATED_NOT_EXECUTED,
                        "Length-truncated response was not executed")));
            }
            finishLifecycle(run);
            return new TurnAcceptance.Ended(history());
        }
        if (!assistant.calls().isEmpty()) {
            beginBatch(run, assistant.calls());
            run.budget.recordAcceptedResponse();
            accepted.add(assistant);
            phase = Phase.EXECUTING_TOOLS;
            return new TurnAcceptance.CallsReady();
        }
        run.budget.recordAcceptedResponse();
        accepted.add(assistant);
        finishLifecycle(run);
        return new TurnAcceptance.Ended(history());
    }

    public synchronized CallStep nextCall(ActiveRun run) {
        requireCurrent(run);
        if (phase == Phase.STOPPING) return new CallStep.Stopped();
        if (phase != Phase.EXECUTING_TOOLS) throw new IllegalStateException("Call step requires EXECUTING_TOOLS");
        PlannedCall next = nextResult(run);
        if (next == null) {
            Batch.Snapshot snapshot = batchSnapshot(run);
            if (!snapshot.isSettled()) throw new IllegalStateException("Domain results completed before lifecycle batch settled");
            phase = Phase.REQUESTING;
            return new CallStep.Complete();
        }
        CallAdmission admission = admission(run, next, next instanceof PlannedCall.Executable);
        if (next instanceof PlannedCall.Executable executable) {
            return new CallStep.Execute(executable, admission);
        }
        return new CallStep.AccountRejected((PlannedCall.Rejected) next, admission);
    }

    public synchronized void recordToolResult(ActiveRun run, RunMessage.ToolResult result) {
        requireCurrent(run);
        Objects.requireNonNull(result, "result");
        if (phase != Phase.EXECUTING_TOOLS && phase != Phase.STOPPING) {
            throw new IllegalStateException("Tool result requires active tool accounting");
        }
        appendResult(run, result);
        if (phase == Phase.STOPPING) appendCancelledInOrder(run);
    }

    public synchronized StopResult stop(ActiveRun run) {
        return stop(run, ToolOutcome.Reason.CANCELLED_NOT_STARTED,
            "Run stopped before tool admission");
    }
    public synchronized boolean stopIfCurrent(ActiveRun run) {
        Objects.requireNonNull(run, "run");
        if (run.ownerKey != ownerKey || run != current) return false;
        stop(run);
        return true;
    }


    public synchronized StopResult stopForLimit(ActiveRun run) {
        return stop(run, ToolOutcome.Reason.RUN_LIMIT_NOT_STARTED,
            "Run limit reached before tool admission");
    }

    private StopResult stop(ActiveRun run, ToolOutcome.Reason pendingReason, String message) {
        requireCurrent(run);
        RuntimeException cancellationFailure = null;
        try {
            run.cancellation.cancel();
        } catch (RuntimeException failure) {
            cancellationFailure = failure;
        }
        RunLifecycle.StopResult stopped = lifecycle.stop(run.handle);
        if (stopped instanceof RunLifecycle.StopResult.Rejected(var reason)) {
            throw new IllegalStateException("Current lifecycle run was rejected: " + reason);
        }
        phase = Phase.STOPPING;
        provisional = null;
        run.pendingReason = pendingReason;
        run.pendingMessage = message;
        appendCancelledInOrder(run);
        try {
            run.resources.cancel();
        } catch (RuntimeException failure) {
            if (cancellationFailure == null) cancellationFailure = failure;
            else cancellationFailure.addSuppressed(failure);
        }
        if (cancellationFailure != null) throw cancellationFailure;
        return new StopResult.Stopping(history());
    }

    public synchronized FinishResult finish(ActiveRun run) {
        requireCurrent(run);
        run.resources.cancel();
        RunLifecycle.FinishRunResult result = lifecycle.finishRun(run.handle);
        if (result instanceof RunLifecycle.FinishRunResult.Rejected(var reason)) {
            return new FinishResult.Pending(reason);
        }
        current = null;
        provisional = null;
        phase = disposed ? Phase.DISPOSED : Phase.IDLE;
        return new FinishResult.Finished(history(), phase);
    }

    public synchronized void dispose() {
        disposed = true;
        provisional = null;
        lifecycle.close();
        if (current == null) {
            phase = Phase.DISPOSED;
            return;
        }
        RuntimeException cancellationFailure = null;
        try {
            current.cancellation.cancel();
        } catch (RuntimeException failure) {
            cancellationFailure = failure;
        }
        try {
            current.resources.cancel();
        } catch (RuntimeException failure) {
            if (cancellationFailure == null) cancellationFailure = failure;
            else cancellationFailure.addSuppressed(failure);
        }
        phase = Phase.STOPPING;
        if (cancellationFailure != null) throw cancellationFailure;
    }

    private static RunMessage.Assistant assistant(ValidatedAssistantTurn turn) {
        return switch (turn) {
            case ValidatedAssistantTurn.Complete complete -> complete.assistant();
            case ValidatedAssistantTurn.LengthText length -> length.assistant();
            case ValidatedAssistantTurn.LengthCalls length -> length.assistant();
            case ValidatedAssistantTurn.Rejected ignored -> throw new IllegalArgumentException("Rejected turn has no assistant");
        };
    }

    private void beginBatch(ActiveRun run, List<PlannedCall> calls) {
        RunLifecycle.BeginBatchResult result = lifecycle.beginBatch(run.handle,
            Call.Batch.of(calls.stream().map(PlannedCall::id).toList()));
        if (!(result instanceof RunLifecycle.BeginBatchResult.Begun(var batch))) {
            throw new IllegalStateException("Validated call batch was rejected: "
                + ((RunLifecycle.BeginBatchResult.Rejected) result).reason());
        }
        run.batch = batch;
        run.plan = List.copyOf(calls);
        run.results.clear();
        run.nextResultIndex = 0;
    }

    private CallAdmission admission(ActiveRun run, PlannedCall call, boolean countsAsTool) {
        if (run.batch == null) throw new IllegalStateException("No current lifecycle batch");
        return new CallAdmission(lifecycle, run.batch, call.id(), run.budget, run.timeSource, countsAsTool);
    }

    private void appendResult(ActiveRun run, RunMessage.ToolResult result) {
        PlannedCall expected = nextResult(run);
        if (expected == null) throw new IllegalStateException("No unsettled call accepts a result");
        if (!expected.id().equals(result.callId())) throw new IllegalArgumentException("Out-of-order tool result");
        appendTerminalResult(run, expected, result);
    }

    private void appendCancelledInOrder(ActiveRun run) {
        while (true) {
            PlannedCall next = nextResult(run);
            if (next == null || admission(run, next, false).status() != Call.Status.CANCELLED_BEFORE_START) return;
            appendTerminalResult(run, next, new RunMessage.ToolResult(next.id(),
                new ToolOutcome.NotStarted(run.pendingReason, run.pendingMessage)));
        }
    }

    private PlannedCall nextResult(ActiveRun run) {
        return run.nextResultIndex < run.plan.size() ? run.plan.get(run.nextResultIndex) : null;
    }

    private void appendTerminalResult(ActiveRun run, PlannedCall expected, RunMessage.ToolResult result) {
        Call.Status status = admission(run, expected, false).status();
        if (!status.isTerminal()) throw new IllegalStateException("Tool result arrived before lifecycle settlement");
        if (run.results.putIfAbsent(result.callId(), result) != null) {
            throw new IllegalStateException("Duplicate tool result");
        }
        run.nextResultIndex++;
        accepted.add(result);
    }

    private Batch.Snapshot batchSnapshot(ActiveRun run) {
        RunLifecycle.BatchSnapshotResult result = lifecycle.batchSnapshot(run.batch);
        if (result instanceof RunLifecycle.BatchSnapshotResult.Available(var snapshot)) return snapshot;
        throw new IllegalStateException("Current lifecycle batch became stale");
    }

    private void finishLifecycle(ActiveRun run) {
        run.resources.cancel();
        RunLifecycle.FinishRunResult result = lifecycle.finishRun(run.handle);
        if (!(result instanceof RunLifecycle.FinishRunResult.Finished)) {
            throw new IllegalStateException("Terminal turn did not settle lifecycle");
        }
        current = null;
        phase = disposed ? Phase.DISPOSED : Phase.IDLE;
    }

    private void requireCurrent(ActiveRun run) {
        Objects.requireNonNull(run, "run");
        if (run.ownerKey != ownerKey || run != current) throw new IllegalArgumentException("Stale or foreign run");
    }

    private static void requireExecuted(CallAdmission.Result result) {
        if (!(result instanceof CallAdmission.Result.Executed)) {
            throw new IllegalStateException("Accounting callback was not executed: " + result);
        }
    }

    public enum Phase { IDLE, REQUESTING, EXECUTING_TOOLS, STOPPING, DISPOSED }

    public static final class ActiveRun {
        private final Object ownerKey;
        private final RunHandle handle;
        private final RunLimits.Budget budget;
        private final RunTimeSource timeSource;
        private final RunCancellation cancellation = new RunCancellation();
        private final RunResources resources = new RunResources();
        private Batch.Handle batch;
        private List<PlannedCall> plan = List.of();
        private int nextResultIndex;
        private final Map<Call.Id, RunMessage.ToolResult> results = new LinkedHashMap<>();

        private ToolOutcome.Reason pendingReason = ToolOutcome.Reason.CANCELLED_NOT_STARTED;
        private String pendingMessage = "Run stopped before tool admission";
        private ActiveRun(Object ownerKey, RunHandle handle, RunLimits.Budget budget, RunTimeSource timeSource) {
            this.ownerKey = ownerKey;
            this.handle = handle;
            this.budget = budget;
            this.timeSource = timeSource;
        }

        public RunCancellation cancellation() { return cancellation; }
        public RunResources resources() { return resources; }
        public RunLimits.Snapshot budget() { return budget.snapshot(); }
    }

    public record HistorySnapshot(List<RunMessage> messages) {
        public HistorySnapshot { messages = List.copyOf(Objects.requireNonNull(messages, "messages")); }
    }

    public sealed interface ProvisionalObservation {
        record Present(String text) implements ProvisionalObservation {
            public Present { Objects.requireNonNull(text, "text"); }
        }
        enum Absent implements ProvisionalObservation { INSTANCE }
    }

    public sealed interface StartResult {
        record Started(ActiveRun run) implements StartResult { public Started { Objects.requireNonNull(run); } }
        record Rejected(RunLifecycle.StartRejection reason) implements StartResult { public Rejected { Objects.requireNonNull(reason); } }
    }

    public sealed interface TurnAcceptance {
        record Ended(HistorySnapshot history) implements TurnAcceptance { public Ended { Objects.requireNonNull(history); } }
        record CallsReady() implements TurnAcceptance { }
        record ProtocolRejected(ValidatedAssistantTurn.ProtocolFailure failure, HistorySnapshot history) implements TurnAcceptance {
            public ProtocolRejected { Objects.requireNonNull(failure); Objects.requireNonNull(history); }
        }
    }

    public sealed interface CallStep {
        record Execute(PlannedCall.Executable call, CallAdmission admission) implements CallStep {
            public Execute { Objects.requireNonNull(call); Objects.requireNonNull(admission); }
        }
        record AccountRejected(PlannedCall.Rejected call, CallAdmission admission) implements CallStep {
            public AccountRejected { Objects.requireNonNull(call); Objects.requireNonNull(admission); }
        }
        record Complete() implements CallStep { }
        record Stopped() implements CallStep { }
    }

    public sealed interface StopResult {
        record Stopping(HistorySnapshot history) implements StopResult { public Stopping { Objects.requireNonNull(history); } }
    }

    public sealed interface FinishResult {
        record Finished(HistorySnapshot history, Phase phase) implements FinishResult {
            public Finished { Objects.requireNonNull(history); Objects.requireNonNull(phase); }
        }
        record Pending(RunLifecycle.FinishRejection reason) implements FinishResult {
            public Pending { Objects.requireNonNull(reason); }
        }
    }
}
