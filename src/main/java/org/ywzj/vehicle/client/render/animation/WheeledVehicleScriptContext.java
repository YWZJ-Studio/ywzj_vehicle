package org.ywzj.vehicle.client.render.animation;

import net.minecraft.util.Mth;
import org.jetbrains.annotations.ApiStatus;
import org.ywzj.vehicle.entity.vehicle.WheeledVehicle;
import org.ywzj.vehicle.vehicle.parts.RotatableUnit;

@SuppressWarnings("unused")
public class WheeledVehicleScriptContext {
    private WheeledVehicle vehicle;
    private float partialTick;

    public WheeledVehicleScriptContext(WheeledVehicle vehicle) {
        this.vehicle = vehicle;
    }

    @ApiStatus.Internal
    public void update(float partialTick, WheeledVehicle vehicle) {
        this.vehicle = vehicle;
        this.partialTick = partialTick;
    }

    public float getPartXRot(String id) {
        return vehicle.getPartUnit(id).map(part -> {
            if (part instanceof RotatableUnit<?> rotatable) {
                var xRot = rotatable.getXRot();
                var xRotO = rotatable.xRotO;
                return Mth.rotLerp(partialTick, xRotO, xRot);
            }
            return 0f;
        }).orElse(0f);
    }

    public float getPartYRot(String id) {
        return vehicle.getPartUnit(id).map(part -> {
            if (part instanceof RotatableUnit<?> rotatable) {
                var yRot = rotatable.getYRot();
                var yRotO = rotatable.yRotO;
                return Mth.rotLerp(partialTick, yRotO, yRot);
            }
            return 0f;
        }).orElse(0f);
    }

    public float getForwardSpeed() {
        return vehicle.getForwardSpeed();
    }

    public float getTurnAngle() {
        return vehicle.getTurnAngle();
    }

    public float getWheelRotation() {
        return vehicle.wheelRotation;
    }

    public float setWheelRotation(float rotation) {
        vehicle.wheelRotation = rotation % 360;
        return vehicle.wheelRotation;
    }

    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    public long lastRenderTime() {
        return vehicle.lastRenderTime;
    }
}
