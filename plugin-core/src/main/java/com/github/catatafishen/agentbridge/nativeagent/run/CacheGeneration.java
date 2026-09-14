package com.github.catatafishen.agentbridge.nativeagent.run;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

public final class CacheGeneration {
    private final Id id;
    private final PrefixFingerprint fingerprint;
    private final CacheReset reset;
    private final List<RunMessage> accepted = new ArrayList<>();
    private final CachePrefixGuard guard = new CachePrefixGuard();
    private CacheRequest current;
    private CachePrefixGuard.Observation lastObservation;

    private CacheGeneration(Id id, CacheReset reset, List<CacheField> head) {
        this.id = Objects.requireNonNull(id, "id");
        this.reset = Objects.requireNonNull(reset, "reset");
        List<CacheField> fields = List.copyOf(Objects.requireNonNull(head, "head"));
        if (fields.isEmpty()) throw new IllegalArgumentException("Cache request head must not be empty");
        List<CacheRequest.Part> headParts = new ArrayList<>(fields.size());
        fields.forEach(field -> headParts.add(new CacheRequest.Part(
            CacheRequest.Component.HEAD, field, OptionalInt.empty())));
        this.current = new CacheRequest(id, headParts);
        this.fingerprint = PrefixFingerprint.from(current.ownedBytes());
    }

    public static CacheGeneration initial(Id id, List<CacheField> head) {
        return new CacheGeneration(id, CacheReset.SessionStart.INSTANCE, head);
    }

    public static CacheGeneration replacement(RunSession previousSession, CacheReset reset,
                                              Id id, List<CacheField> head) {
        Objects.requireNonNull(previousSession, "previousSession");
        Objects.requireNonNull(reset, "reset");
        if (reset == CacheReset.SessionStart.INSTANCE) {
            throw new IllegalArgumentException("SessionStart is only valid for the initial generation");
        }
        if (previousSession.phase() != RunSession.Phase.IDLE) {
            throw new IllegalStateException("Cache generation replacement requires an idle session");
        }
        return new CacheGeneration(id, reset, head);
    }

    public synchronized CacheRequest requestFor(RunSession.HistorySnapshot history,
                                                ModelItemRenderer renderer) {
        Objects.requireNonNull(history, "history");
        Objects.requireNonNull(renderer, "renderer");
        List<RunMessage> messages = history.messages();
        if (messages.size() < accepted.size()
            || !messages.subList(0, accepted.size()).equals(accepted)) {
            throw new IllegalStateException("Accepted model context is not append-only");
        }
        if (messages.size() == accepted.size()) return current;
        List<CacheRequest.Part> appended = new ArrayList<>();
        for (int index = accepted.size(); index < messages.size(); index++) {
            RunMessage message = messages.get(index);
            List<CacheField> rendered = List.copyOf(Objects.requireNonNull(
                renderer.render(message, index), "renderer result"));
            if (rendered.isEmpty()) throw new IllegalArgumentException("Accepted model item rendered no fields");
            for (CacheField field : rendered) {
                appended.add(new CacheRequest.Part(CacheRequest.Component.INPUT, field, OptionalInt.of(index)));
            }
        }
        CacheRequest candidate = CacheRequest.extend(current, appended);
        CachePrefixGuard.Observation observation = guard.verify(current, candidate);
        accepted.addAll(messages.subList(accepted.size(), messages.size()));
        lastObservation = observation;
        current = candidate;
        return candidate;
    }

    public Id id() { return id; }
    public PrefixFingerprint fingerprint() { return fingerprint; }
    public CacheReset reset() { return reset; }
    public synchronized Optional<CachePrefixGuard.Observation> lastObservation() {
        return Optional.ofNullable(lastObservation);
    }

    public record Id(String value) {
        public Id {
            Objects.requireNonNull(value, "value");
            if (value.isBlank()) throw new IllegalArgumentException("Cache generation ID must not be blank");
        }
    }

    public static final class PrefixFingerprint {
        private final byte[] digest;
        private PrefixFingerprint(byte[] digest) { this.digest = digest; }
        static PrefixFingerprint from(byte[] input) {
            try {
                return new PrefixFingerprint(MessageDigest.getInstance("SHA-256").digest(input));
            } catch (NoSuchAlgorithmException impossible) {
                throw new AssertionError(impossible);
            }
        }
        public byte[] digest() { return Arrays.copyOf(digest, digest.length); }
        @Override public boolean equals(Object other) {
            return other instanceof PrefixFingerprint fingerprint && Arrays.equals(digest, fingerprint.digest);
        }
        @Override public int hashCode() { return Arrays.hashCode(digest); }
    }

    public sealed interface CacheReset {
        enum SessionStart implements CacheReset { INSTANCE }
        record ModelChange(String previousModel, String nextModel) implements CacheReset {
            public ModelChange {
                Objects.requireNonNull(previousModel, "previousModel");
                Objects.requireNonNull(nextModel, "nextModel");
                if (previousModel.isBlank() || nextModel.isBlank() || previousModel.equals(nextModel)) {
                    throw new IllegalArgumentException("ModelChange requires distinct nonblank models");
                }
            }
        }
        enum ExplicitContextReset implements CacheReset { INSTANCE }
    }
}
