package org.ywzj.vehicle.custom.weapon.data;

import com.google.gson.annotations.SerializedName;

public class VehicleCannonWeaponData extends BaseVehicleWeaponData {

    @SerializedName("explosion")
    private boolean explosion = false;

    public boolean isExplosion() {
        return explosion;
    }

    public void setExplosion(boolean explosion) {
        this.explosion = explosion;
    }

}
