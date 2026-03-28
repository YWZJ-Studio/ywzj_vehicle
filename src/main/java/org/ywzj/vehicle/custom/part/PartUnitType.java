package org.ywzj.vehicle.custom.part;

import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import org.ywzj.vehicle.custom.part.data.PartUnitData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.part.PartUnit;

/**
 * 代表一个载具部件类型
 * @param id 类型id
 * @param dataSerializer 配置解析器
 * @param factory 部件工厂
 * @param <T> 载具部件实现类
 * @param <D> 载具部件配置
 */
public record PartUnitType<T extends PartUnit<D>, D extends PartUnitData>(
        ResourceLocation id,
        PartUnitType.DataSerializer<D> dataSerializer,
        PartUnitFactory<T, D> factory
) {

    public PartUnitEntry<T, D> parseAndCreate(JsonElement jsonElement) {
        D data = dataSerializer.parse(jsonElement);
        if (data == null) {
            return null;
        }
        return new PartUnitEntry<>(this, data);
    }

    @FunctionalInterface
    public interface DataSerializer<D> {
        D parse(JsonElement json);
    }

    @FunctionalInterface
    public interface PartUnitFactory<T extends PartUnit<D>, D extends PartUnitData> {
        T create(int index, AbstractVehicle vehicle, D data);
    }

    public static class Builder<T extends PartUnit<D>, D extends PartUnitData> {

        private final ResourceLocation id;
        private DataSerializer<D> dataSerializer;
        private PartUnitFactory<T, D> factory;

        private Builder(ResourceLocation id) {
            this.id = id;
        }

        public static <T extends PartUnit<D>, D extends PartUnitData> PartUnitType.Builder<T, D> of(ResourceLocation id) {
            return new PartUnitType.Builder<>(id);
        }

        public PartUnitType.Builder<T, D> setDataSerializer(PartUnitType.DataSerializer<D> dataSerializer) {
            this.dataSerializer = dataSerializer;
            return this;
        }

        public PartUnitType.Builder<T, D> setFactory(PartUnitType.PartUnitFactory<T, D> factory) {
            this.factory = factory;
            return this;
        }

        public PartUnitType<T, D> build() {
            return new PartUnitType<>(id, dataSerializer, factory);
        }

    }

}
