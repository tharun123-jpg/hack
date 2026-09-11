package com.wraith.mixin;

import com.wraith.WraithClient;
import com.wraith.modules.render.NoFovShift;
import com.wraith.modules.render.Zoom;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {

    /**
     * One hook serves two modules. NoFovShift first replaces vanilla's
     * speed/flight-modified value with the raw option, then Zoom scales whatever
     * survived - so both features compose instead of fighting over the return.
     */
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void wraith$getFov(Camera camera, float tickDelta, boolean changingFov,
                              CallbackInfoReturnable<Float> cir) {
        if (WraithClient.modules() == null) return;
        float fov = cir.getReturnValueF();

        NoFovShift flatten = WraithClient.modules().get(NoFovShift.class);
        if (flatten != null && flatten.shouldFlatten()) fov = flatten.baseFov();

        Zoom zoom = WraithClient.modules().get(Zoom.class);
        if (zoom != null) fov = zoom.modifyFov(fov);
        if (fov > 0.0001f) cir.setReturnValue(fov);
    }
}
