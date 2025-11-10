package org.ywzj.vehicle.client.resource.vehicle;

import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 载具客户端效果配置类型，用于从资源包反序列化载具效果配置<br/>
 * display同时将作为完成初始化后的模型、动画、客户端侧音效等客户端资源缓存的载体
 * @param <D> 配置类型
 */
public record VehicleDisplayType<D extends BaseVehicleDisplay> (
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
    public interface DataSerializer<D extends BaseVehicleDisplay> {
        @Nullable
        D parse(JsonElement json);
    }

    public static class Builder<D extends BaseVehicleDisplay> {
        private final ResourceLocation id;
        private DataSerializer<D> dataSerializer;

        private Builder(ResourceLocation id) {
            this.id = id;
        }

        public static <D extends BaseVehicleDisplay> Builder<D> of(ResourceLocation id) {
            return new Builder<>(id);
        }

        public Builder<D> setDataSerializer(DataSerializer<D> dataSerializer) {
            this.dataSerializer = dataSerializer;
            return this;
        }

        public VehicleDisplayType<D> build() {
            return new VehicleDisplayType<>(id, dataSerializer);
        }
    }
}
