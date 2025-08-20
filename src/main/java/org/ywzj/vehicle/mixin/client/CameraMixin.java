package org.ywzj.vehicle.mixin.client;

import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow
    protected abstract void setPosition(double pX, double pY, double pZ);

    @Inject(method = "setup", at = @At("TAIL"))
    public void superbWarfare$setup(BlockGetter pLevel, Entity pEntity, boolean pDetached, boolean pThirdPersonReverse, float pPartialTick, CallbackInfo ci) {
        if (pEntity instanceof Player player && player.getVehicle() instanceof AbstractVehicle) {
            LocalVehiclePlayer localVehiclePlayer = LocalVehiclePlayer.instance;
            this.setPosition(Mth.lerp(pPartialTick, localVehiclePlayer.cameraXO, localVehiclePlayer.cameraX),
                    Mth.lerp(pPartialTick, localVehiclePlayer.cameraYO, localVehiclePlayer.cameraY),
                    Mth.lerp(pPartialTick, localVehiclePlayer.cameraZO, localVehiclePlayer.cameraZ));
        }
    }

}
