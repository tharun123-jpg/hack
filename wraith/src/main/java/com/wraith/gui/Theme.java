package com.wraith.gui;

import com.wraith.module.settings.BooleanSetting;
import com.wraith.module.settings.ColorSetting;
import com.wraith.module.settings.NumberSetting;
import com.wraith.util.ColorUtil;

/**
 * Every colour and metric the GUI/HUD draws, resolved once per frame from the
 * ClickGui module's settings. Keeping this a value object (rather than reading
 * settings at draw sites) is what lets the theme change consistently mid-frame.
 */
public final class Theme {

    private int accentRgb = 0x9C6BFF;
    private int background = ColorUtil.argb(215, 16, 16, 22);
    private int panel = ColorUtil.argb(235, 22, 22, 30);
    private int slot = ColorUtil.argb(255, 30, 30, 40);
    private int border = ColorUtil.argb(255, 46, 46, 60);
    private int text = ColorUtil.argb(255, 232, 232, 238);
    private int textDim = ColorUtil.argb(255, 140, 140, 156);
    private int textOff = ColorUtil.argb(255, 104, 104, 120);

    private float radius = 4f;
    private float animationSpeed = 9f;
    private float panelWidth = 112f;
    private float rowHeight = 15f;
    private boolean rainbow;
    private boolean contrastText;
    private int hueOffset;

    public void bind(ColorSetting accent, BooleanSetting rainbowMode, NumberSetting alpha, NumberSetting rounding,
                     NumberSetting speed, NumberSetting width, NumberSetting rowHeight, BooleanSetting contrast) {
        this.accentRgb = accent.get();
        this.rainbow = rainbowMode.on();
        this.animationSpeed = (float) speed.get();
        this.panelWidth = (float) width.get();
        this.rowHeight = (float) rowHeight.get();
        this.contrastText = contrast.on();
        this.radius = (float) rounding.get();
        int a = Math.round((float) alpha.get() * 255f);
        this.background = ColorUtil.argb(a, 16, 16, 22);
        this.panel = ColorUtil.argb(Math.min(255, a + 20), 22, 22, 30);
        this.slot = ColorUtil.argb(Math.min(255, a + 30), 30, 30, 40);
        this.border = ColorUtil.argb(Math.min(255, a + 10), 46, 46, 60);
    }

    /** Accent at a given alpha; rainbow mode walks the hue ramp per draw call. */
    public int accent(int alpha) {
        if (rainbow) return ColorUtil.rainbow(0.35f, hueOffset++, alpha);
        return ColorUtil.rgba(accentRgb, alpha);
    }

    public int accentSolid() { return accent(255); }

    public int background() { return background; }
    public int panel() { return panel; }
    public int slot() { return slot; }
    public int border() { return border; }
    public int text() { return contrastText ? ColorUtil.argb(255, 255, 255, 255) : text; }
    public int textDim() { return textDim; }
    public int textOff() { return textOff; }
    public float radius() { return radius; }
    public float animationSpeed() { return animationSpeed; }
    public float panelWidth() { return panelWidth; }
    public float rowHeight() { return rowHeight; }
    public boolean rainbow() { return rainbow; }

    /** Accent blended toward black - used for slider tracks and pressed rows. */
    public int accentDeep(int alpha) {
        return ColorUtil.blend(accent(alpha), ColorUtil.argb(alpha, 20, 20, 28), 0.45f);
    }
}
