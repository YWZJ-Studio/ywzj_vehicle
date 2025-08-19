package org.ywzj.vehicle.mixin;

import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow
    protected abstract void setPosition(double pX, double pY, double pZ);

    @Shadow private Vec3 position;

    @Inject(method = "setup", at = @At("TAIL"))
    public void superbWarfare$setup(BlockGetter pLevel, Entity pEntity, boolean pDetached, boolean pThirdPersonReverse, float pPartialTick, CallbackInfo ci) {
        if (pEntity instanceof Player player && player.getVehicle() instanceof AbstractVehicle vehicle) {
            Vec3 finalPos = vehicle.relativeRotPos(this.position.add(vehicle.getCameraOffset()));
            this.setPosition(finalPos.x, finalPos.y, finalPos.z);
        }
    }

}
