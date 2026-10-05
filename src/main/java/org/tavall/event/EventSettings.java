package org.tavall.event;

import org.tavall.enums.EventDomain;
import org.tavall.enums.EventPriority;
import org.tavall.enums.EventTag;

import java.time.Duration;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable typed configuration settings for a {@link TavallEvent}.
 * Defines runtime dispatch behavior, cancellation support, priority, tags, and domain scoping.
 */
public final class EventSettings {

    private static final EventSettings DEFAULT = builder().build();

    private final boolean async;
    private final boolean cancellable;
    private final EventPriority priority;
    private final Duration timeout;
    private final Set<EventTag> tags;
    private final EventDomain domain;

    private EventSettings(Builder builder) {
        this.async = builder.async;
        this.cancellable = builder.cancellable;
        this.priority = builder.priority;
        this.timeout = builder.timeout;
        this.tags = builder.tags.isEmpty()
                ? Collections.emptySet()
                : Collections.unmodifiableSet(EnumSet.copyOf(builder.tags));
        this.domain = builder.domain;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static EventSettings defaultSettings() {
        return DEFAULT;
    }

    public boolean isAsync() {
        return async;
    }

    public boolean isCancellable() {
        return cancellable;
    }

    public EventPriority getPriority() {
        return priority;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public Set<EventTag> getTags() {
        return tags;
    }

    public EventDomain getDomain() {
        return domain;
    }

    public static final class Builder {
        private boolean async = false;
        private boolean cancellable = true;
        private EventPriority priority = EventPriority.NORMAL;
        private Duration timeout = Duration.ofSeconds(30);
        private final Set<EventTag> tags = EnumSet.noneOf(EventTag.class);
        private EventDomain domain = EventDomain.GLOBAL;

        private Builder() {}

        public Builder async(boolean async) {
            this.async = async;
            return this;
        }

        public Builder cancellable(boolean cancellable) {
            this.cancellable = cancellable;
            return this;
        }

        public Builder priority(EventPriority priority) {
            this.priority = Objects.requireNonNull(priority, "priority cannot be null");
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = Objects.requireNonNull(timeout, "timeout cannot be null");
            return this;
        }

        public Builder tag(EventTag tag) {
            this.tags.add(Objects.requireNonNull(tag, "tag cannot be null"));
            return this;
        }

        public Builder tags(Set<EventTag> tags) {
            Objects.requireNonNull(tags, "tags cannot be null");
            this.tags.clear();
            this.tags.addAll(tags);
            return this;
        }

        public Builder domain(EventDomain domain) {
            this.domain = Objects.requireNonNull(domain, "domain cannot be null");
            return this;
        }

        public EventSettings build() {
            return new EventSettings(this);
        }
    }
}
