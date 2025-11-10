package org.ywzj.vehicle.client.resource.vehicle;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/**
 * 基础载具效果配置实例
 */
public class BaseVehicleDisplayPojo {
    @SerializedName("model")
    public ResourceLocation model;

    @SerializedName("texture")
    public ResourceLocation texture;

    @SerializedName("animations")
    public ResourceLocation animations;

    @SerializedName("sounds")
    public Map<String, ResourceLocation> sounds;

}
