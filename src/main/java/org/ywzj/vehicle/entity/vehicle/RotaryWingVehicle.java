package org.ywzj.vehicle.entity.vehicle;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.ywzj.vehicle.api.animation.IAnimationEntity;
import org.ywzj.vehicle.api.animation.IAnimationInstance;
import org.ywzj.vehicle.audio.VehicleSound;
import org.ywzj.vehicle.client.render.animation.context.RotaryWingVehicleContext;
import org.ywzj.vehicle.client.resource.vehicle.BaseDisplay;
import org.ywzj.vehicle.client.resource.vehicle.RotaryWingVehicleDisplay;
import org.ywzj.vehicle.network.message.ClientVehicleAction;
import org.ywzj.vehicle.particle.SmokeCloudOption;
import org.ywzj.vehicle.util.EntityUtil;
import org.ywzj.vehicle.util.VectorUtil;
import org.ywzj.vehicle.vehicle.part.DoorUnit;
import org.ywzj.vehicle.vehicle.part.LandingGearUnit;
import org.ywzj.vehicle.vehicle.part.RopeUnit;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;
import org.ywzj.vehicle.vehicle.pojo.AimContext;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class RotaryWingVehicle extends AbstractVehicle
        implements IAnimationEntity<RotaryWingVehicle, RotaryWingVehicleContext> {

    public static final EntityDataAccessor<Float> COLLECTIVE_PITCH = SynchedEntityData.defineId(RotaryWingVehicle.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> PITCH_INPUT = SynchedEntityData.defineId(RotaryWingVehicle.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> ROLL_INPUT = SynchedEntityData.defineId(RotaryWingVehicle.class, EntityDataSerializers.FLOAT);
    public float mainRotorForce = 1.4f * physicsEngine.G * physicsEngine.mass;
    public float ceiling = 256;
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
    public boolean hoverMode;
    public boolean fastRoping;
    public String fastRopingDoorId;
    public HashMap<UUID, FastRopingContext> fastRopingContexts = new HashMap<>();
    public RopeUnit fastRopingRope;
    public Entity cargo;
    public RopeUnit cargoRope;
    private int hookCooldown;
    public float collectivePitch;
    public float collectivePitchO;
    public float pitchInput;
    public float pitchInputO;
    public float rollInput;
    public float rollInputO;
    public String landingGearPartId;
    public LandingGearUnit landingGear;
    private VehicleSound engineStartSoundInstance;
    private VehicleSound engineStopSoundInstance;
    private VehicleSound engineRunSoundInstance;
    private IAnimationInstance<RotaryWingVehicleContext> animationInstance;

    public RotaryWingVehicle(EntityType<? extends AbstractVehicle> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.driverXYRotControl = true;
        this.physicsEngine.lockCenterRot = true;
    }

    @Override
    public IAnimationInstance<RotaryWingVehicleContext> getAnimationInstance() {
        return animationInstance;
    }

    @Override
    public void initDisplayData(BaseDisplay display) {
        super.initDisplayData(display);
        if (display instanceof RotaryWingVehicleDisplay rotaryWingVehicleDisplay) {
            this.animationInstance = rotaryWingVehicleDisplay.createAnimationInstance(this);
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(COLLECTIVE_PITCH, 0f);
        this.entityData.define(PITCH_INPUT, 0f);
        this.entityData.define(ROLL_INPUT, 0f);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putFloat("CollectivePitch", getCollectivePitch());
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("CollectivePitch")) {
            setCollectivePitch(Mth.clamp(compound.getFloat("CollectivePitch"), 0, 100));
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
    public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            if (fastRoping) {
                tickFastRoping();
            }
            tickCargo();
        }
        tickInput();
    }

    private void tickInput() {
        collectivePitchO = collectivePitch;
        pitchInputO = pitchInput;
        rollInputO = rollInput;
        if (level().isClientSide()) {
            collectivePitch = getCollectivePitch();
            pitchInput = getPitchInput();
            rollInput = getRollInput();
        } else {
            if (controlUnit.left || controlUnit.right) {
                setRollInput(controlUnit.left ? -1 : 1);
            } else {
                setRollInput(0);
            }
            if (controlUnit.forward || controlUnit.backward) {
                setPitchInput(controlUnit.forward ? -1 : 1);
            } else {
                setPitchInput(Mth.clamp((getXRot() - controlUnit.xRot) / 30, -1f, 1f));
            }
        }
    }

    protected void tickFastRoping() {
        fastRopingContexts.values().removeIf(context -> {
            if (context.isTouchGround() || context.livingEntity.isShiftKeyDown()) {
                return true;
            }
            context.tickDescending();
            return false;
        });
        if (fastRopingContexts.isEmpty() && fastRopingRope != null) {
            fastRopingRope.retract();
        }
    }

    @Override
    protected Vec3 tickMove() {
        // 总距控制
        float collectivePitch = getCollectivePitch();
        if (controlUnit.up) {
            collectivePitch += 5;
        } else if (controlUnit.down) {
            collectivePitch -= 5;
        }
        setCollectivePitch(Mth.clamp(collectivePitch, 0f, 100f));

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
        double scaleAir = position().y < 64 ? 1 : Math.pow(Math.max(0, ceiling - position().y), 0.2) / Math.pow(ceiling - 64, 0.2);
        // 螺旋桨方向的力
        Vec3 force = vP.scale(scale * scaleAir * mainRotorForce);
        // 桨叶水平方向的空速带来升力
        double dVH = Math.sqrt(Math.pow(airSpeed.length(), 2) - Math.pow(dVV, 2));
        force.add(vP.scale(dVH * scaleAir * 0.005f));
        airSpeed = airSpeed.add(force);
        double al = airSpeed.length();
        // 空气阻力
        airSpeed = airSpeed.normalize().scale(al - al * physicsEngine.friction / physicsEngine.mass);
        if (airSpeed.length() >= maxAirSpeed) {
            airSpeed = airSpeed.normalize().scale(maxAirSpeed);
        }
        this.setDeltaMovement(airSpeed);

        if (hoverMode) {
            controlUnit.xRot = 0;
            controlUnit.yRot = getYRot();
            double vy = airSpeed.y - physicsEngine.G;
            if (vy > 0.01) {
                setCollectivePitch(Mth.clamp(getCollectivePitch() - 1f, 0f, 100f));
            } else if (vy < -0.01) {
                setCollectivePitch(Mth.clamp(getCollectivePitch() + 1f, 0f, 100f));
            }
        }

        float xRotSpeedAcceleration = (float) (this.xRotSpeedAcceleration * scale);
        float yRotSpeedAcceleration = (float) (this.yRotSpeedAcceleration * scale);
        float zRotSpeedAcceleration = (float) (this.zRotSpeedAcceleration * scale);
        if (!(controlUnit.leftYaw || controlUnit.rightYaw) && !controlUnit.yRotKeep && scale > 0) {
            float yDiff = Mth.wrapDegrees(controlUnit.yRot - this.getYRot());
            float shrink = Math.min(1, Math.abs(yDiff) / yRotSpeedAcceleration);
            if (yDiff > 0) {
                yRotSpeed = Math.min(yRotSpeedMax, yRotSpeed + yRotSpeedAcceleration * shrink);
            } else if (yDiff < 0) {
                yRotSpeed = Math.max(-yRotSpeedMax, yRotSpeed - yRotSpeedAcceleration * shrink);
            }
            if (yRotSpeed != 0) {
                if (Math.abs(yDiff) > 3) {
                    if (Math.abs(yDiff) <= Math.abs(yRotSpeed)) {
                        yRotSpeed = (float) Mth.lerp(0.3, yRotSpeed, yDiff);
                    }
                    this.setYRot(this.getYRot() + yRotSpeed);
                } else {
                    yRotSpeed = 0;
                    this.setYRot(controlUnit.yRot);
                }
            }
        } else {
            if (controlUnit.rightYaw) {
                yRotSpeed = Math.min(yRotSpeedMax, yRotSpeed + yRotSpeedAcceleration);
            }
            if (controlUnit.leftYaw) {
                yRotSpeed = Math.max(-yRotSpeedMax, yRotSpeed - yRotSpeedAcceleration);
            }
            if (yRotSpeed != 0) {
                this.setYRot(this.getYRot() + yRotSpeed);
            }
        }

        if (!(controlUnit.forward || controlUnit.backward) && !controlUnit.xRotKeep) {
            float xDiff = Mth.wrapDegrees(controlUnit.xRot - this.getXRot());
            float shrink = Math.min(1, Math.abs(xDiff) / xRotSpeedAcceleration);
            if (xDiff > 0) {
                xRotSpeed = Math.min(xRotSpeedMax, xRotSpeed + xRotSpeedAcceleration * shrink);
            } else if (xDiff < 0) {
                xRotSpeed = Math.max(-xRotSpeedMax, xRotSpeed - xRotSpeedAcceleration * shrink);
            }
            if (xRotSpeed != 0) {
                if (Math.abs(xDiff) > 3) {
                    this.setXRot(this.getXRot() + xRotSpeed);
                } else {
                    xRotSpeed = 0;
                    this.setXRot(controlUnit.xRot);
                }
            }
        } else {
            if (controlUnit.forward) {
                xRotSpeed = Math.min(xRotSpeedMax, xRotSpeed + xRotSpeedAcceleration);
            }
            if (controlUnit.backward) {
                xRotSpeed = Math.max(-xRotSpeedMax, xRotSpeed - xRotSpeedAcceleration);
            }
            if (xRotSpeed != 0) {
                this.setXRot(this.getXRot() + xRotSpeed);
            }
        }

        if (!(controlUnit.left || controlUnit.right)) {
            float zDiff = Mth.wrapDegrees(-this.getZRot());
            float shrink = Math.min(1, Math.abs(zDiff) / zRotSpeedAcceleration);
            if (zDiff > 0) {
                zRotSpeed = Math.min(zRotSpeedMax, zRotSpeed + zRotSpeedAcceleration * shrink);
            } else if (zDiff < 0) {
                zRotSpeed = Math.max(-zRotSpeedMax, zRotSpeed - zRotSpeedAcceleration * shrink);
            }
            if (zRotSpeed != 0) {
                if (Math.abs(zDiff) > 3) {
                    this.setZRot(this.getZRot() + zRotSpeed);
                } else {
                    zRotSpeed = 0;
                    this.setZRot(0);
                }
            }
        } else {
            if (controlUnit.right) {
                zRotSpeed = Math.min(zRotSpeedMax, zRotSpeed + zRotSpeedAcceleration);
            }
            if (controlUnit.left) {
                zRotSpeed = Math.max(-zRotSpeedMax, zRotSpeed - zRotSpeedAcceleration);
            }
            if (zRotSpeed != 0) {
                this.setZRot(this.getZRot() + zRotSpeed);
            }
        }
        yRotSpeed = Math.signum(yRotSpeed) * (Math.max(0, Math.abs(yRotSpeed) - 0.1f));
        xRotSpeed = Math.signum(xRotSpeed) * (Math.max(0, Math.abs(xRotSpeed) - 0.1f));
        zRotSpeed = Math.signum(zRotSpeed) * (Math.max(0, Math.abs(zRotSpeed) - 0.1f));

        if (isDestroyed() && !onGround()) {
            this.setYRot(this.getYRot() + 10);
        }

        return force;
    }

    protected void tickCargo() {
        if (cargoRope == null) {
            return;
        }
        float cableLength = cargoRope.getLength();
        if (controlUnit.functionalDown) {
            cableLength = Mth.clamp(cableLength + 1, 0, cargoRope.getMaxLength());
        } else if (controlUnit.functionalUp) {
            cableLength = Mth.clamp(cableLength - 1, 0, cargoRope.getMaxLength());
        }
        cargoRope.setLength(cableLength);
        if (cableLength == 0) {
            if (cargo != null) {
                cargo.fallDistance = 0;
                cargo = null;
            }
            return;
        }
        Vec3 hookPos = cargoRope.getEndPosition();
        while (level().getBlockState(BlockPos.containing(hookPos)).isSolid() && cableLength > 0) {
            cableLength = Mth.clamp(cableLength - 1, 0, cargoRope.getMaxLength());
            cargoRope.setLength(cableLength);
            hookPos = cargoRope.getEndPosition();
        }
        hookCooldown = Math.max(0, hookCooldown - 1);
        if (controlUnit.functionalLeft && cargo != null) {
            cargo.fallDistance = 0;
            cargo = null;
            hookCooldown = 20;
            return;
        }
        if (hookCooldown == 0 && cargo == null && getDriver() != null) {
            List<Entity> entities = level().getEntities(this, AABB.ofSize(hookPos, 2, 4, 2),
                    entity -> !hasPassenger(entity));
            if (!entities.isEmpty()) {
                cargo = entities.get(0);
                this.playSound(SoundEvents.IRON_TRAPDOOR_OPEN);
            }
        }
        if (cargo != null) {
            cargo.fallDistance = 0;
            cargo.setDeltaMovement(Vec3.ZERO);
            if (!cargo.isAlive()) {
                cargo = null;
            } else {
                hookPos = hookPos.subtract(0, cargo.getEyeHeight(), 0);
                if (cargo.onGround()) {
                    hookPos = new Vec3(hookPos.x, Math.max(cargo.position().y, hookPos.y), hookPos.z);
                }
                cargo.teleportTo(hookPos.x, hookPos.y, hookPos.z);
                cargo.hasImpulse = true;
            }
        }
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
        }
    }

    @Override
    protected void tickParticle() {
        super.tickParticle();
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
        // 引擎烟
        if (hasPower()) {
            float engineSpeed = getPower();
            float collectivePitch = getCollectivePitch();
            if ((engineSpeed > 0 && engineParticleTick > Mth.clamp(10 - collectivePitch / 10, 3, 10))) {
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
    }

    @NotNull
    @Override
    public Vec3 getDismountLocationForPassenger(@NotNull LivingEntity pPassenger) {
        if (fastRoping) {
            FastRopingContext fastRopingContext = fastRopingContexts.get(pPassenger.getUUID());
            if (fastRopingContext != null) {
                return fastRopingContext.rope.getAnchorPosition();
            }
        }
        return super.getDismountLocationForPassenger(pPassenger);
    }

    @Override
    public void onLeaveVehicle(LivingEntity pPassenger) {
        super.onLeaveVehicle(pPassenger);
        if (!level().isClientSide() && fastRoping) {
            if (position().y - EntityUtil.getGroundY(level(), position()) < 8) {
                return;
            }
            Vec3 dismountLocation;
            if (fastRopingDoorId != null && partUnitMap.get(fastRopingDoorId) instanceof DoorUnit doorUnit) {
                dismountLocation = doorUnit.worldPosition(doorUnit.getPivotOffset()).subtract(0, pPassenger.getEyeHeight() / 2, 0);
                doorUnit.setOn(true);
            } else {
                dismountLocation = getDismountLocationForPassenger(pPassenger);
            }
            if (fastRopingRope == null) {
                return;
            }
            RopeUnit rope = fastRopingRope;
            rope.setPivotOffset(relativeRotPos(dismountLocation, true).subtract(position()));
            rope.deploy();
            FastRopingContext fastRopingContext = new FastRopingContext();
            fastRopingContext.rope = rope;
            fastRopingContext.livingEntity = pPassenger;
            fastRopingContexts.put(pPassenger.getUUID(), fastRopingContext);
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
        if (message.toggleHoverMode) {
            if (!hoverMode) {
                hoverMode = true;
                player.displayClientMessage(Component.translatable("tips.hover_mode_on"), true);
            } else {
                hoverMode = false;
                player.displayClientMessage(Component.translatable("tips.hover_mode_off"), true);
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

    @Override
    public void impact(Entity entity) {
        if (entity == cargo) {
            return;
        }
        super.impact(entity);
    }

    @Override
    public double thirdPersonDistance(float cameraXRot) {
        double distance = isViewZoomed()
                ? getViewInfo().thirdPersonDistanceZoomed
                : getViewInfo().thirdPersonDistance;
        distance = distance + cargoRope.getLength() * 1.5f;
        if (!isViewZoomed()) {
            distance = Math.max(0, distance - cameraXRot / 90 * getViewInfo().thirdPersonCenterOffset.y);
        }
        return distance;
    }

    public float getCollectivePitch() {
        return entityData.get(COLLECTIVE_PITCH);
    }

    public void setCollectivePitch(float value) {
        collectivePitch = value;
        this.entityData.set(COLLECTIVE_PITCH, value);
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

    public boolean isLandingGearUp() {
        return landingGear != null && landingGear.isOn();
    }

    public static class FastRopingContext {

        public LivingEntity livingEntity;
        public RopeUnit rope;

        public boolean isTouchGround() {
            return livingEntity.level().getBlockState(livingEntity.blockPosition().below()).isSolid() || livingEntity.getVehicle() != null;
        }

        public void tickDescending() {
            List<RopeUnit.RopeNode> ropeNodes = rope.getRopeNodes();
            if (!ropeNodes.isEmpty()) {
                RopeUnit.RopeNode lastNode = ropeNodes.get(ropeNodes.size() - 1);
                if (livingEntity.position().y <= lastNode.pos.y) {
                    applyMovement(lastNode.pos, true);
                    return;
                }
            }
            for (RopeUnit.RopeNode node : ropeNodes) {
                if (livingEntity.position().y < node.pos.y) {
                    applyMovement(node.pos, false);
                    break;
                }
            }
        }

        private void applyMovement(Vec3 nodePos, boolean lastNode) {
            Vec3 targetPos = new Vec3(nodePos.x, lastNode ? nodePos.y : livingEntity.position().y - 0.2, nodePos.z);
            if (livingEntity instanceof ServerPlayer serverPlayer) {
                serverPlayer.teleportTo(targetPos.x, targetPos.y, targetPos.z);
            } else {
                livingEntity.moveTo(targetPos.subtract(livingEntity.getDeltaMovement()));
            }
            livingEntity.fallDistance = 0;
        }

    }

}
