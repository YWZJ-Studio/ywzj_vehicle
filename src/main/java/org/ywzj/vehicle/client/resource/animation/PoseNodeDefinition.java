package org.ywzj.vehicle.client.resource.animation;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * JSON POJO for pose node definition.
 * Represents a node in the pose graph.
 */
public class PoseNodeDefinition {
    @SerializedName("type")
    private String type;

    // For state_machine node
    @SerializedName("ref")
    private String ref;

    // For blend node
    @SerializedName("a")
    private PoseNodeDefinition a;

    @SerializedName("b")
    private PoseNodeDefinition b;

    // For layered_blend node
    @SerializedName("base")
    private PoseNodeDefinition base;

    @SerializedName("layers")
    private List<LayerDefinition> layers;

    // For additive node
    @SerializedName("add")
    private PoseNodeDefinition add;

    // Weight configuration
    @SerializedName("weight")
    private Object weight;

    public String getType() {
        return type;
    }

    public String getRef() {
        return ref;
    }

    public PoseNodeDefinition getA() {
        return a;
    }

    public PoseNodeDefinition getB() {
        return b;
    }

    public PoseNodeDefinition getBase() {
        return base;
    }

    public List<LayerDefinition> getLayers() {
        return layers;
    }

    public PoseNodeDefinition getAdd() {
        return add;
    }

    public Object getWeight() {
        return weight;
    }

    /**
     * Layer definition for layered_blend node
     */
    public static class LayerDefinition {
        @SerializedName("pose")
        private PoseNodeDefinition pose;

        @SerializedName("mask")
        private String mask;

        @SerializedName("weight")
        private Object weight;

        public PoseNodeDefinition getPose() {
            return pose;
        }

        public String getMask() {
            return mask;
        }

        public Object getWeight() {
            return weight;
        }
    }
}
