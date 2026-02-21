package org.ywzj.vehicle.client.render.animation.graph.node;

import com.maydaymemory.mae.basic.DummyPose;
import com.maydaymemory.mae.basic.Pose;
import org.ywzj.vehicle.api.animation.IAnimationInstance;
import org.ywzj.vehicle.client.render.animation.context.VehicleContext;

public class EventAnimationNode implements PoseNode{

    @Override
    public Pose evaluate(IAnimationInstance<?> context) {
        if (context.getContext() instanceof VehicleContext<?> vehicleContext) {
            return vehicleContext.getEventPose();
        }
        return DummyPose.INSTANCE;
    }

}
