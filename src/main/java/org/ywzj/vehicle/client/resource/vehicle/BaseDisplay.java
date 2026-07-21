package org.ywzj.vehicle.client.resource.vehicle;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.BoneIndexProvider;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.baked.BakerOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import org.jetbrains.annotations.Nullable;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.client.resource.animation.AnimationControllerDefinition;
import org.ywzj.vehicle.client.resource.animation.PoseNodeDefinition;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

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
    protected ResourceLocation animationControllerPath;
    protected Map<String, SoundEvent> soundEvents = new HashMap<>();
    protected String description;
    protected int tabIndex;
    protected List<SpecialBoneEffect> specialBoneEffects = new ArrayList<>();

    public BaseDisplay() {}

    /**
     * 需要在资源包重载期间构造，否则无法正常获取模型等资源
     * @param pojo 配置数据
     */
    public BaseDisplay(BaseDisplayPojo pojo) {
        var animationPojo = pojo.animations == null ? null : ClientAssetsManager.INSTANCE.getAnimation(pojo.animations).orElse(null);
        Set<String> animatedBones = animationPojo == null ? Set.of() : BakerOptions.collectAnimatedBones(animationPojo);
        BakerOptions bakerOptions = createBakerOptions(pojo, animatedBones);
        var modelPojo = ClientAssetsManager.INSTANCE.getModel(pojo.model);
        this.modelPath = pojo.model;
        this.specialBoneEffects = pojo.specialBoneEffects == null ? List.of() : List.copyOf(pojo.specialBoneEffects);
        modelPojo.ifPresent(bedrockModelPOJO ->
                this.model = new VehicleBedrockModel(bedrockModelPOJO, specialBoneEffects, bakerOptions));

        this.texture = pojo.texture;
        this.slotTexture = pojo.slotTexture;
        this.animationControllerPath = pojo.animationController;

        BoneIndexProvider animationIndexProvider = getAnimationIndexProvider();
        if (animationPojo != null && animationIndexProvider != null) {
            var loadedAnimations = BedrockAnimation.createAnimation(animationPojo, animationIndexProvider);
            var map = new HashMap<String, BedrockAnimation>();
            for (var animation : loadedAnimations) {
                map.put(animation.getName(), animation);
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
        this.tabIndex = pojo.tabIndex;
    }

    @Nullable
    protected BoneIndexProvider getAnimationIndexProvider() {
        if (model == null) {
            return null;
        }
        BoneIndexProvider bakedModel = model.getBakedModel();
        return bakedModel != null ? bakedModel : model;
    }

    @Nullable
    private static BakerOptions createBakerOptions(BaseDisplayPojo pojo, Set<String> animatedBones) {
        BaseDisplayPojo.BakedModelConfig bakedModel = pojo.bakedModel;
        if (bakedModel == null || !bakedModel.enabled) {
            return null;
        }

        Set<String> preservedBones = new LinkedHashSet<>(animatedBones);
        if (pojo.animationController != null) {
            AnimationControllerDefinition controller = ClientAssetsManager.INSTANCE
                    .getAnimationControllerDefinition(pojo.animationController)
                    .orElse(null);
            if (controller == null) {
                YwzjVehicle.LOGGER.warn("Baked model disabled because animation controller is missing: {}", pojo.animationController);
                return null;
            }
            collectControllerBindingBones(controller.getGraph(), preservedBones);
        }
        addBones(preservedBones, bakedModel.preserveBones);

        Set<Pattern> preservedBonePatterns = new LinkedHashSet<>();
        if (bakedModel.preserveBoneRegexes != null) {
            try {
                for (String regex : bakedModel.preserveBoneRegexes) {
                    if (regex != null && !regex.isBlank()) {
                        preservedBonePatterns.add(Pattern.compile(regex));
                    }
                }
            } catch (PatternSyntaxException exception) {
                YwzjVehicle.LOGGER.error("Baked model disabled because preserve_bone_regexes is invalid for {}", pojo.model, exception);
                return null;
            }
        }

        return new BakerOptions(animatedBones, preservedBones, preservedBonePatterns, true, false, true);
    }

    private static void collectControllerBindingBones(@Nullable PoseNodeDefinition node, Set<String> preservedBones) {
        if (node == null) {
            return;
        }
        if (node.getSpecialBindings() != null) {
            for (PoseNodeDefinition.SpecialBindingDefinition binding : node.getSpecialBindings()) {
                addBones(preservedBones, binding.getBones());
            }
        }
        if (node.getPartBindings() != null) {
            for (PoseNodeDefinition.PartBindingDefinition binding : node.getPartBindings()) {
                addBone(preservedBones, binding.getBone());
            }
        }
        if (node.getInputs() != null) {
            for (PoseNodeDefinition input : node.getInputs()) {
                collectControllerBindingBones(input, preservedBones);
            }
        }
        collectControllerBindingBones(node.getA(), preservedBones);
        collectControllerBindingBones(node.getB(), preservedBones);
        collectControllerBindingBones(node.getBase(), preservedBones);
        collectControllerBindingBones(node.getAdd(), preservedBones);
        if (node.getLayers() != null) {
            for (PoseNodeDefinition.LayerDefinition layer : node.getLayers()) {
                collectControllerBindingBones(layer.getPose(), preservedBones);
            }
        }
    }

    private static void addBones(Set<String> preservedBones, @Nullable List<String> bones) {
        if (bones == null) {
            return;
        }
        for (String bone : bones) {
            addBone(preservedBones, bone);
        }
    }

    private static void addBone(Set<String> preservedBones, @Nullable String bone) {
        if (bone != null && !bone.isBlank()) {
            preservedBones.add(bone);
        }
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

    public ResourceLocation getAnimationControllerPath() {
        return animationControllerPath;
    }

    public Map<String, SoundEvent> getSoundEvents() {
        return soundEvents;
    }

    public String getDescription() {
        return description;
    }

    public int getTabIndex() {
        return tabIndex;
    }

    public List<SpecialBoneEffect> getSpecialBoneEffects() {
        return specialBoneEffects;
    }
}
