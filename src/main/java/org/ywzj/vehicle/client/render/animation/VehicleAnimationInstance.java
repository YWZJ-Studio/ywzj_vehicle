package org.ywzj.vehicle.client.render.animation;

import com.maydaymemory.mae.basic.Pose;
import com.maydaymemory.mae.control.statemachine.AnimationStateMachine;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

public class VehicleAnimationInstance {

    private AnimationStateMachine<VehicleContext> stateMachine;

    public VehicleAnimationInstance(AbstractVehicle vehicle) {
        ClientAssetsManager.INSTANCE.getVehicleDisplay(vehicle.getVehicleId())
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
        stateMachine.tick();
    }

    public void onFire() {
        stateMachine.getContext().setShouldShoot(true);
    }

    public Pose getCurrentPose() {
        return stateMachine.getPose();
    }

}
