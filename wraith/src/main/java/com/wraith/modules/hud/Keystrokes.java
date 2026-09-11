package com.wraith.modules.hud;

import com.wraith.gui.Render2D;
import com.wraith.gui.hud.Anchor;
import com.wraith.gui.hud.HudModule;
import com.wraith.module.settings.BooleanSetting;
import com.wraith.module.settings.ModeSetting;
import com.wraith.util.ColorUtil;
import com.wraith.util.InputUtil;
import com.wraith.util.MCUtil;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

/**
 * Read-only view of the movement keys, driven by the same GLFW poll the GUI uses.
 * A snapshot of your own input - no gameplay effect.
 */
public class Keystrokes extends HudModule {

    private final ModeSetting layout = mode("Layout", "WASD", "WASD", "Compact");
    private final BooleanSetting mouse = bool("Mouse Buttons", true);
    private final BooleanSetting spaceRow = bool("Sneak / Jump", true);
    private final NumberSetting keySize = num("Key Size", 18.0, 12.0, 28.0, 1, "px");

    public Keystrokes() {
        super("Keystrokes", "Shows which movement keys are held", Anchor.BOTTOM_LEFT);
    }

    @Override
    protected float draw(DrawContext context, float x, float y, float tickDelta) {
        float size = keySize.floatGet();
        float gap = 2;
        int pressedRows = 0;

        if (layout.is("Compact")) {
            String[] labels = {"W", "A", "S", "D"};
            int[] keys = {GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_D};
            float width = 4 * (size * 0.8f) + 3 * gap;
            for (int i = 0; i < 4; i++) {
                key(context, x + i * (size * 0.8f + gap), y, size * 0.8f, size, labels[i], keys[i]);
            }
            pressedRows = 1;
            size(size, size);
            return size;
        }

        key(context, x + size + gap, y, size, size, "W", GLFW.GLFW_KEY_W);
        key(context, x, y + size + gap, size, size, "A", GLFW.GLFW_KEY_A);
        key(context, x + size + gap, y + size + gap, size, size, "S", GLFW.GLFW_KEY_S);
        key(context, x + (size + gap) * 2, y + size + gap, size, size, "D", GLFW.GLFW_KEY_D);
        float width = size * 3 + gap * 2;
        float height = size * 2 + gap;

        if (spaceRow.on()) {
            float jumpY = y + height + gap;
            key(context, x, jumpY, size * 0.6f, size * 0.7f, "Shift", GLFW.GLFW_KEY_LEFT_SHIFT);
            key(context, x + size * 0.6f + gap, jumpY, width - size * 0.6f - gap, size * 0.7f, "Space", GLFW.GLFW_KEY_SPACE);
            height += size * 0.7f + gap;
            pressedRows = 2;
        }
        if (mouse.on()) {
            float mouseY = y + height + gap;
            float half = (width - gap) / 2f;
            key(context, x, mouseY, half, size * 0.7f, "LMB", -1);
            key(context, x + half + gap, mouseY, half, size * 0.7f, "RMB", -2);
            height += size * 0.7f + gap;
        }
        size(width, height);
        return width;
    }

    /** Negative "key" values mean mouse buttons, read from the same poll. */
    private void key(DrawContext context, float x, float y, float w, float h, String label, int key) {
        boolean down = key >= 0 ? InputUtil.isDown(key)
                : key == -1 ? InputUtil.mouseDown(0)
                : InputUtil.mouseDown(1);
        // Nudge up while held, the physical-keyboard tell players recognise.
        float offset = down ? 1 : 0;
        Render2D.rounded(context, x, y + offset, w, h, 3, theme().background());
        Render2D.rounded(context, x, y + offset, w, h - (down ? 0 : 2), 3,
                down ? theme().accent(150) : ColorUtil.withAlpha(theme().slot(), 255));
        Render2D.rounded(context, x + 1, y + offset + 1, w - 2, h - 2 - offset, 2,
                down ? theme().accentDeep(220) : ColorUtil.withAlpha(theme().background(), 230));
        int textW = Render2D.textWidth(label);
        Render2D.text(context, label, x + w / 2f - textW / 2f, y + offset + h / 2f - 4f,
                down ? 0xFFFFFFFF : ColorUtil.withAlpha(theme().textDim(), 220), false);
    }
}
