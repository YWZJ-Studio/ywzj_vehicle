package org.ywzj.vehicle.vehicle.weapon;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.INBTSerializable;
import org.ywzj.vehicle.api.YwzjVehicleAPI;
import org.ywzj.vehicle.api.custom.sync.SyncDataSerializers;
import org.ywzj.vehicle.api.event.VehicleFireEvent;
import org.ywzj.vehicle.custom.part.data.WeaponUnitData;
import org.ywzj.vehicle.custom.sync.PartUnitSyncData;
import org.ywzj.vehicle.custom.sync.SyncDataHolder;
import org.ywzj.vehicle.custom.weapon.data.BaseVehicleWeaponData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.network.Channel;
import org.ywzj.vehicle.network.message.ClientVehicleAction;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;
import org.ywzj.vehicle.vehicle.pojo.AimContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** 可配置的抽象武器模块<br/>
 * @param <T> 配置数据结构
 */
public abstract class AbstractVehicleWeapon<T extends BaseVehicleWeaponData> implements INBTSerializable<CompoundTag> {

    private final AbstractVehicle vehicle;
    private final WeaponUnit weaponUnit;
    private final int index;
    private final T data;
    private final HashMap<String, SoundEvent> soundEvents = new HashMap<>();
    private final String serializeId;
    private Component displayName;
    protected long lastShootTime = 0;
    protected int remainAmmo = 0;
    protected int reloadTime = 0;
    protected SyncDataHolder<Integer> remainAmmoHolder;
    protected SyncDataHolder<Integer> reloadTimeHolder;
    protected final ThreadPoolExecutor soundsExecutor = new ThreadPoolExecutor(
            3,
            8,
            60,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100),
            Executors.defaultThreadFactory(),
            new ThreadPoolExecutor.AbortPolicy()
    );

    /**
     *  你应该尽可能从工厂方法构建一个武器模块，而不是直接调用武器的构造方法<br/>
     *  此方法仅供子类实现使用<br/>
     *  参见{@link YwzjVehicleAPI#getVehicleWeaponManager()}
     */
    protected AbstractVehicleWeapon(AbstractVehicle vehicle, WeaponUnit weaponUnit, int index, T data, String serializeId) {
        this.vehicle = vehicle;
        this.weaponUnit = weaponUnit;
        this.index = index;
        this.data = data;
        if (data.sounds != null) {
            data.sounds.forEach((soundName, soundResourceLocation) ->
                    this.soundEvents.put(soundName, SoundEvent.createVariableRangeEvent(soundResourceLocation)));
        }
        this.displayName = Component.translatable(data.getName());
        this.serializeId = serializeId;
    }

    public void defineSyncData(PartUnitSyncData syncData) {
        this.remainAmmoHolder = syncData.define(SyncDataSerializers.INT, this::setRemainAmmo, this::getRemainAmmo, remainAmmo);
        this.reloadTimeHolder = syncData.define(SyncDataSerializers.INT, this::setReloadTime, this::getReloadTime, reloadTime);
    }

    public abstract boolean shoot(List<AimContext> aimContexts, LivingEntity shooter);

    public boolean check(List<AimContext> aimContexts, LivingEntity shooter) {
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
        if (MinecraftForge.EVENT_BUS.post(new VehicleFireEvent.Pre(vehicle, this, Minecraft.getInstance().player))) {
            return false;
        }
        if (isCoolingDown()) {
            return false;
        }
        if (!hasAmmo()) {
            return false;
        }

        int partUnitIndex = weaponUnit.getParentWeaponUnit() != null ? weaponUnit.getParentWeaponUnit().getIndex() : weaponUnit.getIndex();
        List<AimContext> aimContexts;
        if (weaponUnit.getFiringMode() == WeaponUnitData.FiringMode.RIPPLE) {
            aimContexts = Collections.singletonList(weaponUnit.aimContext());
        } else if (weaponUnit.getFiringMode() == WeaponUnitData.FiringMode.SALVO) {
            aimContexts = weaponUnit.aimContexts();
        } else {
            return false;
        }

        lastShootTime = System.currentTimeMillis();
        sendShoot(this.getVehicle(), partUnitIndex, getIndex(), aimContexts);
        return true;
    }

    @OnlyIn(Dist.CLIENT)
    public void soundsAndParticles() {
        Level level = vehicle.level();
        if (getFireSound() != null) {
            level.playSound(LocalVehiclePlayer.instance.getPlayer(), vehicle, getFireSound(), SoundSource.PLAYERS, 4f, 1f);
            if (getShellSound() != null) {
                long interval;
                if (data.getMaxCapacity() == 1) {
                    interval = Math.min(2000, data.getReload().getTime() / 20 * 1000L / 2);
                } else {
                    interval = data.getShootInterval() / 20 * 1000 / 2;
                }
                long finalInterval = interval;
                soundsExecutor.submit(() -> {
                    try {
                        Thread.sleep(finalInterval);
                    } catch (Exception ignore) {}
                    Minecraft.getInstance().submit(() -> level.playSound(LocalVehiclePlayer.instance.getPlayer(), vehicle, getShellSound(), SoundSource.PLAYERS, 4f, 1f));
                });
            }
        }
        List<AimContext> aimContexts;
        if (weaponUnit.getFiringMode() == WeaponUnitData.FiringMode.RIPPLE) {
            aimContexts = Collections.singletonList(weaponUnit.aimContext());
            weaponUnit.countFire(1);
        } else if (weaponUnit.getFiringMode() == WeaponUnitData.FiringMode.SALVO) {
            aimContexts = weaponUnit.aimContexts();
            weaponUnit.countFire(aimContexts.size());
        } else {
            return;
        }
        float recoil = data.getRecoil();
        for (Vec3 muzzlePos : aimContexts.stream().map(aimContext -> aimContext.position).toList()) {
            for (int i = 0; i < 20 * recoil; i++) {
                double dx = (level.random.nextDouble() - 0.5) * 0.4 * recoil;
                double dy = (level.random.nextDouble() - 0.5) * 0.2 * recoil;
                double dz = (level.random.nextDouble() - 0.5) * 0.4 * recoil;
                level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true,
                        muzzlePos.x + dx, muzzlePos.y + dy, muzzlePos.z + dz,
                        0.01, 0.01, 0.01);
            }
            for (int i = 0; i < 10 * recoil + 1; i++) {
                double dx = (level.random.nextDouble() - 0.5) * 0.4 * recoil;
                double dy = (level.random.nextDouble() - 0.5) * 0.2 * recoil;
                double dz = (level.random.nextDouble() - 0.5) * 0.4 * recoil;
                level.addParticle(ParticleTypes.FLAME, true,
                        muzzlePos.x + dx, muzzlePos.y + dy, muzzlePos.z + dz,
                        0.01, 0.01, 0.01);
            }
            for (int i = 0; i < 15 * recoil + 1; i++) {
                double dx = (level.random.nextDouble() - 0.5) * 0.4 * recoil;
                double dy = (level.random.nextDouble() - 0.5) * 0.2 * recoil;
                double dz = (level.random.nextDouble() - 0.5) * 0.4 * recoil;
                level.addParticle(ParticleTypes.SMOKE, true,
                        muzzlePos.x + dx, muzzlePos.y + dy, muzzlePos.z + dz,
                        0.01, 0.01, 0.01);
            }
        }
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
    public static void sendShoot(AbstractVehicle abstractVehicle, int partUnitIndex, int weaponIndex, List<AimContext> aimContexts) {
        ClientVehicleAction action = new ClientVehicleAction();
        action.vehicleEntityId = abstractVehicle.getId();
        action.partUnitIndex = partUnitIndex;
        action.shoot = true;
        action.weaponIndex = weaponIndex;
        action.aimContexts = aimContexts;
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
        return hasStorageAmmo() && !isReloading() && weaponUnit.getOwner() != null;
    }

    public void startReload() {
        this.reloadTime = this.getData().getReload().getTime();
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
        if (getReloadSound() != null) {
            vehicle.level().playSound(null, vehicle, getReloadSound(), SoundSource.PLAYERS, 2f, 1f);
        }
    }

    public boolean isReloading() {
        return reloadTime > 0;
    }

    public void tick() {
        if (vehicle.level().isClientSide()) {
            return;
        }
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

    public void setDisplayName(Component displayName) {
        this.displayName = displayName;
    }

    public Component getDisplayName() {
        return displayName;
    }

    public SoundEvent getFireSound() {
        return this.soundEvents.get("fire");
    }

    public SoundEvent getShellSound() {
        return this.soundEvents.get("shell");
    }

    public SoundEvent getReloadSound() {
        return this.soundEvents.get("reload");
    }

    public boolean hasSyncData() {
        return true;
    }

    @Override
    public CompoundTag serializeNBT() {
        var tag = new CompoundTag();
        tag.putInt("RemainAmmo", remainAmmo);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.remainAmmo = nbt.getInt("RemainAmmo");
    }

    public String getSerializeId() {
        return serializeId;
    }

}
