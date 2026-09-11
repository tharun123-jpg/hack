package com.wraith.event.events;

import com.wraith.event.Event;

/** Fired once per client tick, before vanilla's tick body runs. */
public class TickEvent extends Event {
    public static final TickEvent INSTANCE = new TickEvent();
}
