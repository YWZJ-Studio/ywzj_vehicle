package org.ywzj.vehicle.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.part.PartUnit;
import org.ywzj.vehicle.vehicle.part.RadarUnit;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;

import java.util.function.Supplier;

public class ClientRadarAction {

    public Action action;
    public int toEntityId;

    public ClientRadarAction() {}

    public static ClientRadarAction decode(FriendlyByteBuf buf) {
        ClientRadarAction clientRadarAction = new ClientRadarAction();
        clientRadarAction.action = buf.readEnum(Action.class);
        clientRadarAction.toEntityId = buf.readInt();
        return clientRadarAction;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(action);
        buf.writeInt(toEntityId);
    }

    public static void onClientMessageReceived(ClientRadarAction message, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context context = ctxSupplier.get();
        context.setPacketHandled(true);
        ctxSupplier.get().enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            if (player.getVehicle() instanceof AbstractVehicle vehicle) {
                PartUnit<?> partUnit = vehicle.getOwnOperatorUnit(player);
                if (partUnit instanceof WeaponUnit weaponUnit) {
                    RadarUnit mainRadarUnit = weaponUnit.getMainRadarUnit();
                    if (mainRadarUnit == null) {
                        return;
                    }
                    Level level = player.level();
                    Entity toEntity = level.getEntity(message.toEntityId);
                    if (message.action == Action.LOCK) {
                        mainRadarUnit.setLockedEntity(toEntity);
                    } else if (message.action == Action.SEARCH) {
                        if (toEntity != null) {
                            mainRadarUnit.detect(toEntity);
                        }
                    }
                }
            }
        });
    }

    public enum Action {
        SEARCH, LOCK
    }

}
