package org.ywzj.vehicle.client.render.animation;

import com.maydaymemory.mae.basic.Pose;
import com.maydaymemory.mae.control.statemachine.AnimationStateMachine;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.weapon.AbstractVehicleWeapon;

public class VehicleAnimationInstance {

    private AnimationStateMachine<VehicleContext> stateMachine;

    public VehicleAnimationInstance(AbstractVehicle vehicle) {
        ClientAssetsManager.INSTANCE.getVehicleDisplay(vehicle.getDisplayId())
                .ifPresent(display -> {
                    this.stateMachine = new AnimationStateMachine<>(
                            VehicleAnimationStates.IDLE_STATE,
                            new VehicleContext(vehicle, display.getAnimations()),
                            System::nanoTime
                    );
                });
    }

    public AnimationStateMachine<VehicleContext> getStateMachine() {
        return stateMachine;
    }

    public void tick() {
        if (stateMachine != null) {
            stateMachine.tick();
        }
    }

    public void onFire(AbstractVehicleWeapon<?> weapon) {
        stateMachine.getContext().offerAnimation(weapon.getWeaponUnit().getId() + "_shoot");
    }

    public Pose getCurrentPose() {
        return stateMachine.getPose();
    }

}
