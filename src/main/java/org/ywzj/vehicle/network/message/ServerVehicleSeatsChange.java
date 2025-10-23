package org.ywzj.vehicle.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

import java.util.function.Supplier;

public class ServerVehicleSeatsChange {

    public int vehicleEntityId;
    public int[] passengerIdsBySeat;

    public ServerVehicleSeatsChange() {}

    public ServerVehicleSeatsChange(AbstractVehicle vehicle) {
        this.vehicleEntityId = vehicle.getId();
        int size = vehicle.seats.size();
        this.passengerIdsBySeat = new int[size];
        for(int i = 0; i < size; ++i) {
            this.passengerIdsBySeat[i] = vehicle.seats.get(i).passengerId;
        }
    }

    public static ServerVehicleSeatsChange decode(FriendlyByteBuf buf) {
        ServerVehicleSeatsChange vehicleSeatsChange = new ServerVehicleSeatsChange();
        vehicleSeatsChange.vehicleEntityId = buf.readInt();
        vehicleSeatsChange.passengerIdsBySeat = new int[buf.readInt()];
        for(int index = 0; index < vehicleSeatsChange.passengerIdsBySeat.length; index += 1) {
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

    public static void onServerMessageReceived(ServerVehicleSeatsChange message, Supplier<NetworkEvent.Context> ctxSupplier) {
        ctxSupplier.get().setPacketHandled(true);
        ctxSupplier.get().enqueueWork(() -> AbstractVehicle.onServerVehicleSeatsChange(message));
    }

}
