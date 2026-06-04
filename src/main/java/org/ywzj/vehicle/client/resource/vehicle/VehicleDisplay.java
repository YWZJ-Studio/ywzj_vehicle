package org.ywzj.vehicle.client.resource.vehicle;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation;
import org.mozillaa.javascript.*;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.api.animation.IAnimationInstance;
import org.ywzj.vehicle.api.scripts.ScriptContextFactory;
import org.ywzj.vehicle.client.render.animation.VehicleAnimationInstance;
import org.ywzj.vehicle.client.render.animation.compiler.*;
import org.ywzj.vehicle.client.render.animation.context.AnimationContextFactory;
import org.ywzj.vehicle.client.render.animation.context.VehicleContext;
import org.ywzj.vehicle.client.render.animation.controller.AnimationController;
import org.ywzj.vehicle.client.render.animation.graph.node.ScriptPoseNode;
import org.ywzj.vehicle.client.render.animation.runner.LoopAnimationRunner;
import org.ywzj.vehicle.client.render.animation.runner.SwitchableRunner;
import org.ywzj.vehicle.client.render.animation.util.AnimationHandler;
import org.ywzj.vehicle.client.render.animation.util.PoseHelper;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.client.resource.ScriptManager;
import org.ywzj.vehicle.client.resource.animation.AnimationControllerDefinition;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.part.SwitchableUnit;

import java.util.*;

public class VehicleDisplay<E extends AbstractVehicle, CTX extends VehicleContext<E>> extends BaseDisplay {
    
    protected AnimationController<CTX> animationController;
    protected AnimationContextFactory<E, CTX> contextFactory;
    protected Scriptable scriptScope;
    protected Map<String, Function> scriptFunctions = new HashMap<>();

    public VehicleDisplay(BaseDisplayPojo pojo) {
        super(pojo);
    }

    public AnimationController<CTX> getAnimationController() {
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
        if (animationControllerPath == null) {
            return;
        }

        Optional<AnimationControllerDefinition> definitionOpt = ClientAssetsManager.INSTANCE.getAnimationControllerDefinition(animationControllerPath);
        if (definitionOpt.isEmpty()) {
            YwzjVehicle.LOGGER.warn("Animation controller definition not found: {}", animationControllerPath);
            return;
        }

        AnimationController<CTX> controller = compileAnimationController(
            definitionOpt.get(), scriptManager, scriptContextFactory
        );
        
        if (controller != null) {
            this.animationController = controller;
        }
    }

    protected AnimationController<CTX> compileAnimationController(AnimationControllerDefinition definition,
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

            // 初始化此display的脚本作用域，并导入外部脚本定义的函数
            if (compiledScript != null) {
                initializeScriptScope(compiledScript, scriptContextFactory);
            }

            ScriptCompiler scriptCompiler = new ScriptCompiler(scriptContextFactory, scriptScope);
            ActionCompiler actionCompiler = new ActionCompiler(scriptCompiler);
            ConditionCompiler conditionCompiler = new ConditionCompiler(scriptCompiler);
            StateMachineCompiler stateMachineCompiler = new StateMachineCompiler(scriptCompiler, actionCompiler, conditionCompiler);

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
                    stateMachineCompiler, scriptNodeResolver, model
            );

            return controllerCompiler.compile(definition);
        } catch (Exception e) {
            YwzjVehicle.LOGGER.warn("Failed to compile animation controller", e);
            return null;
        }
    }

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
        } catch (Exception e) {
            YwzjVehicle.LOGGER.warn("Failed to initialize script scope", e);
        }
    }

    public IAnimationInstance<CTX> createAnimationInstance(E entity) {
        if (animationController == null || contextFactory == null) {
            return null;
        }

        CTX context = contextFactory.create(entity);
        context.setAnimations(animations);

        AnimationController<CTX> typedController = animationController;
        Set<String> partIds = new HashSet<>();
        for (var entry : typedController.getSwitchableAnimations().entrySet()) {
            String animationName = entry.getValue().getAnimation();
            boolean invert = entry.getValue().isInvert();
            BedrockAnimation animation = context.getAnimation(animationName);
            if (animation == null) {
                continue;
            }
            entity.getPartUnit(entry.getValue().getPartId()).ifPresent(part -> {
                if (part instanceof SwitchableUnit<?> switchablePart) {
                    context.addSwitchableRunner(entry.getKey(), new SwitchableRunner(switchablePart, animation, invert));
                    partIds.add(entry.getKey());
                }
            });
        }

        // 尝试为所有未声明pose源，但是存在对应名称动画的SwitchableUnit添加SwitchableRunner
        for (var unit : entity.getPartUnits()) {
            if (unit instanceof SwitchableUnit<?> switchableUnit) {
                String partId = unit.getId();
                BedrockAnimation animation = context.getAnimation(partId + "_switch");
                if (animation == null) {
                    continue;
                }
                if (!partIds.contains(partId)) {
                    context.addSwitchableRunner(partId, new SwitchableRunner(switchableUnit, animation, switchableUnit.defaultOpen()));
                }
            }
        }

        for (var entry : typedController.getLoopAnimations().entrySet()) {
            String animationName = entry.getValue().getAnimation();
            BedrockAnimation animation = context.getAnimation(animationName);
            if (animation == null) {
                continue;
            }
            context.addLoopRunner(entry.getKey(), new LoopAnimationRunner(animation));
        }

        var eventAnimations = typedController.getEventAnimations();
        if (eventAnimations != null) {
            context.setEventAnimationHandler(new AnimationHandler(context::getAnimation, eventAnimations));
        }

        return new VehicleAnimationInstance<>(typedController, context);
    }

}
