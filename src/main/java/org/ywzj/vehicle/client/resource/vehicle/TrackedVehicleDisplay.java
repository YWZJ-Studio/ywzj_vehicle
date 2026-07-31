package org.ywzj.vehicle.client.resource.vehicle;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation;
import net.minecraft.resources.ResourceLocation;
import org.ywzj.vehicle.client.render.animation.context.TrackedVehicleContext;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.entity.vehicle.TrackedVehicle;

import javax.annotation.Nullable;
import java.util.List;

public class TrackedVehicleDisplay extends VehicleDisplay<TrackedVehicle, TrackedVehicleContext> {

    private final TrackConfig trackConfig;
    private final VehicleBedrockModel trackModel;
    private final ResourceLocation trackTexture;
    private BedrockAnimation leftTrackAnimation;
    private BedrockAnimation rightTrackAnimation;

    public TrackedVehicleDisplay(TrackedVehicleDisplayPojo pojo) {
        super(pojo);
        this.trackConfig = pojo.trackConfig;
        if (trackConfig != null) {
            this.trackModel = trackConfig.model == null
                    ? null
                    : ClientAssetsManager.INSTANCE.getModel(trackConfig.model)
                    .map(modelPojo -> new VehicleBedrockModel(modelPojo, List.of()))
                    .orElse(null);
            this.trackTexture = trackConfig.texture;
            if (trackConfig.isValid()) {
                leftTrackAnimation = animations.get(trackConfig.leftTrack);
                rightTrackAnimation = animations.get(trackConfig.rightTrack);
            }
        } else {
            this.trackModel = null;
            this.trackTexture = null;
        }
    }

    @Nullable
    public TrackConfig getTrackConfig() {
        return trackConfig;
    }

    @Nullable
    public VehicleBedrockModel getTrackModel() {
        return trackModel;
    }

    @Nullable
    public ResourceLocation getTrackTexture() {
        return trackTexture;
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
