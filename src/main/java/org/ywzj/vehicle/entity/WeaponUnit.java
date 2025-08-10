package org.ywzj.vehicle.entity;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class WeaponUnit {
    private final AbstractVehicle vehicle;
    private LivingEntity operator;
    public float aimXRot;
    public float aimYRot;
    public float xRot;
    public float yRot;
    public float xRotO;
    public float yRotO;
    public float xRotSpeed;
    public  float yRotSpeed;
    public float maxXRot;
    public float minXRot;

    public WeaponUnit(AbstractVehicle vehicle) {
        this.vehicle = vehicle;
    }

    public void shoot(Vec3 ammoSpawnPosition) {
        BulletEntity bulletEntity = new BulletEntity(vehicle.level(), operator, ammoSpawnPosition);
        bulletEntity.shootFromRotation(vehicle, xRot, yRot, 0, 10.0f, 0f);
        bulletEntity.setDamage(25);
        bulletEntity.setHeadShot(1.5f);
        vehicle.level().addFreshEntity(bulletEntity);
    }

    public void setOperator(LivingEntity operator) {
        this.operator = operator;
    }

    public void tick() {
        if (vehicle.level().isClientSide()) {
            this.xRotO = this.xRot;
            this.yRotO = this.yRot;
        } else {
            float xDiff = Mth.wrapDegrees(this.aimXRot - this.xRot);
            float yDiff = Mth.wrapDegrees(this.aimYRot - this.yRot);

            if (Math.abs(xDiff) > xRotSpeed) {
                this.xRot += Math.signum(xDiff) * xRotSpeed;
            } else {
                this.xRot = this.aimXRot;
            }
            this.xRot = Math.max(Math.min(this.xRot, maxXRot), minXRot);

            if (Math.abs(yDiff) > yRotSpeed) {
                this.yRot += Math.signum(yDiff) * yRotSpeed;
            } else {
                this.yRot = this.aimYRot;
            }
        }
    }
}
