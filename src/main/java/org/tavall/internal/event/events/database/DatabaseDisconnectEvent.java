package org.tavall.internal.event.events.database;

import org.tavall.platform.global.abstracts.AbstractEvent;
import org.tavall.enums.*;
import org.tavall.managers.MySQL;
import org.tavall.managers.Redis;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

abstract class DatabaseDisconnectEvent extends AbstractEvent {
    public enum DatabaseType {
        MYSQL, REDIS, GRAPHQL, POSTGRES, MONGODB
    }

    private final DatabaseType type;
    private final Map<String, String> details;
    private final MySQL mySQL;
    private final Redis redis;

    protected DatabaseDisconnectEvent(DatabaseType type, Map<String, String> details, MySQL mySQL, Redis redis) {
        this.type = type;
        this.details = details != null ? new ConcurrentHashMap<>(details) : new ConcurrentHashMap<>();
        this.mySQL = mySQL;
        this.redis = redis;
        this.enableCapabilities(EventCapability.SUPPRESSIBLE, EventCapability.ASYNC,
                EventCapability.FIREABLE, EventCapability.RETRYABLE)
                .disableCapabilities(EventCapability.CANCELABLE)
                .setSource(EventSource.DATABASE_MONITOR)
                .setTags(EventTag.DATABASE).setStatus(EventStatus.PENDING).setDomain(EventDomain.DATABASE);
    }

    public DatabaseType getDatabaseType() {
        return type;
    }

    public Map<String, String> getDetails() {
        return Map.copyOf(details);
    }

    public String getDetail(String key) {
        return details.get(key);
    }

    public MySQL getMySQL() {
        return mySQL;
    }

    public Redis getRedis() {
        return redis;
    }
}
