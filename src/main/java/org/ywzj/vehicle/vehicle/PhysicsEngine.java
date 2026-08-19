package org.ywzj.vehicle.vehicle;

import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Math;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.ywzj.vehicle.all.AllConfigs;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.util.VectorUtil;
import org.ywzj.vehicle.vehicle.collision.BoxBuffer;
import org.ywzj.vehicle.vehicle.collision.ChunkCollisionCache;
import org.ywzj.vehicle.vehicle.collision.SweptHull;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;
import org.ywzj.vehicle.vehicle.pojo.PhysicsInfo;
import org.ywzj.vehicle.vehicle.structure.OBB;
import org.ywzj.vehicle.vehicle.structure.VehicleCubeOBB;

import java.util.*;
import java.util.function.Function;

public class PhysicsEngine {

    public static final double MAGIC_NUMBER = .943;
    public static float G = 9.8f / 400;
    public final AbstractVehicle vehicle;
    public float radarCrossSection;
    public float mass = 1;
    public Vec3 center;
    public float bounce = 0.05f;
    public float angularDampingGround = 0.88f;
    public float angularDampingAir = 0.96f;
    public float torqueScale = 4.0f;
    public float maxRotV = 0.3f;
    public float maxTipSpeed = 3.6f;
    /** Whether this vehicle's physics data allows it to tumble. */
    public boolean canTumble = true;
    /** Per-tick angular damping while airborne and tumbling. */
    public float tumbleAirDamping = 0.995f;
    /**
     * Scales the spin an impact imparts while tumbling. 1 is the impulse the contacts applied,
     * 0 is purely linear.
     */
    public float tumbleImpactScale = 1.0f;
    /** Angular speed about the current pivot axis, a derived view of angularVelocity. */
    public float rotV = 0;

    /** World-frame angular velocity; the actual rotational state, containing rotations about all axes. */
    public final Vector3f angularVelocity = new Vector3f();

    /** Inverse inertia tensor in body axes. Diagonal for a box shape. Rebuilt when the hull changes. */
    private final Vector3f invInertiaBody = new Vector3f();
    private final Matrix3f invInertiaWorld = new Matrix3f();
    private final Matrix3f inertiaScratch = new Matrix3f();
    private final Vector3f axisScratch = new Vector3f();

    /** Angular impulse per unit mass from this tick's contacts, in body axes; filled by motionByImpact. */
    private final Vector3f contactSpin = new Vector3f();
    /**
     * Per-face impact gather: largest velocity component cancelled, sum of contact positions,
     * and count. Six faces, cleared per tick.
     */
    private final float[] faceSpeed = new float[6];
    private final float[] faceCentroid = new float[18];
    private final int[] faceCount = new int[6];
    /**
     * Body axis each face's normal lies along, indexed by face ordinal:
     * FRONT, BACK, LEFT, RIGHT, TOP, BOTTOM.
     */
    private static final int[] FACE_AXIS = {2, 2, 0, 0, 1, 1};
    /** Tumble scratch: spin axis, offset, Euler angles, and pivot in world space. */
    private final Vector3f tumbleAxis = new Vector3f();
    private final Vector3f tumbleRel = new Vector3f();
    private final Vector3f tumbleEuler = new Vector3f();
    private final Vector3f tumblePivot = new Vector3f();
    private final Vector3f tumbleCentroid = new Vector3f();
    private final Quaternionf tumblePose = new Quaternionf();
    public int rotTick;
    public Quaternionf stepRot;
    public Vector3f localRotAxisStart;
    public Vector3f localRotAxisStartO;
    public Vector3f localRotAxisEnd;
    public Vector3f localRotAxisEndO;
    public Vector3f localRotAxisVec;
    public Vector3f planeSupport;
    public Vector3f planeU;
    public Vector3f planeV;
    public float friction = 0.005f;
    public float destroyExplosionVelocity = 0.8f;
    /**
     * Blocks of rise allowed per block of horizontal travel, the steepest slope the vehicle can
     * drive up. 1.0 is 45 degrees.
     */
    public float climbGradient = 1.0f;
    public Vector3f velocity = new Vector3f(0, 0, 0);
    public Vector3f velocityO = new Vector3f(0, 0, 0);
    /** Block cells in contact per tick needed to trigger block breaking. */
    private static final int STUCK_DESTROY_THRESHOLD = 10;
    /** Cap on the upward velocity given to a hull that has sunk into geometry. */
    private static final double MAX_SUPPORT_LIFT = 0.1;
    /** Rise below which climb does nothing; slightly over one tick of gravity. */
    private static final double CLIMB_DEADBAND = 0.03;
    /** Nose-up pitch in degrees past which climbing is refused as unphysical. */
    private static final float MAX_CLIMB_PITCH = -60.0f;
    /** Per-tick factor the attitude auto-correct keeps to settle level smoothly. */
    private static final float AUTO_LEVEL_EASE = 0.7f;
    /** Degrees under which the ease snaps to exactly level. */
    private static final float AUTO_LEVEL_SNAP = 0.1f;
    /**
     * Speed a contact must cancel to be credited with spin while tumbling. Filters out gravity
     * on idle contacts.
     */
    private static final double TUMBLE_IMPULSE_DEADBAND = 0.08;
    /** Angular speed below which a supported tumbling hull is stopped exactly. */
    private static final float TUMBLE_REST_SPIN = 0.004f;
    private final OBB climbHull = new OBB(new Vector3f(), new Vector3f(), new Quaternionf());
    /** SAT precomputation for headroom cast, reused per call. */
    private final OBB.SatFrame castFrame = new OBB.SatFrame();
    /** Support-polygon scratch; 2D projections, sort and hull indices, and 3D points. */
    private float[] planeXs = new float[64];
    private float[] planeYs = new float[64];
    private int[] hullSorted = new int[64];
    private int[] hullOut = new int[129];
    private final List<Vector3f> forcePoints = new ArrayList<>();
    public boolean lockZRot;
    public boolean lockCenterRot;
    public boolean canDestroyBlock;
    public int stuckTick;

    /** Hardness at or above which the vehicle cannot break the block. */
    private static final float UNBREAKABLE_HARDNESS = 20.0F;
    private final LongArrayList pendingBreaks = new LongArrayList();
    /** Cells already queued this tick to prevent duplicates from overlapping break passes. */
    private final LongOpenHashSet queuedCells = new LongOpenHashSet();
    /** Cells that blocked the hull this tick, deduplicated. Reused. */
    private final LongOpenHashSet blockingCells = new LongOpenHashSet();
    /** Cell to jammed-face ordinal for the grind pass. Reused. */
    private final Long2ByteOpenHashMap grindFaces = new Long2ByteOpenHashMap();
    /** Cursor into the break loop to avoid allocation per cell. */
    private final BlockPos.MutableBlockPos breakCursor = new BlockPos.MutableBlockPos();

    public PhysicsEngine(AbstractVehicle vehicle) {
        this.vehicle = vehicle;
    }


    /** Rebuilds the inverse inertia tensor from the hull's box dimensions and mass. */
    public void refreshInertia(PhysicsRig rig) {
        VehicleCubeOBB cube = vehicle.getMainCubeOBB();
        if (cube == null || mass <= 0) {
            invInertiaBody.set(0, 0, 0);
            invInertiaWorld.zero();
            return;
        }
        float w = (float) cube.getWidth();
        float h = (float) cube.getHeight();
        float d = (float) cube.getDepth();
        float k = mass / 12.0f;
        float ix = k * (h * h + d * d);
        float iy = k * (w * w + d * d);
        float iz = k * (w * w + h * h);
        invInertiaBody.set(ix > 1.0e-6f ? 1 / ix : 0, iy > 1.0e-6f ? 1 / iy : 0,
                iz > 1.0e-6f ? 1 / iz : 0);
        // I_world^-1 = R * I_body^-1 * R^T
        Matrix3f r = rig.rotYXZ().get(inertiaScratch);
        invInertiaWorld.set(r);
        invInertiaWorld.scale(invInertiaBody.x, invInertiaBody.y, invInertiaBody.z);
        invInertiaWorld.mul(r.transpose());
    }

    /** Returns the world-space direction of the edge the hull is pivoting on, or null if none. */
    private Vector3f pivotAxisWorld(PhysicsRig rig, Vector3f[] axes, Vector3f dest) {
        if (localRotAxisStart == null || localRotAxisEnd == null) {
            return null;
        }
        Vector3f start = rig.hull.localToWorld(localRotAxisStart, axes);
        Vector3f end = rig.hull.localToWorld(localRotAxisEnd, axes);
        dest.set(end).sub(start);
        return dest.lengthSquared() < 1.0e-9f ? null : dest.normalize();
    }

    /** Writes an angular speed about the pivot edge into the vector state and mirrors it to rotV. */
    private void setPivotSpin(PhysicsRig rig, Vector3f[] axes, float speed) {
        Vector3f axis = pivotAxisWorld(rig, axes, axisScratch);
        if (axis == null) {
            angularVelocity.zero();
        } else {
            angularVelocity.set(axis).mul(speed);
        }
        rotV = speed;
    }

    /** Damps both angular velocity vector and rotV scalar; stops exactly when very small. */
    private void dampSpin(float factor) {
        angularVelocity.mul(factor);
        if (angularVelocity.lengthSquared() < 1.0e-6f) {
            angularVelocity.zero();
        }
        rotV *= factor;
        if (Math.abs(rotV) < 0.001f) {
            rotV = 0;
        }
    }

    private void clearSpin() {
        angularVelocity.zero();
        rotV = 0;
    }

    /** Returns true if the hull may tumble freely. */
    public boolean tumbles() {
        return AllConfigs.Cached.tumbling && canTumble && !lockCenterRot;
    }

    /** Holds a tumble to the tip-speed ceiling. */
    private void clampSpin() {
        float max = effectiveMaxRotV();
        float speedSq = angularVelocity.lengthSquared();
        if (speedSq > max * max && speedSq > 0) {
            angularVelocity.mul(max / Math.sqrt(speedSq));
        }
    }

    /** Turns this tick's accumulated contact impulses into spin. */
    private void applyContactSpin(PhysicsRig rig) {
        if (contactSpin.lengthSquared() < 1.0e-12f || tumbleImpactScale == 0) {
            contactSpin.zero();
            return;
        }
        tumbleRel.set(contactSpin)
                .mul(mass * tumbleImpactScale)
                .mul(invInertiaBody);
        rig.hull.rotation().transform(tumbleRel);
        angularVelocity.add(tumbleRel);
        clampSpin();
        contactSpin.zero();
    }

    /** Spins a tumbling hull about its centre of mass. */
    private void tumbleFreeRot(PhysicsRig rig, Vector3f[] axes, Vector3f comLocal) {
        rig.hull.localToWorld(comLocal, axes, tumblePivot);
        tumbleAbout(rig, tumblePivot);
    }

    /** Spins a tumbling hull about the centre of the patch it is standing on. */
    private void tumbleSupportRot(PhysicsRig rig, Vector3f[] axes, int pointCount) {
        if (pointCount <= 0) {
            return;
        }
        tumbleCentroid.zero();
        for (int i = 0; i < pointCount; i++) {
            tumbleCentroid.add(forcePoints.get(i));
        }
        tumbleCentroid.div(pointCount);
        rig.hull.localToWorld(tumbleCentroid, axes, tumblePivot);
        tumbleAbout(rig, tumblePivot);
    }

    /** Spins a tumbling hull about the edge it is standing on. */
    private void tumbleEdgeRot(PhysicsRig rig, Vector3f[] axes) {
        if (localRotAxisStart == null || localRotAxisEnd == null) {
            return;
        }
        rig.hull.localToWorld(localRotAxisStart, axes, tumbleRel);
        rig.hull.localToWorld(localRotAxisEnd, axes, tumblePivot);
        tumblePivot.add(tumbleRel).mul(0.5f);
        tumbleAbout(rig, tumblePivot);
    }

    /** Updates hull attitude by rotating about pivotWorld by one tick of angular velocity. */
    private void tumbleAbout(PhysicsRig rig, Vector3f pivotWorld) {
        float speed = angularVelocity.length();
        if (speed < 1.0e-5f) {
            rotV = 0;
            return;
        }
        tumbleAxis.set(angularVelocity).div(speed);
        // Replaced rather than mutated: captureState holds this reference for the async rewind,
        // on the promise that these fields are only ever reassigned.
        stepRot = new Quaternionf().fromAxisAngleRad(tumbleAxis, speed);
        tumblePose.set(stepRot).mul(rig.rotYXZ());
        tumblePose.getEulerAnglesYXZ(tumbleEuler);
        if (!Float.isFinite(tumbleEuler.x) || !Float.isFinite(tumbleEuler.y)
                || !Float.isFinite(tumbleEuler.z)) {
            return;
        }
        // The arm from the pivot is taken in doubles and only then narrowed, so the rounding is
        // the pivot's own float precision rather than that of the vehicle's world coordinate.
        tumbleRel.set((float) (rig.x - pivotWorld.x),
                (float) (rig.y - pivotWorld.y),
                (float) (rig.z - pivotWorld.z));
        stepRot.transform(tumbleRel);
        rotTick = 10;
        double beforeY = rig.y;
        rig.setPos(pivotWorld.x + (double) tumbleRel.x,
                pivotWorld.y + (double) tumbleRel.y,
                pivotWorld.z + (double) tumbleRel.z);
        trace(PhysicsTrace.Source.ROTATION, rig.y - beforeY);
        rig.setYRot(-(float) Math.toDegrees(tumbleEuler.y));
        rig.setXRot((float) Math.toDegrees(tumbleEuler.x));
        // Honoured here as in rot(): a vehicle whose data says it does not roll does not roll,
        // tumbling or not. That is an authored property of the hull, not one of the safeguards
        // the toggle exists to remove.
        rig.setZRot(lockZRot ? 0 : (float) Math.toDegrees(tumbleEuler.z));
        // Keep the scalar view current. It is not decoration: collisionSubsteps sizes the tick's
        // movement slices from it, so a fast tumble has to be visible there or the hull's corner
        // steps through thin geometry.
        rotV = speed;
    }

    public VehicleCubeOBB physicsCube() {
        return vehicle.getMainCubeOBB();
    }

    /** Credits a vertical change to its source for trace accounting. */
    private void trace(PhysicsTrace.Source source, double amount) {
        PhysicsTrace trace = vehicle.physicsTrace();
        if (trace != null) {
            trace.add(source, amount);
        }
    }


    public float effectiveMaxRotV() {
        float radius = vehicle.getMainCubeOBB().obb().extents().length();
        if (radius < 1.0e-3f) {
            return maxRotV;
        }
        return Math.min(maxRotV, maxTipSpeed / radius);
    }

    /**
     * 载具的正朝向约定为自身Z轴正方向
     * 车体视作理想刚体，采样点受方块的力垂直于OBB面向内
     * 方块作用力将完全抵消载具速度在力反方向上的分速度
     * 追加一个模拟撞击力导致的力方向上的微小速度
     * 为助于攀爬方块，一定车体高度下的方块碰撞会被忽略
     * 车体底面若有陷地则会施加较大的向上速度
     */
    public Vec3 motionByImpact(PhysicsRig rig, List<VehicleCubeOBB.CubePoint> touchPoints, Vector3f[] axes, Vec3 velocity) {
        VehicleCubeOBB physicsCube = vehicle.getMainCubeOBB();
        boolean isStuck = false;
        // Jammed faces as a bitmask over CubeFace ordinals, collected by contact area not point count.
        int stuckFaces = 0;
        // Break passes reuse these sets; cleared each tick.
        blockingCells.clear();
        queuedCells.clear();
        // Whether the hull is buried and needs upward lift.
        boolean embedded = false;
        double climbSkirt = physicsCube.rideSkirt(vehicle.maxUpStep());
        PhysicsTrace trace = vehicle.physicsTrace();
        double tracedVelocityY = velocity.y;
        int bottomContacts = 0;
        int blockingContacts = 0;
        if (trace != null) {
            trace.mark();
        }

        // Loop on components to avoid Vec3 allocation; axes normalized in doubles.
        double vx = velocity.x;
        double vy = velocity.y;
        double vz = velocity.z;
        double velocityO = Math.sqrt(vx * vx + vy * vy + vz * vz);
        double ax0 = axes[0].x, ay0 = axes[0].y, az0 = axes[0].z;
        double ax1 = axes[1].x, ay1 = axes[1].y, az1 = axes[1].z;
        double ax2 = axes[2].x, ay2 = axes[2].y, az2 = axes[2].z;
        double inv = 1.0 / Math.sqrt(ax0 * ax0 + ay0 * ay0 + az0 * az0);
        ax0 *= inv; ay0 *= inv; az0 *= inv;
        inv = 1.0 / Math.sqrt(ax1 * ax1 + ay1 * ay1 + az1 * az1);
        ax1 *= inv; ay1 *= inv; az1 *= inv;
        inv = 1.0 / Math.sqrt(ax2 * ax2 + ay2 * ay2 + az2 * az2);
        ax2 *= inv; ay2 *= inv; az2 *= inv;
        // Snapshot cursor for block state reads; avoids tick-thread coupling.
        ChunkCollisionCache.Cursor cursor = rig.cursor;
        // Largest component a blocking side contact cancelled; feeds ram break budget.
        double ramSpeed = 0;
        // Per-face impact gather, not per-contact, to match impulse to contact centroid.
        boolean tumbling = tumbles();
        contactSpin.zero();
        if (tumbling) {
            Arrays.fill(faceSpeed, 0f);
            Arrays.fill(faceCentroid, 0f);
            Arrays.fill(faceCount, 0);
        }
        for (int i = 0, size = touchPoints.size(); i < size; i++) {
            VehicleCubeOBB.CubePoint touchPoint = touchPoints.get(i);
            VehicleCubeOBB.CubeFace face = touchPoint.cubeFace();
            if (face == VehicleCubeOBB.CubeFace.LEFT || face == VehicleCubeOBB.CubeFace.RIGHT) {
                if (touchPoint.obbLocalPos().y < climbSkirt) {
                    continue;
                }
                blockingContacts++;
                if (tumbling) {
                    noteFaceContact(face, touchPoint);
                }
                double d = vx * ax0 + vy * ay0 + vz * az0;
                boolean blocking = face == VehicleCubeOBB.CubeFace.LEFT ? d > 0 : d < 0;
                if (blocking) {
                    ramSpeed = Math.max(ramSpeed, Math.abs(d));
                    vx -= d * ax0; vy -= d * ay0; vz -= d * az0;
                    isStuck = true;
                    stuckFaces |= 1 << face.ordinal();
                    recordBlockingCell(touchPoint);
                    if (tumbling) {
                        noteFaceImpulse(face, d);
                    }
                } else {
                    double push = face == VehicleCubeOBB.CubeFace.LEFT ? d + bounce : d - bounce;
                    vx -= push * ax0; vy -= push * ay0; vz -= push * az0;
                }
            } else if (face == VehicleCubeOBB.CubeFace.FRONT || face == VehicleCubeOBB.CubeFace.BACK) {
                if (touchPoint.obbLocalPos().y < climbSkirt) {
                    continue;
                }
                blockingContacts++;
                if (tumbling) {
                    noteFaceContact(face, touchPoint);
                }
                double d = vx * ax2 + vy * ay2 + vz * az2;
                boolean blocking = face == VehicleCubeOBB.CubeFace.FRONT ? d > 0 : d < 0;
                if (blocking) {
                    ramSpeed = Math.max(ramSpeed, Math.abs(d));
                    vx -= d * ax2; vy -= d * ay2; vz -= d * az2;
                    isStuck = true;
                    stuckFaces |= 1 << face.ordinal();
                    recordBlockingCell(touchPoint);
                    if (tumbling) {
                        noteFaceImpulse(face, d);
                    }
                } else {
                    double push = face == VehicleCubeOBB.CubeFace.FRONT ? d + bounce : d - bounce;
                    vx -= push * ax2; vy -= push * ay2; vz -= push * az2;
                }
            } else if (face == VehicleCubeOBB.CubeFace.TOP || face == VehicleCubeOBB.CubeFace.BOTTOM) {
                if (vy > -0.1 && touchPoint.obbLocalPos().y < -physicsCube.obb().extents().y - 0.01) {
                    continue;
                }
                if (tumbling) {
                    noteFaceContact(face, touchPoint);
                }
                double d = vx * ax1 + vy * ay1 + vz * az1;
                if (face == VehicleCubeOBB.CubeFace.TOP) {
                    if (d > 0) {
                        vx -= d * ax1; vy -= d * ay1; vz -= d * az1;
                        if (tumbling) {
                            noteFaceImpulse(face, d);
                        }
                    } else {
                        double push = d + bounce;
                        vx -= push * ax1; vy -= push * ay1; vz -= push * az1;
                    }
                } else {
                    bottomContacts++;
                    if (d < 0) {
                        vx -= d * ax1; vy -= d * ay1; vz -= d * az1;
                        if (tumbling) {
                            noteFaceImpulse(face, d);
                        }
                    }
                    if (!embedded) {
                        float offsetY = (float) (physicsCube().offset().y - physicsCube.height / 2);
                        Vector3f cachedWorldPos = touchPoint.cachedWorldPos();
                        double testY = cachedWorldPos.y + 0.1f + offsetY;
                        int blockX = Mth.floor(cachedWorldPos.x);
                        int blockY = Mth.floor(testY);
                        int blockZ = Mth.floor(cachedWorldPos.z);
                        BlockState blockState =
                                cursor == null ? null : cursor.collisionAt(blockX, blockY, blockZ);
                        if (blockState != null && blockState.isSolid()
                                && (!isHalfBlock(touchPoint.cubePointContext.blockState())
                                    || testY < blockY + 0.55)) {
                            embedded = true;
                        }
                    }
                }
            }
        }
        if (tumbling) {
            gatherContactSpin();
        }
        if (embedded) {
            // Lift hull out once, sized by how far over it is.
            double tilt = Math.acos(Mth.clamp((float) ay1, -1, 1));
            double peakTilt = Math.toRadians(15);
            double zeroTilt = Math.toRadians(30);
            double tiltRatio = tilt <= peakTilt
                    ? tilt / peakTilt
                    : Math.max(0, (zeroTilt - tilt) / (zeroTilt - peakTilt));
            double target = Mth.lerp(tiltRatio, 0, MAX_SUPPORT_LIFT);
            double current = vx * ax1 + vy * ay1 + vz * az1;
            if (current < target) {
                double before = vy;
                double lift = target - current;
                vx += lift * ax1; vy += lift * ay1; vz += lift * az1;
                trace(PhysicsTrace.Source.SUPPORT_LIFT, vy - before);
            }
        }
        if (stuckFaces != 0) {
            queueGrindBreaks(cursor, physicsCube, stuckFaces, touchPoints);
        }
        Vector3f centre = rig.hull.center();
        BlockState centreState = cursor == null ? null
                : cursor.collisionAt(Mth.floor(centre.x), Mth.floor(centre.y), Mth.floor(centre.z));
        if (centreState != null && centreState.isSolid()) {
            vy += 0.1;
            trace(PhysicsTrace.Source.CENTRE_KICK, 0.1);
        }
        if (!isStuck) {
            stuckTick = Math.max(stuckTick - 1, 0);
        }
        if (trace != null) {
            // Whatever the contact loop did to height that the two named lifts above did not.
            trace.remainder(PhysicsTrace.Source.IMPACT, vy - tracedVelocityY);
            trace.contacts(touchPoints.size(), bottomContacts, blockingContacts);
        }
        // Replaced, never mutated; velocity and velocityO must remain distinct.
        this.velocity = new Vector3f((float) vx, (float) vy, (float) vz);
        double velocityDiff = velocityO - Math.sqrt(vx * vx + vy * vy + vz * vz);
        // Ram breaking uses blocked speed; impact damage uses total velocity change.
        queueRamBreaks(cursor, ramSpeed);
        if (velocityDiff > 0.5) {
            // Deferred to end-of-tick flush; impact damage cannot run on solve thread.
            rig.impactVelocityDiff = velocityDiff;
        }
        return new Vec3(vx, vy, vz);
    }

    /** Records a contact on face for impact centroid; all contacts on the face count toward it. */
    private void noteFaceContact(VehicleCubeOBB.CubeFace face, VehicleCubeOBB.CubePoint point) {
        int f = face.ordinal();
        faceCount[f]++;
        Vector3f local = point.obbLocalPos();
        faceCentroid[f * 3] += local.x;
        faceCentroid[f * 3 + 1] += local.y;
        faceCentroid[f * 3 + 2] += local.z;
    }

    /** Records the speed a face cancelled; keeps the largest component. */
    private void noteFaceImpulse(VehicleCubeOBB.CubeFace face, double speed) {
        int f = face.ordinal();
        if (Math.abs(speed) > Math.abs(faceSpeed[f])) {
            faceSpeed[f] = (float) speed;
        }
    }

    /** Turns the per-face gather into one angular impulse in body axes. */
    private void gatherContactSpin() {
        double comX = center.x, comY = center.y, comZ = center.z;
        double sx = 0, sy = 0, sz = 0;
        for (int f = 0; f < 6; f++) {
            int count = faceCount[f];
            double d = faceSpeed[f];
            if (count == 0 || Math.abs(d) <= TUMBLE_IMPULSE_DEADBAND) {
                continue;
            }
            double rx = faceCentroid[f * 3] / count - comX;
            double ry = faceCentroid[f * 3 + 1] / count - comY;
            double rz = faceCentroid[f * 3 + 2] / count - comZ;
            // r × J, with J = -d along the face's body axis.
            switch (FACE_AXIS[f]) {
                case 0 -> { sy -= d * rz; sz += d * ry; }
                case 1 -> { sx += d * rz; sz -= d * rx; }
                default -> { sx -= d * ry; sy += d * rx; }
            }
        }
        contactSpin.set((float) sx, (float) sy, (float) sz);
    }

    /** Records a cell that blocked the hull; counts by cell, not sample point. */
    private void recordBlockingCell(VehicleCubeOBB.CubePoint touchPoint) {
        VehicleCubeOBB.CubePointContext context = touchPoint.cubePointContext;
        // Only track world geometry, not provider geometry.
        if (context.hasWorldCell()) {
            blockingCells.add(context.cellPos());
        }
    }

    /** Breaks blocks the vehicle rammed if it has enough momentum. */
    private void queueRamBreaks(ChunkCollisionCache.Cursor cursor, double speedLost) {
        if (blockingCells.isEmpty() || speedLost <= 0 || !breakingEnabled()) {
            return;
        }
        if (cursor == null) {
            return;
        }
        // Momentum = mass * speed; normalizes heavy-slow and light-fast impacts.
        double momentum = mass * speedLost;
        double perHardness = AllConfigs.Cached.ramBreakMomentum;
        // Hardness budget for all blocks in this impact.
        double hardnessBudget = perHardness <= 0 ? Double.MAX_VALUE : momentum / perHardness;
        LongIterator iterator = blockingCells.iterator();
        while (iterator.hasNext()) {
            queueBreak(cursor, iterator.nextLong(), hardnessBudget);
        }
    }

    /**
     * Maximum cells to queue from one refused rotation, avoiding infinite enumeration against
     * merged geometry.
     */
    private static final int MAX_ROTATION_RAM_CELLS = 27;

    /** Rams blocks that blocked a rotation attempt. */
    public void ramByRotation(AABB blocker, double tipSpeed) {
        if (tipSpeed <= 0 || !breakingEnabled()) {
            return;
        }
        ChunkCollisionCache.Cursor cursor = vehicle.collisionCursor();
        if (cursor == null) {
            return;
        }
        double perHardness = AllConfigs.Cached.ramBreakMomentum;
        double budget = perHardness <= 0
                ? Double.MAX_VALUE
                : (mass * tipSpeed) / perHardness;
        if (budget <= 0) {
            return;
        }
        AABB region = blocker.intersect(vehicle.getBoundingBox().inflate(0.1));
        if (region.getXsize() <= 0 || region.getYsize() <= 0 || region.getZsize() <= 0) {
            return;
        }
        int queued = 0;
        for (int y = Mth.floor(region.minY); y <= Mth.floor(region.maxY - 1.0e-6); y++) {
            for (int z = Mth.floor(region.minZ); z <= Mth.floor(region.maxZ - 1.0e-6); z++) {
                for (int x = Mth.floor(region.minX); x <= Mth.floor(region.maxX - 1.0e-6); x++) {
                    if (queued++ >= MAX_ROTATION_RAM_CELLS) {
                        return;
                    }
                    queueBreak(cursor, BlockPos.asLong(x, y, z), budget);
                }
            }
        }
    }

    /**
     * Grinds through blocks a jammed face is pressed against, driven by contacted cells rather
     * than sample points.
     */
    private void queueGrindBreaks(ChunkCollisionCache.Cursor cursor,
                                  VehicleCubeOBB physicsCube,
                                  int stuckFaces,
                                  List<VehicleCubeOBB.CubePoint> touchPoints) {
        grindFaces.clear();
        double climbSkirt = physicsCube.rideSkirt(vehicle.maxUpStep());
        for (int i = 0, size = touchPoints.size(); i < size; i++) {
            VehicleCubeOBB.CubePoint touchPoint = touchPoints.get(i);
            // Strictly below skirt; exact match means blocked and worth grinding.
            if ((stuckFaces & (1 << touchPoint.cubeFace().ordinal())) == 0
                    || touchPoint.obbLocalPos().y < climbSkirt) {
                continue;
            }
            VehicleCubeOBB.CubePointContext context = touchPoint.cubePointContext;
            if (!context.hasWorldCell()) {
                continue;
            }
            grindFaces.putIfAbsent(context.cellPos(), (byte) touchPoint.cubeFace().ordinal());
        }
        if (grindFaces.isEmpty()) {
            return;
        }

        stuckTick += grindFaces.size();
        // Compare against threshold, not exact equality, to avoid silent skips.
        if (stuckTick < STUCK_DESTROY_THRESHOLD) {
            return;
        }
        if (breakingEnabled() && cursor != null) {
            Vector3f[] axes = physicsCube.obb().getAxes();
            LongIterator iterator = grindFaces.keySet().iterator();
            while (iterator.hasNext()) {
                long cell = iterator.nextLong();
                VehicleCubeOBB.CubeFace face = FACES[grindFaces.get(cell)];
                Vector3f faceNormal =
                        face == VehicleCubeOBB.CubeFace.LEFT || face == VehicleCubeOBB.CubeFace.RIGHT
                                ? axes[0] : axes[2];
                boolean normalAlongX = Math.abs(faceNormal.x) >= Math.abs(faceNormal.z);
                int blockX = BlockPos.getX(cell);
                int blockY = BlockPos.getY(cell);
                int blockZ = BlockPos.getZ(cell);
                // 3x3 grid around contact, packed as integer offsets.
                for (int vertical = -1; vertical <= 1; vertical++) {
                    for (int horizontal = -1; horizontal <= 1; horizontal++) {
                        queueBreak(cursor,
                                BlockPos.asLong(
                                        blockX + (normalAlongX ? 0 : horizontal),
                                        blockY + vertical,
                                        blockZ + (normalAlongX ? horizontal : 0)),
                                // No momentum in a jam; break anything soft.
                                Double.MAX_VALUE);
                    }
                }
            }
        }
        // Keep just below threshold to match old digging cadence.
        stuckTick = STUCK_DESTROY_THRESHOLD - 2;
    }

    /** Face ordinals cached to avoid allocation in grind pass. */
    private static final VehicleCubeOBB.CubeFace[] FACES = VehicleCubeOBB.CubeFace.values();

    private boolean breakingEnabled() {
        return canDestroyBlock && AllConfigs.Cached.canDestroyBlock;
    }

    /** Queues one cell for breaking if it exists, is soft enough, and not already queued. */
    private void queueBreak(ChunkCollisionCache.Cursor cursor, long cell, double hardnessBudget) {
        if (!queuedCells.add(cell)) {
            return;
        }
        BlockState state = cursor.collisionAt(
                BlockPos.getX(cell), BlockPos.getY(cell), BlockPos.getZ(cell));
        if (state == null) {
            return;
        }
        float hardness;
        try {
            hardness = state.getDestroySpeed(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
        } catch (RuntimeException probeFailed) {
            // Modded override may need level and position; queue for recheck at apply time.
            pendingBreaks.add(cell);
            return;
        }
        if (hardness < 0 || hardness >= UNBREAKABLE_HARDNESS || hardness > hardnessBudget) {
            return;
        }
        pendingBreaks.add(cell);
    }

    /** Applies queued block breaks; tick thread only. Re-checks hardness against live level. */
    public void applyPendingBreaks() {
        if (pendingBreaks.isEmpty()) {
            return;
        }
        Level level = vehicle.level();
        if (level.isClientSide()) {
            // Guard against client-side desync.
            pendingBreaks.clear();
            return;
        }
        // No drops; blocks are smashed not mined.
        boolean drops = false;
        for (int i = 0, size = pendingBreaks.size(); i < size; i++) {
            long cell = pendingBreaks.getLong(i);
            breakCursor.set(BlockPos.getX(cell), BlockPos.getY(cell), BlockPos.getZ(cell));
            BlockState state = level.getBlockState(breakCursor);
            if (state.isAir()) {
                continue;
            }
            float hardness = state.getDestroySpeed(level, breakCursor);
            if (hardness < 0 || hardness >= UNBREAKABLE_HARDNESS) {
                continue;
            }
            // Use immutable position here; destroyBlock reaches deep and some paths retain it.
            level.destroyBlock(breakCursor.immutable(), drops, vehicle);
        }
        pendingBreaks.clear();
    }

    /**
     * 阻力影响
     */
    public Vec3 decelerationByFriction(List<VehicleCubeOBB.CubePoint> touchPoints, Vec3 velocity) {
        if (!touchPoints.isEmpty()) {
            // 接触摩擦力
            double before = velocity.y;
            double length = velocity.length();
            // Scale in one operation; preserves small-length cutoff for exact zero.
            velocity = length < 1.0e-4
                    ? Vec3.ZERO
                    : velocity.scale(Math.max(0, length - friction / mass) / length);
            trace(PhysicsTrace.Source.FRICTION, velocity.y - before);
        }
        this.velocity = velocity.toVector3f();
        return velocity;
    }

    /**
     * 受重力影响下的自由落体与三轴滚动
     */
    public Vec3 rotAndFallByGravity(PhysicsRig rig, List<VehicleCubeOBB.CubePoint> touchPoints, Vector3f[] axes, Vector3f force, Vector3f velocity) {
        var physicsCube = vehicle.getMainCubeOBB();
        // Update inertia tensor to match current hull attitude.
        refreshInertia(rig);
        boolean tumbling = tumbles();
        try {
            // 加速度使得重心偏移
            Vector3f a = new Vector3f(velocity).sub(this.velocityO);
            Vector3f gravityCenter = center.toVector3f();
            gravityCenter.add(a.mul((float) (physicsCube.height * 8)));
            // Apply contact spin first, before rotation branches; earliest point where tensor is up to date.
            if (tumbling) {
                applyContactSpin(rig);
            }
            // 升力影响
            if (force.y >= G * mass) {
                velocity.y -= G;
                trace(PhysicsTrace.Source.GRAVITY, -G);
                rig.setOnGround(false);
                return new Vec3(velocity);
            }
            // No contacts; free rotation and fall.
            if (touchPoints.isEmpty()) {
                if (tumbling) {
                    tumbleFreeRot(rig, axes, gravityCenter);
                    dampSpin(tumbleAirDamping);
                } else {
                    centerRot(rig, gravityCenter, axes);
                    dampSpin(angularDampingAir);
                }
                velocity.y -= G;
                trace(PhysicsTrace.Source.GRAVITY, -G);
                rig.setOnGround(false);
                return new Vec3(velocity);
            }
            rig.setOnGround(true);
            // Call climb every grounded tick; it carries its own guards and does nothing if not needed.
            climb(rig, touchPoints);
            // Gravity component along each axis; collect contacts on supporting faces.
            float gx = -axes[0].y;
            float gy = -axes[1].y;
            float gz = -axes[2].y;
            int supportFaces = 0;
            if (gx > 0) {
                supportFaces |= 1 << VehicleCubeOBB.CubeFace.LEFT.ordinal();
            } else if (gx < 0) {
                supportFaces |= 1 << VehicleCubeOBB.CubeFace.RIGHT.ordinal();
            }
            if (gy > 0) {
                supportFaces |= 1 << VehicleCubeOBB.CubeFace.TOP.ordinal();
            } else if (gy < 0) {
                supportFaces |= 1 << VehicleCubeOBB.CubeFace.BOTTOM.ordinal();
            }
            if (gz > 0) {
                supportFaces |= 1 << VehicleCubeOBB.CubeFace.FRONT.ordinal();
            } else if (gz < 0) {
                supportFaces |= 1 << VehicleCubeOBB.CubeFace.BACK.ordinal();
            }
            forcePoints.clear();
            for (int i = 0, size = touchPoints.size(); i < size; i++) {
                VehicleCubeOBB.CubePoint touchPoint = touchPoints.get(i);
                if ((supportFaces & (1 << touchPoint.cubeFace().ordinal())) == 0) {
                    continue;
                }
                VehicleCubeOBB.CubePointContext context = touchPoint.cubePointContext;
                Vector3f worldPos = touchPoint.cachedWorldPos();
                double surfaceY = context.surfaceY();
                if (!Double.isNaN(surfaceY)) {
                    // Reject contacts above geometry; tolerance covers point placement offset.
                    if (worldPos.y > surfaceY + 0.1) {
                        continue;
                    }
                } else if (isHalfBlock(context.blockState()) && context.hasCell()
                        && worldPos.y > context.cellY() + 0.6f) {
                    // No geometry; fallback estimate for half blocks.
                    continue;
                }
                forcePoints.add(touchPoint.obbLocalPos());
            }
            // 重力方向在局部坐标系下的向量
            Vector3f gLocalDirection = new Vector3f(gx, gy, gz);
            // 重力、受力点投影到重力为法向量的平面上
            Vector2f gc = getPlaneXY(gLocalDirection, gravityCenter);
            int pointCount = forcePoints.size();
            if (planeXs.length < pointCount) {
                int grown = Math.max(planeXs.length * 2, pointCount);
                planeXs = new float[grown];
                planeYs = new float[grown];
                hullSorted = new int[grown];
                hullOut = new int[grown * 2 + 1];
            }
            for (int i = 0; i < pointCount; i++) {
                // Project point onto support plane for 2D convex hull computation.
                Vector3f p = forcePoints.get(i);
                float along = p.dot(planeSupport);
                float projX = p.x - planeSupport.x * along;
                float projY = p.y - planeSupport.y * along;
                float projZ = p.z - planeSupport.z * along;
                planeXs[i] = projX * planeU.x + projY * planeU.y + projZ * planeU.z;
                planeYs[i] = projX * planeV.x + projY * planeV.y + projZ * planeV.z;
            }
            if (pointCount > 2) {
                localRotAxisStartO = localRotAxisStart;
                localRotAxisEndO = localRotAxisEnd;
                int hullCount = VectorUtil.convexHullIndices(planeXs, planeYs, pointCount,
                        hullSorted, hullOut);
                // 重心于支撑点闭包内，转动停止，自由落体停止
                if (VectorUtil.isPointInPolygonIndexed(gc.x, gc.y, planeXs, planeYs,
                        hullOut, hullCount)) {
                    PhysicsTrace supportTrace = vehicle.physicsTrace();
                    if (supportTrace != null) {
                        supportTrace.supported();
                        supportTrace.add(PhysicsTrace.Source.SUPPORT_CLAMP,
                                Math.max(0, velocity.y) - velocity.y);
                    }
                    velocity.y = Math.max(0, velocity.y);
                    // Whether the hull truly came to rest vs just passing through supported while rolling.
                    boolean settled = true;
                    if (tumbling) {
                        dampSpin(angularDampingGround);
                        if (angularVelocity.lengthSquared() > TUMBLE_REST_SPIN * TUMBLE_REST_SPIN) {
                            tumbleSupportRot(rig, axes, pointCount);
                            settled = false;
                        } else {
                            clearSpin();
                        }
                    } else {
                        clearSpin();
                    }
                    // Ease is skipped mid-roll but applies to settled hulls to polish final angle.
                    if (settled) {
                        boolean allBelowUnderside = true;
                        for (int i = 0; i < pointCount; i++) {
                            if (!(forcePoints.get(i).y < -physicsCube.obb().extents().y - 0.01)) {
                                allBelowUnderside = false;
                                break;
                            }
                        }
                        if (!allBelowUnderside) {
                            // 保持静态倾斜的理论极限角度是半格高垫起车身边，再小则自动补正
                            double angleWidth = Math.toDegrees(Math.atan2(0.5, physicsCube.getWidth()));
                            double angleDepth = Math.toDegrees(Math.atan2(0.5, physicsCube.getDepth()));
                            boolean shouldRotUpdate = false;
                            // Ease toward level rather than snap; avoids jittery animation on stairs.
                            float roll = rig.getZRot();
                            if (roll != 0 && Mth.abs(roll) < angleWidth - MAGIC_NUMBER / 10) {
                                rig.setZRot(Mth.abs(roll) < AUTO_LEVEL_SNAP ? 0 : roll * AUTO_LEVEL_EASE);
                                shouldRotUpdate = true;
                            }
                            float pitch = rig.getXRot();
                            if (pitch != 0 && Mth.abs(pitch) < angleDepth - MAGIC_NUMBER / 10) {
                                rig.setXRot(Mth.abs(pitch) < AUTO_LEVEL_SNAP ? 0 : pitch * AUTO_LEVEL_EASE);
                                shouldRotUpdate = true;
                            }
                            if (shouldRotUpdate && rotTick > 0) {
                                rig.posRotUpdates++;
                                rotTick -= 1;
                            }
                        }
                    }
                    // Self-righting safeguard when tumbling is disabled.
                    if (!tumbling && AllConfigs.Cached.selfRighting) {
                        if (Mth.abs(rig.getXRot()) >= 75 || Mth.abs(rig.getZRot()) >= 75) {
                            rig.setXRot(0);
                            rig.setZRot(0);
                        }
                    }
                    return new Vec3(velocity);
                }
                float minDist = Float.MAX_VALUE;
                int minIdx = -1;
                for (int i = 0; i < hullCount; i++) {
                    int j = (i + 1) % hullCount;
                    float d = VectorUtil.pointToSegmentDist(gc.x, gc.y,
                            planeXs[hullOut[i]], planeYs[hullOut[i]],
                            planeXs[hullOut[j]], planeYs[hullOut[j]]);
                    if (d < minDist) {
                        minDist = d;
                        minIdx = i;
                    }
                }
                if (minIdx == -1) {
                    return new Vec3(velocity);
                }
                localRotAxisStart = forcePoints.get(hullOut[minIdx]);
                localRotAxisEnd = forcePoints.get(hullOut[(minIdx + 1) % hullCount]);
            } else if (pointCount == 2) {
                localRotAxisStart = forcePoints.get(0);
                localRotAxisEnd = forcePoints.get(1);
            } else if (pointCount == 1) {
                // 从接触点到重心的向量，投影到支撑平面上
                Vector3f v = new Vector3f(gravityCenter).sub(forcePoints.get(0));
                Vector3f vProj = new Vector3f(v).sub(new Vector3f(gLocalDirection).mul(v.dot(gLocalDirection)));
                float len = vProj.length();
                if (len < 0.0001f) {
                    clearSpin();
                    return new Vec3(velocity);
                }
                // 旋转轴在支撑平面内，垂直于vProj：axis = gLocal × vProj
                Vector3f axisDir = new Vector3f(gLocalDirection).cross(vProj).normalize();
                float axisHalfLen = 0.5f;
                localRotAxisStart = new Vector3f(axisDir).mul(axisHalfLen).add(forcePoints.get(0));
                localRotAxisEnd = new Vector3f(axisDir).mul(-axisHalfLen).add(forcePoints.get(0));
            } else {
                // 重力在三轴方向上的分力所对应三面无接触点，则无支持力，因转动惯量而继续转动，因重力而自由落体
                if (tumbling) {
                    tumbleFreeRot(rig, axes, gravityCenter);
                    dampSpin(tumbleAirDamping);
                } else {
                    dampSpin(angularDampingAir);
                    centerRot(rig, gravityCenter, axes);
                }
                velocity.y -= G;
                trace(PhysicsTrace.Source.GRAVITY, -G);
                return new Vec3(velocity);
            }
            checkDirection(gravityCenter);
            // Apply spin loss on managed pivot (not tumbling) when edge swings past 90 degrees.
            if (!tumbling) {
                rotLoss(gc);
            }
            localRotAxisVec = new Vector3f(localRotAxisEnd).sub(localRotAxisStart);
            // 基于力矩和转动惯量计算角加速度
            // 合力 = 重力 + 外部推力（force在局部坐标系下的投影）
            Vector3f netForceLocal = new Vector3f(gLocalDirection).mul(G * mass);
            netForceLocal.add(force.dot(axes[0]), force.dot(axes[1]), force.dot(axes[2]));
            float torque = computeTorque(localRotAxisStart, localRotAxisEnd, gravityCenter, netForceLocal);
            float moi = computeMomentOfInertia(localRotAxisStart, localRotAxisEnd, physicsCube, mass, gravityCenter);
            float angularAccel = moi > 0.001f ? torqueScale * torque / moi : 0;
            // For tumbling, add torque to existing spin; for managed pivot, replace it.
            if (tumbling) {
                // Add torque impulse; keep spin state across edge changes.
                dampSpin(angularDampingGround);
                Vector3f pivotAxis = pivotAxisWorld(rig, axes, axisScratch);
                if (pivotAxis != null) {
                    angularVelocity.fma(angularAccel, pivotAxis);
                }
                clampSpin();
                tumbleEdgeRot(rig, axes);
            } else {
                float spin = Math.min(rotV * angularDampingGround + angularAccel, effectiveMaxRotV());
                setPivotSpin(rig, axes, spin);
                rot(rig, axes);
            }
            return new Vec3(velocity);
        } catch (Exception exception) {
            exception.printStackTrace();
        } finally {
            this.velocity = velocity;
        }
        return new Vec3(velocity);
    }

    /** Applies recoil spin to the vehicle. Tick thread only, deferred through queueRecoil. */
    public void recoil(WeaponUnit weaponUnit, float recoil) {
        Vec3 fireDirection = weaponUnit.worldVec();
        OBB obb = vehicle.getMainCubeOBB().obb();
        Vector3f[] axes = obb.getAxes();
        Vector3f forceStartLocal = obb.worldToLocal(weaponUnit.worldPivotPosition().add(fireDirection.scale(5)).toVector3f(), axes);
        Vector3f forcePointLocal = obb.worldToLocal(weaponUnit.worldPivotPosition().toVector3f(), axes);
        // 后坐力方向在局部坐标系下的矢量
        Vector3f force = new Vector3f(forcePointLocal).sub(forceStartLocal);
        getPlaneXY(force, forcePointLocal);
        Optional<Vector3f> forceEdge = obb.clip(new Vector3f(obb.center()).add(fireDirection.normalize().scale(16).toVector3f().negate()), obb.center());
        if (forceEdge.isPresent()) {
            Vector3f forceEdgeLocal = obb.worldToLocal(forceEdge.get(), axes);
            Vec3 axis = new Vec3(-force.z, 0, force.x).add(forceEdgeLocal.x, 0, forceEdgeLocal.z);
            localRotAxisStart = axis.normalize().scale(5).toVector3f();
            localRotAxisEnd = axis.normalize().scale(-5).toVector3f();
            checkDirection(forcePointLocal);
            PhysicsRig rig = new PhysicsRig();
            rig.capturePose(vehicle);
            // Recoil spins through the vector state like all other rotation.
            setPivotSpin(rig, axes, Math.min(0.05f * recoil, effectiveMaxRotV()));
            rot(rig, axes);
            // Keep rotation, discard displacement.
            vehicle.setXRot(rig.getXRot());
            vehicle.setYRot(rig.getYRot());
            vehicle.setZRot(rig.getZRot());
            // Recoil also imparts forward momentum.
            force = force.normalize();
            double motion = force.dot(new Vector3f(0, 0, 1)) * 0.03 * recoil;
            vehicle.setDeltaMovement(vehicle.getDeltaMovement().add(new Vec3(axes[2]).scale(motion)));
        }
    }

    public void climb(PhysicsRig rig, List<VehicleCubeOBB.CubePoint> touchPoints) {
        // One pass, no intermediate list; filter and measure max rise only.
        boolean anyClimbPoint = false;
        double rise = 0;
        for (int i = 0, size = touchPoints.size(); i < size; i++) {
            VehicleCubeOBB.CubePoint point = touchPoints.get(i);
            VehicleCubeOBB.CubeFace face = point.cubeFace();
            if (face != VehicleCubeOBB.CubeFace.FRONT
                    && face != VehicleCubeOBB.CubeFace.BOTTOM
                    && face != VehicleCubeOBB.CubeFace.BACK) {
                continue;
            }
            anyClimbPoint = true;
            Vector3f worldPos = point.cachedWorldPos();
            if (worldPos == null) {
                continue;
            }
            double top = contactTop(point);
            if (top != Double.NEGATIVE_INFINITY) {
                rise = java.lang.Math.max(rise, top - worldPos.y);
            }
        }
        if (!anyClimbPoint) {
            return;
        }
        // Guard against nose-up reared position; real work now done by rise-per-contact measurement.
        if (rig.getXRot() < MAX_CLIMB_PITCH) {
            return;
        }
        // Rise per contact against what it touched, not global hull spread; avoids deadlock on slopes.
        if (rise < CLIMB_DEADBAND) {
            return;
        }

        // Above step height is a wall and stops progress.
        if (rise > vehicle.maxUpStep()) {
            return;
        }

        // Cap lift by requested horizontal travel to avoid teleport. Use requested, not realised, movement.
        Vec3 requested = vehicle.deltaMovementO;
        double travel = requested == null
                ? 0
                : java.lang.Math.sqrt(requested.x * requested.x + requested.z * requested.z);
        double toLift = java.lang.Math.min(rise, travel * climbGradient);
        if (toLift <= 1.0e-4) {
            return;
        }
        toLift = headroom(rig, toLift);
        if (toLift <= 1.0e-4) {
            return;
        }
        rig.setPos(rig.x, rig.y + toLift, rig.z);
        trace(PhysicsTrace.Source.CLIMB, toLift);
    }

    /** Limits climb lift so unconditional setPos never puts the hull inside geometry. */
    private double headroom(PhysicsRig rig, double lift) {
        if (lift <= 0) {
            return 0;
        }
        // Use prepared broadphase snapshots to check clearance.
        OBB hull = SweptHull.climbHull(rig.hull, vehicle.sweepSkirt(), climbHull);
        BoxBuffer boxes = vehicle.sweptBroadphase().near(hull, 0, lift, 0);
        // Check through combined cast including carrier structures.
        double free = vehicle.climbToi(hull, boxes, lift, castFrame.set(hull.rotation()));
        return lift * free;
    }

    /** Returns the world Y height where a contact would sit on top of what it touched. */
    private static double contactTop(VehicleCubeOBB.CubePoint point) {
        VehicleCubeOBB.CubePointContext context = point.cubePointContext;
        double surfaceY = context.surfaceY();
        if (!Double.isNaN(surfaceY)) {
            return surfaceY;
        }
        if (!context.hasCell()) {
            return Double.NEGATIVE_INFINITY;
        }
        return context.cellY() + (isHalfBlock(context.blockState()) ? 0.5 : 1.0);
    }

    private void checkDirection(Vector3f localRotToPoint) {
        // 左手系下，拇指为rotAxisStart -> rotAxisEnd方向，四指为重力旋转方向
        Vector3f v1 = new Vector3f(localRotAxisStart).sub(localRotToPoint);
        Vector3f v2 = new Vector3f(localRotAxisEnd).sub(localRotToPoint);
        if (v1.cross(v2).dot(planeSupport) < 0) {
            Vector3f tmp = localRotAxisEnd;
            localRotAxisEnd = localRotAxisStart;
            localRotAxisStart = tmp;
        }
    }

    private void rotLoss(Vector2f rotToPoint) {
        if (localRotAxisStartO == null || localRotAxisEndO == null) {
            return;
        }
        Vector2f v1 = getPlaneXY(null, localRotAxisStart);
        Vector2f v2 = getPlaneXY(null, localRotAxisEnd);
        Vector2f v3 = getPlaneXY(null, localRotAxisStartO);
        Vector2f v4 = getPlaneXY(null, localRotAxisEndO);
        Function<Vector2f[], Vector2f> getPerp = (arr) -> {
            Vector2f a = arr[0], b = arr[1];
            Vector2f ab = new Vector2f(b).sub(a);
            Vector2f ap = new Vector2f(rotToPoint).sub(a);
            float t = ap.dot(ab) / ab.dot(ab);
            Vector2f proj = new Vector2f(a).add(new Vector2f(ab).mul(t));
            return new Vector2f(rotToPoint).sub(proj);
        };
        Vector2f perp1 = getPerp.apply(new Vector2f[]{v1, v2});
        Vector2f perp2 = getPerp.apply(new Vector2f[]{v3, v4});
        if (perp1.lengthSquared() == 0 || perp2.lengthSquared() == 0) {
            return;
        }
        float cosTheta = perp1.dot(perp2) / (perp1.length() * perp2.length());
        cosTheta = Math.max(-1.0f, Math.min(1.0f, cosTheta));
        float angleRad = Math.acos(cosTheta);
        if (angleRad > Math.PI / 2) {
            dampSpin(0.5f);
        }
    }

    private void centerRot(PhysicsRig rig, Vector3f center, Vector3f[] axes) {
        if (lockCenterRot) {
            return;
        }
        if (localRotAxisVec != null) {
            localRotAxisStart = new Vector3f(center).sub(localRotAxisVec);
            localRotAxisEnd = new Vector3f(center).add(localRotAxisVec);
            // 目前仅考虑重力
            Vector3f g = new Vector3f(0, -1, 0);
            // 重力方向在局部坐标系下的矢量
            float gx = g.dot(axes[0]);
            float gy = g.dot(axes[1]);
            float gz = g.dot(axes[2]);
            Vector3f gLocal = new Vector3f(gx, gy, gz);
            getPlaneXY(gLocal, center);
            checkDirection(center);
            rot(rig, axes);
        }
    }

    private void rot(PhysicsRig rig, Vector3f[] axes) {
        if (localRotAxisStart == null || localRotAxisEnd == null || rotV == 0) {
            return;
        }
        Vec3 pRot = new Vec3(rotateAroundAxis(rig.position().toVector3f(),
                rig.hull.localToWorld(localRotAxisStart, axes),
                rig.hull.localToWorld(localRotAxisEnd, axes),
                rotV));
        Quaternionf q = new Quaternionf(stepRot).mul(rig.rotYXZ());
        Vector3f as = new Vector3f();
        q.getEulerAnglesYXZ(as);
        if (Double.isNaN(as.x) || Double.isNaN(as.y) || Double.isNaN(as.z)) {
            return;
        }
        rotTick = 10;
        double beforeY = rig.y;
        rig.setPos(pRot);
        trace(PhysicsTrace.Source.ROTATION, rig.y - beforeY);
        rig.setYRot(-(float) Math.toDegrees(as.y));
        rig.setXRot((float) Math.toDegrees(as.x));
        if (lockZRot) {
            rig.setZRot(0);
        } else {
            rig.setZRot((float) Math.toDegrees(as.z));
        }
    }

    private Vector3f rotateAroundAxis(Vector3f point, Vector3f a, Vector3f b, float radians) {
        Vector3f axis = new Vector3f(b).sub(a).normalize();
        stepRot = new Quaternionf().fromAxisAngleRad(axis, radians);
        Vector3f relative = new Vector3f(point).sub(a);
        stepRot.transform(relative);
        return relative.add(a);
    }

    private Vector2f getPlaneXY(Vector3f support, Vector3f point) {
        if (support != null) {
            // 以planeSupport为法向量的平面有planeU, planeV两轴
            planeSupport = new Vector3f(support).normalize();
            Vector3f tmp = new Vector3f(1, 0, 0);
            planeU = tmp.cross(planeSupport).normalize();
            planeV = new Vector3f(planeSupport).cross(planeU).normalize();
        }
        // 求point在平面上的投影点x, y
        Vector3f projected = new Vector3f(point).sub(new Vector3f(planeSupport).mul(point.dot(planeSupport)));
        return new Vector2f(projected.dot(planeU), projected.dot(planeV));
    }

    public static boolean isHalfBlock(BlockState blockState) {
        if (blockState == null) {
            return false;
        }
        return blockState.hasProperty(BlockStateProperties.HALF)
                || blockState.getBlock() instanceof SlabBlock;
    }

    /**
     * 计算合力绕旋转轴的力矩标量
     * τ = (r × F) · axis
     */
    private float computeTorque(Vector3f axisStart, Vector3f axisEnd, Vector3f com, Vector3f netForceLocal) {
        Vector3f r = new Vector3f(com).sub(axisStart);
        Vector3f torqueVec = r.cross(netForceLocal);
        Vector3f axis = new Vector3f(axisEnd).sub(axisStart).normalize();
        return Math.abs(torqueVec.dot(axis));
    }

    /** Engine state snapshot for async solve rollback; restored if guard check fails. */
    public static final class State {

        private final Vector3f velocity = new Vector3f();
        private final Vector3f velocityO = new Vector3f();
        private final Vector3f angularVelocity = new Vector3f();
        private float rotV;
        private int rotTick;
        private int stuckTick;
        private Quaternionf stepRot;
        private Vector3f localRotAxisStart, localRotAxisStartO;
        private Vector3f localRotAxisEnd, localRotAxisEndO;
        private Vector3f localRotAxisVec;
        private Vector3f planeSupport, planeU, planeV;
        private final LongArrayList pendingBreaks = new LongArrayList();

    }

    /** Captures the persistent state before submitting an async solve. Tick thread only. */
    public void captureState(State state) {
        state.velocity.set(velocity);
        state.velocityO.set(velocityO);
        state.angularVelocity.set(angularVelocity);
        state.rotV = rotV;
        state.rotTick = rotTick;
        state.stuckTick = stuckTick;
        // Reference copies; pipeline replaces these fields, never mutates through them.
        state.stepRot = stepRot;
        state.localRotAxisStart = localRotAxisStart;
        state.localRotAxisStartO = localRotAxisStartO;
        state.localRotAxisEnd = localRotAxisEnd;
        state.localRotAxisEndO = localRotAxisEndO;
        state.localRotAxisVec = localRotAxisVec;
        state.planeSupport = planeSupport;
        state.planeU = planeU;
        state.planeV = planeV;
        state.pendingBreaks.clear();
        state.pendingBreaks.addAll(pendingBreaks);
    }

    /** Restores the engine to a captured state, discarding failed async solve changes. */
    public void restoreState(State state) {
        velocity = new Vector3f(state.velocity);
        velocityO = new Vector3f(state.velocityO);
        angularVelocity.set(state.angularVelocity);
        rotV = state.rotV;
        rotTick = state.rotTick;
        stuckTick = state.stuckTick;
        stepRot = state.stepRot;
        localRotAxisStart = state.localRotAxisStart;
        localRotAxisStartO = state.localRotAxisStartO;
        localRotAxisEnd = state.localRotAxisEnd;
        localRotAxisEndO = state.localRotAxisEndO;
        localRotAxisVec = state.localRotAxisVec;
        planeSupport = state.planeSupport;
        planeU = state.planeU;
        planeV = state.planeV;
        pendingBreaks.clear();
        pendingBreaks.addAll(state.pendingBreaks);
        // contactSpin is not captured; filled and consumed each solve. Zero it to avoid stale impulse.
        contactSpin.zero();
    }

    /**
     * 计算长方体绕任意空间轴（过枢轴点）的转动惯量
     * 使用主轴转动惯量 + 平行轴定理
     */
    private float computeMomentOfInertia(Vector3f axisStart, Vector3f axisEnd, VehicleCubeOBB cube, float mass, Vector3f com) {
        float w = (float) cube.getWidth();
        float h = (float) cube.getHeight();
        float d = (float) cube.getDepth();
        float Ix = mass / 12f * (h * h + d * d);
        float Iy = mass / 12f * (w * w + d * d);
        float Iz = mass / 12f * (w * w + h * h);
        Vector3f axis = new Vector3f(axisEnd).sub(axisStart).normalize();
        float I_center = Ix * axis.x * axis.x + Iy * axis.y * axis.y + Iz * axis.z * axis.z;
        Vector3f r = new Vector3f(com).sub(axisStart);
        Vector3f projOnAxis = new Vector3f(axis).mul(r.dot(axis));
        Vector3f perp = new Vector3f(r).sub(projOnAxis);
        float distSq = perp.lengthSquared();
        return I_center + mass * distSq;
    }

}
