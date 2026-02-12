package org.ywzj.vehicle.client.render.animation.compiler;

import org.mozillaa.javascript.Context;
import org.mozillaa.javascript.Function;
import org.mozillaa.javascript.Scriptable;
import org.ywzj.vehicle.api.scripts.ScriptContextFactory;
import org.ywzj.vehicle.client.render.animation.context.BaseAnimationContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ScriptCompiler {
    private final Map<String, Function> compiledFunctionCache = new ConcurrentHashMap<>();
    private final ScriptContextFactory contextFactory;
    private final Scriptable scriptScope;

    public ScriptCompiler(ScriptContextFactory contextFactory, Scriptable scriptScope) {
        this.contextFactory = contextFactory;
        this.scriptScope = scriptScope;
    }

    public Function compile(String scriptCode) {
        if (scriptCode == null || scriptCode.isEmpty()) {
            return null;
        }

        return compiledFunctionCache.computeIfAbsent(scriptCode, code -> {
            try (Context cx = contextFactory.enterContext()) {
                // 包裹内联表达式
                String wrappedCode = "(function(context) { return (" + code + "); })";
                
                Scriptable scope = scriptScope != null ? scriptScope : cx.initStandardObjects();
                Object result = cx.evaluateString(scope, wrappedCode, "<inline>", 1, null);
                
                if (result instanceof Function) {
                    return (Function) result;
                }
                throw new IllegalStateException("Failed to compile script as function: " + code);
            }
        });
    }

    public Object execute(Function function, BaseAnimationContext context) {
        if (function == null) {
            return null;
        }

        Context cx = contextFactory.enterContext();
        try {
            Scriptable scope = scriptScope != null ? scriptScope : cx.initStandardObjects();
            Object contextArg = Context.javaToJS(context, scope);
            return function.call(cx, scope, scope, new Object[]{contextArg});
        } finally {
            Context.exit();
        }
    }

    /**
     * Evaluate a function as a boolean condition.
     * Uses the shared scriptScope from VehicleDisplay.
     */
    public boolean evaluateBoolean(Function function, BaseAnimationContext context) {
        Object result = execute(function, context);
        if (result instanceof Boolean) {
            return (Boolean) result;
        }
        return Context.toBoolean(result);
    }
}
