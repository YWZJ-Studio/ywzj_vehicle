package org.ywzj.vehicle.mixin.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Final
    @Shadow
    public Options options;

    @Redirect(
            method = "handleKeybinds",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/KeyMapping;consumeClick()Z",
                    ordinal = 4
            )
    )
    private boolean redirectInventory(KeyMapping key) {
        Minecraft mc = (Minecraft) (Object) this;
        boolean consumeResult = key.consumeClick();
        if (key == this.options.keyInventory && mc.player != null && mc.player.getVehicle() instanceof AbstractVehicle) {
            return false;
        }
        return consumeResult;
    }

    @Redirect(
            method = "handleKeybinds",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/KeyMapping;consumeClick()Z",
                    ordinal = 7
            )
    )
    private boolean redirectDrop(KeyMapping key) {
        Minecraft mc = (Minecraft) (Object) this;
        boolean consumeResult = key.consumeClick();
        if (key == this.options.keyDrop && mc.player != null && mc.player.getVehicle() instanceof AbstractVehicle) {
            return false;
        }
        return consumeResult;
    }

}
