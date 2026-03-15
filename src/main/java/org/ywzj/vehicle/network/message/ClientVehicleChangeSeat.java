package org.ywzj.vehicle.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
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
        NetworkEvent.Context context = ctxSupplier.get();
        context.setPacketHandled(true);
        ctxSupplier.get().enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            Level level = player.level();
            Entity entity = level.getEntity(message.vehicleEntityId);
            if (!(entity instanceof AbstractVehicle vehicle)) {
                return;
            }
            vehicle.onClientVehicleChangeSeat(message, player);
        });
    }

}
