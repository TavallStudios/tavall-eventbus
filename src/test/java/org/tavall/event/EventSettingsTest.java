package org.tavall.event;

import org.junit.jupiter.api.Test;
import org.tavall.enums.EventDomain;
import org.tavall.enums.EventPriority;
import org.tavall.enums.EventTag;

import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EventSettingsTest {

    @Test
    void testDefaultSettings() {
        EventSettings settings = EventSettings.defaultSettings();
        assertFalse(settings.isAsync());
        assertTrue(settings.isCancellable());
        assertEquals(EventPriority.NORMAL, settings.getPriority());
        assertEquals(Duration.ofSeconds(30), settings.getTimeout());
        assertEquals(EventDomain.GLOBAL, settings.getDomain());
        assertTrue(settings.getTags().isEmpty());
    }

    @Test
    void testCustomSettingsBuilder() {
        EventSettings settings = EventSettings.builder()
                .async(true)
                .cancellable(false)
                .priority(EventPriority.CRITICAL)
                .timeout(Duration.ofSeconds(10))
                .domain(EventDomain.DATABASE)
                .tag(EventTag.DATABASE)
                .tag(EventTag.LOCAL)
                .build();

        assertTrue(settings.isAsync());
        assertFalse(settings.isCancellable());
        assertEquals(EventPriority.CRITICAL, settings.getPriority());
        assertEquals(Duration.ofSeconds(10), settings.getTimeout());
        assertEquals(EventDomain.DATABASE, settings.getDomain());
        assertEquals(Set.of(EventTag.DATABASE, EventTag.LOCAL), settings.getTags());
    }

    @Test
    void testNonCancellableEventThrowsOnCancel() {
        EventContext context = new EventContext(false);
        assertFalse(context.isCancellable());
        assertThrows(UnsupportedOperationException.class, context::cancel);
        assertFalse(context.isCancelled());
    }

    @Test
    void testCancellableEventSucceedsOnCancel() {
        EventContext context = new EventContext(true);
        assertTrue(context.isCancellable());
        assertFalse(context.isCancelled());
        assertTrue(context.cancel());
        assertTrue(context.isCancelled());
        assertFalse(context.cancel(), "Subsequent cancel should return false as already cancelled");
    }
}
