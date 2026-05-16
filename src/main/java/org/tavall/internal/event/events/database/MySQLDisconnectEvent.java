package org.tavall.internal.event.events.database;
import org.tavall.platform.global.abstracts.AbstractEvent;

/**
 * Represents a MySQL disconnection event with suppression logic to avoid spamming.
 * Suppression can be toggled and timed, and runs asynchronously.
 */
public class MySQLDisconnectEvent extends AbstractEvent {
    private final String reason;

    public MySQLDisconnectEvent(String reason) {
        this.reason = reason;
        this.setSuppressible(true).isDatabase().isAPI();
        // Let AbstractEvent handle tags and suppression setup, so no repeats here
    }

    public String getReason() {
        return reason;
    }
}
