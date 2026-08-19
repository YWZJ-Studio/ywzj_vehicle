package org.ywzj.vehicle.entity;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Math;
import org.ywzj.vehicle.YwzjVehicle;

import java.util.List;

public abstract class ContainerCraft extends Entity implements ContainerEntity, HasCustomInventoryScreen {

    private static final EntityDataAccessor<Float> HEALTH = SynchedEntityData.defineId(ContainerCraft.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> MAX_HEALTH = SynchedEntityData.defineId(ContainerCraft.class, EntityDataSerializers.FLOAT);
    private final InvWrapper itemHandler = new InvWrapper(this);
    protected double lerpX;
    protected double lerpY;
    protected double lerpZ;
    protected int lerpSteps;
    protected final NonNullList<ItemStack> items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
    public float healthO = -1;
    public int hurtTick = 0;

    protected ContainerCraft(EntityType<? extends Entity> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(HEALTH, -1F);
        builder.define(MAX_HEALTH, -1F);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        ContainerHelper.saveAllItems(compound, this.getItemStacks(), this.registryAccess());
        compound.putFloat("MaxHealth", this.getMaxHealth());
        compound.putFloat("Health", this.getHealth());
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        ContainerHelper.loadAllItems(compound, this.getItemStacks(), this.registryAccess());
        if (compound.contains("MaxHealth", 99)) {
            this.setMaxHealth(compound.getFloat("MaxHealth"));
        }
        if (compound.contains("Health", 99)) {
            this.setHealth(compound.getFloat("Health"));
        }
    }

    @Override
    public void lerpTo(double pX, double pY, double pZ, float pYaw, float pPitch, int pPosRotationIncrements) {
        this.lerpX = pX;
        this.lerpY = pY;
        this.lerpZ = pZ;
        this.lerpSteps = pPosRotationIncrements;
    }

    @Override
    public void tick() {
        super.tick();
        tickHurt();
    }

    protected void tickHurt() {
        if (healthO == -1) {
            healthO = getHealth();
            return;
        }
        if (hurtTick > 0) {
            hurtTick--;
            if (hurtTick <= 0) {
                healthO = getHealth();
            }
        } else if (healthO != getHealth()) {
            hurtTick = 10;
        }
    }

    public void heal(float pHealAmount) {
        if (pHealAmount <= 0) return;
        float f = this.getHealth();
        if (f > 0.0F) {
            this.setHealth(f + pHealAmount);
        }
    }

    public InvWrapper getItemHandler() {
        return itemHandler;
    }

    public float getHealth() {
        return this.entityData.get(HEALTH);
    }

    public void setHealth(float health) {
        this.entityData.set(HEALTH, Mth.clamp(health, 0.0F, this.getMaxHealth()));
    }

    public float getMaxHealth() {
        return this.entityData.get(MAX_HEALTH);
    }

    public void setMaxHealth(float maxHealth) {
        this.entityData.set(MAX_HEALTH, maxHealth);
    }

    @Override
    public NonNullList<ItemStack> getItemStacks() {
        return this.items;
    }

    @Override
    public void clearItemStacks() {
        this.items.clear();
    }

    protected void pushEntities() {
        if (this.level().isClientSide()) {
            this.level().getEntities(EntityTypeTest.forClass(Player.class), this.getBoundingBox(), EntitySelector.pushableBy(this))
                    .forEach(entity -> entity.push(this));
        } else {
            List<Entity> list = this.level().getEntities(this, this.getBoundingBox(), EntitySelector.pushableBy(this));
            if (!list.isEmpty()) {
                int i = this.level().getGameRules().getInt(GameRules.RULE_MAX_ENTITY_CRAMMING);
                if (i > 0 && list.size() > i - 1 && this.random.nextInt(4) == 0) {
                    int j = 0;
                    for (int k = 0; k < list.size(); ++k) {
                        if (!list.get(k).isPassenger()) {
                            ++j;
                        }
                    }
                    if (j > i - 1) {
                        this.hurt(this.damageSources().cramming(), 6.0F);
                    }
                }
                for (int l = 0; l < list.size(); ++l) {
                    Entity entity = list.get(l);
                    entity.push(this);
                }
            }
        }
    }

    @Override
    public int getContainerSize() {
        return 54;
    }

    @NotNull
    @Override
    public ItemStack getItem(int slot) {
        if (!this.hasContainer() || slot >= this.getContainerSize() || slot < 0) {
            return ItemStack.EMPTY;
        }
        return this.items.get(slot);
    }

    @NotNull
    @Override
    public ItemStack removeItem(int slot, int pAmount) {
        if (!this.hasContainer() || slot >= this.getContainerSize() || slot < 0) {
            return ItemStack.EMPTY;
        }
        return ContainerHelper.removeItem(this.items, slot, pAmount);
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int slot) {
        if (!this.hasContainer() || slot >= this.getContainerSize() || slot < 0) {
            return ItemStack.EMPTY;
        }
        ItemStack itemstack = this.items.get(slot);
        if (itemstack.isEmpty()) {
            return ItemStack.EMPTY;
        } else {
            this.items.set(slot, ItemStack.EMPTY);
            return itemstack;
        }
    }

    @Override
    public void setItem(int slot, ItemStack pStack) {
        if (!this.hasContainer() || slot >= this.getContainerSize() || slot < 0) {
            return;
        }
        var limit = Math.min(this.getMaxStackSize(), pStack.getMaxStackSize());
        if (!pStack.isEmpty() && pStack.getCount() > limit) {
            YwzjVehicle.LOGGER.warn("try inserting ItemStack {} exceeding the maximum stack size: {}, clamped to {}", pStack.getItem(), limit, limit);
            pStack.setCount(limit);
        }
        this.items.set(slot, pStack);
    }

    @Override
    public void setChanged() {}

    @Override
    public boolean stillValid(Player pPlayer) {
        return this.hasContainer() && !this.isRemoved();
    }

    @Override
    public void clearContent() {
        this.items.clear();
    }

    public boolean hasContainer() {
        return this.getContainerSize() > 0;
    }

    @Override
    public void openCustomInventoryScreen(Player pPlayer) {
        pPlayer.openMenu(this);
        if (!pPlayer.level().isClientSide()) {
            this.gameEvent(GameEvent.CONTAINER_OPEN, pPlayer);
        }
    }

    @Override
    public @Nullable ResourceKey<LootTable> getLootTable() {
        return null;
    }

    @Override
    public void setLootTable(@Nullable ResourceKey<LootTable> pLootTable) {}

    @Override
    public long getLootTableSeed() {
        return 0;
    }

    @Override
    public void setLootTableSeed(long pLootTableSeed) {}

    @Override
    public @Nullable AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        if (!pPlayer.isSpectator()) {
            return ChestMenu.sixRows(pContainerId, pPlayerInventory, this);
        }
        return null;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean shouldBeSaved() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    public boolean isInWall() {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

}
