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

    // For state_machine / switchable_animation node
    @SerializedName("ref")
    private String ref;

    // For merge node
    @SerializedName("inputs")
    private List<PoseNodeDefinition> inputs;

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

    // For script node
    @SerializedName("function")
    private String function;

    // Weight configuration
    @SerializedName("weight")
    private Object weight;

    // For bone_binding node
    @SerializedName("wheel_bindings")
    private List<WheelBindingDefinition> wheelBindings;

    @SerializedName("part_bindings")
    private List<PartBindingDefinition> partBindings;

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

    public String getFunction() {
        return function;
    }

    public Object getWeight() {
        return weight;
    }

    public List<WheelBindingDefinition> getWheelBindings() {
        return wheelBindings;
    }

    public List<PartBindingDefinition> getPartBindings() {
        return partBindings;
    }

    public List<PoseNodeDefinition> getInputs() {
        return inputs;
    }


    /**
     * Layer definition for layered_blend node
     */
    public static class LayerDefinition {
        @SerializedName("pose")
        private PoseNodeDefinition pose;

        @SerializedName("weight")
        private Object weight;

        public PoseNodeDefinition getPose() {
            return pose;
        }

        public Object getWeight() {
            return weight;
        }
    }

    /**
     * Wheel binding definition for bone_binding node
     */
    public static class WheelBindingDefinition {
        @SerializedName("bones")
        private List<String> bones;

        @SerializedName("side")
        private String side;

        @SerializedName("radius")
        private float radius;

        @SerializedName("axis")
        private String axis;

        public List<String> getBones() {
            return bones;
        }

        public String getSide() {
            return side;
        }

        public float getRadius() {
            return radius;
        }

        public String getAxis() {
            return axis;
        }
    }

    /**
     * Part binding definition for bone_binding node
     */
    public static class PartBindingDefinition {
        @SerializedName("bone")
        private String bone;

        @SerializedName("part")
        private String part;

        @SerializedName("rotation_type")
        private String rotationType;

        @SerializedName("axis")
        private String axis;

        @SerializedName("invert")
        private boolean invert;

        public String getBone() {
            return bone;
        }

        public String getPart() {
            return part;
        }

        public String getRotationType() {
            return rotationType;
        }

        public String getAxis() {
            return axis;
        }

        public boolean isInvert() {
            return invert;
        }
    }
}
