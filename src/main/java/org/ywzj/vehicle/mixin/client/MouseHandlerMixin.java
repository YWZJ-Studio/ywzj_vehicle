package org.ywzj.vehicle.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {

    @WrapOperation(
            method = "turnPlayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"
            )
    )
    private void wrapPlayerTurn(LocalPlayer instance, double d2, double d3, Operation<Void> original) {
        if (!LocalVehiclePlayer.instance.handlePlayerTurn(d2, d3)) {
            original.call(instance, d2, d3);
        }
    }

}
