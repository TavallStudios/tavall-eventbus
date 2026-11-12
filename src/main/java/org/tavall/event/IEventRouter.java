package org.tavall.event;

import org.tavall.enums.EventPriority;

import java.util.concurrent.CompletableFuture;

/**
 * Service interface owning the runtime registration, ordering, and dispatch of typed events.
 */
public interface IEventRouter {

    /**
     * Registers a handler for the given event definition with default priority and no owner.
     */
    <E> EventSubscription<E> register(TavallEvent<E> eventDefinition, EventHandler<E> handler);

    /**
     * Registers a handler for the given event definition with default priority and a specified owner.
     */
    <E> EventSubscription<E> register(TavallEvent<E> eventDefinition, Object owner, EventHandler<E> handler);

    /**
     * Registers a handler for the given event definition with an explicit priority and owner.
     */
    <E> EventSubscription<E> register(TavallEvent<E> eventDefinition, Object owner, EventPriority priority, EventHandler<E> handler);

    /**
     * Synchronously dispatches an event payload through its event definition.
     */
    <E> void dispatch(TavallEvent<E> eventDefinition, E payload);

    /**
     * Asynchronously dispatches an event payload through its event definition.
     */
    <E> CompletableFuture<Void> dispatchAsync(TavallEvent<E> eventDefinition, E payload);

    /**
     * Unregisters a specific active subscription.
     */
    void unregister(EventSubscription<?> subscription);

    /**
     * Unregisters all subscriptions associated with the given owner object (e.g. during module reload/unload).
     */
    void unregisterAll(Object owner);

    /**
     * Clears all subscriptions registered across all event definitions.
     */
    void reset();

    /**
     * @return the total number of active subscriptions across all event definitions
     */
    int subscriptionCount();

    /**
     * @return the number of active subscriptions registered for the given payload class
     */
    int subscriptionCount(Class<?> payloadType);
}
