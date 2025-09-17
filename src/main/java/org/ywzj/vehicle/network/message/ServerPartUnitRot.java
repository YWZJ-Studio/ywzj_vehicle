package org.ywzj.vehicle.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.misc.weapon.AbstractTurretUnit;
import org.ywzj.vehicle.vehicle.PartUnit;

import java.util.function.Supplier;

public class ServerPartUnitRot {

    public int vehicleEntityId;
    public int partUnitIndex;
    public float xRot;
    public float yRot;

    public ServerPartUnitRot() {}

    public ServerPartUnitRot(PartUnit partUnit) {
        this.vehicleEntityId = partUnit.getVehicle().getId();
        this.partUnitIndex = partUnit.getIndex();
        this.xRot = partUnit.xRot;
        this.yRot = partUnit.yRot;
    }

    public ServerPartUnitRot(AbstractTurretUnit<?> partUnit) {
        this.vehicleEntityId = partUnit.getVehicle().getId();
        this.partUnitIndex = partUnit.getIndex();
        this.xRot = partUnit.xRot;
        this.yRot = partUnit.yRot;
    }

    public static ServerPartUnitRot decode(FriendlyByteBuf buf) {
        ServerPartUnitRot serverPartUnitRot = new ServerPartUnitRot();
        serverPartUnitRot.vehicleEntityId = buf.readInt();
        serverPartUnitRot.partUnitIndex = buf.readInt();
        serverPartUnitRot.xRot = buf.readFloat();
        serverPartUnitRot.yRot = buf.readFloat();
        return serverPartUnitRot;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(vehicleEntityId);
        buf.writeInt(partUnitIndex);
        buf.writeFloat(xRot);
        buf.writeFloat(yRot);
    }

    public static void onServerMessageReceived(ServerPartUnitRot message, Supplier<NetworkEvent.Context> ctxSupplier) {
        ctxSupplier.get().setPacketHandled(true);
        ctxSupplier.get().enqueueWork(() -> AbstractVehicle.onServerPartUnitRot(message));
    }

}
