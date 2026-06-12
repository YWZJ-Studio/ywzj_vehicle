package org.ywzj.vehicle.vehicle.weapon;

import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.network.PacketDistributor;
import org.ywzj.vehicle.all.AllItems;
import org.ywzj.vehicle.api.YwzjVehicleAPI;
import org.ywzj.vehicle.api.custom.sync.SyncDataSerializers;
import org.ywzj.vehicle.api.event.VehicleFireEvent;
import org.ywzj.vehicle.audio.VehicleSound;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.client.resource.vehicle.BaseDisplay;
import org.ywzj.vehicle.custom.part.data.WeaponUnitData;
import org.ywzj.vehicle.custom.sync.PartUnitSyncData;
import org.ywzj.vehicle.custom.sync.SyncDataHolder;
import org.ywzj.vehicle.custom.weapon.data.BaseVehicleWeaponData;
import org.ywzj.vehicle.custom.weapon.data.VehicleCannonWeaponData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.network.message.ClientVehicleAction;
import org.ywzj.vehicle.particle.SmokeCloudOption;
import org.ywzj.vehicle.util.VectorUtil;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.part.AutoWeaponUnit;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;
import org.ywzj.vehicle.vehicle.pojo.AimContext;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** 可配置的抽象武器模块<br/>
 * @param <T> 配置数据结构
 */
public abstract class AbstractVehicleWeapon<T extends BaseVehicleWeaponData> implements INBTSerializable<CompoundTag> {

    protected final AbstractVehicle vehicle;
    protected final WeaponUnit weaponUnit;
    private final int index;
    private final T data;
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

    public void onSwitchTo() {}

    public void onSwitchFrom() {}

    public void setRemainAmmo(int remainAmmo) {
        this.remainAmmo = remainAmmo;
    }

    protected void setReloadTime(int reloadTime) {
        this.reloadTime = reloadTime;
        if (reloadTime == 0) {
            if (getReloadSound() != null) {
                Vec3 soundPosition = getWeaponUnit().worldPivotPosition();
                VehicleSound reloadSound = new VehicleSound(getReloadSound(), soundPosition.subtract(vehicle.position()),
                        1f, 2f, 1f,
                        false, 0, false, false, vehicle.getId());
                reloadSound.play();
            }
        }
    }

    public boolean isCoolingDown() {
        return System.currentTimeMillis() - lastShootTime < (this.getShootInterval() - (vehicle.level().isClientSide() ? 0 : 20));
    }

    public long getShootInterval() {
        return this.getData().getShootInterval();
    }

    @OnlyIn(Dist.CLIENT)
    public boolean doClientShoot() {
        VehicleFireEvent.Pre __VehicleFireEvent_Pre = new VehicleFireEvent.Pre(vehicle, this, Minecraft.getInstance().player);
        NeoForge.EVENT_BUS.post(__VehicleFireEvent_Pre);
        if (__VehicleFireEvent_Pre.isCanceled()) {
            return false;
        }
        if (isCoolingDown()) {
            return false;
        }
        if (!hasAmmo()) {
            return false;
        }
        int partUnitIndex;
        Optional<AbstractVehicleWeapon<?>> weaponOptional = weaponUnit.getCurrentWeapon();
        if (weaponOptional.isPresent() && weaponOptional.get() == this) {
            partUnitIndex = weaponUnit.getIndex();
        } else {
            partUnitIndex = weaponUnit.getParentWeaponUnit() != null ? weaponUnit.getParentWeaponUnit().getIndex() : weaponUnit.getIndex();
        }
        List<AimContext> aimContexts;
        if (weaponUnit.getFiringMode() == WeaponUnitData.FiringMode.RIPPLE) {
            aimContexts = Collections.singletonList(weaponUnit.aimContext());
        } else if (weaponUnit.getFiringMode() == WeaponUnitData.FiringMode.SALVO) {
            aimContexts = weaponUnit.aimContexts();
        } else {
            return false;
        }
        WeaponUnit rootParentWeaponUnit = weaponUnit.getRootParentWeaponUnit();
        List<Vec3> positions = rootParentWeaponUnit.aimContexts().stream().map(context -> context.from).toList();
        double x = positions.stream().mapToDouble(pos -> pos.x).average().orElse(0);
        double y = positions.stream().mapToDouble(pos -> pos.y).average().orElse(0);
        double z = positions.stream().mapToDouble(pos -> pos.z).average().orElse(0);
        AimContext currentAimContext = rootParentWeaponUnit.aimContext();
        Vec3 targetVec = VectorUtil.rotToVec(currentAimContext.direction.x, currentAimContext.direction.y);
        Vec3 start = new Vec3(x, y, z);
        Vec3 end = start.add(targetVec.scale(LocalVehiclePlayer.renderDistance()));
        Vec3 aimHitPosition = VectorUtil.hitPosition(LocalVehiclePlayer.instance.getPlayer(), start, end);
        aimContexts.forEach(aimContext -> {
            aimContext.from = aimContext.from.add(vehicle.getDeltaMovement());
            aimContext.position = aimHitPosition;
        });
        lastShootTime = System.currentTimeMillis();
        sendShoot(this.getVehicle(), partUnitIndex, getIndex(), aimContexts);
        return true;
    }

    @OnlyIn(Dist.CLIENT)
    public void onClientFire() {
        Level level = vehicle.level();
        Vec3 soundPosition = getWeaponUnit().worldPivotPosition();
        if (getFireSound() != null) {
            level.playSound(LocalVehiclePlayer.instance.getPlayer(), soundPosition.x, soundPosition.y, soundPosition.z,
                    getFireSound(), SoundSource.PLAYERS,
                    4f, 1f);
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
                    Minecraft.getInstance().submit(() -> {
                        VehicleSound shellSound = new VehicleSound(getShellSound(), soundPosition.subtract(vehicle.position()),
                                1f, 2f, 1f,
                                false, 0, false, false, vehicle.getId());
                        shellSound.play();
                    });
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
        float caliber = 20;
        if (data instanceof VehicleCannonWeaponData vehicleCannonWeaponData) {
            caliber = vehicleCannonWeaponData.getCaliber();
        }
        float scale = caliber / 20;
        for (Vec3 muzzlePos : aimContexts.stream().map(aimContext -> aimContext.from).toList()) {
            for (int i = 0; i < 3 * recoil; i++) {
                double dx = (level.random.nextDouble() - 0.5) * 0.4 * recoil;
                double dy = (level.random.nextDouble() - 0.5) * 0.2 * recoil;
                double dz = (level.random.nextDouble() - 0.5) * 0.4 * recoil;
                level.addParticle(new SmokeCloudOption(1f, 0.35f + (float) level.random.nextDouble() * 0.3f, 0f, 6, 0.18f * scale, 0.005f), true,
                        muzzlePos.x + dx, muzzlePos.y + dy, muzzlePos.z + dz,
                        0.015, 0.015, 0.015);
            }
            for (int i = 0; i < 3 * recoil + 1; i++) {
                double dx = (level.random.nextDouble() - 0.5) * 0.4 * recoil;
                double dy = (level.random.nextDouble() - 0.5) * 0.2 * recoil;
                double dz = (level.random.nextDouble() - 0.5) * 0.4 * recoil;
                level.addParticle(new SmokeCloudOption(0.7f, 0.7f, 0.7f, 12, 0.3f * scale, 0.005f), true,
                        muzzlePos.x + dx, muzzlePos.y + dy, muzzlePos.z + dz,
                        0.015, 0.015, 0.015);
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
        return weaponUnit.getAmmoCapacity() != -1 ? weaponUnit.getAmmoCapacity() : this.getData().getMaxCapacity();
    }

    @OnlyIn(Dist.CLIENT)
    public static void sendShoot(AbstractVehicle abstractVehicle, int partUnitIndex, int weaponIndex, List<AimContext> aimContexts) {
        ClientVehicleAction action = new ClientVehicleAction();
        action.vehicleEntityId = abstractVehicle.getId();
        action.partUnitIndex = partUnitIndex;
        action.shoot = true;
        action.weaponIndex = weaponIndex;
        action.aimContexts = aimContexts;
        PacketDistributor.sendToServer(action);
    }

    public boolean hasAmmo() {
        return remainAmmo > 0;
    }

    public boolean consumeAmmo(List<AimContext> aimContexts) {
        if (remainAmmo == 0) {
            return false;
        }
        if (remainAmmo >= aimContexts.size()) {
            remainAmmo -= aimContexts.size();
            return true;
        }
        aimContexts.subList(remainAmmo, aimContexts.size()).clear();
        remainAmmo = 0;
        return true;
    }

    /**
     * 获取载具储存中的弹药数量，此方法在客户端无效
     * @return 弹药数量
     */
    public int getStorageAmmo() {
        if (vehicle.level().isClientSide()) {
            return 0;
        }
        int total = 0;
        for (int i = 0; i < vehicle.getItemHandler().getSlots(); i++) {
            ItemStack stack = vehicle.getItemHandler().getStackInSlot(i);
            if (isAmmoForWeapon(stack)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /**
     * 获取载具储存中是否有可供该武器换弹的弹药，此方法在客户端无效
     * @return 是否有弹药
     */
    public boolean hasStorageAmmo() {
        if (vehicle.level().isClientSide()) {
            return false;
        }
        for (int i = 0; i < vehicle.getItemHandler().getSlots(); i++) {
            ItemStack stack = vehicle.getItemHandler().getStackInSlot(i);
            if (stack.getItem() == AllItems.AMMO_CREATIVE.get()) {
                return true;
            }
            if (isAmmoForWeapon(stack)) {
                return true;
            }
        }
        return false;
    }

    public boolean isAmmoForWeapon(ItemStack stack) {
        return this.getData().getReload().isAmmo(stack);
    }

    public boolean canReload() {
        return hasStorageAmmo() && !isReloading() && (weaponUnit.getOwner() != null || weaponUnit instanceof AutoWeaponUnit);
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
        int maxCap = getMaxCapacity();
        if (vehicle.getItemStacks().stream().anyMatch(stack -> stack.getItem() == AllItems.AMMO_CREATIVE.get())) {
            remainAmmo = maxCap;
        } else {
            for (var item : vehicle.getItemStacks()) {
                int need = maxCap - remainAmmo;
                if (need <= 0) {
                    break;
                }
                if (this.isAmmoForWeapon(item)) {
                    int toTake = Math.min(need, item.getCount());
                    item.shrink(toTake);
                    remainAmmo += toTake;
                }
            }
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

    public boolean withSeeker() {
        return false;
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
        Optional<BaseDisplay> displayOptional = ClientAssetsManager.INSTANCE.getWeaponDisplay(data.getWeaponId());
        return displayOptional.map(display -> display.getSoundEvents().get("fire")).orElse(null);
    }

    public SoundEvent getShellSound() {
        Optional<BaseDisplay> displayOptional = ClientAssetsManager.INSTANCE.getWeaponDisplay(data.getWeaponId());
        return displayOptional.map(display -> display.getSoundEvents().get("shell")).orElse(null);
    }

    public SoundEvent getReloadSound() {
        Optional<BaseDisplay> displayOptional = ClientAssetsManager.INSTANCE.getWeaponDisplay(data.getWeaponId());
        return displayOptional.map(display -> display.getSoundEvents().get("reload")).orElse(null);
    }

    public boolean hasSyncData() {
        return true;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        var tag = new CompoundTag();
        tag.putInt("RemainAmmo", remainAmmo);
        return tag;
    }

    @Override
    public void deserializeNBT(@org.jetbrains.annotations.NotNull HolderLookup.Provider provider, @org.jetbrains.annotations.NotNull CompoundTag nbt) {
        this.remainAmmo = nbt.getInt("RemainAmmo");
    }

    public String getSerializeId() {
        return serializeId;
    }

}
