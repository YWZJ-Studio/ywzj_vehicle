package org.ywzj.vehicle.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.part.HookUnit;

import java.util.function.Supplier;

public record ClientHookAdjustLength(int vehicleEntityId, int partUnitIndex, boolean shorten) {

    public static void encode(ClientHookAdjustLength message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.vehicleEntityId);
        buffer.writeInt(message.partUnitIndex);
        buffer.writeBoolean(message.shorten);
    }

    public static ClientHookAdjustLength decode(FriendlyByteBuf buffer) {
        return new ClientHookAdjustLength(buffer.readInt(), buffer.readInt(), buffer.readBoolean());
    }

    public static void handle(ClientHookAdjustLength message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.setPacketHandled(true);
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player != null && player.level().getEntity(message.vehicleEntityId) instanceof AbstractVehicle vehicle
                    && vehicle.getPartUnit(message.partUnitIndex).orElse(null) instanceof HookUnit hook) {
                hook.adjustLength(player, message.shorten);
            }
        });
    }

}
