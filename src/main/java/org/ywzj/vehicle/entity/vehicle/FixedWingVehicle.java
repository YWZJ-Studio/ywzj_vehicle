package org.ywzj.vehicle.entity.vehicle;

import net.minecraft.core.particles.DustParticleOptions;
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
import org.ywzj.vehicle.all.AllDamageTypes;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.api.animation.IAnimationEntity;
import org.ywzj.vehicle.api.animation.IAnimationInstance;
import org.ywzj.vehicle.audio.VehicleSound;
import org.ywzj.vehicle.capability.VehicleCapabilityProvider;
import org.ywzj.vehicle.client.render.animation.context.FixedWingVehicleContext;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.client.resource.vehicle.BaseDisplay;
import org.ywzj.vehicle.client.resource.vehicle.FixedWingVehicleDisplay;
import org.ywzj.vehicle.network.message.ClientVehicleAction;
import org.ywzj.vehicle.particle.SmokeCloudOption;
import org.ywzj.vehicle.util.VectorUtil;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.part.*;
import org.ywzj.vehicle.vehicle.pojo.AimContext;
import org.ywzj.vehicle.vehicle.structure.VehicleCubeOBB;

import java.util.List;
import java.util.Optional;

public class FixedWingVehicle extends AbstractVehicle
        implements IAnimationEntity<FixedWingVehicle, FixedWingVehicleContext> {

    public static final EntityDataAccessor<Float> THROTTLE_LEVEL = SynchedEntityData.defineId(FixedWingVehicle.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> PITCH_INPUT = SynchedEntityData.defineId(FixedWingVehicle.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> YAW_INPUT = SynchedEntityData.defineId(FixedWingVehicle.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> ROLL_INPUT = SynchedEntityData.defineId(FixedWingVehicle.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Boolean> AEROBATIC_SMOKE_ON = SynchedEntityData.defineId(FixedWingVehicle.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Integer> AEROBATIC_SMOKE_R = SynchedEntityData.defineId(FixedWingVehicle.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> AEROBATIC_SMOKE_G = SynchedEntityData.defineId(FixedWingVehicle.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> AEROBATIC_SMOKE_B = SynchedEntityData.defineId(FixedWingVehicle.class, EntityDataSerializers.INT);
    public VehicleCubeOBB aerodynamicCubeOBB;
    public float thrust = 0.02f;
    public float thrustK = 1.5f;
    public float ceiling = 512;
    public float xRotInputStep = 0.2f;
    public float yRotInputStep = 0.5f;
    public float zRotInputStep = 0.2f;
    public float airDragKMin = 1f / 500;
    public float airDragKMax = 4f / 500;
    public float liftToDragK = 6;
    public float angleOfAttackMin = -10f;
    public float angleOfAttackMax = 25f;
    public float xRotInputDragK = 1f;
    public float yRotInputDragK = 1f / 4;
    public float zRotInputDragK = 1f / 8;
    public float turnRateBySpeed = 1f / 2.5f;
    public float xTurnRate = 2;
    public float yTurnRate = 3;
    public float zTurnRate = 8;
    public List<Vec3> vortexOffsets;
    public List<Vec3> aerobaticSmokeOffsets;
    public List<AfterburnerUnit> afterburnerUnits;
    public float throttleLevelO;
    public float throttleLevel;
    public float pitchInput;
    public float pitchInputO;
    public float yawInput;
    public float yawInputO;
    public float rollInput;
    public float rollInputO;
    public LandingGearUnit landingGear;
    public AirbrakeUnit airbrakeUnit;
    public ThrustUnit thrustUnit;
    private VehicleSound engineStartSoundInstance;
    private VehicleSound engineStopSoundInstance;
    private VehicleSound engineRunSoundInstance;
    private VehicleSound engineThrustSoundInstance;
    private VehicleSound landingSoundInstance;
    private VehicleSound aerobaticSmokeSoundInstance;
    private VehicleSound passbySoundInstance;
    private IAnimationInstance<FixedWingVehicleContext> animationInstance;

    public FixedWingVehicle(EntityType<? extends AbstractVehicle> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.driverXYRotControl = true;
        this.physicsEngine.lockCenterRot = true;
    }

    @Override
    public IAnimationInstance<FixedWingVehicleContext> getAnimationInstance() {
        return animationInstance;
    }

    @Override
    public void initDisplayData(BaseDisplay display) {
        super.initDisplayData(display);
        if (display instanceof FixedWingVehicleDisplay fixedWingVehicleDisplay) {
            this.animationInstance = fixedWingVehicleDisplay.createAnimationInstance(this);
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(THROTTLE_LEVEL, 0f);
        this.entityData.define(PITCH_INPUT, 0f);
        this.entityData.define(YAW_INPUT, 0f);
        this.entityData.define(ROLL_INPUT, 0f);
        this.entityData.define(AEROBATIC_SMOKE_ON, false);
        this.entityData.define(AEROBATIC_SMOKE_R, 179);
        this.entityData.define(AEROBATIC_SMOKE_G, 179);
        this.entityData.define(AEROBATIC_SMOKE_B, 179);
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
            setThrottleLevel(Mth.clamp(compound.getFloat("ThrottleLevel"), 0, 100));
        }
        if (landingGear != null) {
            landingGear.setOn(isLandingGearUp());
        }
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        super.writeSpawnData(buffer);
        buffer.writeBoolean(isLandingGearUp());
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buffer) {
        super.readSpawnData(buffer);
        if (landingGear != null) {
            landingGear.setOn(isLandingGearUp());
        }
    }

    @Override
    public void onClientVehicleAction(ClientVehicleAction message, Player player) {
        if (message.toggleLandingGear) {
            if (landingGear == null) {
                player.displayClientMessage(Component.translatable("tips.no_landing_gear"), true);
            } else if (hasPower()) {
                landingGear.setOn(!isLandingGearUp());
            }
        }
        if (message.toggleAirbrake) {
            if (airbrakeUnit == null) {
                player.displayClientMessage(Component.translatable("tips.no_airbrake"), true);
            } else if (hasPower()) {
                airbrakeUnit.setOn(!airbrakeUnit.isOn());
            }
        }
        if (message.toggleAerobaticSmoke) {
            setAerobaticSmokeOn(!isAerobaticSmokeOn());
            setAerobaticSmokeR(message.aerobaticSmokeR);
            setAerobaticSmokeG(message.aerobaticSmokeG);
            setAerobaticSmokeB(message.aerobaticSmokeB);
            if (isAerobaticSmokeOn()) {
                playVehicleSound(AllSounds.AEROBATICS_SMOKE_START.get(), true);
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
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            tickInput();
        } else {
            tickAfterburner();
        }
    }

    @Override
    public void updateOBBs() {
        super.updateOBBs();
        aerodynamicCubeOBB.update(this);
    }

    @Override
    protected void tickEnergy() {
        getCapability(VehicleCapabilityProvider.CAPABILITY).ifPresent(cap -> {
            float fuel = cap.getFuel();
            fuel = org.joml.Math.max(0, fuel - energyInfo.energyConsumptionPerTick * getPower() / 100 * getThrottleLevel() / 100);
            physicsEngine.mass = curbWeight + fuel;
            entityData.set(ENERGY, fuel);
            setEnergy(fuel);
        });
    }

    private void tickInput() {
        throttleLevelO = throttleLevel;
        pitchInputO = pitchInput;
        yawInputO = yawInput;
        rollInputO = rollInput;
        if (level().isClientSide()) {
            throttleLevel = getThrottleLevel();
            pitchInput = getPitchInput();
            yawInput = getYawInput();
            rollInput = getRollInput();
        }
    }

    private void tickAfterburner() {
        for (PartUnit<?> partUnit : partUnits) {
            if (partUnit instanceof AfterburnerUnit afterburner) {
                afterburner.setOn(hasPower() && getThrottleLevel() > 100);
            }
        }
    }

    @Override
    protected Vec3 tickMove() {
        // 三个正交轴
        Vector3f[] axes;
        boolean onGround = level().getBlockState(blockPosition().below()).isSolid();
        if (onGround) {
            axes = getMainCubeOBB().obb().getAxes();
        } else {
            axes = aerodynamicCubeOBB.obb().getAxes();
        }
        Vec3 forwardDirection = new Vec3(axes[2]);
        Vec3 upDirection = new Vec3(axes[1]);
        Vec3 leftDirection = new Vec3(axes[0]);
        // 节流阀
        float throttleLevel;
        if (isDestroyed()) {
            throttleLevel = 0;
        } else {
            throttleLevel = getThrottleLevel();
            if (controlUnit.forward || controlUnit.backward) {
                if (thrustK > 1) {
                    if (controlUnit.forward && throttleLevel + 5 > 100) {
                        throttleLevel = 100 * thrustK;
                    } else if (controlUnit.backward && throttleLevel > 100) {
                        throttleLevel = 100;
                    } else {
                        throttleLevel = Mth.clamp(throttleLevel + (controlUnit.forward ? 5 : -5), 0, 100);
                    }
                } else {
                    throttleLevel = Mth.clamp(throttleLevel + (controlUnit.forward ? 5 : -5), 0, 100);
                }
            }
        }
        setThrottleLevel(Math.max(0f, throttleLevel));
        // 矢量控制
        if (thrustUnit != null) {
            if ((controlUnit.functionalUp || controlUnit.functionalDown)) {
                if (controlUnit.functionalUp) {
                    thrustUnit.setXAimRot(Math.max(thrustUnit.getXAimRot() - 5, thrustUnit.getXRotMin()));
                } else {
                    thrustUnit.setXAimRot(Math.min(thrustUnit.getXAimRot() + 5, thrustUnit.getXRotMax()));
                }
                if (controlUnit.getOperator() instanceof Player player) {
                    player.displayClientMessage(Component.translatable("tips.thrust_vector", thrustUnit.getXAimRot()), true);
                }
            }
        }
        // 三个杆量
        tickInput();
        float xRotInput = pitchInput;
        float yRotInput = yawInput;
        float zRotInput = rollInput;
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
        if (getDriver() == null) {
            controlUnit.xRot = 0;
            controlUnit.yRot = getYRot();
        }
        Vec3 aimVec = VectorUtil.rotToVec(controlUnit.xRotKeep ? getXRot() : controlUnit.xRot, controlUnit.yRotKeep ? getYRot() : controlUnit.yRot);
        if (!(controlUnit.up || controlUnit.down)) {
            double xDiff = aimVec.dot(upDirection);
            xRotInput = (float) (Math.signum(-xDiff) * Math.min(1, Math.abs(xDiff) * xRotInputStep * 40));
        }
        if (!(controlUnit.leftYaw || controlUnit.rightYaw)) {
            double yDiff = aimVec.dot(leftDirection);
            yRotInput = (float) (Math.signum(yDiff) * Math.min(1, Math.abs(yDiff) * yRotInputStep * 40));
        }
        if (!(controlUnit.left || controlUnit.right)) {
            float yRotDiff = VectorUtil.vecToRot(forwardDirection).y - controlUnit.yRot;
            double yDiff = aimVec.dot(leftDirection);
            float zRot = getZRot();
            if (Math.abs(yRotDiff) > 5) {
                if (Math.abs(yDiff) <= 0.05) {
                    // 滚转保持
                    zRotInput = zRotInput / 4;
                } else {
                    // 滚转倾向目标位置
                    zRotInput = (float) (Math.signum(-yDiff) * Math.min(1, Math.abs(yDiff) * zRotInputStep * 4));
                }
            } else {
                // 滚转自动回正
                float toZRotInput = -Math.signum(zRot) * Math.min(1, Math.abs(zRot) / 128);
                float zRotInputDiff = toZRotInput - zRotInput;
                if (Math.abs(zRotInputDiff) < zRotInputStep) {
                    zRotInput = toZRotInput;
                } else {
                    zRotInput += Math.signum(zRotInputDiff) * zRotInputStep;
                }
            }
        }
        double mass = physicsEngine.mass;
        // 空速
        Vec3 airSpeed = getDeltaMovement();
        // 地面航行
        if (onGround()) {
            double al = airSpeed.length();
            if (controlUnit.leftYaw || controlUnit.rightYaw) {
                float k = (float) (al / 1.4);
                setYRot(getYRot() + (controlUnit.leftYaw ? -k : k));
                forwardDirection = getLookAngle();
                airSpeed = forwardDirection.scale(Math.max(0, al - 0.0001));
            }
            if (isLandingGearUp() && airSpeed.length() > 0.1) {
                airSpeed = airSpeed.normalize().scale(Math.max(0, al - 0.001));
                hurt(AllDamageTypes.Sources.vehicleCollision(level().registryAccess(), this, this.getDriver(), null), 1);
            } else if (controlUnit.backward) {
                airSpeed = airSpeed.normalize().scale(Math.max(0, al - 0.0001));
            }
        }
        float power = throttleLevel / 100 * getPower() / 100;
        float thrust = this.thrust * power;
        // 推力加速度
        double a = thrust / mass;
        Vec3 thrustDirection = onGround ? new Vec3(forwardDirection.x, 0, forwardDirection.z).normalize() : forwardDirection;
        if (thrustUnit != null) {
            thrustDirection = thrustUnit.worldVec();
        }
        airSpeed = airSpeed.add(thrustDirection.scale(a));
        // 迎角
        double angelX = VectorUtil.angleBetween(airSpeed, upDirection) - Math.PI / 2;
        // 空气阻力
        float scaleAir = position().y < 64 ? 1 : (float) (Math.pow(Math.max(0, ceiling - position().y), 0.5) / Math.pow(ceiling - 64, 0.5));
        scaleAir = (float) Math.max(0.001, scaleAir);
        double liftToDragK = this.liftToDragK * scaleAir;
        double k = ((airDragKMax - airDragKMin) * Math.abs(Math.sin(angelX)) + airDragKMin);
        double al = airSpeed.length();
        double f = al * al * k;
        airSpeed = airSpeed.normalize().scale(al - f / mass);
        // 升力
        double aRaw = airSpeed.length();
        double degreeX = Math.toDegrees(angelX);
        double fl;
        if (degreeX >= angleOfAttackMin && degreeX <= angleOfAttackMax) {
            // 升力区间
            fl = f * (liftToDragK + 2 * scaleAir * degreeX / angleOfAttackMax);
        } else {
            // 失速区间
            double exceed = (degreeX > angleOfAttackMax) ?
                    (degreeX - angleOfAttackMax) :
                    (angleOfAttackMin - degreeX);
            fl = f * liftToDragK * Math.exp(-1 * exceed);
        }
        Vec3 force = upDirection.scale(fl);
        if (fl != 0) {
            airSpeed = airSpeed.add(upDirection.scale(fl / mass));
        }
        // 尾舵力
        double angelY = VectorUtil.angleBetween(airSpeed, leftDirection) - Math.PI / 2;
        double at = airSpeed.dot(forwardDirection);
        double ft = at * at * 8 * k * Math.sin(angelY);
        airSpeed = airSpeed.add(leftDirection.scale(ft / mass));
        // 操控面与部件阻力
        double controlDrag = (Math.abs(xRotInput) * xRotInputDragK
                + Math.abs(yRotInput) * yRotInputDragK
                + Math.abs(zRotInput) * zRotInputDragK
                + (landingGear != null ? landingGear.level() * landingGear.getDragK() : 0)
                + (airbrakeUnit != null ? airbrakeUnit.level() * airbrakeUnit.getDragK() : 0)
        ) * airDragKMin;
        double fc = al * al * controlDrag;
        aRaw -= fc / mass;
        airSpeed = airSpeed.normalize().scale(aRaw);
        al = airSpeed.length();
        Quaternionf q = rotYXZ();
        // 气动影响转动
        double ke = scaleAir * al * turnRateBySpeed;
        // 滚转
        if (zRotInput != 0) {
            double d = Math.min(zTurnRate, ke * zTurnRate);
            q.rotateZ((float) Math.toRadians(zRotInput * d));
        }
        // 俯仰
        if (xRotInput != 0) {
            double d = Math.min(xTurnRate, ke * xTurnRate);
            q.rotateX((float) Math.toRadians(xRotInput * d));
        }
        // 偏航
        if (yRotInput != 0) {
            double d0 = Math.min(yTurnRate, ke * yTurnRate);
            double d1 = yRotInput * d0;
            double d2 = Math.toDegrees(VectorUtil.angleBetween(airSpeed, leftDirection) - Math.PI / 2);
            double d3 = Math.min(1, 2 / Math.abs(d2));
            double r1 = d1 * d3;
            if (thrustUnit != null) {
                r1 += yRotInput * yTurnRate / 2f * power * Math.abs(thrustUnit.worldRot().x - getXRot()) / 90;
            }
            q.rotateY((float) Math.toRadians(r1));
        } else {
            // 无输入时尾舵使得自动回正
            double d = VectorUtil.angleBetween(airSpeed, leftDirection) - Math.PI / 2;
            q.rotateY((float) (ke * -d / 5));
        }
        // 失速尾旋
        Vec3 downDirection = upDirection.scale(-1);
        if (VectorUtil.angleBetween(airSpeed, downDirection) < Math.PI / 4) {
            double vd = airSpeed.dot(downDirection);
            double kvd = Math.min(1.5, vd);
            q.rotateY((float) (Math.PI / 72 * kvd));
        }
        Vector3f rot = new Vector3f();
        q.getEulerAnglesYXZ(rot);
        setXRot((float) Math.toDegrees(rot.x));
        setYRot((float) Math.toDegrees(-rot.y));
        setZRot((float) Math.toDegrees(rot.z));
        setDeltaMovement(airSpeed);
        setPitchInput(xRotInput);
        setYawInput(yRotInput);
        setRollInput(zRotInput);
        return force;
    }

    @Override
    protected void tickSound() {
        super.tickSound();
        boolean onGround = level().getBlockState(blockPosition().below()).isSolid();
        double speed = getDeltaMovement().length();
        if (onGround) {
            if (speed > 0.05) {
                if (landingSoundInstance == null) {
                    landingSoundInstance = new VehicleSound(AllSounds.LANDING.get(), 1f, viewInfo.soundDistance, 1f, true, 50, false, true, this.getId());
                    landingSoundInstance.play();
                }
            } else if (landingSoundInstance != null) {
                landingSoundInstance.stop();
                landingSoundInstance = null;
            }
        } else if (landingSoundInstance != null) {
            landingSoundInstance.stop();
            landingSoundInstance = null;
        }
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
        if (speed > 1 && !onGround) {
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
        if (isAerobaticSmokeOn() && aerobaticSmokeSoundInstance == null) {
            aerobaticSmokeSoundInstance = new VehicleSound(AllSounds.AEROBATICS_SMOKE_LOOP.get(), 1f, viewInfo.soundDistance, 1f, true, 50, false, true, this.getId());
            aerobaticSmokeSoundInstance.play();
        } else if (!isAerobaticSmokeOn() && aerobaticSmokeSoundInstance != null) {
            aerobaticSmokeSoundInstance.stop();
            aerobaticSmokeSoundInstance = null;
        }
    }

    @Override
    protected void tickParticle() {
        super.tickParticle();
        Vec3 airSpeed = getDeltaMovement();
        if (airSpeed.length() > 1) {
            Vector3f[] axes = mainCubeOBB.obb().getAxes();
            double angelX = VectorUtil.angleBetween(airSpeed, new Vec3(axes[1])) - Math.PI / 2;
            double degreeX = Math.toDegrees(angelX);
            if (degreeX < angleOfAttackMin * 3f || degreeX > angleOfAttackMax * 0.9f) {
                vortexOffsets.forEach(offset -> {
                    Vec3 particlePos = relativeRotPos(position().add(offset), false);
                    level().addParticle(new DustParticleOptions(new Vector3f(1.0F, 1.0F, 1.0F), 3.0F),
                            true, particlePos.x, particlePos.y, particlePos.z,
                            0, 0, 0);
                });
            }
        }
        // 引擎烟
        if (hasPower()) {
            float engineSpeed = getPower();
            float throttlelevel = getThrottleLevel();
            if ((engineSpeed > 0 && engineParticleTick > Mth.clamp(10 - throttlelevel / 10, 3, 10))) {
                energyInfo.engineParticleOffsets.forEach(offset -> {
                    Vec3 engineSmokePos = this.position().add(offset);
                    engineSmokePos = relativeRotPos(engineSmokePos, false);
                    Vec3 engineSmokeVelocity = this.getLookAngle().normalize().scale(-0.3);
                    level().addParticle(new SmokeCloudOption(0.3f, 0.3f, 0.3f,
                                    0.0f, 0.0f, 0.0f, 0.7f,
                                    20, 0.3f, 0.4f, 0.005f), true,
                            engineSmokePos.x, engineSmokePos.y, engineSmokePos.z,
                            engineSmokeVelocity.x + (level().random.nextDouble() - 0.5) * 0.05,
                            engineSmokeVelocity.y + (level().random.nextDouble() - 0.5) * 0.05,
                            engineSmokeVelocity.z + (level().random.nextDouble() - 0.5) * 0.05);
                });
                engineParticleTick = 0;
            } else {
                engineParticleTick += 1;
            }
        }
        // 特技拉烟
        if (level().isClientSide() && isAerobaticSmokeOn() && aerobaticSmokeOffsets != null) {
            Vec3 vehiclePos = position();
            Vec3 vehiclePosO = new Vec3(xo, yo, zo);
            Vec3 step = vehiclePos.subtract(vehiclePosO);
            int segments = (int) (step.length());
            Vec3 dir = step.normalize();
            float r = getAerobaticSmokeR() / 255f;
            float g = getAerobaticSmokeG() / 255f;
            float b = getAerobaticSmokeB() / 255f;
            Level level = level();
            for (Vec3 offset : aerobaticSmokeOffsets) {
                for (int i = 0; i <= segments; i++) {
                    double x = this.random.triangle(0, 0.1f);
                    double y = this.random.triangle(0, 0.1f);
                    double z = this.random.triangle(0, 0.1f);
                    Vec3 interpolatedPos = vehiclePos.add(offset);
                    Vec3 worldPos = relativeRotPos(interpolatedPos, false).subtract(dir.scale(i)).subtract(getDeltaMovement());
                    level.addParticle(new SmokeCloudOption(false, r, g, b, r, g, b,
                                    0.5f, 0.1f, 1200,
                                    0.3f, 5f, 0.01f), true,
                            worldPos.x, worldPos.y, worldPos.z,
                            x, y, z);
                }
            }
        }
    }

    public float getThrottleLevel() {
        return this.entityData.get(THROTTLE_LEVEL);
    }

    public void setThrottleLevel(float value) {
        throttleLevel = value;
        this.entityData.set(THROTTLE_LEVEL, value);
    }

    public float getPitchInput() {
        return this.entityData.get(PITCH_INPUT);
    }

    public void setPitchInput(float value) {
        pitchInput = value;
        this.entityData.set(PITCH_INPUT, value);
    }

    public float getYawInput() {
        return this.entityData.get(YAW_INPUT);
    }

    public void setYawInput(float value) {
        yawInput = value;
        this.entityData.set(YAW_INPUT, value);
    }

    public float getRollInput() {
        return this.entityData.get(ROLL_INPUT);
    }

    public void setRollInput(float value) {
        rollInput = value;
        this.entityData.set(ROLL_INPUT, value);
    }

    public boolean isLandingGearUp() {
        return landingGear != null && landingGear.isOn();
    }

    public boolean isAirbrakeOn() {
        return airbrakeUnit != null && airbrakeUnit.isOn();
    }

    public boolean isAerobaticSmokeOn() {
        return this.entityData.get(AEROBATIC_SMOKE_ON);
    }

    public void setAerobaticSmokeOn(boolean value) {
        this.entityData.set(AEROBATIC_SMOKE_ON, value);
    }

    public int getAerobaticSmokeR() {
        return this.entityData.get(AEROBATIC_SMOKE_R);
    }

    public void setAerobaticSmokeR(int value) {
        this.entityData.set(AEROBATIC_SMOKE_R, value);
    }

    public int getAerobaticSmokeG() {
        return this.entityData.get(AEROBATIC_SMOKE_G);
    }

    public void setAerobaticSmokeG(int value) {
        this.entityData.set(AEROBATIC_SMOKE_G, value);
    }

    public int getAerobaticSmokeB() {
        return this.entityData.get(AEROBATIC_SMOKE_B);
    }

    public void setAerobaticSmokeB(int value) {
        this.entityData.set(AEROBATIC_SMOKE_B, value);
    }

}
