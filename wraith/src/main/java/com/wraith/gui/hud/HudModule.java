package com.wraith.gui.hud;

import com.wraith.WraithClient;
import com.wraith.event.events.RenderHudEvent;
import com.wraith.gui.Render2D;
import com.wraith.module.Category;
import com.wraith.module.Module;
import com.wraith.module.settings.BooleanSetting;
import com.wraith.module.settings.NumberSetting;
import com.wraith.util.MCUtil;
import net.minecraft.client.gui.DrawContext;

/**
 * A module that draws a box on the HUD.
 *
 * Position is stored as an anchor plus a pixel offset instead of raw x/y, which is
 * what stops elements drifting off-screen on resize or GUI-scale change. The box
 * size used for alignment is last frame's measurement (standard for HUD layers:
 * text metrics are stable frame to frame, so it never visibly lags).
 */
public abstract class HudModule extends Module implements HudElementAccessor {

    protected final BooleanSetting shadow = bool("Shadow", true);
    protected final NumberSetting scale = num("Scale", 1.0, 0.5, 2.0, 0.05, "x");

    private Anchor anchor = Anchor.TOP_LEFT;
    private float offsetX = 4;
    private float offsetY = 4;
    private float measuredWidth;
    private float measuredHeight;

    protected HudModule(String name, String description, Anchor defaultAnchor) {
        super(name, description, Category.HUD);
        this.anchor = defaultAnchor;
    }

    /**
     * Draw with (0,0) as the box origin and report the width used. Call
     * {@link #size(float, float)} to record an exact box (e.g. padded rows).
     */
    protected abstract float draw(DrawContext context, float x, float y, float tickDelta);

    public Anchor anchor() { return anchor; }
    public float offsetX() { return offsetX; }
    public float offsetY() { return offsetY; }
    public boolean shadow() { return shadow.on(); }
    public float scaleValue() { return scale.floatGet(); }

    /** Box in screen space, scale applied - hit testing and dragging use this. */
    public float boxX() {
        return Render2D.alignX(anchor, MCUtil.screenWidth(), measuredWidth * scaleValue()) + offsetX;
    }

    public float boxY() {
        return Render2D.alignY(anchor, MCUtil.screenHeight(), measuredHeight * scaleValue()) + offsetY;
    }

    public float boxWidth() { return measuredWidth * scaleValue(); }
    public float boxHeight() { return measuredHeight * scaleValue(); }

    public boolean contains(float px, float py, float pad) {
        return Render2D.inBox(px, py, boxX() - pad, boxY() - pad, boxWidth() + pad * 2, boxHeight() + pad * 2);
    }

    /**
     * Repoint the anchor while dragging: whichever third of the screen the box
     * centre falls into becomes the anchor, so a release near a screen edge or the
     * middle snaps there and the offset is re-based against it.
     */
    void draggedTo(float pointerX, float pointerY, float grabX, float grabY) {
        float w = boxWidth();
        float h = boxHeight();
        float left = pointerX - grabX;
        float top = pointerY - grabY;
        anchor = nearestAnchor((left + w / 2f) / Math.max(1f, MCUtil.screenWidth()),
                (top + h / 2f) / Math.max(1f, MCUtil.screenHeight()));
        offsetX = Math.round(left - Render2D.alignX(anchor, MCUtil.screenWidth(), w));
        offsetY = Math.round(top - Render2D.alignY(anchor, MCUtil.screenHeight(), h));
        WraithClient.config().markDirty();
    }

    @Override public Anchor hudAnchor() { return anchor; }
    @Override public float hudOffsetX() { return offsetX; }
    @Override public float hudOffsetY() { return offsetY; }
    @Override public void hudSetAnchor(Anchor anchor) { this.anchor = anchor; }
    @Override public void hudSetOffsetX(float x) { this.offsetX = x; }
    @Override public void hudSetOffsetY(float y) { this.offsetY = y; }

    public void resetPosition() {
        anchor = Anchor.TOP_LEFT;
        offsetX = 4;
        offsetY = 4;
    }

    private static Anchor nearestAnchor(float nx, float ny) {
        int col = nx < 1f / 3f ? 0 : nx > 2f / 3f ? 2 : 1;
        int row = ny < 1f / 3f ? 0 : ny > 2f / 3f ? 2 : 1;
        for (Anchor value : Anchor.values()) {
            if (value.row == row && value.col == col) return value;
        }
        return Anchor.TOP_LEFT;
    }

    protected final void size(float width, float height) {
        measuredWidth = Math.max(0, width);
        measuredHeight = Math.max(0, height);
    }

    protected com.wraith.gui.Theme theme() {
        return WraithClient.modules().gui().theme();
    }

    protected final float textWidth(String text) {
        return MCUtil.mc().textRenderer.getWidth(text);
    }

    @Override
    protected void onRenderHud(RenderHudEvent event) {
        if (event != null) renderBox(event.context(), event.tickDelta());
    }

    /**
     * Draw regardless of enabled state. The HUD editor uses this so a disabled
     * element still has a real box to grab - without it, "show disabled" could only
     * ever draw a zero-sized placeholder.
     */
    public void renderBox(DrawContext context, float delta) {
        float scale = scaleValue();
        float x = boxX();
        float y = boxY();
        Render2D.push(context, x, y, scale);
        float w = draw(context, 0, 0, delta);
        Render2D.pop(context);
        if (measuredHeight <= 0 && w > 0) size(w, 12);
        else if (w > 0) size(w, measuredHeight);
    }
}
