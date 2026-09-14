package com.github.catatafishen.agentbridge.nativeagent.lifecycle;

import java.util.Objects;

public sealed interface LifecycleSnapshot {
    LifecyclePhase phase();

    record Idle() implements LifecycleSnapshot {
        @Override
        public LifecyclePhase phase() {
            return LifecyclePhase.IDLE;
        }
    }

    record Active(LifecyclePhase phase) implements LifecycleSnapshot {
        public Active {
            Objects.requireNonNull(phase, "phase");
            if (phase != LifecyclePhase.RUNNING
                && phase != LifecyclePhase.STOPPING
                && phase != LifecyclePhase.CLOSING) {
                throw new IllegalArgumentException("Active snapshot requires an active phase");
            }
        }
    }

    record Closed() implements LifecycleSnapshot {
        @Override
        public LifecyclePhase phase() {
            return LifecyclePhase.CLOSED;
        }
    }
}
