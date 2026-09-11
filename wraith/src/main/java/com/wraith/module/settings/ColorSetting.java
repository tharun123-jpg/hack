package com.wraith.module.settings;

import com.wraith.module.Setting;
import com.wraith.util.ColorUtil;

import java.util.function.Consumer;

/** RGB only - alpha belongs to the theme so fades stay consistent everywhere. */
public class ColorSetting extends Setting<Integer> {

    public ColorSetting(String name, int rgb) { this(name, rgb, null); }

    public ColorSetting(String name, int rgb, Consumer<Integer> onChange) {
        super(name, rgb & 0xFFFFFF, onChange);
    }

    public int red() { return ColorUtil.red(get()); }
    public int green() { return ColorUtil.green(get()); }
    public int blue() { return ColorUtil.blue(get()); }

    public int alpha(int a) { return ColorUtil.rgba(get(), a); }

    public void setChannel(int channel, int value) {
        value = Math.min(255, Math.max(0, value));
        int current = get();
        int next = switch (channel) {
            case 0 -> ColorUtil.argb(255, value, ColorUtil.green(current), ColorUtil.blue(current));
            case 1 -> ColorUtil.argb(255, ColorUtil.red(current), value, ColorUtil.blue(current));
            default -> ColorUtil.argb(255, ColorUtil.red(current), ColorUtil.green(current), value);
        };
        set(next & 0xFFFFFF);
    }

    @Override public String encode() { return Integer.toHexString(get()); }

    @Override public void decode(String raw) {
        try {
            set(Integer.parseInt(raw, 16));
        } catch (NumberFormatException ignored) {
        }
    }
}
