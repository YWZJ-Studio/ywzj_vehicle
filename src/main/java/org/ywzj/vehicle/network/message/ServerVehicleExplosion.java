package org.ywzj.vehicle.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.util.VehicleExplosion;
public record ServerVehicleExplosion(
        int sourceEntityId,
        double x,
        double y,
        double z,
        float radius
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ServerVehicleExplosion> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(YwzjVehicle.MOD_ID, "server_vehicle_explosion"));
    public static final StreamCodec<FriendlyByteBuf, ServerVehicleExplosion> STREAM_CODEC = StreamCodec.of((buf, msg) -> encode(msg, buf), ServerVehicleExplosion::decode);

    public static void encode(ServerVehicleExplosion msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.sourceEntityId);
        buf.writeDouble(msg.x);
        buf.writeDouble(msg.y);
        buf.writeDouble(msg.z);
        buf.writeFloat(msg.radius);
    }

    public static ServerVehicleExplosion decode(FriendlyByteBuf buf) {
        return new ServerVehicleExplosion(
                buf.readInt(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readFloat()
        );
    }

    public static void handle(ServerVehicleExplosion msg, IPayloadContext ctxSupplier) {
        ctxSupplier.enqueueWork(() -> VehicleExplosion.effect(msg));
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}
