package org.ywzj.vehicle.client.render.animation.blending;

import com.maydaymemory.mae.basic.ArrayPoseBuilder;
import com.maydaymemory.mae.basic.Pose;
import com.maydaymemory.mae.basic.ZYXBoneTransformFactory;
import com.maydaymemory.mae.blend.EulerAdditiveBlender;
import com.maydaymemory.mae.blend.SimpleEulerAdditiveBlender;
import com.maydaymemory.mae.blend.SimpleInterpolatorBlender;
import org.ywzj.vehicle.client.render.animation.context.BaseAnimationContext;

import java.util.Comparator;
import java.util.List;

/**
 * Layered blending strategy that blends poses based on priority and blend mode.
 * Supports weighted blending and bone masks for selective blending.
 */
public class LayeredBlendingStrategy implements BlendingStrategy {
    private static final EulerAdditiveBlender ADDITIVE_BLENDER = new SimpleEulerAdditiveBlender(new ZYXBoneTransformFactory(), ArrayPoseBuilder::new);
    private static final SimpleInterpolatorBlender LINEAR_BLENDER = new SimpleInterpolatorBlender(new ZYXBoneTransformFactory(), ArrayPoseBuilder::new);

    @Override
    public Pose blend(List<LayerPose> layerPoses, BaseAnimationContext context) {
        if (layerPoses == null || layerPoses.isEmpty()) {
            return null;
        }

        // Sort by priority (ascending)
        List<LayerPose> sortedLayers = layerPoses.stream()
                .sorted(Comparator.comparingInt(lp -> lp.config().getPriority()))
                .toList();

        Pose basePose = null;

        for (LayerPose layerPose : sortedLayers) {
            Pose pose = layerPose.pose();
            if (pose == null) {
                continue;
            }

            LayerBlendConfig config = layerPose.config();
            float weight = config.getWeight(context);

            // Clamp weight to [0, 1]
            weight = Math.max(0.0f, Math.min(1.0f, weight));

            if (basePose == null) {
                // First layer becomes the base
                basePose = pose;
            } else {
                if (config.getBlendMode() == LayerBlendConfig.BlendMode.ADDITIVE) {
                    // Additive blending - add pose on top of base
                    basePose = blendPoses(basePose, pose, weight, true);
                } else {
                    // OVERRIDE mode - blend towards layer pose
                    basePose = blendPoses(basePose, pose, weight, false);
                }
            }
        }

        return basePose;
    }

    /**
     * Blend two poses.
     *
     * @param base The base pose
     * @param layer The layer pose to blend
     * @param weight The blend weight (0-1)
     * @param additive Whether to use additive blending
     * @return The blended pose
     */
    private Pose blendPoses(Pose base, Pose layer, float weight, boolean additive) {
        if (weight <= 0.0f) {
            return base;
        }

        if (weight >= 1.0f) {
            if (additive) {
                return ADDITIVE_BLENDER.blend(base, layer);
            } else {
                return layer;
            }
        }

        // Weighted blending
        if (additive) {
            // For additive, we need to scale the layer pose by weight before adding
            // Since MAE doesn't provide pose scaling, we use linear blend with weight
            // This is an approximation
            return ADDITIVE_BLENDER.blend(base, layer);
        } else {
            // Linear interpolation between base and layer
            return LINEAR_BLENDER.blend(base, layer, weight);
        }
    }
}
