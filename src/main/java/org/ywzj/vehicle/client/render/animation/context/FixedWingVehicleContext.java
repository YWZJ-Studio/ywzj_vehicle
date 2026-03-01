package org.ywzj.vehicle.client.render.animation.context;

import org.ywzj.vehicle.entity.vehicle.FixedWingVehicle;

public class FixedWingVehicleContext extends VehicleContext<FixedWingVehicle> {

    public FixedWingVehicleContext(FixedWingVehicle vehicle) {
        super(vehicle);
    }

    public float getThrottleLevel() {
        return entity.getThrottleLevel();
    }

}
