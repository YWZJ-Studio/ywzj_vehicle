package org.ywzj.vehicle.entity.weapon;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.PlayMessages;
import org.ywzj.vehicle.all.AllEntities;
import org.ywzj.vehicle.api.entity.SightObstruction;
import org.ywzj.vehicle.custom.weapon.data.VehicleGrenadeWeaponData;
import org.ywzj.vehicle.particle.SmokeCloudOption;

public class SmokeGrenadeEntity extends GrenadeEntity implements SightObstruction {

    public SmokeGrenadeEntity(EntityType<SmokeGrenadeEntity> type, Level level, VehicleGrenadeWeaponData data) {
        super(type, level, data.getWeaponId());
        initGrenade(data);
    }

    public SmokeGrenadeEntity(EntityType<SmokeGrenadeEntity> type, Level level) {
        super(type, level);
    }

    public SmokeGrenadeEntity(PlayMessages.SpawnEntity spawnEntity, Level level) {
        super(AllEntities.SMOKE_GRENADE.get(), level);
    }

    @Override
    protected AABB makeBoundingBox() {
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
                    this.level().addParticle(new SmokeCloudOption(true, 1, 1, 1, 1, 1, 1,
                                    1f, 1f, 20,
                                    1f, 1.5f, 0f), true,
                            x + offsetX, y + offsetY, z + offsetZ,
                            0, 0, 0);
                }
            }
        }
    }

}
