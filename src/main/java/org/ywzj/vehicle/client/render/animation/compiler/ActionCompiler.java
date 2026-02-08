package org.ywzj.vehicle.client.render.animation.compiler;

import org.mozillaa.javascript.Script;
import org.ywzj.vehicle.client.render.animation.context.BaseAnimationContext;
import org.ywzj.vehicle.client.render.animation.context.SoundResolver;
import org.ywzj.vehicle.client.render.animation.util.AnimationPlayType;
import org.ywzj.vehicle.client.resource.animation.ActionDefinition;

import java.util.function.Consumer;

public class ActionCompiler {
    private final ScriptCompiler scriptCompiler;

    public ActionCompiler(ScriptCompiler scriptCompiler) {
        this.scriptCompiler = scriptCompiler;
    }

    public <T extends BaseAnimationContext> Consumer<T> compileAction(ActionDefinition def) {
        if (def == null) {
            return (context) -> {};
        }

        String type = def.getType();
        if (type == null) {
            throw new IllegalArgumentException("Action type is required");
        }

        return switch (type) {
            case "play_animation" -> compilePlayAnimation(def);
            case "set_variable" -> compileSetVariable(def);
            case "play_sound" -> compilePlaySound(def);
            case "script" -> compileScript(def);
            default -> throw new IllegalArgumentException("Unknown action type: " + type);
        };
    }

    // 播放动画动作
    private <T extends BaseAnimationContext> Consumer<T> compilePlayAnimation(ActionDefinition def) {
        String animationName = def.getAnimation();
        if (animationName == null) {
            throw new IllegalArgumentException("play_animation requires 'animation' field");
        }
        String track = def.getTrack() != null ? def.getTrack() : "main";

        AnimationPlayType playType = def.getPlayType();

        return (context) -> {
            var animation = context.getAnimation(animationName);
            if (animation != null) {
                context.getAnimationRunners().playAnimation(track, animation, playType);
            }
        };
    }

    // 设置变量动作
    private <T extends BaseAnimationContext> Consumer<T> compileSetVariable(ActionDefinition def) {
        String varName = def.getName();
        if (varName == null) {
            throw new IllegalArgumentException("set_variable requires 'name' field");
        }

        Object value = def.getValue();
        return (context) -> context.setParameter(varName, value);
    }

    // 播放声音动作
    private <T extends BaseAnimationContext> Consumer<T> compilePlaySound(ActionDefinition def) {
        var soundLocation = def.getSound();
        if (soundLocation == null) {
            throw new IllegalArgumentException("play_sound requires 'sound' field");
        }

        float volume = def.getVolume() != null ? def.getVolume() : 1.0f;
        float pitch = def.getPitch() != null ? def.getPitch() : 1.0f;

        return (context) -> {
            if (context instanceof SoundResolver soundContext) {
                soundContext.playSound(soundLocation, volume, pitch);
            }
        };
    }

    // 脚本动作
    private <T extends BaseAnimationContext> Consumer<T> compileScript(ActionDefinition def) {
        String scriptCode = def.getScript();
        if (scriptCode == null || scriptCode.isEmpty()) {
            return (context) -> {};
        }

        Script compiledScript = scriptCompiler.compile(scriptCode);
        return (context) -> scriptCompiler.execute(compiledScript, context);
    }
}
