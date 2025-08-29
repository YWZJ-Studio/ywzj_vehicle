package org.ywzj.vehicle.all;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.custom.WeaponUnitData;
import org.ywzj.vehicle.custom.WeaponUnitType;
import org.ywzj.vehicle.custom.WeaponUnitTypeManager;
import org.ywzj.vehicle.vehicle.weapon.AbstractWeaponUnit;
import org.ywzj.vehicle.vehicle.weapon.CannonUnit;

public class AllWeaponUnitType {
    public static final DeferredRegister<WeaponUnitType<?, ?>> WEAPON_UNIT_TYPES = DeferredRegister.create(ModRegistries.WEAPON_UNIT_TYPE, YwzjVehicle.MOD_ID);

    public static final RegistryObject<WeaponUnitType<CannonUnit, WeaponUnitData>> CANNON = register(
            "cannon", json -> WeaponUnitTypeManager.GSON.fromJson(json, WeaponUnitData.class), CannonUnit::new
    );

    private static <T extends AbstractWeaponUnit<D>, D> RegistryObject<WeaponUnitType<T, D>> register(
            String name,
            WeaponUnitType.DataSerializer<D> dataSerializer,
            WeaponUnitType.WeaponUnitFactory<T, D> factory
    ) {
        return WEAPON_UNIT_TYPES.register(name,
                () -> WeaponUnitType.Builder.<T, D>of(YwzjVehicle.modLoc(name))
                        .setDataSerializer(dataSerializer)
                        .setFactory(factory)
                        .build()
        );
    }

    public static void register(IEventBus eventBus) {
        WEAPON_UNIT_TYPES.register(eventBus);
    }
}
