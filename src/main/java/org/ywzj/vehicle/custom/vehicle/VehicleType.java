package org.ywzj.vehicle.custom.vehicle;

import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.PartUnit;

import java.util.List;
import java.util.logging.Level;

/**
 * 载具类型，用于从数据包反序列化载具参数，并在载具实体创建时初始化载具
 * @param <E>
 * @param <D>
 */
public class VehicleType<E extends AbstractVehicle, D extends BaseVehicleData> {


    public void initVehicle(E entity, D data, Level level) {

    }

    protected List<PartUnit> buildPartUnits(E vehicle, D data) {
        // 由数据构建部件单元
        return List.of();
    }
}
