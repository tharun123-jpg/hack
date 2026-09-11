package com.wraith.gui;

import com.wraith.gui.hud.Anchor;
import com.wraith.util.MCUtil;
import net.minecraft.client.gui.DrawContext;

/**
 * The only file in the project that talks to DrawContext's matrix/stack API.
 *
 * Minecraft's 2D layer renames things between minor versions (getMatrices() vs
 * matrices(), scissor argument order, Identifier vs ResourceLocation), so the
 * whole client is deliberately funnelled through these few methods: when Loom
 * complains, the fix belongs here and nowhere else.
 */
public final class Render2D {

    public static final int FONT_HEIGHT = 9;

    private Render2D() {}

    // ---- transform -------------------------------------------------------

    /**
     * 1.21.2+ uses a 2D Matrix3x2fStack (joml), so translate/scale take two args
     * and push/pop are the stack ops. Older Yarn (1.21.1 and below) hands back a
     * MatrixStack instead; if that is your target, change these three bodies to
     * translate(x, y, 0) / scale(scale, scale, 1) / pushMatrix()+popMatrix() - and
     * change nothing else, since every module draws through here.
     */
    public static void push(DrawContext context, float x, float y, float scale) {
        matrices(context).push();
        matrices(context).translate(x, y);
        if (Math.abs(scale - 1f) > 0.001f) matrices(context).scale(scale, scale);
    }

    public static void pop(DrawContext context) {
        matrices(context).pop();
    }

    private static org.joml.Matrix3x2fStack matrices(DrawContext context) {
        return context.getMatrices();
    }

    // ---- primitives ------------------------------------------------------

    public static void rect(DrawContext context, float x, float y, float w, float h, int argb) {
        if (w <= 0 || h <= 0) return;
        context.fill(round(x), round(y), round(x + w), round(y + h), argb);
    }

    public static void hline(DrawContext context, float x, float y, float w, int argb) {
        rect(context, x, y, w, 1, argb);
    }

    public static void vline(DrawContext context, float x, float y, float h, int argb) {
        rect(context, x, y, 1, h, argb);
    }

    public static void gradient(DrawContext context, float x, float y, float w, float h, int top, int bottom) {
        if (w <= 0 || h <= 0) return;
        context.fillGradient(round(x), round(y), round(x + w), round(y + h), top, bottom);
    }

    /** Corner-rounded box. Rows are inset along a circle: cheap, crisp at GUI scale. */
    public static void rounded(DrawContext context, float x, float y, float w, float h, float radius, int argb) {
        if (w <= 0 || h <= 0) return;
        int xi = round(x);
        int yi = round(y);
        int wi = round(x + w) - xi;
        int hi = round(y + h) - yi;
        int r = Math.min((int) radius, Math.min(wi, hi) / 2);
        if (r <= 0) {
            context.fill(xi, yi, xi + wi, yi + hi, argb);
            return;
        }
        context.fill(xi, yi + r, xi + wi, yi + hi - r, argb);
        for (int j = 1; j <= r; j++) {
            int inset = r - (int) Math.floor(Math.sqrt(Math.max(0d, (double) r * r - (double) (r - j) * (r - j))));
            int rowTop = yi + j - 1;
            int rowBottom = yi + hi - j;
            context.fill(xi + inset, rowTop, xi + wi - inset, rowTop + 1, argb);
            context.fill(xi + inset, rowBottom, xi + wi - inset, rowBottom + 1, argb);
        }
    }

    public static void roundedOutline(DrawContext context, float x, float y, float w, float h, float radius,
                                      int fill, int outline, float thickness) {
        rounded(context, x, y, w, h, radius, outline);
        float t = Math.max(1f, thickness);
        rounded(context, x + t, y + t, w - t * 2, h - t * 2, Math.max(0, radius - t), fill);
    }

    /**
     * Perimeter-only rounded box. DrawContext has no stroke primitive, so the outline
     * is emitted as 1px fills: straight edges plus one pixel per row through the
     * corner radius. Needed because a "0 thickness" translucent fill is not a stroke.
     */
    public static void roundedStroke(DrawContext context, float x, float y, float w, float h, float radius, int color) {
        if (w <= 0 || h <= 0) return;
        int xi = round(x);
        int yi = round(y);
        int wi = round(x + w) - xi;
        int hi = round(y + h) - yi;
        int r = Math.min((int) radius, Math.min(wi, hi) / 2);
        if (r <= 0) {
            context.fill(xi, yi, xi + wi, yi + 1, color);
            context.fill(xi, yi + hi - 1, xi + wi, yi + hi, color);
            context.fill(xi, yi + 1, xi + 1, yi + hi - 1, color);
            context.fill(xi + wi - 1, yi + 1, xi + wi, yi + hi - 1, color);
            return;
        }
        context.fill(xi + r, yi, xi + wi - r, yi + 1, color);
        context.fill(xi + r, yi + hi - 1, xi + wi - r, yi + hi, color);
        context.fill(xi, yi + r, xi + 1, yi + hi - r, color);
        context.fill(xi + wi - 1, yi + r, xi + wi, yi + hi - r, color);
        for (int j = 1; j < r; j++) {
            int inset = r - (int) Math.floor(Math.sqrt(Math.max(0d, (double) r * r - (double) (r - j) * (r - j))));
            int top = yi + j;
            int bottom = yi + hi - 1 - j;
            context.fill(xi + inset, top, xi + inset + 1, top + 1, color);
            context.fill(xi + wi - 1 - inset, top, xi + wi - inset, top + 1, color);
            context.fill(xi + inset, bottom, xi + inset + 1, bottom + 1, color);
            context.fill(xi + wi - 1 - inset, bottom, xi + wi - inset, bottom + 1, color);
        }
    }

    public static void roundedGradient(DrawContext context, float x, float y, float w, float h, float radius,
                                       int top, int bottom) {
        if (w <= 0 || h <= 0) return;
        int steps = Math.max(1, (int) (h / 2));
        for (int i = 0; i < steps; i++) {
            float t0 = (float) i / steps;
            float t1 = (float) (i + 1) / steps;
            int color = com.wraith.util.ColorUtil.blend(top, bottom, t0);
            float ry = y + h * t0;
            rounded(context, x, ry, w, Math.max(1, h * (t1 - t0)), radius, color);
        }
    }

    // ---- text ------------------------------------------------------------

    public static int textWidth(String text) {
        return MCUtil.mc().textRenderer.getWidth(text);
    }

    public static void text(DrawContext context, String text, float x, float y, int argb, boolean shadow) {
        if (text == null || text.isEmpty()) return;
        int xi = round(x);
        int yi = round(y);
        if (shadow) {
            context.drawTextWithShadow(MCUtil.mc().textRenderer, text, xi, yi, argb);
        } else {
            context.drawText(MCUtil.mc().textRenderer, text, xi, yi, argb, false);
        }
    }

    /** 1px drop shadow behind text - enough legibility on any backdrop. */
    public static void textShadow(DrawContext context, String text, float x, float y, int argb) {
        text(context, text, x + 1, y + 1, com.wraith.util.ColorUtil.withAlpha(argb, 120), false);
        text(context, text, x, y, argb, false);
    }

    public static float centered(String text, float x, float w) {
        return x + w / 2f - textWidth(text) / 2f;
    }

    // ---- clipping --------------------------------------------------------

    public static void scissor(DrawContext context, float x, float y, float w, float h) {
        context.enableScissor(round(x), round(y), round(x + w), round(y + h));
    }

    public static void unscissor(DrawContext context) {
        context.disableScissor();
    }

    /** Draw fn may read mouse state; wrap in try/finally so a throw can't leak a scissor. */
    public static void clipped(DrawContext context, float x, float y, float w, float h, Runnable draw) {
        scissor(context, x, y, w, h);
        try {
            draw.run();
        } finally {
            unscissor(context);
        }
    }

    // ---- geometry --------------------------------------------------------

    public static boolean inBox(double px, double py, double x, double y, double w, double h) {
        return px >= x && px <= x + w && py >= y && py <= y + h;
    }

    public static float alignX(Anchor anchor, int screenW, float width) {
        return switch (anchor.col) {
            case 0 -> 0f;
            case 1 -> screenW / 2f - width / 2f;
            default -> screenW - width;
        };
    }

    public static float alignY(Anchor anchor, int screenH, float height) {
        return switch (anchor.row) {
            case 0 -> 0f;
            case 1 -> screenH / 2f - height / 2f;
            default -> screenH - height;
        };
    }

    /**
     * Frame-rate independent exponential approach; used for every GUI animation.
     * Uses our own frame timer rather than Minecraft's, because the accessor for
     * last-frame time moved to RenderTickCounter and varies more than the maths.
     */
    public static float animate(float current, float target, float speed) {
        float delta = Math.min(1f, speed * com.wraith.util.InputUtil.frameDelta() / 1000f);
        return current + (target - current) * delta;
    }

    private static int round(float value) {
        return Math.round(value);
    }
}
