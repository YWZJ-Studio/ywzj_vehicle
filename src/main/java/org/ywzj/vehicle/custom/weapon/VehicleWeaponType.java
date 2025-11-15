package org.ywzj.vehicle.custom.weapon;

import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.ywzj.vehicle.custom.weapon.data.BaseVehicleWeaponData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;
import org.ywzj.vehicle.vehicle.weapon.AbstractVehicleWeapon;

import javax.annotation.Nullable;

/**
 * 载具武器类型类，用于定义某一类型载具武器的配置数据解析器和工厂
 * @param <T> 载具武器的实际实现类
 * @param <D> 配置数据类型
 * @param id 载具武器类型id
 * @param dataSerializer 配置数据解析器
 * @param factory 武器单元工厂
 */
public record VehicleWeaponType<T extends AbstractVehicleWeapon<D>, D extends BaseVehicleWeaponData>(
        ResourceLocation id,
        VehicleWeaponType.DataSerializer<D> dataSerializer,
        VehicleWeaponType.WeaponUnitFactory<T, D> factory
) {
    @Nullable
    public T create(AbstractVehicle vehicle, WeaponUnit unit, int index, D data, String serializeId) {
        return factory.create(vehicle, unit, index, data, serializeId);
    }

    @Nullable
    public VehicleWeaponIndex<T, D> parseAndLoad(@NotNull ResourceLocation id, @NotNull JsonElement json) {
        D data = dataSerializer.parse(json);
        if (data == null) {
            return null;
        }
        return new VehicleWeaponIndex<>(id, this, data);
    }

    @NotNull
    public ResourceLocation getId() {
        return id;
    }

    @FunctionalInterface
    public interface DataSerializer<D> {
        @Nullable D parse(JsonElement json);
    }

    @FunctionalInterface
    public interface WeaponUnitFactory<T extends AbstractVehicleWeapon<D>, D extends BaseVehicleWeaponData> {
        T create(AbstractVehicle vehicle, WeaponUnit unit, int index, D data, String serializeId);
    }

    public static class Builder<T extends AbstractVehicleWeapon<D>, D extends BaseVehicleWeaponData> {
        private final ResourceLocation id;
        private DataSerializer<D> dataSerializer;
        private WeaponUnitFactory<T, D> factory;

        private Builder(ResourceLocation id) {
            this.id = id;
        }

        public static <T extends AbstractVehicleWeapon<D>, D extends BaseVehicleWeaponData> Builder<T, D> of(ResourceLocation id) {
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
