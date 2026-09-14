package com.github.catatafishen.agentbridge.nativeagent.lifecycle;

import java.util.Objects;

public sealed interface BatchObservation {
    record Present(BatchSnapshot snapshot) implements BatchObservation {
        public Present {
            Objects.requireNonNull(snapshot, "snapshot");
        }
    }

    record Absent() implements BatchObservation {
    }
}
