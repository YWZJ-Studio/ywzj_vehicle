package org.ywzj.vehicle.entity.weapon;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.PlayMessages;
import org.ywzj.vehicle.all.AllEntities;
import org.ywzj.vehicle.all.AllParticleTypes;
import org.ywzj.vehicle.api.entity.BoundingBoxChangeable;
import org.ywzj.vehicle.api.entity.SightObstruction;

public class SmokeGrenadeEntity extends GrenadeEntity implements BoundingBoxChangeable, SightObstruction {

    public SmokeGrenadeEntity(Entity entity, Level level, ResourceLocation weaponId) {
        super(AllEntities.SMOKE_GRENADE.get(), entity, level, weaponId);
    }

    public SmokeGrenadeEntity(PlayMessages.SpawnEntity spawnEntity, Level level) {
        super(AllEntities.SMOKE_GRENADE.get(), level);
    }

    public SmokeGrenadeEntity(EntityType<SmokeGrenadeEntity> type, Level level) {
        super(type, level);
    }

    public AABB getAABB() {
        if (entityData.get(EXPLODED)) {
            return AABB.ofSize(position(), 10, 10, 10);
        }
        return AABB.ofSize(position(), 0.3, 0.3, 0.3);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            if (entityData.get(EXPLODED)) {
                // 在半球形范围内生成烟雾粒子
                double x = this.getX();
                double y = this.getY();
                double z = this.getZ();
                for (int i = 0; i < 48; i++) {
                    double offsetX = this.random.triangle(0, 10);
                    double offsetY = this.random.triangle(0, 8);
                    double offsetZ = this.random.triangle(0, 10);
                    this.level().addParticle(AllParticleTypes.SMOKE_CLOUD.get(), true, x + offsetX, y + offsetY, z + offsetZ, 0.0D, 0.0D, 0.0D);
                }
            }
        }
    }

}
