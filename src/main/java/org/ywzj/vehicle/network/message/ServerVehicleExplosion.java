package org.ywzj.vehicle.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.ywzj.vehicle.util.VehicleExplosion;

import java.util.function.Supplier;

public record ServerVehicleExplosion(
        int sourceEntityId,
        double x,
        double y,
        double z,
        float radius
) {

    public static void encode(ServerVehicleExplosion msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.sourceEntityId);
        buf.writeDouble(msg.x);
        buf.writeDouble(msg.y);
        buf.writeDouble(msg.z);
        buf.writeFloat(msg.radius);
    }

    public static ServerVehicleExplosion decode(FriendlyByteBuf buf) {
        return new ServerVehicleExplosion(
                buf.readInt(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readFloat()
        );
    }

    public static void onServerMessageReceived(ServerVehicleExplosion msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context context = ctxSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> VehicleExplosion.effect(msg));
        }
        context.setPacketHandled(true);
    }

}
