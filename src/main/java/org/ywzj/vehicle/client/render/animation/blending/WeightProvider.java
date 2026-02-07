package org.ywzj.vehicle.client.render.animation.blending;

import org.ywzj.vehicle.client.render.animation.context.BaseAnimationContext;

/**
 * Provides dynamic weight values for layer blending.
 */
public interface WeightProvider {
    /**
     * Get the weight value for the current context
     * @param context Animation controller context
     * @return Weight value (typically 0.0 to 1.0)
     */
    float getWeight(BaseAnimationContext context);
}
