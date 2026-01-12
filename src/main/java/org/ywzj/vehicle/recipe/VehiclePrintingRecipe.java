package org.ywzj.vehicle.recipe;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.ywzj.vehicle.all.AllRecipe;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class VehiclePrintingRecipe implements Recipe<Inventory> {
    private final ResourceLocation id;
    private final ItemStack result;
    private final List<VehiclePrintingIngredient> inputs;

    public VehiclePrintingRecipe(ResourceLocation id, ItemStack result, List<VehiclePrintingIngredient> inputs) {
        this.id = id;
        this.result = result;
        this.inputs = inputs;
    }

    @Override
    @Deprecated
    public boolean matches(Inventory playerInventory, Level level) {
        return false;
    }

    @Override
    @Deprecated
    public ItemStack assemble(Inventory playerInventory, RegistryAccess registryAccess) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int pWidth, int pHeight) {
        return true;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return this.result.copy();
    }

    @Override
    public ResourceLocation getId() {
        return this.id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return AllRecipe.VEHICLE_PRINTING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return AllRecipe.VEHICLE_PRINTING.get();
    }

    public List<VehiclePrintingIngredient> getInputs() {
        return inputs;
    }
}
