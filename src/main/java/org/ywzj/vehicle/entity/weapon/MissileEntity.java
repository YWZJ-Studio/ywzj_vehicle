package org.ywzj.vehicle.entity.weapon;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.PlayMessages;
import org.ywzj.vehicle.all.AllEntities;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.api.entity.RemoteTickEntity;
import org.ywzj.vehicle.api.entity.SightObstruction;
import org.ywzj.vehicle.api.entity.TargetObstruction;
import org.ywzj.vehicle.audio.VehicleSound;
import org.ywzj.vehicle.custom.part.data.WeaponUnitData;
import org.ywzj.vehicle.custom.weapon.data.VehicleMissileWeaponData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.network.Channel;
import org.ywzj.vehicle.network.message.ServerVehicleWarn;
import org.ywzj.vehicle.util.EntityUtil;
import org.ywzj.vehicle.util.VectorUtil;
import org.ywzj.vehicle.util.VehicleExplosion;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.PhysicsEngine;
import org.ywzj.vehicle.vehicle.part.RadarUnit;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;
import org.ywzj.vehicle.vehicle.pojo.WarnType;
import org.ywzj.vehicle.vehicle.weapon.seeker.Radar;

import java.util.List;

public class MissileEntity extends AmmoEntity implements RemoteTickEntity {

    public float seekerFov;
    public float mass;
    public float thrust;
    public float motorBurnTime;
    public float dragCoefficient;
    public float maxG;
    public float referenceSpeed;
    public float activeRadarActivationRange = 1024;
    private VehicleMissileWeaponData.Guidance guidance;
    private VehicleMissileWeaponData.HomingMode homingMode;
    public boolean activeRadarOn;
    public int activeRadarLostTargetTick;
    public Entity targetEntity;
    public Vec3 targetVec;
    public Vec3 targetPos;
    public int ownerId;
    private WeaponUnit weaponUnit;
    private VehicleSound sound;

    public MissileEntity(EntityType<? extends Projectile> entityType, Level level, VehicleMissileWeaponData data, WeaponUnit weaponUnit) {
        super(entityType, level, data.getWeaponId());
        this.seekerFov = data.getSeekerFov();
        this.mass = data.getMass();
        this.thrust = data.getThrust();
        this.motorBurnTime = data.getMotorBurnTime();
        this.dragCoefficient = data.getDragCoefficient();
        this.maxG = data.getMaxG();
        this.referenceSpeed = data.getReferenceSpeed();
        this.guidance = data.getGuidance();
        this.homingMode = data.getHomingMode();
        this.weaponUnit = weaponUnit;
        this.damage = data.getDamage();
        this.explosion = data.getExplosion();
        this.life = data.getLife();
    }

    public MissileEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level, null);
    }

    public MissileEntity(PlayMessages.SpawnEntity spawnEntity, Level level) {
        super(AllEntities.MISSILE.get(), level, null);
    }

    public void shoot(AbstractVehicle vehicle, Component name, Vec3 spawnPos, float ammoXRot, float ammoYRot, LivingEntity shooter) {
        this.vehicle = vehicle;
        this.name = name;
        this.setPos(spawnPos);
        this.setRot(ammoYRot, ammoXRot);
        this.setOwner(shooter);
    }

    @Override
    public void remoteTick() {
        updateOwner();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            tickParticle();
            tickSound();
            updateOwner();
        } else {
            tickGuidance();
            tickMove();
            tickHit();
            life -= 1;
            if (life < 0 && !isRemoved()) {
                if (explosion != null) {
                    VehicleExplosion vehicleExplosion = new VehicleExplosion(level(), this.getOwner(), this.vehicle, this.position(), explosion.radius, explosion.damage, explosion.destroyBlock);
                    vehicleExplosion.explode();
                }
                this.discard();
            }
        }
    }

    private void tickGuidance() {
        if (guidance == VehicleMissileWeaponData.Guidance.HOMING) {
            tickTrack();
            if (targetEntity != null) {
                targetPos = targetEntity.position();
                // 主动弹雷达开机
                if (homingMode == VehicleMissileWeaponData.HomingMode.ACTIVE_RADAR
                        && targetEntity.distanceTo(this) <= activeRadarActivationRange) {
                    activeRadarOn = true;
                }
            }
            if (activeRadarOn && targetEntity == null) {
                if (activeRadarLostTargetTick < 60) {
                    activeRadarLostTargetTick += 1;
                } else {
                    // 主动弹雷达长时间脱锁则自爆
                    life = 0;
                }
            }
            // 截射
            if (targetEntity != null) {
                proportionalGuide(targetEntity, null);
            } else if (targetPos != null) {
                proportionalGuide(null, targetPos);
            }
        } else if (guidance == VehicleMissileWeaponData.Guidance.PRESET) {
            if (targetPos != null) {
                proportionalGuide(null, targetPos);
            }
        } else if (guidance == VehicleMissileWeaponData.Guidance.SACLOS) {
            Vec2 rot = weaponUnit.worldRot();
            Vec3 start = weaponUnit.worldPivotPosition();
            Vec3 dir = VectorUtil.rotToVec(rot.x, rot.y).normalize();
            Vec3 pos = this.position();
            // 计算实体在驾束射线上的投影点
            Vec3 startToPos = pos.subtract(start);
            double t = startToPos.dot(dir); // 投影系数（沿射线方向的距离）
            if (t < 0) t = 0; // 限制在射线范围内
            Vec3 proj = start.add(dir.scale(t));
            // 当前点逐渐靠近射线（朝投影点移动）
            double speed = 0.8;
            // 逐步解锁机动
            float maneuverability = Math.min((float) tickCount / 20, 1);
            // 每 tick 靠近速度
            speed *= maneuverability;
            Vec3 delta = proj.subtract(pos);
            if (delta.length() > speed) {
                delta = delta.normalize().scale(speed);
            }
            this.setPos(pos.add(delta));
            this.setRot(Mth.lerp(maneuverability, this.getYRot(), rot.y), Mth.lerp(maneuverability, this.getXRot(), rot.x));
        }
    }

    private void tickMove() {
        if (targetVec != null && targetPos != null && this.position().distanceTo(targetPos) < 5f) {
            targetPos = VectorUtil.hitPosition(this, targetPos, targetPos.add(targetVec.scale(256)));
        }
        EntityUtil.keepChunkLoaded(this, this.position());
        EntityUtil.keepChunkLoaded(this, this.position().add(getLookAngle().normalize().scale(16)));
        Vec3 velocity = this.getDeltaMovement();
        Vec3 lookDir = this.getLookAngle();
        // 推力
        if (this.tickCount <= motorBurnTime) {
            double acceleration = (this.thrust / this.mass);
            velocity = velocity.add(lookDir.scale(acceleration));
        }
        // 空气阻力
        double speedSqr = velocity.lengthSqr();
        if (speedSqr > 0) {
            Vec3 drag = velocity.normalize().scale(-dragCoefficient * speedSqr);
            velocity = velocity.add(drag);
        }
        // 重力
        velocity = velocity.subtract(0, PhysicsEngine.G, 0);
        // 更新速度与位置
        this.setDeltaMovement(velocity);
        double dx = this.getX() + velocity.x;
        double dy = this.getY() + velocity.y;
        double dz = this.getZ() + velocity.z;
        this.setPos(dx, dy, dz);
        // 自动归正
        if (this.tickCount > motorBurnTime && targetEntity == null && targetPos == null) {
            if (velocity.lengthSqr() > 0.01) {
                Vec3 normVel = velocity.normalize();
                double pitch = Math.toDegrees(-Math.asin(normVel.y));
                double yaw = Math.toDegrees(Math.atan2(normVel.z, normVel.x)) - 90.0;
                // 缓慢过渡到速度方向
                this.setXRot((float) Mth.lerp(0.2, this.getXRot(), pitch));
                this.setYRot((float) Mth.lerp(0.2, this.getYRot(), yaw));
            }
        }
    }

    private void tickTrack() {
        boolean radar = false;
        if (homingMode == VehicleMissileWeaponData.HomingMode.SEMI_ACTIVE_RADAR) {
            // 半主动雷达制导
            if (weaponUnit.getRadarUnit() != null) {
                // 持续从载机雷达获取目标
                targetEntity = weaponUnit.getRadarUnit().getLockedEntity();
                // 半主动雷达弹引导需告警
                radar = true;
            }
        } else if (homingMode == VehicleMissileWeaponData.HomingMode.ACTIVE_RADAR) {
            // 主动雷达制导
            if (targetEntity == null) {
                // 若导弹丢失目标，且主动弹雷达未开机，则从载机雷达获取目标
                if (weaponUnit.getRadarUnit() != null && !activeRadarOn) {
                    targetEntity = weaponUnit.getRadarUnit().getLockedEntity();
                }
                // 若载机雷达无目标，或主动弹开机，则自行寻找目标
                if (targetEntity == null) {
                    List<Entity> detectedEntities = scanTargets();
                    if (!detectedEntities.isEmpty()) {
                        targetEntity = detectedEntities.get(0);
                        // 主动弹雷达锁定需告警
                        radar = true;
                    }
                }
            } else {
                // 主动弹雷达未开机，载机雷达是否仍扫描到目标
                if (!activeRadarOn) {
                    RadarUnit radarUnit = weaponUnit.getRadarUnit();
                    if (radarUnit == null || !radarUnit.getDetectedEntities().containsKey(targetEntity.getId())) {
                        targetEntity = null;
                    }
                }
                // 主动弹雷达开机，导弹雷达是否仍扫描到目标
                else {
                    List<Entity> detectedEntities = scanTargets();
                    targetEntity = Radar.checkTarget(this, detectedEntities, targetEntity);
                    // 主动弹雷达锁定需告警
                    radar = true;
                }
            }
        } else if (weaponUnit.getFireControlSensorType() == WeaponUnitData.FireControlSensorType.IR
                || weaponUnit.getFireControlSensorType() == WeaponUnitData.FireControlSensorType.EO) {
            if (targetEntity == null) {
                return;
            }
            Vec3 checkStart = this.position();
            Vec3 checkEnd = targetEntity.position();
            EntityHitResult entityHit = VectorUtil.hitEntity(vehicle, checkStart, checkEnd);
            if (entityHit != null) {
                Entity entity = entityHit.getEntity();
                // 锁定实体是否被视觉遮挡
                if (entity instanceof SightObstruction) {
                    targetEntity = null;
                }
                // 锁定实体是否被干扰
                if (entity instanceof TargetObstruction) {
                    targetEntity = entity;
                }
            }
        }
        // 通知导弹锁定给目标载具乘客
        if (tickCount % 2 == 0 && targetEntity != null && radar) {
            ServerVehicleWarn packet = new ServerVehicleWarn(this.getId(), targetEntity.getId(), WarnType.MISSILE_LAUNCH, "MSL");
            Channel.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> targetEntity), packet);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void tickParticle() {
        Vec3 pos = this.position();
        level().addParticle(
                ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, true,
                pos.x, pos.y, pos.z,
                0.0D, 0.0D, 0.0D
        );
    }

    @OnlyIn(Dist.CLIENT)
    public void tickSound() {
        if (sound == null) {
            sound = new VehicleSound(AllSounds.ROCKET_FLYING.get(), 1f, 1f, 1f, false, 50, true, true, this.getId());
            sound.play();
        }
    }

    @Override
    public void writeData(CompoundTag data) {
        if (getOwner() != null) {
            data.putInt("ownerId", getOwner().getId());
        }
    }

    @Override
    public void readData(CompoundTag data) {
        if (data.contains("ownerId")) {
            ownerId = data.getInt("ownerId");
        }
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        super.writeSpawnData(buffer);
        buffer.writeInt(getOwner() == null ? -1 : getOwner().getId());
    }

    @Override
    public void readSpawnData(FriendlyByteBuf additionalData) {
        super.readSpawnData(additionalData);
        ownerId = additionalData.readInt();
    }

    private List<Entity> scanTargets() {
        return Radar.scanTargets(this, this.position(), activeRadarActivationRange, entityPos -> {
            Vec2 aimRot = VectorUtil.vecToRot(entityPos.subtract(this.position()));
            return Math.abs(aimRot.x - getXRot()) <= seekerFov && Math.abs(aimRot.y - getYRot()) <= seekerFov;
        });
    }

    private void updateOwner() {
        if (ownerId == LocalVehiclePlayer.instance.getPlayer().getId()) {
            LocalVehiclePlayer.instance.missiles.put(this, LocalVehiclePlayer.instance.getPlayer().tickCount);
        }
    }

    public void proportionalGuide(Entity target, Vec3 pos) {
        Vec3 missilePos = this.position();
        Vec3 missileVel = this.getDeltaMovement();
        Vec3 targetPos;
        Vec3 targetVel;
        if (target != null) {
            targetVel = target.getDeltaMovement();
            targetPos = target.getEyePosition().add(targetVel);
        } else {
            targetPos = pos;
            targetVel = Vec3.ZERO;
        }

        Vec3 relPos = targetPos.subtract(missilePos);
        Vec3 relVel = targetVel.subtract(missileVel);

        double missileSpeed = missileVel.length();
        double closingSpeed = missileSpeed - relVel.dot(relPos.normalize());
        double t = relPos.length() / Math.max(closingSpeed, 0.1);

        Vec3 interceptPos = targetPos.add(targetVel.scale(t));

        Vec3 desiredDir = interceptPos.subtract(missilePos).normalize();
        Vec3 currentDir = missileVel.lengthSqr() < 1e-4
                ? desiredDir
                : missileVel.normalize();

        double N = 256; // PN 系数
        Vec3 steering = desiredDir.subtract(currentDir).scale(N);

        // 截射目的地
        Vec3 newVel = missileVel.add(steering)
                .normalize()
                .scale(missileSpeed);
        Vec3 finalTargetPos = missilePos.add(newVel);

        double d0 = finalTargetPos.x - missilePos.x;
        double d1 = finalTargetPos.y - missilePos.y;
        double d2 = finalTargetPos.z - missilePos.z;
        double d3 = Math.sqrt(d0 * d0 + d2 * d2);
        float xRot = Mth.wrapDegrees((float)(-(Mth.atan2(d1, d3) * (double)(180F / (float)Math.PI))));
        float yRot = Mth.wrapDegrees((float)(Mth.atan2(d2, d0) * (double)(180F / (float)Math.PI)) - 90.0F);

        // 适配过载限制
        Vec3 vel = this.getDeltaMovement();
        double speed = vel.length();

        // 失速
        if (speed < 0.2) {
            return;
        }

        // 机动
        double dynamicPressureFactor = Math.min(1.0, (speed * speed) / (referenceSpeed * referenceSpeed));
        // 当前速度下物理能达到的最大过载
        double availableG = this.maxG * dynamicPressureFactor;

        // 角速度
        double maxAccelLimit = availableG * PhysicsEngine.G;

        // 根据向心加速度公式 a = v * omega  =>  omega = a / v (弧度/tick)
        double maxOmega = maxAccelLimit / speed;
        double maxAnglePerTick = Math.toDegrees(maxOmega);

        // 逐步解锁机动
        if (this.tickCount < 5) {
            maxAnglePerTick *= Math.pow((double) this.tickCount / 5.0, 2);
        }

        float curX = this.getXRot();
        float curY = this.getYRot();
        float deltaX = Mth.wrapDegrees(xRot - curX);
        float deltaY = Mth.wrapDegrees(yRot - curY);
        deltaX = Mth.clamp(deltaX, (float)-maxAnglePerTick, (float)maxAnglePerTick);
        deltaY = Mth.clamp(deltaY, (float)-maxAnglePerTick, (float)maxAnglePerTick);

        this.setXRot(curX + deltaX);
        this.setYRot(curY + deltaY);
    }

}
