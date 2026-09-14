package com.github.catatafishen.agentbridge.nativeagent.lifecycle;

/**
 * Synchronous side effect invoked by {@link RunLifecycle#execute} after admission, outside the owner monitor.
 *
 * <p>Normal return records {@code COMPLETED}. Any throwable, including a checked exception raised from Kotlin
 * or through a generic rethrow, records {@code FAILED_AFTER_START} and is rethrown as the same object. The
 * driver decides whether a failed call becomes a tool error result and whether the run continues.
 *
 * <p>Do not queue or detach work that outlives this call: the owner treats return as settlement. The effect
 * receives no stop signal. The driver owns any cooperative cancellation token and must cancel it together with
 * {@link RunLifecycle#stop}. Inside the callback, {@link RunLifecycle#snapshot()} reports {@code STOPPING} or
 * {@code CLOSING} once Stop or close has won.
 */
@FunctionalInterface
public interface Effect {
    void execute();
}
