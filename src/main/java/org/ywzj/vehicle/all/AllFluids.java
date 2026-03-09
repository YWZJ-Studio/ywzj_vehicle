package org.ywzj.vehicle.all;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.ywzj.vehicle.YwzjVehicle;

import java.util.function.Consumer;

public class AllFluids {

    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, YwzjVehicle.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(ForgeRegistries.FLUIDS, YwzjVehicle.MOD_ID);
    public static final DeferredRegister<net.minecraft.world.level.block.Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, YwzjVehicle.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, YwzjVehicle.MOD_ID);

    public static final RegistryObject<FluidType> FUEL_FLUID_TYPE = FLUID_TYPES.register("fuel", () ->
            new FluidType(FluidType.Properties.create()
                    .descriptionId("block." + YwzjVehicle.MOD_ID + ".fuel")
                    .fallDistanceModifier(0F)
                    .canExtinguish(false)
                    .canSwim(false)
                    .canPushEntity(true)
                    .density(900)
                    .viscosity(1200)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)) {
                        @Override
                        public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                            consumer.accept(new IClientFluidTypeExtensions() {

                                // 使用原版水的材质路径
                                private static final ResourceLocation
                                        STILL = YwzjVehicle.resourceLocation("block/water_still"),
                                        FLOW = YwzjVehicle.resourceLocation("block/water_flow");

                                @Override
                                public ResourceLocation getStillTexture() { return STILL; }

                                @Override
                                public ResourceLocation getFlowingTexture() { return FLOW; }

                                @Override
                                public int getTintColor() {
                                    return 0xFFE3BC2D;
                                }

                            });
                        }
                    });

    public static final RegistryObject<FlowingFluid> FUEL_SOURCE = FLUIDS.register("fuel", () ->
            new ForgeFlowingFluid.Source(AllFluids.FUEL_PROPERTIES));
    public static final RegistryObject<FlowingFluid> FUEL_FLOWING = FLUIDS.register("fuel_flowing", () ->
            new ForgeFlowingFluid.Flowing(AllFluids.FUEL_PROPERTIES));
    public static final RegistryObject<LiquidBlock> FUEL_BLOCK = BLOCKS.register("fuel_block", () ->
            new LiquidBlock(FUEL_SOURCE, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).noCollission().strength(100.0F).pushReaction(PushReaction.DESTROY).noLootTable()));
    public static final RegistryObject<Item> FUEL_BUCKET = ITEMS.register("fuel_bucket", () ->
            new BucketItem(FUEL_SOURCE, new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    private static final ForgeFlowingFluid.Properties FUEL_PROPERTIES = new ForgeFlowingFluid.Properties(
            FUEL_FLUID_TYPE, FUEL_SOURCE, FUEL_FLOWING)
            .slopeFindDistance(2)
            .levelDecreasePerBlock(1)
            .block(FUEL_BLOCK)
            .bucket(FUEL_BUCKET);

    public static void register(IEventBus eventBus) {
        FLUID_TYPES.register(eventBus);
        FLUIDS.register(eventBus);
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
    }

}
