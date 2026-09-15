package com.github.catatafishen.agentbridge.nativeagent.run.cache;

import java.util.Objects;
import java.util.OptionalInt;

public final class CachePrefixViolation extends IllegalStateException {
    private final CacheRequest.Component component;
    private final String field;
    private final OptionalInt itemIndex;
    private final int byteOffset;
    private final int previousBytes;
    private final int candidateBytes;
    private final int reusedBytes;

    CachePrefixViolation(CacheRequest.Component component, String field, OptionalInt itemIndex,
                         int byteOffset, int previousBytes, int candidateBytes, int reusedBytes) {
        super("Cache prefix diverged at " + component + "/" + field + " byte " + byteOffset);
        this.component = Objects.requireNonNull(component, "component");
        this.field = Objects.requireNonNull(field, "field");
        this.itemIndex = Objects.requireNonNull(itemIndex, "itemIndex");
        this.byteOffset = byteOffset;
        this.previousBytes = previousBytes;
        this.candidateBytes = candidateBytes;
        this.reusedBytes = reusedBytes;
    }

    public CacheRequest.Component component() { return component; }
    public String field() { return field; }
    public OptionalInt itemIndex() { return itemIndex; }
    public int byteOffset() { return byteOffset; }
    public int previousBytes() { return previousBytes; }
    public int candidateBytes() { return candidateBytes; }
    public int reusedBytes() { return reusedBytes; }
}
