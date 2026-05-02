package org.ywzj.vehicle.mixin.sable;

import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.LivingEntityMovementExtension;
import dev.ryanhcode.sable.sublevel.entity_collision.SubLevelEntityCollision;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

@Mixin(AbstractVehicle.class)
@Implements(@Interface(iface = LivingEntityMovementExtension.class, prefix = "sableext$"))
public abstract class AbstractVehicleSableMixin {

    @Unique
    private final Vector3d sable$inheritedVelocity = new Vector3d();

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void sable$injectTickDrag(CallbackInfo ci) {
        AbstractVehicle self = (AbstractVehicle) (Object) this;
        if (!self.level().isClientSide()) {
            sable$tickDrag();
        }
    }

    @Unique
    private void sable$tickDrag() {
        final EntityMovementExtension ext = (EntityMovementExtension) this;
        final SubLevelEntityCollision.CollisionInfo info = ext.sable$getCollisionInfo();

        final double threshold = 0.0000001;
        if (this.sable$inheritedVelocity.lengthSquared() <= threshold) {
            this.sable$inheritedVelocity.zero();
        }

        if ((info == null || info.inheritedMotion == null) && this.sable$inheritedVelocity.lengthSquared() > threshold) {
            sable$applyDrag();
        }
    }

    @Unique
    private void sable$applyDrag() {
        AbstractVehicle self = (AbstractVehicle) (Object) this;

        if (self.verticalCollision || self.onGround()) {
            final double drag = 0.7;
            this.sable$inheritedVelocity.mul(drag, 0.0, drag);
        }

        if (self.horizontalCollision) {
            final double drag = 0.8;
            this.sable$inheritedVelocity.mul(drag, 0.6, drag);
        }

        if (self.isInWater()) {
            this.sable$inheritedVelocity.mul(0.9);
        }

        this.sable$inheritedVelocity.mul(0.99);

        if (Math.abs(this.sable$inheritedVelocity.y) < 0.01) {
            this.sable$inheritedVelocity.y = 0.0;
        }
    }

    public Vector3d sableext$sable$getInheritedVelocity() {
        return this.sable$inheritedVelocity;
    }

}
