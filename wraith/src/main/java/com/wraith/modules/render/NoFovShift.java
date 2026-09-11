package com.wraith.modules.render;

import com.wraith.module.Category;
import com.wraith.module.Module;
import com.wraith.module.settings.BooleanSetting;
import com.wraith.util.MCUtil;
import com.wraith.util.VanillaOptions;
import net.minecraft.item.SpyglassItem;

/**
 * Cancels vanilla's automatic FOV kicks when sprinting, flying or falling. Purely
 * visual comfort - a fixed FOV is how many players avoid motion sickness.
 */
public class NoFovShift extends Module {

    private final BooleanSetting keepSpyglass = bool("Keep Spyglass", true);

    public NoFovShift() {
        super("NoFovShift", "Keep the field of view constant while moving", Category.RENDER);
    }

    /** Consumed by MixinGameRenderer; true when the modifier stack should be dropped. */
    public boolean shouldFlatten() {
        if (!isEnabled()) return false;
        if (!keepSpyglass.on() || MCUtil.mc().player == null) return true;
        boolean usingSpyglass = MCUtil.mc().player.isUsingItem()
                && MCUtil.mc().player.getActiveItem().getItem() instanceof SpyglassItem;
        return !usingSpyglass;
    }

    public float baseFov() {
        return (float) VanillaOptions.fov();
    }
}
