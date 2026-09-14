package com.github.catatafishen.agentbridge.nativeagent.run;

import java.util.List;
import java.util.Objects;

public sealed interface ToolOutcome {
    record Completed(String content, List<String> touchedPaths) implements ToolOutcome {
        public Completed {
            Objects.requireNonNull(content, "content");
            touchedPaths = paths(touchedPaths);
        }
    }

    record NotStarted(Reason reason, String content) implements ToolOutcome {
        public NotStarted {
            Objects.requireNonNull(reason, "reason");
            Objects.requireNonNull(content, "content");
            if (content.isBlank()) throw new IllegalArgumentException("Not-started content must not be blank");
        }
    }

    record FailedAfterStart(String content, List<String> touchedPaths) implements ToolOutcome {
        public FailedAfterStart {
            Objects.requireNonNull(content, "content");
            if (content.isBlank()) throw new IllegalArgumentException("Failure content must not be blank");
            touchedPaths = paths(touchedPaths);
        }
    }

    enum Reason {
        UNKNOWN_TOOL,
        INVALID_ARGUMENTS,
        TRUNCATED_NOT_EXECUTED,
        CANCELLED_NOT_STARTED,
        RUN_LIMIT_NOT_STARTED
    }

    private static List<String> paths(List<String> input) {
        List<String> copy = List.copyOf(Objects.requireNonNull(input, "touchedPaths"));
        if (copy.stream().anyMatch(String::isBlank)) {
            throw new IllegalArgumentException("Touched paths must not be blank");
        }
        return copy;
    }
}
