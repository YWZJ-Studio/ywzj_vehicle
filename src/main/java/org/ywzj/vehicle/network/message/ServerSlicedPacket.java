package org.ywzj.vehicle.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.network.SliceReassembler;

import java.util.UUID;

public record ServerSlicedPacket(UUID id, int index, int total, byte[] data) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ServerSlicedPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(YwzjVehicle.MOD_ID, "server_sliced_packet"));
    public static final StreamCodec<FriendlyByteBuf, ServerSlicedPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> encode(pkt, buf), ServerSlicedPacket::decode);

    public static void encode(ServerSlicedPacket pkt, FriendlyByteBuf buf) {
        buf.writeUUID(pkt.id);
        buf.writeInt(pkt.index);
        buf.writeInt(pkt.total);
        buf.writeVarInt(pkt.data.length);
        buf.writeBytes(pkt.data);
    }

    public static ServerSlicedPacket decode(FriendlyByteBuf buf) {
        UUID id = buf.readUUID();
        int index = buf.readInt();
        int total = buf.readInt();
        int len = buf.readVarInt();
        byte[] data = new byte[len];
        buf.readBytes(data);
        return new ServerSlicedPacket(id, index, total, data);
    }

    public static void handle(ServerSlicedPacket pkt, IPayloadContext ctxSupplier) {
        SliceReassembler.receiveSlice(pkt.id, pkt.index, pkt.total, pkt.data);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}
