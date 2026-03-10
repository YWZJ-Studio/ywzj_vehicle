package org.ywzj.vehicle.vehicle.pojo;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public class WeaponInfo {

    /**
     * 武器id
     */
    @SerializedName("id")
    public ResourceLocation id;

    /**
     * 子武器站的载具部件ID
     */
    @Nullable
    @SerializedName("part_unit_id")
    public String partUnitId;

    @SerializedName("save_id")
    public String saveId;

    @SerializedName("secondary")
    public boolean secondary;

    public WeaponInfo(ResourceLocation id, @Nullable String partUnitId, boolean secondary, String saveId) {
        this.id = id;
        this.partUnitId = partUnitId;
        this.secondary = secondary;
        this.saveId = saveId;
    }

}
