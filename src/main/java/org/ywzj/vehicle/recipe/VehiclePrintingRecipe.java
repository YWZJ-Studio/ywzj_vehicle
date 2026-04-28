package org.ywzj.vehicle.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.ywzj.vehicle.all.AllRecipe;

import java.util.List;

public class VehiclePrintingRecipe implements CraftingRecipe {

    private final ItemStack result;
    private final List<VehiclePrintingIngredient> inputs;
    private final int printingTime;

    public VehiclePrintingRecipe(ItemStack result, List<VehiclePrintingIngredient> inputs, int printingTime) {
        this.result = result;
        this.inputs = inputs;
        this.printingTime = printingTime;
    }

    @Override
    public boolean matches(CraftingInput craftingInput, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(CraftingInput craftingInput, HolderLookup.Provider provider) {
        return null;
    }

    @Override
    public boolean canCraftInDimensions(int pWidth, int pHeight) {
        return pWidth * pHeight >= 2;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider provider) {
        return null;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return AllRecipe.VEHICLE_PRINTING_SERIALIZER.get();
    }

    public ItemStack getResult() {
        return result;
    }

    public List<VehiclePrintingIngredient> getInputs() {
        return inputs;
    }

    public int getPrintingTime() {
        return printingTime;
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }

}
