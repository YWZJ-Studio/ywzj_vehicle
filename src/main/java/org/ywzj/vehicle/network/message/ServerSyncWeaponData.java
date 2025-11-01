package org.ywzj.vehicle.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import org.ywzj.vehicle.custom.VehicleWeaponManager;

import java.util.Map;
import java.util.function.Supplier;

public record ServerSyncWeaponData(
        Map<ResourceLocation, String> weaponUnitTypes
) {

    public static void encode(ServerSyncWeaponData msg, FriendlyByteBuf buf) {
        buf.writeMap(msg.weaponUnitTypes, FriendlyByteBuf::writeResourceLocation, FriendlyByteBuf::writeUtf);
    }

    public static ServerSyncWeaponData decode(FriendlyByteBuf buf) {
        return new ServerSyncWeaponData(buf.readMap(FriendlyByteBuf::readResourceLocation, FriendlyByteBuf::readUtf));
    }

    public static void onServerMessageReceived(ServerSyncWeaponData msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context context = ctxSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> handle(msg));
        }
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handle(ServerSyncWeaponData message) {
        VehicleWeaponManager.fromNetwork(message.weaponUnitTypes);
    }
}
