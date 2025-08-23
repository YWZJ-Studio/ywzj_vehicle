package org.ywzj.vehicle.custom;

import com.google.gson.annotations.SerializedName;

public class WeaponUnitData {
    @SerializedName("damage")
    private float damage;

    @SerializedName("headshot_multiplier")
    private float headshot;

    public float getDamage() {
        return damage;
    }

    public float getHeadshot() {
        return headshot;
    }
}
