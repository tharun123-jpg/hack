package com.wraith.modules.render;

import com.wraith.event.events.WorldEvent;
import com.wraith.gui.Render2D;
import com.wraith.module.Category;
import com.wraith.module.Module;
import com.wraith.module.settings.BooleanSetting;
import com.wraith.module.settings.ModeSetting;
import com.wraith.module.settings.NumberSetting;
import com.wraith.util.VanillaOptions;

/**
 * Raises the gamma option instead of granting night vision or writing light levels
 * into the world, so it is purely a display setting and reverts on toggle.
 */
public class FullBright extends Module {

    private final ModeSetting mode = mode("Mode", "Gamma", "Gamma", "Smooth");
    private final NumberSetting level = num("Level", 15.0, 1.0, 100.0, 0.5);
    private final BooleanSetting restoreOriginal = bool("Restore On Off", true);

    private double savedGamma = -1;
    private double current;

    public FullBright() {
        super("FullBright", "Brighten the world by driving the gamma option", Category.RENDER);
    }

    @Override
    protected void onEnable() {
        savedGamma = VanillaOptions.gamma();
        current = savedGamma;
    }

    @Override
    protected void onDisable() {
        if (restoreOriginal.on() && savedGamma >= 0) VanillaOptions.gamma(savedGamma);
        savedGamma = -1;
        current = 0;
    }

    @Override
    protected void onTick() {
        double target = level.get();
        // Smooth lerps the option so fading between dark and bright is not a flash.
        if (mode.is("Smooth")) {
            current = Render2D.animate((float) current, (float) target, 12f);
            VanillaOptions.gamma(current);
        } else {
            current = target;
            VanillaOptions.gamma(target);
        }
    }

    @Override
    protected void onWorld(WorldEvent event) {
        if (!event.isJoining() && restoreOriginal.on() && savedGamma >= 0) {
            VanillaOptions.gamma(savedGamma);
        }
    }

    @Override
    public String suffix() {
        return mode.get();
    }

}
