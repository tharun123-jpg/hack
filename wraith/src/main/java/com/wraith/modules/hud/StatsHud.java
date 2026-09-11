package com.wraith.modules.hud;

import com.wraith.WraithClient;
import com.wraith.gui.Render2D;
import com.wraith.gui.hud.Anchor;
import com.wraith.gui.hud.HudModule;
import com.wraith.module.settings.BooleanSetting;
import com.wraith.module.settings.ModeSetting;
import com.wraith.util.ColorUtil;
import com.wraith.util.InputUtil;
import net.minecraft.client.gui.DrawContext;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;

/**
 * FPS, frame time, uptime and heap - all from the JVM, so this panel never needs a
 * render hook of its own and cannot desync from the game loop.
 */
public class StatsHud extends HudModule {

    private final ModeSetting rows = mode("Rows", "FPS + Ms", "FPS", "FPS + Ms", "All");
    private final BooleanSetting sparkline = bool("Frame Graph", true);
    private final BooleanSetting memory = bool("Memory", false);

    private final float[] history = new float[48];
    private int cursor;

    public StatsHud() {
        super("Stats", "Frame rate, frame time, uptime and heap", Anchor.BOTTOM_LEFT);
    }

    @Override
    protected float draw(DrawContext context, float x, float y, float tickDelta) {
        float frameMs = InputUtil.frameDelta() * 1000f;
        history[cursor = (cursor + 1) % history.length] = frameMs;

        java.util.List<String> lines = new ArrayList<>();
        lines.add("FPS " + (frameMs <= 0.01f ? 0 : Math.round(1000f / frameMs)));
        if (!rows.is("FPS")) lines.add(String.format(java.util.Locale.ROOT, "Ms %.2f", frameMs));
        if (rows.is("All")) {
            lines.add("Up " + uptime());
            if (memory.on()) lines.add("Heap " + (usedHeap() / 1024 / 1024) + " MB");
        }

        float widest = 0;
        for (String line : lines) widest = Math.max(widest, Render2D.textWidth(line));
        float graphW = sparkline.on() ? 30 : 0;
        float boxW = widest + 8 + graphW;
        float boxH = lines.size() * 10f + 4;

        Render2D.rounded(context, x, y, boxW, boxH, 3, theme().background());
        float cursorY = y + 3;
        int accent = theme().accent(255);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            Render2D.text(context, line, x + 4, cursorY,
                    i == 0 ? accent : ColorUtil.withAlpha(theme().text(), 220), false);
            cursorY += 10;
        }
        if (sparkline.on()) drawGraph(context, x + boxW - graphW - 2, y + 3, graphW, boxH - 6);
        size(boxW, boxH);
        return boxW;
    }

    /** Last 48 frames as a mirrored bar graph; 33ms is the reference line. */
    private void drawGraph(DrawContext context, float x, float y, float w, float h) {
        float worst = 50f;
        for (float sample : history) worst = Math.max(worst, sample);
        Render2D.rect(context, x, y + h - h * (33f / worst), w, 1, ColorUtil.argb(90, 255, 255, 255));
        float barW = Math.max(1f, w / history.length);
        for (int i = 0; i < history.length; i++) {
            int idx = (cursor + 1 + i) % history.length;
            float value = history[idx];
            float barH = Math.max(1f, h * Math.min(1f, value / worst));
            int color = value > 50 ? ColorUtil.argb(220, 255, 92, 92)
                    : value > 33 ? ColorUtil.argb(200, 255, 190, 90)
                    : ColorUtil.withAlpha(theme().accent(255), 200);
            Render2D.rect(context, x + i * barW, y + h - barH, barW - 0.5f, barH, color);
        }
    }

    private static String uptime() {
        long seconds = ManagementFactory.getRuntimeMXBean().getUptime() / 1000;
        return String.format(java.util.Locale.ROOT, "%d:%02d:%02d", seconds / 3600, (seconds / 60) % 60, seconds % 60);
    }

    private static long usedHeap() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
}
