package org.ywzj.vehicle.item;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
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
    private static final String DISPLAY_DATA = "figureBoxDisplay";
    private static final String OPEN = "open";
    private static final String SCALE = "scale";
    private static final String X_SHIFT = "xShift";
    private static final String Y_SHIFT = "yShift";
    private static final String Z_SHIFT = "zShift";
    private static final String X_ROT = "xRot";
    private static final String Y_ROT = "yRot";
    private static final String Z_ROT = "zRot";

    public FigureBoxItem(Properties pProperties) {
        super(pProperties);
    }

    public static void saveDisplayData(CompoundTag itemTag, FigureBoxBlockEntity figureBox) {
        CompoundTag displayData = new CompoundTag();
        displayData.putBoolean(OPEN, figureBox.open);
        displayData.putFloat(SCALE, figureBox.scale);
        displayData.putFloat(X_SHIFT, figureBox.xShift);
        displayData.putFloat(Y_SHIFT, figureBox.yShift);
        displayData.putFloat(Z_SHIFT, figureBox.zShift);
        displayData.putFloat(X_ROT, figureBox.xRot);
        displayData.putFloat(Y_ROT, figureBox.yRot);
        displayData.putFloat(Z_ROT, figureBox.zRot);
        itemTag.put(DISPLAY_DATA, displayData);
    }

    public static void loadDisplayData(CompoundTag itemTag, FigureBoxBlockEntity figureBox) {
        if (!itemTag.contains(DISPLAY_DATA, Tag.TAG_COMPOUND)) {
            return;
        }
        CompoundTag displayData = itemTag.getCompound(DISPLAY_DATA);
        figureBox.open = displayData.getBoolean(OPEN);
        figureBox.scale = displayData.getFloat(SCALE);
        figureBox.xShift = displayData.getFloat(X_SHIFT);
        figureBox.yShift = displayData.getFloat(Y_SHIFT);
        figureBox.zShift = displayData.getFloat(Z_SHIFT);
        figureBox.xRot = displayData.getFloat(X_ROT);
        figureBox.yRot = displayData.getFloat(Y_ROT);
        figureBox.zRot = displayData.getFloat(Z_ROT);
    }

    @Override
    @SuppressWarnings("removal")
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
        CompoundTag tag = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.contains(ENTITY_DATA)) {
            player.displayClientMessage(Component.translatable("tips.figure_box_has_entity"), true);
            return InteractionResult.FAIL;
        }
        CompoundTag entityData = new CompoundTag();
        target.saveWithoutId(entityData);
        entityData.remove("Passengers");
        tag.put(ENTITY_DATA, entityData);
        tag.putString(ENTITY_TYPE, EntityType.getKey(target.getType()).toString());
        itemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        target.discard();
        player.level().playSound(null, player.blockPosition(), SoundEvents.SHULKER_BOX_CLOSE, SoundSource.PLAYERS, 1.0F, 1.0F);
        player.displayClientMessage(Component.translatable("tips.figure_box_entity_saved"), true);
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult interactEntity(ItemStack itemStack, Player player, Entity target, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || target instanceof Player || target instanceof FakePlayer) {
            return InteractionResult.PASS;
        }
        if (player.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (AllConfigs.common.figureBoxOnlyCaptureVehicle.get() && !(target instanceof AbstractVehicle)) {
            return InteractionResult.PASS;
        }
        if (target instanceof AbstractVehicle vehicle
                && vehicle.getPassengers().stream().anyMatch(entity -> entity instanceof Player)) {
            return InteractionResult.PASS;
        }
        String entityId = EntityType.getKey(target.getType()).toString();
        if (AllConfigs.figureBoxCaptureBlacklist.contains(entityId)) {
            return InteractionResult.PASS;
        }
        return capture(itemStack, player, target);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.pass(itemStack);
        }
        HitResult hitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(itemStack);
        }
        if (level.isClientSide()) {
            return InteractionResultHolder.sidedSuccess(itemStack, true);
        }
        BlockPos hitPos = ((BlockHitResult) hitResult).getBlockPos();
        CompoundTag tag = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(ENTITY_DATA)) {
            // 若右击打印机方块
            if (level.getBlockEntity(hitPos) instanceof MachineMaxBlockEntity machineMaxBlockEntity) {
                if (machineMaxBlockEntity.hasProduct()) {
                    AbstractVehicle vehicle = machineMaxBlockEntity.takeProduct();
                    CompoundTag entityData = new CompoundTag();
                    vehicle.saveWithoutId(entityData);
                    tag.put(ENTITY_DATA, entityData);
                    tag.putString(ENTITY_TYPE, EntityType.getKey(vehicle.getType()).toString());
                    itemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                    player.level().playSound(null, player.blockPosition(), SoundEvents.SHULKER_BOX_CLOSE, SoundSource.PLAYERS, 1.0F, 1.0F);
                    player.displayClientMessage(Component.translatable("tips.figure_box_entity_saved"), true);
                    return InteractionResultHolder.sidedSuccess(itemStack, false);
                }
            }
            // 尝试抓取地面掉落物
            AABB searchArea = new AABB(hitPos).inflate(1.0D);
            List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, searchArea);
            if (items.isEmpty()) {
                player.displayClientMessage(Component.translatable("tips.figure_box_empty"), true);
                return InteractionResultHolder.fail(itemStack);
            }
            return new InteractionResultHolder<>(capture(itemStack, player, items.get(0)), itemStack);
        }
        String entityType = tag.getString(ENTITY_TYPE);
        CompoundTag entityData = tag.getCompound(ENTITY_DATA);
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(YwzjVehicle.resourceLocation(entityType));
        if (type != null) {
            Entity entity = type.create(level);
            if (entity != null) {
                entity.load(entityData);
                if (player.isShiftKeyDown()) {
                    // 放置手办盒
                    BlockPos pos = hitPos.above();
                    BlockState blockState = level.getBlockState(pos);
                    if (blockState.canBeReplaced()) {
                        level.setBlock(pos, AllBlocks.FIGURE_BOX_BLOCK.get().defaultBlockState()
                                .setValue(FigureBoxBlock.FACING, Direction.fromYRot(player.getYRot()).getOpposite()), 1);
                        BlockEntity blockEntity = level.getBlockEntity(pos);
                        if (blockEntity instanceof FigureBoxBlockEntity figureBoxBlockEntity) {
                            figureBoxBlockEntity.setEntity(entity);
                            loadDisplayData(tag, figureBoxBlockEntity);
                            figureBoxBlockEntity.setChanged();
                            level.setBlockAndUpdate(pos, figureBoxBlockEntity.getBlockState()
                                    .setValue(FigureBoxBlock.OPEN, figureBoxBlockEntity.open));
                            itemStack.shrink(1);
                            return InteractionResultHolder.sidedSuccess(itemStack, false);
                        }
                    }
                    player.displayClientMessage(Component.translatable("tips.figure_box_place_failed"), true);
                    return InteractionResultHolder.fail(itemStack);
                }
                // 释放内容物
                Vec3 position = hitResult.getLocation();
                entity.moveTo(position.x, position.y, position.z, player.getYRot(), 0);
                if (level.addFreshEntity(entity)) {
                    tag.remove(ENTITY_DATA);
                    tag.remove(ENTITY_TYPE);
                    itemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                    level.playSound(null, player.blockPosition(), SoundEvents.SHULKER_BOX_OPEN, SoundSource.PLAYERS, 1.0F, 1.0F);
                    player.displayClientMessage(Component.translatable("tips.figure_box_release_entity"), true);
                    return InteractionResultHolder.sidedSuccess(itemStack, false);
                }
            }
        }
        player.displayClientMessage(Component.translatable("tips.figure_box_entity_generate_failed"), true);
        return InteractionResultHolder.fail(itemStack);
    }

    @Override
    public void appendHoverText(ItemStack itemStack, Item.TooltipContext level, List<Component> tooltip, TooltipFlag flag) {
        if (itemStack.has(DataComponents.CUSTOM_DATA) && itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().contains(ENTITY_TYPE)) {
            tooltip.add(Component.translatable("tips.figure_box_with_entity").append(itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString(ENTITY_TYPE)));
        }
    }

}
