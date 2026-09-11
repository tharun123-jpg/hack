package com.wraith.mixin;

import com.wraith.WraithClient;
import com.wraith.modules.render.Freelook;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Freelook needs to steer the camera without touching the player's rotation (that
 * would be sent to the server), and Camera#setRotation is protected - so instead of
 * an @Invoker the hook writes the two private fields after vanilla has updated them.
 *
 * yaw/pitch have been the Yarn names for these fields for many versions; this is
 * the only place they are touched.
 */
@Mixin(Camera.class)
public abstract class MixinCamera {

    @Shadow private float yaw;
    @Shadow private float pitch;

    @Inject(method = "update", at = @At("TAIL"))
    private void wraith$freelook(CallbackInfo ci) {
        if (WraithClient.modules() == null) return;
        Freelook freelook = WraithClient.modules().get(Freelook.class);
        if (freelook == null || !freelook.ownsCamera()) return;
        this.yaw = freelook.yaw();
        this.pitch = freelook.pitch();
    }
}
