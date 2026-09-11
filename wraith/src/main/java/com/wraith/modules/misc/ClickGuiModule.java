package com.wraith.modules.misc;

import com.wraith.gui.Theme;
import com.wraith.module.Category;
import com.wraith.module.Module;
import com.wraith.module.settings.BooleanSetting;
import com.wraith.module.settings.ColorSetting;
import com.wraith.module.settings.ModeSetting;
import com.wraith.module.settings.NumberSetting;
import org.lwjgl.glfw.GLFW;

/**
 * The client chrome itself. Vape keeps its menu as a HUD overlay rather than a
 * Screen, which is why it never pauses the game or loses mouse capture; Wraith
 * copies that, so the GUI is a module with theme settings, not a Screen subclass.
 */
public class ClickGuiModule extends Module {

    private final ColorSetting accent = color("Accent", 0x9C6BFF);
    private final BooleanSetting rainbow = bool("Rainbow", false);
    private final NumberSetting alpha = num("Opacity", 0.85, 0.2, 1.0, 0.01);
    private final NumberSetting rounding = num("Roundness", 4.0, 0.0, 10.0, 0.5, "px");
    private final NumberSetting speed = num("Anim Speed", 9.0, 1.0, 25.0, 0.5);
    private final NumberSetting width = num("Panel Width", 112.0, 88.0, 180.0, 1, "px");
    private final NumberSetting rowHeight = num("Row Height", 15.0, 11.0, 22.0, 0.5, "px");
    private final BooleanSetting contrast = bool("Max Contrast Text", false);
    private final BooleanSetting blurBackdrop = bool("Dim Backdrop", true);
    private final NumberSetting openKey = new NumberSetting("Open Key", GLFW.GLFW_KEY_RIGHT_SHIFT,
            GLFW.GLFW_KEY_UNKNOWN, GLFW.GLFW_KEY_LAST, 1);
    private final ModeSetting closeBehaviour = new ModeSetting("Close On", "Escape", "Escape", "Toggle", "Never");

    /** Profiles are switched from the GUI but applied on the next tick, so a value
     * write never runs in the middle of the frame that is drawing it. */
    private final NumberSetting profileSlot = new NumberSetting("Profile", 1, 1, 8, 1, "",
            value -> pendingProfile = "profile" + Math.round(value));

    private final Theme theme = new Theme();
    private String flash;
    private long flashUntil;
    private String pendingProfile;

    public ClickGuiModule() {
        super("ClickGui", "The Wraith menu, theme and HUD editor chrome", Category.MISC);
        // Client chrome is always live; hide it from the ArrayList and the toggles.
        setHidden(true);
    }

    public Theme theme() {
        theme.bind(accent, rainbow, alpha, rounding, speed, width, rowHeight, contrast);
        return theme;
    }

    public boolean dimBackdrop() { return blurBackdrop.on(); }
    public NumberSetting openKey() { return openKey; }
    public ModeSetting closeBehaviour() { return closeBehaviour; }

    public String takePendingProfile() {
        String value = pendingProfile;
        pendingProfile = null;
        return value;
    }

    public void flash(String message) {
        flash = message;
        flashUntil = System.currentTimeMillis() + 1600;
    }

    public String flash() {
        if (flash == null || System.currentTimeMillis() > flashUntil) return null;
        return flash;
    }

}
