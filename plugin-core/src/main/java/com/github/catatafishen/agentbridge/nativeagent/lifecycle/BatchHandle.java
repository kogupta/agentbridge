package com.github.catatafishen.agentbridge.nativeagent.lifecycle;

public final class BatchHandle {
    private final Object ownerKey;
    private final RunHandle run;

    BatchHandle(Object ownerKey, RunHandle run) {
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
