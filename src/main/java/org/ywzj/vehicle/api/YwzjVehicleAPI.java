package org.ywzj.vehicle.api;

import org.ywzj.vehicle.api.custom.IVehicleDataManager;
import org.ywzj.vehicle.api.custom.IVehicleWeaponManager;
import org.ywzj.vehicle.custom.VehicleDataManager;
import org.ywzj.vehicle.custom.VehicleWeaponManager;

public class YwzjVehicleAPI {
    /**
     * 获取载具武器管理器<br/>
     * 在单人模式或是作为服务端时，返回原始的载具武器管理器实例；<br/>
     * 作为客户端加入服务器时，获取从网络获得的缓存<br/>
     * @return 载具武器管理器实例
     */
    public static IVehicleWeaponManager getVehicleWeaponManager() {
        return VehicleWeaponManager.get();
    }

    public static IVehicleDataManager getVehicleDataManager() {
        return VehicleDataManager.get();
    }
}
