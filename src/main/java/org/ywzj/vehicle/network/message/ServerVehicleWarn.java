package org.ywzj.vehicle.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.ywzj.vehicle.vehicle.passenger.WarningReceiver;
import org.ywzj.vehicle.vehicle.pojo.WarnType;

import java.util.function.Supplier;

public class ServerVehicleWarn {

    public int fromEntityId;
    public int toEntityId;
    public WarnType warnType;
    public String info;

    public ServerVehicleWarn() {}

    public ServerVehicleWarn(int fromEntityId, int toEntityId, WarnType warnType, String info) {
        this.fromEntityId = fromEntityId;
        this.toEntityId = toEntityId;
        this.warnType = warnType;
        this.info = info;
    }

    public static ServerVehicleWarn decode(FriendlyByteBuf buf) {
        ServerVehicleWarn data = new ServerVehicleWarn();
        data.fromEntityId = buf.readInt();
        data.toEntityId = buf.readInt();
        data.warnType = buf.readEnum(WarnType.class);
        data.info = buf.readUtf();
        return data;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(fromEntityId);
        buf.writeInt(toEntityId);
        buf.writeEnum(warnType);
        buf.writeUtf(info);
    }

    public static void onServerMessageReceived(ServerVehicleWarn message, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context context = ctxSupplier.get();
        context.setPacketHandled(true);
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> WarningReceiver.handle(message));
        }
    }

}
