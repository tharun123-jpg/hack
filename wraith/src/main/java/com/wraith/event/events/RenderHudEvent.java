package com.wraith.event.events;

import com.wraith.event.Event;
import net.minecraft.client.gui.DrawContext;

/**
 * Fired every rendered frame while the in-game HUD draws. This is where all of
 * Wraith's 2D output (HUD elements, click GUI overlay) happens.
 */
public class RenderHudEvent extends Event {
    private final DrawContext context;
    private final float tickDelta;
    private final int screenWidth;
    private final int screenHeight;

    public RenderHudEvent(DrawContext context, float tickDelta, int screenWidth, int screenHeight) {
        this.context = context;
        this.tickDelta = tickDelta;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    public DrawContext context() { return context; }

    /** Convenience for widgets that only need to know the box they are in. */
    public int scaledWidth() { return screenWidth; }
    public int scaledHeight() { return screenHeight; }
    public float tickDelta() { return tickDelta; }
    public int screenWidth() { return screenWidth; }
    public int screenHeight() { return screenHeight; }
}
