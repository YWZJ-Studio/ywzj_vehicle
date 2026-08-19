package org.ywzj.vehicle.mixin.tacz;

import com.tacz.guns.client.input.ShootKey;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;

@Mixin(value = ShootKey.class, remap = false)
public class ShootKeyMixin {

    @Inject(
            method = "autoShoot",
            at = @At(value = "HEAD"),
            cancellable = true)
    private static void autoShoot(TickEvent.ClientTickEvent event, CallbackInfo ci) {
        AbstractVehicle.Seat seat = LocalVehiclePlayer.instance.seat;
        if (seat != null && !seat.partUnit.getData().passengerCanUseItem()) {
            ci.cancel();
        }
    }

    @Inject(
            method = "semiShoot",
            at = @At(value = "HEAD"),
            cancellable = true)
    private static void semiShoot(InputEvent.MouseButton.Post event, CallbackInfo ci) {
        AbstractVehicle.Seat seat = LocalVehiclePlayer.instance.seat;
        if (seat != null && !seat.partUnit.getData().passengerCanUseItem()) {
            ci.cancel();
        }
    }

}
