package org.tavall.event;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Direct house-style test for the real {@link EventContext} production class.
 * Exercises cancellation semantics and dynamic attribute storage against the real subject.
 */
final class EventContextTest {

    @Test
    void nonCancellableContextRejectsCancel() {
        EventContext context = new EventContext(false);
        assertFalse(context.isCancellable());
        assertFalse(context.isCancelled());

        assertThrows(UnsupportedOperationException.class, context::cancel);
        assertFalse(context.isCancelled(), "Non-cancellable context must never enter cancelled state");
    }

    @Test
    void cancellableContextCancelsExactlyOnce() {
        EventContext context = new EventContext(true);
        assertTrue(context.isCancellable());
        assertFalse(context.isCancelled());

        assertTrue(context.cancel());
        assertTrue(context.isCancelled());
        assertFalse(context.cancel(), "A second cancel must report false once already cancelled");
    }

    @Test
    void attributesAreStoredReadAndRemoved() {
        EventContext context = new EventContext(true);

        assertNull(context.getAttribute("traceId"));

        context.setAttribute("traceId", "abc-123");
        assertEquals("abc-123", context.getAttribute("traceId"));

        context.setAttribute("traceId", null);
        assertNull(context.getAttribute("traceId"), "Setting a null value must remove the attribute");
    }

    @Test
    void attributeMapIsUnmodifiableSnapshot() {
        EventContext context = new EventContext(true);
        context.setAttribute("k", "v");

        assertThrows(UnsupportedOperationException.class, () -> context.getAttributes().put("k2", "v2"));
    }

    @Test
    void createdAtIsCapturedAtConstruction() {
        EventContext context = new EventContext(true);
        assertNotNull(context.getCreatedAt());
    }
}
