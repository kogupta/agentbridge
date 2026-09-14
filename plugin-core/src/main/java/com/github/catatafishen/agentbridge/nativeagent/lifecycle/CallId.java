package com.github.catatafishen.agentbridge.nativeagent.lifecycle;

import java.util.Objects;

public record CallId(String value) {
    public CallId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("CallId must not be blank");
        }
    }
}
