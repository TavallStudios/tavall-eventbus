package org.tavall.event;

import org.tavall.enums.EventPriority;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Canonical abstract base for Tavall typed event definitions, analogous to AbstractCache.
 * <p>
 * A concrete event definition owns typed settings, registration, and dispatch mechanics:
 * <pre>{@code
 * public class PlayerJoinEventHandler extends TavallEvent<PlayerJoinPayload> {
 *     public PlayerJoinEventHandler(IEventRouter router) {
 *         super(PlayerJoinPayload.class, EventSettings.builder()
 *                 .domain(EventDomain.PLAYER)
 *                 .cancellable(true)
 *                 .build(), router);
 *     }
 * }
 * }</pre>
 *
 * @param <E> the plain data event payload type (does not extend any framework base)
 */
public abstract class TavallEvent<E> {

    private final Class<E> payloadType;
    private final EventSettings settings;
    private final IEventRouter router;

    /**
     * Constructs a TavallEvent definition with default settings and the default router.
     *
     * @param payloadType explicit payload class
     */
    protected TavallEvent(Class<E> payloadType) {
        this(payloadType, EventSettings.defaultSettings());
    }

    /**
     * Constructs a TavallEvent definition with explicit settings and the default router.
     *
     * @param payloadType explicit payload class
     * @param settings    immutable event configuration settings
     */
    protected TavallEvent(Class<E> payloadType, EventSettings settings) {
        this(payloadType, settings, EventRouter.getDefault());
    }

    /**
     * Constructs a TavallEvent definition with explicit settings and a designated router.
     *
     * @param payloadType explicit payload class
     * @param settings    immutable event configuration settings
     * @param router      the routing and dispatch coordinator
     */
    protected TavallEvent(Class<E> payloadType, EventSettings settings, IEventRouter router) {
        this.payloadType = Objects.requireNonNull(payloadType, "payloadType cannot be null");
        this.settings = Objects.requireNonNull(settings, "settings cannot be null");
        this.router = Objects.requireNonNull(router, "router cannot be null");
    }

    /**
     * @return the explicit payload class
     */
    public Class<E> getPayloadType() {
        return payloadType;
    }

    /**
     * @return immutable event configuration settings
     */
    public EventSettings getSettings() {
        return settings;
    }

    /**
     * @return the event router owning dispatch and subscriptions
     */
    public IEventRouter getRouter() {
        return router;
    }

    /**
     * Subscribes a handler to this event definition with default priority and no specific owner.
     *
     * @param handler the functional handler callback
     * @return a subscription handle allowing unregistration
     */
    public EventSubscription<E> subscribe(EventHandler<E> handler) {
        return router.register(this, handler);
    }

    /**
     * Subscribes a handler associated with an owning object (for lifecycle unregistration).
     *
     * @param owner   the owning component, service, or module
     * @param handler the functional handler callback
     * @return a subscription handle allowing unregistration
     */
    public EventSubscription<E> subscribe(Object owner, EventHandler<E> handler) {
        return router.register(this, owner, handler);
    }

    /**
     * Subscribes a handler with an explicit priority and owning object.
     *
     * @param owner    the owning component, service, or module
     * @param priority the execution priority
     * @param handler  the functional handler callback
     * @return a subscription handle allowing unregistration
     */
    public EventSubscription<E> subscribe(Object owner, EventPriority priority, EventHandler<E> handler) {
        return router.register(this, owner, priority, handler);
    }

    /**
     * Dispatches an event payload through the router.
     * If {@link EventSettings#isAsync()} is true, dispatches asynchronously and awaits completion.
     *
     * @param payload the typed event payload instance
     */
    public void fire(E payload) {
        router.dispatch(this, payload);
    }

    /**
     * Dispatches an event payload asynchronously on the router's executor pool.
     *
     * @param payload the typed event payload instance
     * @return a CompletableFuture representing completion of the dispatch
     */
    public CompletableFuture<Void> fireAsync(E payload) {
        return router.dispatchAsync(this, payload);
    }

    /**
     * Lifecycle hook invoked immediately before handlers are executed.
     *
     * @param payload the event payload
     * @param context the execution context
     */
    protected void beforeDispatch(E payload, EventContext context) {
    }

    /**
     * Lifecycle hook invoked immediately after all active handlers have executed.
     *
     * @param payload the event payload
     * @param context the execution context
     */
    protected void afterDispatch(E payload, EventContext context) {
    }
}
