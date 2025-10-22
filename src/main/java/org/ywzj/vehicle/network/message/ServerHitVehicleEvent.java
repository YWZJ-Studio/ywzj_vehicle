package org.ywzj.vehicle.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import org.ywzj.vehicle.all.AllConfigs;
import org.ywzj.vehicle.client.gui.VehicleHitIndicatorOverlay;
import org.ywzj.vehicle.event.HitVehicleEvent;

import java.util.function.Supplier;

public class ServerHitVehicleEvent {

    public int entityId;
    public Vec3 hitPosition;
    public Vec3 hitVector;

    public ServerHitVehicleEvent() {}

    public ServerHitVehicleEvent(HitVehicleEvent hitVehicleEvent) {
        this.entityId = hitVehicleEvent.entityId;
        this.hitPosition = hitVehicleEvent.hitPosition;
        this.hitVector = hitVehicleEvent.hitVector;
    }

    public static ServerHitVehicleEvent decode(FriendlyByteBuf buf) {
        ServerHitVehicleEvent vehicleSeatsChange = new ServerHitVehicleEvent();
        vehicleSeatsChange.entityId = buf.readInt();
        vehicleSeatsChange.hitPosition = new Vec3(buf.readVector3f());
        vehicleSeatsChange.hitVector = new Vec3(buf.readVector3f());
        return vehicleSeatsChange;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeVector3f(hitPosition.toVector3f());
        buf.writeVector3f(hitVector.toVector3f());
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
