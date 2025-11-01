package org.ywzj.vehicle.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import org.ywzj.vehicle.custom.VehicleDataManager;

import java.util.Map;
import java.util.function.Supplier;

public record ServerSyncVehicleData(
        Map<ResourceLocation, String> vehicleData
) {

    public static void encode(ServerSyncVehicleData msg, FriendlyByteBuf buf) {
        buf.writeMap(msg.vehicleData, FriendlyByteBuf::writeResourceLocation, FriendlyByteBuf::writeUtf);
    }

    public static ServerSyncVehicleData decode(FriendlyByteBuf buf) {
        return new ServerSyncVehicleData(buf.readMap(FriendlyByteBuf::readResourceLocation, FriendlyByteBuf::readUtf));
    }

    public static void onServerMessageReceived(ServerSyncVehicleData msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context context = ctxSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> handle(msg));
        }
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handle(ServerSyncVehicleData message) {
        VehicleDataManager.fromNetwork(message.vehicleData);
    }
}
