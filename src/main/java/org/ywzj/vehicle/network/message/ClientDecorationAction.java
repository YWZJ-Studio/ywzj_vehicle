package org.ywzj.vehicle.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import org.ywzj.vehicle.all.AllItems;
import org.ywzj.vehicle.custom.part.data.PartUnitData;
import org.ywzj.vehicle.custom.part.data.PartUnitPojo;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.item.DecorationItem;
import org.ywzj.vehicle.network.Channel;
import org.ywzj.vehicle.vehicle.part.DecorationAction;
import org.ywzj.vehicle.vehicle.part.DecorationUnit;

import java.util.function.Supplier;

public class ClientDecorationAction extends DecorationAction {

    public ClientDecorationAction() {}

    public static ClientDecorationAction decode(FriendlyByteBuf buf) {
        ClientDecorationAction clientDecorationAction = new ClientDecorationAction();
        clientDecorationAction.action = buf.readEnum(Action.class);
        clientDecorationAction.decorationDisplayId = buf.readUtf();
        if (clientDecorationAction.action == Action.UPDATE_ITEM) {
            return clientDecorationAction;
        }
        clientDecorationAction.vehicleId = buf.readInt();
        clientDecorationAction.decorationUnitId = buf.readUtf();
        if (clientDecorationAction.action == Action.REMOVE) {
            return clientDecorationAction;
        }
        clientDecorationAction.baseBoneName = buf.readUtf();
        clientDecorationAction.scale = buf.readFloat();
        clientDecorationAction.selfXRot = buf.readFloat();
        clientDecorationAction.selfYRot = buf.readFloat();
        clientDecorationAction.selfZRot = buf.readFloat();
        clientDecorationAction.offsetFromBone = new Vec3(buf.readVector3f());
        return clientDecorationAction;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(action);
        buf.writeUtf(decorationDisplayId);
        if (action == Action.UPDATE_ITEM) {
            return;
        }
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

    public static void onClientMessageReceived(ClientDecorationAction message, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context context = ctxSupplier.get();
        context.setPacketHandled(true);
        ctxSupplier.get().enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            if (message.action == Action.UPDATE_ITEM) {
                ItemStack itemStack = player.getItemInHand(InteractionHand.MAIN_HAND);
                if (itemStack.getItem() instanceof DecorationItem) {
                    itemStack.getOrCreateTag().putString(DecorationItem.TAG_DECORATION_DISPLAY_ID, message.decorationDisplayId);
                }
            } else if (player.level().getEntity(message.vehicleId) instanceof AbstractVehicle vehicle) {
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
                        ItemStack itemStack = player.getItemInHand(InteractionHand.MAIN_HAND);
                        if (itemStack.getItem() instanceof DecorationItem) {
                            itemStack.shrink(1);
                        }
                    }
                    ServerDecorationAction serverDecorationAction = new ServerDecorationAction(message);
                    Channel.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> vehicle), serverDecorationAction);
                } else if (message.action == Action.REMOVE) {
                    vehicle.getDecorationUnits().remove(message.decorationUnitId);
                    ItemStack itemStack = AllItems.DECORATION_ITEM.get().getDefaultInstance();
                    itemStack.getOrCreateTag().putString(DecorationItem.TAG_DECORATION_DISPLAY_ID, message.decorationDisplayId);
                    player.level().addFreshEntity(new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), itemStack));
                    ServerDecorationAction serverDecorationAction = new ServerDecorationAction(message);
                    Channel.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> vehicle), serverDecorationAction);
                }
            }
        });
    }

}
