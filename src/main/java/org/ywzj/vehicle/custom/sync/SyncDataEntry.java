package org.ywzj.vehicle.custom.sync;

import net.minecraft.network.FriendlyByteBuf;

public record SyncDataEntry<T> (int index, SyncDataSerializer<T> serializer, T value) {
    public void write(FriendlyByteBuf buf) {
        serializer.write(buf, value);
    }
}
