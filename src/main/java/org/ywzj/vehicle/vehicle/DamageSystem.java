package org.ywzj.vehicle.vehicle;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Team;
import net.minecraftforge.common.MinecraftForge;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.all.AllDamageTypes;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.api.event.HitVehicleEvent;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.weapon.AmmoEntity;
import org.ywzj.vehicle.util.VectorUtil;
import org.ywzj.vehicle.vehicle.part.PartUnit;
import org.ywzj.vehicle.vehicle.structure.OBB;

public class DamageSystem {

    public static void hurt(DamageSource damageSource, float amount, AbstractVehicle vehicle) {
        Entity driver = vehicle.getDriver();
        if (driver != null) {
            Team driverTeam = driver.getTeam();
            if (driverTeam != null) {
                Entity attacker = damageSource.getEntity();
                if (attacker != null && attacker != driver) {
                    Team attackerTeam = attacker.getTeam();
                    if (attackerTeam != null) {
                        if (!driverTeam.isAllowFriendlyFire() && driverTeam.equals(attackerTeam)) {
                            return;
                        }
                    }
                }
            }
        }
        double scale = 1;
        boolean explosion = damageSource.getMsgId().equals("ywzj_vehicle.explosion");
        PartUnit<?> hitPartUnit = null;
        Vec3 hitPos = null;
        float caliber = 5.8f;
        if (damageSource.getDirectEntity() instanceof Projectile projectile) {
            hitPartUnit = VectorUtil.hitPartUnit(vehicle, projectile.position(), projectile.position().add(projectile.getDeltaMovement()), false);
            Vec3 closestHitPos = VectorUtil.closestHitObbPosition(vehicle, projectile.position(), projectile.position().add(projectile.getDeltaMovement()));
            if (closestHitPos != null) {
                hitPos = closestHitPos;
            }
            if (damageSource.getDirectEntity() instanceof AmmoEntity ammoEntity) {
                caliber = ammoEntity.getCaliber();
            }
        }
        if (explosion && damageSource.getDirectEntity() != null) {
            hitPos = damageSource.getDirectEntity().position();
        }
        if (amount < 0.1) {
            amount = 0;
        }
        // 部件损伤
        if (hitPartUnit != null && hitPartUnit.isDefensive() && !hitPartUnit.isDetached()) {
            float partUnitDamage = amount;
            if (partUnitDamage < 0.1) {
                partUnitDamage = 0;
            } else if (amount < hitPartUnit.getDefenseStats().damageThreshold) {
                partUnitDamage = 0.1f;
            }
            hitPartUnit.hurt(damageSource, partUnitDamage);
            amount = partUnitDamage * hitPartUnit.getDefenseStats().damageTransferCoefficient;
        }
        // 载具损伤
        if (amount < vehicle.defenseStats.damageThreshold) {
            amount = 0.1f;
        } else {
            if (hitPos == null) {
                scale = 0.2;
            } else if (!explosion) {
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
            Vec3 soundOffset = vehicle.relativeRotPos(hitPos, true).subtract(vehicle.position());
            MutableComponent message;
            float damageScale = amount / vehicle.getMaxHealth();
            if (damageScale >= 0.5) {
                vehicle.playVehicleSound(AllSounds.VEHICLE_HIT_BIG.get(), soundOffset, 1f, 4f, 1f, 0, false, false, true);
                message = Component.translatable("message.vehicle.damage_system.critical");
            } else if (damageScale >= 0.25) {
                vehicle.playVehicleSound(AllSounds.VEHICLE_HIT_MED.get(), soundOffset, 1f, 4f, 1f, 0, false, false, true);
                message = Component.translatable("message.vehicle.damage_system.hurt");
            } else if (damageScale > 0) {
                vehicle.playVehicleSound(AllSounds.VEHICLE_HIT_SMALL.get(), soundOffset, 1f, 4f, 1f, 0, false, false, true);
                message = Component.translatable("message.vehicle.damage_system.hit");
            } else {
                return;
            }
            if (damageSource.getDirectEntity() instanceof Projectile projectile && projectile.getOwner() != null) {
                HitVehicleEvent hitVehicleEvent = new HitVehicleEvent(projectile.getOwner().getUUID(),
                        vehicle.getId(),
                        hitPos,
                        projectile.getDeltaMovement(),
                        caliber,
                        amount,
                        message);
                MinecraftForge.EVENT_BUS.post(hitVehicleEvent);
            } else if (damageSource.getDirectEntity() instanceof AbstractVehicle && damageSource.getEntity() instanceof ServerPlayer serverPlayer) {
                Vec3 direction = vehicle.getBoundingBox().getCenter().subtract(hitPos).normalize();
                if (explosion) {
                    message = message.append(" ").append(Component.translatable("message.vehicle.damage_system.explosion").withStyle(ChatFormatting.YELLOW));
                }
                HitVehicleEvent hitVehicleEvent = new HitVehicleEvent(serverPlayer.getUUID(),
                        vehicle.getId(),
                        hitPos,
                        direction,
                        caliber,
                        amount,
                        message);
                MinecraftForge.EVENT_BUS.post(hitVehicleEvent);
            }
        } else {
            vehicle.playSound(vehicle.getHurtSound(damageSource), 2, 1);
        }
        YwzjVehicle.LOGGER.debug("{} damaged by {} with amount: {}", vehicle, damageSource, amount);
        vehicle.setHealth(vehicle.getHealth() - amount);
    }

    public static void impactHurt(double velocityDiff, AbstractVehicle vehicle) {
        velocityDiff *= 20;
        float damage = (float) (0.5 * vehicle.physicsEngine.physicsInfo.mass * velocityDiff * velocityDiff * vehicle.defenseStats.impactMultiplier);
        vehicle.hurt(AllDamageTypes.Sources.vehicleCollision(vehicle.level().registryAccess(), vehicle, vehicle.getDriver(), null), damage);
    }

}
