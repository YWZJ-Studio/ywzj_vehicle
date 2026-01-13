package org.ywzj.vehicle.block;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.ywzj.vehicle.all.AllBlockEntities;
import org.ywzj.vehicle.all.AllItems;
import org.ywzj.vehicle.blockentity.MachineMaxBlockEntity;

import javax.annotation.Nullable;

public class MachineMaxBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public MachineMaxBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide && hand == InteractionHand.MAIN_HAND) {
            if (player.getItemInHand(hand).getItem().equals(AllItems.FIGURE_BOX.get())) {
                return InteractionResult.PASS;
            }
            if (level.getBlockEntity(pos) instanceof MachineMaxBlockEntity machineMaxBlockEntity) {
                open(machineMaxBlockEntity);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @OnlyIn(Dist.CLIENT)
    public void open(MachineMaxBlockEntity machineMaxBlockEntity) {
        Minecraft.getInstance().setScreen(new org.ywzj.vehicle.client.screen.MachineMaxScreen(machineMaxBlockEntity));
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {
        return createTickerHelper(type, AllBlockEntities.MACHINE_MAX_BLOCK_ENTITY.get(), MachineMaxBlockEntity::tick);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineMaxBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return this.defaultBlockState().setValue(FACING, pContext.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState pState, Rotation pRotation) {
        return pState.setValue(FACING, pRotation.rotate(pState.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState pState, Mirror pMirror) {
        return pState.rotate(pMirror.getRotation(pState.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING);
    }

}
