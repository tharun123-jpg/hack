package com.wraith.modules.render;

import com.wraith.WraithClient;
import com.wraith.event.events.RenderHudEvent;
import com.wraith.gui.Render2D;
import com.wraith.module.Category;
import com.wraith.module.Module;
import com.wraith.module.settings.BooleanSetting;
import com.wraith.module.settings.ColorSetting;
import com.wraith.module.settings.ModeSetting;
import com.wraith.module.settings.NumberSetting;
import com.wraith.util.ColorUtil;
import com.wraith.util.MCUtil;
import net.minecraft.client.gui.DrawContext;

/**
 * Replaces the vanilla sprite with a drawn crosshair. MixinInGameHud hides the
 * vanilla one while this is on; the hit test still uses vanilla's raycast, so this
 * is appearance only.
 */
public class CustomCrosshair extends Module {

    private final ModeSetting style = mode("Style", "Cross", "Cross", "Dot", "Circle", "Ticks", "None");
    private final NumberSetting size = num("Size", 5.0, 1.0, 14.0, 0.5, "px");
    private final NumberSetting thickness = num("Thickness", 1.0, 1.0, 4.0, 0.5, "px");
    private final NumberSetting gap = num("Gap", 2.0, 0.0, 8.0, 0.5, "px");
    private final BooleanSetting outline = bool("Outline", true);
    private final BooleanSetting dynamicColor = bool("Hit Colour", true);
    private final ColorSetting color = color("Colour", 0xFFFFFF);

    private float hitPulse;

    public CustomCrosshair() {
        super("Crosshair", "Vector crosshair with gap, weight and colour control", Category.RENDER);
    }

    /** Used by MixinInGameHud to decide whether to cancel the vanilla sprite. */
    public boolean hidesVanilla() { return isEnabled() && !style.is("None"); }

    @Override
    protected void onRenderHud(RenderHudEvent event) {
        if (style.is("None")) return;
        DrawContext context = event.context();
        int w = MCUtil.screenWidth();
        int h = MCUtil.screenHeight();
        // Vanilla crosshair is drawn at the exact centre of the screen.
        float cx = w / 2f;
        float cy = h / 2f;

        boolean targeting = MCUtil.mc().crosshairTarget != null
                && MCUtil.mc().crosshairTarget.getType() != net.minecraft.util.hit.HitResult.Type.MISS;
        hitPulse = Render2D.animate(hitPulse, targeting && dynamicColor.on() ? 1f : 0f, 14f);

        int base = color.alpha(235);
        int tinted = ColorUtil.blend(base, WraithClient.modules().gui().theme().accent(255), hitPulse);
        int outlineColor = ColorUtil.argb(200, 0, 0, 0);
        float s = size.floatGet();
        float g = gap.floatGet();
        float t = thickness.floatGet();

        switch (style.get()) {
            case "Dot" -> {
                float d = Math.max(2f, t * 2f);
                if (outline.on()) Render2D.rounded(context, cx - d / 2 - 1, cy - d / 2 - 1, d + 2, d + 2, d / 2, outlineColor);
                Render2D.rounded(context, cx - d / 2, cy - d / 2, d, d, d / 2, tinted);
            }
            case "Circle" -> {
                if (outline.on()) circle(context, cx, cy, s + t, outlineColor);
                circle(context, cx, cy, s, tinted);
            }
            case "Ticks" -> {
                for (int i = 0; i < 4; i++) {
                    boolean horizontal = i % 2 == 0;
                    float sign = i < 2 ? 1 : -1;
                    drawBar(context, cx, cy, horizontal, g, s, t, sign, tinted, outline.on());
                }
            }
            default -> {
                for (int i = 0; i < 4; i++) {
                    boolean horizontal = i % 2 == 0;
                    float sign = i < 2 ? 1 : -1;
                    drawBar(context, cx, cy, horizontal, g, s, t, sign, tinted, outline.on());
                }
            }
        }
    }

    private void drawBar(DrawContext context, float cx, float cy, boolean horizontal, float gap, float size,
                         float thickness, float sign, int color, boolean outline) {
        float x = horizontal ? cx + sign * gap : cx - thickness / 2;
        float y = horizontal ? cy - thickness / 2 : cy + sign * gap;
        float w = horizontal ? size : thickness;
        float h = horizontal ? thickness : size;
        if (outline) Render2D.rect(context, x - sign, y - (horizontal ? 0 : sign), w + 2, h + 2, ColorUtil.argb(190, 0, 0, 0));
        Render2D.rect(context, x, y, w, h, color);
    }

    /** Approximated with axis-aligned ticks; crisp and cheap at GUI scale 1-4. */
    private void circle(DrawContext context, float cx, float cy, float radius, int color) {
        int steps = Math.max(12, (int) (radius * 6));
        for (int i = 0; i < steps; i++) {
            double a0 = Math.toRadians(360d * i / steps);
            float x = cx + (float) Math.cos(a0) * radius;
            float y = cy + (float) Math.sin(a0) * radius;
            Render2D.rect(context, x, y, 1.2f, 1.2f, color);
        }
    }
}
