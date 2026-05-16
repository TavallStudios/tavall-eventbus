package org.tavall.enums;

public enum EventPriority {
    LOW(0),
    NORMAL(5),
    HIGH(10),
    CRITICAL(15);

    private final int level;

    EventPriority(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }
}

