package org.ywzj.vehicle.client.render.animation.context;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation;
import org.ywzj.vehicle.client.render.animation.util.AnimationRunnerHolder;
import org.ywzj.vehicle.client.render.animation.util.LoopAnimationRunner;
import org.ywzj.vehicle.client.render.animation.util.SwitchableRunner;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
    // 循环动画播放器
    private final Map<String, LoopAnimationRunner> loopRunners = new ConcurrentHashMap<>();
    // 可用动画
    private Map<String, BedrockAnimation> animations;
    // 脚本自定义变量
    private Object scriptParam;

    private final Set<String> events = new HashSet<>();

    public BaseAnimationContext() {
    }

    public void saveScriptParam(Object param) {
        this.scriptParam = param;
    }

    public Object getScriptParam() {
        return scriptParam;
    }

    public void offerEvent(String event) {
        events.add(event);
    }

    public boolean hasEvent(String event) {
        return events.contains(event);
    }

    public void clearEvents() {
        events.clear();
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
        for (LoopAnimationRunner runner : loopRunners.values()) {
            runner.tick();
        }
    }

    public SwitchableRunner getSwitchableRunner(String key) {
        return switchableRunners.get(key);
    }

    public void addSwitchableRunner(String key, SwitchableRunner runner) {
        switchableRunners.put(key, runner);
    }

    public void addLoopRunner(String key, LoopAnimationRunner loopAnimationRunner) {
        loopRunners.put(key, loopAnimationRunner);
    }

    public LoopAnimationRunner getLoopRunner(String key) {
        return loopRunners.get(key);
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
    
    /**
     * Get value from a data source for special bindings.
     * Override this method in subclasses to provide custom data sources.
     * 
     * @param source The data source name
     * @param param Optional parameter for the data source
     * @return The value from the data source, or 0 if not supported
     */
    public float getBindingValue(String source, Float param) {
        return 0f;
    }
    // endregion
}
