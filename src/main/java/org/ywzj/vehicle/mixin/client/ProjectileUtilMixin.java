package org.ywzj.vehicle.mixin.client;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.ywzj.vehicle.api.entity.OBBEntity;
import org.ywzj.vehicle.util.VectorUtil;

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
        EntityHitResult result = null;
        Entity entity = original.getEntity();
        if (entity instanceof OBBEntity) {
            Vec3 closestHitPos = VectorUtil.closestHitObbPosition(entity, pStartVec, pEndVec);
            if (closestHitPos != null) {
                result = new EntityHitResult(original.getEntity(), closestHitPos);
            }
            cir.setReturnValue(result);
        }
    }

}
