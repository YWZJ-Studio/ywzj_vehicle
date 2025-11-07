package org.ywzj.vehicle.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.registries.ForgeRegistries;
import org.ywzj.vehicle.all.AllBlocks;
import org.ywzj.vehicle.block.FigureBoxBlock;
import org.ywzj.vehicle.blockentity.FigureBoxBlockEntity;

import java.util.List;

public class FigureBoxItem extends VehicleItem {

    public FigureBoxItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResult interactEntity(ItemStack itemStack, Player player, Entity target, InteractionHand hand) {
        if (!player.level().isClientSide && hand == InteractionHand.MAIN_HAND) {
            CompoundTag tag = itemStack.getOrCreateTag();
            if (tag.contains("entityData")) {
                player.displayClientMessage(Component.translatable("tips.figure_box_has_entity"), true);
                return InteractionResult.FAIL;
            }
            CompoundTag entityData = new CompoundTag();
            target.saveWithoutId(entityData);
            tag.put("entityData", entityData);
            tag.putString("entityId", EntityType.getKey(target.getType()).toString());
            itemStack.setTag(tag);
            target.remove(Entity.RemovalReason.DISCARDED);
            player.displayClientMessage(Component.translatable("tips.figure_box_entity_saved"), true);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack itemStack = context.getItemInHand();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        CompoundTag tag = itemStack.getTag();
        if (tag == null || !tag.contains("entityData")) {
            player.displayClientMessage(Component.translatable("tips.figure_box_empty"), true);
            return InteractionResult.FAIL;
        }
        String id = tag.getString("entityId");
        CompoundTag entityData = tag.getCompound("entityData");
        BlockPos pos = context.getClickedPos().above();
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(id));
        if (type != null) {
            Entity entity = type.create(level);
            entity.load(entityData);
            if (player.isShiftKeyDown()) {
                player.level().setBlock(pos, AllBlocks.FIGURE_BOX_BLOCK.get().defaultBlockState()
                        .setValue(FigureBoxBlock.FACING, Direction.fromYRot(player.getYRot()).getOpposite()),
                        1);
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof FigureBoxBlockEntity figureBoxBlockEntity) {
                    figureBoxBlockEntity.setEntity(entity);
                    figureBoxBlockEntity.setChanged();
                    player.getItemInHand(context.getHand()).shrink(1);
                    return InteractionResult.SUCCESS;
                }
            } else {
                entity.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getYRot(), 0);
                level.addFreshEntity(entity);
                tag.remove("entityData");
                tag.remove("entityId");
                itemStack.setTag(tag);
                player.displayClientMessage(Component.translatable("tips.figure_box_release_entity"), true);
                return InteractionResult.SUCCESS;
            }
        }
        player.displayClientMessage(Component.translatable("tips.figure_box_entity_generate_failed"), true);
        return InteractionResult.FAIL;
    }

    @Override
    public void appendHoverText(ItemStack itemStack, Level level, List<Component> tooltip, TooltipFlag flag) {
        if (itemStack.hasTag() && itemStack.getTag().contains("entityId")) {
            tooltip.add(Component.translatable("tips.figure_box_with_entity").append(itemStack.getTag().getString("entityId")));
        }
    }

}
