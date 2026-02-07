package org.ywzj.vehicle.client.resource.animation;

import com.google.gson.annotations.SerializedName;

/**
 * JSON POJO for layer configuration.
 * Defines how a state machine layer blends with others.
 */
public class LayerConfig {
    /**
     * Blend mode: "override" or "additive"
     */
    @SerializedName("blend_mode")
    private String blendMode;

    /**
     * Weight value - can be a Number (static) or a Map with "type" and "script" (dynamic)
     */
    @SerializedName("weight")
    private Object weight;

    /**
     * Layer priority (lower values are processed first)
     */
    @SerializedName("priority")
    private int priority;

    /**
     * Bone mask - defines which bones this layer affects.
     * Can be a Map with "type" and "script" for dynamic masks,
     * or a Map with "bones" array for static masks.
     */
    @SerializedName("bone_mask")
    private Object boneMask;

    public String getBlendMode() {
        return blendMode;
    }

    public Object getWeight() {
        return weight;
    }

    public int getPriority() {
        return priority;
    }

    public Object getBoneMask() {
        return boneMask;
    }
}
