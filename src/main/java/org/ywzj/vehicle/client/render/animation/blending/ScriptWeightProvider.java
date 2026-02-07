package org.ywzj.vehicle.client.render.animation.blending;

import org.mozillaa.javascript.Script;
import org.ywzj.vehicle.client.render.animation.compiler.ScriptCompiler;
import org.ywzj.vehicle.client.render.animation.context.BaseAnimationContext;

/**
 * Script-based weight provider that evaluates JavaScript to get weight.
 */
public class ScriptWeightProvider implements WeightProvider {
    private final Script compiledScript;
    private final ScriptCompiler scriptCompiler;

    public ScriptWeightProvider(Script compiledScript, ScriptCompiler scriptCompiler) {
        this.compiledScript = compiledScript;
        this.scriptCompiler = scriptCompiler;
    }

    @Override
    public float getWeight(BaseAnimationContext context) {
        Object result = scriptCompiler.execute(compiledScript, context);
        if (result instanceof Number) {
            return ((Number) result).floatValue();
        }
        return 1.0f; // Default weight
    }
}
