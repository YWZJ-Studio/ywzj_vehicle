package org.ywzj.vehicle.client.resource.animation;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

/**
 * Inline state machine definition embedded in animation controller.
 * Combines state machine definition with layer configuration.
 */
public class StateMachineDefinition {
    /**
     * State machine name (used as layer name)
     */
    @SerializedName("name")
    private String name;

    /**
     * Start state name
     */
    @SerializedName("start_state")
    private String startState;

    /**
     * State definitions
     */
    @SerializedName("states")
    private Map<String, StateDefinition> states;

    public String getName() {
        return name;
    }

    public String getStartState() {
        return startState;
    }

    public Map<String, StateDefinition> getStates() {
        return states;
    }
}
