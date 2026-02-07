package org.ywzj.vehicle.client.render.animation.compiler;

import org.ywzj.vehicle.client.render.animation.context.BaseAnimationContext;
import org.ywzj.vehicle.client.render.animation.controller.AnimationController;
import org.ywzj.vehicle.client.render.animation.controller.CompiledStateMachine;
import org.ywzj.vehicle.client.render.animation.graph.BoneMask;
import org.ywzj.vehicle.client.render.animation.graph.PoseGraph;
import org.ywzj.vehicle.client.resource.animation.AnimationControllerDefinition;
import org.ywzj.vehicle.client.resource.animation.ParameterDefinition;
import org.ywzj.vehicle.client.resource.animation.StateMachineDefinition;

import java.util.HashMap;
import java.util.Map;

/**
 * Compiles animation controller definitions into runtime AnimationController objects.
 * New architecture: Parameters + State Machines + Pose Graph
 */
public class AnimationControllerCompiler {
    private final StateMachineCompiler stateMachineCompiler;
    private final PoseGraphCompiler poseGraphCompiler;

    public AnimationControllerCompiler(StateMachineCompiler stateMachineCompiler,
                                      Map<String, BoneMask> boneMasks) {
        this.stateMachineCompiler = stateMachineCompiler;
        this.poseGraphCompiler = new PoseGraphCompiler(boneMasks);
    }

    /**
     * Compile an animation controller definition
     */
    public <T extends BaseAnimationContext> AnimationController<T> compile(
            AnimationControllerDefinition definition) {

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

        // Compile pose graph
        PoseGraph poseGraph = null;
        if (definition.getGraph() != null) {
            poseGraph = poseGraphCompiler.compile(definition.getGraph());
        }

        return new AnimationController<>(name, parameters, compiledStateMachines, poseGraph);
    }
}
