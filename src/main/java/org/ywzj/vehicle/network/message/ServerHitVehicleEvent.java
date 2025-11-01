package org.ywzj.vehicle.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import org.ywzj.vehicle.all.AllConfigs;
import org.ywzj.vehicle.api.event.HitVehicleEvent;
import org.ywzj.vehicle.client.gui.VehicleHitIndicatorOverlay;

import java.util.function.Supplier;

public class ServerHitVehicleEvent {

    public int entityId;
    public Vec3 hitRelativePosition;
    public Vec3 hitRelativeVector;

    public ServerHitVehicleEvent() {}

    public ServerHitVehicleEvent(HitVehicleEvent hitVehicleEvent) {
        this.entityId = hitVehicleEvent.entityId;
        this.hitRelativePosition = hitVehicleEvent.hitRelativePosition;
        this.hitRelativeVector = hitVehicleEvent.hitRelativeVector;
    }

    public static ServerHitVehicleEvent decode(FriendlyByteBuf buf) {
        ServerHitVehicleEvent vehicleSeatsChange = new ServerHitVehicleEvent();
        vehicleSeatsChange.entityId = buf.readInt();
        vehicleSeatsChange.hitRelativePosition = new Vec3(buf.readVector3f());
        vehicleSeatsChange.hitRelativeVector = new Vec3(buf.readVector3f());
        return vehicleSeatsChange;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeVector3f(hitRelativePosition.toVector3f());
        buf.writeVector3f(hitRelativeVector.toVector3f());
    }

    public static void onServerMessageReceived(ServerHitVehicleEvent message, Supplier<NetworkEvent.Context> ctxSupplier) {
        ctxSupplier.get().setPacketHandled(true);
        if (!AllConfigs.common.hitIndicator.get()) {
            return;
        }
        ctxSupplier.get().enqueueWork(() -> {
            VehicleHitIndicatorOverlay.lastHitTime = System.currentTimeMillis();
            if (!VehicleHitIndicatorOverlay.events.isEmpty() && VehicleHitIndicatorOverlay.events.get(0).entityId != message.entityId) {
                VehicleHitIndicatorOverlay.events.clear();
            }
            VehicleHitIndicatorOverlay.events.add(message);
            if (VehicleHitIndicatorOverlay.events.size() > 10) {
                VehicleHitIndicatorOverlay.events.remove(0);
            }
        });
    }

}
