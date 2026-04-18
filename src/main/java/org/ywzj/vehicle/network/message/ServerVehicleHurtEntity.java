package org.ywzj.vehicle.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.ywzj.vehicle.client.gui.VehicleHitIndicatorOverlay;

import java.util.function.Supplier;

public class ServerVehicleHurtEntity {

    public int vehicleEntityId;
    public int entityId;
    public boolean hitVehicle;
    public boolean kill;

    public ServerVehicleHurtEntity() {}

    public ServerVehicleHurtEntity(int vehicleEntityId, int entityId, boolean hitVehicle, boolean kill) {
        this.vehicleEntityId = vehicleEntityId;
        this.entityId = entityId;
        this.hitVehicle = hitVehicle;
        this.kill = kill;
    }

    public static void encode(ServerVehicleHurtEntity msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.vehicleEntityId);
        buf.writeInt(msg.entityId);
        buf.writeBoolean(msg.hitVehicle);
        buf.writeBoolean(msg.kill);
    }

    public static ServerVehicleHurtEntity decode(FriendlyByteBuf buf) {
        ServerVehicleHurtEntity serverVehicleHurtEntity = new ServerVehicleHurtEntity();
        serverVehicleHurtEntity.vehicleEntityId = buf.readInt();
        serverVehicleHurtEntity.entityId = buf.readInt();
        serverVehicleHurtEntity.hitVehicle = buf.readBoolean();
        serverVehicleHurtEntity.kill = buf.readBoolean();
        return serverVehicleHurtEntity;
    }

    public static void onServerMessageReceived(ServerVehicleHurtEntity message, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context context = ctxSupplier.get();
        context.setPacketHandled(true);
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> VehicleHitIndicatorOverlay.markHitTimestamp(message.hitVehicle, message.kill));
        }
    }

}
