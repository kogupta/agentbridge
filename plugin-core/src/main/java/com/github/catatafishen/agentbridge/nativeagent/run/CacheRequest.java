package com.github.catatafishen.agentbridge.nativeagent.run;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;

public final class CacheRequest {
    private final CacheGeneration.Id generation;
    private final byte[] bytes;
    private final List<ComponentRange> components;

    CacheRequest(CacheGeneration.Id generation, List<Part> parts) {
        this.generation = Objects.requireNonNull(generation, "generation");
        List<Part> copy = List.copyOf(Objects.requireNonNull(parts, "parts"));
        int length = copy.stream().mapToInt(part -> part.field().size()).sum();
        byte[] bytes = new byte[length];
        List<ComponentRange> ranges = new ArrayList<>(copy.size());
        appendFields(copy, bytes, ranges, 0);
        this.bytes = bytes;
        this.components = List.copyOf(ranges);
    }

    static CacheRequest extend(CacheRequest previous, List<Part> appended) {
        Objects.requireNonNull(previous, "previous");
        List<Part> tail = List.copyOf(Objects.requireNonNull(appended, "appended"));
        int base = previous.bytes.length;
        int length = base + tail.stream().mapToInt(part -> part.field().size()).sum();
        byte[] bytes = new byte[length];
        System.arraycopy(previous.bytes, 0, bytes, 0, base);
        List<ComponentRange> ranges = new ArrayList<>(previous.components.size() + tail.size());
        ranges.addAll(previous.components);
        appendFields(tail, bytes, ranges, base);
        return new CacheRequest(previous.generation, bytes, ranges);
    }

    private CacheRequest(CacheGeneration.Id generation, byte[] bytes, List<ComponentRange> components) {
        this.generation = generation;
        this.bytes = bytes;
        this.components = List.copyOf(components);
    }

    private static void appendFields(List<Part> parts, byte[] bytes, List<ComponentRange> ranges, int offset) {
        for (Part part : parts) {
            int end = offset + part.field().size();
            part.field().copyTo(bytes, offset);
            ranges.add(new ComponentRange(part.component(), part.field().name(), part.itemIndex(), offset, end));
            offset = end;
        }
    }

    public CacheGeneration.Id generation() { return generation; }
    public byte[] bytes() { return Arrays.copyOf(bytes, bytes.length); }
    public int length() { return bytes.length; }
    public List<ComponentRange> components() { return components; }

    ComponentRange componentAt(int offset) {
        if (components.isEmpty()) throw new IllegalStateException("Cache request has no components");
        return components.stream().filter(range -> offset >= range.startByte() && offset < range.endByte())
            .findFirst().orElse(components.getLast());
    }

    byte[] ownedBytes() { return bytes; }

    public enum Component { HEAD, INPUT }

    public record ComponentRange(Component component, String field, OptionalInt itemIndex,
                                 int startByte, int endByte) {
        public ComponentRange {
            Objects.requireNonNull(component, "component");
            Objects.requireNonNull(field, "field");
            Objects.requireNonNull(itemIndex, "itemIndex");
            if (field.isBlank() || startByte < 0 || endByte <= startByte) {
                throw new IllegalArgumentException("Invalid cache component range");
            }
        }
    }

    record Part(Component component, CacheField field, OptionalInt itemIndex) {
        Part {
            Objects.requireNonNull(component, "component");
            Objects.requireNonNull(field, "field");
            Objects.requireNonNull(itemIndex, "itemIndex");
        }
    }
}
