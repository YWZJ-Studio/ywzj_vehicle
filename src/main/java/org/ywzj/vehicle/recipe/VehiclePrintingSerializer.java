package org.ywzj.vehicle.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.NotNull;

public class VehiclePrintingSerializer implements RecipeSerializer<VehiclePrintingRecipe> {

    private static final MapCodec<VehiclePrintingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    ItemStack.STRICT_CODEC.fieldOf("result").forGetter(VehiclePrintingRecipe::getResult),
                    VehiclePrintingIngredient.CODEC.listOf().fieldOf("materials").forGetter(VehiclePrintingRecipe::getInputs),
                    Codec.INT.fieldOf("printingTime").forGetter(VehiclePrintingRecipe::getPrintingTime)
            ).apply(instance, VehiclePrintingRecipe::new)
    );

    private static final StreamCodec<RegistryFriendlyByteBuf, VehiclePrintingRecipe> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC, VehiclePrintingRecipe::getResult,
            VehiclePrintingIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), VehiclePrintingRecipe::getInputs,
            ByteBufCodecs.VAR_INT, VehiclePrintingRecipe::getPrintingTime,
            VehiclePrintingRecipe::new
    );

    @Override
    public @NotNull MapCodec<VehiclePrintingRecipe> codec() {
        return CODEC;
    }

    @Override
    public @NotNull StreamCodec<RegistryFriendlyByteBuf, VehiclePrintingRecipe> streamCodec() {
        return STREAM_CODEC;
    }

}
