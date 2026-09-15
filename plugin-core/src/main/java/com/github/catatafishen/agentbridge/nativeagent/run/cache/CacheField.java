package com.github.catatafishen.agentbridge.nativeagent.run.cache;

import java.util.Arrays;
import java.util.Objects;

public final class CacheField {
    private final String name;
    private final byte[] bytes;

    public CacheField(String name, byte[] bytes) {
        this.name = Objects.requireNonNull(name, "name");
        if (name.isBlank()) throw new IllegalArgumentException("Cache field name must not be blank");
        this.bytes = Arrays.copyOf(Objects.requireNonNull(bytes, "bytes"), bytes.length);
        if (bytes.length == 0) throw new IllegalArgumentException("Cache field bytes must not be empty");
    }

    public String name() { return name; }
    public byte[] bytes() { return Arrays.copyOf(bytes, bytes.length); }
    int size() { return bytes.length; }
    void copyTo(byte[] target, int offset) { System.arraycopy(bytes, 0, target, offset, bytes.length); }
}
