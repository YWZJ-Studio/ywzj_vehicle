package org.ywzj.vehicle.custom.serialize;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.custom.part.PartUnitEntry;
import org.ywzj.vehicle.custom.pojo.WeaponInfo;

public class GsonUtil {
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(ResourceLocation.class, new ResourceLocation.Serializer())
            .registerTypeAdapter(Vec3.class, new Vec3Serializer())
            .registerTypeAdapter(Ingredient.class, new IngredientSerializer())
            .registerTypeAdapter(WeaponInfo.class, new WeaponInfoSerializer())
            .registerTypeAdapter(PartUnitEntry.class, new PartUnitEntrySerializer())
            .create();

}
