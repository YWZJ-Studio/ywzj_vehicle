package org.ywzj.vehicle.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.part.HookUnit;

public record ClientHookAdjustLength(int vehicleEntityId, int partUnitIndex, boolean shorten) implements CustomPacketPayload {

    public static final Type<ClientHookAdjustLength> TYPE = new Type<>(YwzjVehicle.modLocation("hook_adjust_length"));
    public static final StreamCodec<FriendlyByteBuf, ClientHookAdjustLength> STREAM_CODEC = StreamCodec.of(
            (buffer, message) -> {
                buffer.writeInt(message.vehicleEntityId);
                buffer.writeInt(message.partUnitIndex);
                buffer.writeBoolean(message.shorten);
            },
            buffer -> new ClientHookAdjustLength(buffer.readInt(), buffer.readInt(), buffer.readBoolean())
    );

    public static void handle(ClientHookAdjustLength message, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player
                && player.level().getEntity(message.vehicleEntityId) instanceof AbstractVehicle vehicle
                && vehicle.getPartUnit(message.partUnitIndex).orElse(null) instanceof HookUnit hook) {
            hook.adjustLength(player, message.shorten);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}
