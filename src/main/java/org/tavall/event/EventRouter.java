package org.tavall.event;

import org.tavall.enums.EventPriority;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Canonical runtime routing and dispatch implementation for Tavall events.
 * Manages thread-safe subscription registries, priority ordering, cancellation semantics,
 * and leak-free lifecycle unloading.
 */
public class EventRouter implements IEventRouter, AutoCloseable {

    private static final EventRouter DEFAULT_INSTANCE = new EventRouter();

    private final ConcurrentMap<Class<?>, List<DefaultEventSubscription<?>>> subscriptions = new ConcurrentHashMap<>();
    private final ExecutorService executorService;
    private final boolean ownedExecutor;

    public EventRouter() {
        this(ForkJoinPool.commonPool(), false);
    }

    public EventRouter(ExecutorService executorService) {
        this(executorService, true);
    }

    private EventRouter(ExecutorService executorService, boolean ownedExecutor) {
        this.executorService = Objects.requireNonNull(executorService, "executorService cannot be null");
        this.ownedExecutor = ownedExecutor;
    }

    public static EventRouter getDefault() {
        return DEFAULT_INSTANCE;
    }

    @Override
    public <E> EventSubscription<E> register(TavallEvent<E> eventDefinition, EventHandler<E> handler) {
        return register(eventDefinition, null, eventDefinition.getSettings().getPriority(), handler);
    }

    @Override
    public <E> EventSubscription<E> register(TavallEvent<E> eventDefinition, Object owner, EventHandler<E> handler) {
        return register(eventDefinition, owner, eventDefinition.getSettings().getPriority(), handler);
    }

    @Override
    public <E> EventSubscription<E> register(TavallEvent<E> eventDefinition, Object owner, EventPriority priority, EventHandler<E> handler) {
        Objects.requireNonNull(eventDefinition, "eventDefinition cannot be null");
        Objects.requireNonNull(priority, "priority cannot be null");
        Objects.requireNonNull(handler, "handler cannot be null");

        DefaultEventSubscription<E> subscription = new DefaultEventSubscription<>(
                this,
                eventDefinition.getPayloadType(),
                owner,
                priority,
                handler
        );

        subscriptions.compute(eventDefinition.getPayloadType(), (key, existing) -> {
            List<DefaultEventSubscription<?>> list = existing == null ? new CopyOnWriteArrayList<>() : existing;
            list.add(subscription);
            return list;
        });

        return subscription;
    }

    @Override
    public <E> void dispatch(TavallEvent<E> eventDefinition, E payload) {
        Objects.requireNonNull(eventDefinition, "eventDefinition cannot be null");
        Objects.requireNonNull(payload, "payload cannot be null");

        if (eventDefinition.getSettings().isAsync()) {
            dispatchAsync(eventDefinition, payload).join();
            return;
        }

        executeDispatch(eventDefinition, payload);
    }

    @Override
    public <E> CompletableFuture<Void> dispatchAsync(TavallEvent<E> eventDefinition, E payload) {
        Objects.requireNonNull(eventDefinition, "eventDefinition cannot be null");
        Objects.requireNonNull(payload, "payload cannot be null");

        return CompletableFuture.runAsync(() -> executeDispatch(eventDefinition, payload), executorService);
    }

    @SuppressWarnings("unchecked")
    private <E> void executeDispatch(TavallEvent<E> eventDefinition, E payload) {
        EventContext context = new EventContext(eventDefinition.getSettings().isCancellable());

        eventDefinition.beforeDispatch(payload, context);

        List<DefaultEventSubscription<?>> rawList = subscriptions.get(eventDefinition.getPayloadType());
        if (rawList != null && !rawList.isEmpty()) {
            List<DefaultEventSubscription<E>> sorted = new ArrayList<>();
            for (DefaultEventSubscription<?> sub : rawList) {
                if (sub.isActive()) {
                    sorted.add((DefaultEventSubscription<E>) sub);
                }
            }

            // Sort descending by priority level (CRITICAL 15 first down to LOW 0)
            sorted.sort((a, b) -> Integer.compare(b.getPriority().getLevel(), a.getPriority().getLevel()));

            for (DefaultEventSubscription<E> sub : sorted) {
                if (!sub.isActive()) {
                    continue;
                }
                if (context.isCancelled()) {
                    break;
                }
                try {
                    sub.handler.handle(payload, context);
                } catch (Exception ex) {
                    throw new EventDispatchException(
                            eventDefinition.getPayloadType(),
                            "Handler execution failed for event: " + eventDefinition.getPayloadType().getName(),
                            ex
                    );
                }
            }
        }

        eventDefinition.afterDispatch(payload, context);
    }

    @Override
    public void unregister(EventSubscription<?> subscription) {
        if (subscription instanceof DefaultEventSubscription<?> defSub) {
            defSub.deactivate();
            List<DefaultEventSubscription<?>> list = subscriptions.get(defSub.getPayloadType());
            if (list != null) {
                list.remove(defSub);
                if (list.isEmpty()) {
                    subscriptions.remove(defSub.getPayloadType(), list);
                }
            }
        }
    }

    @Override
    public void unregisterAll(Object owner) {
        if (owner == null) return;
        for (List<DefaultEventSubscription<?>> list : subscriptions.values()) {
            list.removeIf(sub -> {
                if (owner.equals(sub.getOwner())) {
                    sub.deactivate();
                    return true;
                }
                return false;
            });
        }
        subscriptions.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    @Override
    public void reset() {
        for (List<DefaultEventSubscription<?>> list : subscriptions.values()) {
            for (DefaultEventSubscription<?> sub : list) {
                sub.deactivate();
            }
            list.clear();
        }
        subscriptions.clear();
    }

    @Override
    public int subscriptionCount() {
        int count = 0;
        for (List<DefaultEventSubscription<?>> list : subscriptions.values()) {
            count += list.size();
        }
        return count;
    }

    @Override
    public int subscriptionCount(Class<?> payloadType) {
        List<DefaultEventSubscription<?>> list = subscriptions.get(payloadType);
        return list == null ? 0 : list.size();
    }

    @Override
    public void close() {
        reset();
        if (ownedExecutor && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    private static final class DefaultEventSubscription<E> implements EventSubscription<E> {
        private final EventRouter router;
        private final Class<E> payloadType;
        private final Object owner;
        private final EventPriority priority;
        private final EventHandler<E> handler;
        private final AtomicBoolean active = new AtomicBoolean(true);

        private DefaultEventSubscription(
                EventRouter router,
                Class<E> payloadType,
                Object owner,
                EventPriority priority,
                EventHandler<E> handler
        ) {
            this.router = router;
            this.payloadType = payloadType;
            this.owner = owner;
            this.priority = priority;
            this.handler = handler;
        }

        @Override
        public Class<E> getPayloadType() {
            return payloadType;
        }

        @Override
        public EventPriority getPriority() {
            return priority;
        }

        @Override
        public Object getOwner() {
            return owner;
        }

        @Override
        public boolean isActive() {
            return active.get();
        }

        @Override
        public void unsubscribe() {
            router.unregister(this);
        }

        void deactivate() {
            active.set(false);
        }
    }
}
