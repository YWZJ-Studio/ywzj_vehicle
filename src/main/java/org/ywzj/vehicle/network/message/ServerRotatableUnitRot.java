package org.ywzj.vehicle.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.parts.RotatableUnit;

import java.util.function.Supplier;

public class ServerRotatableUnitRot {

    public int vehicleEntityId;
    public int partUnitIndex;
    public float xAimRot;
    public float yAimRot;

    public ServerRotatableUnitRot() {}

    public ServerRotatableUnitRot(RotatableUnit rotatableUnit) {
        this.vehicleEntityId = rotatableUnit.getVehicle().getId();
        this.partUnitIndex = rotatableUnit.getIndex();
        this.xAimRot = rotatableUnit.getXAimRot();
        this.yAimRot = rotatableUnit.getYAimRot();
    }

    public static ServerRotatableUnitRot decode(FriendlyByteBuf buf) {
        ServerRotatableUnitRot serverRotatableUnitRot = new ServerRotatableUnitRot();
        serverRotatableUnitRot.vehicleEntityId = buf.readInt();
        serverRotatableUnitRot.partUnitIndex = buf.readInt();
        serverRotatableUnitRot.xAimRot = buf.readFloat();
        serverRotatableUnitRot.yAimRot = buf.readFloat();
        return serverRotatableUnitRot;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(vehicleEntityId);
        buf.writeInt(partUnitIndex);
        buf.writeFloat(xAimRot);
        buf.writeFloat(yAimRot);
    }

    public static void onServerMessageReceived(ServerRotatableUnitRot message, Supplier<NetworkEvent.Context> ctxSupplier) {
        ctxSupplier.get().setPacketHandled(true);
        ctxSupplier.get().enqueueWork(() -> AbstractVehicle.onServerRotatableUnitRot(message));
    }

}
