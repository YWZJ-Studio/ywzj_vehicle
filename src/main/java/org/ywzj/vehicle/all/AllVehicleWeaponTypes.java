package org.ywzj.vehicle.all;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.custom.serialize.GsonUtil;
import org.ywzj.vehicle.custom.weapon.VehicleWeaponType;
import org.ywzj.vehicle.custom.weapon.data.BaseVehicleWeaponData;
import org.ywzj.vehicle.custom.weapon.data.VehicleCannonWeaponData;
import org.ywzj.vehicle.custom.weapon.data.VehicleGrenadeWeaponData;
import org.ywzj.vehicle.custom.weapon.data.VehicleMissileWeaponData;
import org.ywzj.vehicle.vehicle.weapon.AbstractVehicleWeapon;
import org.ywzj.vehicle.vehicle.weapon.VehicleCannon;
import org.ywzj.vehicle.vehicle.weapon.VehicleGrenade;
import org.ywzj.vehicle.vehicle.weapon.VehicleMissile;

public class AllVehicleWeaponTypes {
    public static final DeferredRegister<VehicleWeaponType<?, ?>> WEAPON_TYPES = DeferredRegister.create(ModRegistries.VEHICLE_WEAPON_TYPE, YwzjVehicle.MOD_ID);

    public static final RegistryObject<VehicleWeaponType<VehicleCannon, VehicleCannonWeaponData>> CANNON = register(
            "cannon", json -> GsonUtil.GSON.fromJson(json, VehicleCannonWeaponData.class), VehicleCannon::new
    );

    public static final RegistryObject<VehicleWeaponType<VehicleMissile, VehicleMissileWeaponData>> MISSILE = register(
            "missile", json -> GsonUtil.GSON.fromJson(json, VehicleMissileWeaponData.class), VehicleMissile::new
    );

    public static final RegistryObject<VehicleWeaponType<VehicleGrenade, VehicleGrenadeWeaponData>> GRENADE = register(
            "grenade", json -> GsonUtil.GSON.fromJson(json, VehicleGrenadeWeaponData.class), VehicleGrenade::new
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
