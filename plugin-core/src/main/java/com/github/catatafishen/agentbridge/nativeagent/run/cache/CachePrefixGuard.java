package com.github.catatafishen.agentbridge.nativeagent.run.cache;

import java.util.Objects;

public final class CachePrefixGuard {
    public Observation verify(CacheRequest previous, CacheRequest candidate) {
        Objects.requireNonNull(previous, "previous");
        Objects.requireNonNull(candidate, "candidate");
        if (!previous.generation().equals(candidate.generation())) {
            throw new IllegalArgumentException("Cache requests belong to different generations");
        }
        byte[] before = previous.ownedBytes();
        byte[] after = candidate.ownedBytes();
        int reused = commonPrefix(before, after);
        if (reused != before.length) {
            CacheRequest.ComponentRange range = reused < after.length
                ? candidate.componentAt(reused)
                : previous.componentAt(reused);
            throw new CachePrefixViolation(range.component(), range.field(), range.itemIndex(),
                reused, before.length, after.length, reused);
        }
        return new Observation(before.length, after.length, reused);
    }

    private static int commonPrefix(byte[] left, byte[] right) {
        int bound = Math.min(left.length, right.length);
        int index = 0;
        while (index < bound && left[index] == right[index]) index++;
        return index;
    }

    public record Observation(int previousBytes, int candidateBytes, int reusedBytes) {
        public Observation {
            if (previousBytes < 0 || candidateBytes < 0 || reusedBytes < 0 || reusedBytes > previousBytes) {
                throw new IllegalArgumentException("Invalid cache reuse observation");
            }
        }

        public double structuralReuse() {
            return previousBytes == 0 ? 1.0 : (double) reusedBytes / previousBytes;
        }
    }
}
