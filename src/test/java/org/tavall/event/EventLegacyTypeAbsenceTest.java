package org.tavall.event;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tavall Architectural Role: Classpath contract test for retired EventBus API types.
 * Ownership: Tavall EventBus test suite; keeps removed legacy types unavailable to consumers.
 */
final class EventLegacyTypeAbsenceTest {
    @Test
    void retiredAbstractEventTypeIsUnavailable() {
        assertThrows(ClassNotFoundException.class, () ->
                Class.forName("org.tavall.platform.global.abstracts.AbstractEvent"));
    }

    @Test
    void retiredManagerTypesAreUnavailable() {
        assertThrows(ClassNotFoundException.class, () ->
                Class.forName("org.tavall.managers.MySQL"));
        assertThrows(ClassNotFoundException.class, () ->
                Class.forName("org.tavall.managers.Redis"));
    }
}
