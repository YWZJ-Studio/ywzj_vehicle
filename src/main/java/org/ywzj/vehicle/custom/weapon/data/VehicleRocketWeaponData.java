package org.ywzj.vehicle.custom.weapon.data;

import com.google.gson.annotations.SerializedName;
import org.ywzj.vehicle.vehicle.pojo.Explosion;

public class VehicleRocketWeaponData extends BaseVehicleWeaponData {

    @SerializedName("explosion")
    private Explosion explosion = new Explosion();

    public Explosion getExplosion() {
        return explosion;
    }

    public void setExplosion(Explosion explosion) {
        this.explosion = explosion;
    }

}
