package org.ywzj.vehicle.network.message;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

import java.util.function.Supplier;

public class ServerVehicleChangeDisplay {

    public int vehicleEntityId;
    public ResourceLocation displayId;

    public ServerVehicleChangeDisplay() {}

    public ServerVehicleChangeDisplay(int vehicleEntityId, ResourceLocation displayId) {
        this.vehicleEntityId = vehicleEntityId;
        this.displayId = displayId;
    }

    public static ServerVehicleChangeDisplay decode(FriendlyByteBuf buf) {
        ServerVehicleChangeDisplay serverVehicleChangeDisplay = new ServerVehicleChangeDisplay();
        serverVehicleChangeDisplay.vehicleEntityId = buf.readInt();
        serverVehicleChangeDisplay.displayId = buf.readResourceLocation();
        return serverVehicleChangeDisplay;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(vehicleEntityId);
        buf.writeResourceLocation(displayId);
    }

    public static void onServerMessageReceived(ServerVehicleChangeDisplay message, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context context = ctxSupplier.get();
        context.setPacketHandled(true);
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> {
                if (Minecraft.getInstance().level.getEntity(message.vehicleEntityId) instanceof AbstractVehicle vehicle) {
                    vehicle.setDisplayId(message.displayId);
                    vehicle.initDisplayData();
                }
            });
        }
    }

}
