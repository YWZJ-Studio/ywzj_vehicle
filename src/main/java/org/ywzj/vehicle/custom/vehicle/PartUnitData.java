package org.ywzj.vehicle.custom.vehicle;

import com.google.gson.annotations.SerializedName;

public class PartUnitData {
    public enum PartType {
        @SerializedName("weapon")
        WEAPON,
        @SerializedName("spotter")
        SPOTTER
    }

    @SerializedName("type")
    private PartType partType = PartType.WEAPON;

    @SerializedName("name")
    private String name = "";

    @SerializedName("structure_bone")
    private String structureBone = null;

    @SerializedName("parent")
    private String parent = null;

    public PartType getPartType() {
        return partType;
    }
}
