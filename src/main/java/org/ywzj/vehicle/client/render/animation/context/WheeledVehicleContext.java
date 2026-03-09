package org.ywzj.vehicle.client.render.animation.context;

import org.ywzj.vehicle.entity.vehicle.WheeledVehicle;

public class WheeledVehicleContext extends VehicleContext<WheeledVehicle> {

    private float wheelRotation = 0f;
    private float steeringAngle = 0f;

    public WheeledVehicleContext(WheeledVehicle vehicle) {
        super(vehicle);
    }

    public float getForwardSpeed() {
        return entity.getForwardSpeed();
    }

    public float getTurnAngle() {
        return entity.getTurnAngle();
    }

    @Override
    public void tick() {
        super.tick();
        this.advanceWheelRotation();
        this.updateSteeringAngle();
    }

    private void advanceWheelRotation() {
        float deltaTime = (this.currentTimeMillis() - this.lastRenderTime()) / 1000f;
        float forwardSpeed = this.getForwardSpeed();
        
        // 累积轮胎旋转角度，速度单位已经是方块/tick，需要转换为方块/秒
        float distance = forwardSpeed * deltaTime * 20; // 转换为方块/秒
        wheelRotation += distance * 360f; // 简化计算，后续在getWheelDegrees中根据半径调整
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
