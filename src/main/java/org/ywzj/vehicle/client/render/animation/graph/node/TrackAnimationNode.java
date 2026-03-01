package org.ywzj.vehicle.client.render.animation.graph.node;

import com.maydaymemory.mae.basic.DummyPose;
import com.maydaymemory.mae.basic.Pose;
import org.ywzj.vehicle.api.animation.IAnimationInstance;
import org.ywzj.vehicle.client.render.animation.context.TrackedVehicleContext;

/**
 * 履带式专用节点，生成履带动画pose
 */
public class TrackAnimationNode implements PoseNode {

    public TrackAnimationNode() {}

    @Override
    public Pose evaluate(IAnimationInstance<?> context) {
        if (context.getContext() instanceof TrackedVehicleContext trackedContext) {
            return trackedContext.getTrackPose().build();
        }
        return DummyPose.INSTANCE;
    }

}
