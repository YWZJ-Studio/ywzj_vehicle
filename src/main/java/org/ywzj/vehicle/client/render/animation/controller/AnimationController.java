package org.ywzj.vehicle.client.render.animation.controller;

import com.maydaymemory.mae.control.statemachine.AnimationStateMachine;
import com.maydaymemory.mae.util.LongSupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ywzj.vehicle.client.render.animation.context.BaseAnimationContext;
import org.ywzj.vehicle.client.render.animation.graph.PoseGraph;
import org.ywzj.vehicle.client.resource.animation.LoopAnimationDefinition;
import org.ywzj.vehicle.client.resource.animation.ParameterDefinition;
import org.ywzj.vehicle.client.resource.animation.SwitchableAnimationDefinition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 动画控制器原型
 * @param <T>
 */
public class AnimationController<T extends BaseAnimationContext> {

    // 控制器名称
    private final String name;
    // 变量定义
    private final Map<String, ParameterDefinition> parameters;
    // 状态机
    private final Map<String, CompiledStateMachine<T>> stateMachines;
    // 可开关动画定义
    private final Map<String, SwitchableAnimationDefinition> switchableAnimations;
    // 循环动画定义
    private final Map<String, LoopAnimationDefinition> loopAnimations;
    // 事件动画定义
    private final Map<String, List<String>> eventAnimations;

    private final PoseGraph poseGraph;

    public AnimationController(String name,
                               Map<String, ParameterDefinition> parameters,
                               Map<String, CompiledStateMachine<T>> stateMachines,
                               Map<String, SwitchableAnimationDefinition> switchableAnimations,
                               Map<String, LoopAnimationDefinition> loopAnimationDefinitions,
                               Map<String, List<String>> eventAnimations,
                               PoseGraph poseGraph) {
        this.name = name;
        this.parameters = parameters != null ? Map.copyOf(parameters) : Map.of();
        this.stateMachines = stateMachines != null ? Map.copyOf(stateMachines) : Map.of();
        this.switchableAnimations = switchableAnimations != null ? Map.copyOf(switchableAnimations) : Map.of();
        this.loopAnimations = loopAnimationDefinitions != null ? Map.copyOf(loopAnimationDefinitions) : Map.of();
        this.eventAnimations = eventAnimations;
        this.poseGraph = poseGraph;
    }

    public Map<String, AnimationStateMachine<T>> initialize(T context) {
        // 为上下文注入脚本变量默认值
        if (parameters != null) {
            for (Map.Entry<String, ParameterDefinition> entry : parameters.entrySet()) {
                String paramName = entry.getKey();
                ParameterDefinition paramDef = entry.getValue();

                switch (paramDef.getType()) {
                    case FLOAT -> context.setFloat(paramName, paramDef.getDefaultFloat());
                    case BOOL -> context.setBool(paramName, paramDef.getDefaultBool());
                    case INT -> context.setInt(paramName, paramDef.getDefaultInt());
                }
            }
        }
        // 以初始状态创建所有状态机实例
        Map<String, AnimationStateMachine<T>> map = new HashMap<>();
        LongSupplier timeSupplier = System::nanoTime;
        for (Map.Entry<String, CompiledStateMachine<T>> entry : stateMachines.entrySet()) {
            String smName = entry.getKey();
            CompiledStateMachine<T> compiledSM = entry.getValue();
            AnimationStateMachine<T> instance = compiledSM.createInstance(context, timeSupplier);
            compiledSM.entryState().onEnter(context, compiledSM.entryState());
            map.put(smName, instance);
        }

        return map;
    }

    /**
     * Get a state machine by name
     */
    public CompiledStateMachine<T> getStateMachine(String name) {
        return stateMachines.get(name);
    }

    /**
     * Get the controller name
     */
    public String getName() {
        return name;
    }

    /**
     * Get all state machines
     */
    @NotNull
    public Map<String, CompiledStateMachine<T>> getStateMachines() {
        return stateMachines;
    }

    /**
     * Get the pose graph
     */
    public PoseGraph getPoseGraph() {
        return poseGraph;
    }

    /**
     * Get parameter definitions
     */
    public Map<String, ParameterDefinition> getParameters() {
        return parameters;
    }

    /**
     * Get switchable animation definitions
     */
    @NotNull
    public Map<String, SwitchableAnimationDefinition> getSwitchableAnimations() {
        return switchableAnimations;
    }

    /**
     * Get a switchable animation definition by name
     */
    public SwitchableAnimationDefinition getSwitchableAnimation(String name) {
        return switchableAnimations.get(name);
    }

    public Map<String, LoopAnimationDefinition> getLoopAnimations() {
        return loopAnimations;
    }

    @Nullable
    public Map<String, List<String>> getEventAnimations() {
        return eventAnimations;
    }

}
