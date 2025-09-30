package org.ywzj.vehicle.mixin.client;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.ywzj.vehicle.client.event.InputHandler;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

@Mixin(Player.class)
public class PlayerMixin {

    @Inject(
            method = "wantsToStopRiding",
            at = @At("HEAD"),
            cancellable = true)
    public void wantsToStopRiding(CallbackInfoReturnable<Boolean> cir) {
        if (((Player) (Object) this).getVehicle() instanceof AbstractVehicle) {
            cir.setReturnValue(InputHandler.leaveVehicle);
        }
    }

}
