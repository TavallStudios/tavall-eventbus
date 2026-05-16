package org.tavall.internal.event.tags.interfaces;

import org.tavall.platform.global.abstracts.AbstractEvent;

public interface EventListener<T extends AbstractEvent> {

    boolean supports(AbstractEvent event);

    void handle(T event);
}
