package org.tavall.event;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tavall.enums.EventPriority;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Direct house-style test for the real {@link EventRouter} production class.
 *
 * <p>Exercises the canonical runtime dispatch implementation with real {@link TavallEvent} definitions,
 * real {@link EventHandler} implementations and the real in-memory subscription registry. No mocks:
 * {@link EventRouter} is the production composition path ({@code new EventRouter()}) and all neighbours
 * are real. Subscription/unregistration behaviour owned by this router (previously spread across
 * {@code EventSubscriptionTest}) is consolidated here as the concrete behaviour owner.
 */
final class EventRouterTest {

    private EventRouter router;

    @BeforeEach
    void setUp() {
        router = new EventRouter();
    }

    @AfterEach
    void tearDown() {
        router.close();
    }

    record RoutingPayload(String name) {}

    static class RoutingEvent extends TavallEvent<RoutingPayload> {
        RoutingEvent(IEventRouter router) {
            super(RoutingPayload.class, EventSettings.defaultSettings(), router);
        }
    }

    @Test
    void registerThenDispatchInvokesHandlerSynchronously() {
        RoutingEvent event = new RoutingEvent(router);
        List<String> log = new ArrayList<>();

        event.subscribe((payload, context) -> log.add(payload.name()));

        event.fire(new RoutingPayload("hello"));

        assertEquals(List.of("hello"), log);
    }

    @Test
    void handlersExecuteInPriorityOrder() {
        RoutingEvent event = new RoutingEvent(router);
        List<String> executionOrder = new ArrayList<>();

        event.subscribe(this, EventPriority.LOW, (payload, context) -> executionOrder.add("LOW"));
        event.subscribe(this, EventPriority.CRITICAL, (payload, context) -> executionOrder.add("CRITICAL"));
        event.subscribe(this, EventPriority.NORMAL, (payload, context) -> executionOrder.add("NORMAL"));
        event.subscribe(this, EventPriority.HIGH, (payload, context) -> executionOrder.add("HIGH"));

        event.fire(new RoutingPayload("test"));

        assertEquals(List.of("CRITICAL", "HIGH", "NORMAL", "LOW"), executionOrder);
    }

    @Test
    void cancellationHaltsSubsequentHandlers() {
        RoutingEvent event = new RoutingEvent(router);
        List<String> executed = new ArrayList<>();

        event.subscribe(this, EventPriority.HIGH, (payload, context) -> {
            executed.add("HIGH");
            context.cancel();
        });
        event.subscribe(this, EventPriority.NORMAL, (payload, context) -> executed.add("NORMAL"));

        event.fire(new RoutingPayload("cancel-test"));

        assertEquals(List.of("HIGH"), executed);
    }

    @Test
    void unsubscribeStopsFutureInvocations() {
        RoutingEvent event = new RoutingEvent(router);
        List<String> executed = new ArrayList<>();

        EventSubscription<RoutingPayload> sub = event.subscribe((payload, context) -> executed.add(payload.name()));

        assertTrue(sub.isActive());
        event.fire(new RoutingPayload("first"));
        assertEquals(List.of("first"), executed);

        sub.unsubscribe();
        assertFalse(sub.isActive());

        event.fire(new RoutingPayload("second"));
        assertEquals(List.of("first"), executed, "Unsubscribed handler must not be invoked");
    }

    @Test
    void subscriptionCountReflectsActiveRegistrations() {
        RoutingEvent event = new RoutingEvent(router);

        assertEquals(0, router.subscriptionCount());
        assertEquals(0, router.subscriptionCount(RoutingPayload.class));

        event.subscribe((payload, context) -> {});
        event.subscribe((payload, context) -> {});

        assertEquals(2, router.subscriptionCount());
        assertEquals(2, router.subscriptionCount(RoutingPayload.class));
    }

    @Test
    void asyncDispatchCompletesThroughExecutor() {
        RoutingEvent event = new RoutingEvent(router);
        AtomicInteger count = new AtomicInteger(0);

        event.subscribe((payload, context) -> count.addAndGet(payload.name().length()));

        CompletableFuture<Void> future = event.fireAsync(new RoutingPayload("async"));
        future.join();

        assertEquals(5, count.get());
    }

    @Test
    void handlerFailureIsWrappedInDispatchException() {
        RoutingEvent event = new RoutingEvent(router);
        event.subscribe((payload, context) -> {
            throw new IllegalArgumentException("handler simulated failure");
        });

        EventDispatchException ex = assertThrows(EventDispatchException.class,
                () -> event.fire(new RoutingPayload("fail")));

        assertEquals(RoutingPayload.class, ex.getPayloadType());
        assertTrue(ex.getCause() instanceof IllegalArgumentException);
        assertEquals("handler simulated failure", ex.getCause().getMessage());
    }
}
