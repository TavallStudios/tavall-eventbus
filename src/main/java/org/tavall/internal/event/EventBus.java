package org.tavall.internal.event;

import org.tavall.platform.global.abstracts.AbstractEvent;
import org.tavall.platform.global.annotations.ModuleScope;
import org.tavall.platform.global.annotations.SubscribeEvent;
import org.tavall.enums.EventCapability;
import org.tavall.enums.EventDomain;
import org.tavall.enums.EventStatus;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Reflective event dispatcher for Tavall events and {@link SubscribeEvent}-annotated listeners.
 *
 * <p>The bus supports two dispatch paths. {@link #post(AbstractEvent, EventDomain)} uses the
 * per-event listener registry populated by {@link #register(Object)}, while {@link #fire(AbstractEvent)}
 * drives the lifecycle defined by {@link AbstractEvent} and dispatches against the registered
 * listener objects. Asynchronous fireable events are submitted to the bus executor.</p>
 */
public class EventBus {

    private final Map<Class<? extends AbstractEvent>, List<ListenerWrapper>> registry = new HashMap<>();
    private final List<Object> listeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<AbstractEvent>> middleware = new CopyOnWriteArrayList<>();

    private final ExecutorService asyncExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "EventBus-AsyncExecutor");
        t.setDaemon(true);
        return t;
    });

    /**
     * Registers an object and discovers its event subscription methods.
     *
     * <p>Declared methods annotated with {@link SubscribeEvent} are eligible when they accept
     * exactly one parameter assignable from {@link AbstractEvent}. Eligible methods are wrapped,
     * associated with their declared event type, and ordered from highest to lowest subscription
     * priority. A {@link ModuleScope} annotation supplies the listener domain when present;
     * otherwise the listener is registered with {@link EventDomain#GLOBAL}.</p>
     *
     * @param listener object containing subscription methods
     */
    public void register(Object listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
        for (Method method : listener.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(SubscribeEvent.class)) {
                Class<?>[] params = method.getParameterTypes();
                if (params.length != 1 || !AbstractEvent.class.isAssignableFrom(params[0]))
                    continue;

                SubscribeEvent sub = method.getAnnotation(SubscribeEvent.class);
                ModuleScope scope = method.getAnnotation(ModuleScope.class);

                method.setAccessible(true);
                Class<? extends AbstractEvent> eventClass = (Class<? extends AbstractEvent>) params[0];

                ListenerWrapper wrapper = new ListenerWrapper(
                        listener,
                        method,
                        sub.priority(),
                        sub.async(),
                        scope != null ? scope.eventDomain() : EventDomain.GLOBAL
                );

                registry.computeIfAbsent(eventClass, c -> new ArrayList<>()).add(wrapper);
                registry.get(eventClass).sort(Comparator.comparingInt(w -> -w.priority));
            }
        }
    }

    /**
     * Removes a listener object and all subscription wrappers owned by it.
     *
     * @param listener listener instance to remove
     */
    public void unregister(Object listener) {
        listeners.remove(listener);
        for (List<ListenerWrapper> wrappers : registry.values()) {
            wrappers.removeIf(w -> w.instance.equals(listener));
        }
    }

    /**
     * Posts an event to wrappers registered for the event's concrete class and dispatch domain.
     *
     * <p>Dispatch stops once the event reports itself cancelled. Wrappers marked asynchronous are
     * invoked on newly created threads; synchronous wrappers run on the caller thread.</p>
     *
     * @param event event instance to dispatch
     * @param eventDomain domain used to select eligible listeners
     */
    public void post(AbstractEvent event, EventDomain eventDomain) {
        List<ListenerWrapper> listeners = registry.get(event.getClass());
        if (listeners == null) return;

        for (ListenerWrapper wrapper : listeners) {
            if (wrapper.eventDomain == EventDomain.GLOBAL || wrapper.eventDomain != eventDomain) {
                continue;
            }
            if (event.isCancelled()) break;

            if (wrapper.async) {
                new Thread(() -> wrapper.invoke(event)).start();
            } else {
                wrapper.invoke(event);
            }
        }
    }

    /**
     * Adds middleware to this bus when the same consumer has not already been registered.
     *
     * @param mw middleware consumer to register
     */
    public void registerMiddleware(Consumer<AbstractEvent> mw) {
        if (!middleware.contains(mw)) {
            middleware.add(mw);
        }
    }

    /**
     * Removes middleware from this bus when present.
     *
     * @param mw middleware consumer to remove
     */
    public void unregisterMiddleware(Consumer<AbstractEvent> mw) {
        middleware.remove(mw);
    }

    /**
     * Executes the lifecycle of a fireable event and dispatches it to compatible subscriptions.
     *
     * <p>The event must advertise {@link EventCapability#FIREABLE}. Before dispatch, its status is
     * advanced through fired/running state and the event's pre-fire and middleware hooks execute.
     * Successful listener dispatch marks the event successful; failures are logged through the
     * event and mark it failed. Completion metadata and the post-fire hook are applied in all
     * cases. Events carrying {@link EventCapability#ASYNC} are dispatched on the bus executor.</p>
     *
     * @param event event to execute
     * @param <T> concrete event type
     * @throws IllegalStateException if the event is not marked fireable
     */
    public <T extends AbstractEvent> void fire(T event) {
        if (!event.hasCapability(EventCapability.FIREABLE)) {
            throw new IllegalStateException("Event is not marked as FIREABLE: " + event.getClass().getSimpleName());
        }

        long start = System.nanoTime();

        event.setStatus(EventStatus.FIRED);
        event.setStatus(EventStatus.RUNNING);
        event.beforeFire();
        event.applyMiddleware();

        Runnable dispatch = () -> {
            try {
                for (Object listener : listeners) {
                    for (Method method : listener.getClass().getDeclaredMethods()) {
                        if (!method.isAnnotationPresent(SubscribeEvent.class)) continue;
                        Class<?>[] params = method.getParameterTypes();
                        if (params.length != 1 || !params[0].isAssignableFrom(event.getClass())) continue;

                        method.setAccessible(true);
                        method.invoke(listener, event);
                    }
                }
                event.setStatus(EventStatus.SUCCESS);
            } catch (Throwable ex) {
                event.logException(ex);
                event.setStatus(EventStatus.FAILED);
            } finally {
                event.completed = true;
                event.completedAt = System.currentTimeMillis();
                long duration = System.nanoTime() - start;
                System.out.println("[Event Timer] " + event.getClass().getSimpleName() + " took " + duration + " ns");
                event.onFire();
            }
        };

        if (event.hasCapability(EventCapability.ASYNC)) {
            asyncExecutor.execute(dispatch);
        } else {
            dispatch.run();
        }
    }

    /**
     * Posts an event using the global domain.
     *
     * @param event event instance to dispatch
     */
    public void post(AbstractEvent event) {
        post(event, EventDomain.GLOBAL);
    }
}
