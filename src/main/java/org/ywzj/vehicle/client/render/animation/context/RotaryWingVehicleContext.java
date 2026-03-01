package org.ywzj.vehicle.client.render.animation.context;

import org.ywzj.vehicle.entity.vehicle.RotaryWingVehicle;

public class RotaryWingVehicleContext extends VehicleContext<RotaryWingVehicle> {

    public RotaryWingVehicleContext(RotaryWingVehicle vehicle) {
        super(vehicle);
    }

    public float getCollectivePitch() {
        return entity.getCollectivePitch();
    }

}
