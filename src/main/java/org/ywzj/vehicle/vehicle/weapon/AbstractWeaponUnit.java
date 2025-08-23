package org.ywzj.vehicle.vehicle.weapon;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

/** 可配置的抽象武器单元类
 * @param <T> 配置数据结构
 */
public abstract class AbstractWeaponUnit<T> {
    private final AbstractVehicle vehicle;
    private final int index;
    private final T data;
    private Component name = Component.empty();
    private LivingEntity operator;

    protected AbstractWeaponUnit(AbstractVehicle vehicle, int index, T data) {
        this.vehicle = vehicle;
        this.index = index;
        this.data = data;
    }

    public abstract void shoot(Vec3 origin, float ammoXRot, float ammoYRot);

    public void tick() {
    }

    public T getData() {
        return data;
    }

    public int getIndex() {
        return index;
    }

    public AbstractVehicle getVehicle() {
        return vehicle;
    }

    public void setOperator(LivingEntity operator) {
        this.operator = operator;
    }

    public LivingEntity getOperator() {
        return operator;
    }

    public void setName(Component name) {
        this.name = name;
    }

    public Component getName() {
        return name;
    }
}
