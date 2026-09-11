package com.wraith.modules.render;

import com.wraith.module.Category;
import com.wraith.module.Module;
import com.wraith.module.settings.BooleanSetting;
import com.wraith.module.settings.ModeSetting;
import com.wraith.module.settings.NumberSetting;
import com.wraith.util.InputUtil;
import com.wraith.util.MCUtil;
import com.wraith.util.VanillaOptions;
import net.minecraft.client.option.Perspective;
import org.lwjgl.glfw.GLFW;

/**
 * Detaches the camera from the player's facing so you can look around while the
 * player stays put - the camera-only half of Vape's freelook.
 *
 * Deliberately narrow: it never writes player rotation, never freezes movement and
 * never moves the eye position through geometry, so nothing here is sent to a
 * server or gives information the player does not already have.
 */
public class Freelook extends Module {

    private final ModeSetting activation = mode("Activation", "Hold", "Hold", "Toggle");
    private final ModeSetting perspective = mode("Perspective", "First", "First", "Back", "Front");
    private final NumberSetting sensitivity = num("Sensitivity", 1.0, 0.2, 4.0, 0.1, "x");
    private final BooleanSetting invertY = bool("Invert Y", false);
    private final BooleanSetting resetOnRelease = bool("Snap Back", true);

    private float yaw;
    private float pitch;
    private boolean active;
    private Perspective saved = Perspective.FIRST_PERSON;

    public Freelook() {
        super("Freelook", "Look around independently of your body", Category.RENDER);
        setBind(GLFW.GLFW_KEY_G);
    }

    public boolean ownsCamera() { return active; }
    public float yaw() { return yaw; }
    public float pitch() { return pitch; }

    /** Fed by MixinMouse in place of vanilla's rotation handling. */
    public void applyLook(double deltaX, double deltaY) {
        float sens = 0.1f * sensitivity.floatGet();
        yaw += (float) (deltaX * sens);
        pitch -= (float) (deltaY * sens * (invertY.on() ? -1 : 1));
        pitch = Math.max(-89.9f, Math.min(89.9f, pitch));
    }

    @Override
    protected void onEnable() {
        saved = VanillaOptions.perspective();
        applyPerspective(true);
        if (MCUtil.mc().player != null) {
            yaw = MCUtil.mc().player.getYaw();
            pitch = MCUtil.mc().player.getPitch();
        }
        active = !activation.is("Hold") || holding();
    }

    @Override
    protected void onDisable() {
        if (resetOnRelease.on() && MCUtil.mc().player != null) {
            yaw = MCUtil.mc().player.getYaw();
            pitch = MCUtil.mc().player.getPitch();
        }
        active = false;
        applyPerspective(false);
        VanillaOptions.perspective(saved);
    }

    @Override
    protected void onTick() {
        boolean wantActive = activation.is("Hold") ? holding() : true;
        if (wantActive != active) {
            active = wantActive;
            applyPerspective(wantActive);
        }
    }

    private boolean holding() { return InputUtil.isDown(bind()); }

    private void applyPerspective(boolean on) {
        if (!on) return;
        switch (perspective.get()) {
            case "Back" -> VanillaOptions.perspective(Perspective.THIRD_PERSON_BACK);
            case "Front" -> VanillaOptions.perspective(Perspective.THIRD_PERSON_FRONT);
            default -> VanillaOptions.perspective(Perspective.FIRST_PERSON);
        }
    }

    @Override
    public String suffix() {
        return active ? "active" : activation.get();
    }
}
