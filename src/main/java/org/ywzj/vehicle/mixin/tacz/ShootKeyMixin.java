//package org.ywzj.vehicle.mixin.tacz;
//
//import com.tacz.guns.client.input.ShootKey;
//import net.neoforged.neoforge.client.event.ClientTickEvent;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
//
//@Mixin(value = ShootKey.class, remap = false)
//public class ShootKeyMixin {
//
//    @Inject(
//            method = "autoShoot",
//            at = @At(value = "HEAD"),
//            cancellable = true)
//    private static void autoShoot(ClientTickEvent.Post event, CallbackInfo ci) {
//        if (LocalVehiclePlayer.instance.onVehicle()) {
//            ci.cancel();
//        }
//    }
//
//}
