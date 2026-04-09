package org.ywzj.vehicle.vehicle.pojo;

import com.google.gson.annotations.SerializedName;
import net.minecraft.world.phys.Vec3;

public class PhysicsInfo {

    @SerializedName("mass")
    public float mass = 1;

    @SerializedName("center")
    public Vec3 center = Vec3.ZERO;

    @SerializedName("can_destroy_block")
    public boolean canDestroyBlock = false;

    @SerializedName("radar_cross_section")
    public float radarCrossSection = 1f;

}
