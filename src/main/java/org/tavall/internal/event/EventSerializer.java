package org.tavall.internal.event;

import org.tavall.platform.global.abstracts.AbstractEvent;

public class EventSerializer {

    public static String serialize(AbstractEvent event) {
        // Basic example: customize for your data
        return "{\"type\":\"" + event.getClass().getSimpleName() +
                "\",\"status\":\"" + event.getStatus() +
                "\",\"tags\":\"" + event.getTags() + "\"}";
    }

    public static AbstractEvent deserialize(String json) {
        // This would normally require reflection or a type map
        return null; // Stub, to implement if you want dynamic event reconstruction
    }
}
