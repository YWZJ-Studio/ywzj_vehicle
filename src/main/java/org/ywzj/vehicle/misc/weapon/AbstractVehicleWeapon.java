package org.ywzj.vehicle.misc.weapon;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.ywzj.vehicle.all.AllItems;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.network.Channel;
import org.ywzj.vehicle.network.message.ClientVehicleAction;
import org.ywzj.vehicle.vehicle.WeaponUnit;

/** 可配置的抽象武器模块<br/>
 * @param <T> 配置数据结构
 */
public abstract class AbstractVehicleWeapon<T> {

    private final AbstractVehicle vehicle;
    private final WeaponUnit weaponUnit;
    private final int index;
    private final T data;
    private Component name = Component.empty();
    protected long lastShootTime = 0;
    protected int remainAmmo = 0;
    protected int reloadTime = 0;

    // 你应该从工厂方法构建一个武器模块，而不是直接调用构造方法
    protected AbstractVehicleWeapon(AbstractVehicle vehicle, WeaponUnit weaponUnit, int index, T data) {
        this.vehicle = vehicle;
        this.weaponUnit = weaponUnit;
        this.index = index;
        this.data = data;
    }

    public abstract void shoot(Vec3 origin, float ammoXRot, float ammoYRot, LivingEntity shooter);

    public void writeSyncData(FriendlyByteBuf buf) {
        buf.writeInt(remainAmmo);
        buf.writeInt(reloadTime);
    }

    public void readSyncData(FriendlyByteBuf buf) {
        this.remainAmmo = buf.readInt();
        this.reloadTime = buf.readInt();
    }

    public boolean isCoolingDown() {
        return System.currentTimeMillis() - lastShootTime < this.getShootInterval();
    }

    public long getShootInterval() {
        return 100;
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
        Vec3 ammoSpawnPosition = weaponUnit.ammoSpawnPosition();
        Vec2 rot = weaponUnit.worldRot();

        sendShoot(this.getVehicle(), index, ammoSpawnPosition, rot.x, rot.y);

        return true;
    }

    public int getReloadTime() {
        return reloadTime;
    }

    public int getRemainAmmo() {
        return remainAmmo;
    }

    @OnlyIn(Dist.CLIENT)
    private static void sendShoot(AbstractVehicle abstractVehicle, int weaponIndex, Vec3 ammoSpawnPosition, float ammoXRot, float ammoYRot) {
        ClientVehicleAction action = new ClientVehicleAction();
        action.vehicleEntityId = abstractVehicle.getId();
        action.weaponIndex = weaponIndex;
        action.shoot = true;
        action.ammoX = (float) ammoSpawnPosition.x;
        action.ammoY = (float) ammoSpawnPosition.y;
        action.ammoZ = (float) ammoSpawnPosition.z;
        action.ammoXRot = ammoXRot;
        action.ammoYRot = ammoYRot;
        Channel.CHANNEL.sendToServer(action);
    }

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
        return stack.is(AllItems.AMMO_AUTO_CANNON.get());
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
        int maxCap = 64;
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
