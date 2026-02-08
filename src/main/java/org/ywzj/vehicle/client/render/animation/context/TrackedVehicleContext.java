package org.ywzj.vehicle.client.render.animation.context;

import org.ywzj.vehicle.client.render.animation.TrackAnimationInstance;
import org.ywzj.vehicle.client.render.animation.util.PoseHelper;
import org.ywzj.vehicle.entity.vehicle.TrackedVehicle;

public class TrackedVehicleContext extends VehicleContext<TrackedVehicle> {
    private TrackAnimationInstance trackAnimationInstance;

    public TrackedVehicleContext(TrackedVehicle vehicle) {
        super(vehicle);
    }

    public float getForwardSpeed() {
        return entity.getForwardSpeed();
    }

    public float getTurnSpeed() {
        return entity.getTurnSpeed();
    }

    public void advanceTrackProgress(String leftTrack, String rightTrack, float moduleLength, float trackWidth) {
        float deltaTime = (this.currentTimeMillis() - this.lastRenderTime()) / 1000f;

        float forwardSpeed = this.getForwardSpeed();
        float turnSpeed = this.getTurnSpeed();

        trackWidth /= 20f;
        float leftTrackSpeed = (forwardSpeed + turnSpeed * trackWidth / 2) * 20;
        float rightTrackSpeed = (forwardSpeed - turnSpeed * trackWidth / 2) * 20;

        if (trackAnimationInstance == null) {
            trackAnimationInstance = new TrackAnimationInstance(
                    this.getAnimation(leftTrack), this.getAnimation(rightTrack)
            );
        }
        trackAnimationInstance.advanceProgress(leftTrackSpeed, rightTrackSpeed, deltaTime, moduleLength);
    }

    public PoseHelper getTrackPose() {
        if (trackAnimationInstance == null) {
            return PoseHelper.DUMMY;
        }
        return new PoseHelper(trackAnimationInstance.evaluate());
    }

    public float getLeftWheelDegrees(float leftDriveRadius) {
        return trackAnimationInstance.leftWheelDegrees(leftDriveRadius);
    }

    public float getRightWheelDegrees(float rightDriveRadius) {
        return trackAnimationInstance.rightWheelDegrees(rightDriveRadius);
    }
}
