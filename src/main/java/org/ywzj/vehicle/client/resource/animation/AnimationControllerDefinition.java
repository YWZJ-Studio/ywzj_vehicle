package org.ywzj.vehicle.client.resource.animation;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/**
 * 动画控制器定义
 */
public class AnimationControllerDefinition {
    /**
     * Controller name
     */
    @SerializedName("name")
    private String name;

    /**
     * Controller description
     */
    @SerializedName("description")
    private String description;

    /**
     * External script file shared by all state machines in this controller
     */
    @SerializedName("script")
    private ResourceLocation script;

    /**
     * Parameter definitions (typed parameters)
     */
    @SerializedName("parameters")
    private Map<String, ParameterDefinition> parameters;

    /**
     * State machine definitions (pose sources)
     */
    @SerializedName("state_machines")
    private Map<String, StateMachineDefinition> stateMachines;

    /**
     * Pose graph definition (blending logic)
     */
    @SerializedName("graph")
    private PoseNodeDefinition graph;

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ResourceLocation getScript() {
        return script;
    }

    public Map<String, ParameterDefinition> getParameters() {
        return parameters;
    }

    public Map<String, StateMachineDefinition> getStateMachines() {
        return stateMachines;
    }

    public PoseNodeDefinition getGraph() {
        return graph;
    }
}
