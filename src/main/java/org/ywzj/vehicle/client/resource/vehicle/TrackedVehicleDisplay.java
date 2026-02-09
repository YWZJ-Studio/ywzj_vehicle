package org.ywzj.vehicle.client.resource.vehicle;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation;
import org.ywzj.vehicle.client.render.animation.context.TrackedVehicleContext;
import org.ywzj.vehicle.entity.vehicle.TrackedVehicle;

import javax.annotation.Nullable;

public class TrackedVehicleDisplay extends VehicleDisplay<TrackedVehicle, TrackedVehicleContext> {

    private final TrackConfig trackConfig;
    private BedrockAnimation leftTrackAnimation;
    private BedrockAnimation rightTrackAnimation;

    public TrackedVehicleDisplay(BaseDisplayPojo pojo) {
        super(pojo);
        this.trackConfig = pojo.trackConfig;

        if (trackConfig != null && trackConfig.isValid()) {
            leftTrackAnimation = animations.get(trackConfig.leftTrack);
            rightTrackAnimation = animations.get(trackConfig.rightTrack);
        }
    }


    @Nullable
    public TrackConfig getTrackConfig() {
        return trackConfig;
    }

    @Nullable
    public BedrockAnimation getLeftTrackAnimation() {
        return leftTrackAnimation;
    }

    @Nullable
    public BedrockAnimation getRightTrackAnimation() {
        return rightTrackAnimation;
    }

    public boolean hasTrackConfig() {
        return trackConfig != null && trackConfig.isValid();
    }

}
