package com.github.catatafishen.agentbridge.nativeagent.run.policy;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public record RunLimits(int maxResponses, int maxToolInvocations, Duration maxDuration) {
    public RunLimits {
        if (maxResponses < 1) throw new IllegalArgumentException("maxResponses must be positive");
        if (maxToolInvocations < 1) throw new IllegalArgumentException("maxToolInvocations must be positive");
        Objects.requireNonNull(maxDuration, "maxDuration");
        if (maxDuration.isZero() || maxDuration.isNegative()) {
            throw new IllegalArgumentException("maxDuration must be positive");
        }
    }

    public static RunLimits mvp() {
        return new RunLimits(20, 100, Duration.ofMinutes(15));
    }

    public static final class Budget {
        private final RunLimits limits;
        private final Instant startedAt;
        private int responses;
        private int toolInvocations;

        public Budget(RunLimits limits, Instant startedAt) {
            this.limits = Objects.requireNonNull(limits, "limits");
            this.startedAt = Objects.requireNonNull(startedAt, "startedAt");
        }

        public synchronized Admission admitRequest(Instant now) {
            return deadlineReached(now) ? Admission.DEADLINE_REACHED
                : responses >= limits.maxResponses ? Admission.RESPONSE_LIMIT_REACHED
                : Admission.ADMITTED;
        }

        public synchronized void recordAcceptedResponse() {
            if (responses >= limits.maxResponses) throw new IllegalStateException("Response limit already reached");
            responses++;
        }

        public synchronized Admission admitTool(Instant now) {
            if (deadlineReached(now)) return Admission.DEADLINE_REACHED;
            if (toolInvocations >= limits.maxToolInvocations) return Admission.TOOL_LIMIT_REACHED;
            toolInvocations++;
            return Admission.ADMITTED;
        }

        public synchronized Snapshot snapshot() {
            return new Snapshot(responses, toolInvocations, startedAt, limits);
        }

        private boolean deadlineReached(Instant now) {
            Objects.requireNonNull(now, "now");
            if (now.isBefore(startedAt)) throw new IllegalArgumentException("Clock moved before run start");
            return !now.isBefore(startedAt.plus(limits.maxDuration));
        }
    }

    public enum Admission { ADMITTED, RESPONSE_LIMIT_REACHED, TOOL_LIMIT_REACHED, DEADLINE_REACHED }

    public record Snapshot(int responses, int toolInvocations, Instant startedAt, RunLimits limits) {
        public Snapshot {
            Objects.requireNonNull(startedAt, "startedAt");
            Objects.requireNonNull(limits, "limits");
        }
    }
}
