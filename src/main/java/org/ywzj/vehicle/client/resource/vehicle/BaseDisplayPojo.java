package org.ywzj.vehicle.client.resource.vehicle;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

/**
 * 基础载具效果配置实例
 */
public class BaseDisplayPojo {

    @SerializedName("model")
    public ResourceLocation model;

    @SerializedName("texture")
    public ResourceLocation texture;

    @SerializedName("slot_texture")
    public ResourceLocation slotTexture;

    @SerializedName("animations")
    public ResourceLocation animations;

    @SerializedName("animation_controller")
    public ResourceLocation animationController;

    @SerializedName("special_bone_effects")
    public List<SpecialBoneEffect> specialBoneEffects;

    /** 通用载具渲染器的可选 v2 baked 模型配置；缺失或未启用时保留 v1 流程。 */
    @SerializedName("baked_model")
    public BakedModelConfig bakedModel;

    public static class BakedModelConfig {
        @SerializedName("enabled")
        public boolean enabled;

        /** 脚本、控制器或其他外部逻辑需要的具名骨骼。 */
        @SerializedName("preserve_bones")
        public List<String> preserveBones;

        /** 供无法静态枚举的脚本控制骨骼组使用的保留正则。 */
        @SerializedName("preserve_bone_regexes")
        public List<String> preserveBoneRegexes;
    }

    @SerializedName("sounds")
    public Map<String, ResourceLocation> sounds;

    @SerializedName("description")
    public String description;

    @SerializedName("tab_index")
    public int tabIndex;
}
