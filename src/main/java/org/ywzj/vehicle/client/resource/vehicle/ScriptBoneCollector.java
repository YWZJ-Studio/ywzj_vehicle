package org.ywzj.vehicle.client.resource.vehicle;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.BoneIndexProvider;
import org.jetbrains.annotations.Nullable;
import org.mozillaa.javascript.*;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.api.scripts.ScriptContextFactory;
import org.ywzj.vehicle.client.render.animation.util.PoseHelper;
import org.ywzj.vehicle.client.resource.animation.*;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

final class ScriptBoneCollector {

    private static final double[] CONTEXT_VALUES = {-1, 0, 1};

    private ScriptBoneCollector() {}

    static Set<String> collect(AnimationControllerDefinition controller, @Nullable Script script) {
        Set<String> functionNames = new LinkedHashSet<>();
        collectScriptFunctions(controller.getGraph(), functionNames);
        Set<String> inlineScripts = collectInlineScripts(controller);
        if (functionNames.isEmpty() && inlineScripts.isEmpty()) {
            return Set.of();
        }

        Set<String> bones = new LinkedHashSet<>();
        Map<String, Integer> boneIndexes = new HashMap<>();
        BoneIndexProvider collectingIndexProvider = boneName -> {
            if (boneName == null || boneName.isBlank()) {
                return -1;
            }
            bones.add(boneName);
            return boneIndexes.computeIfAbsent(boneName, ignored -> boneIndexes.size());
        };

        ScriptContextFactory contextFactory = ScriptContextFactory.get();
        try (Context context = contextFactory.enterContext()) {
            Scriptable scope = contextFactory.createScope(context);
            BaseFunction createPoseBuilder = new BaseFunction() {
                @Override
                public Object call(Context cx, Scriptable callScope, Scriptable thisObj, Object[] args) {
                    return Context.javaToJS(new PoseHelper(collectingIndexProvider), scope);
                }
            };
            ScriptableObject.putProperty(scope, "createPoseBuilder", createPoseBuilder);
            if (script != null) {
                script.exec(context, scope);
            }

            for (String functionName : functionNames) {
                Object value = ScriptableObject.getProperty(scope, functionName);
                if (!(value instanceof Function function)) {
                    YwzjVehicle.LOGGER.warn("Unable to collect script bones: function {} was not found", functionName);
                    continue;
                }
                executeFunction(context, scope, function, functionName, collectingIndexProvider);
            }
            for (String inlineScript : inlineScripts) {
                try {
                    String wrappedCode = "(function(context) { return (" + inlineScript + "); })";
                    Object value = context.evaluateString(scope, wrappedCode, "<bone-collector>", 1, null);
                    if (value instanceof Function function) {
                        executeFunction(context, scope, function, "<inline>", collectingIndexProvider);
                    }
                } catch (RuntimeException exception) {
                    YwzjVehicle.LOGGER.debug("Unable to compile inline script for bone collection: {}", inlineScript, exception);
                }
            }
        } catch (RuntimeException exception) {
            YwzjVehicle.LOGGER.warn("Failed to initialize script bone collection", exception);
        }
        return bones;
    }

    private static void executeFunction(Context context, Scriptable scope, Function function, String functionName, BoneIndexProvider boneIndexProvider) {
        for (double contextValue : CONTEXT_VALUES) {
            try {
                CollectionContext collectionContext = new CollectionContext(scope, contextValue, boneIndexProvider);
                function.call(context, scope, scope, new Object[]{collectionContext});
            } catch (RuntimeException exception) {
                YwzjVehicle.LOGGER.debug(
                        "Script bone collection stopped early for function {} with context value {}",
                        functionName, contextValue, exception
                );
            }
        }
    }

    private static Set<String> collectInlineScripts(AnimationControllerDefinition controller) {
        Set<String> inlineScripts = new LinkedHashSet<>();
        Map<String, StateMachineDefinition> stateMachines = controller.getStateMachines();
        if (stateMachines == null) {
            return inlineScripts;
        }
        for (StateMachineDefinition stateMachine : stateMachines.values()) {
            if (stateMachine == null || stateMachine.getStates() == null) {
                continue;
            }
            for (StateDefinition state : stateMachine.getStates().values()) {
                EvaluateConfig evaluate = state == null ? null : state.getEvaluate();
                if (evaluate == null || (evaluate.getType() != null && !"script".equalsIgnoreCase(evaluate.getType()))) {
                    continue;
                }
                if (evaluate.getScript() != null && !evaluate.getScript().isBlank()) {
                    inlineScripts.add(evaluate.getScript());
                }
            }
        }
        return inlineScripts;
    }

    private static void collectScriptFunctions(PoseNodeDefinition node, Set<String> functionNames) {
        if (node == null) {
            return;
        }
        if ("script".equalsIgnoreCase(node.getType()) && node.getFunction() != null && !node.getFunction().isBlank()) {
            functionNames.add(node.getFunction());
        }
        if (node.getInputs() != null) {
            for (PoseNodeDefinition input : node.getInputs()) {
                collectScriptFunctions(input, functionNames);
            }
        }
        collectScriptFunctions(node.getA(), functionNames);
        collectScriptFunctions(node.getB(), functionNames);
        collectScriptFunctions(node.getBase(), functionNames);
        collectScriptFunctions(node.getAdd(), functionNames);
        if (node.getLayers() != null) {
            for (PoseNodeDefinition.LayerDefinition layer : node.getLayers()) {
                collectScriptFunctions(layer.getPose(), functionNames);
            }
        }
    }

    private static final class CollectionContext extends ScriptableObject {

        private final double defaultValue;
        private final BoneIndexProvider boneIndexProvider;

        private CollectionContext(Scriptable scope, double defaultValue, BoneIndexProvider boneIndexProvider) {
            this.defaultValue = defaultValue;
            this.boneIndexProvider = boneIndexProvider;
            setParentScope(scope);
            setPrototype(ScriptableObject.getObjectPrototype(scope));
        }

        @Override
        public String getClassName() {
            return "BoneCollectionContext";
        }

        @Override
        public boolean has(String name, Scriptable start) {
            return true;
        }

        @Override
        public Object get(String name, Scriptable start) {
            Object value = super.get(name, start);
            if (value != Scriptable.NOT_FOUND) {
                return value;
            }
            BaseFunction fallback = new BaseFunction() {
                @Override
                public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                    if (name.startsWith("set")) {
                        if (name.startsWith("setBone") && args.length > 0 && args[0] instanceof CharSequence boneName) {
                            boneIndexProvider.getIndex(boneName.toString());
                        }
                        return Undefined.instance;
                    }
                    if (name.startsWith("has") || name.startsWith("is")) {
                        return defaultValue != 0;
                    }
                    if (name.equals("getFloat") && args.length > 1) {
                        return args[1];
                    }
                    if (name.endsWith("Name")) {
                        return "";
                    }
                    return defaultValue;
                }
            };
            put(name, this, fallback);
            return fallback;
        }
    }

}
