package org.tavall.event.database;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Plain data payload representing a database disconnection event.
 * Carries connection and failure metadata without any framework inheritance.
 */
public record DatabaseDisconnectPayload(
        String databaseType,
        String databaseName,
        String reason,
        Map<String, String> details,
        Instant disconnectedAt
) {
    public DatabaseDisconnectPayload {
        Objects.requireNonNull(databaseType, "databaseType cannot be null");
        details = details == null ? Map.of() : Map.copyOf(details);
        disconnectedAt = disconnectedAt == null ? Instant.now() : disconnectedAt;
    }

    public static DatabaseDisconnectPayload of(String databaseType, String reason) {
        return new DatabaseDisconnectPayload(databaseType, "default", reason, Map.of(), Instant.now());
    }
}
