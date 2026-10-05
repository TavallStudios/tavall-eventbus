package org.tavall.event;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tavall.enums.EventDomain;
import org.tavall.enums.EventPriority;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class TavallEventMechanicsTest {

    private EventRouter router;

    @BeforeEach
    void setUp() {
        router = new EventRouter();
    }

    @AfterEach
    void tearDown() {
        router.close();
    }

    // Plain data payload class - NO inheritance from framework base
    record SamplePayload(String message, int value) {}

    // Typed event definition extending TavallEvent<SamplePayload>
    static class SampleEvent extends TavallEvent<SamplePayload> {
        final AtomicBoolean beforeCalled = new AtomicBoolean(false);
        final AtomicBoolean afterCalled = new AtomicBoolean(false);

        SampleEvent(IEventRouter router) {
            super(SamplePayload.class, EventSettings.builder()
                    .domain(EventDomain.CORE)
                    .priority(EventPriority.HIGH)
                    .cancellable(true)
                    .build(), router);
        }

        @Override
        protected void beforeDispatch(SamplePayload payload, EventContext context) {
            beforeCalled.set(true);
            context.setAttribute("traceId", "test-trace-" + payload.value());
        }

        @Override
        protected void afterDispatch(SamplePayload payload, EventContext context) {
            afterCalled.set(true);
        }
    }

    @Test
    void testSynchronousDispatchAndLifecycleHooks() {
        SampleEvent event = new SampleEvent(router);
        AtomicReference<String> receivedMessage = new AtomicReference<>();
        AtomicReference<String> receivedTrace = new AtomicReference<>();

        event.subscribe((payload, context) -> {
            receivedMessage.set(payload.message());
            receivedTrace.set(context.getAttribute("traceId"));
        });

        event.fire(new SamplePayload("hello", 42));

        assertEquals("hello", receivedMessage.get());
        assertEquals("test-trace-42", receivedTrace.get());
        assertTrue(event.beforeCalled.get(), "beforeDispatch hook should be called");
        assertTrue(event.afterCalled.get(), "afterDispatch hook should be called");
    }

    @Test
    void testAsynchronousDispatch() {
        SampleEvent event = new SampleEvent(router);
        AtomicInteger count = new AtomicInteger(0);

        event.subscribe((payload, context) -> {
            count.addAndGet(payload.value());
        });

        CompletableFuture<Void> future = event.fireAsync(new SamplePayload("async", 10));
        future.join();

        assertEquals(10, count.get());
        assertTrue(event.beforeCalled.get());
        assertTrue(event.afterCalled.get());
    }

    @Test
    void testHandlerExceptionWrappedInDispatchException() {
        SampleEvent event = new SampleEvent(router);
        event.subscribe((payload, context) -> {
            throw new IllegalArgumentException("handler simulated failure");
        });

        EventDispatchException ex = assertThrows(EventDispatchException.class, () -> {
            event.fire(new SamplePayload("fail", 1));
        });

        assertEquals(SamplePayload.class, ex.getPayloadType());
        assertTrue(ex.getCause() instanceof IllegalArgumentException);
        assertEquals("handler simulated failure", ex.getCause().getMessage());
    }
}
