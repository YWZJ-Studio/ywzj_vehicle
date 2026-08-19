package org.ywzj.vehicle.entity.misc;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.all.AllItems;
import org.ywzj.vehicle.item.ParachutePackItem;

public class ParagliderCanopy extends Entity {

    private static final int POST_LANDING_LIFETIME = 60;
    private static final double FALLING_GRAVITY = 0.04;
    private static final double FALLING_DRAG = 0.98;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(ParagliderCanopy.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> FALLING = SynchedEntityData.defineId(ParagliderCanopy.class, EntityDataSerializers.BOOLEAN);
    private boolean landed;
    private int landedTicks;

    public ParagliderCanopy(EntityType<? extends ParagliderCanopy> entityType, Level level) {
        super(entityType, level);
        noPhysics = true;
        setNoGravity(true);
    }

    public void equip(ServerPlayer player) {
        entityData.set(OWNER_ID, player.getId());
        setPos(player.position());
        level().addFreshEntity(this);
    }

    public void fallThenDiscard() {
        entityData.set(FALLING, true);
        Vec3 movement = getDeltaMovement();
        setDeltaMovement(0, movement.y, 0);
        Entity owner = getOwner();
        if (owner != null && (owner.onGround() || owner.isInWater())) {
            landed = true;
        }
    }

    public Entity getOwner() {
        return level().getEntity(entityData.get(OWNER_ID));
    }

    public boolean isFalling() {
        return entityData.get(FALLING);
    }

    @Override
    public void tick() {
        super.tick();
        if (isFalling()) {
            tickFalling();
            return;
        }
        if (level().isClientSide) {
            return;
        }
        Entity owner = getOwner();
        if (owner instanceof LivingEntity living && living.isAlive()) {
            if (!living.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).is(AllItems.PARACHUTE_PACK.get())
                    || !ParachutePackItem.isOpen(living.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST))) {
                fallThenDiscard();
                return;
            }
            setPos(living.position());
            setYRot(living.getYHeadRot());
            setDeltaMovement(living.getDeltaMovement());
            return;
        }
        fallThenDiscard();
    }

    private void tickFalling() {
        Vec3 movement = getDeltaMovement();
        double verticalMovement = movement.y;
        if (!level().isClientSide) {
            if (!landed && (!level().noCollision(this, getBoundingBox().expandTowards(0, verticalMovement, 0))
                    || getY() < level().getMinBuildHeight())) {
                landed = true;
            }
            if (landed && ++landedTicks > POST_LANDING_LIFETIME) {
                discard();
                return;
            }
        }
        setPos(position().add(0, verticalMovement, 0));
        setDeltaMovement(0, verticalMovement * FALLING_DRAG - FALLING_GRAVITY, 0);
    }

    @Override
    public Vec3 getLightProbePosition(float pPartialTicks) {
        return position().add(0, 6, 0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(OWNER_ID, -1);
        builder.define(FALLING, false);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

}
