package org.tavall.event.database;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tavall.enums.EventDomain;
import org.tavall.enums.EventPriority;
import org.tavall.enums.EventTag;
import org.tavall.event.EventRouter;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseDisconnectEventTest {

    private EventRouter router;

    @BeforeEach
    void setUp() {
        router = new EventRouter();
    }

    @AfterEach
    void tearDown() {
        router.close();
    }

    @Test
    void testDatabaseDisconnectEventHandling() {
        DatabaseDisconnectEvent disconnectEvent = new DatabaseDisconnectEvent(router);

        assertEquals(EventDomain.DATABASE, disconnectEvent.getSettings().getDomain());
        assertEquals(EventPriority.HIGH, disconnectEvent.getSettings().getPriority());
        assertTrue(disconnectEvent.getSettings().getTags().contains(EventTag.DATABASE));
        assertFalse(disconnectEvent.getSettings().isCancellable());

        AtomicReference<DatabaseDisconnectPayload> received = new AtomicReference<>();
        disconnectEvent.subscribe((payload, context) -> received.set(payload));

        DatabaseDisconnectPayload payload = new DatabaseDisconnectPayload(
                "POSTGRESQL",
                "analytics_db",
                "Connection timeout after 30s",
                Map.of("host", "db.internal.tavall.org", "port", "5432"),
                null
        );

        disconnectEvent.fire(payload);

        assertNotNull(received.get());
        assertEquals("POSTGRESQL", received.get().databaseType());
        assertEquals("analytics_db", received.get().databaseName());
        assertEquals("Connection timeout after 30s", received.get().reason());
        assertEquals("5432", received.get().details().get("port"));
        assertNotNull(received.get().disconnectedAt());
    }
}
