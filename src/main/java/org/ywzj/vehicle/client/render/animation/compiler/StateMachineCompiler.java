package org.ywzj.vehicle.client.render.animation.compiler;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.SimpleAnimationState;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.SimpleTransition;
import com.maydaymemory.mae.basic.DummyPose;
import com.maydaymemory.mae.basic.Pose;
import org.mozillaa.javascript.Wrapper;
import org.ywzj.vehicle.client.render.animation.context.BaseAnimationContext;
import org.ywzj.vehicle.client.render.animation.controller.CompiledStateMachine;
import org.ywzj.vehicle.client.render.animation.util.PoseHelper;
import org.ywzj.vehicle.client.resource.animation.EvaluateConfig;
import org.ywzj.vehicle.client.resource.animation.StateDefinition;
import org.ywzj.vehicle.client.resource.animation.StateMachineDefinition;
import org.ywzj.vehicle.client.resource.animation.TransitionDefinition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class StateMachineCompiler {
    private final ScriptCompiler scriptCompiler;
    private final ActionCompiler actionCompiler;
    private final ConditionCompiler conditionCompiler;

    public StateMachineCompiler(ScriptCompiler scriptCompiler,
                                ActionCompiler actionCompiler,
                                ConditionCompiler conditionCompiler) {
        this.scriptCompiler = scriptCompiler;
        this.actionCompiler = actionCompiler;
        this.conditionCompiler = conditionCompiler;
    }

    public <T extends BaseAnimationContext> CompiledStateMachine<T> compile(StateMachineDefinition definition) {
        String name = definition.getName();

        // Step 1: Compile all states
        Map<String, SimpleAnimationState<T>> stateMap = new HashMap<>();
        for (Map.Entry<String, StateDefinition> entry : definition.getStates().entrySet()) {
            String stateName = entry.getKey();
            StateDefinition stateDef = entry.getValue();
            SimpleAnimationState<T> state = compileState(stateDef);
            stateMap.put(stateName, state);
        }

        // Step 2: Compile all transitions
        for (Map.Entry<String, StateDefinition> entry : definition.getStates().entrySet()) {
            String sourceName = entry.getKey();
            StateDefinition stateDef = entry.getValue();
            SimpleAnimationState<T> sourceState = stateMap.get(sourceName);

            if (stateDef.getTransitions() != null) {
                for (TransitionDefinition transDef : stateDef.getTransitions()) {
                    compileTransition(transDef, sourceState, stateMap);
                }
            }
        }

        // Step 3: Get entry state
        String entryStateName = definition.getStartState();
        SimpleAnimationState<T> entryState = stateMap.get(entryStateName);
        if (entryState == null) {
            throw new RuntimeException("Entry state not found: " + entryStateName);
        }

        return new CompiledStateMachine<>(name, entryState);
    }

    private <T extends BaseAnimationContext> SimpleAnimationState<T> compileState(StateDefinition stateDef) {

        SimpleAnimationState.Builder<T> builder = new SimpleAnimationState.Builder<>();

        // Compile on_enter actions
        if (stateDef.getOnEnter() != null && !stateDef.getOnEnter().isEmpty()) {
            List<Consumer<T>> actions = stateDef.getOnEnter().stream()
                    .map(actionCompiler::<T>compileAction)
                    .toList();
            builder.onEnter((context, fromState) -> {
                for (Consumer<T> action : actions) {
                    action.accept(context);
                }
            });
        }

        // Compile on_update actions
        if (stateDef.getOnUpdate() != null && !stateDef.getOnUpdate().isEmpty()) {
            List<Consumer<T>> actions = stateDef.getOnUpdate().stream()
                    .map(actionCompiler::<T>compileAction)
                    .toList();
            builder.onUpdate(context -> {
                for (Consumer<T> action : actions) {
                    action.accept(context);
                }
            });
        }

        // Compile on_exit actions
        if (stateDef.getOnExit() != null && !stateDef.getOnExit().isEmpty()) {
            List<Consumer<T>> actions = stateDef.getOnExit().stream()
                    .map(actionCompiler::<T>compileAction)
                    .toList();
            builder.onExit((context, toState) -> {
                for (Consumer<T> action : actions) {
                    action.accept(context);
                }
            });
        }

        // Compile evaluate pose
        Function<T, Pose> evaluateFunc = compileEvaluate(stateDef.getEvaluate());
        builder.evaluatePose(evaluateFunc);

        return builder.build();
    }

    private <T extends BaseAnimationContext> Function<T, Pose> compileEvaluate(EvaluateConfig evaluateConfig) {
        if (evaluateConfig == null) {
            throw new IllegalArgumentException("State must have an evaluate configuration");
        }

        String type = evaluateConfig.getType();
        if (type == null) {
            type = "script";
        }

        return switch (type) {
            // 由脚本决定输出
            case "script" -> {
                String scriptCode = evaluateConfig.getScript();
                if (scriptCode == null || scriptCode.isEmpty()) {
                    throw new IllegalArgumentException("Script evaluate requires script code");
                }
                org.mozillaa.javascript.Function compiledFunction = scriptCompiler.compile(scriptCode);
                yield context -> {
                    Object result = scriptCompiler.execute(compiledFunction, context);
                    if (result instanceof Wrapper wrapper) {
                        result = wrapper.unwrap();
                    }
                    if (result instanceof PoseHelper helper) {
                        return helper.build();
                    }
                    return DummyPose.INSTANCE;
                };
            }
            // 取得特定轨道的动画输出
            case "track" -> {
                String track = evaluateConfig.getTrack();
                if (track == null) {
                    throw new IllegalArgumentException("Runner evaluate requires 'track' field");
                }
                yield context -> {
                    var runnerHolder = context.getAnimationRunners();
                    var runner = runnerHolder.getAnimationRunner(track);
                    if (runner != null) {
                        return runner.evaluate();
                    }
                    return DummyPose.INSTANCE;
                };
            }
            default -> throw new IllegalArgumentException("Unknown evaluate type: " + type);
        };
    }

    private <T extends BaseAnimationContext> void compileTransition(
            TransitionDefinition transDef,
            SimpleAnimationState<T> sourceState,
            Map<String, SimpleAnimationState<T>> stateMap
    ) {
        String targetName = transDef.getTargetState();
        SimpleAnimationState<T> targetState = stateMap.get(targetName);
        if (targetState == null) {
            throw new RuntimeException("Target state not found: " + targetName);
        }

        SimpleTransition.Builder<T> builder = new SimpleTransition.Builder<T>()
                .from(sourceState)
                .target(targetState)
                .duration(transDef.getDuration());

        // Compile condition
        if (transDef.getCondition() != null) {
            Predicate<T> predicate = conditionCompiler.compileCondition(transDef.getCondition());
            builder.predicate(predicate);
        }

        builder.build();
    }
}
