package org.ywzj.vehicle.custom.weapon.data;

import com.google.gson.annotations.SerializedName;
import org.ywzj.vehicle.vehicle.pojo.Explosion;

public class VehicleAerialBombWeaponData extends BaseVehicleWeaponData {

    @SerializedName("fuse_delay_tick")
    private int fuseDelayTick = 60;

    @SerializedName("explosion")
    private Explosion explosion = new Explosion();

    public int getFuseDelayTick() {
        return fuseDelayTick;
    }

    public void setFuseDelayTick(int fuseDelayTick) {
        this.fuseDelayTick = fuseDelayTick;
    }

    public Explosion getExplosion() {
        return explosion;
    }

    public void setExplosion(Explosion explosion) {
        this.explosion = explosion;
    }

}
