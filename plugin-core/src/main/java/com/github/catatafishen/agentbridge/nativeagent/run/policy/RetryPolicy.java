package com.github.catatafishen.agentbridge.nativeagent.run.policy;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

public final class RetryPolicy {
    private static final Duration DEFAULT_DELAY = Duration.ofSeconds(1);
    private static final Duration MAX_DELAY = Duration.ofSeconds(10);

    public Decision decide(Failure failure, boolean alreadyRetried) {
        Objects.requireNonNull(failure, "failure");
        if (alreadyRetried || !failure.kind().retryable()) return new Decision.NoRetry();
        if (failure.kind() != FailureKind.RATE_LIMIT) return new Decision.RetryAfter(DEFAULT_DELAY);
        if (failure.retryAfter().isEmpty()) return new Decision.RetryAfter(DEFAULT_DELAY);
        Duration requested = failure.retryAfter().orElseThrow();
        if (requested.isNegative() || requested.compareTo(MAX_DELAY) > 0) {
            return new Decision.RateLimitWait(failure.retryAfter());
        }
        return new Decision.RetryAfter(requested);
    }

    public record Failure(FailureKind kind, Optional<Duration> retryAfter) {
        public Failure {
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(retryAfter, "retryAfter");
            if (kind != FailureKind.RATE_LIMIT && retryAfter.isPresent()) {
                throw new IllegalArgumentException("Retry-After belongs only to rate-limit failures");
            }
        }
    }

    public enum FailureKind {
        TRANSIENT_TRANSPORT(true), EOF(true), RATE_LIMIT(true), SERVER(true),
        MALFORMED_CONTENT(false), UNSUPPORTED_MODEL(false), BAD_SCHEMA(false),
        BILLING_OR_QUOTA(false), CONTEXT_LIMIT(false), STOPPED(false), EXECUTED_CALL(false);

        private final boolean retryable;

        FailureKind(boolean retryable) { this.retryable = retryable; }
        boolean retryable() { return retryable; }
    }

    public sealed interface Decision {
        record RetryAfter(Duration delay) implements Decision {
            public RetryAfter {
                Objects.requireNonNull(delay, "delay");
                if (delay.isNegative() || delay.compareTo(MAX_DELAY) > 0) {
                    throw new IllegalArgumentException("Retry delay must be from zero through ten seconds");
                }
            }
        }
        record RateLimitWait(Optional<Duration> requested) implements Decision {
            public RateLimitWait { Objects.requireNonNull(requested, "requested"); }
        }
        record NoRetry() implements Decision { }
    }
}
