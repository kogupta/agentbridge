package com.github.catatafishen.agentbridge.nativeagent.lifecycle;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public final class Call {
    private Call() {}

    public static final class Batch {
        private final List<Id> calls;

        private Batch(List<Id> calls) {
            this.calls = calls;
        }

        public static Batch of(List<Id> calls) {
            Objects.requireNonNull(calls, "calls");
            List<Id> snapshot = List.copyOf(calls);
            if (snapshot.isEmpty()) {
                throw new IllegalArgumentException("CallBatch must not be empty");
            }
            if (new HashSet<>(snapshot).size() != snapshot.size()) {
                throw new IllegalArgumentException("CallBatch CallIds must be distinct");
            }
            return new Batch(snapshot);
        }

        public List<Id> calls() {
            return calls;
        }
    }

    public record Id(String value) {
        public Id {
            Objects.requireNonNull(value, "value");
            if (value.isBlank()) {
                throw new IllegalArgumentException("CallId must not be blank");
            }
        }
    }

    public record Snapshot(Id call, Status status) {
        public Snapshot {
            Objects.requireNonNull(call, "call");
            Objects.requireNonNull(status, "status");
        }
    }

    public enum Status {
        PENDING, EXECUTING, COMPLETED,
        FAILED_AFTER_START, CANCELLED_BEFORE_START;

        public boolean isTerminal() {
            return switch (this) {
                case COMPLETED, FAILED_AFTER_START, CANCELLED_BEFORE_START -> true;
                case PENDING, EXECUTING -> false;
            };
        }
    }
}
