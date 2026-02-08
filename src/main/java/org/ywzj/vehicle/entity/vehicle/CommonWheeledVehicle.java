package org.ywzj.vehicle.entity.vehicle;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PlayMessages;
import org.jetbrains.annotations.Nullable;
import org.ywzj.vehicle.api.entity.ICustomVehicle;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;
import org.ywzj.vehicle.vehicle.pojo.AimContext;

import java.util.List;


/**
 * 数据包载具模板，轮式车辆
 */
public class CommonWheeledVehicle extends WheeledVehicle  {

    public static final EntityType<CommonWheeledVehicle> TYPE = EntityType.Builder
            .<CommonWheeledVehicle>of(CommonWheeledVehicle::new, MobCategory.MISC)
            .sized(1f, 1f)
            .updateInterval(1)
            .clientTrackingRange(16)
            .setCustomClientFactory(CommonWheeledVehicle::new)
            .build("common_wheeled_vehicle");

    private ResourceLocation vehicleId = ICustomVehicle.EMPTY_ID;

    public CommonWheeledVehicle(EntityType<? extends AbstractVehicle> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public CommonWheeledVehicle(PlayMessages.SpawnEntity spawnEntity, Level level) {
        this(TYPE, level);
    }

//    @Override
//    public void initData(ResourceLocation vehicleId) {
//        this.setMaxHealth(100);
//        this.setHealth(this.getMaxHealth());
//        this.vehicleId = vehicleId;
//        CommonAssetsManager.vehicleDataManager().getVehicleData(vehicleId).ifPresent(data -> {
//            var struct = data.getVehicleStructObbs();
//            this.mainCubeOBB = struct.mainCubeOBB();
//            this.vehicleOBBs = struct.obbs();
//            var weapons = data.createPartUnits(this);
//            this.partUnits.addAll(weapons.partUnitMap().values());
//            this.seats.addAll(weapons.seats());
//        });
//        Map<String, PartUnit<?>> map = new HashMap<>();
//        for (PartUnit<?> partUnit : partUnits) {
//            map.put(partUnit.getId(), partUnit);
//        }
//        this.partUnitMap = map;
//        if (this.level().isClientSide()) {
//            this.animationInstance = new VehicleAnimationInstance(this);
//        }
//    }

    @Override
    protected void tickParticle() {

    }

    @Override
    public void shoot(int partUnitIndex, int weaponIndex, List<AimContext> aimContexts, @Nullable LivingEntity operator) {
        if (partUnits.get(partUnitIndex) instanceof WeaponUnit weaponUnit) {
            weaponUnit.shoot(weaponIndex, aimContexts, operator);
        }
    }

}
