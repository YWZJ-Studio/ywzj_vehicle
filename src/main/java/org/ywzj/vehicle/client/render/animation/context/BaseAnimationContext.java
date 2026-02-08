package org.ywzj.vehicle.client.render.animation.context;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation;
import com.maydaymemory.mae.control.Tickable;
import org.ywzj.vehicle.client.render.animation.util.AnimationRunnerHolder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基本动画上下文
 */
public abstract class BaseAnimationContext implements Tickable {
    protected float partialTick;
    // 自定义变量
    private final Map<String, Object> parameters = new ConcurrentHashMap<>();
    // 动画播放管理器
    private final AnimationRunnerHolder animationRunnerHolder = new AnimationRunnerHolder(this::getAnimation);
    // 可用动画
    private Map<String, BedrockAnimation> animations;

    public BaseAnimationContext() {
    }

    public void setPartialTick(float partialTick) {
        this.partialTick = partialTick;
    }

    public float getPartialTick() {
        return partialTick;
    }

    public void setAnimations(Map<String, BedrockAnimation> animations) {
        this.animations = animations;
    }

    @Override
    public void tick() {
        animationRunnerHolder.tick();
    }

    public AnimationRunnerHolder getAnimationRunners() {
        return animationRunnerHolder;
    }

    public BedrockAnimation getAnimation(String name) {
        return animations.get(name);
    }

    public Map<String, BedrockAnimation> getAnimations() {
        return animations;
    }

    // region 参数管理
    public void setParameter(String name, Object value) {
        parameters.put(name, value);
    }

    public void setFloat(String name, float value) {
        parameters.put(name, value);
    }

    public void setBool(String name, boolean value) {
        parameters.put(name, value);
    }

    public void setInt(String name, int value) {
        parameters.put(name, value);
    }

    public Object getParameter(String name) {
        return parameters.get(name);
    }

    public float getFloat(String name, float defaultValue) {
        Object value = parameters.get(name);
        if (value instanceof Number number) {
            return number.floatValue();
        }
        return defaultValue;
    }

    public boolean getBool(String name, boolean defaultValue) {
        Object value = parameters.get(name);
        if (value instanceof Boolean bool) {
            return bool;
        }
        return defaultValue;
    }

    public int getInt(String name, int defaultValue) {
        Object value = parameters.get(name);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return defaultValue;
    }
    // endregion
}
