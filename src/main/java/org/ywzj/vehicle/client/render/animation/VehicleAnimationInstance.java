package org.ywzj.vehicle.client.render.animation;

import com.maydaymemory.mae.basic.Pose;
import com.maydaymemory.mae.control.statemachine.AnimationStateMachine;
import org.ywzj.vehicle.api.animation.IAnimationInstance;
import org.ywzj.vehicle.client.render.animation.context.BaseAnimationContext;
import org.ywzj.vehicle.client.render.animation.context.VehicleContext;
import org.ywzj.vehicle.client.render.animation.controller.AnimationController;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.weapon.AbstractVehicleWeapon;

import java.util.HashMap;
import java.util.Map;

public class VehicleAnimationInstance<T extends BaseAnimationContext> implements IAnimationInstance<T> {

    private Map<String, AnimationStateMachine<T>> stateMachines = new HashMap<>();
    private AnimationController<T> controller;
    private T context;

    public VehicleAnimationInstance(AbstractVehicle vehicle) {
        ClientAssetsManager.INSTANCE.getVehicleDisplay(vehicle.getDisplayId())
            .ifPresent(display -> {
                context = new VehicleContext(vehicle, display.getAnimations());
                stateMachines = controller.initialize(context);
            });


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
    }

    public void onFire(AbstractVehicleWeapon<?> weapon) {
        stateMachine.getContext().offerAnimation(weapon.getWeaponUnit().getId() + "_shoot");
    }

    public Pose getCurrentPose() {
        return controller.getPoseGraph().evaluate();
    }

}
