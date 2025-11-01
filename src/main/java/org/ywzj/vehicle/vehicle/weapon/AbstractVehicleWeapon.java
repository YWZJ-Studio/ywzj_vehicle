package org.ywzj.vehicle.vehicle.weapon;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.ywzj.vehicle.api.YwzjVehicleAPI;
import org.ywzj.vehicle.api.custom.sync.SyncDataSerializers;
import org.ywzj.vehicle.custom.sync.PartUnitSyncData;
import org.ywzj.vehicle.custom.sync.SyncDataHolder;
import org.ywzj.vehicle.custom.weapon.data.BaseVehicleWeaponData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.network.Channel;
import org.ywzj.vehicle.network.message.ClientVehicleAction;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;

import java.util.Collections;
import java.util.List;

/** 可配置的抽象武器模块<br/>
 * @param <T> 配置数据结构
 */
public abstract class AbstractVehicleWeapon<T extends BaseVehicleWeaponData> {

    private final AbstractVehicle vehicle;
    private final WeaponUnit weaponUnit;
    private final int index;
    private final T data;
    private Component name;
    protected long lastShootTime = 0;
    protected int remainAmmo = 0;
    protected int reloadTime = 0;

    protected SyncDataHolder<Integer> remainAmmoHolder;
    protected SyncDataHolder<Integer> reloadTimeHolder;

    /**
     *  你应该尽可能从工厂方法构建一个武器模块，而不是直接调用武器的构造方法<br/>
     *  此方法仅供子类实现使用<br/>
     *  参见{@link YwzjVehicleAPI#getVehicleWeaponManager()}
     */
    protected AbstractVehicleWeapon(AbstractVehicle vehicle, WeaponUnit weaponUnit, int index, T data) {
        this.vehicle = vehicle;
        this.weaponUnit = weaponUnit;
        this.index = index;
        this.data = data;
        this.name = Component.translatable(data.getName());
    }

    public void defineSyncData(PartUnitSyncData syncData) {
        this.remainAmmoHolder = syncData.define(SyncDataSerializers.INT, this::setRemainAmmo , this::getRemainAmmo, remainAmmo);
        this.reloadTimeHolder = syncData.define(SyncDataSerializers.INT, this::setReloadTime, this::getReloadTime, reloadTime);
    }

    public abstract void shoot(List<Vec3> ammoSpawnPositions, float ammoXRot, float ammoYRot, LivingEntity shooter);

    public boolean check(List<Vec3> ammoSpawnPositions, float ammoXRot, float ammoYRot, LivingEntity shooter) {
        return true;
    }

    public void onSwitchTo() {
        // 可选覆盖
    }

    public void onSwitchFrom() {
        this.reloadTime = 0;
    }

    protected void setRemainAmmo(int remainAmmo) {
        this.remainAmmo = remainAmmo;
    }

    protected void setReloadTime(int reloadTime) {
        this.reloadTime = reloadTime;
    }

    public boolean isCoolingDown() {
        return System.currentTimeMillis() - lastShootTime < this.getShootInterval();
    }

    public long getShootInterval() {
        return this.getData().getShootInterval();
    }

    @OnlyIn(Dist.CLIENT)
    public boolean doClientShoot() {
        if (isCoolingDown()) {
            return false;
        }
        if (!hasAmmo()) {
            return false;
        }
        lastShootTime = System.currentTimeMillis();
        Vec2 rot = weaponUnit.worldRot();
        int partUnitIndex = weaponUnit.getParentWeaponUnit() != null ? weaponUnit.getParentWeaponUnit().getIndex() : weaponUnit.getIndex();
        if (weaponUnit.getFiringMode() == WeaponUnit.FiringMode.RIPPLE) {
            sendShoot(this.getVehicle(), partUnitIndex, Collections.singletonList(weaponUnit.ammoSpawnPosition()), rot.x, rot.y);
            weaponUnit.countFire(1);
        } else if (weaponUnit.getFiringMode() == WeaponUnit.FiringMode.SALVO) {
            List<Vec3> ammoSpawnPositions = weaponUnit.ammoSpawnPositions();
            sendShoot(this.getVehicle(), partUnitIndex, ammoSpawnPositions, rot.x, rot.y);
            weaponUnit.countFire(ammoSpawnPositions.size());
        }
        return true;
    }

    public int getReloadTime() {
        return reloadTime;
    }

    public int getRemainAmmo() {
        return remainAmmo;
    }

    public int getMaxCapacity() {
        return this.getData().getMaxCapacity();
    }

    @OnlyIn(Dist.CLIENT)
    public static void sendShoot(AbstractVehicle abstractVehicle, int weaponIndex, List<Vec3> ammoSpawnPositions, float ammoXRot, float ammoYRot) {
        ClientVehicleAction action = new ClientVehicleAction();
        action.vehicleEntityId = abstractVehicle.getId();
        action.partUnitIndex = weaponIndex;
        action.shoot = true;
        action.ammoSpawnPositions = ammoSpawnPositions;
        action.ammoXRot = ammoXRot;
        action.ammoYRot = ammoYRot;
        Channel.CHANNEL.sendToServer(action);
    }

    public boolean hasAmmo() {
        return remainAmmo > 0;
    }

    public boolean consumeAmmo(int count) {
        if (remainAmmo >= count) {
            remainAmmo -= count;
            return true;
        }
        return false;
    }

    /**
     * 获取载具储存中的弹药数量，此方法在客户端无效
     * @return 弹药数量
     */
    public int getStorageAmmo() {
        if (vehicle.level().isClientSide()) {
            return 0;
        }
        return vehicle.getCapability(ForgeCapabilities.ITEM_HANDLER).map(cap -> {
            int total = 0;
            for (int i = 0; i < cap.getSlots(); i++) {
                ItemStack stack = cap.getStackInSlot(i);
                if (isAmmoForWeapon(stack)) {
                    total += stack.getCount();
                }
            }
            return total;
        }).orElse(0);
    }

    /**
     * 获取载具储存中是否有可供该武器换弹的弹药，此方法在客户端无效
     * @return 是否有弹药
     */
    public boolean hasStorageAmmo() {
        if (vehicle.level().isClientSide()) {
            return false;
        }
        return vehicle.getCapability(ForgeCapabilities.ITEM_HANDLER).map(cap -> {
            for (int i = 0; i < cap.getSlots(); i++) {
                ItemStack stack = cap.getStackInSlot(i);
                if (isAmmoForWeapon(stack)) {
                    return true;
                }
            }
            return false;
        }).orElse(false);
    }

    public boolean isAmmoForWeapon(ItemStack stack) {
        return this.getData().getReload().isAmmo(stack);
    }

    public boolean canReload() {
        return hasStorageAmmo() && !isReloading();
    }

    public void startReload() {
        this.reloadTime = 60;
    }

    public void tickReload() {
        if (reloadTime > 0) {
            reloadTime--;
            if (reloadTime == 0) {
                reload();
            }
        }
    }

    public void reload() {
        int maxCap = this.getData().getMaxCapacity();
        for (var item : vehicle.getItemStacks()) {
            int need = maxCap - remainAmmo;
            if (need <= 0) break;

            if (this.isAmmoForWeapon(item)) {
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

    public WeaponUnit getWeaponUnit() {
        return weaponUnit;
    }

    public void setName(Component name) {
        this.name = name;
    }

    public Component getName() {
        return name;
    }

    public boolean hasSyncData() {
        return true;
    }

}
