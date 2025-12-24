package org.ywzj.vehicle.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.ywzj.vehicle.vehicle.passenger.WarningReceiver;
import org.ywzj.vehicle.vehicle.pojo.WarnType;

import java.util.function.Supplier;

public class ServerVehicleWarn {

    public int fromVehicleId;
    public int toVehicleId;
    public WarnType warnType;
    public boolean on;

    public ServerVehicleWarn() {}

    public ServerVehicleWarn(int fromVehicleId, int toVehicleId, WarnType warnType, boolean on) {
        this.fromVehicleId = fromVehicleId;
        this.toVehicleId = toVehicleId;
        this.warnType = warnType;
        this.on = on;
    }

    public static ServerVehicleWarn decode(FriendlyByteBuf buf) {
        ServerVehicleWarn data = new ServerVehicleWarn();
        data.fromVehicleId = buf.readInt();
        data.toVehicleId = buf.readInt();
        data.warnType = buf.readEnum(WarnType.class);
        data.on = buf.readBoolean();
        return data;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(fromVehicleId);
        buf.writeInt(toVehicleId);
        buf.writeEnum(warnType);
        buf.writeBoolean(on);
    }

    public static void onServerMessageReceived(ServerVehicleWarn message, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context context = ctxSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> WarningReceiver.handle(message));
        }
        context.setPacketHandled(true);
    }

}
