package org.ywzj.vehicle.client.render.animation.blending;

import org.ywzj.vehicle.client.render.animation.context.BaseAnimationContext;

/**
 * Static weight provider that returns a fixed weight value.
 */
public class StaticWeightProvider implements WeightProvider {
    private final float weight;

    public StaticWeightProvider(float weight) {
        this.weight = weight;
    }

    @Override
    public float getWeight(BaseAnimationContext context) {
        return weight;
    }
}
