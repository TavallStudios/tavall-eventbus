package org.tavall.event.architecture;

import org.junit.jupiter.api.Test;
import org.tavall.event.EventSettings;
import org.tavall.event.TavallEvent;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class EventArchitectureTest {

    // === Architecture Fixtures (Accepting & Rejecting) ===

    // ACCEPTING FIXTURE: Plain data class with no framework inheritance
    record ValidEventPayload(String id, long timestamp) {}

    // ACCEPTING FIXTURE: Definition extending TavallEvent with explicit payload type
    static class ValidEventHandler extends TavallEvent<ValidEventPayload> {
        ValidEventHandler() {
            super(ValidEventPayload.class, EventSettings.defaultSettings());
        }
    }

    // REJECTING FIXTURE 1: Payload attempting to inherit from framework event class
    static class InvalidPayloadWithInheritance extends TavallEvent<String> {
        InvalidPayloadWithInheritance() {
            super(String.class);
        }
    }

    // REJECTING FIXTURE 2: Prohibited Manager naming convention
    static class InvalidEventManager {
    }

    // === Rule Validators ===

    static boolean isPayloadCompliant(Class<?> payloadClass) {
        // Rule: Payload must not extend TavallEvent or any framework event class
        return !TavallEvent.class.isAssignableFrom(payloadClass);
    }

    static boolean isNameCompliant(Class<?> clazz) {
        // Rule: Prohibit *Manager classes
        return !clazz.getSimpleName().endsWith("Manager");
    }

    @Test
    void testArchitectureRuleSelfValidationWithFixtures() {
        // Verify accepting fixtures pass
        assertTrue(isPayloadCompliant(ValidEventPayload.class), "Valid plain data payload must pass");
        assertTrue(isNameCompliant(ValidEventHandler.class), "Valid handler naming must pass");

        // Verify rejecting fixtures fail
        assertFalse(isPayloadCompliant(InvalidPayloadWithInheritance.class), "Payload extending TavallEvent must be rejected");
        assertFalse(isNameCompliant(InvalidEventManager.class), "*Manager class naming must be rejected");
    }

    @Test
    void testLegacyAbstractEventIsCompletelyRemoved() {
        assertThrows(ClassNotFoundException.class, () -> {
            Class.forName("org.tavall.platform.global.abstracts.AbstractEvent");
        }, "AbstractEvent must be completely eradicated from runtime classpath");
    }

    @Test
    void testLegacyManagersAreCompletelyRemoved() {
        assertThrows(ClassNotFoundException.class, () -> {
            Class.forName("org.tavall.managers.MySQL");
        }, "MySQL manager must be completely eradicated");

        assertThrows(ClassNotFoundException.class, () -> {
            Class.forName("org.tavall.managers.Redis");
        }, "Redis manager must be completely eradicated");
    }

    @Test
    void testProductionCodeFollowsNamingRules() throws IOException {
        Path mainJavaDir = Path.of("src/main/java");
        if (!Files.exists(mainJavaDir)) {
            return;
        }

        List<String> managerClasses = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(mainJavaDir)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .forEach(p -> {
                        String fileName = p.getFileName().toString();
                        if (fileName.endsWith("Manager.java")) {
                            managerClasses.add(fileName);
                        }
                    });
        }

        assertTrue(managerClasses.isEmpty(), "No *Manager classes allowed in production code: " + managerClasses);
    }
}
