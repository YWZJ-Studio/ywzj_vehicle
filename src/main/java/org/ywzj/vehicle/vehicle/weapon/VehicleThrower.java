package org.ywzj.vehicle.vehicle.weapon;

import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.registries.ForgeRegistries;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.custom.weapon.data.VehicleThrowerWeaponData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.weapon.AmmoEntity;
import org.ywzj.vehicle.util.VectorUtil;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;
import org.ywzj.vehicle.vehicle.pojo.AimContext;

import java.util.List;

public class VehicleThrower extends AbstractVehicleWeapon<VehicleThrowerWeaponData> {

    private final EntityType<?> entityType;

    public VehicleThrower(AbstractVehicle vehicle, WeaponUnit unit, int index, VehicleThrowerWeaponData data, String serializeId) {
        super(vehicle, unit, index, data, serializeId);
        this.entityType = data.getEntityId() != null && ForgeRegistries.ENTITY_TYPES.containsKey(data.getEntityId())
                ? ForgeRegistries.ENTITY_TYPES.getValue(data.getEntityId()) : null;
        if (entityType == null) {
            YwzjVehicle.LOGGER.warn("Unknown thrower entity {} for weapon {}", data.getEntityId(), data.getWeaponId());
        }
    }

    @Override
    protected void reloadFromInventory(int maxCapacity) {
        InvWrapper inventory = new InvWrapper(vehicle);
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            if (!isAmmoForWeapon(inventory.getStackInSlot(slot))) {
                continue;
            }
            ItemStack supply = inventory.extractItem(slot, 1, false);
            if (supply.isEmpty()) {
                continue;
            }
            remainAmmo = maxCapacity;
            ItemStack remainder = supply.getCraftingRemainingItem();
            if (!remainder.isEmpty()) {
                remainder = inventory.insertItem(slot, remainder, false);
                remainder = ItemHandlerHelper.insertItemStacked(inventory, remainder, false);
                if (!remainder.isEmpty()) {
                    vehicle.spawnAtLocation(remainder);
                }
            }
            return;
        }
    }

    @Override
    public boolean shoot(List<AimContext> aimContexts, LivingEntity shooter) {
        if (entityType == null || vehicle.level().isClientSide() || aimContexts.isEmpty() || isCoolingDown() || isReloading() || !hasAmmo()) {
            return false;
        }
        int shots = Math.min(remainAmmo, aimContexts.size());
        boolean fired = false;
        for (int i = 0; i < shots; i++) {
            AimContext aim = aimContexts.get(i);
            Entity entity = entityType.create(vehicle.level());
            if (entity == null) {
                continue;
            }
            if (entity instanceof AmmoEntity ammo) {
                ammo.setWeaponId(getData().getWeaponId());
                ammo.vehicle = vehicle;
                ammo.name = getDisplayName();
                ammo.life = getData().getLife();
                ammo.damage = getData().getDamage();
                ammo.headShot = getData().getHeadshotMultiplier();
            }
            if (entity instanceof Projectile projectile) {
                projectile.setOwner(shooter);
            }
            double spread = 0.0172275 * getData().getInaccuracy();
            Vec3 direction = VectorUtil.rotToVec(aim.direction.x, aim.direction.y).add(
                    vehicle.level().random.triangle(0, spread),
                    vehicle.level().random.triangle(0, spread),
                    vehicle.level().random.triangle(0, spread)).normalize();
            entity.moveTo(aim.from.x, aim.from.y, aim.from.z, aim.direction.y, aim.direction.x);
            entity.setDeltaMovement(direction.scale(getData().getVelocity()).add(vehicle.getDeltaMovement()));
            if (vehicle.level().addFreshEntity(entity)) {
                remainAmmo--;
                fired = true;
                vehicle.physicsEngine.recoil(getWeaponUnit(), getData().getRecoil());
            }
        }
        if (fired) {
            lastShootTime = System.currentTimeMillis();
        }
        return fired;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void onClientFire() {
        playFireSounds();
        List<AimContext> aimContexts = getFiringAimContexts();
        if (aimContexts.isEmpty()) {
            return;
        }
        weaponUnit.countFire(aimContexts.size());
        var particleType = getData().getMuzzleParticle() == null ? null
                : ForgeRegistries.PARTICLE_TYPES.getValue(getData().getMuzzleParticle());
        if (particleType instanceof SimpleParticleType muzzleParticle) {
            spawnMuzzleParticles(aimContexts, muzzleParticle);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void spawnMuzzleParticles(List<AimContext> aimContexts, SimpleParticleType muzzleParticle) {
        for (AimContext aim : aimContexts) {
            Vec3 muzzle = aim.from;
            Vec3 motion = VectorUtil.rotToVec(aim.direction.x, aim.direction.y).scale(getData().getVelocity() * 0.35);
            for (int i = 0; i < 8; i++) {
                vehicle.level().addParticle(muzzleParticle,
                        muzzle.x, muzzle.y, muzzle.z,
                        motion.x + (vehicle.level().random.nextDouble() - 0.5) * 0.08, motion.y,
                        motion.z + (vehicle.level().random.nextDouble() - 0.5) * 0.08);
            }
        }
    }

}
