package org.tavall.enums;

public enum EventSource {
    PLAYER_ACTION,         // Triggered by player interactions
    COMMAND_EXECUTION,     // Issued from a command
    SCHEDULED_TASK,        // Automated or cron-based system event
    DATABASE_MONITOR,      // Triggered by MySQL/Redis/GraphQL connection watchers
    REDIS_PUBSUB,          // Redis messaging or event pub-sub
    SYSTEM_BOOT,           // Server or plugin start sequence
    SYSTEM_SHUTDOWN,       // Graceful shutdown signals
    CROSS_REGION_BRIDGE,   // Remote call from another region/server
    VELOCITY_BRIDGE,       // Velocity proxy-specific trigger
    PAPER_BRIDGE,          // Paper-specific trigger (world logic, ticks, etc)
    API_REQUEST,           // External web server request hit this
    WEBHOOK_INBOUND,       // Discord/Webhook callback
    INTERNAL_EVENT_CHAIN,  // Triggered by another event firing internally
    FALLBACK_HANDLER,      // Recovery or failover logic
    UNKNOWN                // Unidentifiable or generic source
}

