package org.ywzj.vehicle.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.PartUnit;

import java.util.function.Supplier;

public class ServerPartUnitRot {

    public int vehicleEntityId;
    public int partUnitIndex;
    public float xAimRot;
    public float yAimRot;

    public ServerPartUnitRot() {}

    public ServerPartUnitRot(PartUnit partUnit) {
        this.vehicleEntityId = partUnit.getVehicle().getId();
        this.partUnitIndex = partUnit.getIndex();
        this.xAimRot = partUnit.xAimRot;
        this.yAimRot = partUnit.yAimRot;
    }

    public static ServerPartUnitRot decode(FriendlyByteBuf buf) {
        ServerPartUnitRot serverPartUnitRot = new ServerPartUnitRot();
        serverPartUnitRot.vehicleEntityId = buf.readInt();
        serverPartUnitRot.partUnitIndex = buf.readInt();
        serverPartUnitRot.xAimRot = buf.readFloat();
        serverPartUnitRot.yAimRot = buf.readFloat();
        return serverPartUnitRot;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(vehicleEntityId);
        buf.writeInt(partUnitIndex);
        buf.writeFloat(xAimRot);
        buf.writeFloat(yAimRot);
    }

    public static void onServerMessageReceived(ServerPartUnitRot message, Supplier<NetworkEvent.Context> ctxSupplier) {
        ctxSupplier.get().setPacketHandled(true);
        ctxSupplier.get().enqueueWork(() -> AbstractVehicle.onServerPartUnitRot(message));
    }

}
