package org.ywzj.vehicle.entity.vehicle.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.all.AllEntities;
import org.ywzj.vehicle.entity.misc.Rope;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.vehicle.RotaryWingVehicle;

import java.util.List;

public class Quadcopter extends RotaryWingVehicle {

    private static final float MAX_CABLE_LENGTH = 8f;
    public static final EntityDataAccessor<Float> CABLE_LENGTH = SynchedEntityData.defineId(Quadcopter.class, EntityDataSerializers.FLOAT);
    public Entity cargo;
    private Rope cargoRope;
    private int hookCooldown;

    public Quadcopter(EntityType<? extends AbstractVehicle> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(CABLE_LENGTH, 0f);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            float cableLength = entityData.get(CABLE_LENGTH);
            this.viewInfo.thirdPersonDistance = 8 + cableLength * 1.5f;
        }
    }

    @Override
    protected Vec3 tickMove() {
        Vec3 force = super.tickMove();
        Rope rope = getOrCreateCargoRope();
        rope.setPos(position());
        float cableLength = entityData.get(CABLE_LENGTH);
        if (controlUnit.functionalDown) {
            cableLength = Mth.clamp(cableLength + 1, 0, MAX_CABLE_LENGTH);
        } else if (controlUnit.functionalUp) {
            cableLength = Mth.clamp(cableLength - 1, 0, MAX_CABLE_LENGTH);
        }
        rope.setControlledLength(cableLength);
        Vec3 hookPos = rope.getEndPosition();
        while (level().getBlockState(BlockPos.containing(hookPos)).isSolid() && cableLength > 0) {
            cableLength = Mth.clamp(cableLength - 1, 0, MAX_CABLE_LENGTH);
            rope.setControlledLength(cableLength);
            hookPos = rope.getEndPosition();
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
            List<Entity> entities = level().getEntities(this, AABB.ofSize(hookPos, 2, 4, 2),
                    entity -> !(entity instanceof Rope) && !hasPassenger(entity));
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
        return force;
    }

    private Rope getOrCreateCargoRope() {
        if (cargoRope == null || !cargoRope.isAlive()) {
            cargoRope = new Rope(AllEntities.ROPE.get(), level());
            cargoRope.setPos(position());
            cargoRope.setControlledLength(entityData.get(CABLE_LENGTH));
            level().addFreshEntity(cargoRope);
        }
        return cargoRope;
    }

    @Override
    public void onRemovedFromWorld() {
        super.onRemovedFromWorld();
        if (!level().isClientSide() && cargoRope != null) {
            cargoRope.discard();
            cargoRope = null;
        }
    }

}
