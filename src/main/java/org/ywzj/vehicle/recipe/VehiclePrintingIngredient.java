package org.ywzj.vehicle.recipe;

import com.google.gson.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Ingredient;

import java.lang.reflect.Type;

public record VehiclePrintingIngredient(Ingredient ingredient, int count) {

    /**
     * 用于从 JSON/DataPack 读取数据
     */
    public static final Codec<VehiclePrintingIngredient> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Ingredient.CODEC.fieldOf("ingredient").forGetter(VehiclePrintingIngredient::ingredient),
            Codec.INT.optionalFieldOf("count", 1).forGetter(VehiclePrintingIngredient::count)
    ).apply(inst, VehiclePrintingIngredient::new));

    /**
     * 用于网络同步 (S2C/C2S)
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, VehiclePrintingIngredient> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, VehiclePrintingIngredient::ingredient,
            ByteBufCodecs.VAR_INT, VehiclePrintingIngredient::count,
            VehiclePrintingIngredient::new
    );

    public static class Deserializer implements JsonDeserializer<VehiclePrintingIngredient> {

        @Override
        public VehiclePrintingIngredient deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx) throws JsonParseException {
            if (!json.isJsonObject()) {
                throw new JsonParseException("VehiclePrintingIngredient must be a JSON object");
            }
            JsonObject obj = json.getAsJsonObject();
            JsonElement ingredientElement = obj.has("ingredient") ? obj.get("ingredient") : obj;
            Ingredient ingredient = Ingredient.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, ingredientElement).getOrThrow(JsonParseException::new);
            int cnt = net.minecraft.util.GsonHelper.getAsInt(obj, "count", 1);
            return new VehiclePrintingIngredient(ingredient, cnt);
        }

    }

}
