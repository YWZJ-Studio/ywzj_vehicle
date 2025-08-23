package org.ywzj.vehicle.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import org.ywzj.vehicle.custom.WeaponUnitTypeManager;

import java.util.Map;
import java.util.function.Supplier;

public record ServerSyncData(
        Map<ResourceLocation, String> weaponUnitTypes
) {

    public static void encode(ServerSyncData msg, FriendlyByteBuf buf) {
        buf.writeMap(msg.weaponUnitTypes, FriendlyByteBuf::writeResourceLocation, FriendlyByteBuf::writeUtf);
    }

    public static ServerSyncData decode(FriendlyByteBuf buf) {
        return new ServerSyncData(buf.readMap(FriendlyByteBuf::readResourceLocation, FriendlyByteBuf::readUtf));
    }

    public static void onServerMessageReceived(ServerSyncData msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context context = ctxSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> handle(msg));
        }
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handle(ServerSyncData message) {
        WeaponUnitTypeManager.fromNetwork(message.weaponUnitTypes);
    }
}
