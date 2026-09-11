package com.wraith.mixin;

import com.wraith.WraithClient;
import com.wraith.modules.render.Freelook;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Suppresses camera rotation while the menu/HUD editor owns the cursor - that is
 * all this hook does. Freelook reuses the same cancellation for its own look lock.
 */
@Mixin(Mouse.class)
public abstract class MixinMouse {

    @Inject(method = "updateMouse", at = @At("HEAD"), cancellable = true)
    private void wraith$updateMouse(double deltaX, double deltaY, CallbackInfo ci) {
        if (WraithClient.modules() == null || WraithClient.gui() == null) return;

        // The menu and the HUD editor own the cursor, so the camera must not move.
        if (WraithClient.gui().capturesMouse()) {
            ci.cancel();
            return;
        }
        // Freelook needs the same deltas vanilla would have used, just not on the player.
        Freelook freelook = WraithClient.modules().get(Freelook.class);
        if (freelook != null && freelook.ownsCamera()) {
            freelook.applyLook(deltaX, deltaY);
            ci.cancel();
        }
    }
}
