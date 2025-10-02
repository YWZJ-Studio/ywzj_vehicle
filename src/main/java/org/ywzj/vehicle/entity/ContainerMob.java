package org.ywzj.vehicle.entity;

import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Math;
import org.ywzj.vehicle.YwzjVehicle;

public abstract class ContainerMob extends Mob implements ContainerEntity, HasCustomInventoryScreen {

    private LazyOptional<?> itemHandler = LazyOptional.of(() -> new InvWrapper(this));
    protected final NonNullList<ItemStack> items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);

    protected ContainerMob(EntityType<? extends Mob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    public NonNullList<ItemStack> getItemStacks() {
        return this.items;
    }

    @Override
    public void clearItemStacks() {
        this.items.clear();
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
        return this.hasContainer() && !this.isRemoved() && this.position().closerThan(pPlayer.position(), 8.0D);
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
    public void setLootTable(@Nullable ResourceLocation pLootTable) {}

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
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER && this.hasContainer()) {
            return itemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        if (this.hasContainer()) {
            itemHandler.invalidate();
        }
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        if (this.hasContainer()) {
            itemHandler = LazyOptional.of(() -> new InvWrapper(this));
        }
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        ContainerHelper.saveAllItems(compound, this.getItemStacks());
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        ContainerHelper.loadAllItems(compound, this.getItemStacks());
    }

}
