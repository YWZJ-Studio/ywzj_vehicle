package org.ywzj.vehicle.client.render.animation;

import com.maydaymemory.mae.basic.DummyPose;
import com.maydaymemory.mae.basic.Pose;
import com.maydaymemory.mae.control.statemachine.AnimationStateMachine;
import org.jetbrains.annotations.NotNull;
import org.ywzj.vehicle.api.animation.IAnimationInstance;
import org.ywzj.vehicle.client.render.animation.context.BaseAnimationContext;
import org.ywzj.vehicle.client.render.animation.controller.AnimationController;

import java.util.Map;

public class VehicleAnimationInstance<T extends BaseAnimationContext> implements IAnimationInstance<T> {

    private final Map<String, AnimationStateMachine<T>> stateMachines;
    private final AnimationController<T> controller;
    private final T context;

    public VehicleAnimationInstance(AnimationController<T> controller, T context) {
        this.controller = controller;
        this.context = context;
        this.stateMachines = controller.initialize(context);
    }

    public AnimationStateMachine<T> getStateMachine(String name) {
        return stateMachines.get(name);
    }

    public T getContext() {
        return context;
    }

    public void tick() {
        for (var value : stateMachines.values()) {
            value.tick();
        }
        if (context != null) {
            context.tick();
        }
    }

    @NotNull
    public Pose getCurrentPose() {
        Pose pose = controller.getPoseGraph().evaluate(this);
        return pose != null ? pose : DummyPose.INSTANCE;
    }

}
