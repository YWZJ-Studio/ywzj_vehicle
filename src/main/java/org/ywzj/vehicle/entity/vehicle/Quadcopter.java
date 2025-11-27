package org.ywzj.vehicle.entity.vehicle;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.ywzj.vehicle.all.AllEntities;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.entity.misc.FakePlayer;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;

import java.util.List;

public class Quadcopter extends RotaryWingVehicle {

    public static final EntityDataAccessor<Float> CABLE_LENGTH = SynchedEntityData.defineId(Quadcopter.class, EntityDataSerializers.FLOAT);
    public Entity cargo;
    private int hookCooldown;
    private Vec3 fakeOperatorPos;
    private FakePlayer fakeOperator;

    public Quadcopter(EntityType<? extends AbstractVehicle> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.physicsEngine.mass = 1f;
        this.mainRotorForce = 1.2f * physicsEngine.gravityA * physicsEngine.mass;
        this.xRotSpeedAcceleration = 4f;
        this.xRotSpeedMax = 8f;
        this.yRotSpeedAcceleration = 4f;
        this.yRotSpeedMax = 8f;
        this.zRotSpeedAcceleration = 4f;
        this.zRotSpeedMax = 8f;
        this.maxAirSpeed = 2f;
        this.uav = true;
//        this.thirdPersonCenterOffset = new Vec3(0, 3, 0);
//        this.thirdPersonDistance = 7;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(CABLE_LENGTH, 0f);
    }

    @Override
    public SoundEvent getEngineStartSound() {
        return null;
    }

    @Override
    public SoundEvent getEngineStopSound() {
        return null;
    }

    @Override
    public SoundEvent getEngineRunSound() {
        return AllSounds.Z10_ENGINE_RUN.get();
    }

    @Override
    public void initPartUnits() {
        WeaponUnit sightingSystem = new WeaponUnit("sighting_system", 0, this,
                new Vec3(0f, 0f, 0f),
                0,
                new Vec3(0f, 0f, 0.2f),
                null,
                new Vec3(0f, 2f, 0.2f),
                null);
        sightingSystem.xRotSpeed = 180f / 20;
        sightingSystem.yRotSpeed = 180f / 20;
        sightingSystem.xRotMax = 90f;
        sightingSystem.xRotMin = -13f;
        sightingSystem.yRotMax = 45f;
        sightingSystem.yRotMin = -45f;
        this.partUnits.add(sightingSystem);
        this.seats.add(new Seat(0, sightingSystem));
    }

    @Override
    public InteractionResult interact(Player pPlayer, InteractionHand pHand) {
        return InteractionResult.PASS;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            float cableLength = entityData.get(CABLE_LENGTH);
            this.viewInfo.thirdPersonDistance = 8 + cableLength * 1.5f;
        } else {
            if (fakeOperatorPos != null) {
                keepChunkLoaded(fakeOperatorPos);
            }
        }
    }

    @Override
    protected void tickPower() {
        FluidState fluidState = level().getFluidState(BlockPos.containing(new Vec3(mainCubeOBB.obb().center())));
        if (!fluidState.isEmpty()) {
            setPower(0);
            return;
        }
        setPower(Mth.clamp(getPower() + (isEngineOn() ? 1 : -1), 0, 100));
        if (getEnergy() == 0) {
            setPower(0);
        }
    }

    @Override
    protected Vec3 tickMove() {
        Vec3 force = super.tickMove();
        float cableLength = entityData.get(CABLE_LENGTH);
        if (controlUnit.functionalDown) {
            cableLength = Mth.clamp(cableLength + 1, 0, 8);
        } else if (controlUnit.functionalUp) {
            cableLength = Mth.clamp(cableLength - 1, 0, 8);
        }
        Vec3 hookPos = relativeRotPos(position().add(new Vec3(0, -cableLength, 0)), false);
        while (level().getBlockState(BlockPos.containing(hookPos)).isSolid() && cableLength > 0) {
            cableLength = Mth.clamp(cableLength - 1, 0, 8);
            hookPos = relativeRotPos(position().add(new Vec3(0, -cableLength, 0)), false);
        }
        this.entityData.set(CABLE_LENGTH, cableLength);
        hookCooldown = Math.max(0, hookCooldown - 1);
        if (controlUnit.functionalLeft || getDriver() == null) {
            if (cargo != null) {
                cargo.fallDistance = 0;
                cargo = null;
                hookCooldown = 20;
                return force;
            }
        }
        if (hookCooldown == 0 && cargo == null && getDriver() != null) {
            List<Entity> entities = level().getEntities(this, AABB.ofSize(hookPos, 2, 4, 2), entity -> !hasPassenger(entity));
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
                hookPos = hookPos.subtract(new Vec3(0, cargo.getEyeHeight(), 0)).add(this.getDeltaMovement().scale(1.5f));
                if (cargo.onGround()) {
                    hookPos = new Vec3(hookPos.x, Math.max(cargo.position().y, hookPos.y), hookPos.z);
                }
                cargo.teleportTo(hookPos.x, hookPos.y, hookPos.z);
                cargo.hasImpulse = true;
            }
        }
        return force;
    }

    @Override
    public void onEnterVehicle(LivingEntity livingEntity) {
        super.onEnterVehicle(livingEntity);
        if (livingEntity instanceof ServerPlayer serverPlayer) {
            fakeOperatorPos = livingEntity.position();
            fakeOperator = new FakePlayer(AllEntities.FAKE_PLAYER.get(), level());
            fakeOperator.spawn(serverPlayer);
            fakeOperator.setPos(fakeOperatorPos);
            level().addFreshEntity(fakeOperator);
        }
    }

    @Override
    public void onRemovedFromWorld() {
        super.onRemovedFromWorld();
        if (getDriver() instanceof ServerPlayer serverPlayer && fakeOperator != null) {
            onLeaveVehicle(serverPlayer);
            serverPlayer.unRide();
            Vec3 pos = fakeOperator.position();
            serverPlayer.teleportTo(pos.x, pos.y, pos.z);
            serverPlayer.setYRot(fakeOperator.getYRot());
            serverPlayer.setYBodyRot(fakeOperator.yBodyRot);
            serverPlayer.setXRot(fakeOperator.getXRot());
            fakeOperatorPos = null;
        }
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity pPassenger) {
        if (getDriver() instanceof ServerPlayer serverPlayer && fakeOperator != null) {
            onLeaveVehicle(serverPlayer);
            Vec3 pos = fakeOperator.position();
            fakeOperatorPos = null;
            serverPlayer.setYRot(fakeOperator.getYRot());
            serverPlayer.setYBodyRot(fakeOperator.yBodyRot);
            serverPlayer.setXRot(fakeOperator.getXRot());
            return pos;
        }
        return super.getDismountLocationForPassenger(pPassenger);
    }

    @Override
    public void shoot(int partUnitIndex, List<Vec3> ammoSpawnPositions, float ammoXRot, float ammoYRot, @Nullable LivingEntity operator) {

    }

}
