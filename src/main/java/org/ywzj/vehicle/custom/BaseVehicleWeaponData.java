package org.ywzj.vehicle.custom;

import com.google.gson.annotations.SerializedName;

public class BaseVehicleWeaponData {
    @SerializedName("damage")
    private float damage;

    @SerializedName("headshot_multiplier")
    private float headshot;

    @SerializedName("shoot_interval")
    private long shootInterval;

    public float getDamage() {
        return damage;
    }

    public float getHeadshotMultiplier() {
        return headshot;
    }

    public long getShootInterval() {
        return shootInterval;
    }
}
