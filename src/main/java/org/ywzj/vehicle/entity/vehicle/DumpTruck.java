package org.ywzj.vehicle.entity.vehicle;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Math;
import org.joml.Vector3f;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.vehicle.OBB;
import org.ywzj.vehicle.vehicle.PartUnit;
import org.ywzj.vehicle.vehicle.SpotterUnit;

public class DumpTruck extends WheeledVehicle {

    public DumpTruck(EntityType<? extends Mob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    public void initPartUnits() {
        PartUnit dumpTruckBed = new PartUnit("dump_truck_bed", 0, this);
        dumpTruckBed.ownerViewOffset = new Vec3(0.5 ,1, 3);
        dumpTruckBed.seatOffset = new Vec3(0.5 ,1, 3);
        this.partUnits.add(dumpTruckBed);
        this.operatorUnits.add(dumpTruckBed);
        this.spotterUnit = new SpotterUnit(this,
                new Vec3(0, 4.54d, -0.375d),
                new Vec3(0, 1.5d, 0),
                new Vec3(0, 0, 0),
                null);
    }

    @Override
    public SoundEvent getEngineStartSound() {
        return AllSounds.LAV150_ENGINE_START.get();
    }

    @Override
    public SoundEvent getEngineIdleSound() {
        return AllSounds.LAV150_ENGINE_IDLE.get();
    }

    @Override
    public SoundEvent getEngineRunSound() {
        return AllSounds.LAV150_ENGINE_RUN.get();
    }

    @Override
    protected void tickParticle() {
        if (!this.getPassengers().isEmpty() && tickCount % 10 == 0) {
            Vec3 v1 = this.getLookAngle();
            Vec3 v2 = new Vec3(-v1.z, 0, v1.x).normalize();
            Vec3 engineSmokePos = this.position().add(this.getLookAngle().normalize().scale(2f)).add(v2.scale(-1.6)).add(0, 3, 0);
            level().addParticle(ParticleTypes.LARGE_SMOKE, true, engineSmokePos.x, engineSmokePos.y, engineSmokePos.z, 0, 0, 0);
        }
    }

    @Override
    public void shoot(int weaponIndex, Vec3 ammoSpawnPosition, float ammoXRot, float ammoYRot) {
//        if (weaponIndex < operatorUnits.size()) {
//            if (operatorUnits.get(weaponIndex) instanceof WeaponUnit machineGunTurret) {
//                machineGunTurret.shoot(ammoSpawnPosition, ammoXRot, ammoYRot);
//                this.level().playSound(null, this, AllSounds.LAV150_SHOOT.get(), SoundSource.PLAYERS, 16f, 1f);
//            }
//        }
    }

    @Override
    public void support(Entity pEntity) {
        Vec3 feetPosition = pEntity.position().subtract(new Vec3(0, 0.1f, 0));

        Vector3f sum = new Vector3f();
        for (OBB obb : getOBBs()) {
            sum.add(obb.center());
        }
        Vector3f c = sum.div(getOBBs().size());

        for (OBB obb : getOBBs()) {
            if (obb.contains(feetPosition)) {
                if (!pEntity.noPhysics && !this.noPhysics) {
                    double onVehicleGravity = Math.max(0, pEntity.getDeltaMovement().y);
                    if (onVehicleGravity == 0) {
                        pEntity.setOnGround(true);
                    }
                    double d = obb.embeddingDepth(feetPosition);
                    pEntity.setDeltaMovement(this.getDeltaMovement().add(0, onVehicleGravity + d < 0.1f ? 0 : d, 0));
                    continue;
                }
            }
            if (!pEntity.noPhysics && !this.noPhysics) {
                if (OBB.isColliding(obb, pEntity.getBoundingBox())) {
                    Vector3f[] axes = obb.getAxes();
                    Vector3f pos = pEntity.position().toVector3f();
                    if (pos.dot(axes[0]) >= 0.01F) {

                    }


                    pEntity.setPos(new Vec3(c));
                    pEntity.hasImpulse = true;

//                    double d0 = pEntity.getX() - obb.center().x;
//                    double d1 = pEntity.getZ() - obb.center().z;
//                    double d2 = Mth.absMax(d0, d1);
//                    if (d2 >= (double)0.01F) {
//                        d2 = Math.sqrt(d2);
//                        d0 /= d2;
//                        d1 /= d2;
//                        double d3 = 1.0D / d2;
//                        if (d3 > 1.0D) {
//                            d3 = 1.0D;
//                        }
//                        d0 *= d3;
//                        d1 *= d3;
//                        d0 *= 0.05F;
//                        d1 *= 0.05F;
//                        if (pEntity.isPushable()) {
//                            pEntity.setPos(pEntity.position().add(new Vec3(d0, 0.0D, d1)));
//                            pEntity.hasImpulse = true;
////                            pEntity.push(d0, 0.0D, d1);
//                        }
//                    }
                }
            }
        }
    }

}
