package org.tavall.event;

/**
 * Functional callback interface for handling a strongly typed event payload with context.
 *
 * @param <E> the event payload type
 */
@FunctionalInterface
public interface EventHandler<E> {

    /**
     * Handles the event payload and its execution context.
     *
     * @param payload the typed event payload
     * @param context the dispatch execution context
     * @throws Exception if handler processing fails
     */
    void handle(E payload, EventContext context) throws Exception;
}
