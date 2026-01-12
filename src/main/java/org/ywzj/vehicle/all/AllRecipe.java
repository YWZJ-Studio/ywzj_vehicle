package org.ywzj.vehicle.all;

import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.recipe.VehiclePrintingRecipe;
import org.ywzj.vehicle.recipe.VehiclePrintingSerializer;

public class AllRecipe {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, YwzjVehicle.MOD_ID);
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, YwzjVehicle.MOD_ID);

    public static RegistryObject<RecipeSerializer<?>> VEHICLE_PRINTING_SERIALIZER = RECIPE_SERIALIZERS.register("vehicle_printing", VehiclePrintingSerializer::new);
    public static RegistryObject<RecipeType<VehiclePrintingRecipe>> VEHICLE_PRINTING = RECIPE_TYPES.register("vehicle_printing", () -> new RecipeType<>() {
        @Override
        public String toString() {
            return YwzjVehicle.MOD_ID + ":vehicle_printing";
        }
    });

    public static void register(IEventBus eventBus) {
        RECIPE_SERIALIZERS.register(eventBus);
        RECIPE_TYPES.register(eventBus);
    }
}
