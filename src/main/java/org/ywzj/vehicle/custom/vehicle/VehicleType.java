package org.ywzj.vehicle.custom.vehicle;

import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

import java.util.logging.Level;

/**
 * 载具类型，用于从数据包反序列化载具参数，并在载具实体创建时提供必要参数<br/>
 * 由于无法接管完整的实体创建流程，需要在合适的阶段主动进行数据初始化
 *
 * @param <E> 实体类型
 * @param <D> 数据类型
 */
public record VehicleType<E extends AbstractVehicle, D extends BaseVehicleData> (
        ResourceLocation id,
        DataSerializer<D> dataSerializer,
        VehicleFactory<E, D> factory
) {

    /**
     * 创建载具实体，
     * @param level 世界
     * @param data 载具数据
     * @return 载具实体
     */
    @Nullable
    public E create(Level level, D data) {
        return factory.create(level, data);
    }

    @Nullable
    public VehicleIndex<E, D> parseAndLoad(@NotNull ResourceLocation id, @NotNull JsonElement json) {
        D data = dataSerializer.parse(json);
        if (data == null) {
            return null;
        }
        return new VehicleIndex<>(id, this, data);
    }

    @NotNull
    public ResourceLocation getId() {
        return id;
    }

    @FunctionalInterface
    public interface DataSerializer<D extends BaseVehicleData> {
        @Nullable
        D parse(JsonElement json);
    }

    @FunctionalInterface
    public interface VehicleFactory<E extends AbstractVehicle, D extends BaseVehicleData> {
        @Nullable
        E create(Level level, D data);
    }
}
