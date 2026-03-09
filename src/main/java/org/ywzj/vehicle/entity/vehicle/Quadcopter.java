package org.ywzj.vehicle.entity.vehicle;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.ywzj.vehicle.all.AllEntities;
import org.ywzj.vehicle.entity.misc.FakePlayer;
import org.ywzj.vehicle.util.EntityUtil;

import java.util.List;

public class Quadcopter extends RotaryWingVehicle {

    public static final EntityDataAccessor<Float> CABLE_LENGTH = SynchedEntityData.defineId(Quadcopter.class, EntityDataSerializers.FLOAT);
    public Entity cargo;
    private int hookCooldown;
    private Vec3 fakeOperatorPosition;
    private FakePlayer fakeOperator;

    public Quadcopter(EntityType<? extends AbstractVehicle> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.uav = true;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(CABLE_LENGTH, 0f);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        if (fakeOperatorPosition != null) {
            compound.putDouble("fakeOperatorPositionX", fakeOperatorPosition.x);
            compound.putDouble("fakeOperatorPositionY", fakeOperatorPosition.y);
            compound.putDouble("fakeOperatorPositionZ", fakeOperatorPosition.z);
        }
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("fakeOperatorPositionX")) {
            fakeOperatorPosition = new Vec3(
                    compound.getDouble("fakeOperatorPositionX"),
                    compound.getDouble("fakeOperatorPositionY"),
                    compound.getDouble("fakeOperatorPositionZ")
            );
        }
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
            if (fakeOperatorPosition != null) {
                EntityUtil.keepChunkLoaded(this, fakeOperatorPosition);
            }
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
        if (controlUnit.functionalLeft) {
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
        if (livingEntity instanceof ServerPlayer serverPlayer && tickCount != 0) {
            fakeOperatorPosition = livingEntity.position();
            fakeOperator = new FakePlayer(AllEntities.FAKE_PLAYER.get(), level());
            fakeOperator.spawn(serverPlayer);
            fakeOperator.setPos(fakeOperatorPosition);
            level().addFreshEntity(fakeOperator);
            livingEntity.teleportTo(this.position().x, this.position().y, this.position().z);
        }
        super.onEnterVehicle(livingEntity);
    }

    @Override
    public void onRemovedFromWorld() {
        super.onRemovedFromWorld();
        if (!level().isClientSide()) {
            if (getDriver() instanceof ServerPlayer serverPlayer && fakeOperator != null) {
                onLeaveVehicle(serverPlayer);
                serverPlayer.unRide();
                Vec3 backPosition = fakeOperator.position();
                serverPlayer.teleportTo(backPosition.x, backPosition.y, backPosition.z);
                serverPlayer.setYRot(fakeOperator.getYRot());
                serverPlayer.setYBodyRot(fakeOperator.yBodyRot);
                serverPlayer.setXRot(fakeOperator.getXRot());
                fakeOperatorPosition = null;
            }
        }
    }

    @NotNull
    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity pPassenger) {
        if (fakeOperator != null) {
            Vec3 position = fakeOperator.position();
            fakeOperatorPosition = null;
            pPassenger.setYRot(fakeOperator.getYRot());
            pPassenger.setYBodyRot(fakeOperator.yBodyRot);
            pPassenger.setXRot(fakeOperator.getXRot());
            return position;
        } else if (fakeOperatorPosition != null) {
            // 可能是服务端崩溃或客户端异常退出
            return fakeOperatorPosition;
        }
        return super.getDismountLocationForPassenger(pPassenger);
    }

}
