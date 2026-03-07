package org.ywzj.vehicle.client.render.animation.context;

import net.minecraft.util.Mth;
import org.ywzj.vehicle.entity.vehicle.FixedWingVehicle;

public class FixedWingVehicleContext extends VehicleContext<FixedWingVehicle> {

    public FixedWingVehicleContext(FixedWingVehicle vehicle) {
        super(vehicle);
    }

    public float getThrottleLevel() {
        return entity.getThrottleLevel();
    }

    public float getPitchInput() {
        return Mth.lerp(partialTick, entity.pitchInputO, entity.pitchInput);
    }

    public float getYawInput() {
        return Mth.lerp(partialTick, entity.yawInputO, entity.yawInput);
    }

    public float getRollInput() {
        return Mth.lerp(partialTick, entity.rollInputO, entity.rollInput);
    }

}
