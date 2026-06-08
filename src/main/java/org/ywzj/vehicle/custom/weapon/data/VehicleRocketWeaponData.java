package org.ywzj.vehicle.custom.weapon.data;

import com.google.gson.annotations.SerializedName;
import org.ywzj.vehicle.vehicle.pojo.Explosion;

public class VehicleRocketWeaponData extends BaseVehicleWeaponData {

    @SerializedName("mass")
    private float mass = 0.01f;

    @SerializedName("thrust")
    private float thrust = 0.01f;

    @SerializedName("motor_burn_time")
    private float motorBurnTime = 10f;

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
