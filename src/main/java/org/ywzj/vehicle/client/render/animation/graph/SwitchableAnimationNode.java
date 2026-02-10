package org.ywzj.vehicle.client.render.animation.graph;

import com.maydaymemory.mae.basic.DummyPose;
import com.maydaymemory.mae.basic.Pose;
import org.ywzj.vehicle.api.animation.IAnimationInstance;
import org.ywzj.vehicle.client.render.animation.context.VehicleContext;

public class SwitchableAnimationNode implements PoseNode {
    private final String partId;

    public SwitchableAnimationNode(String partId) {
        this.partId = partId;
    }

    @Override
    public Pose evaluate(IAnimationInstance<?> instance) {
        if (instance.getContext() instanceof VehicleContext<?> ctx) {
            var runner = ctx.getSwitchableRunner(partId);
            if (runner != null) {
                return runner.evaluate();
            }
        }

        return DummyPose.INSTANCE;
    }
}
