package com.github.catatafishen.agentbridge.nativeagent.run;

import java.time.Instant;

@FunctionalInterface
public interface RunTimeSource {
    Instant now();
}
