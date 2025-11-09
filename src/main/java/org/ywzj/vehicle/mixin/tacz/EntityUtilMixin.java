package org.ywzj.vehicle.mixin.tacz;

import com.tacz.guns.api.event.common.HitBodyPartEvent;
import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.util.EntityUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.ywzj.vehicle.api.event.HitVehicleEvent;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

@Pseudo
@Mixin(EntityUtil.class)
public class EntityUtilMixin {

    @Inject(method = "getHitResult", at = @At("HEAD"), cancellable = true, remap = false)
    private static void getHitResult(Projectile bulletEntity, Entity entity, Vec3 startVec, Vec3 endVec, CallbackInfoReturnable<EntityKineticBullet.EntityResult> cir) {
        if (entity instanceof AbstractVehicle vehicle) {
            Vec3 closestHitPos = null;
            double minDistance = Double.MAX_VALUE;
            for (var obb : vehicle.getOBBs()) {
                var obbVec = obb.clip(startVec.toVector3f(), endVec.toVector3f()).orElse(null);
                if (obbVec != null) {
                    Vec3 hitPos = new Vec3(obbVec);
                    double distance = hitPos.distanceToSqr(startVec);
                    if (distance < minDistance) {
                        minDistance = distance;
                        closestHitPos = hitPos;
                    }
                }
            }
            if (closestHitPos != null) {
                if (bulletEntity.getOwner() != null) {
                    HitVehicleEvent hitVehicleEvent = new HitVehicleEvent(bulletEntity.getOwner().getUUID(),
                            entity.getId(),
                            vehicle.relativeRotPos(closestHitPos, true).subtract(entity.position()),
                            vehicle.relativeRotDirection(bulletEntity.getDeltaMovement(), true));
                    MinecraftForge.EVENT_BUS.post(hitVehicleEvent);
                }
                cir.setReturnValue(new EntityKineticBullet.EntityResult(entity, closestHitPos, false, HitBodyPartEvent.BodyPart.TORSO));
            } else {
                cir.setReturnValue(null);
            }
        }
    }

}
