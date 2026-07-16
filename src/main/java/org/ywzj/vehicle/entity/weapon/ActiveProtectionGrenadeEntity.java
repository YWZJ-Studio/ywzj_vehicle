package org.ywzj.vehicle.entity.weapon;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.all.AllEntities;
import org.ywzj.vehicle.custom.weapon.data.VehicleGrenadeWeaponData;

import java.util.Comparator;
import java.util.List;

public class ActiveProtectionGrenadeEntity extends GrenadeEntity {

    public ActiveProtectionGrenadeEntity(EntityType<ActiveProtectionGrenadeEntity> type, Level level, VehicleGrenadeWeaponData data) {
        super(type, level, data.getWeaponId());
        initGrenade(data);
    }

    public ActiveProtectionGrenadeEntity(EntityType<ActiveProtectionGrenadeEntity> type, Level level) {
        super(type, level);
    }

    public ActiveProtectionGrenadeEntity(PlayMessages.SpawnEntity spawnEntity, Level level) {
        super(AllEntities.APS_GRENADE.get(), level);
    }

    @Override
    protected void onHit(HitResult result) {
        this.discard();
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            ServerLevel level = (ServerLevel) this.level();
            double radius = 8.0;
            AABB box = getBoundingBox().inflate(radius);
            List<Entity> entities = level().getEntities(
                    this,
                    box,
                    this::shouldCatch
            );
            if (entities.isEmpty()) {
                return;
            }
            entities.sort(Comparator.comparingDouble(entity -> -entity.getDeltaMovement().lengthSqr()));
            Projectile target = (Projectile) entities.get(0);
            Vec3 from = this.position();
            Vec3 to = target.position();
            for (ServerPlayer player : level.getPlayers(player -> player.distanceTo(this) < 256)) {
                level.sendParticles(
                        player,
                        ParticleTypes.EXPLOSION,
                        true,
                        from.x, from.y, from.z,
                        3,
                        0.2F, 0.2F, 0.2F,
                        0.15
                );
                level.sendParticles(
                        player,
                        ParticleTypes.EXPLOSION,
                        true,
                        to.x, to.y, to.z,
                        3,
                        0.2F, 0.2F, 0.2F,
                        0.15
                );
                Vec3 delta = to.subtract(from);
                int steps = 16;
                for (int i = 0; i <= steps; i++) {
                    double t = i / (double) steps;
                    Vec3 pos = from.add(delta.scale(t));
                    level.sendParticles(
                            ParticleTypes.SMOKE,
                            pos.x,
                            pos.y,
                            pos.z,
                            1,
                            0.0, 0.02, 0.0,
                            0.0
                    );
                }
                level.playSound(
                        null,
                        from.x, from.y, from.z,
                        SoundEvents.GENERIC_EXPLODE,
                        SoundSource.BLOCKS,
                        0.4F,
                        1.6F
                );
            }
            this.discard();
            target.discard();
            if (target.getOwner() instanceof ServerPlayer player) {
                player.displayClientMessage(Component.translatable("tips.active_protection_system_intercept"), true);
            }
        }
    }

    private boolean shouldCatch(Entity entity) {
        return entity.isAlive()
                && entity != this
                && entity instanceof Projectile;
    }

}
