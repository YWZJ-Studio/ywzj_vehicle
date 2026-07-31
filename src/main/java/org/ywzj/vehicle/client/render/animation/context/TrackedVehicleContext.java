package org.ywzj.vehicle.client.render.animation.context;

import org.ywzj.vehicle.client.render.animation.util.PoseHelper;
import org.ywzj.vehicle.entity.vehicle.TrackedVehicle;
import org.ywzj.vehicle.vehicle.part.TrackUnit;

import java.util.OptionalDouble;

public class TrackedVehicleContext extends VehicleContext<TrackedVehicle> {

    private TrackAnimationInstance trackAnimationInstance;
    private float leftTrackDisplacement;
    private float rightTrackDisplacement;

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
        float deltaTime = (this.currentTimeMillis() - this.lastRenderTime()) / 1000f;
        float forwardSpeed = this.getForwardSpeed();
        float turnSpeed = this.getTurnSpeed();
        if (trackAnimationInstance != null) {
            float trackWidth = trackAnimationInstance.getTrackWidth() / 20f;
            float leftTrackSpeed = (forwardSpeed + turnSpeed * trackWidth / 2) * 20;
            float rightTrackSpeed = (forwardSpeed - turnSpeed * trackWidth / 2) * 20;
            trackAnimationInstance.advanceProgress(leftTrackSpeed, rightTrackSpeed, deltaTime);
            return;
        }
        float linearSpeed = forwardSpeed * 20;
        firstTrackLateralOffset(true).ifPresent(offset ->
                leftTrackDisplacement += (float) (linearSpeed + turnSpeed * offset) * deltaTime);
        firstTrackLateralOffset(false).ifPresent(offset ->
                rightTrackDisplacement += (float) (linearSpeed + turnSpeed * offset) * deltaTime);
    }

    public PoseHelper getTrackPose() {
        if (trackAnimationInstance == null) {
            return PoseHelper.DUMMY;
        }
        return new PoseHelper(trackAnimationInstance.evaluate());
    }

    public float getLeftWheelDegrees(float leftDriveRadius) {
        if (trackAnimationInstance != null) {
            return trackAnimationInstance.leftWheelDegrees(leftDriveRadius);
        }
        return wheelRotation(leftTrackDisplacement, leftDriveRadius);
    }

    public float getRightWheelDegrees(float rightDriveRadius) {
        if (trackAnimationInstance != null) {
            return trackAnimationInstance.rightWheelDegrees(rightDriveRadius);
        }
        return wheelRotation(rightTrackDisplacement, rightDriveRadius);
    }

    private OptionalDouble firstTrackLateralOffset(boolean left) {
        for (var partUnit : entity.getPartUnits()) {
            if (partUnit instanceof TrackUnit trackUnit) {
                OptionalDouble offset = left
                        ? trackUnit.getFirstLeftTrackLateralOffset()
                        : trackUnit.getFirstRightTrackLateralOffset();
                if (offset.isPresent()) {
                    return offset;
                }
            }
        }
        return OptionalDouble.empty();
    }

    private static float wheelRotation(float displacement, float radius) {
        if (radius == 0) {
            return 0;
        }
        return displacement / (2f * (float) Math.PI * radius) * 360f;
    }

    @Override
    public float getBindingValue(String source, Float param) {
        float paramValue = param != null ? param : 0f;
        return switch (source) {
            case "left_wheel_rotation" -> getLeftWheelDegrees(paramValue);
            case "right_wheel_rotation" -> getRightWheelDegrees(paramValue);
            default -> super.getBindingValue(source, param);
        };
    }

}
