package org.ywzj.vehicle.mixin.common;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.ywzj.vehicle.stream.DetachedBodyStreaming;
import org.ywzj.vehicle.stream.DetachedTracked;

@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public abstract class TrackedEntityMixin implements DetachedTracked {

    @Shadow
    @Final
    Entity entity;

    @Shadow
    public abstract void updatePlayer(ServerPlayer player);

    @ModifyVariable(method = "updatePlayer(Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At("STORE"), ordinal = 0)
    private boolean ywzj$detachedVisibility(boolean visible, ServerPlayer player) {
        if (DetachedBodyStreaming.shouldReveal(this.entity, player)) {
            return true;
        }
        if (DetachedBodyStreaming.isPiloting(player.getUUID())) {
            return DetachedBodyStreaming.mustStayPaired(this.entity, player) && visible;
        }
        return visible;
    }

    @Override
    public void ywzj$updatePlayer(ServerPlayer player) {
        updatePlayer(player);
    }

}
