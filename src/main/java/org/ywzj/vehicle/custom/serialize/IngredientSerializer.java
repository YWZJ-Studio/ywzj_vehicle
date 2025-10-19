package org.ywzj.vehicle.custom.serialize;

import com.google.gson.*;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.crafting.CraftingHelper;

import java.lang.reflect.Type;

public class IngredientSerializer implements JsonDeserializer<Ingredient> {
    public Ingredient deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx) throws JsonParseException {
        if (GsonHelper.isStringValue(json.getAsJsonPrimitive())) {
            String str = json.getAsString();

            JsonObject object = new JsonObject();
            object.addProperty("type", "minecraft:item");
            object.addProperty("item", str);

            return CraftingHelper.getIngredient(object, false);
        }
        return CraftingHelper.getIngredient(json, false);
    }
}