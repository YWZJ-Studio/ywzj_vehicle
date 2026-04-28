package org.ywzj.vehicle.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.api.custom.sync.SyncDataSerializer;
import org.ywzj.vehicle.custom.sync.SyncDataEntry;
import org.ywzj.vehicle.custom.sync.SyncDataManager;

import java.util.ArrayList;
import java.util.List;

public record ServerEntityDataUpdate (
        int entityId,
        int partIndex,
        List<SyncDataEntry<?>> entries
) implements CustomPacketPayload {

    public static final StreamCodec<FriendlyByteBuf, ServerEntityDataUpdate> STREAM_CODEC = StreamCodec.of((buf, msg) -> msg.encode(buf), ServerEntityDataUpdate::decode);
    public static final CustomPacketPayload.Type<ServerEntityDataUpdate> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(YwzjVehicle.MOD_ID, "entity_data_update"));

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeInt(partIndex);
        buf.writeVarInt(entries.size());
        for (SyncDataEntry<?> entry : entries) {
            buf.writeInt(entry.index());
            int id = SyncDataManager.get().getId(entry.serializer());
            buf.writeVarInt(id);
            entry.write(buf);
        }
    }

    public static ServerEntityDataUpdate decode(FriendlyByteBuf buf) {
        int entityId = buf.readInt();
        int partIndex = buf.readInt();
        int size = buf.readVarInt();
        List<SyncDataEntry<?>> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            int index = buf.readInt();
            int serializerId = buf.readVarInt();
            var serializer = SyncDataManager.get().getSerializer(serializerId);
            entries.add(SyncDataSerializer.readEntry(buf, serializer, index));
        }
        return new ServerEntityDataUpdate(entityId, partIndex, entries);
    }

    public static void handle(ServerEntityDataUpdate msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> SyncDataManager.onMessage(msg));
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}
