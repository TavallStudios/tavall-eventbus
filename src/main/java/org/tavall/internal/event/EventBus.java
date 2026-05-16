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

public class EventBus {

    private final Map<Class<? extends AbstractEvent>, List<ListenerWrapper>> registry = new HashMap<>(); //changed
    private final List<Object> listeners = new CopyOnWriteArrayList<>(); //changed
    private final List<Consumer<AbstractEvent>> middleware = new CopyOnWriteArrayList<>();

    private final ExecutorService asyncExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "EventBus-AsyncExecutor");
        t.setDaemon(true);
        return t;
    });
    public void register(Object listener) { //changed
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

    public void unregister(Object listener) { //changed
        listeners.remove(listener);
        for (List<ListenerWrapper> wrappers : registry.values()) {
            wrappers.removeIf(w -> w.instance.equals(listener));
        }
    }

    public void post(AbstractEvent event, EventDomain eventDomain) {
        List<ListenerWrapper> listeners = registry.get(event.getClass());
        if (listeners == null) return;

        for (ListenerWrapper wrapper : listeners) {
            if (wrapper.eventDomain != EventDomain.GLOBAL && wrapper.eventDomain != eventDomain) {
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

    public void registerMiddleware(Consumer<AbstractEvent> mw) {
        if (!middleware.contains(mw)) {
            middleware.add(mw);
        }
    }

    public void unregisterMiddleware(Consumer<AbstractEvent> mw) {
        middleware.remove(mw);
    }

    public <T extends AbstractEvent> void fire(T event) {
        if (!event.hasCapability(EventCapability.FIREABLE)) {
            throw new IllegalStateException("Event is not marked as FIREABLE: " + event.getClass().getSimpleName());
        }

        long start = System.nanoTime();

        event.setStatus(EventStatus.FIRED); //changed
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

    public void post(AbstractEvent event) {
        post(event, EventDomain.GLOBAL);
    }
}
