package com.wraith.util;

/** Pack/unpack helpers plus the rainbow ramp every accent colour feeds off. */
public final class ColorUtil {

    private ColorUtil() {}

    public static int rgba(int rgb, int alpha) {
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }

    public static int argb(int a, int r, int g, int b) {
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int alpha(int color) { return (color >> 24) & 0xFF; }
    public static int red(int color) { return (color >> 16) & 0xFF; }
    public static int green(int color) { return (color >> 8) & 0xFF; }
    public static int blue(int color) { return color & 0xFF; }

    public static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0xFFFFFF);
    }

    public static int fade(int color, float factor) {
        int a = Math.round(alpha(color) * factor);
        return argb(Math.min(255, Math.max(0, a)), red(color), green(color), blue(color));
    }

    /** Cycle through the hue ramp; offset lets stacked rows desynchronise. */
    public static int rainbow(float speed, int offset, int alpha) {
        float hue = (float) ((System.currentTimeMillis() % (long) (360000L / Math.max(1f, speed))) / 600d
                + offset * 12d) % 360f / 360f;
        int rgb = java.awt.Color.HSBtoRGB(hue, 0.65f, 1f);
        return argb(alpha, (rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
    }

    public static int blend(int from, int to, float t) {
        t = Math.min(1f, Math.max(0f, t));
        return argb(
                Math.round(alpha(from) + (alpha(to) - alpha(from)) * t),
                Math.round(red(from) + (red(to) - red(from)) * t),
                Math.round(green(from) + (green(to) - green(from)) * t),
                Math.round(blue(from) + (blue(to) - blue(from)) * t));
    }
}
