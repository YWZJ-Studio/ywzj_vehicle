package org.ywzj.vehicle.client.render.animation.graph;

import com.maydaymemory.mae.basic.Pose;
import org.ywzj.vehicle.api.animation.IAnimationInstance;
import org.ywzj.vehicle.client.render.animation.util.PoseBlenders;

/**
 * Pose node that performs additive blending.
 * Pose = Base + (Add - Reference) * weight
 */
public class AdditiveNode implements PoseNode {
    private final PoseNode baseNode;
    private final PoseNode addNode;
    private final WeightSource weightSource;

    public AdditiveNode(PoseNode baseNode, PoseNode addNode, WeightSource weightSource) {
        this.baseNode = baseNode;
        this.addNode = addNode;
        this.weightSource = weightSource;
    }

    @Override
    public Pose evaluate(IAnimationInstance<?> context) {
        Pose basePose = baseNode.evaluate(context);
        Pose addPose = addNode.evaluate(context);

        if (basePose == null) return addPose;
        if (addPose == null) return basePose;

        float weight = Math.max(0.0f, Math.min(1.0f, weightSource.getWeight(context)));

        return PoseBlenders.INTERPOLATOR_BLENDER.blend(basePose, addPose, weight);
    }
}
