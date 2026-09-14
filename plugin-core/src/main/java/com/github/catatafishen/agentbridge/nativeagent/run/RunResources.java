package com.github.catatafishen.agentbridge.nativeagent.run;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RunResources {
    private final List<Entry> entries = new ArrayList<>();
    private boolean cancelled;

    public Registration register(AutoCloseable resource) {
        Objects.requireNonNull(resource, "resource");
        Entry entry = new Entry(resource);
        synchronized (this) {
            if (!cancelled) {
                entries.add(entry);
                return new Registration(this, entry);
            }
        }
        entry.close();
        return new Registration(this, entry);
    }

    public void cancel() {
        List<Entry> snapshot;
        synchronized (this) {
            if (cancelled) return;
            cancelled = true;
            snapshot = List.copyOf(entries);
            entries.clear();
        }
        RuntimeException failure = null;
        for (Entry entry : snapshot) {
            try {
                entry.close();
            } catch (RuntimeException error) {
                if (failure == null) failure = error;
                else failure.addSuppressed(error);
            }
        }
        if (failure != null) throw failure;
    }

    public synchronized boolean isSettled() { return cancelled && entries.isEmpty(); }

    private void close(Entry entry) {
        synchronized (this) { entries.remove(entry); }
        entry.close();
    }

    public static final class Registration implements AutoCloseable {
        private final RunResources owner;
        private final Entry entry;
        private Registration(RunResources owner, Entry entry) { this.owner = owner; this.entry = entry; }
        @Override public void close() { owner.close(entry); }
    }

    private static final class Entry {
        private final AutoCloseable resource;
        private final AtomicBoolean closed = new AtomicBoolean();
        private Entry(AutoCloseable resource) { this.resource = resource; }
        private void close() {
            if (!closed.compareAndSet(false, true)) return;
            try {
                resource.close();
            } catch (RuntimeException error) {
                throw error;
            } catch (Exception error) {
                throw new ResourceCloseException(error);
            }
        }
    }

    public static final class ResourceCloseException extends RuntimeException {
        private ResourceCloseException(Exception cause) { super("Run resource close failed", cause); }
    }
}
