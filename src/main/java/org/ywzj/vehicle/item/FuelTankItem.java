package org.ywzj.vehicle.item;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidHandlerItemStack;
import org.ywzj.vehicle.all.AllConfigs;
import org.ywzj.vehicle.all.AllDataComponents;
import org.ywzj.vehicle.all.AllItems;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

public class FuelTankItem extends VehicleItem {

    public FuelTankItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactEntity(ItemStack stack, Player player, Entity target, InteractionHand hand) {
        if (!player.level().isClientSide && hand == InteractionHand.MAIN_HAND) {
            if (target instanceof AbstractVehicle vehicle) {
                int amount = stack.getMaxDamage() - stack.getDamageValue();
                amount = (int) (vehicle.addEnergy((float) amount / 1000) * 1000);
                ((FuelTankItem) AllItems.FUEL_TANK.get()).remain(stack, amount);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    public void remain(ItemStack stack, int amount) {
        setDamage(stack, stack.getMaxDamage() - amount);
        var handler = stack.getCapability(Capabilities.FluidHandler.ITEM);
        if (handler instanceof FuelTankFluidHandler fluidHandler) {
            fluidHandler.syncFluidWithDamage();
        }
    }

    public static class FuelTankFluidHandler extends FluidHandlerItemStack {

        private final int capacity;

        public FuelTankFluidHandler(ItemStack container, int capacity, Fluid fluid, int amount) {
            super(AllDataComponents.FLUID, container, capacity);
            this.capacity = capacity;
            setFluid(new FluidStack(fluid, amount));
        }

        @Override
        public boolean canFillFluidType(FluidStack fluid) {
            String id = BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString();
            return AllConfigs.common.fuelNameWhiteList.get()
                    .stream()
                    .anyMatch(id::contains);
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
            int maxDamage = container.getMaxDamage();
            float ratio = (float) getFluid().getAmount() / capacity;
            int damage = Math.round((1.0f - ratio) * maxDamage);
            container.setDamageValue(Math.clamp(damage, 0, maxDamage));
        }

        public void syncFluidWithDamage() {
            if (container.isEmpty() || !container.isDamageableItem()) {
                return;
            }
            int damage = container.getDamageValue();
            int maxDamage = container.getMaxDamage();
            float ratio = 1.0f - (float) damage / maxDamage;
            FluidStack fluid = getFluid();
            if (!fluid.isEmpty()) {
                fluid.setAmount(Math.round(capacity * ratio));
                setFluid(fluid);
            }
        }

    }

}
