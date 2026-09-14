package com.github.catatafishen.agentbridge.nativeagent.lifecycle;

import java.util.Objects;

public record CallSnapshot(CallId call, CallStatus status) {
    public CallSnapshot {
        Objects.requireNonNull(call, "call");
        Objects.requireNonNull(status, "status");
    }
}
