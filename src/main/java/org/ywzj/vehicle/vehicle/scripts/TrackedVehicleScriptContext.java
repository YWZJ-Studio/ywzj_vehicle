package org.ywzj.vehicle.vehicle.scripts;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel;
import com.maydaymemory.mae.basic.Pose;
import org.ywzj.vehicle.client.render.animation.TrackAnimationInstance;
import org.ywzj.vehicle.client.render.entity.vehicle.VehicleRender;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.entity.vehicle.TrackedVehicle;

public class TrackedVehicleScriptContext extends VehicleScriptContext<TrackedVehicle> {

    public TrackedVehicleScriptContext(TrackedVehicle vehicle, BedrockModel model) {
        super(vehicle, model);
    }

    public float getForwardSpeed() {
        return entity.getForwardSpeed();
    }

    public float getTurnSpeed() {
        return entity.getTurnSpeed();
    }

    public void advanceProgress(String leftTrack, String rightTrack, float leftTrackSpeed, float rightTrackSpeed, float deltaTime, float moduleLength) {
        TrackAnimationInstance instance = entity.getTrackAnimationInstance();
        if (instance == null) {
            var display = ClientAssetsManager.INSTANCE.getVehicleDisplay(entity.getCustomDisplayId()).orElse(null);
            if (display == null) {
                return;
            }
            var animations = display.getAnimations();
            instance = new TrackAnimationInstance(animations.get(leftTrack), animations.get(rightTrack));
            entity.setTrackAnimationInstance(instance);
        }
        instance.advanceProgress(leftTrackSpeed, rightTrackSpeed, deltaTime, moduleLength);
        Pose bindPose = model.getBindPose();
        Pose blended = VehicleRender.BLENDER.blend(bindPose, instance.evaluate());
        model.applyPose(blended);
    }

    public float leftWheelDegrees(float leftDriveRadius) {
        return entity.getTrackAnimationInstance().leftWheelDegrees(leftDriveRadius);
    }

    public float rightWheelDegrees(float rightDriveRadius) {
        return entity.getTrackAnimationInstance().rightWheelDegrees(rightDriveRadius);
    }

}
