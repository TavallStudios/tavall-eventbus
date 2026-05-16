package org.tavall.internal.event.tags.interfaces;

import org.tavall.platform.global.abstracts.AbstractEvent;

public interface SuppressibleEvent {

    /**
     * Returns true if this event should be suppressed (not fired).
     */
    boolean shouldSuppress();

    /**
     * Marks the event as suppressed.
     */
    void markSuppressed();

    /**
     * Marks the event as suppressed for a set number of seconds.
     * Runs asynchronously.
     *
     * @param seconds Time in seconds to suppress event.
     */
    void markSuppressed(int seconds);

    /**
     * Resets the suppression flag and cancels any scheduled reset task.
     */
    void resetSuppression();

    /**
     * Returns true if this event supports suppression logic.
     */
    boolean isSuppressible();

    /**
     * Enables or disables suppression support for this event.
     *
     * @return
     */
    AbstractEvent setSuppressible(boolean suppressible);
}
