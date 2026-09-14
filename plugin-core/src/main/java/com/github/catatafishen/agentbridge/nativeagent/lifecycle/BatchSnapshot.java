package com.github.catatafishen.agentbridge.nativeagent.lifecycle;

import java.util.List;
import java.util.Objects;

public record BatchSnapshot(List<CallSnapshot> calls) {
    public BatchSnapshot {
        List<CallSnapshot> snapshot = List.copyOf(Objects.requireNonNull(calls, "calls"));
        if (snapshot.isEmpty()) {
            throw new IllegalArgumentException("BatchSnapshot must not be empty");
        }
        calls = snapshot;
    }

    public boolean isSettled() {
        return calls.stream().allMatch(call -> call.status().isTerminal());
    }
}
