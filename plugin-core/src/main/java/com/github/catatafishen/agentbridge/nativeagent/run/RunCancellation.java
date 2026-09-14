package com.github.catatafishen.agentbridge.nativeagent.run;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RunCancellation {
    private final List<Entry> listeners = new ArrayList<>();
    private boolean cancelled;

    public synchronized boolean isCancelled() { return cancelled; }

    public Registration onCancel(Runnable listener) {
        Objects.requireNonNull(listener, "listener");
        Entry entry = new Entry(listener);
        synchronized (this) {
            if (!cancelled) {
                listeners.add(entry);
                return new Registration(this, entry);
            }
        }
        entry.run();
        return new Registration(this, entry);
    }

    public void cancel() {
        List<Entry> snapshot;
        synchronized (this) {
            if (cancelled) return;
            cancelled = true;
            snapshot = List.copyOf(listeners);
            listeners.clear();
        }
        RuntimeException failure = null;
        for (Entry entry : snapshot) {
            try {
                entry.run();
            } catch (RuntimeException error) {
                if (failure == null) failure = error;
                else failure.addSuppressed(error);
            }
        }
        if (failure != null) throw failure;
    }

    private synchronized void remove(Entry entry) { listeners.remove(entry); }

    public static final class Registration implements AutoCloseable {
        private final RunCancellation owner;
        private final Entry entry;
        private Registration(RunCancellation owner, Entry entry) { this.owner = owner; this.entry = entry; }
        @Override public void close() { owner.remove(entry); }
    }

    private static final class Entry {
        private final Runnable listener;
        private final AtomicBoolean ran = new AtomicBoolean();
        private Entry(Runnable listener) { this.listener = listener; }
        private void run() { if (ran.compareAndSet(false, true)) listener.run(); }
    }
}
