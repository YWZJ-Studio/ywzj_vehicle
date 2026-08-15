package org.ywzj.vehicle.client.resource.vehicle;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;

public class VehicleDisplayPojo extends BaseDisplayPojo {

    @SerializedName("cabin_model")
    public ResourceLocation cabinModel;

    @SerializedName("cabin_texture")
    public ResourceLocation cabinTexture;

}
