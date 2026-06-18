package org.ywzj.vehicle.network.message;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.all.AllItems;
import org.ywzj.vehicle.custom.part.data.PartUnitData;
import org.ywzj.vehicle.custom.part.data.PartUnitPojo;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.item.DecorationItem;
import org.ywzj.vehicle.vehicle.part.DecorationUnit;
import org.ywzj.vehicle.vehicle.pojo.DecorationAction;

public class ClientDecorationAction extends DecorationAction implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ClientDecorationAction> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(YwzjVehicle.MOD_ID, "client_decoration_action"));
    public static final StreamCodec<FriendlyByteBuf, ClientDecorationAction> STREAM_CODEC = StreamCodec.of((buf, msg) -> msg.encode(buf), ClientDecorationAction::decode);

    public ClientDecorationAction() {}

    public static ClientDecorationAction decode(FriendlyByteBuf buf) {
        ClientDecorationAction clientDecorationAction = new ClientDecorationAction();
        clientDecorationAction.action = buf.readEnum(Action.class);
        clientDecorationAction.displayId = buf.readUtf();
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
        buf.writeUtf(displayId);
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

    public static void handle(ClientDecorationAction message, IPayloadContext ctx) {
        ServerPlayer player = (ServerPlayer) ctx.player();
        if (message.action == Action.UPDATE_ITEM) {
            ItemStack itemStack = player.getItemInHand(InteractionHand.MAIN_HAND);
            if (itemStack.getItem() instanceof DecorationItem) {
                CustomData customData = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
                CompoundTag tag = customData.copyTag();
                tag.putString(DecorationItem.TAG_DECORATION_DISPLAY_ID, message.displayId);
                itemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
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
                PacketDistributor.sendToPlayersTrackingEntity(vehicle, serverDecorationAction);
            } else if (message.action == Action.REMOVE) {
                vehicle.getDecorationUnits().remove(message.decorationUnitId);
                ItemStack itemStack = AllItems.DECORATION_ITEM.get().getDefaultInstance();
                itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().putString(DecorationItem.TAG_DECORATION_DISPLAY_ID, message.displayId);
                player.level().addFreshEntity(new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), itemStack));
                ServerDecorationAction serverDecorationAction = new ServerDecorationAction(message);
                PacketDistributor.sendToPlayersTrackingEntity(vehicle, serverDecorationAction);
            }
        }
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}
