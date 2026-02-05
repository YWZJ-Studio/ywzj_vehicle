package org.ywzj.vehicle.mixin.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Final
    @Shadow
    public Options options;

    /**
     * Prevents F5 perspective switching when player is in vehicle camera modes.
     * Only allows perspective change in THIRD_PERSON mode (external view).
     */
    @Inject(
            method = "handleKeybinds",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Options;getCameraType()Lnet/minecraft/client/CameraType;",
                    ordinal = 0
            ),
            cancellable = true
    )
    private void blockPerspectiveInVehicleCamera(CallbackInfo ci) {
        Minecraft mc = (Minecraft) (Object) this;
        
        if (mc.player != null && mc.player.getVehicle() instanceof AbstractVehicle) {
            LocalVehiclePlayer.ViewType viewType = LocalVehiclePlayer.instance.viewType;
            if (viewType == LocalVehiclePlayer.ViewType.OPERATOR || viewType == LocalVehiclePlayer.ViewType.SCOPE) {
                ci.cancel();
            }
        }
    }

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
