package org.tavall.event.database;

import org.tavall.enums.EventDomain;
import org.tavall.enums.EventPriority;
import org.tavall.enums.EventTag;
import org.tavall.event.EventRouter;
import org.tavall.event.EventSettings;
import org.tavall.event.IEventRouter;
import org.tavall.event.TavallEvent;

/**
 * Concrete event definition for database disconnections.
 * Extends {@link TavallEvent} bound to the typed payload {@link DatabaseDisconnectPayload}.
 */
public class DatabaseDisconnectEvent extends TavallEvent<DatabaseDisconnectPayload> {

    public DatabaseDisconnectEvent() {
        this(EventRouter.getDefault());
    }

    public DatabaseDisconnectEvent(IEventRouter router) {
        super(
                DatabaseDisconnectPayload.class,
                EventSettings.builder()
                        .domain(EventDomain.DATABASE)
                        .tag(EventTag.DATABASE)
                        .priority(EventPriority.HIGH)
                        .cancellable(false)
                        .build(),
                router
        );
    }
}
