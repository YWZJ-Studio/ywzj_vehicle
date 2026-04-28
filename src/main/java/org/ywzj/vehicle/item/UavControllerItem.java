package org.ywzj.vehicle.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.vehicle.Quadcopter;

import java.util.List;
import java.util.UUID;

public class UavControllerItem extends VehicleItem {

    private static final String UAV_UUID = "uavUUID";
    private static final String UAV_X = "uavX";
    private static final String UAV_Y = "uavY";
    private static final String UAV_Z = "uavZ";

    public UavControllerItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand pHand) {
        ItemStack uavControllerItemStack = player.getItemInHand(pHand);
        if (!level.isClientSide && pHand == InteractionHand.MAIN_HAND) {
            ServerLevel serverLevel = (ServerLevel) level;
            CompoundTag tag = uavControllerItemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            if (tag.contains(UAV_UUID)) {
                UUID uavUUID = tag.getUUID(UAV_UUID);
                Entity entity = serverLevel.getEntity(uavUUID);
                if (entity instanceof AbstractVehicle vehicle && vehicle.uav && !vehicle.isDestroyed()) {
                    player.startRiding(vehicle);
                } else if (tag.contains(UAV_X)) {
                    ChunkPos chunkpos = new ChunkPos(BlockPos.containing(new Vec3(tag.getDouble(UAV_X), tag.getDouble(UAV_Y), tag.getDouble(UAV_Z))));
                    serverLevel.getChunkSource().addRegionTicket(TicketType.POST_TELEPORT, chunkpos, 3, player.getId());
                    player.displayClientMessage(Component.translatable("tips.uav_controller.reconnect"), true);
                }
            }
        }
        return InteractionResultHolder.sidedSuccess(uavControllerItemStack, level.isClientSide());
    }

    @Override
    public InteractionResult interactEntity(ItemStack stack, Player player, Entity target, InteractionHand pHand) {
        if (!player.level().isClientSide) {
            if (pHand == InteractionHand.MAIN_HAND) {
                if (target instanceof Quadcopter quadcopter) {
                    ItemStack uavControllerItemStack = player.getItemInHand(pHand);
                    CompoundTag tag = uavControllerItemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                    tag.putUUID(UAV_UUID, quadcopter.getUUID());
                    uavControllerItemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                    player.displayClientMessage(Component.translatable("tips.uav_controller.paired"), true);
                }
            }
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    @Override
    public void inventoryTick(ItemStack pStack, Level pLevel, Entity pEntity, int pSlotId, boolean pIsSelected) {
        if (!pLevel.isClientSide && pLevel.getGameTime() % 10 == 0) {
            CompoundTag tag = pStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            if (tag.contains(UAV_UUID)) {
                UUID uavUUID = tag.getUUID(UAV_UUID);
                ServerLevel serverLevel = (ServerLevel) pLevel;
                if (serverLevel.getEntity(uavUUID) instanceof AbstractVehicle vehicle && !vehicle.isRemoved()) {
                    tag.putDouble(UAV_X, vehicle.getX());
                    tag.putDouble(UAV_Y, vehicle.getY());
                    tag.putDouble(UAV_Z, vehicle.getZ());
                }
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack pStack, Item.TooltipContext pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        CompoundTag tag = pStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.contains(UAV_UUID)) {
            pTooltipComponents.add(Component.translatable("tips.uav_controller", pStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getUUID(UAV_UUID)).withStyle(ChatFormatting.GRAY));
            if (tag.contains(UAV_X)) {
                String posStr = String.format("%.1f, %.1f, %.1f", tag.getDouble(UAV_X), tag.getDouble(UAV_Y), tag.getDouble(UAV_Z));
                pTooltipComponents.add(Component.translatable("tips.uav_controller.uav_at", posStr).withStyle(ChatFormatting.AQUA));
            }
        }
        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
    }

}
