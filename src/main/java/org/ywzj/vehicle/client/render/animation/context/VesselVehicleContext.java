package org.ywzj.vehicle.client.render.animation.context;

import net.minecraft.util.Mth;
import org.ywzj.vehicle.entity.vehicle.VesselVehicle;

public class VesselVehicleContext extends VehicleContext<VesselVehicle> {

    public VesselVehicleContext(VesselVehicle vehicle) {
        super(vehicle);
    }

    public float getForwardSpeed() {
        return entity.getForwardSpeed();
    }

    public float getTurnAngle() {
        return Mth.lerp(partialTick, entity.turnAngleO, entity.turnAngle);
    }

    @Override
    public float getBindingValue(String source, Float param) {
        return switch (source) {
            case "steering_angle" -> -getTurnAngle() * 16f;
            default -> super.getBindingValue(source, param);
        };
    }

}
