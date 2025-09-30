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
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.ywzj.vehicle.audio.VehicleSound;
import org.ywzj.vehicle.network.Channel;
import org.ywzj.vehicle.network.message.ClientWeaponUnitControl;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.WeaponUnit;

public abstract class HelicopterVehicle extends AbstractVehicle {

    public static final EntityDataAccessor<Integer> ENGINE_SPEED = SynchedEntityData.defineId(HelicopterVehicle.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> COLLECTIVE_PITCH = SynchedEntityData.defineId(HelicopterVehicle.class, EntityDataSerializers.INT);
    public float xRotSpeed = 4;
    public float yRotSpeed = 4;
    public float zRotSpeed = 4;
    public Vec3 airSpeed = new Vec3(0, 0, 0);
    public float maxAirSpeed = 0.7f;
    public float propellerRotation;
    public long lastRenderTime;
    private VehicleSound engineStartSoundInstance;
    private VehicleSound engineStopSoundInstance;
    private VehicleSound engineRunSoundInstance;

    public HelicopterVehicle(EntityType<? extends Mob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.physicsEngine.lockCenterRot = true;
    }

    @Override
    public int getSeats() {
        return 2;
    }

    public abstract SoundEvent getEngineStartSound();

    public abstract SoundEvent getEngineStopSound();

    public abstract SoundEvent getEngineRunSound();

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ENGINE_SPEED, 0);
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
    protected void tickAim() {
        if (getOwnOperatorUnit(LocalVehiclePlayer.instance.getPlayer()) instanceof WeaponUnit weaponUnit) {
            Vec2 rot = null;
            if (LocalVehiclePlayer.instance.viewType == LocalVehiclePlayer.ViewType.DEFAULT) {
                rot = LocalVehiclePlayer.instance.cameraToWeaponRot();
            } else if (LocalVehiclePlayer.instance.viewType == LocalVehiclePlayer.ViewType.SCOPE) {
                rot = LocalVehiclePlayer.instance.scopeAimRot();
            }
            if (rot == null) {
                return;
            }
            if (weaponUnit.xAimRot != rot.x || weaponUnit.yAimRot != rot.y) {
                weaponUnit.xAimRot = rot.x;
                weaponUnit.yAimRot = rot.y;
                ClientWeaponUnitControl control = new ClientWeaponUnitControl();
                control.vehicleEntityId = this.getId();
                control.weaponIndex = weaponUnit.getIndex();
                control.xAimRot = rot.x;
                control.yAimRot = rot.y;
                Channel.CHANNEL.sendToServer(control);
            }
        }
    }

    @Override
    protected void tickSound() {
        int engineSpeed = getEngineSpeed();
        if (engineSpeed < 80 && engineRunSoundInstance != null && engineStopSoundInstance == null) {
            engineStopSoundInstance = new VehicleSound(getEngineStopSound(), 1f, 1f, false, 50, true, true, this.getId());
            engineStopSoundInstance.play();
            if (engineStartSoundInstance != null) {
                engineStartSoundInstance.setVolume((float) engineSpeed / 100);
            }
        }
        if (engineSpeed == 0 && engineStartSoundInstance != null) {
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
                engineStartSoundInstance = new VehicleSound(getEngineStartSound(), 1f, 1f, false, 0, false, false, this.getId());
                engineStartSoundInstance.play();
            }
            if (engineSpeed > 80 && engineRunSoundInstance == null) {
                engineRunSoundInstance = new VehicleSound(getEngineRunSound(), 1f, 1f, true, 50, true, true, this.getId());
                engineRunSoundInstance.play();
            }
            if (engineRunSoundInstance != null) {
                engineRunSoundInstance.setVolume(Math.max(0.2f, (float) engineSpeed / 100));
            }
        }
    }

    @Override
    protected void tickMove() {
        // 转速变化
        int engineSpeed = getEngineSpeed();
        if (getDriver() != null && engineSpeed < 100) {
            engineSpeed += 1;
        } else if (getDriver() == null && engineSpeed > 0) {
            engineSpeed -= 1;
        }
        entityData.set(ENGINE_SPEED, engineSpeed);

        // 总距控制
        int collectivePitch = getCollectivePitch();
        if (controlUnit.up) {
            collectivePitch += 5;
        } else if (controlUnit.down) {
            collectivePitch -= 5;
        }
        entityData.set(COLLECTIVE_PITCH, Mth.clamp(collectivePitch, 0, 100));

        if (getDriver() == null) {
            controlUnit.reset();
        }

        // 螺旋桨方向的力产生加速度
        double scale = (double) (getEngineSpeed() / 100 * getCollectivePitch()) / 100;
        Vec3 force = relativeRotDirection(new Vec3(0, 1, 0), false)
                .scale(scale * (physicsEngine.gravityA + 0.01));
        airSpeed = getDeltaMovement();
        airSpeed = airSpeed.add(force);
        if (airSpeed.length() > maxAirSpeed) {
            airSpeed = airSpeed.scale(maxAirSpeed);
        }
        this.setDeltaMovement(airSpeed);

        if (force.length() > physicsEngine.gravityA / 3) {
            float xRotSpeed = (float) (this.xRotSpeed * scale);
            float yRotSpeed = (float) (this.yRotSpeed * scale);
            float zRotSpeed = (float) (this.zRotSpeed * scale);
            if (getDriver() != null) {
                LivingEntity driver = getDriver();
                float yDiff = Mth.wrapDegrees(driver.getYRot() - this.getYRot());
                if (Math.abs(yDiff) > yRotSpeed) {
                    this.setYRot(this.getYRot() + Math.signum(yDiff) * yRotSpeed);
                } else {
                    this.setYRot(driver.getYRot());
                }

                if (!(controlUnit.forward || controlUnit.backward)) {
                    float xDiff = Mth.wrapDegrees(driver.getXRot() - this.getXRot());
                    if (Math.abs(xDiff) > xRotSpeed) {
                        this.setXRot(this.getXRot() + Math.signum(xDiff) * xRotSpeed);
                    } else {
                        this.setXRot(driver.getXRot());
                    }
                } else {
                    if (controlUnit.forward) {
                        this.setXRot(this.getXRot() + xRotSpeed);
                    }
                    if (controlUnit.backward) {
                        this.setXRot(this.getXRot() - xRotSpeed);
                    }
                }

                if (!(controlUnit.left || controlUnit.right)) {
                    float zDiff = Mth.wrapDegrees(-this.getZRot());
                    if (Math.abs(zDiff) > zRotSpeed) {
                        this.setZRot(this.getZRot() + Math.signum(zDiff) * zRotSpeed);
                    } else {
                        this.setZRot(0);
                    }
                } else {
                    if (controlUnit.left) {
                        this.setZRot(this.getZRot() - zRotSpeed);
                    }
                    if (controlUnit.right) {
                        this.setZRot(this.getZRot() + zRotSpeed);
                    }
                }
            }
        }
    }

    @Override
    protected void tickParticle() {
        // 飞行扬尘效果
        if (getEngineSpeed() > 30 && tickCount % 2 == 0) {
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
                if (radius > 0) {
                    int pointCount = 8; // 生成的粒子数量
                    int particleCount = 2; // 生成的粒子数量
                    for (int i = 0; i < pointCount; i++) {
                        for (int j = 0; j < particleCount; j++) {
                            double bias = ((2 * Math.PI) / pointCount) * random.nextDouble();
                            double angle = (i * 2 * Math.PI) / pointCount;
                            double xOffset = radius * Math.cos(angle + bias) + random.nextDouble() * 0.5;
                            double zOffset = radius * Math.sin(angle + bias) + random.nextDouble() * 0.5;
                            Vec3 particlePos = new Vec3(basePos.getX() + xOffset, basePos.getY() + 2 + random.nextDouble() * 1, basePos.getZ() + zOffset);
                            level().addParticle(new DustParticleOptions(new Vector3f(1.0F, 1.0F, 1.0F), 3.0F), true, particlePos.x, particlePos.y, particlePos.z, 0, 0, 0);
                        }
                    }
                }
            }
        }
    }

    public int getEngineSpeed() {
        return entityData.get(ENGINE_SPEED);
    }

    public int getCollectivePitch() {
        return entityData.get(COLLECTIVE_PITCH);
    }

}
