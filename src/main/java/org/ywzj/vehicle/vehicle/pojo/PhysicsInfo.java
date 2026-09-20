package org.ywzj.vehicle.vehicle.pojo;

import com.google.gson.annotations.SerializedName;
import net.minecraft.world.phys.Vec3;

public class PhysicsInfo {

    @SerializedName("mass")
    public float mass = 1000;

    @SerializedName("density")
    public float density = 8000;

    @SerializedName("friction")
    public float friction = 2000f;

    @SerializedName("liquid_damping")
    public float liquidDamping = 0.1f;

    @SerializedName("suspension_response")
    public float suspensionResponse = 2.0f;

    @SerializedName("center")
    public Vec3 center = Vec3.ZERO;

    @SerializedName("can_destroy_block")
    public boolean canDestroyBlock = false;

    @SerializedName("radar_cross_section")
    public float radarCrossSection = 1f;

    @SerializedName("destroy_explosion_velocity")
    public float destroyExplosionVelocity = 0.5f;

    public PhysicsInfo copy() {
        PhysicsInfo copy = new PhysicsInfo();
        copy.mass = this.mass;
        copy.density = this.density;
        copy.friction = this.friction;
        copy.liquidDamping = this.liquidDamping;
        copy.suspensionResponse = this.suspensionResponse;
        copy.center = new Vec3(this.center.x, this.center.y, this.center.z);
        copy.canDestroyBlock = this.canDestroyBlock;
        copy.radarCrossSection = this.radarCrossSection;
        copy.destroyExplosionVelocity = this.destroyExplosionVelocity;
        return copy;
    }

}
