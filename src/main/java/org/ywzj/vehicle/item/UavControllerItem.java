package org.ywzj.vehicle.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.stream.wakeup.VehicleWakeup;
import org.ywzj.vehicle.stream.wakeup.VehicleWakeupData;

import java.util.List;
import java.util.UUID;

public class UavControllerItem extends VehicleItem {

    private static final String UAV_UUID = "uavUUID";
    private static final String UAV_X = "uavX";
    private static final String UAV_Y = "uavY";
    private static final String UAV_Z = "uavZ";
    private static final String UAV_DIMENSION = "uavDimension";

    public UavControllerItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand pHand) {
        ItemStack uavControllerItemStack = player.getItemInHand(pHand);
        if (!level.isClientSide && pHand == InteractionHand.MAIN_HAND && player instanceof ServerPlayer serverPlayer) {
            CompoundTag tag = uavControllerItemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            if (tag.contains(UAV_UUID)) {
                connect(serverPlayer, tag.getUUID(UAV_UUID));
            }
        }
        return InteractionResultHolder.sidedSuccess(uavControllerItemStack, level.isClientSide());
    }

    private static void connect(ServerPlayer player, UUID uavUUID) {
        VehicleWakeup.Result result = VehicleWakeup.request(player, uavUUID, new VehicleWakeup.WakeCallback() {

            @Override
            public void onWake(ServerPlayer requester, Entity vehicle) {
                if (vehicle instanceof AbstractVehicle uav && uav.uav && !uav.isDestroyed()) {
                    requester.startRiding(uav);
                } else {
                    requester.displayClientMessage(Component.translatable("tips.uav_controller.unavailable"), true);
                }
            }

            @Override
            public void onTimeout(ServerPlayer requester) {
                requester.displayClientMessage(Component.translatable("tips.uav_controller.wake_failed"), true);
            }

        });
        switch (result) {
            case WAKING -> player.displayClientMessage(Component.translatable("tips.uav_controller.reconnect"), true);
            case UNKNOWN -> player.displayClientMessage(Component.translatable("tips.uav_controller.unknown"), true);
            case OTHER_DIMENSION ->
                    player.displayClientMessage(Component.translatable("tips.uav_controller.other_dimension"), true);
            default -> {
            }
        }
    }

    @Override
    public InteractionResult interactEntity(ItemStack stack, Player player, Entity target, InteractionHand pHand) {
        if (!player.level().isClientSide) {
            if (pHand == InteractionHand.MAIN_HAND) {
                if (target instanceof AbstractVehicle vehicle && vehicle.uav && !vehicle.isDestroyed()) {
                    ItemStack uavControllerItemStack = player.getItemInHand(pHand);
                    CompoundTag tag = uavControllerItemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                    tag.putUUID(UAV_UUID, vehicle.getUUID());
                    uavControllerItemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                    player.displayClientMessage(Component.translatable("tips.uav_controller.paired"), true);
                }
            }
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    @Override
    public void inventoryTick(ItemStack pStack, Level pLevel, Entity pEntity, int pSlotId, boolean pIsSelected) {
        if (!pLevel.isClientSide && pLevel.getGameTime() % 10 == 0 && pLevel instanceof ServerLevel serverLevel) {
            CompoundTag tag = pStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            if (tag.contains(UAV_UUID)) {
                VehicleWakeupData.Entry entry = VehicleWakeup.lookup(serverLevel.getServer(), tag.getUUID(UAV_UUID));
                if (entry != null) {
                    tag.putDouble(UAV_X, entry.position().x);
                    tag.putDouble(UAV_Y, entry.position().y);
                    tag.putDouble(UAV_Z, entry.position().z);
                    tag.putString(UAV_DIMENSION, entry.dimension().location().toString());
                    pStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                }
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack pStack, Item.TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        CompoundTag tag = pStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.contains(UAV_UUID)) {
            pTooltipComponents.add(Component.translatable("tips.uav_controller", tag.getUUID(UAV_UUID).toString()).withStyle(ChatFormatting.GRAY));
            if (tag.contains(UAV_X)) {
                String posStr = String.format("%.1f, %.1f, %.1f", tag.getDouble(UAV_X), tag.getDouble(UAV_Y), tag.getDouble(UAV_Z));
                pTooltipComponents.add(Component.translatable("tips.uav_controller.uav_at", posStr).withStyle(ChatFormatting.AQUA));
            }
            if (tag.contains(UAV_DIMENSION)) {
                pTooltipComponents.add(Component.translatable("tips.uav_controller.uav_in", tag.getString(UAV_DIMENSION)).withStyle(ChatFormatting.DARK_AQUA));
            }
        }
        super.appendHoverText(pStack, pContext, pTooltipComponents, pIsAdvanced);
    }

}
