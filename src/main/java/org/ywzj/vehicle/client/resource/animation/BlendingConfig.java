package org.ywzj.vehicle.client.resource.animation;

import com.google.gson.annotations.SerializedName;

/**
 * JSON POJO for blending configuration.
 * Defines how multiple state machine layers are blended together.
 */
public class BlendingConfig {
    /**
     * Blending strategy: "layered"
     */
    @SerializedName("strategy")
    private String strategy;

    /**
     * Whether to smooth transitions between layers
     */
    @SerializedName("transition_smoothing")
    private Boolean transitionSmoothing;

    /**
     * Default blend time for transitions (in seconds)
     */
    @SerializedName("default_blend_time")
    private Float defaultBlendTime;

    public String getStrategy() {
        return strategy;
    }

    public Boolean getTransitionSmoothing() {
        return transitionSmoothing;
    }

    public Float getDefaultBlendTime() {
        return defaultBlendTime;
    }
}
