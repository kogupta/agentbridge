package com.github.catatafishen.agentbridge.nativeagent.lifecycle;

public enum CallStatus {
    PENDING,
    EXECUTING,
    COMPLETED,
    FAILED_AFTER_START,
    CANCELLED_BEFORE_START;

    public boolean isTerminal() {
        return switch (this) {
            case COMPLETED, FAILED_AFTER_START, CANCELLED_BEFORE_START -> true;
            case PENDING, EXECUTING -> false;
        };
    }
}
