package org.ywzj.vehicle.client.resource.vehicle;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 基础载具效果配置实例
 * 只保留基本的模型、纹理、动画、音效等资源信息
 */
public class BaseDisplay {

    protected ResourceLocation displayId;
    protected ResourceLocation modelPath;
    protected VehicleBedrockModel model;
    protected ResourceLocation texture;
    protected ResourceLocation slotTexture;
    protected Map<String, BedrockAnimation> animations = Map.of();
    protected ResourceLocation animationControllerRef;
    protected Map<String, SoundEvent> soundEvents = new HashMap<>();
    protected String description;
    protected List<SpecialBoneEffect> specialBoneEffects = new ArrayList<>();

    public BaseDisplay() {}

    /**
     * 需要在资源包重载期间构造，否则无法正常获取模型等资源
     * @param pojo 配置数据
     */
    public BaseDisplay(BaseDisplayPojo pojo) {
        var modelPojo = ClientAssetsManager.INSTANCE.getModel(pojo.model);
        this.modelPath = pojo.model;

        if (pojo.specialBoneEffects != null && !pojo.specialBoneEffects.isEmpty()) {
            this.specialBoneEffects = pojo.specialBoneEffects;
            modelPojo.ifPresent(bedrockModelPOJO ->
                this.model = new VehicleBedrockModel(bedrockModelPOJO, pojo.specialBoneEffects));
        } else {
            modelPojo.ifPresent(bedrockModelPOJO -> this.model = new VehicleBedrockModel(bedrockModelPOJO, List.of()));
        }

        this.texture = pojo.texture;
        this.slotTexture = pojo.slotTexture;
        this.animationControllerRef = pojo.animationController;

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

    public ResourceLocation getDisplayId() {
        return displayId;
    }

    public void setDisplayId(ResourceLocation displayId) {
        this.displayId = displayId;
    }

    public ResourceLocation getModelPath() {
        return modelPath;
    }

    public ResourceLocation getTexture() {
        return texture;
    }

    public ResourceLocation getSlotTexture() {
        return slotTexture;
    }

    public VehicleBedrockModel getModel() {
        return model;
    }

    public Map<String, BedrockAnimation> getAnimations() {
        return animations;
    }

    public ResourceLocation getAnimationControllerRef() {
        return animationControllerRef;
    }

    public Map<String, SoundEvent> getSoundEvents() {
        return soundEvents;
    }

    public String getDescription() {
        return description;
    }

    public List<SpecialBoneEffect> getSpecialBoneEffects() {
        return specialBoneEffects;
    }
}
