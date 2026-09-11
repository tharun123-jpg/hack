package com.wraith.modules.qol;

import com.wraith.module.Category;
import com.wraith.module.Module;
import com.wraith.module.settings.BooleanSetting;
import com.wraith.module.settings.ModeSetting;
import com.wraith.util.MCUtil;

/**
 * Keeps sprint held so forward movement stops being a finger workout. This is the
 * same thing vanilla's "Sprint: Toggle" option does; it exists here because that
 * option also stops sprinting when you strafe, and it eats hunger while airborne.
 */
public class ToggleSprint extends Module {

    private final ModeSetting mode = mode("Mode", "Forward", "Forward", "Omni");
    private final BooleanSetting whileAirborne = bool("While Airborne", false);
    private final BooleanSetting keepOffGround = bool("Keep Through Jump", true);

    public ToggleSprint() {
        super("ToggleSprint", "Hold sprint for you, like the vanilla toggle option", Category.QOL);
    }

    @Override
    protected void onTick() {
        var player = MCUtil.mc().player;
        if (player == null || MCUtil.mc().currentScreen != null) return;
        boolean wantSprint = player.getHealth() > 6f
                && !player.isSneaking()
                && player.getHungerManager().getFoodLevel() > 6
                && (keepOffGround.on() || player.isOnGround())
                && (whileAirborne.on() || player.isOnGround());
        if (!wantSprint) return;
        if (mode.is("Forward") && !movingForward()) return;
        if (!player.isSprinting()) player.setSprinting(true);
    }

    private boolean movingForward() {
        var player = MCUtil.mc().player;
        return player != null && (player.input.movementForward > 0.1f
                || (mode.is("Omni") && (Math.abs(player.input.movementSideways) > 0.1f)));
    }

    @Override
    public String suffix() {
        return mode.get();
    }
}
