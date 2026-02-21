package org.ywzj.vehicle.client.render.animation.compiler;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.BoneIndexProvider;
import org.ywzj.vehicle.client.render.animation.context.BaseAnimationContext;
import org.ywzj.vehicle.client.render.animation.controller.AnimationController;
import org.ywzj.vehicle.client.render.animation.controller.CompiledStateMachine;
import org.ywzj.vehicle.client.render.animation.graph.PoseGraph;
import org.ywzj.vehicle.client.resource.animation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnimationControllerCompiler {
    private final StateMachineCompiler stateMachineCompiler;
    private final PoseGraphCompiler poseGraphCompiler;

    public AnimationControllerCompiler(StateMachineCompiler stateMachineCompiler,
                                       PoseGraphCompiler.ScriptNodeResolver scriptNodeResolver,
                                       BoneIndexProvider boneIndexProvider) {
        this.stateMachineCompiler = stateMachineCompiler;
        this.poseGraphCompiler = new PoseGraphCompiler(scriptNodeResolver, boneIndexProvider);
    }

    public <T extends BaseAnimationContext> AnimationController<T> compile(AnimationControllerDefinition definition) {
        String name = definition.getName();

        // Get parameter definitions
        Map<String, ParameterDefinition> parameters = definition.getParameters();

        // Compile all state machines
        Map<String, CompiledStateMachine<T>> compiledStateMachines = new HashMap<>();
        if (definition.getStateMachines() != null) {
            for (Map.Entry<String, StateMachineDefinition> entry : definition.getStateMachines().entrySet()) {
                String smName = entry.getKey();
                StateMachineDefinition smDef = entry.getValue();
                CompiledStateMachine<T> compiledSM = stateMachineCompiler.compile(smDef);
                compiledStateMachines.put(smName, compiledSM);
            }
        }

        Map<String, SwitchableAnimationDefinition> switchableAnimations = definition.getSwitchableAnimations();
        // todo 其实应该带条件，现在暂时原样传，后续补
        Map<String, LoopAnimationDefinition> loopAnimations = definition.getLoopAnimations();
        Map<String, List<String>> eventAnimations = definition.getEventAnimations();

        // Compile pose graph
        PoseGraph poseGraph = null;
        if (definition.getGraph() != null) {
            poseGraph = poseGraphCompiler.compile(definition.getGraph());
        }

        return new AnimationController<>(
                name, parameters, compiledStateMachines,
                switchableAnimations,
                loopAnimations,
                eventAnimations,
                poseGraph
        );
    }
}
