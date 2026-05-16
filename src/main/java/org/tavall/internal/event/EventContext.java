package org.tavall.internal.event;

import org.tavall.enums.*;

import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EventContext {
    private final EnumSet<EventTag> tags = EnumSet.noneOf(EventTag.class);
    private final EnumSet<EventCapability> capabilities = EnumSet.noneOf(EventCapability.class);
    private final Map<String, Object> data = new ConcurrentHashMap<>();
    private EventSource source;
    private EventStatus status = EventStatus.PENDING;
    private EventPriority priority = EventPriority.NORMAL;
    private EventDomain domain;
    private EventTag tag;

    public EnumSet<EventTag> getTags() { return tags; }
    public Map<String, Object> getData() { return data; }
    public EventSource getSource() { return source; }
    public EnumSet<EventCapability> getCapabilities() { return capabilities; }

    public EventStatus getStatus() { return status; }
    public EventPriority getPriority() { return priority; }
    public void setSource(EventSource source) { this.source = source; }
    public void setStatus(EventStatus status) { this.status = status; }
    public void setPriority(EventPriority priority) { this.priority = priority; }
    public void setDomain(EventDomain domain){
        this.domain = domain;
    }
    public void setTags(EventTag tag){
        this.tag = tag;
    }
    public EventContext put(String key, Object value) {
        data.put(key, value);
        return this;
    }
    public <T> T get(String key, Class<T> type) {
        return type.cast(data.get(key));
    }

    public boolean has(String key) {
        return data.containsKey(key);
    }

    public boolean containsKey(String key) {
        return data.containsKey(key);
    }
}

