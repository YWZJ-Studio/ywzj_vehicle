package org.ywzj.vehicle.entity.weapon;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.ywzj.vehicle.custom.weapon.data.VehicleGrenadeWeaponData;
import org.ywzj.vehicle.util.VehicleExplosion;

public class FragGrenadeEntity extends GrenadeEntity {

    public FragGrenadeEntity(EntityType<FragGrenadeEntity> type, Level level, VehicleGrenadeWeaponData data) {
        super(type, level, data.getWeaponId());
        initGrenade(data);
    }

    public FragGrenadeEntity(EntityType<FragGrenadeEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void onHit(HitResult result) {
        if (result.getType() == HitResult.Type.ENTITY) {
            this.onDeath(result);
            return;
        }
        super.onHit(result);
    }

    @Override
    public void onDeath(HitResult hitResult) {
        if (!this.isRemoved() && explosion != null && explosion.radius > 0) {
            VehicleExplosion vehicleExplosion = new VehicleExplosion(level(), this.getOwner(), this, this.position(),
                    explosion.radius, explosion.damage, explosion.destroyBlock);
            vehicleExplosion.explode();
        }
        this.discard();
    }

}
