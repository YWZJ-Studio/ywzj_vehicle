package org.ywzj.vehicle.custom.weapon.data;

import com.google.gson.annotations.SerializedName;
import org.ywzj.vehicle.vehicle.pojo.Explosion;

public class VehicleAerialBombWeaponData extends BaseVehicleWeaponData {

    @SerializedName("fuse_delay_tick")
    private int fuseDelayTick = 60;

    @SerializedName("penetration_depth")
    private float penetrationDepth = 0;

    @SerializedName("homing")
    private boolean homing = false;

    @SerializedName("drag_coefficient")
    private float dragCoefficient = 0.005f;

    @SerializedName("max_g")
    private float maxG = 2.0f;

    @SerializedName("reference_speed")
    private float referenceSpeed = 2f;

    @SerializedName("explosion")
    private Explosion explosion = new Explosion();

    public int getFuseDelayTick() {
        return fuseDelayTick;
    }

    public void setFuseDelayTick(int fuseDelayTick) {
        this.fuseDelayTick = fuseDelayTick;
    }

    public float getPenetrationDepth() {
        return penetrationDepth;
    }

    public void setPenetrationDepth(float penetrationDepth) {
        this.penetrationDepth = penetrationDepth;
    }

    public boolean isHoming() {
        return homing;
    }

    public void setHoming(boolean homing) {
        this.homing = homing;
    }

    public float getDragCoefficient() {
        return dragCoefficient;
    }

    public void setDragCoefficient(float dragCoefficient) {
        this.dragCoefficient = dragCoefficient;
    }

    public float getMaxG() {
        return maxG;
    }

    public void setMaxG(float maxG) {
        this.maxG = maxG;
    }

    public float getReferenceSpeed() {
        return referenceSpeed;
    }

    public void setReferenceSpeed(float referenceSpeed) {
        this.referenceSpeed = referenceSpeed;
    }

    public Explosion getExplosion() {
        return explosion;
    }

    public void setExplosion(Explosion explosion) {
        this.explosion = explosion;
    }

}
