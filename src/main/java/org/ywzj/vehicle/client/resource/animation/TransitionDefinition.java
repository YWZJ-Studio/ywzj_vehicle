package org.ywzj.vehicle.client.resource.animation;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * 状态转换连线定义
 */
public class TransitionDefinition {
    /**
     * 来源状态名称 (仅用于 Root Transitions，普通 State 中的 transitions 隐含了来源)
     */
    @SerializedName("from")
    private List<String> fromStates;

    /**
     * 目标状态名称
     */
    @SerializedName("target")
    private String targetState;

    /**
     * 转换条件定义
     */
    @SerializedName("condition")
    private ConditionDefinition condition;

    /**
     * 转换时长 (秒)
     */
    @SerializedName("duration")
    private float duration = 0.0f;

    /**
     * 混合曲线类型: "linear", "ease_in_out", etc.
     */
    @SerializedName("blend_curve")
    private String blendCurve;

    public List<String> getFromStates() {
        return fromStates;
    }

    public String getTargetState() {
        return targetState;
    }

    public ConditionDefinition getCondition() {
        return condition;
    }

    public float getDuration() {
        return duration;
    }

    public String getBlendCurve() {
        return blendCurve;
    }
}
