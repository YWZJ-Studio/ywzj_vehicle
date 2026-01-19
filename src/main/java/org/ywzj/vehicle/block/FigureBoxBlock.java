package org.ywzj.vehicle.block;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.ywzj.vehicle.all.AllItems;
import org.ywzj.vehicle.blockentity.FigureBoxBlockEntity;

import java.util.List;

public class FigureBoxBlock extends HorizontalEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

    public FigureBoxBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH).setValue(OPEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof FigureBoxBlockEntity figureBoxBlockEntity) {
            if (level.isClientSide) {
                openScreen(figureBoxBlockEntity);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        if (builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof FigureBoxBlockEntity figureBoxBlockEntity) {
            ItemStack itemStack = AllItems.FIGURE_BOX.get().getDefaultInstance().copy();
            Entity entity = figureBoxBlockEntity.getEntity();
            if (entity != null) {
                CompoundTag tag = itemStack.getOrCreateTag();
                CompoundTag entityData = new CompoundTag();
                entity.saveWithoutId(entityData);
                tag.put("entityData", entityData);
                tag.putString("entityId", EntityType.getKey(entity.getType()).toString());
                itemStack.setTag(tag);
            }
            return List.of(itemStack);
        }
        return List.of();
    }

    @OnlyIn(Dist.CLIENT)
    public void openScreen(FigureBoxBlockEntity figureBoxBlockEntity) {
        Minecraft.getInstance().setScreen(new org.ywzj.vehicle.client.screen.FigureBoxScreen(figureBoxBlockEntity));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        FigureBoxBlockEntity figureBoxBlockEntity = new FigureBoxBlockEntity(pos, state);
        float yRot = 0f;
        if (!figureBoxBlockEntity.getBlockState().isAir()) {
            Direction facing = figureBoxBlockEntity.getBlockState().getValue(FigureBoxBlock.FACING);
            switch (facing) {
                case NORTH -> yRot = 180f;
                case SOUTH -> yRot = 0f;
                case WEST -> yRot = -90f;
                case EAST -> yRot = 90f;
                default -> yRot = 0f;
            }
        }
        yRot -= 45;
        figureBoxBlockEntity.yRot = yRot;
        return figureBoxBlockEntity;
    }

}
