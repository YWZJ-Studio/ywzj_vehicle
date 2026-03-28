package org.ywzj.vehicle.api.entity;

import net.minecraft.nbt.CompoundTag;

public interface RemoteTickEntity {

    void writeData(CompoundTag data);

    void readData(CompoundTag data);

    void remoteTick();

}
