package org.tavall.event;

import org.tavall.enums.EventPriority;

/**
 * Handle representing an active registration of an {@link EventHandler}.
 * Allows inspection of priority and lifecycle unregistration.
 *
 * @param <E> the event payload type
 */
public interface EventSubscription<E> {

    /**
     * @return the payload class handled by this subscription
     */
    Class<E> getPayloadType();

    /**
     * @return the execution priority of this handler
     */
    EventPriority getPriority();

    /**
     * @return the owning object (e.g. module or service instance) or null
     */
    Object getOwner();

    /**
     * @return true if this subscription is still active and registered
     */
    boolean isActive();

    /**
     * Unregisters this handler from its router.
     */
    void unsubscribe();
}
