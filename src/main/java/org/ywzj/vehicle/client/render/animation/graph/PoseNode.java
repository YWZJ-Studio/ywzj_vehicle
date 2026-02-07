package org.ywzj.vehicle.client.render.animation.graph;

import com.maydaymemory.mae.basic.Pose;
import org.ywzj.vehicle.api.animation.IAnimationInstance;

/**
 * Base interface for pose graph nodes.
 * Each node produces a Pose output based on its inputs and context.
 */
public interface PoseNode {
    /**
     * Evaluate this node and return the output pose.
     *
     * @param context Animation controller context with parameters
     * @return Output pose, or null if no pose is available
     */
    Pose evaluate(IAnimationInstance<?> context);
}
