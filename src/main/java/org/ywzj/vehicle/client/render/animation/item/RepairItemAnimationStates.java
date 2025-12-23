package org.ywzj.vehicle.client.render.animation.item;


import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.SimpleAnimationState;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.SimpleTransition;

public class RepairItemAnimationStates {
    // 主轨道
    public static class Main {
        public static final SimpleAnimationState<RepairItemContext> INIT_STATE = new SimpleAnimationState.Builder<RepairItemContext>()
                .onEnter((ctx, fromState) -> {
                    ctx.playAnimation("draw");
                })
                .onUpdate(ctx -> {
                    var runner = ctx.getRunner();
                    if (runner == null || runner.getAnimationContext().getProgressInSecond() / runner.getMaxProgressInSecond() > 0.5) {
                        ctx.setInited(true);
                    }
                })
                .evaluatePose(RepairItemContext::evaluatePose)
                .build();

        public static final SimpleAnimationState<RepairItemContext> STATIC_IDLE_STATE = new SimpleAnimationState.Builder<RepairItemContext>()
                .onEnter((ctx, fromState) -> {
                    ctx.playAnimationLoop("static_idle");
                })
                .evaluatePose(RepairItemContext::evaluatePose)
                .build();

        public static final SimpleAnimationState<RepairItemContext> USING_STATE = new SimpleAnimationState.Builder<RepairItemContext>()
                .onEnter((ctx, fromState) -> {
                    ctx.playAnimationLoop("use");
                })
                .evaluatePose(RepairItemContext::evaluatePose)
                .build();

        public static final SimpleTransition<RepairItemContext> FINISH_INIT = new SimpleTransition.Builder<RepairItemContext>()
                .from(INIT_STATE)
                .target(STATIC_IDLE_STATE)
                .predicate((ctx) -> {
                    var runner = ctx.getRunner();
                    return runner == null || runner.getAnimationContext().isEnd();
                })
                .build();

        public static final SimpleTransition<RepairItemContext> TO_USE = new SimpleTransition.Builder<RepairItemContext>()
                .from(STATIC_IDLE_STATE)
                .target(USING_STATE)
                .predicate(RepairItemContext::isUsingItem)
                .afterTrigger(ctx ->{
                    ctx.playAnimation("turn_to_use");
                })
                .duration(0.5f)
                .build();

        public static final SimpleTransition<RepairItemContext> END_USE = new SimpleTransition.Builder<RepairItemContext>()
                .from(USING_STATE)
                .target(STATIC_IDLE_STATE)
                .predicate(ctx -> !ctx.isUsingItem())
                .afterTrigger(ctx ->{
                    ctx.playAnimation("turn_to_idle");
                })
                .duration(0.5f)
                .build();
    }
}
