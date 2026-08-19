package org.ywzj.vehicle.mixin.common;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.ywzj.vehicle.api.entity.VehicleParented;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.parenting.DeckAttachment;
import org.ywzj.vehicle.vehicle.parenting.VehicleParenting;

@Mixin(Entity.class)
public abstract class EntityMixin implements VehicleParented {

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

    @Unique
    @Nullable
    private DeckAttachment ywzj_vehicle$deckAttachment;

    @Override
    @Nullable
    public DeckAttachment ywzj_vehicle$deckAttachment() {
        return this.ywzj_vehicle$deckAttachment;
    }

    @Override
    public void ywzj_vehicle$setDeckAttachment(@Nullable DeckAttachment attachment) {
        this.ywzj_vehicle$deckAttachment = attachment;
    }

    @Inject(
            method = "push(Lnet/minecraft/world/entity/Entity;)V",
            at = @At("HEAD"),
            cancellable = true)
    public void push0(Entity pEntity, CallbackInfo ci) {
        // Mixin typing is sensitive; preserve the (Object) casts.
        if (!((Object) this instanceof AbstractVehicle) && pEntity instanceof AbstractVehicle vehicle) {
            if (this.isPassengerOfSameVehicle(pEntity)) {
                ci.cancel();
                return;
            }
            vehicle.support((Entity) (Object) this);
            ci.cancel();
        }
    }

    /**
     * Runs the vehicle's carry deferred to this entity's own tick.
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void ywzj_vehicle$tickDeckCarry(CallbackInfo ci) {
        if (this.ywzj_vehicle$deckAttachment == null) {
            return;
        }
        VehicleParenting.onEntityTicked((Entity) (Object) this);
    }

    /**
     * Clips this entity's movement against the vehicle it is standing on in the vehicle's frame.
     */
    @Inject(method = "collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;",
            at = @At("RETURN"),
            cancellable = true)
    private void ywzj_vehicle$collideWithDeck(Vec3 wanted, CallbackInfoReturnable<Vec3> cir) {
        if (this.ywzj_vehicle$deckAttachment == null) {
            return;
        }
        Vec3 allowed = cir.getReturnValue();
        Vec3 clipped = VehicleParenting.clipMovement((Entity) (Object) this, allowed);
        if (clipped != allowed) {
            cir.setReturnValue(clipped);
        }
    }

}
