package org.ywzj.vehicle.vehicle;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.collision.ChunkCollisionCache;
import org.ywzj.vehicle.vehicle.collision.SectionSource;
import org.ywzj.vehicle.vehicle.structure.OBB;

import java.util.List;

/**
 * The working pose a physics solve runs against. Thread-confined by construction: filled on the
 * tick thread before solve submission, owned by the solve until it completes, read by flush
 * after join. Each handoff is a happens-before edge, so fields here need not be volatile.
 */
public final class PhysicsRig {

    // ---------------------------------------------------------------- pose
    public double x, y, z;
    private float xRot, yRot, zRot;
    private Vec3 deltaMovement = Vec3.ZERO;
    private boolean onGround;

    // ---------------------------------------------------------------- geometry
    /** Working copy of the main hull's OBB. Substeps translate its center; nothing rotates it. */
    public final OBB hull = new OBB(new Vector3f(), new Vector3f(), new Quaternionf());
    private Vector3f[] axes;

    // ---------------------------------------------------------------- world view
    /** Sections this solve may read: the live cache on-thread, a pinned view off it. */
    public SectionSource sections;
    /** Cursor over sections; what the engine's snapshot reads go through. */
    public ChunkCollisionCache.Cursor cursor;

    // ---------------------------------------------------------------- travel plan and record
    /** The movement the substep loop slices, already clamped to the sweep budget. */
    public Vec3 travelMovement = Vec3.ZERO;
    public boolean swept;
    public int substeps = 1;
    /** Entities to carry between substeps, gathered on the tick thread. */
    public List<Entity> carried = List.of();
    /**
     * Displacement each substep actually took, as x/y/z triples. The flush replays these against
     * the real OBBs so per-part poses and the support pushes see exactly the intermediate states
     * the old inline loop produced.
     */
    public final DoubleArrayList substepMoves = new DoubleArrayList();

    // ---------------------------------------------------------------- deferred side effects
    /** Times the pipeline asked for a client pos/rot broadcast; replayed at flush. */
    public int posRotUpdates;
    /** Speed the tick cost, when it crossed the damage threshold. Applied at flush. */
    public double impactVelocityDiff;
    /**
     * Set when the solve needed world data outside its pinned view. The barrier answers with a
     * synchronous re-solve.
     */
    public boolean needsLiveWorld;

    // ---------------------------------------------------------------- rotYXZ cache
    private final Quaternionf rotCache = new Quaternionf();
    private float rotCacheYaw = Float.NaN;
    private float rotCachePitch = Float.NaN;
    private float rotCacheRoll = Float.NaN;

    // ---------------------------------------------------------------- bounds
    private AABB baseBounds;
    private double baseX, baseY, baseZ;

    /** Captures the vehicle's current pose, velocity, hull and bound. Tick thread only. */
    public void capturePose(AbstractVehicle vehicle) {
        this.x = vehicle.getX();
        this.y = vehicle.getY();
        this.z = vehicle.getZ();
        this.xRot = vehicle.getXRot();
        this.yRot = vehicle.getYRot();
        this.zRot = vehicle.getZRot();
        this.deltaMovement = vehicle.getDeltaMovement();
        this.onGround = vehicle.onGround();
        // Client vehicle may tick before hull data arrives; zero hull is safe for that case.
        if (vehicle.getMainCubeOBB() != null) {
            OBB live = vehicle.getMainCubeOBB().obb();
            hull.center().set(live.center());
            hull.extents().set(live.extents());
            hull.rotation().set(live.rotation());
        } else {
            hull.center().zero();
            hull.extents().zero();
            hull.rotation().identity();
        }
        this.axes = hull.getAxes();
        this.baseBounds = vehicle.getBoundingBox();
        this.baseX = x;
        this.baseY = y;
        this.baseZ = z;
        this.substepMoves.clear();
        this.posRotUpdates = 0;
        this.impactVelocityDiff = 0;
        this.needsLiveWorld = false;
        this.rotCacheYaw = Float.NaN;
        this.rotCachePitch = Float.NaN;
        this.rotCacheRoll = Float.NaN;
    }

    /** Points the rig's world reads at a source and gives it a fresh cursor over it. */
    public void bindWorld(SectionSource sections) {
        this.sections = sections;
        this.cursor = ChunkCollisionCache.cursorOver(sections);
    }

    // ---------------------------------------------------------------- pose access

    public Vec3 position() {
        return new Vec3(x, y, z);
    }

    /** Pose write for climb lift and pivot rotation; hull untouched to match pre-move pose. */
    public void setPos(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void setPos(Vec3 pos) {
        setPos(pos.x, pos.y, pos.z);
    }

    /** Travel substep: moves pose and hull together and records the displacement. */
    public void move(double dx, double dy, double dz) {
        this.x += dx;
        this.y += dy;
        this.z += dz;
        hull.center().add((float) dx, (float) dy, (float) dz);
        substepMoves.add(dx);
        substepMoves.add(dy);
        substepMoves.add(dz);
    }

    public float getXRot() {
        return xRot;
    }

    public void setXRot(float rot) {
        this.xRot = rot;
    }

    public float getYRot() {
        return yRot;
    }

    public void setYRot(float rot) {
        this.yRot = rot;
    }

    public float getZRot() {
        return zRot;
    }

    public void setZRot(float rot) {
        this.zRot = rot;
    }

    public Vec3 getDeltaMovement() {
        return deltaMovement;
    }

    public void setDeltaMovement(Vec3 movement) {
        this.deltaMovement = movement;
    }

    public boolean onGround() {
        return onGround;
    }

    public void setOnGround(boolean onGround) {
        this.onGround = onGround;
    }

    public Vector3f[] axes() {
        return axes;
    }

    /** The vehicle's bound at the rig's current pose, translated by how far the pose has moved. */
    public AABB bounds() {
        double dx = x - baseX;
        double dy = y - baseY;
        double dz = z - baseZ;
        if (dx == 0 && dy == 0 && dz == 0) {
            return baseBounds;
        }
        return baseBounds.move(dx, dy, dz);
    }

    /** Composed rotation at the rig's pose, matching the entity's rotation exactly. */
    public Quaternionf rotYXZ() {
        if (yRot != rotCacheYaw || xRot != rotCachePitch || zRot != rotCacheRoll) {
            rotCache.identity()
                    .rotateY(org.joml.Math.toRadians(-yRot))
                    .rotateX(org.joml.Math.toRadians(xRot))
                    .rotateZ(org.joml.Math.toRadians(zRot));
            rotCacheYaw = yRot;
            rotCachePitch = xRot;
            rotCacheRoll = zRot;
        }
        return new Quaternionf(rotCache);
    }

}
