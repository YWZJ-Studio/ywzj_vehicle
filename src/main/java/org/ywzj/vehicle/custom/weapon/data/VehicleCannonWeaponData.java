package org.ywzj.vehicle.custom.weapon.data;

import com.google.gson.annotations.SerializedName;
import org.ywzj.vehicle.vehicle.pojo.Explosion;

public class VehicleCannonWeaponData extends BaseVehicleWeaponData {

    @SerializedName("friction")
    private float friction = 0.01f;

    @SerializedName("tracer_r")
    private Float tracerR = 0.7f;

    @SerializedName("tracer_g")
    private Float tracerG = 0.5f;

    @SerializedName("tracer_b")
    private Float tracerB = 0.1f;

    @SerializedName("explosion")
    private Explosion explosion = new Explosion();

    public float getFriction() {
        return friction;
    }

    public Float getTracerR() {
        return tracerR;
    }

    public void setTracerR(Float tracerR) {
        this.tracerR = tracerR;
    }

    public Float getTracerG() {
        return tracerG;
    }

    public void setTracerG(Float tracerG) {
        this.tracerG = tracerG;
    }

    public Float getTracerB() {
        return tracerB;
    }

    public void setTracerB(Float tracerB) {
        this.tracerB = tracerB;
    }

    public Explosion getExplosion() {
        return explosion;
    }

    public void setExplosion(Explosion explosion) {
        this.explosion = explosion;
    }

}
