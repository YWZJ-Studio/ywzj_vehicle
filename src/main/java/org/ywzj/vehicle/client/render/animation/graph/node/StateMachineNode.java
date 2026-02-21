package org.ywzj.vehicle.client.render.animation.graph.node;

import com.maydaymemory.mae.basic.DummyPose;
import com.maydaymemory.mae.basic.Pose;
import com.maydaymemory.mae.control.statemachine.AnimationStateMachine;
import org.ywzj.vehicle.api.animation.IAnimationInstance;

/**
 * Pose node that references a state machine as pose source.
 */
public class StateMachineNode implements PoseNode {

    private final String stateMachineName;

    public StateMachineNode(String stateMachineName) {
        this.stateMachineName = stateMachineName;
    }

    @Override
    public Pose evaluate(IAnimationInstance<?> context) {
        AnimationStateMachine<?> stateMachine = context.getStateMachine(stateMachineName);
        if (stateMachine == null) {
            return DummyPose.INSTANCE;
        }
        return stateMachine.getPose();
    }

    public String getStateMachineName() {
        return stateMachineName;
    }

}
