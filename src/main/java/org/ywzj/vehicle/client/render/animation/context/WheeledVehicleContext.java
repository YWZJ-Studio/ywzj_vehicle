package org.ywzj.vehicle.client.render.animation.context;

import net.minecraft.util.Mth;
import org.ywzj.vehicle.entity.vehicle.WheeledVehicle;
import org.ywzj.vehicle.vehicle.part.SuspensionUnit;

public class WheeledVehicleContext extends VehicleContext<WheeledVehicle> {

    private static final float AIRBORNE_WHEEL_DAMPING = 2f;
    private float wheelRollingSpeed = 0f;
    private float wheelRotation = 0f;
    private float steeringAngle = 0f;

    public WheeledVehicleContext(WheeledVehicle vehicle) {
        super(vehicle);
    }

    public float getForwardSpeed() {
        return entity.getForwardSpeed();
    }

    public float getTurnAngle() {
        return Mth.lerp(partialTick, entity.turnAngleO, entity.turnAngle);
    }

    @Override
    public void tick() {
        super.tick();
        this.advanceWheelRotation();
        this.updateSteeringAngle();
    }

    private void advanceWheelRotation() {
        long lastTime = this.lastRenderTime();
        if (lastTime == 0) {
            return;
        }
        float deltaTime = Math.max(0f, Math.min((this.currentTimeMillis() - lastTime) / 1000f, 0.1f));
        boolean grounded = entity.hasSuspension()
                ? entity.getSuspensionUnits().stream().anyMatch(SuspensionUnit::isGrounded)
                : entity.onGround();
        float distance;
        if (grounded) {
            wheelRollingSpeed = this.getForwardSpeed();
            distance = wheelRollingSpeed * deltaTime * 20;
        } else {
            float previousSpeed = wheelRollingSpeed;
            wheelRollingSpeed *= (float) Math.exp(-AIRBORNE_WHEEL_DAMPING * deltaTime);
            distance = (previousSpeed - wheelRollingSpeed) / AIRBORNE_WHEEL_DAMPING * 20;
        }
        wheelRotation += distance * 360f;
    }

    private void updateSteeringAngle() {
        steeringAngle = -this.getTurnAngle() * 16f;
    }

    /**
     * 获取轮胎旋转角度（用于驱动轮）
     * @param radius 轮胎半径
     * @return 旋转角度（度）
     */
    public float getWheelDegrees(float radius) {
        // 根据半径调整累积的旋转角度
        float circumference = (float) (2 * Math.PI * radius);
        return (wheelRotation / circumference) % 360f;
    }

    /**
     * 获取累积的轮胎旋转角度
     */
    public float getWheelRotation() {
        return wheelRotation;
    }

    /**
     * 获取转向角度（用于转向轮）
     */
    public float getSteeringAngle() {
        return steeringAngle;
    }
    
    /**
     * 获取带角度限制的转向角度
     * @param maxAngle 最大转向角度（度）
     * @return 限制后的转向角度（度）
     */
    public float getSteeringAngle(float maxAngle) {
        return Math.max(-maxAngle, Math.min(maxAngle, steeringAngle));
    }

    @Override
    public float getBindingValue(String source, Float param) {
        float paramValue = param != null ? param : 0f;
        return switch (source) {
            case "wheel_rotation" -> getWheelDegrees(paramValue);
            case "steering_angle" -> getSteeringAngle();
            default -> super.getBindingValue(source, param);
        };
    }

}
