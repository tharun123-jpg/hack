package com.wraith.util;

import net.minecraft.client.option.Perspective;

/**
 * Thin wrapper over GameOptions so the modules never poke at SimpleOption
 * directly. setValue/getValue are the Yarn names in 1.21.11; a couple of older
 * mappings call these get/set, which is why every call site is here.
 */
public final class VanillaOptions {

    public static final double MIN_GAMMA = 0.0;
    public static final double MAX_GAMMA = 100.0;

    private VanillaOptions() {}

    public static double gamma() {
        return MCUtil.mc().options.getGamma().getValue();
    }

    public static void gamma(double value) {
        MCUtil.mc().options.getGamma().setValue(Math.max(MIN_GAMMA, Math.min(MAX_GAMMA, value)));
    }

    public static Perspective perspective() {
        return MCUtil.mc().options.getPerspective().getValue();
    }

    public static void perspective(Perspective perspective) {
        MCUtil.mc().options.getPerspective().setValue(perspective);
    }

    public static double fov() {
        return MCUtil.mc().options.getFov().getValue();
    }
}
