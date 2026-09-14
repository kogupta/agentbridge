package com.github.catatafishen.agentbridge.nativeagent.run;

import com.github.catatafishen.agentbridge.nativeagent.lifecycle.Call;

import java.util.List;
import java.util.Objects;

public sealed interface RunMessage {
    record User(String content) implements RunMessage {
        public User {
            Objects.requireNonNull(content, "content");
            if (content.isBlank()) throw new IllegalArgumentException("User message must not be blank");
        }
    }

    record Assistant(String text, Completion completion, List<PlannedCall> calls) implements RunMessage {
        public Assistant {
            Objects.requireNonNull(text, "text");
            Objects.requireNonNull(completion, "completion");
            calls = List.copyOf(Objects.requireNonNull(calls, "calls"));
            long distinctIds = calls.stream().map(PlannedCall::id).distinct().count();
            if (distinctIds != calls.size()) throw new IllegalArgumentException("Assistant call IDs must be distinct");
        }
    }

    record ToolResult(Call.Id callId, ToolOutcome outcome) implements RunMessage {
        public ToolResult {
            Objects.requireNonNull(callId, "callId");
            Objects.requireNonNull(outcome, "outcome");
        }
    }

    enum Completion { COMPLETE, INCOMPLETE }
}
