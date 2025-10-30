package org.ywzj.vehicle.all;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.custom.VehicleWeaponManager;
import org.ywzj.vehicle.custom.weapon.VehicleWeaponType;
import org.ywzj.vehicle.custom.weapon.data.BaseVehicleWeaponData;
import org.ywzj.vehicle.custom.weapon.data.VehicleCannonWeaponData;
import org.ywzj.vehicle.custom.weapon.data.VehicleMissileWeaponData;
import org.ywzj.vehicle.vehicle.weapon.AbstractVehicleWeapon;
import org.ywzj.vehicle.vehicle.weapon.VehicleCannon;
import org.ywzj.vehicle.vehicle.weapon.VehicleMissile;

public class AllVehicleWeaponType {

    public static final DeferredRegister<VehicleWeaponType<?, ?>> WEAPON_TYPES = DeferredRegister.create(ModRegistries.WEAPON_UNIT_TYPE, YwzjVehicle.MOD_ID);

    public static final RegistryObject<VehicleWeaponType<VehicleCannon, VehicleCannonWeaponData>> CANNON = register(
            "cannon", json -> VehicleWeaponManager.GSON.fromJson(json, VehicleCannonWeaponData.class), VehicleCannon::new
    );

    public static final RegistryObject<VehicleWeaponType<VehicleMissile, VehicleMissileWeaponData>> MISSILE = register(
            "missile", json -> VehicleWeaponManager.GSON.fromJson(json, VehicleMissileWeaponData.class), VehicleMissile::new
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
