package org.ywzj.vehicle.client.render.animation.context;

import org.ywzj.vehicle.client.render.animation.util.PoseHelper;
import org.ywzj.vehicle.client.render.animation.util.TrackAnimationInstance;
import org.ywzj.vehicle.entity.vehicle.TrackedVehicle;

public class TrackedVehicleContext extends VehicleContext<TrackedVehicle> {

    private TrackAnimationInstance trackAnimationInstance;

    public TrackedVehicleContext(TrackedVehicle vehicle) {
        super(vehicle);
    }

    public void setTrackAnimationInstance(TrackAnimationInstance trackAnimationInstance) {
        this.trackAnimationInstance = trackAnimationInstance;
    }

    public float getForwardSpeed() {
        return entity.getForwardSpeed();
    }

    public float getTurnSpeed() {
        return entity.getTurnSpeed();
    }

    @Override
    public void tick() {
        super.tick();
        this.advanceTrackProgress();
    }

    // tick里会自动调用
    public void advanceTrackProgress() {
        if (trackAnimationInstance == null) {
            return;
        }

        float deltaTime = (this.currentTimeMillis() - this.lastRenderTime()) / 1000f;
        float forwardSpeed = this.getForwardSpeed();
        float turnSpeed = this.getTurnSpeed();

        float trackWidth = trackAnimationInstance.getTrackWidth() / 20f;
        float leftTrackSpeed = (forwardSpeed + turnSpeed * trackWidth / 2) * 20;
        float rightTrackSpeed = (forwardSpeed - turnSpeed * trackWidth / 2) * 20;
        trackAnimationInstance.advanceProgress(leftTrackSpeed, rightTrackSpeed, deltaTime);
    }

    public PoseHelper getTrackPose() {
        if (trackAnimationInstance == null) {
            return PoseHelper.DUMMY;
        }
        return new PoseHelper(trackAnimationInstance.evaluate());
    }

    public float getLeftWheelDegrees(float leftDriveRadius) {
        if (trackAnimationInstance == null) {
            return 0f;
        }
        return trackAnimationInstance.leftWheelDegrees(leftDriveRadius);
    }

    public float getRightWheelDegrees(float rightDriveRadius) {
        if (trackAnimationInstance == null) {
            return 0f;
        }
        return trackAnimationInstance.rightWheelDegrees(rightDriveRadius);
    }

    @Override
    public float getBindingValue(String source, Float param) {
        float paramValue = param != null ? param : 0f;
        
        return switch (source) {
            case "left_wheel_rotation" -> getLeftWheelDegrees(paramValue > 0 ? paramValue : 0.35f);
            case "right_wheel_rotation" -> getRightWheelDegrees(paramValue > 0 ? paramValue : 0.35f);
            default -> super.getBindingValue(source, param);
        };
    }

}
