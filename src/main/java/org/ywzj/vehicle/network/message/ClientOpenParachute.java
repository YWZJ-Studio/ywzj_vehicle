package org.ywzj.vehicle.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.ywzj.vehicle.item.ParachutePackItem;

import java.util.function.Supplier;

public class ClientOpenParachute {

    public ClientOpenParachute() {}

    public static ClientOpenParachute decode(FriendlyByteBuf buffer) {
        return new ClientOpenParachute();
    }

    public void encode(FriendlyByteBuf buffer) {}

    public static void handle(ClientOpenParachute message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null) {
            context.enqueueWork(() -> ParachutePackItem.open(player));
        }
        context.setPacketHandled(true);
    }

}
