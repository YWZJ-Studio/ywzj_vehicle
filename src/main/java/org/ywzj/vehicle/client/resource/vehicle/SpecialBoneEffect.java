package org.ywzj.vehicle.client.resource.vehicle;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;

/**
 * 特殊骨骼效果配置
 * 用于定义替代骨骼渲染的特效
 */
public class SpecialBoneEffect {

    /**
     * 骨骼名称
     */
    @SerializedName("bone")
    public String bone;

    /**
     * 特效纹理路径
     */
    @SerializedName("texture")
    public ResourceLocation texture;

    /**
     * 特效类型（如 muzzle_flash）
     */
    @SerializedName("type")
    public SpecialBoneEffectType type;

    /**
     * 验证配置是否有效
     * @return 配置是否有效
     */
    public boolean isValid() {
        return bone != null && !bone.isEmpty()
            && texture != null
            && type != null;
    }

    public enum SpecialBoneEffectType {
        @SerializedName("muzzle_flash")
        MUZZLE_FLASH,
        @SerializedName("transparent")
        TRANSPARENT,
        @SerializedName("cockpit")
        COCKPIT
    }

}
