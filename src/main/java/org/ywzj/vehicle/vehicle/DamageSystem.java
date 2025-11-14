package org.ywzj.vehicle.vehicle;

import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.api.event.HitVehicleEvent;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.util.VectorUtil;
import org.ywzj.vehicle.vehicle.structure.OBB;

public class DamageSystem {

    public static void hurt(DamageSource damageSource, float amount, AbstractVehicle vehicle) {
        double scale;
        Vec3 hitPos = null;
        if (damageSource.getDirectEntity() instanceof Projectile) {
            hitPos = damageSource.getDirectEntity().position();
        }
        if (amount < 20) {
            scale = 0.2;
            amount = 1f;
        } else {
            if (hitPos == null) {
                scale = 0.2;
            } else {
                Vec3 hitVec = damageSource.getDirectEntity().getDeltaMovement();
                OBB obb = vehicle.getMainCubeOBB().obb();
                Vec3 corePos = vehicle.relativeRotPos(new Vec3(obb.center()), false);
                Vec3 diff = corePos.subtract(hitPos);
                Vec3 cross = diff.cross(hitVec);
                double distanceToCore = cross.length() / hitVec.length();
                double distanceMax = obb.extents().get(obb.extents().maxComponent()) * 2;
                scale = (distanceMax - distanceToCore) / distanceMax;
            }
        }
        amount *= (float) scale;
        if (hitPos != null) {
            Component message;
            if (scale >= 0.7) {
                vehicle.playSound(AllSounds.VEHICLE_HIT_BIG.get(), 1, 1);
                message = Component.translatable("message.vehicle.damage_system.critical");
            } else if (scale > 0.3) {
                vehicle.playSound(AllSounds.VEHICLE_HIT_MED.get(), 1, 1);
                message = Component.translatable("message.vehicle.damage_system.hurt");
            } else {
                vehicle.playSound(AllSounds.VEHICLE_HIT_SMALL.get(), 1, 1);
                message = Component.translatable("message.vehicle.damage_system.hit");
            }
            if (damageSource.getDirectEntity() instanceof Projectile projectile) {
                if (projectile.getOwner() != null) {
                    Vec3 closestHitPos = VectorUtil.closestHitObbPosition(vehicle, projectile.position(), projectile.position().add(projectile.getDeltaMovement()));
                    if (closestHitPos != null) {
                        hitPos = closestHitPos;
                    }
                    HitVehicleEvent hitVehicleEvent = new HitVehicleEvent(projectile.getOwner().getUUID(),
                            vehicle.getId(),
                            vehicle.relativeRotPos(hitPos, true).subtract(vehicle.position()),
                            vehicle.relativeRotDirection(projectile.getDeltaMovement(), true),
                            amount,
                            message);
                    MinecraftForge.EVENT_BUS.post(hitVehicleEvent);
                }
            }
        } else {
            vehicle.playSound(vehicle.getHurtSound(damageSource), 1, 1);
        }
        vehicle.setHealth(vehicle.getHealth() - amount);
    }

}
