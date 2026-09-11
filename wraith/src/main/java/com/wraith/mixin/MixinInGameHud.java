package com.wraith.mixin;

import com.wraith.WraithClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The single per-frame render hook for the whole client, chosen because
 * InGameHud#render has exactly one overload in 1.21.11 Yarn - so no descriptor is
 * needed and the selector cannot multi-match.
 *
 * On 1.21.1 and older the second parameter is a float tickDelta instead of a
 * RenderTickCounter; if you downgrade, the handler below is the only change:
 *   private void wraith$render(DrawContext context, float tickDelta, CallbackInfo ci)
 */
@Mixin(InGameHud.class)
public abstract class MixinInGameHud {

    @Inject(method = "render", at = @At("TAIL"))
    private void wraith$render(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        WraithClient.onHudFrame(context, tickCounter.getTickDelta(false));
    }

    /**
     * renderCrosshair is private, which Mixin targets fine; suppressing it is the
     * whole cost of CustomCrosshair, since the drawn one comes through the HUD event.
     */
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void wraith$crosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (WraithClient.modules() == null) return;
        com.wraith.modules.render.CustomCrosshair crosshair =
                WraithClient.modules().get(com.wraith.modules.render.CustomCrosshair.class);
        if (crosshair != null && crosshair.hidesVanilla()) ci.cancel();
    }
}
