package org.ywzj.vehicle.vehicle;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.util.INBTSerializable;

public class VehicleCapability implements INBTSerializable<CompoundTag> {

    private float fuel;

    public VehicleCapability() {}

    public float getFuel() {
        return fuel;
    }

    public void setFuel(float fuel) {
        this.fuel = fuel;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("fuel", fuel);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        if (tag.contains("fuel", Tag.TAG_FLOAT)) {
            fuel = tag.getFloat("fuel");
        }
    }

}
