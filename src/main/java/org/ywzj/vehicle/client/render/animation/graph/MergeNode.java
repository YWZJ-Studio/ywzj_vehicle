package org.ywzj.vehicle.client.render.animation.graph;

import com.maydaymemory.mae.basic.DummyPose;
import com.maydaymemory.mae.basic.Pose;
import org.ywzj.vehicle.api.animation.IAnimationInstance;
import org.ywzj.vehicle.client.render.animation.util.PoseBlenders;

import java.util.ArrayList;
import java.util.List;

/**
 * 合并节点，简单合并多个pose，对于同一骨骼，后面的pose会覆盖前面的pose
 */
public class MergeNode implements PoseNode {
    public List<PoseNode> inputNodes;

    public MergeNode(List<PoseNode> inputNodes) {
        this.inputNodes = inputNodes;
    }

    @Override
    public Pose evaluate(IAnimationInstance<?> context) {
        if (this.inputNodes == null || this.inputNodes.isEmpty()) {
            return DummyPose.INSTANCE;
        }
        List<Pose> poses = new ArrayList<>();
        for (PoseNode node : inputNodes) {
            Pose pose = node.evaluate(context);
            if (pose != null && pose != DummyPose.INSTANCE) {
                poses.add(pose);
            }
        }
        return PoseBlenders.MERGE_BLENDER.blend(poses);
    }
}
