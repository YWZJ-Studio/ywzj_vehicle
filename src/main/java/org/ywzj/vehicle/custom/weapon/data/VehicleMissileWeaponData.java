package org.ywzj.vehicle.custom.weapon.data;

import com.google.gson.annotations.SerializedName;
import org.ywzj.vehicle.custom.pojo.Explosion;

public class VehicleMissileWeaponData extends BaseVehicleWeaponData {

    @SerializedName("x_rot_max")
    private float xRotMax = 10f;

    @SerializedName("x_rot_min")
    private float xRotMin = -10f;

    @SerializedName("y_rot_max")
    private float yRotMax = 10f;

    @SerializedName("y_rot_min")
    private float yRotMin = -10f;

    @SerializedName("max_speed")
    private float maxSpeed = 5f;

    @SerializedName("explosion")
    private Explosion explosion = new Explosion();

    public float getXRotMax() {
        return xRotMax;
    }

    public void setXRotMax(float xRotMax) {
        this.xRotMax = xRotMax;
    }

    public float getXRotMin() {
        return xRotMin;
    }

    public void setXRotMin(float xRotMin) {
        this.xRotMin = xRotMin;
    }

    public float getYRotMax() {
        return yRotMax;
    }

    public void setYRotMax(float yRotMax) {
        this.yRotMax = yRotMax;
    }

    public float getYRotMin() {
        return yRotMin;
    }

    public void setYRotMin(float yRotMin) {
        this.yRotMin = yRotMin;
    }

    public float getMaxSpeed() {
        return maxSpeed;
    }

    public Explosion getExplosion() {
        return explosion;
    }

    public void setExplosion(Explosion explosion) {
        this.explosion = explosion;
    }

}
