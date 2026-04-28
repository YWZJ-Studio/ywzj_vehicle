package org.ywzj.vehicle.custom.serialize;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import net.minecraft.world.item.crafting.Ingredient;

import java.lang.reflect.Type;

public class IngredientSerializer implements JsonDeserializer<Ingredient> {

    @Override
    public Ingredient deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx) throws JsonParseException {
        if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("item", json.getAsString());
            return Ingredient.CODEC.parse(JsonOps.INSTANCE, obj).getOrThrow(JsonParseException::new);
        }
        return Ingredient.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(JsonParseException::new);
    }

}
