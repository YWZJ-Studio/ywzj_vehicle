package org.ywzj.vehicle.entity.misc;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import org.ywzj.vehicle.all.AllEntities;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

public class FakePlayer extends Mob {

    public static final String DEFAULT_NAME = "Dummy";
    private ServerPlayer copyPlayer;
    public static final EntityDataAccessor<String> NAME = SynchedEntityData.defineId(FakePlayer.class, EntityDataSerializers.STRING);

    public FakePlayer(EntityType<? extends Mob> entityType, Level level) {
        super(entityType, level);
    }

    public void spawn(ServerPlayer player) {
        this.copyPlayer = player;
        this.entityData.set(FakePlayer.NAME, player.getName().getString());
        this.setItemInHand(InteractionHand.MAIN_HAND, player.getItemInHand(InteractionHand.MAIN_HAND));
        this.setItemInHand(InteractionHand.OFF_HAND, player.getItemInHand(InteractionHand.OFF_HAND));
        this.setItemSlot(EquipmentSlot.HEAD, player.getItemBySlot(EquipmentSlot.HEAD));
        this.setItemSlot(EquipmentSlot.CHEST, player.getItemBySlot(EquipmentSlot.CHEST));
        this.setItemSlot(EquipmentSlot.LEGS, player.getItemBySlot(EquipmentSlot.LEGS));
        this.setItemSlot(EquipmentSlot.FEET, player.getItemBySlot(EquipmentSlot.FEET));
        this.setYRot(player.getYRot());
        this.setYBodyRot(player.yBodyRot);
        this.setXRot(player.getXRot());
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            if (copyPlayer != null) {
                if (copyPlayer.getVehicle() == null) {
                    this.discard();
                } else if (copyPlayer.getVehicle() instanceof AbstractVehicle vehicle) {
                    if (!vehicle.uav) {
                        this.discard();
                    }
                }
            } else {
                this.discard();
            }
        }
    }

    @Override
    public boolean hurt(DamageSource damageSource, float amount) {
        if (copyPlayer != null) {
            copyPlayer.unRide();
            copyPlayer.teleportTo(this.getX(), this.getY(), this.getZ());
            copyPlayer.hurt(damageSource, amount);
        }
        return super.hurt(damageSource, amount);
    }

    @Override
    protected void dropAllDeathLoot(DamageSource source) {}

    @Override
    public Component getName() {
        return Component.literal(this.entityData.get(NAME));
    }

    public EntityType<?> getType() {
        return AllEntities.FAKE_PLAYER.get();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(NAME, DEFAULT_NAME);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putString("name", this.entityData.get(NAME));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.entityData.set(NAME, nbt.getString("name").equals("") ? DEFAULT_NAME : nbt.getString("name"));
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> entityDataAccessor) {
        super.onSyncedDataUpdated(entityDataAccessor);
    }

    @Override
    public boolean shouldBeSaved() {
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double v) {
        return false;
    }

}
