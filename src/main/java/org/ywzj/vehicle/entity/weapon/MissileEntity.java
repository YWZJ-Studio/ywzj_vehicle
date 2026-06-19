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
import org.joml.Vector3f;
import org.ywzj.vehicle.all.AllEntities;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.api.entity.RemoteTickEntity;
import org.ywzj.vehicle.api.entity.SightObstruction;
import org.ywzj.vehicle.api.entity.TargetObstruction;
import org.ywzj.vehicle.audio.VehicleSound;
import org.ywzj.vehicle.custom.CommonAssetsManager;
import org.ywzj.vehicle.custom.part.data.WeaponUnitData;
import org.ywzj.vehicle.custom.weapon.VehicleWeaponIndex;
import org.ywzj.vehicle.custom.weapon.data.VehicleMissileWeaponData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.network.Channel;
import org.ywzj.vehicle.network.message.ServerVehicleWarn;
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
    public int coldLaunchTimeTick;
    public Vec3 coldLaunchVelocity = new Vec3(0, -1, 0);
    public float dragCoefficient;
    public float maxG;
    public float referenceSpeed;
    public float activeRadarActivationRange = 1024;
    private VehicleMissileWeaponData.Guidance guidance;
    private VehicleMissileWeaponData.HomingMode homingMode;
    public boolean activeRadarOn;
    public boolean activeRadarCatch;
    public int activeRadarLostTargetTick;
    public Entity targetEntity;
    public Vec3 targetVec;
    public Vec3 targetPos;
    public int ownerId;
    private WeaponUnit weaponUnit;
    private VehicleSound sound;
    private Vec3 particlePosO;

    public MissileEntity(EntityType<? extends Projectile> entityType, Level level, VehicleMissileWeaponData data, WeaponUnit weaponUnit) {
        super(entityType, level, data.getWeaponId());
        initMissile(data);
        this.weaponUnit = weaponUnit;
        this.keepChunkLoaded = true;
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
    public void writeSpawnData(FriendlyByteBuf buffer) {
        super.writeSpawnData(buffer);
        buffer.writeInt(getOwner() == null ? -1 : getOwner().getId());
        buffer.writeInt(coldLaunchTimeTick);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf additionalData) {
        super.readSpawnData(additionalData);
        ownerId = additionalData.readInt();
        coldLaunchTimeTick = additionalData.readInt();
        VehicleWeaponIndex<?, ?> vehicleWeaponIndex = CommonAssetsManager.vehicleWeaponManager().getIndex(getWeaponId()).orElse(null);
        if (vehicleWeaponIndex != null && vehicleWeaponIndex.data() instanceof VehicleMissileWeaponData data) {
            initMissile(data);
        }
    }

    private void initMissile(VehicleMissileWeaponData data) {
        this.seekerFov = data.getSeekerFov();
        this.mass = data.getMass();
        this.thrust = data.getThrust();
        this.motorBurnTime = data.getMotorBurnTime();
        this.dragCoefficient = data.getDragCoefficient();
        this.maxG = data.getMaxG();
        this.referenceSpeed = data.getReferenceSpeed();
        this.guidance = data.getGuidance();
        this.homingMode = data.getHomingMode();
        this.damage = data.getDamage();
        this.explosion = data.getExplosion();
        this.life = data.getLife();
    }

    public void initColdLaunch(WeaponUnit weaponUnit) {
        if (weaponUnit == null) {
            return;
        }
        this.coldLaunchTimeTick = weaponUnit.getColdLaunchTimeTick();
        this.coldLaunchVelocity = weaponUnit.getColdLaunchVelocity();
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
                    VehicleExplosion vehicleExplosion = new VehicleExplosion(level(), this.getOwner(), this, this.position(), explosion.radius, explosion.damage, explosion.destroyBlock);
                    vehicleExplosion.explode();
                }
                this.discard();
            }
        }
    }

    private void tickGuidance() {
        if (tickCount < coldLaunchTimeTick) {
            return;
        }
        if (guidance == VehicleMissileWeaponData.Guidance.HOMING) {
            tickTrack();
            if (tickCount >= 20 && targetEntity != null) {
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
                intercept(targetEntity, null);
            } else if (targetPos != null) {
                intercept(null, targetPos);
            }
        } else if (guidance == VehicleMissileWeaponData.Guidance.PRESET) {
            if (targetPos != null) {
                intercept(null, targetPos);
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
        Vec3 velocity = this.getDeltaMovement();
        if (tickCount >= coldLaunchTimeTick) {
            Vec3 lookDir = this.getLookAngle();
            int motorTick = tickCount - coldLaunchTimeTick;
            // 推力
            if (motorTick <= motorBurnTime) {
                double acceleration = (this.thrust / this.mass);
                velocity = velocity.add(lookDir.scale(acceleration));
            }
            // 空气阻力
            double speedSqr = velocity.lengthSqr();
            if (speedSqr > 0) {
                Vec3 drag = velocity.normalize().scale(-dragCoefficient * speedSqr);
                velocity = velocity.add(drag);
            }
        }
        if (tickCount < coldLaunchTimeTick) {
            // 弹仓弹射
            Vector3f[] axes = vehicle.getMainCubeOBB().obb().getAxes();
            Vec3 launchVelocity = new Vec3(axes[0]).scale(coldLaunchVelocity.x)
                    .add(new Vec3(axes[1]).scale(coldLaunchVelocity.y))
                    .add(new Vec3(axes[2]).scale(coldLaunchVelocity.z));
            velocity = vehicle.getDeltaMovement().add(launchVelocity);
        } else {
            // 重力
            velocity = velocity.subtract(0, PhysicsEngine.G, 0);
        }
        // 更新速度与位置
        this.setDeltaMovement(velocity);
        double dx = this.getX() + velocity.x;
        double dy = this.getY() + velocity.y;
        double dz = this.getZ() + velocity.z;
        this.setPos(dx, dy, dz);
        // 自动归正
        if (tickCount >= coldLaunchTimeTick) {
            int motorTick = tickCount - coldLaunchTimeTick;
            if (motorTick > motorBurnTime && targetEntity == null && targetPos == null) {
                if (velocity.lengthSqr() > 0.01) {
                    Vec3 normVel = velocity.normalize();
                    double pitch = Math.toDegrees(-Math.asin(normVel.y));
                    double yaw = Math.toDegrees(Math.atan2(normVel.z, normVel.x)) - 90.0;
                    this.setXRot((float) Mth.lerp(0.2, this.getXRot(), pitch));
                    this.setYRot((float) Mth.lerp(0.2, this.getYRot(), yaw));
                }
            }
        }
    }

    private void tickTrack() {
        boolean radar = false;
        if (homingMode == VehicleMissileWeaponData.HomingMode.SEMI_ACTIVE_RADAR) {
            // 半主动雷达制导
            if (weaponUnit.getMainRadarUnit() != null) {
                // 持续从载机雷达获取目标
                targetEntity = weaponUnit.getMainRadarUnit().getLockedEntity();
                // 半主动雷达弹引导需告警
                radar = true;
            }
        } else if (homingMode == VehicleMissileWeaponData.HomingMode.ACTIVE_RADAR) {
            // 主动雷达制导
            if (activeRadarOn) {
                List<Entity> detectedEntities = scanTargets();
                Entity activeRadarTarget = null;
                // 主动雷达是否能截获当前目标
                if (targetEntity != null) {
                    activeRadarTarget = Radar.checkTarget(this, detectedEntities, targetEntity);
                    if (activeRadarTarget == targetEntity) {
                        activeRadarCatch = true;
                    }
                }
                // 若无目标，主动雷达截获一个扫描到的目标
                if (activeRadarTarget == null) {
                    if (!detectedEntities.isEmpty()) {
                        targetEntity = detectedEntities.get(0);
                        activeRadarCatch = true;
                    }
                }
                // 主动雷达锁定需告警
                radar = true;
            }
            // 主动雷达截获目标前，载机雷达是否仍扫描到目标
            if (!activeRadarCatch && targetEntity != null) {
                RadarUnit mainRadarUnit = weaponUnit.getMainRadarUnit();
                if (mainRadarUnit == null || !mainRadarUnit.getDetectedEntities().containsKey(targetEntity.getId())) {
                    targetEntity = null;
                }
            }
        } else if (weaponUnit.getFireControlSensorType() == WeaponUnitData.FireControlSensorType.IR
                || homingMode == VehicleMissileWeaponData.HomingMode.INFRARED
                || weaponUnit.getFireControlSensorType() == WeaponUnitData.FireControlSensorType.EO
                || homingMode == VehicleMissileWeaponData.HomingMode.ELECTRO_OPTICAL) {
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
        if (tickCount < coldLaunchTimeTick) {
            return;
        }
        if (tickCount <= motorBurnTime) {
            Vec3 pos = this.position().add(this.getLookAngle().scale(-3));
            Vec3 posO = particlePosO == null ? pos : particlePosO;
            Vec3 step = pos.subtract(posO);
            double dist = step.length();
            int segments = (int) (dist / 0.5);
            Vec3 dir = step.normalize();
            for (int i = 0; i <= segments; i++) {
                Vec3 particlePos = posO.add(dir.scale(i * 0.5));
                level().addParticle(
                        ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, true,
                        particlePos.x, particlePos.y, particlePos.z,
                        0.0D, 0.0D, 0.0D
                );
            }
            particlePosO = pos;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void tickSound() {
        if (tickCount < coldLaunchTimeTick) {
            return;
        }
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
    public float getCaliber() {
        return 40f;
    }

    private List<Entity> scanTargets() {
        return Radar.scanTargets(this, this.position(), activeRadarActivationRange,
                entityPos -> Math.toDegrees(VectorUtil.angleBetween(this.getLookAngle(), entityPos.subtract(this.position()))) <= seekerFov);
    }

    private void updateOwner() {
        if (ownerId == LocalVehiclePlayer.instance.getPlayer().getId()) {
            LocalVehiclePlayer.instance.missiles.put(this, LocalVehiclePlayer.instance.getPlayer().tickCount);
        }
    }

    public void intercept(Entity target, Vec3 pos) {
        Vec3 missilePos = this.position();
        Vec3 missileVel = this.getDeltaMovement();
        double missileSpeed = missileVel.length();
        if (missileSpeed == 0) {
            return;
        }
        Vec3 targetPos;
        Vec3 targetVel;
        if (target != null) {
            targetVel = target.getDeltaMovement();
            targetPos = target.getEyePosition().add(targetVel);
        } else {
            targetVel = Vec3.ZERO;
            targetPos = pos;
        }
        double targetSpeedSq = targetVel.lengthSqr();
        // 追及相遇时间
        Vec3 d = targetPos.subtract(missilePos);
        double a = targetSpeedSq - (missileSpeed * missileSpeed);
        double b = 2.0 * d.dot(targetVel);
        double c = d.lengthSqr();
        double t = -1.0;
        if (Math.abs(a) < 1e-5) {
            if (b < 0) t = -c / b;
        } else {
            double discriminant = b * b - 4 * a * c;
            if (discriminant >= 0) {
                double t1 = (-b + Math.sqrt(discriminant)) / (2 * a);
                double t2 = (-b - Math.sqrt(discriminant)) / (2 * a);
                // 取最小的正数时间
                if (t1 > 0 && t2 > 0) {
                    t = Math.min(t1, t2);
                } else {
                    t = Math.max(t1, t2);
                }
            }
        }
        // 如果无解（例如目标比导弹快且正在远离），降级为简单的纯追踪或一阶近似
        if (t <= 0) {
            Vec3 relVel = targetVel.subtract(missileVel);
            double closingSpeed = missileSpeed - relVel.dot(d.normalize());
            t = d.length() / Math.max(closingSpeed, 0.1);
        }
        // 拦截点
        Vec3 interceptPos = targetPos.add(targetVel.scale(t));
        // 导弹当前速度与推力下，飞向拦截点的修正指向
        Vec3 desiredDir;
        double acceleration = (this.thrust / this.mass);
        Vec3 targetDir = interceptPos.subtract(missilePos).normalize();
        double dot = missileVel.dot(targetDir);
        double magSq = missileSpeed * missileSpeed;
        double discriminant = dot * dot - (magSq - acceleration * acceleration);
        if (discriminant < 0) {
            desiredDir = targetDir.scale(dot * PhysicsEngine.MAGIC_NUMBER * 4).subtract(missileVel);
        } else {
            double k = dot + Math.sqrt(discriminant);
            desiredDir = targetDir.scale(k).subtract(missileVel);
        }
        Vec2 rot = VectorUtil.vecToRot(desiredDir);
        float targetXRot = rot.x;
        float targetYRot = rot.y;
        // 适配过载限制与空气动力学
        double dynamicPressureFactor = Math.min(1.0, (missileSpeed * missileSpeed) / (referenceSpeed * referenceSpeed));
        // 当前速度下物理能达到的最大过载
        double availableG = this.maxG * dynamicPressureFactor;
        // 向心加速度公式 a = v * omega  =>  omega = a / v (弧度/tick)
        double maxAccelLimit = availableG * PhysicsEngine.G;
        double maxOmega = maxAccelLimit / missileSpeed;
        double maxAnglePerTick = Math.toDegrees(maxOmega);
        // 点火后的逐步解锁机动
        int motorTick = tickCount - coldLaunchTimeTick;
        if (motorTick < 5) {
            maxAnglePerTick *= Math.pow((double) motorTick / 5.0, 2);
        }
        // 平滑施加角速度
        float curX = this.getXRot();
        float curY = this.getYRot();
        float deltaX = Mth.wrapDegrees(targetXRot - curX);
        float deltaY = Mth.wrapDegrees(targetYRot - curY);
        // 限制在最大转向角速度内
        deltaX = Mth.clamp(deltaX, (float)-maxAnglePerTick, (float)maxAnglePerTick);
        deltaY = Mth.clamp(deltaY, (float)-maxAnglePerTick, (float)maxAnglePerTick);
        this.setXRot(curX + deltaX);
        this.setYRot(curY + deltaY);
    }

}
