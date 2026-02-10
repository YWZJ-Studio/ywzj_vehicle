package org.ywzj.vehicle.client.render.animation.graph;

import com.maydaymemory.mae.basic.Pose;
import org.ywzj.vehicle.api.animation.IAnimationInstance;
import org.ywzj.vehicle.client.render.animation.util.PoseBlenders;

/**
 * 简单加性混合，输出两个pose的和
 */
public class AdditiveNode implements PoseNode {
    private final PoseNode baseNode;
    private final PoseNode addNode;

    public AdditiveNode(PoseNode baseNode, PoseNode addNode) {
        this.baseNode = baseNode;
        this.addNode = addNode;
    }

    @Override
    public Pose evaluate(IAnimationInstance<?> context) {
        Pose basePose = baseNode.evaluate(context);
        Pose addPose = addNode.evaluate(context);

        if (basePose == null) return addPose;
        if (addPose == null) return basePose;

        return PoseBlenders.BLENDER.blend(basePose, addPose);
    }
}
