package org.ywzj.vehicle.client.resource.vehicle;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;
import org.ywzj.vehicle.YwzjVehicle;

public class FixedWingVehicleDisplayPojo extends BaseDisplayPojo {

    @SerializedName("afterburner_model")
    public ResourceLocation afterburnerModel = YwzjVehicle.resourceLocation("ywzj_vehicle:effect/afterburner_flame");

    @SerializedName("afterburner_texture")
    public ResourceLocation afterburnerTexture = YwzjVehicle.resourceLocation("ywzj_vehicle:textures/entity/afterburner_flame.png");

    @SerializedName("afterburner_animations")
    public ResourceLocation afterburnerAnimations = YwzjVehicle.resourceLocation("ywzj_vehicle:effect/afterburner_flame.animation");

}
