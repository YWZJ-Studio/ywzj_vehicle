package org.ywzj.vehicle.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.ywzj.vehicle.entity.vehicle.Quadcopter;

public class UavControllerItem extends Item {

    public UavControllerItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand pHand) {
        ItemStack uavControllerItemStack = player.getItemInHand(pHand);
        if (!level.isClientSide) {
            CompoundTag tag = uavControllerItemStack.getOrCreateTag();
            if (tag.contains("uavId")) {
                int uavId = tag.getInt("uavId");
                Entity entity = level.getEntity(uavId);
                if (entity instanceof Quadcopter quadcopter) {
                    if (player.startRiding(quadcopter)) {
                        quadcopter.onEnterVehicle(player);
                    }
                }
            }
        }
        return InteractionResultHolder.sidedSuccess(uavControllerItemStack, level.isClientSide());
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand pHand) {
        if (!player.level().isClientSide) {
            if (pHand == InteractionHand.MAIN_HAND) {
                if (target instanceof Quadcopter quadcopter) {
                    ItemStack uavControllerItemStack = player.getItemInHand(pHand);
                    CompoundTag tag = uavControllerItemStack.getOrCreateTag();
                    tag.putInt("uavId", quadcopter.getId());
                    uavControllerItemStack.setTag(tag);
                    // todo
                    player.displayClientMessage(Component.literal("绑定了无人机"), true);
                }
            }
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

}
