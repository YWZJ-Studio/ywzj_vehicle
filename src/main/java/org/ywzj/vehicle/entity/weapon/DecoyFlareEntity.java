package org.ywzj.vehicle.entity.weapon;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.api.entity.TargetObstruction;
import org.ywzj.vehicle.audio.VehicleSound;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

public class DecoyFlareEntity extends AmmoEntity implements TargetObstruction {

    private static final ResourceLocation DECOY_FLARE_ID = YwzjVehicle.modLocation("decoy_flare");
    public static final int LIFE = 200;
    private VehicleSound tailSound;

    public DecoyFlareEntity(EntityType<? extends Projectile> pEntityType, Level pLevel, ResourceLocation weaponId) {
        super(pEntityType, pLevel, weaponId);
    }

    public DecoyFlareEntity(EntityType<? extends DecoyFlareEntity> entityType, Level level) {
        super(entityType, level, DECOY_FLARE_ID);
    }

    public void shoot(AbstractVehicle vehicle, Component name, Vec3 spawnPos, float ammoXRot, float ammoYRot, LivingEntity shooter) {
        this.vehicle = vehicle;
        this.name = name;
        this.setPos(spawnPos);
        this.setRot(ammoYRot, ammoXRot);
        this.setOwner(shooter);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            if (tickCount > LIFE) {
                this.discard();
                return;
            }
            Vec3 startVec = this.position();
            Vec3 endVec = startVec.add(this.getDeltaMovement());
            BlockHitResult result = this.level().clip(new ClipContext(startVec, endVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (result.getType() != HitResult.Type.MISS) {
                this.discard();
                return;
            }
            Vec3 movement = this.getDeltaMovement();
            double x = movement.x;
            double y = movement.y;
            double z = movement.z;
            double nextPosX = this.getX() + x;
            double nextPosY = this.getY() + y;
            double nextPosZ = this.getZ() + z;
            this.setPos(nextPosX, nextPosY, nextPosZ);
            this.setDeltaMovement(this.getDeltaMovement().scale(1 - 0.01f));
            this.setDeltaMovement(this.getDeltaMovement().add(0, -0.1f, 0));
        } else {
            tickParticle();
            tickSound();
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void tickParticle() {
        Vec3 pos = new Vec3(this.xo, this.yo, this.zo);
        int age = this.tickCount;
        if (age % 2 == 0) {
            level().addParticle(
                    ParticleTypes.FLASH,
                    true,
                    pos.x, pos.y, pos.z,
                    0, 0, 0
            );
        }
        int smokeCount = age < 60 ? 1 : 3;
        for (int i = 0; i < smokeCount; i++) {
            level().addParticle(
                    ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    true,
                    pos.x, pos.y, pos.z,
                    0,
                    0,
                    0
            );
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void tickSound() {
        if (tailSound == null) {
            tailSound = new VehicleSound(AllSounds.DECOY_FLARE_TAIL.get(), 4f, 1f, 1f, false, 0, false, false, this.getId());
            tailSound.play();
        }
    }

    @Override
    public boolean shouldRender(double x, double y, double z) {
        return true;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

}
