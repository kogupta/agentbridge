package com.github.catatafishen.agentbridge.nativeagent.run.resources;

import java.time.Instant;

@FunctionalInterface
public interface RunTimeSource {
    Instant now();
}
