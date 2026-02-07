package org.ywzj.vehicle.client.render.animation.blending;

import com.maydaymemory.mae.basic.Pose;
import org.ywzj.vehicle.client.render.animation.context.BaseAnimationContext;

import java.util.List;

/**
 * 混合策略
 */
public interface BlendingStrategy {
    /**
     * Blend multiple layer poses into a single final pose
     * @param layerPoses List of layer poses with their configurations
     * @param context Animation controller context
     * @return Final blended pose
     */
    Pose blend(List<LayerPose> layerPoses, BaseAnimationContext context);

    /**
     * Container for a layer's pose and its blend configuration
     */
    record LayerPose(Pose pose, LayerBlendConfig config) {}
}
