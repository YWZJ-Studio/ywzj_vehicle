package org.ywzj.vehicle.client.resource.animation;

import com.google.gson.annotations.SerializedName;

/**
 * JSON POJO for pose evaluation configuration.
 * Defines how a state evaluates its pose.
 */
public class EvaluateConfig {
    /**
     * Evaluation type: "script", "context_pose", "blend_space_1d"
     */
    @SerializedName("type")
    private String type;

    /**
     * JavaScript code for script-based evaluation
     */
    @SerializedName("script")
    private String script;

    /**
     * Source for context_pose type (e.g., "multi_animation_runner")
     */
    @SerializedName("track")
    private String track;

    public String getType() {
        return type;
    }

    public String getScript() {
        return script;
    }

    public String getTrack() {
        return track;
    }
}
