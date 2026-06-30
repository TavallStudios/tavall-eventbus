package org.tavall.internal.event;

import org.tavall.enums.EventCapability;
import org.tavall.enums.EventDomain;
import org.tavall.enums.EventStatus;
import org.tavall.platform.global.abstracts.AbstractEvent;
import org.tavall.platform.global.annotations.ModuleScope;
import org.tavall.platform.global.annotations.SubscribeEvent;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventBusTest {

    @Test
    void fireInvokesListenerAndUpdatesStatus() {
        EventBus eventBus = new EventBus();
        CountingListener listener = new CountingListener();
        eventBus.register(listener);

        TestEvent event = new TestEvent();
        eventBus.fire(event);

        assertEquals(1, listener.fireCalls.get());
        assertEquals(EventStatus.SUCCESS, event.getStatus());
        assertTrue(event.completed);
    }

    @Test
    void postHonorsModuleScope() {
        EventBus eventBus = new EventBus();
        ScopedListener listener = new ScopedListener();
        eventBus.register(listener);

        eventBus.post(new ScopedEvent(), EventDomain.GLOBAL);
        eventBus.post(new ScopedEvent(), EventDomain.API);

        assertEquals(0, listener.globalCalls.get());
        assertEquals(1, listener.apiCalls.get());
    }

    private static class TestEvent extends AbstractEvent {
        private TestEvent() {
            enableCapabilities(EventCapability.FIREABLE);
        }
    }

    private static class ScopedEvent extends AbstractEvent {
    }

    private static class CountingListener {
        private final AtomicInteger fireCalls = new AtomicInteger();

        @SubscribeEvent
        public void onTestEvent(TestEvent event) {
            fireCalls.incrementAndGet();
        }
    }

    private static class ScopedListener {
        private final AtomicInteger apiCalls = new AtomicInteger();
        private final AtomicInteger globalCalls = new AtomicInteger();

        @SubscribeEvent
        @ModuleScope(eventDomain = EventDomain.API)
        public void onScopedEvent(ScopedEvent event) {
            apiCalls.incrementAndGet();
        }

        @SubscribeEvent
        @ModuleScope(eventDomain = EventDomain.GLOBAL)
        public void onGlobalEvent(ScopedEvent event) {
            globalCalls.incrementAndGet();
        }
    }
}

