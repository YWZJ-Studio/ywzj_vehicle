package org.ywzj.vehicle.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.ywzj.vehicle.network.SliceReassembler;

import java.util.UUID;
import java.util.function.Supplier;

public record ServerSlicedPacket(UUID id, int index, int total, byte[] data) {

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

    public static void handle(ServerSlicedPacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.setPacketHandled(true);
        ctx.enqueueWork(() -> {
            SliceReassembler.receiveSlice(pkt.id, pkt.index, pkt.total, pkt.data);
        });
    }

}
