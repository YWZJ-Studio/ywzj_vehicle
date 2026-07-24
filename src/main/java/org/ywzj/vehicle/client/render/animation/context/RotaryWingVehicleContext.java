package org.ywzj.vehicle.client.render.animation.context;

import net.minecraft.util.Mth;
import org.ywzj.vehicle.entity.vehicle.RotaryWingVehicle;

public class RotaryWingVehicleContext extends VehicleContext<RotaryWingVehicle> {

    public RotaryWingVehicleContext(RotaryWingVehicle vehicle) {
        super(vehicle);
    }

    public float getCollectivePitch() {
        return Mth.lerp(partialTick, entity.collectivePitchO, entity.collectivePitch);
    }

    public float getPitchInput() {
        return Mth.lerp(partialTick, entity.pitchInputO, entity.pitchInput);
    }

    public float getRollInput() {
        return Mth.lerp(partialTick, entity.rollInputO, entity.rollInput);
    }

    public float getCargoRopeLength() {
        return entity.cargoRope == null ? 0 : entity.cargoRope.getLength();
    }

}
