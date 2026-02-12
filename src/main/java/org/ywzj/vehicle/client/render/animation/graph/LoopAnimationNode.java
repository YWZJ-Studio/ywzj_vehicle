package org.ywzj.vehicle.client.render.animation.graph;

import com.maydaymemory.mae.basic.DummyPose;
import com.maydaymemory.mae.basic.Pose;
import org.ywzj.vehicle.api.animation.IAnimationInstance;

/**
 * 一种特殊的node，永远只会输出一个循环动画，用于替代简单状态机
 */
public class LoopAnimationNode implements PoseNode {
    private final String ref;

    public LoopAnimationNode(String ref) {
        this.ref = ref;
    }

    @Override
    public Pose evaluate(IAnimationInstance<?> context) {
        var runner = context.getContext().getLoopRunner(ref);
        if (runner == null) {
            return DummyPose.INSTANCE;
        }
        return runner.evaluate();
    }

    public String getRef() {
        return ref;
    }
}
