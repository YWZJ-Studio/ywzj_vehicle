package org.ywzj.vehicle.custom;

import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.misc.weapon.AbstractVehicleWeapon;

import javax.annotation.Nullable;

/**
 * 用于创建实际的武器单元
 * @param <T> 武器单元类型
 * @param <D> 配置数据
 */
public class VehicleWeaponType<T extends AbstractVehicleWeapon<D>, D> {
    private final ResourceLocation id;
    private final VehicleWeaponType.DataSerializer<D> dataSerializer;
    private final VehicleWeaponType.WeaponUnitFactory<T, D> factory;
    @Nullable
    private D data; // 此字段由数据包自动填入

    public VehicleWeaponType(ResourceLocation id, DataSerializer<D> dataSerializer, WeaponUnitFactory<T, D> factory) {
        this.id = id;
        this.dataSerializer = dataSerializer;
        this.factory = factory;
    }

    public void parseAndLoad(JsonElement json) {
        this.data = dataSerializer.parse(json);
    }

    public T create(AbstractVehicle vehicle, int index) {
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
    public interface WeaponUnitFactory<T extends AbstractVehicleWeapon<D>, D> {
        T create(AbstractVehicle vehicle, int index, D data);
    }

    public static class Builder<T extends AbstractVehicleWeapon<D>, D> {
        private final ResourceLocation id;
        private DataSerializer<D> dataSerializer;
        private WeaponUnitFactory<T, D> factory;

        private Builder(ResourceLocation id) {
            this.id = id;
        }

        public static <T extends AbstractVehicleWeapon<D>, D> Builder<T, D> of(ResourceLocation id) {
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

        public VehicleWeaponType<T, D> build() {
            return new VehicleWeaponType<>(id, dataSerializer, factory);
        }
    }
}
