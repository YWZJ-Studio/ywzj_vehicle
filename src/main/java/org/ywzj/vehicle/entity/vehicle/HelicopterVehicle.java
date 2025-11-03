package org.ywzj.vehicle.entity.vehicle;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.ywzj.vehicle.audio.VehicleSound;
import org.ywzj.vehicle.util.VectorUtil;

public abstract class HelicopterVehicle extends AbstractVehicle {

    public static final EntityDataAccessor<Integer> COLLECTIVE_PITCH = SynchedEntityData.defineId(HelicopterVehicle.class, EntityDataSerializers.INT);
    public float mainRotorForce = 1.4f * physicsEngine.gravityA * physicsEngine.mass;
    public float xRotSpeed;
    public float xRotSpeedAcceleration = 1f;
    public float xRotSpeedMax = 4;
    public float yRotSpeed;
    public float yRotSpeedAcceleration = 1;
    public float yRotSpeedMax = 4;
    public float zRotSpeed;
    public float zRotSpeedAcceleration = 1;
    public float zRotSpeedMax = 4;
    public Vec3 airSpeed = new Vec3(0, 0, 0);
    public float maxAirSpeed = 1f;
    public float propellerRotation;
    public long lastRenderTime;
    private VehicleSound engineStartSoundInstance;
    private VehicleSound engineStopSoundInstance;
    private VehicleSound engineRunSoundInstance;

    public HelicopterVehicle(EntityType<? extends Mob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.thirdPersonCenterOffset = new Vec3(0, 6, 0);
        this.thirdPersonDistance = 14;
        this.soundDistance = 8;
        this.fuelCapacity = 0.25f;
        this.physicsEngine.lockCenterRot = true;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(COLLECTIVE_PITCH, 0);
    }

    @Override
    public void onEnterVehicle(LivingEntity pPlayer) {
        super.onEnterVehicle(pPlayer);
        this.playSound(SoundEvents.IRON_TRAPDOOR_OPEN);
    }

    @Override
    public void onLeaveVehicle(LivingEntity entity) {
        super.onLeaveVehicle(entity);
        this.playSound(SoundEvents.IRON_TRAPDOOR_CLOSE);
    }

    @Override
    protected void tickSound() {
        float engineSpeed = getPower();
        if (engineSpeed < 80 && engineRunSoundInstance != null && engineStopSoundInstance == null) {
            SoundEvent engineStopSound = getEngineStopSound();
            if (engineStopSound != null) {
                engineStopSoundInstance = new VehicleSound(engineStopSound, soundDistance, 1f, false, 50, true, true, this.getId());
                engineStopSoundInstance.play();
            }
            if (engineStartSoundInstance != null) {
                engineStartSoundInstance.setVolume(engineSpeed / 100 * soundDistance);
            }
        }
        if (engineSpeed == 0) {
            if (engineRunSoundInstance != null) {
                engineRunSoundInstance.stop();
                engineRunSoundInstance = null;
            }
            if (engineStartSoundInstance != null) {
                engineStartSoundInstance.stop();
                engineStartSoundInstance = null;
            }
        } else if (engineSpeed > 0) {
            if (engineSpeed > 80 && engineStopSoundInstance != null) {
                engineStopSoundInstance = null;
            }
            if (engineStartSoundInstance == null) {
                SoundEvent engineStartSound = getEngineStartSound();
                if (engineStartSound != null) {
                    engineStartSoundInstance = new VehicleSound(engineStartSound, soundDistance, 1f, false, 0, false, false, this.getId());
                    engineStartSoundInstance.play();
                }
            }
            if (engineSpeed > 80 && engineRunSoundInstance == null) {
                SoundEvent engineRunSound = getEngineRunSound();
                if (engineRunSound != null) {
                    engineRunSoundInstance = new VehicleSound(engineRunSound, soundDistance, 1f, true, 50, true, true, this.getId());
                    engineRunSoundInstance.play();
                }
            }
            if (engineRunSoundInstance != null) {
                engineRunSoundInstance.setVolume(Math.max(0.2f * soundDistance, engineSpeed / 100 * soundDistance));
            }
        }
    }

    @Override
    protected Vec3 tickMove() {
        if (getDriver() == null) {
            controlUnit.reset();
        }

        // 总距控制
        int collectivePitch = getCollectivePitch();
        if (controlUnit.up) {
            collectivePitch += 5;
        } else if (controlUnit.down) {
            collectivePitch -= 5;
        }
        entityData.set(COLLECTIVE_PITCH, Mth.clamp(collectivePitch, 0, 100));

        airSpeed = getDeltaMovement();
        // 引擎转速与浆距得出力系数，该值越高，桨叶效率越高，则出力越高
        double scale = ((double) getPower() / 100) * ((double) getCollectivePitch() / 100);
        // 桨叶出力方向的当前载具速度分量，该值越高，桨叶效率越低，则出力越低
        Vec3 vP = relativeRotDirection(new Vec3(0, 1, 0), false);
        double dVV = VectorUtil.project(airSpeed, vP).length() * Math.signum(airSpeed.dot(vP));
        if (dVV > 0) {
            scale *= Math.min(1, 8 / (dVV * 20));
        } else if (dVV < 0) {
            scale *= Math.min(2, Math.max(1, -dVV / 0.05));
        }
        // 高度越高，空气越稀薄，发动机出力与桨叶效率都会降低，约定在64格高的海平面以下才可达到满效率
        double scaleAir = position().y < 64 ? 1 : Math.pow(255 - position().y, 0.2) / Math.pow(191, 0.2);
        // 螺旋桨方向的力
        Vec3 force = vP.scale(scale * scaleAir * mainRotorForce);
        // 桨叶水平方向的空速带来升力
        double dVH = Math.sqrt(Math.pow(airSpeed.length(), 2) - Math.pow(dVV, 2));
        force.add(vP.scale(dVH * scaleAir * 0.005f));
        airSpeed = airSpeed.add(force);
        if (airSpeed.length() >= maxAirSpeed) {
            airSpeed = airSpeed.normalize().scale(maxAirSpeed);
        }
        this.setDeltaMovement(airSpeed);

        float xRotSpeedAcceleration = (float) (this.xRotSpeedAcceleration * scale);
        float yRotSpeedAcceleration = (float) (this.yRotSpeedAcceleration * scale);
        float zRotSpeedAcceleration = (float) (this.zRotSpeedAcceleration * scale);
        if (getDriver() != null) {
            if (!(controlUnit.leftYaw || controlUnit.rightYaw)) {
                float yDiff = Mth.wrapDegrees(controlUnit.yRot - this.getYRot());
                float shrink = Math.min(1, Math.abs(yDiff) / yRotSpeedAcceleration);
                if (yDiff > 0) {
                    yRotSpeed = Math.min(yRotSpeedMax, yRotSpeed + yRotSpeedAcceleration * shrink);
                } else if (yDiff < 0) {
                    yRotSpeed = Math.max(-yRotSpeedMax, yRotSpeed - yRotSpeedAcceleration * shrink);
                }
                if (Math.abs(yDiff) > 3) {
                    if (Math.abs(yDiff) <= Math.abs(yRotSpeed)) {
                        yRotSpeed = (float) Mth.lerp(0.3, yRotSpeed, yDiff);
                    }
                    this.setYRot(this.getYRot() + yRotSpeed);
                } else {
                    yRotSpeed = 0;
                    this.setYRot(controlUnit.yRot);
                }
            } else {
                if (controlUnit.rightYaw) {
                    yRotSpeed = Math.min(yRotSpeedMax, yRotSpeed + yRotSpeedAcceleration);
                }
                if (controlUnit.leftYaw) {
                    yRotSpeed = Math.max(-yRotSpeedMax, yRotSpeed - yRotSpeedAcceleration);
                }
                this.setYRot(this.getYRot() + yRotSpeed);
            }

            if (!(controlUnit.forward || controlUnit.backward)) {
                float xDiff = Mth.wrapDegrees(controlUnit.xRot - this.getXRot());
                float shrink = Math.min(1, Math.abs(xDiff) / xRotSpeedAcceleration);
                if (xDiff > 0) {
                    xRotSpeed = Math.min(xRotSpeedMax, xRotSpeed + xRotSpeedAcceleration * shrink);
                } else if (xDiff < 0) {
                    xRotSpeed = Math.max(-xRotSpeedMax, xRotSpeed - xRotSpeedAcceleration * shrink);
                }
                if (Math.abs(xDiff) > 3) {
                    this.setXRot(this.getXRot() + xRotSpeed);
                } else {
                    xRotSpeed = 0;
                    this.setXRot(controlUnit.xRot);
                }
            } else {
                if (controlUnit.forward) {
                    xRotSpeed = Math.min(xRotSpeedMax, xRotSpeed + xRotSpeedAcceleration);
                }
                if (controlUnit.backward) {
                    xRotSpeed = Math.max(-xRotSpeedMax, xRotSpeed - xRotSpeedAcceleration);
                }
                this.setXRot(this.getXRot() + xRotSpeed);
            }

            if (!(controlUnit.left || controlUnit.right)) {
                float zDiff = Mth.wrapDegrees(-this.getZRot());
                float shrink = Math.min(1, Math.abs(zDiff) / zRotSpeedAcceleration);
                if (zDiff > 0) {
                    zRotSpeed = Math.min(zRotSpeedMax, zRotSpeed + zRotSpeedAcceleration * shrink);
                } else if (zDiff < 0) {
                    zRotSpeed = Math.max(-zRotSpeedMax, zRotSpeed - zRotSpeedAcceleration * shrink);
                }
                if (Math.abs(zDiff) > 3) {
                    this.setZRot(this.getZRot() + zRotSpeed);
                } else {
                    zRotSpeed = 0;
                    this.setZRot(0);
                }
            } else {
                if (controlUnit.right) {
                    zRotSpeed = Math.min(zRotSpeedMax, zRotSpeed + zRotSpeedAcceleration);
                }
                if (controlUnit.left) {
                    zRotSpeed = Math.max(-zRotSpeedMax, zRotSpeed - zRotSpeedAcceleration);
                }
                this.setZRot(this.getZRot() + zRotSpeed);
            }
            yRotSpeed = Math.signum(yRotSpeed) * (Math.max(0, Math.abs(yRotSpeed) - 0.1f));
            xRotSpeed = Math.signum(xRotSpeed) * (Math.max(0, Math.abs(xRotSpeed) - 0.1f));
            zRotSpeed = Math.signum(zRotSpeed) * (Math.max(0, Math.abs(zRotSpeed) - 0.1f));
        }
        return force;
    }

    @Override
    protected void tickParticle() {
        // 飞行扬尘效果
        if (getPower() > 30 && tickCount % 2 == 0) {
            // 获取当前位置并从下方开始查找第一个实心方块
            BlockPos basePos = null;
            for (int y = 1; y <= 32; y++) {
                BlockPos checkPos = this.blockPosition().below(y);
                if (!level().getBlockState(checkPos).isAir()) {
                    basePos = checkPos; // 找到第一个实心方块
                    break;
                }
            }
            if (basePos != null) {
                double radius = (double) tickCount % 20 / 20 * 10;
                if (radius > 0 && radius < mainCubeOBB.depth * 1.3f) {
                    int pointCount = 8; // 生成的粒子数量
                    int particleCount = 2; // 生成的粒子数量
                    for (int i = 0; i < pointCount; i++) {
                        for (int j = 0; j < particleCount; j++) {
                            double bias = ((2 * Math.PI) / pointCount) * random.nextDouble();
                            double angle = (i * 2 * Math.PI) / pointCount;
                            double xOffset = radius * Math.cos(angle + bias) + random.nextDouble() * 0.5;
                            double zOffset = radius * Math.sin(angle + bias) + random.nextDouble() * 0.5;
                            Vec3 particlePos = new Vec3(basePos.getX() + xOffset, basePos.getY() + 1 + random.nextDouble() * 1, basePos.getZ() + zOffset);
                            level().addParticle(new DustParticleOptions(new Vector3f(1.0F, 1.0F, 1.0F), 3.0F), true, particlePos.x, particlePos.y, particlePos.z, 0, 0, 0);
                        }
                    }
                }
            }
        }
    }

    public int getCollectivePitch() {
        return entityData.get(COLLECTIVE_PITCH);
    }

}
