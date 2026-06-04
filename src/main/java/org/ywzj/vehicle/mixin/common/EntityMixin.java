package org.ywzj.vehicle.mixin.common;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Shadow
    public abstract Vec3 position() ;

    @Shadow
    public abstract boolean isPassengerOfSameVehicle(Entity pEntity);

    @Shadow
    public abstract double getX();

    @Shadow
    public abstract double getZ();

    @Shadow
    public abstract boolean isVehicle();

    @Inject(
            method = "push(Lnet/minecraft/world/entity/Entity;)V",
            at = @At("HEAD"),
            cancellable = true)
    public void push0(Entity pEntity, CallbackInfo ci) {
        if (!((Object) this instanceof AbstractVehicle) && pEntity instanceof AbstractVehicle vehicle) {
            if (this.isPassengerOfSameVehicle(pEntity)) {
                ci.cancel();
                return;
            }
            vehicle.support((Entity) (Object) this);
            ci.cancel();
        }
    }

}
