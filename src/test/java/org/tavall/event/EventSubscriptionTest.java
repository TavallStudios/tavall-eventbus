package org.tavall.event;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tavall.enums.EventPriority;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EventSubscriptionTest {

    private EventRouter router;

    @BeforeEach
    void setUp() {
        router = new EventRouter();
    }

    @AfterEach
    void tearDown() {
        router.close();
    }

    record PriorityPayload(String name) {}

    static class PriorityEvent extends TavallEvent<PriorityPayload> {
        PriorityEvent(IEventRouter router) {
            super(PriorityPayload.class, EventSettings.defaultSettings(), router);
        }
    }

    @Test
    void testHandlersExecuteInPriorityOrder() {
        PriorityEvent event = new PriorityEvent(router);
        List<String> executionOrder = new ArrayList<>();

        // Register in arbitrary order
        event.subscribe(this, EventPriority.LOW, (payload, context) -> executionOrder.add("LOW"));
        event.subscribe(this, EventPriority.CRITICAL, (payload, context) -> executionOrder.add("CRITICAL"));
        event.subscribe(this, EventPriority.NORMAL, (payload, context) -> executionOrder.add("NORMAL"));
        event.subscribe(this, EventPriority.HIGH, (payload, context) -> executionOrder.add("HIGH"));

        event.fire(new PriorityPayload("test"));

        assertEquals(List.of("CRITICAL", "HIGH", "NORMAL", "LOW"), executionOrder);
    }

    @Test
    void testCancellationHaltsSubsequentHandlers() {
        PriorityEvent event = new PriorityEvent(router);
        List<String> executed = new ArrayList<>();

        event.subscribe(this, EventPriority.HIGH, (payload, context) -> {
            executed.add("HIGH");
            context.cancel();
        });

        event.subscribe(this, EventPriority.NORMAL, (payload, context) -> {
            executed.add("NORMAL");
        });

        event.fire(new PriorityPayload("cancel-test"));

        assertEquals(List.of("HIGH"), executed);
    }

    @Test
    void testUnsubscribeStopsFutureInvocations() {
        PriorityEvent event = new PriorityEvent(router);
        List<String> executed = new ArrayList<>();

        EventSubscription<PriorityPayload> sub = event.subscribe((payload, context) -> {
            executed.add(payload.name());
        });

        assertTrue(sub.isActive());
        event.fire(new PriorityPayload("first"));
        assertEquals(List.of("first"), executed);

        sub.unsubscribe();
        assertFalse(sub.isActive());

        event.fire(new PriorityPayload("second"));
        assertEquals(List.of("first"), executed, "Unsubscribed handler must not be invoked");
    }
}
