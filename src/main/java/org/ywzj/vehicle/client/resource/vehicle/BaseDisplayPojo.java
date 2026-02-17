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

    @SerializedName("script")
    public ResourceLocation script;

    @SerializedName("animation_controller")
    public ResourceLocation animationController;

    @SerializedName("special_bone_effects")
    public List<SpecialBoneEffect> specialBoneEffects;

    @SerializedName("sounds")
    public Map<String, ResourceLocation> sounds;

    @SerializedName("description")
    public String description;

}
