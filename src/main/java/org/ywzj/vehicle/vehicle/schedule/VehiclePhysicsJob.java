package org.ywzj.vehicle.vehicle.schedule;

import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * One vehicle's physics solve for one tick: submitted at the vehicle's tick, joined and applied
 * at the level's end-of-tick barrier.
 */
public final class VehiclePhysicsJob {

    public final AbstractVehicle vehicle;
    public final Vec3 force;

    private final double posX, posY, posZ;
    private final float xRot, yRot, zRot;
    private final Vec3 velocity;
    private final boolean onGround;

    /** Set when the solve threw or was interrupted. */
    public volatile boolean failed;

    Future<?> future;

    public VehiclePhysicsJob(AbstractVehicle vehicle, Vec3 force) {
        this.vehicle = vehicle;
        this.force = force;
        this.posX = vehicle.getX();
        this.posY = vehicle.getY();
        this.posZ = vehicle.getZ();
        this.xRot = vehicle.getXRot();
        this.yRot = vehicle.getYRot();
        this.zRot = vehicle.getZRot();
        this.velocity = vehicle.getDeltaMovement();
        this.onGround = vehicle.onGround();
    }

    void solve() {
        try {
            vehicle.runPhysicsSolve(this);
        } catch (Throwable t) {
            failed = true;
            YwzjVehicle.LOGGER.error("Async physics solve failed for {}; re-solving on the tick"
                    + " thread this tick", vehicle, t);
        }
    }

    /**
     * Blocks until the solve is done. A null future means the pool rejected the submit, so the
     * solve runs here synchronously.
     */
    void await() {
        if (future == null) {
            solve();
            return;
        }
        try {
            future.get();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            failed = true;
        } catch (ExecutionException wrapped) {
            failed = true;
        }
    }

    /** Whether anything outside physics wrote the vehicle's state between submit and now. */
    public boolean interfered() {
        Vec3 movement = vehicle.getDeltaMovement();
        return vehicle.getX() != posX || vehicle.getY() != posY || vehicle.getZ() != posZ
                || vehicle.getXRot() != xRot || vehicle.getYRot() != yRot
                || vehicle.getZRot() != zRot
                || movement.x != velocity.x || movement.y != velocity.y
                || movement.z != velocity.z
                || vehicle.onGround() != onGround;
    }

}
