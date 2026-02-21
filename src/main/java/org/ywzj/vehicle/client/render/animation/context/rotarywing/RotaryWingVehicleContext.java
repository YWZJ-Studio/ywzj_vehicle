package org.ywzj.vehicle.client.render.animation.context.rotarywing;

import org.ywzj.vehicle.client.render.animation.context.VehicleContext;
import org.ywzj.vehicle.entity.vehicle.RotaryWingVehicle;

public class RotaryWingVehicleContext extends VehicleContext<RotaryWingVehicle> {

    public RotaryWingVehicleContext(RotaryWingVehicle vehicle) {
        super(vehicle);
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public float getBindingValue(String source, Float param) {
        float paramValue = param != null ? param : 0f;
        
        return switch (source) {
            default -> super.getBindingValue(source, param);
        };
    }

}
