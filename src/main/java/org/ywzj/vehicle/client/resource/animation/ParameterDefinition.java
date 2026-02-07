package org.ywzj.vehicle.client.resource.animation;

import com.google.gson.annotations.SerializedName;

/**
 * Parameter definition in animation controller.
 * Parameters are the only dynamic input for state machines and pose graph.
 */
public class ParameterDefinition {
    @SerializedName("type")
    private String type;

    @SerializedName("default")
    private Object defaultValue;

    public ParameterType getType() {
        if (type == null) {
            return ParameterType.FLOAT;
        }
        return switch (type.toLowerCase()) {
            case "float" -> ParameterType.FLOAT;
            case "bool", "boolean" -> ParameterType.BOOL;
            case "int", "integer" -> ParameterType.INT;
            default -> throw new IllegalArgumentException("Unknown parameter type: " + type);
        };
    }

    /**
     * Get default value as float
     */
    public float getDefaultFloat() {
        if (defaultValue instanceof Number) {
            return ((Number) defaultValue).floatValue();
        }
        return 0.0f;
    }

    /**
     * Get default value as boolean
     */
    public boolean getDefaultBool() {
        if (defaultValue instanceof Boolean) {
            return (Boolean) defaultValue;
        }
        return false;
    }

    /**
     * Get default value as int
     */
    public int getDefaultInt() {
        if (defaultValue instanceof Number) {
            return ((Number) defaultValue).intValue();
        }
        return 0;
    }
}
