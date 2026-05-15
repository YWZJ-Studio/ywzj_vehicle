package org.ywzj.vehicle.entity.weapon;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.network.PlayMessages;
import org.ywzj.vehicle.all.AllEntities;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.util.VehicleExplosion;

public class FragGrenadeEntity extends GrenadeEntity {

    public AbstractVehicle vehicle;
    private float explosionDamage;
    private float explosionRadius;
    private boolean destroyBlock;

    public FragGrenadeEntity(Entity shooter, Level level, ResourceLocation weaponId) {
        super(AllEntities.FRAG_GRENADE.get(), shooter, level, weaponId);
    }

    public FragGrenadeEntity(PlayMessages.SpawnEntity spawnEntity, Level level) {
        super(AllEntities.FRAG_GRENADE.get(), level);
    }

    public FragGrenadeEntity(EntityType<FragGrenadeEntity> type, Level level) {
        super(type, level);
    }

    public void setExplosionData(float damage, float radius, boolean destroyBlock) {
        this.explosionDamage = damage;
        this.explosionRadius = radius;
        this.destroyBlock = destroyBlock;
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
        if (!this.isRemoved() && explosionRadius > 0) {
            VehicleExplosion vehicleExplosion = new VehicleExplosion(level(), this, this.vehicle, this.position(),
                    explosionRadius, explosionDamage, destroyBlock);
            vehicleExplosion.explode();
        }
        this.discard();
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        super.writeSpawnData(buffer);
        buffer.writeFloat(explosionDamage);
        buffer.writeFloat(explosionRadius);
        buffer.writeBoolean(destroyBlock);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf additionalData) {
        super.readSpawnData(additionalData);
        explosionDamage = additionalData.readFloat();
        explosionRadius = additionalData.readFloat();
        destroyBlock = additionalData.readBoolean();
    }

}
