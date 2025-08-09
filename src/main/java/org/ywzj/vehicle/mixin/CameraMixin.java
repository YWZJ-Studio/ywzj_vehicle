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
import org.ywzj.vehicle.entity.AbstractVehicle;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    protected abstract void move(double x, double y, double z);

    @Inject(method = "setup", at = @At("TAIL"))
    public void superbWarfare$setup(BlockGetter area, Entity entity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        if (entity instanceof Player player && player.getVehicle() instanceof AbstractVehicle vehicle) {
            Vec3 offset = vehicle.getCameraOffset();
            this.move(offset.x, offset.y, offset.z);
        }
    }

}