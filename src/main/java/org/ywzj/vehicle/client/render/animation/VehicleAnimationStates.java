package org.ywzj.vehicle.client.render.animation;

public class VehicleAnimationStates {

    public static final SimpleAnimationState<VehicleContext> IDLE_STATE;

    static {
        // 先定义所有状态
        IDLE_STATE = new SimpleAnimationState.Builder<VehicleContext>()
                .onUpdate((ctx) -> {
                    if (ctx.consumeShoot()) {
                        ctx.playAnimation("shoot");
                    }
                })
                .evaluatePose(EntityContext::evaluatePose)
                .build();

    }
}
