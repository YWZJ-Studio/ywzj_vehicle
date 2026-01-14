package org.ywzj.vehicle.api.entity;

import net.minecraft.resources.ResourceLocation;
import org.ywzj.vehicle.YwzjVehicle;

public interface ICustomVehicle {

    String TAG_VEHICLE_ID = "VehicleId";

    String TAG_VEHICLE_DISPLAY_ID = "VehicleDisplayId";

    ResourceLocation EMPTY_ID = YwzjVehicle.modLocation("empty");

    ResourceLocation getVehicleId();

    void setVehicleId(ResourceLocation vehicleId);

    ResourceLocation getDisplayId();

    void setDisplayId(ResourceLocation displayId);

}
