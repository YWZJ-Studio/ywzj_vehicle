package org.ywzj.vehicle.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;
import org.jetbrains.annotations.Nullable;
import org.ywzj.vehicle.all.AllConfigs;

public class FuelTankItem extends Item {

    public FuelTankItem(Properties properties) {
        super(properties);
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new FluidHandler(stack, stack.getMaxDamage(), stack.getMaxDamage() - stack.getDamageValue());
    }

    public void remain(ItemStack stack, int amount) {
        super.setDamage(stack, stack.getMaxDamage() - amount);
        stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM)
                .ifPresent(handler -> {
                    if (handler instanceof FluidHandler fluidHandler) {
                        fluidHandler.syncFluidWithDamage();
                    }
                });
    }

    public static class FluidHandler extends FluidHandlerItemStack {

        private final int capacity;

        public FluidHandler(ItemStack container, int capacity, int amount) {
            super(container, capacity);
            this.capacity = capacity;
            this.setFluid(new FluidStack(Fluids.WATER.getSource(), amount));
        }

        @Override
        public boolean canFillFluidType(FluidStack fluid) {
            String name = fluid.getFluid().getFluidType().toString();
            return AllConfigs.common.fuelNameWhiteList.get().stream().anyMatch(name::contains);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            int filled = super.fill(resource, action);
            if (action.execute()) syncDamageWithFluid();
            return filled;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            FluidStack drained = super.drain(maxDrain, action);
            if (action.execute()) syncDamageWithFluid();
            return drained;
        }

        private void syncDamageWithFluid() {
            if (container.isEmpty() || !container.isDamageableItem()) {
                return;
            }
            FluidStack fluid = getFluid();
            int maxDamage = container.getMaxDamage();
            float fillRatio = (float) fluid.getAmount() / capacity;
            int damage = Math.round((1.0f - fillRatio) * maxDamage);
            container.setDamageValue(Math.min(Math.max(damage, 0), maxDamage));
        }

        public void syncFluidWithDamage() {
            if (container.isEmpty() || !container.isDamageableItem()) {
                return;
            }
            int damage = container.getDamageValue();
            int maxDamage = container.getMaxDamage();
            float fillRatio = 1.0f - (float) damage / maxDamage;
            FluidStack current = getFluid();
            if (!current.isEmpty()) {
                int newAmount = Math.round(capacity * fillRatio);
                current.setAmount(newAmount);
                setFluid(current);
            }
        }

    }

}
