package org.ywzj.vehicle.client.render.animation.graph;

import com.maydaymemory.mae.basic.DummyPose;
import com.maydaymemory.mae.basic.Pose;
import org.ywzj.vehicle.api.animation.IAnimationInstance;
import org.ywzj.vehicle.client.render.animation.context.TrackedVehicleContext;

/**
 * Pose node that generates track animation from configuration.
 * Replaces script-based track animation logic.
 */
public class TrackAnimationNode implements PoseNode {
    

    public TrackAnimationNode() {
    }

    @Override
    public Pose evaluate(IAnimationInstance<?> context) {
        if (!(context.getContext() instanceof TrackedVehicleContext trackedContext)) {
            return DummyPose.INSTANCE;
        }

        return trackedContext.getTrackPose().build();
    }
}
