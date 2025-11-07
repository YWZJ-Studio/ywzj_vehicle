package org.ywzj.vehicle.api.custom.sync;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

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

    public static SyncDataSerializer<Vec3> VEC3 = SyncDataSerializer.create(
            (friendlyByteBuf, vec3) -> friendlyByteBuf.writeVector3f(vec3.toVector3f()),
            friendlyByteBuf -> new Vec3(friendlyByteBuf.readFloat(), friendlyByteBuf.readFloat(), friendlyByteBuf.readFloat()),
            Vec3::equals
    );

    private SyncDataSerializers(){}

}
