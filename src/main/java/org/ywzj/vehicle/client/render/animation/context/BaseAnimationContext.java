package org.ywzj.vehicle.client.render.animation.context;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation;
import org.ywzj.vehicle.client.render.animation.util.AnimationRunnerHolder;
import org.ywzj.vehicle.client.render.animation.util.SwitchableRunner;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基本动画上下文<br/>
 * 由于在状态机之间共享，如果实现ITickable会导致被重复调用，故需要在AnimationInstance中手动调用tick方法<br/>
 */
public abstract class BaseAnimationContext {
    protected float partialTick;
    // 自定义变量
    private final Map<String, Object> parameters = new ConcurrentHashMap<>();
    // 动画播放管理器
    private final AnimationRunnerHolder animationRunnerHolder = new AnimationRunnerHolder(this::getAnimation);
    // 切换动画播放器
    private final Map<String, SwitchableRunner> switchableRunners = new ConcurrentHashMap<>();
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

    public void tick() {
        animationRunnerHolder.tick();
        for (SwitchableRunner runner : switchableRunners.values()) {
            runner.tick();
        }
    }

    public SwitchableRunner getSwitchableRunner(String key) {
        return switchableRunners.get(key);
    }

    public void addSwitchableRunner(String key, SwitchableRunner runner) {
        switchableRunners.put(key, runner);
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
