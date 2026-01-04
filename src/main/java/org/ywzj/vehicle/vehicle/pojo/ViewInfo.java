package org.ywzj.vehicle.vehicle.pojo;

import com.google.gson.annotations.SerializedName;
import net.minecraft.world.phys.Vec3;

public class ViewInfo {

    @SerializedName("third_person_center_offset")
    public Vec3 thirdPersonCenterOffset = new Vec3(0, 3, 0);

    @SerializedName("third_person_distance")
    public float thirdPersonDistance = 3f;

    @SerializedName("third_person_center_offset_zoomed")
    public Vec3 thirdPersonCenterOffsetZoomed = new Vec3(0, 5, 0);

    @SerializedName("third_person_distance_zoomed")
    public float thirdPersonDistanceZoomed = 0f;

    @SerializedName("sound_distance")
    public float soundDistance = 1f;

    @SerializedName("lock_passenger_y_body_rot")
    public boolean lockPassengerYBodyRot = false;

}
