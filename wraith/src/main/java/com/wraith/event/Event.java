package com.wraith.event;

/**
 * Base event. Events flow through {@link EventBus}; anything that mutates
 * vanilla behaviour is cancellable so a module can opt out of a hook cleanly.
 */
public class Event {
    private boolean cancelled;

    public boolean isCancelled() { return cancelled; }

    public void cancel() { this.cancelled = true; }

    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
}
