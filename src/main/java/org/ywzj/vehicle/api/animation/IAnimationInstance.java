package org.ywzj.vehicle.api.animation;

import com.maydaymemory.mae.basic.Pose;
import com.maydaymemory.mae.control.statemachine.AnimationStateMachine;
import org.jetbrains.annotations.NotNull;
import org.ywzj.vehicle.client.render.animation.context.BaseAnimationContext;

public interface IAnimationInstance<T extends BaseAnimationContext> {

    AnimationStateMachine<T> getStateMachine(String name);

    @NotNull
    T getContext();

    void tick();

    @NotNull
    Pose getCurrentPose();

}
