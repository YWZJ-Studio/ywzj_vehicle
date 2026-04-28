package org.ywzj.vehicle.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

public class ClientVehicleChangeSeat implements CustomPacketPayload {

    public static final StreamCodec<FriendlyByteBuf, ClientVehicleChangeSeat> STREAM_CODEC = StreamCodec.of((buf, msg) -> msg.encode(buf), ClientVehicleChangeSeat::decode);
    public static final CustomPacketPayload.Type<ClientVehicleChangeSeat> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(YwzjVehicle.MOD_ID, "vehicle_change_seat"));
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

    public static void handle(ClientVehicleChangeSeat message, IPayloadContext ctx) {
        Player player = ctx.player();
        Level level = player.level();
        Entity entity = level.getEntity(message.vehicleEntityId);
        if (!(entity instanceof AbstractVehicle vehicle)) {
            return;
        }
        vehicle.onClientVehicleChangeSeat(message, player);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}
