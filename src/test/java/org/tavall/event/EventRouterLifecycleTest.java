package org.tavall.event;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tavall.enums.EventPriority;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EventRouterLifecycleTest {

    private EventRouter router;

    @BeforeEach
    void setUp() {
        router = new EventRouter();
    }

    @AfterEach
    void tearDown() {
        router.close();
    }

    record LifecyclePayload(String message) {}

    static class LifecycleEvent extends TavallEvent<LifecyclePayload> {
        LifecycleEvent(IEventRouter router) {
            super(LifecyclePayload.class, EventSettings.defaultSettings(), router);
        }
    }

    @Test
    void testModuleUnloadCleansUpOnlyOwnedSubscriptions() {
        LifecycleEvent event = new LifecycleEvent(router);
        List<String> log = new ArrayList<>();

        Object moduleA = new Object();
        Object moduleB = new Object();

        EventSubscription<LifecyclePayload> subA1 = event.subscribe(moduleA, (payload, context) -> log.add("A1:" + payload.message()));
        EventSubscription<LifecyclePayload> subA2 = event.subscribe(moduleA, (payload, context) -> log.add("A2:" + payload.message()));
        EventSubscription<LifecyclePayload> subB1 = event.subscribe(moduleB, (payload, context) -> log.add("B1:" + payload.message()));

        assertEquals(3, router.subscriptionCount());
        assertEquals(3, router.subscriptionCount(LifecyclePayload.class));

        event.fire(new LifecyclePayload("first"));
        assertEquals(3, log.size());

        // Simulate module A unload / reload
        router.unregisterAll(moduleA);

        assertFalse(subA1.isActive(), "subA1 must be inactive after moduleA unload");
        assertFalse(subA2.isActive(), "subA2 must be inactive after moduleA unload");
        assertTrue(subB1.isActive(), "subB1 must remain active");

        assertEquals(1, router.subscriptionCount());

        log.clear();
        event.fire(new LifecyclePayload("second"));

        assertEquals(List.of("B1:second"), log, "Only module B handler should execute after module A unregisters");
    }

    @Test
    void testRouterResetClearsAllSubscriptions() {
        LifecycleEvent event = new LifecycleEvent(router);
        Object module = new Object();

        EventSubscription<LifecyclePayload> sub = event.subscribe(module, (payload, context) -> {});
        assertEquals(1, router.subscriptionCount());

        router.reset();

        assertEquals(0, router.subscriptionCount());
        assertFalse(sub.isActive());
    }
}
