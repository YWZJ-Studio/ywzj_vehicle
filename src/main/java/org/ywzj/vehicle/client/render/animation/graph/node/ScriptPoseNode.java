package org.ywzj.vehicle.client.render.animation.graph.node;

import com.maydaymemory.mae.basic.Pose;
import org.mozillaa.javascript.Function;
import org.mozillaa.javascript.Scriptable;
import org.mozillaa.javascript.Wrapper;
import org.ywzj.vehicle.api.animation.IAnimationInstance;
import org.ywzj.vehicle.api.scripts.ScriptContextFactory;
import org.ywzj.vehicle.client.render.animation.util.PoseHelper;

/**
 * 脚本节点，经由JavaScript的指定方法获取pose
 */
public class ScriptPoseNode implements PoseNode {
    
    private final Function scriptFunction;
    private final Scriptable scope;

    public ScriptPoseNode(Function scriptFunction, Scriptable scope) {
        this.scriptFunction = scriptFunction;
        this.scope = scope;
    }

    @Override
    public Pose evaluate(IAnimationInstance<?> context) {
        if (scriptFunction == null) {
            return null;
        }
        try (var ctx = ScriptContextFactory.get().enterContext()) {
            Object result = scriptFunction.call(
                    ctx,
                    scope,
                    scope,
                    new Object[]{context.getContext()}
            );
            if (result instanceof Wrapper wrapper) {
                result = wrapper.unwrap();
            }
            if (result instanceof PoseHelper pose) {
                return pose.build();
            }
        }
        return null;
    }

}
