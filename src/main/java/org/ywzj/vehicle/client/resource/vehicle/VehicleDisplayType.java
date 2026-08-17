package org.ywzj.vehicle.client.resource.vehicle;

import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ywzj.vehicle.client.render.animation.context.AnimationContextFactory;
import org.ywzj.vehicle.client.render.animation.context.VehicleContext;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

/**
 * 载具客户端效果配置类型，用于从资源包反序列化载具效果配置<br/>
 * display同时将作为完成初始化后的模型、动画、客户端侧音效等客户端资源缓存的载体
 * @param <D> 配置类型
 */
public record VehicleDisplayType<D extends BaseDisplay> (
        ResourceLocation id,
        DataSerializer<D> dataSerializer,
        AnimationContextFactory<?, ?> contextFactory
) {
    @Nullable
    public D parse(@NotNull JsonElement json) {
        D display = dataSerializer.parse(json);
        if (display instanceof VehicleDisplay<?, ?> vehicleDisplay && contextFactory != null) {
            setContextFactory(vehicleDisplay, contextFactory);
        }
        return display;
    }

    @SuppressWarnings("unchecked")
    private static <E extends AbstractVehicle, CTX extends VehicleContext<E>> void setContextFactory(
            VehicleDisplay<E, CTX> display,
            AnimationContextFactory<?, ?> factory
    ) {
        AnimationContextFactory<E, CTX> typedFactory = (AnimationContextFactory<E, CTX>) factory;
        display.setContextFactory(typedFactory);
        if (display.cabinDisplay != null) {
            display.cabinDisplay.setContextFactory(typedFactory);
        }
    }

    @NotNull
    public ResourceLocation getId() {
        return id;
    }

    @FunctionalInterface
    public interface DataSerializer<D extends BaseDisplay> {
        @Nullable
        D parse(JsonElement json);
    }

    public static class Builder<D extends BaseDisplay> {
        private final ResourceLocation id;
        private DataSerializer<D> dataSerializer;
        private AnimationContextFactory<?, ?> contextFactory;

        private Builder(ResourceLocation id) {
            this.id = id;
        }

        public static <D extends BaseDisplay> Builder<D> of(ResourceLocation id) {
            return new Builder<>(id);
        }

        public Builder<D> setDataSerializer(DataSerializer<D> dataSerializer) {
            this.dataSerializer = dataSerializer;
            return this;
        }

        public Builder<D> setContextFactory(AnimationContextFactory<?, ?> contextFactory) {
            this.contextFactory = contextFactory;
            return this;
        }

        public VehicleDisplayType<D> build() {
            return new VehicleDisplayType<>(id, dataSerializer, contextFactory);
        }
    }
}
