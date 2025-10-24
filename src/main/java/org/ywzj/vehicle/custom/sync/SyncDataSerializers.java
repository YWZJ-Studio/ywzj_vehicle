package org.ywzj.vehicle.custom.sync;

import net.minecraft.network.FriendlyByteBuf;

public class SyncDataSerializers {

    public static SyncDataSerializer<Integer> INT = SyncDataSerializer.create(
            FriendlyByteBuf::writeInt,
            FriendlyByteBuf::readInt,
            Integer::equals
    );

    public static SyncDataSerializer<Float> FLOAT = SyncDataSerializer.create(
            FriendlyByteBuf::writeFloat,
            FriendlyByteBuf::readFloat,
            Float::equals
    );

    public static SyncDataSerializer<Double> DOUBLE = SyncDataSerializer.create(
            FriendlyByteBuf::writeDouble,
            FriendlyByteBuf::readDouble,
            Double::equals
    );

    private SyncDataSerializers(){}
}
