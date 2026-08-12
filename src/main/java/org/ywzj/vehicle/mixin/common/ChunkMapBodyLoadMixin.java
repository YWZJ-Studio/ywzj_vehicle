package org.ywzj.vehicle.mixin.common;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.ywzj.vehicle.stream.DetachedBodyStreaming;


@Mixin(ChunkMap.class)
public abstract class ChunkMapBodyLoadMixin {

    @Shadow
    private int serverViewDistance;

    @Inject(method = "getPlayerViewDistance(Lnet/minecraft/server/level/ServerPlayer;)I",
            at = @At("HEAD"), cancellable = true)
    private void ywzj$bodyViewDistance(ServerPlayer player, CallbackInfoReturnable<Integer> cir) {
        int radius = DetachedBodyStreaming.bodyViewDistance(player.getUUID(), this.serverViewDistance);
        if (radius > 0) {
            cir.setReturnValue(radius);
        }
    }


    @Inject(method = "skipPlayer(Lnet/minecraft/server/level/ServerPlayer;)Z",
            at = @At("HEAD"), cancellable = true)
    private void ywzj$parkBodyTickets(ServerPlayer player, CallbackInfoReturnable<Boolean> cir) {
        if (DetachedBodyStreaming.parksBodyTickets(player.getUUID())) {
            cir.setReturnValue(true);
        }
    }

}
