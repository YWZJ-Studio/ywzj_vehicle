package org.ywzj.vehicle.all;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.custom.VehicleWeaponManager;
import org.ywzj.vehicle.custom.weapon.BaseVehicleWeaponData;
import org.ywzj.vehicle.custom.weapon.VehicleWeaponType;
import org.ywzj.vehicle.misc.weapon.AbstractVehicleWeapon;
import org.ywzj.vehicle.misc.weapon.VehicleCannon;
import org.ywzj.vehicle.misc.weapon.VehicleMissile;

public class AllVehicleWeaponType {
    public static final DeferredRegister<VehicleWeaponType<?, ?>> WEAPON_TYPES = DeferredRegister.create(ModRegistries.WEAPON_UNIT_TYPE, YwzjVehicle.MOD_ID);

    public static final RegistryObject<VehicleWeaponType<VehicleCannon, BaseVehicleWeaponData>> CANNON = register(
            "cannon", json -> VehicleWeaponManager.GSON.fromJson(json, BaseVehicleWeaponData.class), VehicleCannon::new
    );

    public static final RegistryObject<VehicleWeaponType<VehicleMissile, BaseVehicleWeaponData>> MISSILE = register(
            "missile", json -> VehicleWeaponManager.GSON.fromJson(json, BaseVehicleWeaponData.class), VehicleMissile::new
    );

    private static <T extends AbstractVehicleWeapon<D>, D extends BaseVehicleWeaponData> RegistryObject<VehicleWeaponType<T, D>> register(
            String name,
            VehicleWeaponType.DataSerializer<D> dataSerializer,
            VehicleWeaponType.WeaponUnitFactory<T, D> factory
    ) {
        return WEAPON_TYPES.register(name,
                () -> VehicleWeaponType.Builder.<T, D>of(YwzjVehicle.modLoc(name))
                        .setDataSerializer(dataSerializer)
                        .setFactory(factory)
                        .build()
        );
    }

    public static void register(IEventBus eventBus) {
        WEAPON_TYPES.register(eventBus);
    }
}
