package org.ywzj.vehicle.client.resource.animation;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * JSON POJO for condition definition.
 * Conditions are used in state transitions to determine when to trigger.
 */
public class ConditionDefinition {
    /**
     * Condition type: "script", "and", "or", "not"
     */
    @SerializedName("type")
    private String type;

    // script condition
    @SerializedName("script")
    private String script;

    // and/or conditions
    @SerializedName("conditions")
    private List<ConditionDefinition> conditions;

    // not condition
    @SerializedName("condition")
    private ConditionDefinition condition;

    public String getType() {
        return type;
    }

    public String getScript() {
        return script;
    }

    public List<ConditionDefinition> getConditions() {
        return conditions;
    }

    public ConditionDefinition getCondition() {
        return condition;
    }
}
