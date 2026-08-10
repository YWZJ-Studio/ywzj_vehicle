package org.ywzj.vehicle.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.EquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.ywzj.vehicle.all.AllItems;

@Mixin(PlayerRenderer.class)
public class PlayerRendererMixin {

    @WrapOperation(
            method = "setupRotations",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;isFallFlying()Z")
    )
    private boolean ywzjVehicle$keepParachutingPlayerUpright(AbstractClientPlayer player, Operation<Boolean> original) {
        boolean fallFlying = original.call(player);
        return fallFlying && !player.getItemBySlot(EquipmentSlot.CHEST).is(AllItems.PARACHUTE_PACK.get());
    }

}
