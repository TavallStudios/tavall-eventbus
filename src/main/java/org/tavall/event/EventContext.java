package org.tavall.event;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Execution context for a specific event dispatch invocation.
 * Carries dispatch metadata, cancellation state, and dynamic pipeline attributes.
 */
public final class EventContext {

    private final Instant createdAt = Instant.now();
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final boolean cancellable;
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();

    public EventContext(boolean cancellable) {
        this.cancellable = cancellable;
    }

    public boolean isCancellable() {
        return cancellable;
    }

    public boolean isCancelled() {
        return cancelled.get();
    }

    /**
     * Attempts to cancel further processing of this event.
     *
     * @return true if the event was successfully cancelled, false if it was already cancelled
     * @throws UnsupportedOperationException if the event is non-cancellable
     */
    public boolean cancel() {
        if (!cancellable) {
            throw new UnsupportedOperationException("Event is marked non-cancellable by its EventSettings");
        }
        return cancelled.compareAndSet(false, true);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public EventContext setAttribute(String key, Object value) {
        Objects.requireNonNull(key, "key cannot be null");
        if (value == null) {
            attributes.remove(key);
        } else {
            attributes.put(key, value);
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        Objects.requireNonNull(key, "key cannot be null");
        return (T) attributes.get(key);
    }

    public Map<String, Object> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }
}
