package org.ywzj.vehicle.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {

    @Inject(
            method = "turnPlayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"
            )
    )
    private void beforePlayerTurn(CallbackInfo ci, @Local(name = "d2") double d2, @Local(name = "d3") double d3) {
        LocalVehiclePlayer.instance.mouseTurn(d2, d3);
    }

}
