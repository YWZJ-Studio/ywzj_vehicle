package org.ywzj.vehicle.network.message;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.block.FigureBoxBlock;
import org.ywzj.vehicle.blockentity.FigureBoxBlockEntity;

public class ClientFigureBoxUpdate implements CustomPacketPayload {

    public static final StreamCodec<FriendlyByteBuf, ClientFigureBoxUpdate> STREAM_CODEC = StreamCodec.of((buf, msg) -> msg.encode(buf), ClientFigureBoxUpdate::decode);
    public static final CustomPacketPayload.Type<ClientFigureBoxUpdate> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(YwzjVehicle.MOD_ID, "figure_box_update"));
    public BlockPos blockPos;
    public boolean open;
    public float scale = 1f;
    public float xShift;
    public float yShift;
    public float zShift;
    public float xRot;
    public float yRot;
    public float zRot;

    public ClientFigureBoxUpdate() {}

    public static ClientFigureBoxUpdate decode(FriendlyByteBuf buf) {
        ClientFigureBoxUpdate clientFigureBoxUpdate = new ClientFigureBoxUpdate();
        clientFigureBoxUpdate.blockPos = buf.readBlockPos();
        clientFigureBoxUpdate.open = buf.readBoolean();
        clientFigureBoxUpdate.scale = buf.readFloat();
        clientFigureBoxUpdate.xShift = buf.readFloat();
        clientFigureBoxUpdate.yShift = buf.readFloat();
        clientFigureBoxUpdate.zShift = buf.readFloat();
        clientFigureBoxUpdate.xRot = buf.readFloat();
        clientFigureBoxUpdate.yRot = buf.readFloat();
        clientFigureBoxUpdate.zRot = buf.readFloat();
        return clientFigureBoxUpdate;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(blockPos);
        buf.writeBoolean(open);
        buf.writeFloat(scale);
        buf.writeFloat(xShift);
        buf.writeFloat(yShift);
        buf.writeFloat(zShift);
        buf.writeFloat(xRot);
        buf.writeFloat(yRot);
        buf.writeFloat(zRot);
    }

    public static void handle(ClientFigureBoxUpdate message, IPayloadContext ctx) {
        ServerPlayer serverPlayer = (ServerPlayer) ctx.player();
        Level level = serverPlayer.level();
        if (level.getBlockEntity(message.blockPos) instanceof FigureBoxBlockEntity figureBoxBlockEntity) {
            figureBoxBlockEntity.open = message.open;
            figureBoxBlockEntity.scale = message.scale;
            figureBoxBlockEntity.xShift = message.xShift;
            figureBoxBlockEntity.yShift = message.yShift;
            figureBoxBlockEntity.zShift = message.zShift;
            figureBoxBlockEntity.xRot = message.xRot;
            figureBoxBlockEntity.yRot = message.yRot;
            figureBoxBlockEntity.zRot = message.zRot;
            figureBoxBlockEntity.setChanged();
            level.setBlockAndUpdate(figureBoxBlockEntity.getBlockPos(), figureBoxBlockEntity.getBlockState().setValue(FigureBoxBlock.OPEN, message.open));
        }
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}
