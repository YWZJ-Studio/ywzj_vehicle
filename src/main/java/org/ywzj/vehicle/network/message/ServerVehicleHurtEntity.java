package org.ywzj.vehicle.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.client.gui.VehicleHitIndicatorOverlay;

public class ServerVehicleHurtEntity implements CustomPacketPayload {

    public static final StreamCodec<FriendlyByteBuf, ServerVehicleHurtEntity> STREAM_CODEC = StreamCodec.of((buf, msg) -> encode(msg, buf), ServerVehicleHurtEntity::decode);
    public static final CustomPacketPayload.Type<ServerVehicleHurtEntity> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(YwzjVehicle.MOD_ID, "vehicle_hurt_entity"));
    public int vehicleEntityId;
    public int entityId;
    public boolean hitVehicle;
    public boolean kill;

    public ServerVehicleHurtEntity() {}

    public ServerVehicleHurtEntity(int vehicleEntityId, int entityId, boolean hitVehicle, boolean kill) {
        this.vehicleEntityId = vehicleEntityId;
        this.entityId = entityId;
        this.hitVehicle = hitVehicle;
        this.kill = kill;
    }

    public static void encode(ServerVehicleHurtEntity msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.vehicleEntityId);
        buf.writeInt(msg.entityId);
        buf.writeBoolean(msg.hitVehicle);
        buf.writeBoolean(msg.kill);
    }

    public static ServerVehicleHurtEntity decode(FriendlyByteBuf buf) {
        ServerVehicleHurtEntity serverVehicleHurtEntity = new ServerVehicleHurtEntity();
        serverVehicleHurtEntity.vehicleEntityId = buf.readInt();
        serverVehicleHurtEntity.entityId = buf.readInt();
        serverVehicleHurtEntity.hitVehicle = buf.readBoolean();
        serverVehicleHurtEntity.kill = buf.readBoolean();
        return serverVehicleHurtEntity;
    }

    public static void handle(ServerVehicleHurtEntity message, IPayloadContext ctx) {
        ctx.enqueueWork(() -> VehicleHitIndicatorOverlay.markHitTimestamp(message.hitVehicle, message.kill));
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}
