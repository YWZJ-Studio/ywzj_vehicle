package org.ywzj.vehicle.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

import java.util.function.Supplier;

public class ClientVehicleChangeSeat {

    public int vehicleEntityId;
    public int toSeat;

    public ClientVehicleChangeSeat() {}

    public static ClientVehicleChangeSeat decode(FriendlyByteBuf buf) {
        ClientVehicleChangeSeat changeSeat = new ClientVehicleChangeSeat();
        changeSeat.vehicleEntityId = buf.readInt();
        changeSeat.toSeat = buf.readInt();
        return changeSeat;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(vehicleEntityId);
        buf.writeInt(toSeat);
    }

    public static void onClientMessageReceived(ClientVehicleChangeSeat message, Supplier<NetworkEvent.Context> ctxSupplier) {
        ctxSupplier.get().enqueueWork(() -> AbstractVehicle.onClientVehicleChangeSeat(message, ctxSupplier));
    }

}
