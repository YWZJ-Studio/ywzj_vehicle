package org.ywzj.vehicle.client.render.animation.context.rotarywing;

import org.ywzj.vehicle.client.render.animation.context.VehicleContext;
import org.ywzj.vehicle.entity.vehicle.RotaryWingVehicle;

public class RotaryWingVehicleContext extends VehicleContext<RotaryWingVehicle> {

    public RotaryWingVehicleContext(RotaryWingVehicle vehicle) {
        super(vehicle);
    }

    public float getCollectivePitch() {
        return entity.getCollectivePitch();
    }

}
