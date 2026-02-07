package org.ywzj.vehicle.client.render.animation.graph;

import com.maydaymemory.mae.basic.Pose;
import org.ywzj.vehicle.api.animation.IAnimationInstance;

/**
 * Pose graph that manages the root node and evaluates final pose.
 * The graph is a tree structure with a single root node.
 */
public class PoseGraph {
    private final PoseNode rootNode;

    public PoseGraph(PoseNode rootNode) {
        this.rootNode = rootNode;
    }

    /**
     * Evaluate the pose graph and return the final pose.
     *
     * @param context Animation controller context
     * @return Final pose output
     */
    public Pose evaluate(IAnimationInstance<?> context) {
        if (rootNode == null) {
            return null;
        }
        return rootNode.evaluate(context);
    }

    public PoseNode getRootNode() {
        return rootNode;
    }
}
