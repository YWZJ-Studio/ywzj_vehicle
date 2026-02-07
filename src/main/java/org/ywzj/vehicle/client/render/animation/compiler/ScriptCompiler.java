package org.ywzj.vehicle.client.render.animation.compiler;

import org.mozillaa.javascript.Context;
import org.mozillaa.javascript.Script;
import org.mozillaa.javascript.Scriptable;
import org.mozillaa.javascript.ScriptableObject;
import org.ywzj.vehicle.api.scripts.ScriptContextFactory;
import org.ywzj.vehicle.client.render.animation.context.BaseAnimationContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ScriptCompiler {
    private final Map<String, Script> compiledScriptCache = new ConcurrentHashMap<>();
    private final ScriptContextFactory contextFactory;
    private Script externalScript = null;

    public ScriptCompiler(ScriptContextFactory contextFactory) {
        this.contextFactory = contextFactory;
    }

    /**
     * Load and compile external script file content.
     * This script will be executed in the scope before inline scripts.
     */
    public void loadExternalScript(String scriptContent) {
        if (scriptContent != null && !scriptContent.isEmpty()) {
            try (Context cx = contextFactory.enterContext()) {
                this.externalScript = cx.compileString(scriptContent, "<external>", 1, null);
            } finally {
                Context.exit();
            }
        }
    }

    /**
     * Compile JavaScript code (with caching)
     */
    public Script compile(String scriptCode) {
        if (scriptCode == null || scriptCode.isEmpty()) {
            return null;
        }

        return compiledScriptCache.computeIfAbsent(scriptCode, code -> {
            try (Context cx = contextFactory.enterContext()) {
                return cx.compileString(code, "<inline>", 1, null);
            } finally {
                Context.exit();
            }
        });
    }

    /**
     * Execute a compiled script and return the result
     */
    public Object execute(Script script, BaseAnimationContext context) {
        if (script == null) {
            return null;
        }

        Context cx = contextFactory.enterContext();
        try {
            Scriptable scope = createScope(context, cx);
            return script.exec(cx, scope);
        } finally {
            Context.exit();
        }
    }

    /**
     * Execute a compiled script with bone index available in scope
     */
    public Object executeWithBoneIndex(Script script, BaseAnimationContext context, int boneIndex) {
        if (script == null) {
            return null;
        }

        Context cx = contextFactory.enterContext();
        try {
            Scriptable scope = createScope(context, cx);
            // Add bone_index variable to scope
            ScriptableObject.putProperty(scope, "bone_index", boneIndex);
            return script.exec(cx, scope);
        } finally {
            Context.exit();
        }
    }

    /**
     * Evaluate a script as a boolean condition
     */
    public boolean evaluateBoolean(Script script, BaseAnimationContext context) {
        Object result = execute(script, context);
        if (result instanceof Boolean) {
            return (Boolean) result;
        }
        // JavaScript truthy/falsy conversion
        return Context.toBoolean(result);
    }

    /**
     * Create a scope with context bindings
     */
    public Scriptable createScope(BaseAnimationContext context, Context cx) {
        Scriptable scope = cx.initStandardObjects();

        // Execute external script first to define helper functions
        if (externalScript != null) {
            externalScript.exec(cx, scope);
        }

        // Bind 'context' variable
        ScriptableObject.putProperty(scope, "context", Context.javaToJS(context, scope));

        return scope;
    }
}
