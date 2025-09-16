package org.ywzj.vehicle.mixin.common;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.VehicleBedrockCubeOBB;

@Mixin(Entity.class)
public class EntityMixin {

    @Inject(
            method = "getBoundingBox",
            at = @At("HEAD"),
            cancellable = true)
    public void getBoundingBox(CallbackInfoReturnable<AABB> cir) {
        if ((Object) this instanceof AbstractVehicle vehicle) {
            cir.setReturnValue(vehicle.getAABB());
        }
    }

    @Inject(
            method = "push(Lnet/minecraft/world/entity/Entity;)V",
            at = @At("HEAD"),
            cancellable = true)
    public void push(Entity pEntity, CallbackInfo ci) {
        if (pEntity instanceof AbstractVehicle vehicle) {
            VehicleBedrockCubeOBB bodyCube = vehicle.getVehicleBodyOBBs().get(0);
            if (!bodyCube.obb().contains(((Entity) (Object) this).position())) {
                ci.cancel();
            } else {
                vehicle.impact((Entity) (Object) this);
            }
        }
    }

}
