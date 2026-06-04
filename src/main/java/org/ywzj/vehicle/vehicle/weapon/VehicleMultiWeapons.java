package org.ywzj.vehicle.vehicle.weapon;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.ywzj.vehicle.all.AllItems;
import org.ywzj.vehicle.api.custom.sync.SyncDataSerializers;
import org.ywzj.vehicle.api.event.VehicleFireEvent;
import org.ywzj.vehicle.custom.part.data.WeaponUnitData;
import org.ywzj.vehicle.custom.sync.PartUnitSyncData;
import org.ywzj.vehicle.custom.sync.SyncDataHolder;
import org.ywzj.vehicle.custom.weapon.data.BaseVehicleWeaponData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.util.VectorUtil;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;
import org.ywzj.vehicle.vehicle.pojo.AimContext;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class VehicleMultiWeapons extends AbstractVehicleWeapon<BaseVehicleWeaponData> {

    private final List<AbstractVehicleWeapon<?>> subWeapons;
    private int selectedIndex = 0;
    private SyncDataHolder<Integer> selectedIndexHolder;

    public VehicleMultiWeapons(AbstractVehicle vehicle, WeaponUnit weaponUnit, int index,
                               List<AbstractVehicleWeapon<?>> subWeapons, String serializeId) {
        super(vehicle, weaponUnit, index, new BaseVehicleWeaponData(), serializeId);
        this.subWeapons = subWeapons;
    }

    public AbstractVehicleWeapon<?> getSelectedWeapon() {
        return subWeapons.get(selectedIndex);
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void setSelectedIndex(int selectedIndex) {
        this.selectedIndex = selectedIndex;
    }

    public List<AbstractVehicleWeapon<?>> getSubWeapons() {
        return subWeapons;
    }

    @Override
    public void defineSyncData(PartUnitSyncData syncData) {
        this.selectedIndexHolder = syncData.define(
                SyncDataSerializers.INT,
                this::setSelectedIndex,
                this::getSelectedIndex,
                selectedIndex
        );
        for (AbstractVehicleWeapon<?> sub : subWeapons) {
            sub.defineSyncData(syncData);
        }
    }

    @Override
    public boolean hasSyncData() {
        return true;
    }

    @Override
    public boolean shoot(List<AimContext> aimContexts, LivingEntity shooter) {
        return getSelectedWeapon().shoot(aimContexts, shooter);
    }

    @Override
    public boolean check(List<AimContext> aimContexts, LivingEntity shooter) {
        return getSelectedWeapon().check(aimContexts, shooter);
    }

    @Override
    public void tick() {
        if (vehicle.level().isClientSide()) {
            return;
        }
        getSelectedWeapon().tick();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean doClientShoot() {
        AbstractVehicleWeapon<?> selected = getSelectedWeapon();
        if (MinecraftForge.EVENT_BUS.post(new VehicleFireEvent.Pre(vehicle, this, Minecraft.getInstance().player))) {
            return false;
        }
        if (selected.isCoolingDown()) {
            return false;
        }
        if (!selected.hasAmmo()) {
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
        selected.lastShootTime = System.currentTimeMillis();
        sendShoot(vehicle, partUnitIndex, getIndex(), aimContexts);
        return true;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void onClientFire() {
        getSelectedWeapon().onClientFire();
    }

    @Override
    public BaseVehicleWeaponData getData() {
        return getSelectedWeapon().getData();
    }

    @Override
    public Component getDisplayName() {
        return getSelectedWeapon().getDisplayName();
    }

    @Override
    public SoundEvent getFireSound() {
        return getSelectedWeapon().getFireSound();
    }

    @Override
    public SoundEvent getShellSound() {
        return getSelectedWeapon().getShellSound();
    }

    @Override
    public SoundEvent getReloadSound() {
        return getSelectedWeapon().getReloadSound();
    }

    @Override
    public boolean hasAmmo() {
        return getSelectedWeapon().hasAmmo();
    }

    @Override
    public int getRemainAmmo() {
        return getSelectedWeapon().getRemainAmmo();
    }

    @Override
    public int getReloadTime() {
        return getSelectedWeapon().getReloadTime();
    }

    @Override
    public boolean isCoolingDown() {
        return getSelectedWeapon().isCoolingDown();
    }

    @Override
    public boolean isReloading() {
        return getSelectedWeapon().isReloading();
    }

    @Override
    public boolean isAmmoForWeapon(ItemStack stack) {
        return getSelectedWeapon().isAmmoForWeapon(stack);
    }

    @Override
    public boolean withSeeker() {
        return getSelectedWeapon().withSeeker();
    }

    @Override
    public boolean consumeAmmo(List<AimContext> aimContexts) {
        return getSelectedWeapon().consumeAmmo(aimContexts);
    }

    @Override
    public int getMaxCapacity() {
        return getSelectedWeapon().getMaxCapacity();
    }

    @Override
    public long getShootInterval() {
        return getSelectedWeapon().getShootInterval();
    }

    @Override
    public void onSwitchTo() {
        getSelectedWeapon().onSwitchTo();
    }

    @Override
    public void onSwitchFrom() {
        getSelectedWeapon().onSwitchFrom();
    }

    public void cycleSubWeapon(boolean next) {
        AbstractVehicleWeapon<?> oldWeapon = getSelectedWeapon();
        int oldAmmo = oldWeapon.getRemainAmmo();

        if (oldAmmo > 0 && !vehicle.level().isClientSide()) {
            ItemStack[] matchingItems = oldWeapon.getData().getReload().getAmmo().getItems();
            if (matchingItems.length > 0) {
                AtomicReference<ItemStack> returnStack = new AtomicReference<>(matchingItems[0].copy());
                returnStack.get().setCount(oldAmmo);
                vehicle.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(cap -> {
                    for (int i = 0; i < cap.getSlots(); i++) {
                        ItemStack stack = cap.getStackInSlot(i);
                        if (stack.getItem() == AllItems.AMMO_CREATIVE.get()) {
                            returnStack.set(ItemStack.EMPTY);
                            return;
                        }
                    }
                    for (int i = 0; i < cap.getSlots(); i++) {
                        returnStack.set(cap.insertItem(i, returnStack.get(), false));
                        if (returnStack.get().isEmpty()) {
                            break;
                        }
                    }
                });
                if (!returnStack.get().isEmpty()) {
                    vehicle.spawnAtLocation(returnStack.get());
                }
            }
        }

        oldWeapon.setRemainAmmo(0);
        oldWeapon.startReload();
        int size = subWeapons.size();
        selectedIndex = (selectedIndex + (next ? 1 : size - 1)) % size;
        getSelectedWeapon().setRemainAmmo(0);
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("SelectedIndex", selectedIndex);
        for (int i = 0; i < subWeapons.size(); i++) {
            AbstractVehicleWeapon<?> sub = subWeapons.get(i);
            String subId = sub.getSerializeId();
            if (subId != null) {
                CompoundTag subData = sub.serializeNBT();
                if (!subData.isEmpty()) {
                    tag.put(subId, subData);
                }
            }
        }
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.selectedIndex = nbt.getInt("SelectedIndex");
        for (AbstractVehicleWeapon<?> sub : subWeapons) {
            String subId = sub.getSerializeId();
            if (subId != null && nbt.contains(subId, Tag.TAG_COMPOUND)) {
                sub.deserializeNBT(nbt.getCompound(subId));
            }
        }
    }

}
