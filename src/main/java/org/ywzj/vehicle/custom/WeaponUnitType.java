package org.ywzj.vehicle.custom;

import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.weapon.AbstractWeaponUnit;

import javax.annotation.Nullable;

/**
 * 用于创建实际的武器单元
 * @param <T> 武器单元类型
 * @param <D> 配置数据
 */
public class WeaponUnitType<T extends AbstractWeaponUnit<D>, D> {
    private final ResourceLocation id;
    private final WeaponUnitType.DataSerializer<D> dataSerializer;
    private final WeaponUnitType.WeaponUnitFactory<T, D> factory;
    @Nullable
    private D data; // 此字段由数据包自动填入

    public WeaponUnitType(ResourceLocation id, DataSerializer<D> dataSerializer, WeaponUnitFactory<T, D> factory) {
        this.id = id;
        this.dataSerializer = dataSerializer;
        this.factory = factory;
    }

    public void parseAndLoad(JsonElement json) {
        this.data = dataSerializer.parse(json);
    }

    public T create(AbstractVehicle vehicle, int index, D data) {
        return factory.create(vehicle, index, data);
    }

    @NotNull
    public ResourceLocation getId() {
        return id;
    }

    @Nullable
    public D getData() {
        return data;
    }

    @FunctionalInterface
    public interface DataSerializer<D> {
        @Nullable D parse(JsonElement json);
    }

    @FunctionalInterface
    public interface WeaponUnitFactory<T extends AbstractWeaponUnit<D>, D> {
        T create(AbstractVehicle vehicle, int index, D data);
    }

    public static class Builder<T extends AbstractWeaponUnit<D>, D> {
        private final ResourceLocation id;
        private DataSerializer<D> dataSerializer;
        private WeaponUnitFactory<T, D> factory;

        private Builder(ResourceLocation id) {
            this.id = id;
        }

        public static <T extends AbstractWeaponUnit<D>, D> Builder<T, D> of(ResourceLocation id) {
            return new Builder<>(id);
        }

        public Builder<T, D> setDataSerializer(DataSerializer<D> dataSerializer) {
            this.dataSerializer = dataSerializer;
            return this;
        }

        public Builder<T, D> setFactory(WeaponUnitFactory<T, D> factory) {
            this.factory = factory;
            return this;
        }

        public WeaponUnitType<T, D> build() {
            return new WeaponUnitType<>(id, dataSerializer, factory);
        }
    }
}
