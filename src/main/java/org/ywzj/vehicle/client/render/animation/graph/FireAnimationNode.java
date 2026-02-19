package org.ywzj.vehicle.client.render.animation.graph;

import com.maydaymemory.mae.basic.DummyPose;
import com.maydaymemory.mae.basic.Pose;
import org.ywzj.vehicle.api.animation.IAnimationInstance;
import org.ywzj.vehicle.client.render.animation.context.VehicleContext;

public class FireAnimationNode implements PoseNode{

    @Override
    public Pose evaluate(IAnimationInstance<?> context) {
        if (!(context.getContext() instanceof VehicleContext<?> context1)) {
            return DummyPose.INSTANCE;
        }

        return context1.getFirePose();
    }
}
