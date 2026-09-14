package com.github.catatafishen.agentbridge.nativeagent.run;

import java.util.Objects;

public sealed interface ValidatedAssistantTurn {
    record Complete(RunMessage.Assistant assistant) implements ValidatedAssistantTurn {
        public Complete {
            requireCompletion(assistant, RunMessage.Completion.COMPLETE);
        }
    }

    record LengthText(RunMessage.Assistant assistant) implements ValidatedAssistantTurn {
        public LengthText {
            requireCompletion(assistant, RunMessage.Completion.INCOMPLETE);
            if (!assistant.calls().isEmpty()) throw new IllegalArgumentException("LengthText cannot contain calls");
        }
    }

    record LengthCalls(RunMessage.Assistant assistant) implements ValidatedAssistantTurn {
        public LengthCalls {
            requireCompletion(assistant, RunMessage.Completion.INCOMPLETE);
            if (assistant.calls().isEmpty()) throw new IllegalArgumentException("LengthCalls requires calls");
        }
    }

    record Rejected(ProtocolFailure failure, String provisionalText) implements ValidatedAssistantTurn {
        public Rejected {
            Objects.requireNonNull(failure, "failure");
            Objects.requireNonNull(provisionalText, "provisionalText");
        }
    }

    enum ProtocolFailure {
        MALFORMED_JSON,
        DUPLICATE_CALL_ID,
        MALFORMED_CALL_FRAGMENT,
        UNSUPPORTED_REQUIRED_OUTPUT
    }

    private static void requireCompletion(RunMessage.Assistant assistant, RunMessage.Completion expected) {
        Objects.requireNonNull(assistant, "assistant");
        if (assistant.completion() != expected) {
            throw new IllegalArgumentException("Assistant completion must be " + expected);
        }
    }
}
