package org.ywzj.vehicle.client.render.animation.graph;

import com.maydaymemory.mae.basic.Pose;
import org.ywzj.vehicle.api.animation.IAnimationInstance;
import org.ywzj.vehicle.client.render.animation.util.PoseBlenders;

/**
 * Pose node that performs linear blend between two poses.
 * Pose = lerp(PoseA, PoseB, clamp(weight, 0, 1))
 */
public class BlendNode implements PoseNode {
    private final PoseNode nodeA;
    private final PoseNode nodeB;
    private final WeightSource weightSource;

    public BlendNode(PoseNode nodeA, PoseNode nodeB, WeightSource weightSource) {
        this.nodeA = nodeA;
        this.nodeB = nodeB;
        this.weightSource = weightSource;
    }

    @Override
    public Pose evaluate(IAnimationInstance<?> context) {
        Pose poseA = nodeA.evaluate(context);
        Pose poseB = nodeB.evaluate(context);

        if (poseA == null) return poseB;
        if (poseB == null) return poseA;

        float weight = Math.max(0.0f, Math.min(1.0f, weightSource.getWeight(context)));
        return PoseBlenders.INTERPOLATOR_BLENDER.blend(poseA, poseB, weight);
    }
}
