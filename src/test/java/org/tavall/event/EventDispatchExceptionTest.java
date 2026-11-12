package org.tavall.event;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Direct house-style test for the real {@link EventDispatchException} production class.
 * The exception carries the failing event's payload type alongside the standard message/cause chain.
 */
final class EventDispatchExceptionTest {

    @Test
    void carriesPayloadTypeAndCause() {
        IllegalArgumentException cause = new IllegalArgumentException("handler simulated failure");
        EventDispatchException ex = new EventDispatchException(String.class, "handler failed", cause);

        assertEquals(String.class, ex.getPayloadType());
        assertEquals("handler failed", ex.getMessage());
        assertSame(cause, ex.getCause());
    }

    @Test
    void supportsNullCause() {
        EventDispatchException ex = new EventDispatchException(Integer.class, "no cause", null);

        assertEquals(Integer.class, ex.getPayloadType());
        assertEquals("no cause", ex.getMessage());
        assertNull(ex.getCause());
    }
}
