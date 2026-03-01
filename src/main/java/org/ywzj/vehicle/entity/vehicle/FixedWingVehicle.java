package org.ywzj.vehicle.entity.vehicle;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.ywzj.vehicle.api.animation.IAnimationEntity;
import org.ywzj.vehicle.api.animation.IAnimationInstance;
import org.ywzj.vehicle.audio.VehicleSound;
import org.ywzj.vehicle.client.render.animation.context.FixedWingVehicleContext;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.client.resource.vehicle.BaseDisplay;
import org.ywzj.vehicle.client.resource.vehicle.FixedWingVehicleDisplay;
import org.ywzj.vehicle.network.message.ClientVehicleAction;
import org.ywzj.vehicle.util.DebugUtil;
import org.ywzj.vehicle.util.VectorUtil;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.parts.LandingGearUnit;
import org.ywzj.vehicle.vehicle.parts.PartUnit;
import org.ywzj.vehicle.vehicle.parts.SwitchableUnit;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;
import org.ywzj.vehicle.vehicle.pojo.AimContext;

import java.util.List;
import java.util.Optional;

public class FixedWingVehicle extends AbstractVehicle
        implements IAnimationEntity<FixedWingVehicle, FixedWingVehicleContext> {

    public static final EntityDataAccessor<Float> THROTTLE_LEVEL = SynchedEntityData.defineId(FixedWingVehicle.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> PITCH_INPUT = SynchedEntityData.defineId(FixedWingVehicle.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> ROLL_INPUT = SynchedEntityData.defineId(FixedWingVehicle.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> YAW_INPUT = SynchedEntityData.defineId(FixedWingVehicle.class, EntityDataSerializers.FLOAT);


    public float thrust = physicsEngine.gravityA * physicsEngine.mass;
    public float xRotSpeedAcceleration = 1f;
    public float xRotSpeedMax = 4;
    public float yRotSpeedAcceleration = 1;
    public float yRotSpeedMax = 4;
    public float zRotSpeedAcceleration = 1;
    public float zRotSpeedMax = 4;
    public float maxAirSpeed = 1f;
    public float xRotSpeed;
    public float yRotSpeed;
    public float zRotSpeed;


    public Vec3 airSpeed = new Vec3(0, 0, 0);
    public long lastRenderTime;
    public String landingGearPartId = "landing_gear";
    public LandingGearUnit landingGear;
    public boolean lastLandingGearState = false;
    private VehicleSound engineStartSoundInstance;
    private VehicleSound engineStopSoundInstance;
    private VehicleSound engineRunSoundInstance;
    private VehicleSound engineThrustSoundInstance;
    private VehicleSound passbySoundInstance;
    private IAnimationInstance<FixedWingVehicleContext> animationInstance;

    public FixedWingVehicle(EntityType<? extends AbstractVehicle> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.physicsEngine.lockCenterRot = true;
        this.physicsEngine.friction = 0;
    }

    @Override
    public IAnimationInstance<FixedWingVehicleContext> getAnimationInstance() {
        return animationInstance;
    }

    @Override
    public void initDisplayData(BaseDisplay display) {
        if (display instanceof FixedWingVehicleDisplay fixedWingVehicleDisplay) {
            this.animationInstance = fixedWingVehicleDisplay.createAnimationInstance(this);
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(THROTTLE_LEVEL, 0f);
        this.entityData.define(PITCH_INPUT, 0f);
        this.entityData.define(ROLL_INPUT, 0f);
        this.entityData.define(YAW_INPUT, 0f);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putFloat("ThrottleLevel", getThrottleLevel());
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("ThrottleLevel")) {
            entityData.set(THROTTLE_LEVEL, Mth.clamp(compound.getFloat("ThrottleLevel"), 0, 100));
        }
        if (this.landingGear != null) {
            onLandingGearUpdate(this.landingGear, isLandingGearDown());
        }
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        super.writeSpawnData(buffer);
        buffer.writeBoolean(isLandingGearDown());
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buffer) {
        super.readSpawnData(buffer);
        if (this.landingGear != null) {
            onLandingGearUpdate(this.landingGear, isLandingGearDown());
        }
    }

    @Override
    public void initData() {
        super.initData();
        PartUnit<?> landingGearUnit = partUnitMap.get(this.landingGearPartId);
        if (landingGearUnit instanceof LandingGearUnit switchableUnit) {
            this.landingGear = switchableUnit;
            this.landingGear.setOnStateChange(this::onLandingGearUpdate);
        }
    }

    public void onLandingGearUpdate(LandingGearUnit part, boolean newState) {
        if (newState != lastLandingGearState) {
            double maxHeight = part.getMaxHeight();
            if (newState) {
                mainCubeOBB.height += maxHeight;
                mainCubeOBB.y -= maxHeight;
            } else {
                mainCubeOBB.height -= maxHeight;
                mainCubeOBB.y += maxHeight;
            }
            mainCubeOBB.rebuild();
        }

        this.lastLandingGearState = newState;
    }

    @Nullable
    public LandingGearUnit getLandingGearUnit() {
        return landingGear;
    }

    @Override
    public void onClientVehicleAction(ClientVehicleAction message, Player player) {
        if (message.toggleLandingGear) {
            SwitchableUnit<?> landingGearUnit = this.getLandingGearUnit();
            if (landingGearUnit == null) {
                player.displayClientMessage(Component.translatable("tips.no_landing_gear"), true);
            } else if (hasPower()) {
                boolean landingGearDown = !isLandingGearDown();
                landingGearUnit.setOn(landingGearDown);
            }
        }
        super.onClientVehicleAction(message, player);
    }

    @Override
    public void shoot(int partUnitIndex, int weaponIndex, List<AimContext> aimContexts, @Nullable LivingEntity operator) {
        if (partUnits.get(partUnitIndex) instanceof WeaponUnit weaponUnit) {
            weaponUnit.shoot(weaponIndex, aimContexts, operator);
        }
    }

    public SoundEvent getEngineThrustSound() {
        Optional<BaseDisplay> displayOptional = ClientAssetsManager.INSTANCE.getVehicleDisplay(getDisplayId());
        return displayOptional.map(display -> display.getSoundEvents().get("engine_thrust")).orElse(null);
    }

    public SoundEvent getEnginePassbySound() {
        Optional<BaseDisplay> displayOptional = ClientAssetsManager.INSTANCE.getVehicleDisplay(getDisplayId());
        return displayOptional.map(display -> display.getSoundEvents().get("passby")).orElse(null);
    }

    @Override
    protected void tickSound() {
        super.tickSound();
        float engineSpeed = getPower();
        if (engineSpeed == 0) {
            if (engineRunSoundInstance != null) {
                engineRunSoundInstance.stop();
                engineRunSoundInstance = null;
            }
            if (engineStartSoundInstance != null) {
                engineStartSoundInstance.stop();
                engineStartSoundInstance = null;
            }
            if (engineThrustSoundInstance != null) {
                engineThrustSoundInstance.stop();
                engineThrustSoundInstance = null;
            }
            return;
        }
        if (engineSpeed < 50 && engineRunSoundInstance != null && engineStopSoundInstance == null) {
            SoundEvent engineStopSound = getEngineStopSound();
            if (engineStopSound != null) {
                engineStopSoundInstance = new VehicleSound(engineStopSound, 1f, viewInfo.soundDistance, 1f, false, 50, true, true, this.getId());
                engineStopSoundInstance.play();
            }
            if (engineStartSoundInstance != null) {
                engineStartSoundInstance.setVolume(engineSpeed / 100);
            }
        }
        if (engineSpeed > 0) {
            if (engineSpeed > 50 && engineStopSoundInstance != null) {
                engineStopSoundInstance = null;
            }
            if (engineSpeed < 20 && engineStartSoundInstance == null) {
                SoundEvent engineStartSound = getEngineStartSound();
                if (engineStartSound != null) {
                    engineStartSoundInstance = new VehicleSound(engineStartSound, 1f, viewInfo.soundDistance, 1f, false, 0, false, false, this.getId());
                    engineStartSoundInstance.play();
                }
            }
            if (engineSpeed > 50 && engineRunSoundInstance == null) {
                SoundEvent engineRunSound = getEngineRunSound();
                if (engineRunSound != null) {
                    engineRunSoundInstance = new VehicleSound(engineRunSound, 1f, viewInfo.soundDistance, 0.8f, true, 50, true, true, this.getId());
                    engineRunSoundInstance.play();
                }
            }
            if (engineRunSoundInstance != null) {
                engineRunSoundInstance.setPitch(Math.max(0.8f, 0.8f + 0.2f * engineSpeed / 100));
            }
            if (getThrottleLevel() > 100 && engineThrustSoundInstance == null) {
                SoundEvent engineThrustSound = getEngineThrustSound();
                if (engineThrustSound != null) {
                    engineThrustSoundInstance = new VehicleSound(engineThrustSound, 1f, viewInfo.soundDistance, 1f, true, 50, true, true, this.getId());
                    engineThrustSoundInstance.play();
                }
            } else if (getThrottleLevel() <= 100 && engineThrustSoundInstance != null) {
                engineThrustSoundInstance.stop();
                engineThrustSoundInstance = null;
            }
        }
        if (airSpeed.length() > 1) {
            Player player = LocalVehiclePlayer.instance.getPlayer();
            if (player.getVehicle() != this) {
                if (passbySoundInstance == null && player.distanceTo(this) < 32) {
                    SoundEvent passbySound = getEnginePassbySound();
                    if (passbySound != null) {
                        passbySoundInstance = new VehicleSound(passbySound, 1f, viewInfo.soundDistance, 1f, false, 0, false, false, this.getId());
                        passbySoundInstance.play();
                    }
                } else if (passbySoundInstance != null && player.distanceTo(this) > 64) {
                    passbySoundInstance = null;
                }
            }
        }
    }

    @Override
    protected Vec3 tickMove() {

        DebugUtil.particle(level(), position().add(getDeltaMovement().normalize().scale(12)));

        // 三个正交轴
        Vector3f[] axes = mainCubeOBB.obb().getAxes();
        Vec3 forwardDirection = new Vec3(axes[2]);
        Vec3 upDirection = new Vec3(axes[1]);
        Vec3 leftDirection = new Vec3(axes[0]);
        // 节流阀
        float throttleLevel = getThrottleLevel();
        if (controlUnit.forward || controlUnit.backward) {
            throttleLevel += controlUnit.forward ? 5 : -5;
        }
        entityData.set(THROTTLE_LEVEL, Mth.clamp(throttleLevel, 0f, 110f));
        // 三个杆量
        float xRotInput = getPitchInput();
        float yRotInput = getYawInput();
        float zRotInput = getRollInput();
        float xRotInputStep = 0.2f;
        float yRotInputStep = 0.5f;
        float zRotInputStep = 0.2f;
        // 俯仰偏航滚转输入
        if (controlUnit.up || controlUnit.down) {
            xRotInput += (controlUnit.up ? -1 : 1) * xRotInputStep;
            xRotInput = Math.signum(xRotInput) * Math.min(1, Math.abs(xRotInput));
        }
        if (controlUnit.leftYaw || controlUnit.rightYaw) {
            yRotInput += (controlUnit.leftYaw ? 1 : -1) * yRotInputStep;
            yRotInput = Math.signum(yRotInput) * Math.min(1, Math.abs(yRotInput));
        }
        if (controlUnit.left || controlUnit.right) {
            zRotInput += (controlUnit.left ? -1 : 1) * zRotInputStep;
            zRotInput = Math.signum(zRotInput) * Math.min(1, Math.abs(zRotInput));
        }
        // 鼠标瞄准
        if (getDriver() != null) {
            double xRotDiff = controlUnit.xRot - this.getXRot();
            double yRotDiff = Mth.wrapDegrees(controlUnit.yRot - this.getYRot());
            if (!(controlUnit.up || controlUnit.down)) {
                xRotInput = (float) (Math.signum(xRotDiff) * Math.min(1, Math.abs(xRotDiff) / 16));
            }
            if (!(controlUnit.leftYaw || controlUnit.rightYaw)) {
                yRotInput = (float) (Math.signum(-yRotDiff) * Math.min(1, Math.abs(yRotDiff) / 4));
            }
            if (!(controlUnit.left || controlUnit.right)) {
                float zRot = getZRot();
                // 滚转自动回正
                if (Math.abs(yRotDiff) <= 5) {
                    zRotInput = (-Math.signum(zRot) * Math.min(1, Math.abs(zRot) / 128));
                } else {
                    // 滚转倾向目标位置
                    if (Math.abs(yRotDiff) > 5 && (yRotDiff < 0 && zRot > yRotDiff / 2 || yRotDiff > 0 && zRot < yRotDiff / 2)) {
                        zRotInput = (float) (Math.signum(yRotDiff) * Math.min(1, Math.abs(yRotDiff) / 8));
                    } else if (zRotInput != 0) {
                        zRotInput *= 0.8f;
                        if (Math.abs(zRotInput) < 0.1) {
                            zRotInput = 0;
                        }
                    }
                }
            }
        }
        // 空速
        Vec3 airSpeed = getDeltaMovement();
        // 地面航行
        if (onGround()) {
            double al = airSpeed.length();
            if (controlUnit.leftYaw || controlUnit.rightYaw) {
                float k = (float) (al / 1.4);
                setYRot(getYRot() + (controlUnit.leftYaw ? -k : k));
                forwardDirection = getLookAngle();
                airSpeed = forwardDirection.scale(al * 0.98);
            }
            if (controlUnit.backward) {
                airSpeed = airSpeed.normalize().scale(al * 0.98);
            }
        }
        double thrust = 0.02; // 推力
        thrust *= throttleLevel / 100 * getPower() / 100;
        // 推力加速度
        double a = thrust / physicsEngine.mass;
        airSpeed = airSpeed.add(forwardDirection.scale(a));
        // 迎角
        double angelX = VectorUtil.angleBetween(airSpeed, upDirection) - Math.PI / 2;
        // 空气阻力
        double kMin = 1d / 500; // 基础阻力系数
        double kMax = 8d / 500; // 最大阻力系数
        double kuf = 10; // 升阻比
        double k = Math.abs(Math.sin(angelX));
        double al = airSpeed.length();
        double f = al * al * ((kMax - kMin) * k + kMin);
        airSpeed = airSpeed.normalize().scale(al - f / physicsEngine.mass);
        // 升力
        double degreeX = Math.toDegrees(angelX);
        if (degreeX > -5 && degreeX < 25) { // 迎角有效区间
            double fu = f * (kuf + 2 * degreeX / 25); // 迎角额外升力
            airSpeed = airSpeed.add(upDirection.scale(fu / physicsEngine.mass));
        }
        // 尾舵力
        double angelY = VectorUtil.angleBetween(airSpeed, leftDirection) - Math.PI / 2;
        double at = airSpeed.dot(forwardDirection);
        double fl = at * at * 8 * ((kMax - kMin) * k + kMin) * Math.sin(angelY);
        airSpeed = airSpeed.add(leftDirection.scale(fl / physicsEngine.mass));
        al = airSpeed.length();
        Quaternionf q = rotYXZ();
        // 气动影响转动
        double ke = al / 2.5; // 空速能量
        // 滚转
        if (zRotInput != 0) {
            double d = Math.min(8, ke * 8);
            q.rotateZ((float) Math.toRadians(zRotInput * d));
        }
        // 偏航
        if (yRotInput != 0) {
            double d0 = Math.min(3, ke * 3);
            double d1 = yRotInput * d0;
            double d2 = Math.toDegrees(VectorUtil.angleBetween(airSpeed, leftDirection) - Math.PI / 2);
            double d3 = Math.min(1, 2 / Math.abs(d2));
            q.rotateY((float) Math.toRadians(d1 * d3));
        } else {
            // 无输入时尾舵使得自动回正
            double d = VectorUtil.angleBetween(airSpeed, leftDirection) - Math.PI / 2;
            q.rotateY((float) (ke * -d / 5));
        }
        // 俯仰
        if (xRotInput != 0) {
            double d = Math.min(3, ke * 3);
            q.rotateX((float) Math.toRadians(xRotInput * d));
        }
        Vector3f rot = new Vector3f();
        q.getEulerAnglesYXZ(rot);
        setXRot((float) Math.toDegrees(rot.x));
        setYRot((float) Math.toDegrees(-rot.y));
        setZRot((float) Math.toDegrees(rot.z));

//        airSpeed = airSpeed.normalize().scale(Math.min(8, airSpeed.length()));
        setDeltaMovement(airSpeed);
        setPitchInput(xRotInput);
        setYawInput(yRotInput);
        setRollInput(zRotInput);
        return Vec3.ZERO;
    }

    @Override
    protected void tickParticle() {
        super.tickParticle();
        // 飞行扬尘效果
//        if (getPower() > 30 && tickCount % 2 == 0) {
//            // 获取当前位置并从下方开始查找第一个实心方块
//            BlockPos basePos = null;
//            for (int y = 1; y <= 32; y++) {
//                BlockPos checkPos = this.blockPosition().below(y);
//                if (!level().getBlockState(checkPos).isAir()) {
//                    basePos = checkPos; // 找到第一个实心方块
//                    break;
//                }
//            }
//            if (basePos != null) {
//                double radius = (double) tickCount % 20 / 20 * 10;
//                if (radius > 0 && radius < mainCubeOBB.depth * 1.3f) {
//                    int pointCount = 8; // 生成的粒子数量
//                    int particleCount = 2; // 生成的粒子数量
//                    for (int i = 0; i < pointCount; i++) {
//                        for (int j = 0; j < particleCount; j++) {
//                            double bias = ((2 * Math.PI) / pointCount) * random.nextDouble();
//                            double angle = (i * 2 * Math.PI) / pointCount;
//                            double xOffset = radius * Math.cos(angle + bias) + random.nextDouble() * 0.5;
//                            double zOffset = radius * Math.sin(angle + bias) + random.nextDouble() * 0.5;
//                            Vec3 particlePos = new Vec3(basePos.getX() + xOffset, basePos.getY() + 1 + random.nextDouble() * 1, basePos.getZ() + zOffset);
//                            level().addParticle(new DustParticleOptions(new Vector3f(1.0F, 1.0F, 1.0F), 3.0F), true, particlePos.x, particlePos.y, particlePos.z, 0, 0, 0);
//                        }
//                    }
//                }
//            }
//        }
        // 引擎烟
        if (hasPower()) {
            float engineSpeed = getPower();
            float throttlelevel = getThrottleLevel();
            if ((engineSpeed > 0 && engineParticleTick > Mth.clamp(10 - throttlelevel / 10, 3, 10))) {
                energyInfo.engineParticleOffsets.forEach(offset -> {
                    Vec3 engineSmokePos = this.position().add(offset);
                    engineSmokePos = relativeRotPos(engineSmokePos, false);
                    Vec3 engineSmokeVelocity = this.getLookAngle().normalize().scale(-0.3);
                    level().addParticle(ParticleTypes.LARGE_SMOKE, true,
                            engineSmokePos.x, engineSmokePos.y, engineSmokePos.z,
                            engineSmokeVelocity.x, engineSmokeVelocity.y, engineSmokeVelocity.z);
                });
                engineParticleTick = 0;
            } else {
                engineParticleTick += 1;
            }
        }
    }

    public float getThrottleLevel() {
        return this.entityData.get(THROTTLE_LEVEL);
    }

    public void setThrottleLevel(float value) {
        this.entityData.set(THROTTLE_LEVEL, value);
    }

    public float getPitchInput() {
        return this.entityData.get(PITCH_INPUT);
    }

    public void setPitchInput(float value) {
        this.entityData.set(PITCH_INPUT, value);
    }

    public float getRollInput() {
        return this.entityData.get(ROLL_INPUT);
    }

    public void setRollInput(float value) {
        this.entityData.set(ROLL_INPUT, value);
    }

    public float getYawInput() {
        return this.entityData.get(YAW_INPUT);
    }

    public void setYawInput(float value) {
        this.entityData.set(YAW_INPUT, value);
    }

    public boolean isLandingGearDown() {
        var landingGearUnit = this.getLandingGearUnit();
        return landingGearUnit != null && landingGearUnit.isOn();
    }

}
