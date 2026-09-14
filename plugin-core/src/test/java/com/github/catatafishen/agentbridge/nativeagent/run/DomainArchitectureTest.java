package com.github.catatafishen.agentbridge.nativeagent.run;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DomainArchitectureTest {
    @Test
    void domainBoundaryHasNoJsonPlatformOrCoroutineTypes() throws IOException {
        List<Class<?>> domainTypes = List.of(
            CacheField.class,
            CacheGeneration.class,
            CachePrefixGuard.class,
            CachePrefixViolation.class,
            CacheRequest.class,
            CallAdmission.class,
            PlannedCall.class,
            RetryPolicy.class,
            RunCancellation.class,
            RunLimits.class,
            RunMessage.class,
            RunResources.class,
            RunSession.class,
            ModelItemRenderer.class,
            RunTimeSource.class,
            ToolOutcome.class,
            ValidatedAssistantTurn.class
        );
        List<String> forbidden = List.of("com/intellij/", "com/google/gson/", "org/json/", "kotlinx/coroutines/");
        for (Class<?> type : domainTypes) {
            String resource = "/" + type.getName().replace('.', '/') + ".class";
            try (var stream = type.getResourceAsStream(resource)) {
                assertNotNull(stream, resource);
                String constantPool = new String(stream.readAllBytes(), StandardCharsets.ISO_8859_1);
                for (String prefix : forbidden) {
                    assertFalse(constantPool.contains(prefix), () -> type.getName() + " depends on " + prefix);
                }
            }
        }
    }
}
