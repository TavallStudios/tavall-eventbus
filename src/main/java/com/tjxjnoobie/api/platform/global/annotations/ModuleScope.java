package com.tjxjnoobie.api.platform.global.annotations;

import com.tjxjnoobie.api.enums.EventDomain;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ModuleScope {

    EventDomain eventDomain(); // e.g., "Core", "KingdomFactions"
}