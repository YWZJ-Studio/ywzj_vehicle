package org.ywzj.vehicle.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.vehicle.passenger.WarningReceiver;
import org.ywzj.vehicle.vehicle.pojo.WarnType;

public class ServerVehicleWarn implements CustomPacketPayload {

    public static final StreamCodec<FriendlyByteBuf, ServerVehicleWarn> STREAM_CODEC = StreamCodec.of((buf, msg) -> msg.encode(buf), ServerVehicleWarn::decode);
    public static final CustomPacketPayload.Type<ServerVehicleWarn> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(YwzjVehicle.MOD_ID, "vehicle_warn"));
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

    public static void handle(ServerVehicleWarn message, IPayloadContext ctx) {
        if (ctx.flow().isClientbound()) {
            ctx.enqueueWork(() -> WarningReceiver.handle(message));
        }
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}
