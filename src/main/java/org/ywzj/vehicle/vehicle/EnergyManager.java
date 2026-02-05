package org.ywzj.vehicle.vehicle;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.all.AllConfigs;
import org.ywzj.vehicle.capability.VehicleCapabilityProvider;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

public class EnergyManager {
    private final AbstractVehicle vehicle;

    public EnergyManager(AbstractVehicle vehicle) {
        this.vehicle = vehicle;
    }

    public void tick() {
        if (vehicle.level().isClientSide()) return;

        tickConsumption();
        tickPower();
    }

    private void tickConsumption() {
        vehicle.getCapability(VehicleCapabilityProvider.CAPABILITY).ifPresent(cap -> {
            float fuel = cap.getFuel();
            fuel = Math.max(0, fuel - vehicle.energyInfo.energyConsumptionPerTick * vehicle.getPower() / 100);
            vehicle.physicsEngine.mass = vehicle.curbWeight + fuel;
            vehicle.getEntityData().set(AbstractVehicle.ENERGY, fuel);
            cap.setFuel(fuel);
        });
    }

    private void tickPower() {
        FluidState fluidState = vehicle.level().getFluidState(BlockPos.containing(new Vec3(vehicle.getMainCubeOBB().obb().center())));
        
        boolean engineBroken = vehicle.getPartUnits().stream()
                .anyMatch(part -> part.getId().toLowerCase().contains("engine") && !part.isFunctional());

        if (!fluidState.isEmpty() || vehicle.isDestroyed() || getEnergy() <= 0 || engineBroken) {
            vehicle.setPower(0);
            return;
        }
        vehicle.setPower(Mth.clamp(vehicle.getPower() + (vehicle.isEngineOn() ? 1 : -1), 0, 100));
    }

    public float getEnergy() {
        float amount = vehicle.getEntityData().get(AbstractVehicle.ENERGY);
        if (amount == 0 && AllConfigs.common.infiniteFuel.get()) {
            amount = Float.MIN_VALUE;
        }
        return amount;
    }

    public void setEnergy(float amount) {
        vehicle.getCapability(VehicleCapabilityProvider.CAPABILITY).ifPresent(cap -> {
            cap.setFuel(amount);
            vehicle.getEntityData().set(AbstractVehicle.ENERGY, amount);
            vehicle.physicsEngine.mass = vehicle.curbWeight + amount;
        });
    }

    public float addEnergy(float amount) {
        float current = getEnergy();
        float space = vehicle.energyInfo.energyCapacity - current;
        if (space > amount) {
            setEnergy(current + amount);
            return 0;
        } else {
            setEnergy(vehicle.energyInfo.energyCapacity);
            return amount - space;
        }
    }
}
