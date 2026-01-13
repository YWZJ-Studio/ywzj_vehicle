package org.ywzj.vehicle.client.resource.vehicle;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import org.mozillaa.javascript.*;
import org.ywzj.vehicle.api.scripts.ScriptContextFactory;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.scripts.VehicleScriptContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 基础载具效果配置实例
 */
public class BaseVehicleDisplay {

    public static final Object[] EMPTY_ARGS = new Object[0];
    protected BedrockModel model;
    protected ResourceLocation texture;
    protected ResourceLocation slotTexture;
    // todo 临时存这，实际使用需要封装成状态机
    protected Map<String, BedrockAnimation> animations = Map.of();
    protected Map<String, SoundEvent> soundEvents = new HashMap<>();
    protected Scriptable scope;
    protected Script script;
    protected String description;
    protected Function prepareBonesFunction;
    protected Function tickParticleFunction;
    protected VehicleScriptContext<? extends AbstractVehicle> vehicleScriptContext;

    public BaseVehicleDisplay() {}

    public VehicleScriptContext<? extends AbstractVehicle> buildVehicleScriptContext() {
        return new VehicleScriptContext<>(null, model);
    }

    /**
     * 需要在资源包重载期间构造，否则无法正常获取模型等资源
     * @param pojo 配置数据
     */
    public BaseVehicleDisplay(BaseVehicleDisplayPojo pojo) {
        var modelPojo = ClientAssetsManager.INSTANCE.getModel(pojo.model);
        this.model = modelPojo.map(BedrockModel::new).orElseThrow();

        if (pojo.script != null) {
            this.script = ClientAssetsManager.INSTANCE.getScript(pojo.script).orElse(null);
            if (this.script != null) {
                try (var context = ScriptContextFactory.get().enterContext()) {
                    this.scope = ScriptContextFactory.get().createScope(context);

                    script.exec(context, this.scope);

                    var func = this.scope.get("prepareBones", this.scope);
                    if (func instanceof Function function) {
                        this.prepareBonesFunction = function;
                    }

                    var tickParticleFunc = this.scope.get("tickParticle", this.scope);
                    if (tickParticleFunc instanceof Function function) {
                        this.tickParticleFunction = function;
                    }

                    this.vehicleScriptContext = buildVehicleScriptContext();
                    Object vehicleScriptContextJs = Context.javaToJS(this.vehicleScriptContext, this.scope);
                    ScriptableObject.defineProperty(this.scope, "context", vehicleScriptContextJs, ScriptableObject.READONLY | ScriptableObject.PERMANENT);
                }
            }
        }

        this.texture = pojo.texture;
        this.slotTexture = pojo.slotTexture;

        if (pojo.animations != null) {
            var animationPojo = ClientAssetsManager.INSTANCE.getAnimation(pojo.animations);
            var animations = animationPojo
                    .map(animationPOJO -> BedrockAnimation.createAnimation(animationPOJO, model))
                    .orElse(List.of());
            var map = new HashMap<String, BedrockAnimation>();
            for (var anim : animations) {
                map.put(anim.getName(), anim);
            }
            this.animations = map;
        } else {
            this.animations = Map.of();
        }

        if (pojo.sounds != null) {
            pojo.sounds.forEach((soundName, soundResourceLocation) ->
                    soundEvents.put(soundName, SoundEvent.createVariableRangeEvent(soundResourceLocation)));
        }

        this.description = pojo.description;
    }

    public ResourceLocation getTexture() {
        return texture;
    }

    public ResourceLocation getSlotTexture() {
        return slotTexture;
    }

    public BedrockModel getModel() {
        return model;
    }

    public Map<String, BedrockAnimation> getAnimations() {
        return animations;
    }

    public Map<String, SoundEvent> getSoundEvents() {
        return soundEvents;
    }

    public Script getScript() {
        return script;
    }

    public Function getPrepareBonesFunction() {
        return prepareBonesFunction;
    }

    public Function getTickParticleFunction() {
        return tickParticleFunction;
    }

    public Scriptable getScope() {
        return scope;
    }

    public VehicleScriptContext getVehicleScriptContext() {
        return vehicleScriptContext;
    }

    public String getDescription() {
        return description;
    }

}
