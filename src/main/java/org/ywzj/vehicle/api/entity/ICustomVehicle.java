package org.ywzj.vehicle.api.entity;

import net.minecraft.resources.ResourceLocation;

public interface ICustomVehicle {
    String TAG_VEHICLE_ID = "CustomVehicleId";

    String TAG_DISPLAY_ID = "CustomVehicleDisplayId";

    ResourceLocation EMPTY_ID = new ResourceLocation("ywzj_vehicle", "empty");

    ResourceLocation getCustomId();

    void setCustomId(ResourceLocation customId);

    default ResourceLocation getCustomDisplayId() {
        return this.getCustomId();
    }

    default void setCustomDisplayId(ResourceLocation customDisplayId) {
        // do nothing
    }

}
