package com.wraith.event.events;

import com.wraith.event.Event;

/** Fired on join/leave so stateful modules can reset themselves. */
public class WorldEvent extends Event {
    public static final WorldEvent JOIN = new WorldEvent(true);
    public static final WorldEvent LEAVE = new WorldEvent(false);

    private final boolean joining;

    private WorldEvent(boolean joining) { this.joining = joining; }

    public boolean isJoining() { return joining; }
}
