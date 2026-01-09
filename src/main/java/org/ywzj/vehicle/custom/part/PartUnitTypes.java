package org.ywzj.vehicle.custom.part;

import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.custom.part.data.*;
import org.ywzj.vehicle.custom.serialize.GsonUtil;
import org.ywzj.vehicle.vehicle.parts.*;

public class PartUnitTypes {

    public static final PartUnitType<PartUnit<PartUnitData>, PartUnitData> GENERIC =
            PartUnitType.Builder.of(YwzjVehicle.modLocation("generic"))
                    .setFactory(PartUnit::new)
                    .setDataSerializer((json) -> {
                        var pojo = GsonUtil.GSON.fromJson(json, PartUnitPojo.class);
                        return new PartUnitData(pojo);
                    })
                    .build();

    public static final PartUnitType<RotatableUnit<RotatableUnitData>, RotatableUnitData> ROTATABLE =
            PartUnitType.Builder.<RotatableUnit<RotatableUnitData>, RotatableUnitData>of(YwzjVehicle.modLocation("rotatable"))
                    .setFactory(RotatableUnit::new)
                    .setDataSerializer((json) -> {
                        var pojo = GsonUtil.GSON.fromJson(json, RotatableUnitPojo.class);
                        return new RotatableUnitData(pojo);
                    })
                    .build();

    public static final PartUnitType<WeaponUnit, WeaponUnitData> WEAPON =
            PartUnitType.Builder.<WeaponUnit, WeaponUnitData>of(YwzjVehicle.modLocation("weapon"))
                    .setFactory(WeaponUnit::new)
                    .setDataSerializer((json) -> {
                        var pojo = GsonUtil.GSON.fromJson(json, WeaponUnitPojo.class);
                        return new WeaponUnitData(pojo);
                    })
                    .build();

    public static final PartUnitType<AutoWeaponUnit, WeaponUnitData> AUTO_WEAPON =
            PartUnitType.Builder.<AutoWeaponUnit, WeaponUnitData>of(YwzjVehicle.modLocation("auto_weapon"))
                    .setFactory(AutoWeaponUnit::new)
                    .setDataSerializer((json) -> {
                        var pojo = GsonUtil.GSON.fromJson(json, WeaponUnitPojo.class);
                        return new WeaponUnitData(pojo);
                    })
                    .build();

    public static final PartUnitType<RadarUnit, RadarUnitData> RADAR =
            PartUnitType.Builder.<RadarUnit, RadarUnitData>of(YwzjVehicle.modLocation("radar"))
                    .setFactory(RadarUnit::new)
                    .setDataSerializer((json) -> {
                        var pojo = GsonUtil.GSON.fromJson(json, RadarUnitPojo.class);
                        return new RadarUnitData(pojo);
                    })
                    .build();

}
