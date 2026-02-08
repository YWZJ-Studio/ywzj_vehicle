package org.ywzj.vehicle.client.resource.vehicle;

import net.minecraft.world.entity.Entity;
import org.mozillaa.javascript.*;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.api.animation.IAnimationInstance;
import org.ywzj.vehicle.api.scripts.ScriptContextFactory;
import org.ywzj.vehicle.client.render.animation.VehicleAnimationInstance;
import org.ywzj.vehicle.client.render.animation.compiler.*;
import org.ywzj.vehicle.client.render.animation.context.AnimationContextFactory;
import org.ywzj.vehicle.client.render.animation.context.EntityContext;
import org.ywzj.vehicle.client.render.animation.controller.AnimationController;
import org.ywzj.vehicle.client.render.animation.graph.ScriptPoseNode;
import org.ywzj.vehicle.client.render.animation.util.PoseHelper;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.client.resource.ScriptManager;
import org.ywzj.vehicle.client.resource.animation.AnimationControllerDefinition;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class VehicleDisplay<E extends Entity, CTX extends EntityContext<E>> extends BaseDisplay {
    
    protected AnimationController<?> animationController;
    protected AnimationContextFactory<E, CTX> contextFactory;
    protected Scriptable scriptScope;  // Cached script scope shared by this display instance
    protected Map<String, Function> scriptFunctions = new HashMap<>();  // Cached script functions

    public VehicleDisplay(BaseDisplayPojo pojo) {
        super(pojo);
    }

    public AnimationController<?> getAnimationController() {
        return animationController;
    }

    public void setContextFactory(AnimationContextFactory<E, CTX> factory) {
        this.contextFactory = factory;
    }

    public AnimationContextFactory<E, CTX> getContextFactory() {
        return contextFactory;
    }

    public void initializeAnimationController(ScriptManager scriptManager,
                                             ScriptContextFactory scriptContextFactory) {
        if (animationControllerRef == null) {
            return;
        }

        Optional<AnimationControllerDefinition> definitionOpt = ClientAssetsManager.INSTANCE.getAnimationControllerDefinition(animationControllerRef);
        if (definitionOpt.isEmpty()) {
            System.err.println("Animation controller definition not found: " + animationControllerRef);
            return;
        }

        AnimationController<?> controller = compileAnimationController(
            definitionOpt.get(), scriptManager, scriptContextFactory
        );
        
        if (controller != null) {
            this.animationController = controller;
        }
    }

    protected AnimationController<?> compileAnimationController(AnimationControllerDefinition definition,
                                                               ScriptManager scriptManager,
                                                               ScriptContextFactory scriptContextFactory) {
        try {
            // Load external script if specified
            Script compiledScript = null;
            if (definition.getScript() != null) {
                Optional<Script> scriptOpt = scriptManager.getScript(definition.getScript());
                if (scriptOpt.isPresent()) {
                    compiledScript = scriptOpt.get();
                } else {
                    YwzjVehicle.LOGGER.warn("Script not found: {}", definition.getScript());
                }
            }

            // Initialize script scope if script is present
            if (compiledScript != null) {
                initializeScriptScope(compiledScript, scriptContextFactory);
            }

            ScriptCompiler scriptCompiler = new ScriptCompiler(scriptContextFactory);

            if (compiledScript != null) {
                scriptCompiler.loadCompiledExternalScript(compiledScript);
            }

            ActionCompiler actionCompiler = new ActionCompiler(scriptCompiler);
            ConditionCompiler conditionCompiler = new ConditionCompiler(scriptCompiler);
            StateMachineCompiler stateMachineCompiler = new StateMachineCompiler(
                    scriptCompiler, actionCompiler, conditionCompiler);
            
            // Create script node resolver that uses cached functions and scope
            PoseGraphCompiler.ScriptNodeResolver scriptNodeResolver = functionName -> {
                Function function = scriptFunctions.get(functionName);
                if (function != null) {
                    return new ScriptPoseNode(function, scriptScope);
                }
                if (scriptScope != null) {
                    Object obj = scriptScope.get(functionName, scriptScope);
                    if (obj instanceof Function func) {
                        scriptFunctions.put(functionName, func);
                        return new ScriptPoseNode(func, scriptScope);
                    }
                }
                return null;

            };
            
            AnimationControllerCompiler controllerCompiler = new AnimationControllerCompiler(
                    stateMachineCompiler, scriptNodeResolver);

            return controllerCompiler.compile(definition);

        } catch (Exception e) {
            YwzjVehicle.LOGGER.warn("Failed to compile animation controller", e);
            return null;
        }
    }

    /**
     * Initialize script scope and cache script functions.
     * This scope is shared by all script pose nodes in this display.
     */
    protected void initializeScriptScope(Script compiledScript, ScriptContextFactory scriptContextFactory) {
        try (Context cx = scriptContextFactory.enterContext()) {
            this.scriptScope = scriptContextFactory.createScope(cx);

            BaseFunction createPoseHelperFunc = new BaseFunction() {
                @Override
                public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                    PoseHelper helper = new PoseHelper(VehicleDisplay.this.model);
                    return Context.javaToJS(helper, scope);
                }
            };
            scriptScope.put("createPoseBuilder", scriptScope, createPoseHelperFunc);
            
            // Execute script to define functions
            compiledScript.exec(cx, scriptScope);

            // Cache commonly used functions
            Object prepareBones = scriptScope.get("prepareBones", scriptScope);
            if (prepareBones instanceof Function function) {
                scriptFunctions.put("prepareBones", function);
            }
            
        } catch (Exception e) {
            YwzjVehicle.LOGGER.warn("Failed to initialize script scope", e);
        }
    }

    /**
     * Get cached script function by name.
     */
    public Function getScriptFunction(String name) {
        return scriptFunctions.get(name);
    }

    /**
     * Get script scope.
     */
    public Scriptable getScriptScope() {
        return scriptScope;
    }

    /**
     * Create animation instance for the given entity.
     * This method handles the type casting safely at runtime.
     */
    @SuppressWarnings("unchecked")
    public IAnimationInstance<CTX> createAnimationInstance(E entity) {
        if (animationController == null || contextFactory == null) {
            return null;
        }

        CTX context = contextFactory.create(entity);
        context.setAnimations(animations);

        AnimationController<CTX> typedController = (AnimationController<CTX>) animationController;
        return new VehicleAnimationInstance<>(typedController, context);
    }
}
