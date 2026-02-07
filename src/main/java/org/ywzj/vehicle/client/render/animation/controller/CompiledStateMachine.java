package org.ywzj.vehicle.client.render.animation.controller;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.SimpleAnimationState;
import com.maydaymemory.mae.control.statemachine.AnimationStateMachine;
import com.maydaymemory.mae.util.LongSupplier;
import org.ywzj.vehicle.client.render.animation.context.BaseAnimationContext;

public record CompiledStateMachine<T extends BaseAnimationContext>(
        String name,
        SimpleAnimationState<T> entryState
) {
    public AnimationStateMachine<T> createInstance(T context, LongSupplier timeSupplier) {
        return new AnimationStateMachine<>(entryState, context, timeSupplier);
    }
}
