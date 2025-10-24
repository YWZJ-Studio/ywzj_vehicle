package org.ywzj.vehicle.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.ywzj.vehicle.custom.sync.SyncDataEntry;
import org.ywzj.vehicle.custom.sync.SyncDataManager;
import org.ywzj.vehicle.custom.sync.SyncDataSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record ServerEntityDataUpdate (
        int entityId,
        int partIndex,
        List<SyncDataEntry<?>> entries
) {
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

    public static void onServerMessageReceived(ServerEntityDataUpdate msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context context = ctxSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> SyncDataManager.onMessage(msg));
        }
        context.setPacketHandled(true);
    }
}
