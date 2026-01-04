package org.ywzj.vehicle.network.message;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.ywzj.vehicle.client.gui.VehicleHitIndicatorOverlay;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

import java.util.function.Supplier;

public class ServerVehicleHurtEntity {

    public int vehicleEntityId;
    public int entityId;

    public ServerVehicleHurtEntity() {}

    public ServerVehicleHurtEntity(int vehicleEntityId, int entityId) {
        this.vehicleEntityId = vehicleEntityId;
        this.entityId = entityId;
    }

    public static void encode(ServerVehicleHurtEntity msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.vehicleEntityId);
        buf.writeInt(msg.entityId);
    }

    public static ServerVehicleHurtEntity decode(FriendlyByteBuf buf) {
        ServerVehicleHurtEntity serverVehicleHurtEntity = new ServerVehicleHurtEntity();
        serverVehicleHurtEntity.vehicleEntityId = buf.readInt();
        serverVehicleHurtEntity.entityId = buf.readInt();
        return serverVehicleHurtEntity;
    }

    public static void onServerMessageReceived(ServerVehicleHurtEntity message, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context context = ctxSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> VehicleHitIndicatorOverlay.markHitTimestamp(Minecraft.getInstance().level.getEntity(message.entityId) instanceof AbstractVehicle));
        }
        context.setPacketHandled(true);
    }

}
