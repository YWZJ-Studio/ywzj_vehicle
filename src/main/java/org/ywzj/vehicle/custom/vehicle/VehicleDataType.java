package org.ywzj.vehicle.custom.vehicle;

import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 载具配置数据类型，用于从数据包反序列化载具参数，在载具实体创建时提供必要参数<br/>
 * 由于无法接管完整的实体创建流程，需要由实体实现自行在合适的阶段主动进行配置初始化<br/>
 * 如从nbt load实体，或是客户端收到创建数据包时
 *
 * @param <D> 数据类型
 */
public record VehicleDataType<D extends BaseVehicleData> (
        ResourceLocation id,
        DataSerializer<D> dataSerializer
) {
    @Nullable
    public D parse(@NotNull JsonElement json) {
        return dataSerializer.parse(json);
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

    public static class Builder<D extends BaseVehicleData> {
        private ResourceLocation id;
        private DataSerializer<D> dataSerializer;

        private Builder(ResourceLocation id) {
            this.id = id;
        }

        public static <D extends BaseVehicleData> Builder<D> of(ResourceLocation id) {
            return new Builder<>(id);
        }

        public Builder<D> setDataSerializer(DataSerializer<D> dataSerializer) {
            this.dataSerializer = dataSerializer;
            return this;
        }

        public VehicleDataType<D> build() {
            return new VehicleDataType<>(id, dataSerializer);
        }
    }
}
