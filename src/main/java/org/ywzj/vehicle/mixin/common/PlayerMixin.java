package org.ywzj.vehicle.mixin.common;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.parenting.DeckAttachment;
import org.ywzj.vehicle.vehicle.parenting.DeckCollision;
import org.ywzj.vehicle.vehicle.parenting.VehicleParenting;

@Mixin(Player.class)
public class PlayerMixin {

    @Inject(method = "wantsToStopRiding",
            at = @At("HEAD"),
            cancellable = true)
    public void wantsToStopRiding(CallbackInfoReturnable<Boolean> cir) {
        if (((Player) (Object) this).getVehicle() instanceof AbstractVehicle) {
            cir.setReturnValue(false);
        }
    }

    /**
     * Counts a vehicle deck as ground for sneak edge protection.
     */
    @Inject(method = "canFallAtLeast", at = @At("RETURN"), cancellable = true)
    private void ywzj_vehicle$deckIsGround(double dx, double dz, float depth,
                                           CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) {
            return;
        }
        Player self = (Player) (Object) this;
        DeckAttachment attachment = VehicleParenting.attachmentOf(self);
        if (!VehicleParenting.isUsable(attachment, self)) {
            return;
        }
        if (DeckCollision.supportsOffset(self, attachment.vehicle().deckSnapshot(), attachment,
                dx, dz, depth)) {
            cir.setReturnValue(false);
        }
    }

}
