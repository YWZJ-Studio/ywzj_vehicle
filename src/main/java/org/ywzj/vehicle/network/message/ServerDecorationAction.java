package org.ywzj.vehicle.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.custom.part.data.PartUnitData;
import org.ywzj.vehicle.custom.part.data.PartUnitPojo;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.part.DecorationAction;
import org.ywzj.vehicle.vehicle.part.DecorationUnit;
public class ServerDecorationAction extends DecorationAction implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ServerDecorationAction> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(YwzjVehicle.MOD_ID, "server_decoration_action"));
    public static final StreamCodec<FriendlyByteBuf, ServerDecorationAction> STREAM_CODEC = StreamCodec.of((buf, msg) -> msg.encode(buf), ServerDecorationAction::decode);

    public ServerDecorationAction() {}

    public ServerDecorationAction(DecorationAction decorationAction) {
        this.action = decorationAction.action;
        this.displayId = decorationAction.displayId;
        this.vehicleId = decorationAction.vehicleId;
        this.decorationUnitId = decorationAction.decorationUnitId;
        this.baseBoneName = decorationAction.baseBoneName;
        this.scale = decorationAction.scale;
        this.selfXRot = decorationAction.selfXRot;
        this.selfYRot = decorationAction.selfYRot;
        this.selfZRot = decorationAction.selfZRot;
        this.offsetFromBone = decorationAction.offsetFromBone;
    }

    public static ServerDecorationAction decode(FriendlyByteBuf buf) {
        ServerDecorationAction serverDecorationAction = new ServerDecorationAction();
        serverDecorationAction.action = buf.readEnum(Action.class);
        serverDecorationAction.displayId = buf.readUtf();
        serverDecorationAction.vehicleId = buf.readInt();
        serverDecorationAction.decorationUnitId = buf.readUtf();
        if (serverDecorationAction.action == Action.REMOVE) {
            return serverDecorationAction;
        }
        serverDecorationAction.baseBoneName = buf.readUtf();
        serverDecorationAction.scale = buf.readFloat();
        serverDecorationAction.selfXRot = buf.readFloat();
        serverDecorationAction.selfYRot = buf.readFloat();
        serverDecorationAction.selfZRot = buf.readFloat();
        serverDecorationAction.offsetFromBone = new Vec3(buf.readVector3f());
        return serverDecorationAction;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(action);
        buf.writeUtf(displayId);
        buf.writeInt(vehicleId);
        buf.writeUtf(decorationUnitId);
        if (action == Action.REMOVE) {
            return;
        }
        buf.writeUtf(baseBoneName);
        buf.writeFloat(scale);
        buf.writeFloat(selfXRot);
        buf.writeFloat(selfYRot);
        buf.writeFloat(selfZRot);
        buf.writeVector3f(offsetFromBone.toVector3f());
    }

    public static void handle(ServerDecorationAction message, IPayloadContext ctx) {
        Player player = LocalVehiclePlayer.instance.getPlayer();
        if (player.level().getEntity(message.vehicleId) instanceof AbstractVehicle vehicle) {
            if (message.action == Action.SET) {
                DecorationUnit decorationUnit = vehicle.getDecorationUnits().get(message.decorationUnitId);
                if (decorationUnit != null) {
                    decorationUnit.update(message);
                } else {
                    PartUnitPojo pojo = new PartUnitPojo();
                    pojo.id = message.decorationUnitId;
                    decorationUnit = new DecorationUnit(pojo.id.hashCode(), vehicle, new PartUnitData(pojo));
                    decorationUnit.update(message);
                    vehicle.getDecorationUnits().put(message.decorationUnitId, decorationUnit);
                }
            } else if (message.action == Action.REMOVE) {
                vehicle.getDecorationUnits().remove(message.decorationUnitId);
            }
        }
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}
