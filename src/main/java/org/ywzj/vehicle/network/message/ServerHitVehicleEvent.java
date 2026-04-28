package org.ywzj.vehicle.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.all.AllConfigs;
import org.ywzj.vehicle.api.event.HitVehicleEvent;
import org.ywzj.vehicle.client.gui.VehicleHitIndicatorOverlay;

public class ServerHitVehicleEvent implements CustomPacketPayload {

    public static final StreamCodec<FriendlyByteBuf, ServerHitVehicleEvent> STREAM_CODEC = StreamCodec.of((buf, msg) -> msg.encode(buf), ServerHitVehicleEvent::decode);
    public static final CustomPacketPayload.Type<ServerHitVehicleEvent> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(YwzjVehicle.MOD_ID, "hit_vehicle_event"));
    public int entityId;
    public Vec3 hitRelativePosition;
    public Vec3 hitRelativeVector;
    public float damage;
    public Component message;

    public ServerHitVehicleEvent() {}

    public ServerHitVehicleEvent(HitVehicleEvent hitVehicleEvent) {
        this.entityId = hitVehicleEvent.entityId;
        this.hitRelativePosition = hitVehicleEvent.hitRelativePosition;
        this.hitRelativeVector = hitVehicleEvent.hitRelativeVector;
        this.damage = hitVehicleEvent.damage;
        this.message = hitVehicleEvent.message;
    }

    public static ServerHitVehicleEvent decode(FriendlyByteBuf buf) {
        ServerHitVehicleEvent vehicleSeatsChange = new ServerHitVehicleEvent();
        vehicleSeatsChange.entityId = buf.readInt();
        vehicleSeatsChange.hitRelativePosition = new Vec3(buf.readVector3f());
        vehicleSeatsChange.hitRelativeVector = new Vec3(buf.readVector3f());
        vehicleSeatsChange.damage = buf.readFloat();
        vehicleSeatsChange.message = Component.translatable(buf.readUtf());
        return vehicleSeatsChange;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeVector3f(hitRelativePosition.toVector3f());
        buf.writeVector3f(hitRelativeVector.toVector3f());
        buf.writeFloat(damage);
        buf.writeUtf(message.getString());
    }

    public static void handle(ServerHitVehicleEvent message, IPayloadContext ctx) {
        if (!AllConfigs.common.hitIndicator.get()) {
            return;
        }
        VehicleHitIndicatorOverlay.lastHitTime = System.currentTimeMillis();
        if (!VehicleHitIndicatorOverlay.events.isEmpty() && VehicleHitIndicatorOverlay.events.get(0).entityId != message.entityId) {
            VehicleHitIndicatorOverlay.events.clear();
        }
        VehicleHitIndicatorOverlay.events.add(message);
        if (VehicleHitIndicatorOverlay.events.size() > 128) {
            VehicleHitIndicatorOverlay.events.remove(0);
        }
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}
