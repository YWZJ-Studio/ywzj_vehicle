package org.ywzj.vehicle.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

import java.util.function.Supplier;

public class ClientVehicleChangeDisplay {

    public int vehicleEntityId;
    public ResourceLocation displayId;

    public ClientVehicleChangeDisplay() {}

    public ClientVehicleChangeDisplay(int vehicleEntityId, ResourceLocation displayId) {
        this.vehicleEntityId = vehicleEntityId;
        this.displayId = displayId;
    }

    public static ClientVehicleChangeDisplay decode(FriendlyByteBuf buf) {
        ClientVehicleChangeDisplay clientVehicleChangeDisplay = new ClientVehicleChangeDisplay();
        clientVehicleChangeDisplay.vehicleEntityId = buf.readInt();
        clientVehicleChangeDisplay.displayId = buf.readResourceLocation();
        return clientVehicleChangeDisplay;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(vehicleEntityId);
        buf.writeResourceLocation(displayId);
    }

    public static void onClientMessageReceived(ClientVehicleChangeDisplay message, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context context = ctxSupplier.get();
        context.enqueueWork(() -> {
            if (context.getSender().level().getEntity(message.vehicleEntityId) instanceof AbstractVehicle vehicle) {
                vehicle.setDisplayId(message.displayId);
            }
        });
        context.setPacketHandled(true);
    }

}
