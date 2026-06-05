package org.ywzj.vehicle.client.render.animation.context;

import net.minecraft.util.Mth;
import org.ywzj.vehicle.entity.vehicle.FixedWingVehicle;
import org.ywzj.vehicle.vehicle.part.AfterburnerUnit;

public class FixedWingVehicleContext extends VehicleContext<FixedWingVehicle> {

    public FixedWingVehicleContext(FixedWingVehicle vehicle) {
        super(vehicle);
    }

    public float getThrottleLevel() {
        return Mth.lerp(partialTick, entity.throttleLevelO, entity.throttleLevel);
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

    public void setAfterburnerRotation(int afterburnerIndex, float xRot, float yRot) {
        if (afterburnerIndex >= 0 && afterburnerIndex < entity.afterburnerUnits.size()) {
            AfterburnerUnit afterburnerUnit = entity.afterburnerUnits.get(afterburnerIndex);
            afterburnerUnit.setXRot(xRot);
            afterburnerUnit.setYRot(yRot);
        }
    }

}
