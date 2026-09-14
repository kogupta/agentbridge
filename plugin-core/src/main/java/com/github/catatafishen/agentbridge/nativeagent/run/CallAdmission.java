package com.github.catatafishen.agentbridge.nativeagent.run;

import com.github.catatafishen.agentbridge.nativeagent.lifecycle.Batch;
import com.github.catatafishen.agentbridge.nativeagent.lifecycle.Call;
import com.github.catatafishen.agentbridge.nativeagent.lifecycle.Effect;
import com.github.catatafishen.agentbridge.nativeagent.lifecycle.RunLifecycle;

import java.util.Objects;

public final class CallAdmission {
    private final RunLifecycle lifecycle;
    private final Batch.Handle batch;
    private final Call.Id call;
    private final RunLimits.Budget budget;
    private final RunTimeSource timeSource;
    private final boolean countsAsTool;

    CallAdmission(RunLifecycle lifecycle, Batch.Handle batch, Call.Id call,
                  RunLimits.Budget budget, RunTimeSource timeSource, boolean countsAsTool) {
        this.lifecycle = Objects.requireNonNull(lifecycle, "lifecycle");
        this.batch = Objects.requireNonNull(batch, "batch");
        this.call = Objects.requireNonNull(call, "call");
        this.budget = Objects.requireNonNull(budget, "budget");
        this.timeSource = Objects.requireNonNull(timeSource, "timeSource");
        this.countsAsTool = countsAsTool;
    }

    public Call.Id call() { return call; }

    public Result execute(Effect effect) {
        Objects.requireNonNull(effect, "effect");
        Holder holder = new Holder();
        RunLifecycle.ExecutionResult execution = lifecycle.execute(batch, call, () -> {
            if (countsAsTool) {
                holder.budgetAdmission = budget.admitTool(Objects.requireNonNull(timeSource.now(), "timeSource.now()"));
                if (holder.budgetAdmission != RunLimits.Admission.ADMITTED) return;
            }
            holder.effectStarted = true;
            effect.execute();
        });
        if (execution instanceof RunLifecycle.ExecutionResult.Rejected rejected) {
            return new Result.Rejected(rejected.reason());
        }
        if (holder.budgetAdmission != null && holder.budgetAdmission != RunLimits.Admission.ADMITTED) {
            return new Result.Limited(holder.budgetAdmission);
        }
        if (!holder.effectStarted) throw new IllegalStateException("Lifecycle executed without accounting or effect");
        return new Result.Executed();
    }

    public Call.Status status() {
        RunLifecycle.BatchSnapshotResult result = lifecycle.batchSnapshot(batch);
        if (!(result instanceof RunLifecycle.BatchSnapshotResult.Available available)) {
            throw new IllegalStateException("Call admission batch is stale");
        }
        return available.snapshot().calls().stream()
            .filter(snapshot -> snapshot.call().equals(call))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Call admission identity is absent"))
            .status();
    }

    public sealed interface Result {
        record Executed() implements Result { }
        record Limited(RunLimits.Admission reason) implements Result {
            public Limited {
                Objects.requireNonNull(reason, "reason");
                if (reason == RunLimits.Admission.ADMITTED) throw new IllegalArgumentException("Limited requires refusal");
            }
        }
        record Rejected(RunLifecycle.ExecutionRejection reason) implements Result {
            public Rejected { Objects.requireNonNull(reason, "reason"); }
        }
    }

    private static final class Holder {
        private RunLimits.Admission budgetAdmission;
        private boolean effectStarted;
    }
}
