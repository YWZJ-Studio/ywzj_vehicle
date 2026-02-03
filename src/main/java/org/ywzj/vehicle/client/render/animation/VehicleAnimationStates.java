package org.ywzj.vehicle.client.render.animation;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.SimpleAnimationState;

public class VehicleAnimationStates {

    public static final SimpleAnimationState<VehicleContext> IDLE_STATE;
    static {
        IDLE_STATE = new SimpleAnimationState.Builder<VehicleContext>()
                .onUpdate(VehicleContext::consumeAnimation)
                .evaluatePose(EntityContext::evaluatePose)
                .build();
    }

}
