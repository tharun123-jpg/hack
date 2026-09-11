package com.wraith.modules.hud;

import com.wraith.WraithClient;
import com.wraith.gui.Render2D;
import com.wraith.gui.hud.Anchor;
import com.wraith.gui.hud.HudModule;
import com.wraith.module.settings.BooleanSetting;
import com.wraith.module.settings.ModeSetting;
import com.wraith.util.ColorUtil;
import net.minecraft.client.gui.DrawContext;

/** Brand plate. Corner-rounded, accent underline, optional session clock. */
public class Watermark extends HudModule {

    private final ModeSetting content = mode("Content", "Name + Version", "Name", "Name + Version", "Custom");
    private final BooleanSetting sessionTime = bool("Session Time", true);
    private final BooleanSetting backdrop = bool("Backdrop", true);

    public Watermark() {
        super("Watermark", "Client name and version in the corner", Anchor.TOP_LEFT);
    }

    @Override
    protected float draw(DrawContext context, float x, float y, float tickDelta) {
        String text = switch (content.get()) {
            case "Name" -> WraithClient.NAME;
            case "Custom" -> WraithClient.NAME.toLowerCase();
            default -> WraithClient.NAME + " " + WraithClient.VERSION;
        };
        if (sessionTime.on()) text += "  " + sessionClock();

        int width = Render2D.textWidth(text);
        int accent = WraithClient.modules().gui().theme().accent(255);
        float pad = 4f;
        float height = 11f;

        if (backdrop.on()) {
            Render2D.rounded(context, x, y, width + pad * 2, height, 3, theme().background());
            Render2D.rect(context, x, y + height - 1, (width + pad * 2) * 0.4f, 1, accent);
        }
        Render2D.text(context, text, x + pad, y + (height - Render2D.FONT_HEIGHT) / 2f + 1,
                ColorUtil.withAlpha(theme().text(), backdrop.on() ? 255 : 200), true);
        size(width + pad * 2, height);
        return width + pad * 2;
    }

    private static String sessionClock() {
        long seconds = (System.nanoTime() - START) / 1_000_000_000L;
        return String.format(java.util.Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }

    private static final long START = System.nanoTime();
}
