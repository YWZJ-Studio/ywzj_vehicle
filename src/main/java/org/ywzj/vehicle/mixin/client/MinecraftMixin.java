package org.ywzj.vehicle.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Final
    @Shadow
    public Options options;

    @Inject(
            method = "handleKeybinds",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/KeyMapping;consumeClick()Z",
                    ordinal = 0
            ),
            cancellable = true
    )
    private void redirectInventory(CallbackInfo ci) {
        Minecraft mc = (Minecraft)(Object)this;
        if (this.options.keyInventory.consumeClick() || this.options.keyDrop.consumeClick()) {
            if (mc.player.getVehicle() instanceof AbstractVehicle) {
                ci.cancel();
            }
        }
    }

}
