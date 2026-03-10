package org.ywzj.vehicle.vehicle.pojo;

import com.google.gson.annotations.SerializedName;

public class Explosion {

    @SerializedName("explode")
    public boolean explode = false;

    @SerializedName("damage")
    public float damage;

    @SerializedName("radius")
    public float radius;

    @SerializedName("proximity_fuze")
    public boolean proximityFuze = false;

    @SerializedName("proximity_radius")
    public float proximityRadius;

    @SerializedName("destroy_block")
    public boolean destroyBlock = false;

}
