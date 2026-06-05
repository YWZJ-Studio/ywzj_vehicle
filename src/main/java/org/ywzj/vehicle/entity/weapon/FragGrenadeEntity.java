package org.ywzj.vehicle.entity.weapon;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.ywzj.vehicle.all.AllEntities;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.util.VehicleExplosion;
import org.ywzj.vehicle.vehicle.pojo.Explosion;

public class FragGrenadeEntity extends GrenadeEntity {

    public AbstractVehicle vehicle;
    private Explosion explosion;

    public FragGrenadeEntity(Entity shooter, Level level, ResourceLocation weaponId) {
        super(AllEntities.FRAG_GRENADE.get(), shooter, level, weaponId);
    }

    public FragGrenadeEntity(EntityType<FragGrenadeEntity> type, Level level) {
        super(type, level);
    }

    public void setExplosionData(Explosion explosion) {
        this.explosion = explosion;
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
            VehicleExplosion vehicleExplosion = new VehicleExplosion(level(), this, this.vehicle, this.position(),
                    explosion.radius, explosion.damage, explosion.destroyBlock);
            vehicleExplosion.explode();
        }
        this.discard();
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
        super.writeSpawnData(buffer);
        buffer.writeFloat(explosion != null ? explosion.damage : 0);
        buffer.writeFloat(explosion != null ? explosion.radius : 0);
        buffer.writeBoolean(explosion != null && explosion.destroyBlock);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf additionalData) {
        super.readSpawnData(additionalData);
        explosion = new Explosion();
        explosion.damage = additionalData.readFloat();
        explosion.radius = additionalData.readFloat();
        explosion.destroyBlock = additionalData.readBoolean();
    }

}
