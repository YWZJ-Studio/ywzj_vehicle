package org.ywzj.vehicle.client.resource.animation;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * 单个状态节点的定义
 */
public class StateDefinition {
    /**
     * 进入状态时执行的动作列表
     */
    @SerializedName("on_enter")
    private List<ActionDefinition> onEnter = new ArrayList<>();

    /**
     * 状态更新时每帧执行的动作列表
     */
    @SerializedName("on_update")
    private List<ActionDefinition> onUpdate = new ArrayList<>();

    /**
     * 退出状态时执行的动作列表
     */
    @SerializedName("on_exit")
    private List<ActionDefinition> onExit = new ArrayList<>();

    /**
     * 从该状态出发的转换连线
     */
    @SerializedName("transitions")
    private List<TransitionDefinition> transitions = new ArrayList<>();

    /**
     * Pose 计算配置
     */
    @SerializedName("evaluate")
    private EvaluateConfig evaluate;

    /**
     * 编辑器元数据（仅用于可视化，运行时忽略）
     */
    @SerializedName("editor")
    private EditorMetadata editor;

    public List<ActionDefinition> getOnEnter() {
        return onEnter;
    }

    public List<ActionDefinition> getOnUpdate() {
        return onUpdate;
    }

    public List<ActionDefinition> getOnExit() {
        return onExit;
    }

    public List<TransitionDefinition> getTransitions() {
        return transitions;
    }

    public EvaluateConfig getEvaluate() {
        return evaluate;
    }

    public EditorMetadata getEditor() {
        return editor;
    }

    public static class EditorMetadata {
        @SerializedName("x")
        public int x;
        @SerializedName("y")
        public int y;
        @SerializedName("comment")
        public String comment;
    }
}
