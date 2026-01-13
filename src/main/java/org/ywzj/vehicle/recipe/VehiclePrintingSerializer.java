package org.ywzj.vehicle.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.common.crafting.CraftingHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ywzj.vehicle.custom.serialize.GsonUtil;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
public class VehiclePrintingSerializer implements RecipeSerializer<VehiclePrintingRecipe> {

    @NotNull
    @Override
    public VehiclePrintingRecipe fromJson(ResourceLocation id, JsonObject jsonObject) {
        JsonObject resultJson = GsonHelper.getAsJsonObject(jsonObject, "result");
        ItemStack result = CraftingHelper.getItemStack(resultJson, true);

        JsonArray inputsJson = GsonHelper.getAsJsonArray(jsonObject, "materials");
        List<VehiclePrintingIngredient> inputs = new ArrayList<>();
        for (int i = 0; i < inputsJson.size(); i++) {
            VehiclePrintingIngredient ingredient = GsonUtil.GSON.fromJson(inputsJson.get(i), VehiclePrintingIngredient.class);
            inputs.add(ingredient);
        }
        int printingTime = GsonHelper.getAsInt(jsonObject, "printingTime");

        return new VehiclePrintingRecipe(id, result, inputs, printingTime);
    }

    @Nullable
    @Override
    public VehiclePrintingRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
        ItemStack result = buffer.readItem();

        int inputCount = buffer.readInt();
        List<VehiclePrintingIngredient> inputs = new ArrayList<>();
        for (int i = 0; i < inputCount; i++) {
            Ingredient ingredient = Ingredient.fromNetwork(buffer);
            int count = buffer.readInt();
            inputs.add(new VehiclePrintingIngredient(ingredient, count));
        }
        int printingTime = buffer.readInt();

        return new VehiclePrintingRecipe(recipeId, result, inputs, printingTime);
    }

    @Override
    public void toNetwork(FriendlyByteBuf buffer, VehiclePrintingRecipe recipe) {
        buffer.writeItem(recipe.getResultItem(RegistryAccess.EMPTY));

        List<VehiclePrintingIngredient> inputs = recipe.getInputs();
        buffer.writeInt(inputs.size());
        for (VehiclePrintingIngredient input : inputs) {
            input.ingredient().toNetwork(buffer);
            buffer.writeInt(input.count());
        }
        buffer.writeInt(recipe.getPrintingTime());
    }

}
