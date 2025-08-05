package org.ywzj.vehicle.blockentity;

import com.maydaymemory.mae.control.misc.RealtimeVelocityEstimatorNode;
import com.maydaymemory.mae.control.statemachine.AnimationStateMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.ywzj.vehicle.all.AllBlockEntities;
import org.ywzj.vehicle.client.animation.TestAnimationContext;

public class TestBlockEntity extends BlockEntity {
    @OnlyIn(Dist.CLIENT)
    public AnimationStateMachine<TestAnimationContext> stateMachine;

    @OnlyIn(Dist.CLIENT)
    public RealtimeVelocityEstimatorNode velocityEstimatorNode;

    public TestBlockEntity(BlockPos pos, BlockState state) {
        super(AllBlockEntities.TEST_BLOCK_ENTITY_TYPE.get(), pos, state);
    }

    @OnlyIn(Dist.CLIENT)
    public void nextAnimation() {
        stateMachine.getContext().needTransition = true;
    }

    @OnlyIn(Dist.CLIENT)
    public void tick() {
        velocityEstimatorNode.tick();
        stateMachine.tick();
    }
}
