package org.ywzj.vehicle.item;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.registries.ForgeRegistries;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.all.AllBlocks;
import org.ywzj.vehicle.all.AllConfigs;
import org.ywzj.vehicle.block.FigureBoxBlock;
import org.ywzj.vehicle.blockentity.FigureBoxBlockEntity;
import org.ywzj.vehicle.blockentity.MachineMaxBlockEntity;
import org.ywzj.vehicle.client.render.item.FigureBoxItemRenderer;
import org.ywzj.vehicle.entity.misc.FakePlayer;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

import java.util.List;
import java.util.function.Consumer;

public class FigureBoxItem extends VehicleItem {

    public static final String ENTITY_TYPE = "entityId";
    public static final String ENTITY_DATA = "entityData";

    public FigureBoxItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {

            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                Minecraft minecraft = Minecraft.getInstance();
                if (renderer == null) {
                    renderer = new FigureBoxItemRenderer(minecraft.getBlockEntityRenderDispatcher(), minecraft.getEntityModels());
                }
                return renderer;
            }

        });
    }

    public static InteractionResult capture(ItemStack itemStack, Player player, Entity target) {
        CompoundTag tag = itemStack.getOrCreateTag();
        if (tag.contains(ENTITY_DATA)) {
            player.displayClientMessage(Component.translatable("tips.figure_box_has_entity"), true);
            return InteractionResult.FAIL;
        }
        CompoundTag entityData = new CompoundTag();
        target.saveWithoutId(entityData);
        entityData.remove("Passengers");
        tag.put(ENTITY_DATA, entityData);
        tag.putString(ENTITY_TYPE, EntityType.getKey(target.getType()).toString());
        itemStack.setTag(tag);
        target.discard();
        player.level().playSound(null, player.blockPosition(), SoundEvents.SHULKER_BOX_CLOSE, SoundSource.PLAYERS, 1.0F, 1.0F);
        player.displayClientMessage(Component.translatable("tips.figure_box_entity_saved"), true);
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult interactEntity(ItemStack itemStack, Player player, Entity target, InteractionHand hand) {
        if (!player.level().isClientSide() && hand == InteractionHand.MAIN_HAND) {
            if (target instanceof ServerPlayer || target instanceof FakePlayer) {
                return InteractionResult.PASS;
            }
            if (AllConfigs.common.figureBoxOnlyCaptureVehicle.get() && !(target instanceof AbstractVehicle)) {
                return InteractionResult.PASS;
            }
            String entityId = EntityType.getKey(target.getType()).toString();
            if (AllConfigs.figureBoxCaptureBlacklist.contains(entityId)) {
                return InteractionResult.PASS;
            }
            return capture(itemStack, player, target);
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack itemStack = context.getItemInHand();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        CompoundTag tag = itemStack.getOrCreateTag();
        if (!tag.contains(ENTITY_DATA)) {
            // 若右击打印机方块
            if (level.getBlockEntity(new BlockPos(context.getClickedPos())) instanceof MachineMaxBlockEntity machineMaxBlockEntity) {
                if (machineMaxBlockEntity.hasProduct()) {
                    AbstractVehicle vehicle = machineMaxBlockEntity.takeProduct();
                    CompoundTag entityData = new CompoundTag();
                    vehicle.saveWithoutId(entityData);
                    tag.put(ENTITY_DATA, entityData);
                    tag.putString(ENTITY_TYPE, EntityType.getKey(vehicle.getType()).toString());
                    itemStack.setTag(tag);
                    player.level().playSound(null, player.blockPosition(), SoundEvents.SHULKER_BOX_CLOSE, SoundSource.PLAYERS, 1.0F, 1.0F);
                    player.displayClientMessage(Component.translatable("tips.figure_box_entity_saved"), true);
                    return InteractionResult.SUCCESS;
                }
            }
            // 尝试抓取地面掉落物
            AABB searchArea = new AABB(context.getClickedPos()).inflate(1.0D);
            List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, searchArea);
            if (items.isEmpty()) {
                player.displayClientMessage(Component.translatable("tips.figure_box_empty"), true);
                return InteractionResult.FAIL;
            }
            return capture(itemStack, player, items.get(0));
        }
        String entityType = tag.getString(ENTITY_TYPE);
        CompoundTag entityData = tag.getCompound(ENTITY_DATA);
        BlockPos pos = context.getClickedPos().above();
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(YwzjVehicle.resourceLocation(entityType));
        if (type != null) {
            Entity entity = type.create(level);
            entity.load(entityData);
            if (player.isShiftKeyDown()) {
                // 放置手办盒
                BlockState blockState = player.level().getBlockState(pos);
                if (blockState.canBeReplaced()) {
                    player.level().setBlock(pos, AllBlocks.FIGURE_BOX_BLOCK.get().defaultBlockState()
                            .setValue(FigureBoxBlock.FACING, Direction.fromYRot(player.getYRot()).getOpposite()), 1);
                    BlockEntity blockEntity = level.getBlockEntity(pos);
                    if (blockEntity instanceof FigureBoxBlockEntity figureBoxBlockEntity) {
                        figureBoxBlockEntity.setEntity(entity);
                        figureBoxBlockEntity.setChanged();
                        player.getItemInHand(context.getHand()).shrink(1);
                        return InteractionResult.SUCCESS;
                    }
                } else {
                    player.displayClientMessage(Component.translatable("tips.figure_box_place_failed"), true);
                    return InteractionResult.FAIL;
                }
            } else {
                // 释放内容物
                entity.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getYRot(), 0);
                level.addFreshEntity(entity);
                tag.remove(ENTITY_DATA);
                tag.remove(ENTITY_TYPE);
                itemStack.setTag(tag);
                player.level().playSound(null, player.blockPosition(), SoundEvents.SHULKER_BOX_OPEN, SoundSource.PLAYERS, 1.0F, 1.0F);
                player.displayClientMessage(Component.translatable("tips.figure_box_release_entity"), true);
                return InteractionResult.SUCCESS;
            }
        }
        player.displayClientMessage(Component.translatable("tips.figure_box_entity_generate_failed"), true);
        return InteractionResult.FAIL;
    }

    @Override
    public void appendHoverText(ItemStack itemStack, Level level, List<Component> tooltip, TooltipFlag flag) {
        if (itemStack.hasTag() && itemStack.getOrCreateTag().contains(ENTITY_TYPE)) {
            tooltip.add(Component.translatable("tips.figure_box_with_entity").append(itemStack.getTag().getString(ENTITY_TYPE)));
        }
    }

}
