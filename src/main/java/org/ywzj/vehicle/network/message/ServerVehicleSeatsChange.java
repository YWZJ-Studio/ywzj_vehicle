package org.ywzj.vehicle.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

public class ServerVehicleSeatsChange implements CustomPacketPayload {

    public static final StreamCodec<FriendlyByteBuf, ServerVehicleSeatsChange> STREAM_CODEC = StreamCodec.of((buf, msg) -> msg.encode(buf), ServerVehicleSeatsChange::decode);
    public static final CustomPacketPayload.Type<ServerVehicleSeatsChange> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(YwzjVehicle.MOD_ID, "vehicle_seats_change"));
    public int vehicleEntityId;
    public int[] passengerIdsBySeat;

    public ServerVehicleSeatsChange() {}

    public ServerVehicleSeatsChange(AbstractVehicle vehicle) {
        this.vehicleEntityId = vehicle.getId();
        int size = vehicle.seats.size();
        this.passengerIdsBySeat = new int[size];
        for (int i = 0; i < size; ++i) {
            this.passengerIdsBySeat[i] = vehicle.seats.get(i).passengerId;
        }
    }

    public static ServerVehicleSeatsChange decode(FriendlyByteBuf buf) {
        ServerVehicleSeatsChange vehicleSeatsChange = new ServerVehicleSeatsChange();
        vehicleSeatsChange.vehicleEntityId = buf.readInt();
        vehicleSeatsChange.passengerIdsBySeat = new int[buf.readInt()];
        for (int index = 0; index < vehicleSeatsChange.passengerIdsBySeat.length; index += 1) {
            vehicleSeatsChange.passengerIdsBySeat[index] = buf.readInt();
        }
        return vehicleSeatsChange;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(vehicleEntityId);
        buf.writeInt(passengerIdsBySeat.length);
        for (int id : passengerIdsBySeat) {
            buf.writeInt(id);
        }
    }

    public static void handle(ServerVehicleSeatsChange message, IPayloadContext ctx) {
        ctx.enqueueWork(() -> AbstractVehicle.onServerVehicleSeatsChange(message));
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}
