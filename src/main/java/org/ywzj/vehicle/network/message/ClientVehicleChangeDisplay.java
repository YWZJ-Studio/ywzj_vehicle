package org.ywzj.vehicle.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

public class ClientVehicleChangeDisplay implements CustomPacketPayload {

    public static final StreamCodec<FriendlyByteBuf, ClientVehicleChangeDisplay> STREAM_CODEC = StreamCodec.of((buf, msg) -> msg.encode(buf), ClientVehicleChangeDisplay::decode);
    public static final CustomPacketPayload.Type<ClientVehicleChangeDisplay> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(YwzjVehicle.MOD_ID, "client_vehicle_change_display"));
    public int vehicleEntityId;
    public ResourceLocation displayId;

    public ClientVehicleChangeDisplay() {}

    public ClientVehicleChangeDisplay(int vehicleEntityId, ResourceLocation displayId) {
        this.vehicleEntityId = vehicleEntityId;
        this.displayId = displayId;
    }

    public static ClientVehicleChangeDisplay decode(FriendlyByteBuf buf) {
        ClientVehicleChangeDisplay clientVehicleChangeDisplay = new ClientVehicleChangeDisplay();
        clientVehicleChangeDisplay.vehicleEntityId = buf.readInt();
        clientVehicleChangeDisplay.displayId = buf.readResourceLocation();
        return clientVehicleChangeDisplay;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(vehicleEntityId);
        buf.writeResourceLocation(displayId);
    }

    public static void handle(ClientVehicleChangeDisplay message, IPayloadContext ctx) {
        if (ctx.player().level().getEntity(message.vehicleEntityId) instanceof AbstractVehicle vehicle) {
            vehicle.setDisplayId(message.displayId);
        }
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}
