package com.wraith.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a single-argument method as an event handler. Kept tiny on purpose:
 * a reflective bus that scans once at registration costs nothing at runtime.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EventTarget {
    /** Lower runs first. Use negative values for modules that must win ties. */
    int priority() default 0;
}
