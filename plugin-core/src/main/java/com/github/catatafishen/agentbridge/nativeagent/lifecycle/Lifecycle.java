package com.github.catatafishen.agentbridge.nativeagent.lifecycle;

import com.github.catatafishen.agentbridge.nativeagent.lifecycle.Lifecycle.Snapshot.Closed;
import com.github.catatafishen.agentbridge.nativeagent.lifecycle.Lifecycle.Snapshot.Idle;

import java.util.Objects;

public final class Lifecycle {
    private Lifecycle() {}

    public enum Phase {
        IDLE, RUNNING, STOPPING, CLOSING, CLOSED
    }

    public static Snapshot idle() {return Idle.Instance;}
    public static Snapshot closed() {return Closed.Instance;}

    public sealed interface Snapshot {
        Phase phase();

        enum Idle implements Snapshot {
            Instance;

            @Override
            public Phase phase() {return Phase.IDLE;}
        }

        record Active(Phase phase) implements Snapshot {
            public Active {
                Objects.requireNonNull(phase, "phase");
                if (phase != Phase.RUNNING && phase != Phase.STOPPING && phase != Phase.CLOSING) {
                    throw new IllegalArgumentException("Active snapshot requires an active phase");
                }
            }
        }

        enum Closed implements Snapshot {
            Instance;

            @Override
            public Phase phase() {return Phase.CLOSED;}
        }
    }
}
