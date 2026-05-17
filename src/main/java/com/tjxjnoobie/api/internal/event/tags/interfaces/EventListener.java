package com.tjxjnoobie.api.internal.event.tags.interfaces;

import com.tjxjnoobie.api.platform.global.abstracts.AbstractEvent;

public interface EventListener<T extends AbstractEvent> {

    boolean supports(AbstractEvent event);

    void handle(T event);
}