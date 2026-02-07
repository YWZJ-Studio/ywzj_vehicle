package org.ywzj.vehicle.client.render.animation.blending;

import org.ywzj.vehicle.client.render.animation.context.BaseAnimationContext;

/**
 * Configuration for how a layer blends with others.
 */
public class LayerBlendConfig {
    /**
     * Blend mode enumeration
     */
    public enum BlendMode {
        OVERRIDE,   // Replace base pose
        ADDITIVE    // Add to base pose
    }

    private final BlendMode blendMode;
    private final WeightProvider weightProvider;
    private final int priority;

    public LayerBlendConfig(BlendMode blendMode, WeightProvider weightProvider, int priority) {
        this.blendMode = blendMode;
        this.weightProvider = weightProvider;
        this.priority = priority;
    }

    /**
     * Get the blend mode
     */
    public BlendMode getBlendMode() {
        return blendMode;
    }

    /**
     * Get the weight for the current context
     */
    public float getWeight(BaseAnimationContext context) {
        return weightProvider.getWeight(context);
    }

    /**
     * Get the layer priority
     */
    public int getPriority() {
        return priority;
    }
}
