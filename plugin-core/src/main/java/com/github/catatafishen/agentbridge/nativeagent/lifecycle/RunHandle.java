package com.github.catatafishen.agentbridge.nativeagent.lifecycle;

public final class RunHandle {
    private final Object ownerKey;

    RunHandle(Object ownerKey) {
        this.ownerKey = ownerKey;
    }

    boolean belongsTo(Object expectedOwnerKey) {
        return ownerKey == expectedOwnerKey;
    }
}
