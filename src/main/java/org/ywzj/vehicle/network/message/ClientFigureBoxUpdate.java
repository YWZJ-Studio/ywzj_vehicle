package org.ywzj.vehicle.network.message;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import org.ywzj.vehicle.block.FigureBoxBlock;
import org.ywzj.vehicle.blockentity.FigureBoxBlockEntity;

import java.util.function.Supplier;

public class ClientFigureBoxUpdate {

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

    public static void onClientMessageReceived(ClientFigureBoxUpdate message, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context context = ctxSupplier.get();
        context.setPacketHandled(true);
        ctxSupplier.get().enqueueWork(() -> {
            ServerPlayer serverPlayer = context.getSender();
            if (serverPlayer == null) {
                return;
            }
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
        });
    }

}
