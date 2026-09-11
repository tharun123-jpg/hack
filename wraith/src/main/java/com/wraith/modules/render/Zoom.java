package com.wraith.modules.render;

import com.wraith.WraithClient;
import com.wraith.event.events.RenderHudEvent;
import com.wraith.module.Category;
import com.wraith.module.Module;
import com.wraith.module.settings.BooleanSetting;
import com.wraith.module.settings.ModeSetting;
import com.wraith.module.settings.NumberSetting;
import com.wraith.gui.Render2D;
import com.wraith.util.InputUtil;
import com.wraith.util.MCUtil;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

/**
 * Scales the FOV down. Called from MixinGameRenderer, which keeps the maths out of
 * the render pipeline; everything here is state and easing.
 */
public class Zoom extends Module {

    private final NumberSetting distance = num("Distance", 4.0, 1.0, 16.0, 0.25, "x");
    private final ModeSetting activation = mode("Activation", "Toggle", "Toggle", "Hold");
    private final BooleanSetting smooth = bool("Smooth", true);
    private final BooleanSetting fadeOut = bool("Fade Out", true);

    private float progress;
    private boolean holding;

    public Zoom() {
        super("Zoom", "Optifine-style magnification of the camera frustum", Category.RENDER);
        setBind(GLFW.GLFW_KEY_C);
    }

    public boolean holding() {
        return activation.is("Hold") && isEnabled() && InputUtil.isDown(bind());
    }

    /**
     * Called every render frame from the FOV hook, not just when enabled, so the
     * ease-in/ease-out can complete after the module is switched off.
     */
    public float modifyFov(float fov) {
        boolean wantActive = isEnabled() && (!activation.is("Hold") || holding());
        if (!wantActive && !fadeOut.on() && progress >= 1f) progress = 0f;
        float target = wantActive ? 1f : 0f;
        progress = Render2D.animate(progress, target, smooth.on() ? 10f : 60f);
        if (progress <= 0.001f) return fov;
        float scale = (float) distance.get();
        // Ease the exponent so the animation is perceptually even, not front-loaded.
        float eased = (float) Math.pow(progress, 0.7);
        return fov / (1f + (scale - 1f) * eased);
    }

    @Override
    protected void onRenderHud(RenderHudEvent event) {
        if (progress <= 0.01f) return;
        // Vignette ring: makes it obvious the zoom is engaged, like the reference client.
        int w = MCUtil.screenWidth();
        int h = MCUtil.screenHeight();
        int alpha = Math.round(90 * progress);
        int color = WraithClient.modules().gui().theme().accent(alpha);
        float inset = 6 + 18 * progress;
        DrawContext context = event.context();
        Render2D.roundedStroke(context, inset, inset, w - inset * 2, h - inset * 2, 26, color);
    }

    @Override
    public String suffix() {
        return String.format(java.util.Locale.ROOT, "%.2fx", distance.get() * Math.max(0.001f, progress));
    }

    @Override
    protected void onDisable() {
        if (!fadeOut.on()) progress = 0f;
    }
}
