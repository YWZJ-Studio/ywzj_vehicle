package org.ywzj.vehicle.vehicle.parenting;

import net.minecraft.world.entity.Entity;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

/**
 * An entity's parent frame on a vehicle.
 * Stores the entity's position in the vehicle's local frame, updated every tick from the vehicle's pose.
 */
public final class DeckAttachment {

    /** The vehicle this entity is standing on; always verify with VehicleParenting.isUsable. */
    private AbstractVehicle vehicle;

    /** Entity's feet position in the vehicle's local frame; stored as double for precision. */
    private final Vector3d localPos = new Vector3d();

    /** True after first capture; prevents garbage pose on fresh attachments. */
    private boolean localValid;

    /** Ticks since last deck support; hysteresis counter to survive brief air time during jumps. */
    private int airTicks = Integer.MAX_VALUE;

    /** Set by collision clip when a downward move hits this vehicle's geometry. */
    private boolean supportedThisTick;

    /** Set by vehicle scan when entity is found; cleared after processing. */
    private boolean seen;

    /** Number of consecutive scans where this entity was not found. */
    private int missTicks;

    /** Last platform displacement applied (velocity plus rotational component); kept on detach. */
    private double lastCarryX;
    private double lastCarryY;
    private double lastCarryZ;

    /** Last resolved world position and yaw; carry is applied as delta against these values. */
    private double carriedWorldX;
    private double carriedWorldY;
    private double carriedWorldZ;
    private float carriedYaw;

    /** Last game tick when entity was updated; pending carry tick waiting to apply. */
    private long lastEntityTick = Long.MIN_VALUE;
    private long pendingCarryTick = Long.MIN_VALUE;

    /** Scratch buffers for frame conversions during collision; allocated once per attachment. */
    final Vector3f scratchCentre = new Vector3f();
    final Vector3f scratchMove = new Vector3f();
    final Vector3f scratchGrounded = new Vector3f();
    final Vector3d scratchWorld = new Vector3d();
    final double[] scratchOut = new double[3];
    private float[] scratchNear = new float[8 * 6];

    /** Buffer for compacting boxes; grown as needed for the hull. */
    float[] nearBuffer(int boxCount) {
        if (scratchNear.length < boxCount * 6) {
            scratchNear = new float[boxCount * 6];
        }
        return scratchNear;
    }

    public DeckAttachment(AbstractVehicle vehicle) {
        this.vehicle = vehicle;
    }

    public AbstractVehicle vehicle() {
        return vehicle;
    }

    public void setVehicle(AbstractVehicle vehicle) {
        if (this.vehicle != vehicle) {
            this.vehicle = vehicle;
            this.localValid = false;
            this.airTicks = Integer.MAX_VALUE;
            setLastCarry(0, 0, 0);
        }
        this.missTicks = 0;
    }

    /** Entity's feet position in vehicle-local space; read-only, updated by capture. */
    public Vector3d localPos() {
        return localPos;
    }

    public boolean localValid() {
        return localValid;
    }

    /** Re-derive local position from entity's world position at snapshot pose. */
    public void capture(Entity entity, DeckSnapshot deck) {
        VehicleParenting.toLocal(deck, entity.getX(), entity.getY(), entity.getZ(), localPos);
        setCarried(entity.getX(), entity.getY(), entity.getZ(), deck.yaw());
        localValid = true;
    }

    public double carriedWorldX() {
        return carriedWorldX;
    }

    public double carriedWorldY() {
        return carriedWorldY;
    }

    public double carriedWorldZ() {
        return carriedWorldZ;
    }

    public float carriedYaw() {
        return carriedYaw;
    }

    /** Record resolved position and yaw for delta calculation on next apply. */
    public void setCarried(double x, double y, double z, float yaw) {
        this.carriedWorldX = x;
        this.carriedWorldY = y;
        this.carriedWorldZ = z;
        this.carriedYaw = yaw;
    }

    public long lastEntityTick() {
        return lastEntityTick;
    }

    public void setLastEntityTick(long gameTime) {
        this.lastEntityTick = gameTime;
    }

    public long pendingCarryTick() {
        return pendingCarryTick;
    }

    public void setPendingCarryTick(long gameTime) {
        this.pendingCarryTick = gameTime;
    }

    public void clearPendingCarry() {
        this.pendingCarryTick = Long.MIN_VALUE;
    }

    public int airTicks() {
        return airTicks;
    }

    /** Whether entity is close enough to deck to be carried by it. */
    public boolean gripped() {
        return localValid && airTicks <= VehicleParenting.GRIP_GRACE_TICKS;
    }

    public void markSupported() {
        supportedThisTick = true;
    }

    /** Update airborne counter based on this tick's support; reset support flag. */
    public void advanceSupport() {
        if (supportedThisTick) {
            airTicks = 0;
            supportedThisTick = false;
        } else if (airTicks != Integer.MAX_VALUE) {
            airTicks++;
        }
    }

    /** Establish grip immediately on attachment without waiting for collision detection. */
    public void forceGrip() {
        airTicks = 0;
    }

    public boolean seen() {
        return seen;
    }

    public void markSeen() {
        this.seen = true;
        this.missTicks = 0;
    }

    public void clearSeen() {
        this.seen = false;
    }

    public int incrementMissTicks() {
        return ++missTicks;
    }

    public double lastCarryX() {
        return lastCarryX;
    }

    public double lastCarryY() {
        return lastCarryY;
    }

    public double lastCarryZ() {
        return lastCarryZ;
    }

    /** Squared speed of last platform carry, in blocks per tick. */
    public double lastCarrySpeedSqr() {
        return lastCarryX * lastCarryX + lastCarryY * lastCarryY + lastCarryZ * lastCarryZ;
    }

    public void setLastCarry(double x, double y, double z) {
        this.lastCarryX = x;
        this.lastCarryY = y;
        this.lastCarryZ = z;
    }

    // ---------------------------------------------------------------- surface effects

    /** Distance walked since last footfall; queued for sound playback on tick thread. */
    private double walkDistance;
    private boolean fallSoundPending;

    void addWalk(double distance) {
        walkDistance += distance;
    }

    void queueFallSound() {
        fallSoundPending = true;
    }

    /** Consume one footfall interval if enough distance has been walked. */
    boolean takeStep(double interval) {
        if (walkDistance < interval) {
            return false;
        }
        // Never more than one step per tick: a rider flung along a deck should not machine-gun
        // footsteps, and the leftover is worth nothing once the interval has passed twice.
        walkDistance = walkDistance > interval * 2 ? 0 : walkDistance - interval;
        return true;
    }

    boolean takeFallSound() {
        boolean pending = fallSoundPending;
        fallSoundPending = false;
        return pending;
    }

    /** Clear pending sound effects for discarded attachments. */
    void clearPendingSounds() {
        walkDistance = 0;
        fallSoundPending = false;
    }

}
