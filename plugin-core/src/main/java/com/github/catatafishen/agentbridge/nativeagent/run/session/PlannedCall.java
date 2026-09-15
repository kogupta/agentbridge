package com.github.catatafishen.agentbridge.nativeagent.run.session;

import com.github.catatafishen.agentbridge.nativeagent.lifecycle.Call;

import java.util.Objects;

public sealed interface PlannedCall {
    Call.Id id();

    record Executable(Call.Id id, ToolName tool, ToolOperation operation) implements PlannedCall {
        public Executable {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(tool, "tool");
            Objects.requireNonNull(operation, "operation");
        }
    }

    record Rejected(Call.Id id, CallError error) implements PlannedCall {
        public Rejected {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(error, "error");
        }
    }

    record ToolName(String value) {
        public ToolName {
            Objects.requireNonNull(value, "value");
            if (value.isBlank()) throw new IllegalArgumentException("Tool name must not be blank");
        }
    }

    record CallError(Code code, String message) {
        public CallError {
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(message, "message");
            if (message.isBlank()) throw new IllegalArgumentException("Call error message must not be blank");
        }

        public enum Code { UNKNOWN_TOOL, INVALID_ARGUMENTS }
    }

    interface ToolOperation { }
}
