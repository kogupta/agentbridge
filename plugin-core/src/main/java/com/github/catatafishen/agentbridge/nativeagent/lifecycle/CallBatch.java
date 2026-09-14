package com.github.catatafishen.agentbridge.nativeagent.lifecycle;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public final class CallBatch {
    private final List<CallId> calls;

    private CallBatch(List<CallId> calls) {
        this.calls = calls;
    }

    public static CallBatch of(List<CallId> calls) {
        Objects.requireNonNull(calls, "calls");
        List<CallId> snapshot = List.copyOf(calls);
        if (snapshot.isEmpty()) {
            throw new IllegalArgumentException("CallBatch must not be empty");
        }
        if (new HashSet<>(snapshot).size() != snapshot.size()) {
            throw new IllegalArgumentException("CallBatch CallIds must be distinct");
        }
        return new CallBatch(snapshot);
    }

    public List<CallId> calls() {
        return calls;
    }
}
