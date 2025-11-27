package org.ywzj.vehicle.custom.pojo;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public class WeaponInfo {

    @SerializedName("id")
    public ResourceLocation id;

    /**
     * 子武器站的载具部件ID
     */
    @Nullable
    @SerializedName("part_unit")
    public String partUnit;

    @SerializedName("save_id")
    public String saveId;

    public WeaponInfo(ResourceLocation id, String saveId, @Nullable String partUnit) {
        this.id = id;
        this.partUnit = partUnit;
        this.saveId = saveId;
    }

}
