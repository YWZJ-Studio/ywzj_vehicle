package org.ywzj.vehicle.mixin.common;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Shadow
    public boolean noPhysics;

    @Shadow
    public abstract Vec3 getEyePosition();

    @Shadow
    public abstract boolean isPassengerOfSameVehicle(Entity pEntity);

    @Shadow
    public abstract double getX();

    @Shadow
    public abstract double getZ();

    @Shadow
    public abstract boolean isVehicle();

    @Shadow
    public abstract boolean isPushable();

    @Shadow
    public abstract void push(double pX, double pY, double pZ);

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
    public void push0(Entity pEntity, CallbackInfo ci) {
        if (!((Object) this instanceof AbstractVehicle) && pEntity instanceof AbstractVehicle vehicle) {
            if (vehicle.getMainCubeOBB().obb().contains(getEyePosition())) {
                if (!this.isPassengerOfSameVehicle(pEntity)) {
                    if (!pEntity.noPhysics && !this.noPhysics) {
                        double d0 = pEntity.getX() - this.getX();
                        double d1 = pEntity.getZ() - this.getZ();
                        double d2 = Mth.absMax(d0, d1);
                        if (d2 >= (double)0.01F) {
                            d2 = Math.sqrt(d2);
                            d0 /= d2;
                            d1 /= d2;
                            double d3 = 1.0D / d2;
                            if (d3 > 1.0D) {
                                d3 = 1.0D;
                            }

                            d0 *= d3;
                            d1 *= d3;
                            d0 *= 0.05F;
                            d1 *= 0.05F;
                            if (!this.isVehicle() && this.isPushable()) {
                                this.push(-d0, 0.0D, -d1);
                            }
                        }
                    }
                }
            }
            ci.cancel();
        }
    }

}
