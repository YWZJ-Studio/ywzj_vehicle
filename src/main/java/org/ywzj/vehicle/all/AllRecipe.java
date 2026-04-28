package org.ywzj.vehicle.all;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.recipe.VehiclePrintingSerializer;

public class AllRecipe {

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, YwzjVehicle.MOD_ID);
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, YwzjVehicle.MOD_ID);
    public static final DeferredHolder<RecipeSerializer<?>, VehiclePrintingSerializer> VEHICLE_PRINTING_SERIALIZER =
            RECIPE_SERIALIZERS.register("vehicle_printing", VehiclePrintingSerializer::new);

    public static void register(IEventBus eventBus) {
        RECIPE_SERIALIZERS.register(eventBus);
        RECIPE_TYPES.register(eventBus);
    }

}
