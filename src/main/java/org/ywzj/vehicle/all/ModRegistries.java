package org.ywzj.vehicle.all;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.NewRegistryEvent;
import net.minecraftforge.registries.RegistryBuilder;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.custom.weapon.VehicleWeaponType;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = YwzjVehicle.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModRegistries {
    public static final ResourceKey<Registry<VehicleWeaponType<?, ?>>> WEAPON_UNIT_TYPE = ResourceKey.createRegistryKey(
            new ResourceLocation(YwzjVehicle.MOD_ID, "weapon_unit_type")
    );
    public static Supplier<IForgeRegistry<VehicleWeaponType<?, ?>>> WEAPON_UNIT_TYPE_SUPPLIER;

    @SubscribeEvent
    public static void registerRegistries(NewRegistryEvent event) {
        WEAPON_UNIT_TYPE_SUPPLIER = event.create(new RegistryBuilder<VehicleWeaponType<?, ?>>().setName(WEAPON_UNIT_TYPE.location()));
    }
}
