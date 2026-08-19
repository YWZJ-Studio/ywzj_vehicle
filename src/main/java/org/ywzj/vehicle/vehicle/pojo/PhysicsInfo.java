package org.ywzj.vehicle.vehicle.pojo;

import com.google.gson.annotations.SerializedName;
import net.minecraft.world.phys.Vec3;

public class PhysicsInfo {

    @SerializedName("mass")
    public float mass = 1;

    @SerializedName("friction")
    public float friction = 0.005f;

    @SerializedName("center")
    public Vec3 center = Vec3.ZERO;

    @SerializedName("can_destroy_block")
    public boolean canDestroyBlock = false;

    @SerializedName("can_tumble")
    public boolean canTumble = true;

    @SerializedName("radar_cross_section")
    public float radarCrossSection = 1f;

    @SerializedName("destroy_explosion_velocity")
    public float destroyExplosionVelocity = 0.8f;

    public PhysicsInfo copy() {
        PhysicsInfo copy = new PhysicsInfo();
        copy.mass = this.mass;
        copy.friction = this.friction;
        copy.center = new Vec3(this.center.x, this.center.y, this.center.z);
        copy.canDestroyBlock = this.canDestroyBlock;
        copy.radarCrossSection = this.radarCrossSection;
        copy.destroyExplosionVelocity = this.destroyExplosionVelocity;
        return copy;
    }

}
