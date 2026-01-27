package org.ywzj.vehicle.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.api.custom.IVehicleDataManager;
import org.ywzj.vehicle.api.custom.IVehicleWeaponManager;
import org.ywzj.vehicle.custom.CommonAssetsManager;
import org.ywzj.vehicle.custom.vehicle.BaseVehicleData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

import java.util.Optional;

public class YwzjVehicleAPI {

    public static void summon(Level level, Vec3 position, float yRot, ResourceLocation vehicleId, ResourceLocation displayId) {
        Optional<BaseVehicleData> vehicleDataOptional = CommonAssetsManager.vehicleDataManager().getVehicleData(vehicleId);
        if (vehicleDataOptional.isPresent()) {
            AbstractVehicle vehicle = vehicleDataOptional.get().construct(level, position, 0, yRot);
            try {
                vehicle.setDisplayId(displayId);
            } catch (IllegalArgumentException ignore) {}
            level.addFreshEntity(vehicle);
        }
    }

    /**
     * 获取载具武器管理器<br/>
     * 在单人模式或是作为服务端时，返回原始的载具武器管理器实例；<br/>
     * 作为客户端加入服务器时，获取从网络获得的缓存<br/>
     * @return 载具武器管理器实例
     */
    public static IVehicleWeaponManager getVehicleWeaponManager() {
        return CommonAssetsManager.vehicleWeaponManager();
    }

    public static IVehicleDataManager getVehicleDataManager() {
        return CommonAssetsManager.vehicleDataManager();
    }

}
