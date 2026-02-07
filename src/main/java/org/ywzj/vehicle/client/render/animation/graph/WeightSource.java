package org.ywzj.vehicle.client.render.animation.graph;

import org.ywzj.vehicle.api.animation.IAnimationInstance;

/**
 * Interface for weight sources in pose graph.
 * Weight can be static or dynamic (from parameter).
 */
public interface WeightSource {
    /**
     * Get the weight value for current context.
     *
     * @param context Animation controller context
     * @return Weight value (typically 0.0 to 1.0)
     */
    float getWeight(IAnimationInstance<?> context);

    /**
     * Static weight source
     */
    class Static implements WeightSource {
        private final float value;

        public Static(float value) {
            this.value = value;
        }

        @Override
        public float getWeight(IAnimationInstance<?> context) {
            return value;
        }
    }

    /**
     * Parameter-based weight source
     */
    class Parameter implements WeightSource {
        private final String parameterName;
        private final float defaultValue;

        public Parameter(String parameterName, float defaultValue) {
            this.parameterName = parameterName;
            this.defaultValue = defaultValue;
        }

        @Override
        public float getWeight(IAnimationInstance<?> context) {
            return context.getContext().getFloat(parameterName, defaultValue);
        }
    }
}
