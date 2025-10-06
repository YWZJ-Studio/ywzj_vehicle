package org.ywzj.vehicle.vehicle;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VehicleCapabilityProvider implements ICapabilitySerializable<CompoundTag> {

    public static final Capability<VehicleCapability> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});
    private final VehicleCapability instance = new VehicleCapability();

    @Override
    public CompoundTag serializeNBT() {
        return instance.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        instance.deserializeNBT(tag);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return CAPABILITY.orEmpty(cap, LazyOptional.of(() -> instance).cast());
    }

}
