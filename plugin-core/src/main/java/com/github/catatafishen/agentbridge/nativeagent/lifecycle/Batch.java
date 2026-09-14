package com.github.catatafishen.agentbridge.nativeagent.lifecycle;

import java.util.List;
import java.util.Objects;

public final class Batch {
    private Batch() {}

    public static final class Handle {
        private final Object ownerKey;
        private final RunHandle run;

        Handle(Object ownerKey, RunHandle run) {
            this.ownerKey = ownerKey;
            this.run = run;
        }

        boolean belongsTo(Object expectedOwnerKey) {
            return ownerKey == expectedOwnerKey;
        }

        RunHandle run() {
            return run;
        }
    }

    public sealed interface Observation {
        record Present(Snapshot snapshot) implements Observation {
            public Present {
                Objects.requireNonNull(snapshot, "snapshot");
            }
        }

        enum Absent implements Observation {
            Instance
        }

        static Observation absent() {
            return Absent.Instance;
        }
    }

    public record Snapshot(List<Call.Snapshot> calls) {
        public Snapshot {
            List<Call.Snapshot> snapshot = List.copyOf(Objects.requireNonNull(calls, "calls"));
            if (snapshot.isEmpty()) {
                throw new IllegalArgumentException("BatchSnapshot must not be empty");
            }
            calls = snapshot;
        }

        public boolean isSettled() {
            return calls.stream().allMatch(call -> call.status().isTerminal());
        }
    }
}
