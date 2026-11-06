package org.ywzj.vehicle.entity.weapon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.util.BulletHitResult;
import org.ywzj.vehicle.util.EntityUtil;
import org.ywzj.vehicle.util.VectorUtil;

import java.util.Comparator;

public abstract class AbstractSprayEntity extends AmmoEntity {

    private static final double GRAVITY = 0.035;
    private static final double DRAG = 0.99;
    private final SimpleParticleType trailParticle;

    protected AbstractSprayEntity(EntityType<? extends AbstractSprayEntity> type, Level level,
                                  ResourceLocation weaponId, SimpleParticleType trailParticle) {
        super(type, level, weaponId);
        this.trailParticle = trailParticle;
    }

    public void shoot(AbstractVehicle vehicle, Component name, Vec3 position, float pitch, float yaw,
                      float velocity, float inaccuracy, LivingEntity shooter) {
        this.vehicle = vehicle;
        this.name = name;
        Vec3 direction = VectorUtil.rotToVec(pitch, yaw).add(
                random.triangle(0, 0.0172275 * inaccuracy),
                random.triangle(0, 0.0172275 * inaccuracy),
                random.triangle(0, 0.0172275 * inaccuracy)).normalize();
        setPos(position);
        setDeltaMovement(direction.scale(velocity).add(vehicle.getDeltaMovement()));
        setRot(yaw, pitch);
        setOwner(shooter);
    }

    @Override
    public void tick() {
        Vec3 previous = position();
        super.tick();
        if (isRemoved()) {
            return;
        }
        if (level().isClientSide()) {
            spawnTrail(previous, position());
            return;
        }
        if (--life < 0 || isInWater() || getY() < level().getMinBuildHeight() - 16) {
            discard();
            return;
        }
        Vec3 next = position().add(getDeltaMovement());
        if (!level().hasChunkAt(BlockPos.containing(next))) {
            discard();
            return;
        }
        tickHit();
        if (!isRemoved()) {
            setPos(next);
            setDeltaMovement(getDeltaMovement().scale(DRAG).add(0, -GRAVITY, 0));
        }
    }

    private void spawnTrail(Vec3 from, Vec3 to) {
        Vec3 motion = to.subtract(from).scale(0.35);
        int steps = Math.max(1, Math.min(24, (int) Math.ceil(from.distanceTo(to) / 0.18)));
        for (int i = 1; i <= steps; i++) {
            Vec3 point = from.lerp(to, (double) i / steps);
            for (int j = 0; j < 2; j++) {
                level().addParticle(trailParticle,
                        point.x + random.triangle(0, 0.12), point.y + random.triangle(0, 0.12), point.z + random.triangle(0, 0.12),
                        motion.x + random.triangle(0, 0.025), motion.y, motion.z + random.triangle(0, 0.025));
            }
        }
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        super.writeSpawnData(buffer);
        buffer.writeInt(vehicle == null ? -1 : vehicle.getId());
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buffer) {
        super.readSpawnData(buffer);
        if (level().getEntity(buffer.readInt()) instanceof AbstractVehicle source) {
            vehicle = source;
        }
    }

    @Override
    protected void tickHit() {
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        Vec3 start = position();
        Vec3 end = start.add(getDeltaMovement());
        BlockHitResult block = level().clip(createClipContext(start, end));
        BulletHitResult entity = EntityUtil.findEntitiesOnPath(this, start, end).stream()
                .filter(hit -> hit.getEntity().isAlive() && hit.getEntity() != getOwner()
                        && hit.getEntity() != vehicle && hit.getEntity().getRootVehicle() != vehicle)
                .min(Comparator.comparingDouble(hit -> start.distanceToSqr(hit.getLocation())))
                .orElse(null);
        if (entity != null && (block.getType() == HitResult.Type.MISS
                || start.distanceToSqr(entity.getLocation()) < start.distanceToSqr(block.getLocation()))) {
            onSprayHitEntity(server, entity);
            setPos(entity.getLocation());
            discard();
        } else if (block.getType() != HitResult.Type.MISS) {
            onSprayHitBlock(server, block);
            setPos(block.getLocation());
            discard();
        }
    }

    protected abstract ClipContext createClipContext(Vec3 start, Vec3 end);

    protected abstract void onSprayHitEntity(ServerLevel level, BulletHitResult hit);

    protected abstract void onSprayHitBlock(ServerLevel level, BlockHitResult hit);

}
