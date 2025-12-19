package org.ywzj.vehicle.custom.pojo;

import com.google.gson.annotations.SerializedName;

public class PhysicsInfo {

    @SerializedName("mass")
    public float mass = 1;

    @SerializedName("can_destroy_block")
    public boolean canDestroyBlock = false;

}
