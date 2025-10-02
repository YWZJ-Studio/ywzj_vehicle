package org.ywzj.vehicle.misc.weapon;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

/** 可配置的抽象武器模块<br/>
 * @param <T> 配置数据结构
 */
public abstract class AbstractVehicleWeapon<T> {

    private final AbstractVehicle vehicle;
    private final int index;
    private final T data;
    private Component name = Component.empty();
    protected long lastShootTime = 0;
    protected int remainAmmo = 0;
    protected int reloadTime = 0;

    // 你应该从工厂方法构建一个武器模块，而不是直接调用构造方法
    protected AbstractVehicleWeapon(AbstractVehicle vehicle, int index, T data) {
        this.vehicle = vehicle;
        this.index = index;
        this.data = data;
    }

    public boolean isCoolingDown() {
        return System.currentTimeMillis() - lastShootTime < this.getShootInterval();
    }

    public long getShootInterval() {
        return 100;
    }

    public abstract void shoot(Vec3 origin, float ammoXRot, float ammoYRot, LivingEntity shooter);

    public boolean hasAmmo() {
        return remainAmmo > 0;
    }

    public boolean consumeAmmo() {
        if (remainAmmo > 0) {
            remainAmmo--;
            return true;
        }
        return false;
    }

    public boolean canReload() {
        for (var item : vehicle.getItemStacks()) {
            if (item.is(Items.DIRT)) {
                return true;
            }
        }
        return false;
    }

    public void startReload() {
        this.reloadTime = 60;
    }

    public void tickReload() {
        var d = this.vehicle.getDriver();
        if (d != null) {
            d.sendSystemMessage(Component.literal("Reloading... " + reloadTime));
        }
        if (reloadTime > 0) {
            reloadTime--;
            if (reloadTime == 0) {
                reload();
            }
        }
    }

    public void reload() {
        int maxCap = 64;
        for (var item : vehicle.getItemStacks()) {
            int need = maxCap - remainAmmo;
            if (need <= 0) break;

            if (item.is(Items.DIRT)) {
                int toTake = Math.min(need, item.getCount());
                item.shrink(toTake);
                remainAmmo += toTake;
            }
        }
    }

    public boolean isReloading() {
        return reloadTime > 0;
    }

    public void tick() {
        if (vehicle.level().isClientSide()) return;
        if (remainAmmo == 0) {
            if (isReloading()) {
                tickReload();
            } else if (canReload()) {
                startReload();
            }
        }
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

    public void setName(Component name) {
        this.name = name;
    }

    public Component getName() {
        return name;
    }

}
