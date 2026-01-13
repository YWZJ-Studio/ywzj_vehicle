package org.ywzj.vehicle.recipe;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.Ingredient;

import java.lang.reflect.Type;

public record VehiclePrintingIngredient(Ingredient ingredient, int count) {

    public static class Deserializer implements JsonDeserializer<VehiclePrintingIngredient> {

        @Override
        public VehiclePrintingIngredient deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx) throws JsonParseException {
            if (!json.isJsonObject()) {
                throw new JsonParseException("VehiclePrintingIngredient must be a JSON object");
            }
            Ingredient ingredient = Ingredient.fromJson(json);
            int cnt = GsonHelper.getAsInt(json.getAsJsonObject(), "count", 1);
            return new VehiclePrintingIngredient(ingredient, cnt);
        }

    }

}
