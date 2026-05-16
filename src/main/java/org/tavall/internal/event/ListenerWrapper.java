package org.tavall.internal.event;

import org.tavall.platform.global.abstracts.AbstractEvent;
import org.tavall.enums.EventDomain;

import java.lang.reflect.Method;

public class ListenerWrapper {
    public final Object instance;
    public final Method method;
    public final int priority;
    public final boolean async;
    public final EventDomain eventDomain;

    public ListenerWrapper(Object instance, Method method, int priority, boolean async, EventDomain eventDomain) {
        this.instance = instance;
        this.method = method;
        this.priority = priority;
        this.async = async;
        this.eventDomain = eventDomain;
    }

    public void invoke(AbstractEvent event) {
        try {
            method.invoke(instance, event);
        } catch (Exception e) {
            System.err.println("[EventBus] Error invoking event handler: " + e.getMessage());
            //TODO Replace with logging
            e.printStackTrace();
        }
    }
}
