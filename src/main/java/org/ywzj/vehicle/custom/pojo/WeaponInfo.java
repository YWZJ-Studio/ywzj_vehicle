package org.ywzj.vehicle.custom.pojo;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public class WeaponInfo {
    @SerializedName("id")
    public ResourceLocation id;

    /**
     * 依附的载具部件ID，如果为空则表示依附当前部件
     */
    @Nullable
    @SerializedName("part_unit")
    public String partUnit;

    public WeaponInfo(ResourceLocation id, @Nullable String partUnit) {
        this.id = id;
        this.partUnit = partUnit;
    }
}
