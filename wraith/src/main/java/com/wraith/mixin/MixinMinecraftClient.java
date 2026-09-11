package com.wraith.mixin;

import com.wraith.WraithClient;
import com.wraith.event.events.TickEvent;
import com.wraith.event.events.WorldEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Client tick + world transitions. Two injections, both TAIL-safe and idempotent. */
@Mixin(MinecraftClient.class)
public abstract class MixinMinecraftClient {

    @Inject(method = "tick", at = @At("HEAD"))
    private void wraith$tick(CallbackInfo ci) {
        if (WraithClient.bus() == null) return;
        WraithClient.bus().post(TickEvent.INSTANCE);
    }

    @Inject(method = "setWorld", at = @At("TAIL"))
    private void wraith$setWorld(ClientWorld world, CallbackInfo ci) {
        if (WraithClient.bus() == null) return;
        WraithClient.bus().post(world == null ? WorldEvent.LEAVE : WorldEvent.JOIN);
    }
}
