package org.ywzj.vehicle.mixin.common;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.ywzj.vehicle.api.entity.OBBEntity;
import org.ywzj.vehicle.util.VectorUtil;

import java.util.Optional;
import java.util.function.Predicate;

@Mixin(ProjectileUtil.class)
public class ProjectileUtilMixin {

    @Inject(
            method = "getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;",
            at = @At("TAIL"),
            cancellable = true
    )
    private static void onGetEntityHitResult(
            Entity pShooter,
            Vec3 pStartVec,
            Vec3 pEndVec,
            AABB pBoundingBox,
            Predicate<Entity> pFilter,
            double pDistance,
            CallbackInfoReturnable<EntityHitResult> cir
    ) {
        EntityHitResult original = cir.getReturnValue();
        if (original == null) {
            return;
        }
        Entity entity = original.getEntity();
        if (entity instanceof OBBEntity) {
            EntityHitResult result = null;
            Vec3 closestHitPos = VectorUtil.closestHitObbPosition(entity, pStartVec, pEndVec);
            if (closestHitPos != null) {
                result = new EntityHitResult(original.getEntity(), closestHitPos);
            }
            cir.setReturnValue(result);
        }
    }

    @WrapOperation(
            method = "getEntityHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;F)Lnet/minecraft/world/phys/EntityHitResult;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/AABB;clip(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)Ljava/util/Optional;"
            )
    )
    private static Optional<Vec3> wrapClip(
            AABB aabb,
            Vec3 start,
            Vec3 end,
            Operation<Optional<Vec3>> original,
            @Local(ordinal = 2) Entity entity1
    ) {
        if (entity1 instanceof OBBEntity) {
            Optional<Vec3> optional = aabb.clip(start, end);
            if (optional.isEmpty() && aabb.contains(start)) {
                optional = Optional.of(start);
            }
            return optional;
        }
        return aabb.clip(start, end);
    }

    @Inject(
            method = "getEntityHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;F)Lnet/minecraft/world/phys/EntityHitResult;",
            at = @At("TAIL"),
            cancellable = true
    )
    private static void onGetEntityHitResult(
            Level pLevel,
            Entity pProjectile,
            Vec3 pStartVec,
            Vec3 pEndVec,
            AABB pBoundingBox,
            Predicate<Entity> pFilter,
            float pInflationAmount,
            CallbackInfoReturnable<EntityHitResult> cir) {
        EntityHitResult original = cir.getReturnValue();
        if (original == null) {
            return;
        }
        Entity entity = original.getEntity();
        if (entity instanceof OBBEntity) {
            EntityHitResult result = null;
            Vec3 closestHitPos = VectorUtil.closestHitObbPosition(entity, pStartVec, pEndVec);
            if (closestHitPos != null) {
                result = new EntityHitResult(original.getEntity(), closestHitPos);
            }
            cir.setReturnValue(result);
        }
    }

}
