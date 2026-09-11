package com.wraith.gui.component;

import com.wraith.WraithClient;
import com.wraith.gui.ClickGui;
import com.wraith.gui.Render2D;
import com.wraith.gui.Theme;
import com.wraith.module.Module;
import com.wraith.module.Setting;
import com.wraith.module.settings.BooleanSetting;
import com.wraith.module.settings.ColorSetting;
import com.wraith.module.settings.ModeSetting;
import com.wraith.module.settings.NumberSetting;
import com.wraith.util.ColorUtil;
import com.wraith.util.InputUtil;
import net.minecraft.client.gui.DrawContext;

/**
 * A module row plus, when expanded, its settings as drawn widgets.
 *
 * Height is derived from the same code path used for drawing, so a setting list
 * can grow or shrink (mode chips wrap) without the layout disagreeing with the
 * hit boxes.
 */
public final class ModuleCard {

    private static final float PADDING = 2f;

    private final Module module;
    private final ClickGui gui;
    private final boolean[] colorExpanded = new boolean[4];

    private float fill;
    private float expand;
    private float hover;

    public ModuleCard(Module module, ClickGui gui) {
        this.module = module;
        this.gui = gui;
    }

    public Module module() { return module; }
    public boolean expanded() { return module.isExtended(); }

    private float rowHeight(Theme theme) { return theme.rowHeight(); }

    /** Total height this card occupies, including expanded settings. */
    public float height(Theme theme) {
        float h = rowHeight(theme);
        if (expand <= 0.01f && !module.isExtended()) return h;
        if (module.settings().isEmpty()) return h + PADDING;
        h += 1f + module.settings().size() * settingHeight() + PADDING;
        return h + (colorSliders() * 10f);
    }

    private int colorSliders() {
        int extra = 0;
        for (int i = 0; i < module.settings().size(); i++) {
            if (module.settings().get(i) instanceof ColorSetting && colorExpanded[i]) extra += 3;
        }
        return extra;
    }

    private float settingHeight() { return 17f; }

    public void render(DrawContext context, Theme theme, float x, float y, float w, float tickDelta, float openness) {
        float row = rowHeight(theme);
        float mx = (float) InputUtil.mouseX();
        float my = (float) InputUtil.mouseY();
        boolean over = Render2D.inBox(mx, my, x, y, w, row);

        hover = Render2D.animate(hover, over ? 1f : 0f, theme.animationSpeed());
        fill = Render2D.animate(fill, module.isEnabled() ? 1f : 0f, theme.animationSpeed());
        expand = Render2D.animate(expand, module.isExtended() ? 1f : 0f, theme.animationSpeed());

        // Row plate + accent fill that wipes in from the left when toggled on.
        Render2D.rounded(context, x, y, w, row, theme.radius() - 1,
                ColorUtil.blend(ColorUtil.withAlpha(theme.slot(), (int) (230 * openness)),
                        ColorUtil.withAlpha(theme.accent(50), (int) (255 * openness)), hover));
        if (fill > 0.01f) {
            Render2D.rounded(context, x, y, w * fill, row, theme.radius() - 1, theme.accent((int) (60 * fill * openness)));
            Render2D.rect(context, x, y, 1.5f, row, theme.accent((int) (255 * openness)));
        }

        String name = module.displayName();
        int nameColor = module.isEnabled()
                ? ColorUtil.withAlpha(theme.accent(255), (int) (255 * openness))
                : ColorUtil.withAlpha(theme.textOff(), (int) (255 * openness));
        Render2D.text(context, name, x + 5, y + (row - Render2D.FONT_HEIGHT) / 2f + 1, nameColor, false);

        String suffix = module.suffix();
        if (!suffix.isEmpty()) {
            Render2D.text(context, suffix, x + w - 5 - Render2D.textWidth(suffix), y + 3.5f,
                    ColorUtil.withAlpha(theme.textDim(), (int) (255 * openness)), false);
        }
        if (module.bind() > 0) {
            String bind = "[" + InputUtil.keyName(module.bind()) + "]";
            int bw = Render2D.textWidth(bind);
            Render2D.text(context, bind, x + w - 5 - bw, y + (row - Render2D.FONT_HEIGHT) / 2f + 1,
                    ColorUtil.withAlpha(gui.binding() == module ? theme.accent(255) : theme.textDim(), (int) (255 * openness)), false);
        }

        if (over) {
            if (InputUtil.mouseClicked(0)) {
                module.toggle();
                gui.consumeClick();
            } else if (InputUtil.mouseClicked(1)) {
                module.setExtended(!module.isExtended());
                WraithClient.config().markDirty();
                gui.consumeClick();
            } else if (InputUtil.mouseClicked(2)) {
                gui.requestBind(module);
                gui.consumeClick();
            }
        }

        if (expand <= 0.02f) return;

        float cursor = y + row + 1f;
        for (int i = 0; i < module.settings().size(); i++) {
            Setting<?> setting = module.settings().get(i);
            cursor = settingWidget(context, theme, setting, x + PADDING, cursor, w - PADDING * 2, openness, i);
        }
    }

    /** Returns the y the next widget should start at. */
    private float settingWidget(DrawContext context, Theme theme, Setting<?> setting, float x, float y, float w,
                                float openness, int index) {
        if (setting instanceof BooleanSetting bool) return booleanWidget(context, theme, bool, x, y, w, openness);
        if (setting instanceof NumberSetting number) return numberWidget(context, theme, number, x, y, w, openness);
        if (setting instanceof ModeSetting mode) return modeWidget(context, theme, mode, x, y, w, openness);
        if (setting instanceof ColorSetting colorSetting) return colorWidget(context, theme, colorSetting, x, y, w, openness, index);
        return y;
    }

    private float booleanWidget(DrawContext context, Theme theme, BooleanSetting setting, float x, float y, float w,
                                float openness) {
        float row = rowHeight(theme) - 3;
        boolean over = Render2D.inBox((float) InputUtil.mouseX(), (float) InputUtil.mouseY(), x, y, w, row);
        if (over && gui.clickFree()) {
            setting.toggle();
            gui.consumeClick();
            WraithClient.config().markDirty();
        }
        Render2D.rounded(context, x, y, w, row, 2, ColorUtil.withAlpha(theme.slot(), (int) (255 * openness)));
        int valueColor = setting.on() ? theme.accent(255) : ColorUtil.argb(255, 70, 70, 86);
        Render2D.rounded(context, x + w - 16, y + (row - 6) / 2f, 12, 6, 3, valueColor);
        Render2D.rounded(context, x + w - 16 + (setting.on() ? 6 : 0), y + (row - 6) / 2f - 1, 6, 8, 3,
                ColorUtil.withAlpha(theme.text(), (int) (255 * openness)));
        Render2D.text(context, setting.name(), x + 4, y + (row - Render2D.FONT_HEIGHT) / 2f + 1,
                ColorUtil.withAlpha(setting.on() ? theme.text() : theme.textDim(), (int) (255 * openness)), false);
        return y + rowHeight(theme) - 3 + 1;
    }

    private float numberWidget(DrawContext context, Theme theme, NumberSetting setting, float x, float y, float w,
                               float openness) {
        float row = 17;
        float trackX = x + 2;
        float trackY = y + 11;
        float trackW = w - 4;
        boolean overTrack = Render2D.inBox((float) InputUtil.mouseX(), (float) InputUtil.mouseY(),
                trackX - 2, trackY - 4, trackW + 4, 9);
        boolean active = gui.draggingSetting == setting;

        if (overTrack && gui.clickFree()) {
            gui.startDrag(setting);
            active = true;
        }
        if (active) {
            setting.setProgress((float) ((InputUtil.mouseX() - trackX) / Math.max(1f, trackW)));
            WraithClient.config().markDirty();
        }

        Render2D.rounded(context, x, y, w, row - 4, 2, ColorUtil.withAlpha(theme.background(), (int) (215 * openness)));
        String label = setting.name();
        String value = format(setting) + setting.suffix();
        int valueW = Render2D.textWidth(value);
        Render2D.text(context, label, x + 3, y + 1, ColorUtil.withAlpha(theme.textDim(), (int) (255 * openness)), false);
        Render2D.text(context, value, x + w - 3 - valueW, y + 1,
                ColorUtil.withAlpha(active ? theme.accent(255) : theme.text(), (int) (255 * openness)), false);

        Render2D.rounded(context, trackX, trackY, trackW, 2, 1, ColorUtil.withAlpha(theme.border(), (int) (255 * openness)));
        float progress = setting.progress();
        Render2D.rounded(context, trackX, trackY, Math.max(1, trackW * progress), 2, 1,
                theme.accent((int) (255 * openness)));
        float knobX = trackX + trackW * progress - 1.5f;
        Render2D.rounded(context, knobX, trackY - 1.5f, 3, 5, 1.5f,
                ColorUtil.withAlpha(active || overTrack ? theme.text() : theme.textDim(), (int) (255 * openness)));
        return y + row + 1;
    }

    private float modeWidget(DrawContext context, Theme theme, ModeSetting setting, float x, float y, float w,
                             float openness) {
        float row = 17;
        float chipH = 9;
        Render2D.text(context, setting.name(), x + 3, y + 1, ColorUtil.withAlpha(theme.textDim(), (int) (255 * openness)), false);

        float cursorX = x + 2;
        float cursorY = y + 8;
        for (String mode : setting.modes()) {
            float chipW = Render2D.textWidth(mode) + 6;
            if (cursorX + chipW > x + w - 2 && cursorX > x + 2) {
                cursorX = x + 2;
                cursorY += chipH + 2;
            }
            boolean selected = setting.is(mode);
            boolean over = Render2D.inBox((float) InputUtil.mouseX(), (float) InputUtil.mouseY(), cursorX, cursorY, chipW, chipH);
            if (over && gui.clickFree()) {
                setting.set(mode);
                gui.consumeClick();
                WraithClient.config().markDirty();
            }
            int bg = selected ? theme.accent((int) (255 * openness))
                    : ColorUtil.withAlpha(theme.slot(), (int) (255 * openness));
            Render2D.rounded(context, cursorX, cursorY, chipW, chipH, 2, bg);
            Render2D.text(context, mode, cursorX + 3, cursorY + 1,
                    ColorUtil.withAlpha(selected ? 0xFFFFFFFF : theme.textDim(), (int) (255 * openness)), false);
            cursorX += chipW + 2;
        }
        return cursorY + chipH + 3;
    }

    private float colorWidget(DrawContext context, Theme theme, ColorSetting setting, float x, float y, float w,
                              float openness, int index) {
        float row = 13;
        boolean over = Render2D.inBox((float) InputUtil.mouseX(), (float) InputUtil.mouseY(), x, y, w, row);
        if (over && InputUtil.mouseClicked(0)) {
            colorExpanded[index] = !colorExpanded[index];
            gui.consumeClick();
        }
        if (over && InputUtil.mouseClicked(2)) {
            gui.requestBind(module);
            gui.consumeClick();
        }
        Render2D.rounded(context, x, y, w, row, 2, ColorUtil.withAlpha(theme.slot(), (int) (255 * openness)));
        Render2D.rounded(context, x + w - 14, y + 2, 10, 9, 2, setting.alpha(255));
        Render2D.text(context, setting.name(), x + 3, y + 2, ColorUtil.withAlpha(theme.textDim(), (int) (255 * openness)), false);
        if (!colorExpanded[index]) return y + row + 1;

        float cursor = y + row + 1;
        String[] channels = {"R", "G", "B"};
        for (int channel = 0; channel < 3; channel++) {
            int value = switch (channel) {
                case 0 -> setting.red();
                case 1 -> setting.green();
                default -> setting.blue();
            };
            float trackX = x + 12;
            float trackW = w - 34;
            boolean trackOver = Render2D.inBox((float) InputUtil.mouseX(), (float) InputUtil.mouseY(),
                    trackX - 2, cursor - 1, trackW + 4, 8);
            NumberSetting proxy = channelProxies[channel];
            proxy.set((double) value);
            if (trackOver && gui.clickFree()) gui.startDrag(proxy);
            if (gui.draggingSetting == proxy) {
                float progress = (float) ((InputUtil.mouseX() - trackX) / Math.max(1f, trackW));
                setting.setChannel(channel, Math.round(progress * 255f));
                WraithClient.config().markDirty();
            }
            Render2D.text(context, channels[channel], x, cursor, ColorUtil.withAlpha(theme.textDim(), 255), false);
            Render2D.rounded(context, trackX, cursor + 2, trackW, 2, 1, ColorUtil.withAlpha(theme.border(), 255));
            int channelColor = switch (channel) {
                case 0 -> ColorUtil.argb(255, 220, 60, 60);
                case 1 -> ColorUtil.argb(255, 90, 210, 110);
                default -> ColorUtil.argb(255, 70, 140, 240);
            };
            Render2D.rounded(context, trackX, cursor + 2, Math.max(1, trackW * (value / 255f)), 2, 1, channelColor);
            Render2D.text(context, String.valueOf(value), x + w - 18, cursor,
                    ColorUtil.withAlpha(theme.text(), 255), false);
            cursor += 10;
        }
        return cursor + 1;
    }

    /** Scratch sliders so drag state has something stable to point at. */
    private final NumberSetting[] channelProxies = {
            new NumberSetting("RProxy", 0, 0, 255, 1),
            new NumberSetting("GProxy", 0, 0, 255, 1),
            new NumberSetting("BProxy", 0, 0, 255, 1),
    };

    private static String format(NumberSetting setting) {
        double value = setting.get();
        if (Math.abs(value - Math.round(value)) < 0.0001) return String.valueOf((long) Math.round(value));
        return String.format(java.util.Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", ".0");
    }
}
