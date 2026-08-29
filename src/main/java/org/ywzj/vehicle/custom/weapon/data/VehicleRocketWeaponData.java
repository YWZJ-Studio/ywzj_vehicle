package org.ywzj.vehicle.custom.weapon.data;

import com.google.gson.annotations.SerializedName;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.vehicle.pojo.Explosion;

public class VehicleRocketWeaponData extends BaseVehicleWeaponData {

    @SerializedName("mass")
    private float mass = 0.01f;

    @SerializedName("thrust")
    private float thrust = 0.01f;

    @SerializedName("motor_burn_time")
    private float motorBurnTime = 10f;

    @SerializedName("engine_nozzle_offset")
    private Vec3 engineNozzleOffset = new Vec3(0, 0, -0.37);

    @SerializedName("drag_coefficient")
    private float dragCoefficient = 0.005f;

    @SerializedName("explosion")
    private Explosion explosion = new Explosion();

    public float getMass() {
        return mass;
    }

    public float getThrust() {
        return thrust;
    }

    public float getMotorBurnTime() {
        return motorBurnTime;
    }

    public Vec3 getEngineNozzleOffset() {
        return engineNozzleOffset;
    }

    public float getDragCoefficient() {
        return dragCoefficient;
    }

    public Explosion getExplosion() {
        return explosion;
    }

    public void setExplosion(Explosion explosion) {
        this.explosion = explosion;
    }

}
