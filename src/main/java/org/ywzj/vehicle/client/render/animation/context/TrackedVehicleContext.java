package org.ywzj.vehicle.client.render.animation.context;

import org.ywzj.vehicle.client.render.animation.util.PoseHelper;
import org.ywzj.vehicle.entity.vehicle.TrackedVehicle;
import org.ywzj.vehicle.vehicle.part.SuspensionUnit;
import org.ywzj.vehicle.vehicle.part.TrackUnit;

import java.util.OptionalDouble;

public class TrackedVehicleContext extends VehicleContext<TrackedVehicle> {

    private static final float AIRBORNE_TRACK_DAMPING = 2f;
    private float rollingForwardSpeed;
    private float rollingTurnSpeed;
    private long lastProgressRenderTime = -1;
    private float frameForwardDistance;
    private float frameTurnDistance;
    private float innerBrake;
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

    public void advanceTrackProgress() {
        long lastTime = this.lastRenderTime();
        if (lastProgressRenderTime == lastTime) {
            return;
        }
        lastProgressRenderTime = lastTime;
        frameForwardDistance = 0;
        frameTurnDistance = 0;
        if (lastTime == 0) {
            return;
        }
        float deltaTime = Math.max(0f, Math.min((this.currentTimeMillis() - lastTime) / 1000f, 0.1f));
        boolean grounded = entity.hasSuspension()
                ? entity.getSuspensionUnits().stream().anyMatch(SuspensionUnit::isGrounded)
                : entity.onGround();
        if (grounded) {
            rollingForwardSpeed = this.getForwardSpeed();
            rollingTurnSpeed = this.getTurnSpeed();
        }
        float forwardSpeed = rollingForwardSpeed;
        float turnSpeed = rollingTurnSpeed;
        if (!grounded) {
            float retention = (float) Math.exp(-AIRBORNE_TRACK_DAMPING * deltaTime);
            rollingForwardSpeed *= retention;
            rollingTurnSpeed *= retention;
            deltaTime = (1 - retention) / AIRBORNE_TRACK_DAMPING;
        }
        innerBrake = entity.dualFlow ? 0 : entity.innerTrackBrake(turnSpeed);
        frameForwardDistance = forwardSpeed * 20 * deltaTime;
        frameTurnDistance = turnSpeed * deltaTime;
        if (trackAnimationInstance != null) {
            float halfWidth = trackAnimationInstance.getTrackWidth() / 2f;
            float leftTrackSpeed = (float) trackTravel(forwardSpeed * 20, turnSpeed, halfWidth);
            float rightTrackSpeed = (float) trackTravel(forwardSpeed * 20, turnSpeed, -halfWidth);
            trackAnimationInstance.advanceProgress(leftTrackSpeed, rightTrackSpeed, deltaTime);
            return;
        }
        firstTrackLateralOffset(true).ifPresent(offset ->
                leftTrackDisplacement += (float) getTrackFrameDistance(offset));
        firstTrackLateralOffset(false).ifPresent(offset ->
                rightTrackDisplacement += (float) getTrackFrameDistance(offset));
    }

    public double getTrackFrameDistance(double lateralOffset) {
        return trackTravel(frameForwardDistance, frameTurnDistance, lateralOffset);
    }

    private double trackTravel(float forward, float turn, double lateralOffset) {
        if (innerBrake > 0) {
            double outer = forward / (1 - innerBrake * 0.5f);
            boolean inner = (lateralOffset > 0) == (turn < 0);
            return inner ? outer * (1 - innerBrake) : outer;
        }
        return forward + turn * lateralOffset;
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
