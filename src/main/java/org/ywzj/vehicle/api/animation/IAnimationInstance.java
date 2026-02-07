package org.ywzj.vehicle.api.animation;

import com.maydaymemory.mae.control.statemachine.AnimationStateMachine;
import org.ywzj.vehicle.client.render.animation.context.BaseAnimationContext;

public interface IAnimationInstance<T extends BaseAnimationContext> {
    AnimationStateMachine<T> getStateMachine(String name);
    T getContext();
}
