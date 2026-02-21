package org.ywzj.vehicle.client.render.animation.graph.node;

import com.maydaymemory.mae.basic.Pose;
import org.ywzj.vehicle.api.animation.IAnimationInstance;
import org.ywzj.vehicle.client.render.animation.graph.WeightSource;
import org.ywzj.vehicle.client.render.animation.util.PoseBlenders;

import java.util.List;

public class LayeredBlendNode implements PoseNode {

    private final PoseNode baseNode;
    private final List<Layer> layers;

    public LayeredBlendNode(PoseNode baseNode, List<Layer> layers) {
        this.baseNode = baseNode;
        this.layers = List.copyOf(layers);
    }

    @Override
    public Pose evaluate(IAnimationInstance<?> context) {
        Pose result = baseNode.evaluate(context);
        if (result == null) {
            return null;
        }

        // Apply layers in order
        for (Layer layer : layers) {
            Pose layerPose = layer.poseNode.evaluate(context);
            if (layerPose == null) {
                continue;
            }

            float weight = layer.weightSource.getWeight(context);
            weight = Math.max(0.0f, Math.min(1.0f, weight));

            // Blend with mask
            result = PoseBlenders.INTERPOLATOR_BLENDER.blend(result, layerPose, weight);
        }

        return result;
    }

    /**
     * Layer configuration
     */
    public record Layer(PoseNode poseNode, WeightSource weightSource) {}

}
