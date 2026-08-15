package org.ywzj.vehicle.vehicle;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
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
    /**
     * Angular speed about the current pivot axis. <b>Now a view, not the state.</b> The state is
     * {@link #angularVelocity}; this is its component about whichever edge the hull is tipping on,
     * kept because the substep heuristic, the trace and the recoil model are all written in it.
     */
    public float rotV = 0;

    /**
     * World-frame angular velocity — the actual rotational state.
     * <p>
     * A scalar could only ever describe spinning about one axis at a time, which is why every
     * rotation site had to agree on what that axis was and why airborne rotation could only decay.
     * A vector carries rotation about all three axes at once and composes properly with the
     * inertia tensor below.
     * <p>
     * While the hull is supported it is deliberately constrained to the pivot edge: a vehicle
     * resting on an edge is pivoting on it, not tumbling, and the moment of inertia that governs
     * that is the one about the edge rather than the tensor about the centre of mass.
     */
    public final Vector3f angularVelocity = new Vector3f();

    /** Inverse inertia tensor in body axes, diagonal for a box. Rebuilt when the hull changes. */
    private final Vector3f invInertiaBody = new Vector3f();
    private final Matrix3f invInertiaWorld = new Matrix3f();
    private final Matrix3f inertiaScratch = new Matrix3f();
    private final Vector3f axisScratch = new Vector3f();
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
    /**
     * Blocks of rise allowed per block of horizontal travel, i.e. the steepest slope the vehicle
     * can drive up. 1.0 is 45 degrees, which is what a staircase of whole blocks works out at —
     * one up for one along. Raise it for something meant to scramble, lower it for something that
     * should struggle on a hill.
     * <p>
     * This, not {@code maxUpStep}, is what decides how a climbable step <em>feels</em>.
     * {@code maxUpStep} only decides whether the obstacle is a slope or a wall.
     */
    public float climbGradient = 1.0f;
    public Vector3f velocity = new Vector3f(0, 0, 0);
    public Vector3f velocityO = new Vector3f(0, 0, 0);
    /**
     * Block cells in contact per tick needed to trigger block breaking. Was a count of hull
     * sample points, which made a densely sampled vehicle chew through terrain faster than a
     * coarsely sampled one for the same obstacle.
     */
    private static final int STUCK_DESTROY_THRESHOLD = 10;
    /**
     * Cap on the upward velocity given to a hull that has sunk into geometry. Matches the ceiling
     * the old per-contact accumulation saturated at, so a densely sampled vehicle feels the same.
     */
    private static final double MAX_SUPPORT_LIFT = 0.1;
    /**
     * Rise below which {@code climb} does nothing. Slightly over one tick of gravity, which is
     * how far a supported vehicle can sink before its downward velocity is cancelled.
     */
    private static final double CLIMB_DEADBAND = 0.03;
    /** Nose-up pitch, in degrees, past which climbing is refused as unphysical rather than uphill. */
    private static final float MAX_CLIMB_PITCH = -60.0f;
    /** Scratch for {@code headroom}, so checking a climb allocates nothing per call. */
    private final BoxBuffer climbBoxes = new BoxBuffer();
    private final OBB climbHull = new OBB(new Vector3f(), new Vector3f(), new Quaternionf());
    public boolean lockZRot;
    public boolean lockCenterRot;
    public boolean canDestroyBlock;
    public int stuckTick;

    public PhysicsEngine(AbstractVehicle vehicle) {
        this.vehicle = vehicle;
    }


    /**
     * Rebuilds the inverse inertia tensor from the hull's box dimensions and mass.
     * <p>
     * Diagonal in body axes because the hull is a box; rotated into world axes on demand. This is
     * what a scalar angular speed could never carry — the fact that a long vehicle resists pitching
     * far more than it resists rolling.
     */
    public void refreshInertia() {
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
        Matrix3f r = vehicle.rotYXZ().get(inertiaScratch);
        invInertiaWorld.set(r);
        invInertiaWorld.scale(invInertiaBody.x, invInertiaBody.y, invInertiaBody.z);
        invInertiaWorld.mul(r.transpose());
    }

    /** World-space direction of the edge the hull is currently pivoting on, or null. */
    private Vector3f pivotAxisWorld(Vector3f[] axes, Vector3f dest) {
        if (localRotAxisStart == null || localRotAxisEnd == null) {
            return null;
        }
        VehicleCubeOBB cube = vehicle.getMainCubeOBB();
        Vector3f start = cube.obb().localToWorld(localRotAxisStart, axes);
        Vector3f end = cube.obb().localToWorld(localRotAxisEnd, axes);
        dest.set(end).sub(start);
        return dest.lengthSquared() < 1.0e-9f ? null : dest.normalize();
    }

    /**
     * Writes an angular speed about the pivot edge back into the vector state, and mirrors it into
     * {@link #rotV} for the readers still written in scalar terms.
     */
    private void setPivotSpin(Vector3f[] axes, float speed) {
        Vector3f axis = pivotAxisWorld(axes, axisScratch);
        if (axis == null) {
            angularVelocity.zero();
        } else {
            angularVelocity.set(axis).mul(speed);
        }
        rotV = speed;
    }

    /** Damps the whole vector, not just the pivot component, and drops it to rest when tiny. */
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

    public VehicleCubeOBB physicsCube() {
        return vehicle.getMainCubeOBB();
    }

    /**
     * Credits a vertical change to whatever caused it, when someone is watching. Every site that
     * moves a vehicle up or down reports here, which is what lets {@link PhysicsTrace} close its
     * ledger against the vehicle's real movement and name anything unaccounted for.
     */
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
    public Vec3 motionByImpact(List<VehicleCubeOBB.CubePoint> touchPoints, Vector3f[] axes, Vec3 velocity) {
        VehicleCubeOBB physicsCube = vehicle.getMainCubeOBB();
        boolean isStuck = false;
        // Faces that are jammed this tick. Collected rather than acted on per point, so block
        // breaking can be driven by how much area is in contact instead of how many points
        // happened to be generated there.
        EnumSet<VehicleCubeOBB.CubeFace> stuckFaces = EnumSet.noneOf(VehicleCubeOBB.CubeFace.class);
        // Whether the hull is buried in something and wants pushing back out. A flag rather than
        // a nudge applied inside the loop, see the lift below.
        boolean embedded = false;
        double climbSkirt = physicsCube.climbSkirt();
        PhysicsTrace trace = vehicle.physicsTrace();
        double tracedVelocityY = velocity.y;
        int bottomContacts = 0;
        int blockingContacts = 0;
        if (trace != null) {
            trace.mark();
        }

        double velocityO = velocity.length();
        for (VehicleCubeOBB.CubePoint touchPoint : touchPoints) {
            if (touchPoint.cubeFace() == VehicleCubeOBB.CubeFace.LEFT || touchPoint.cubeFace() == VehicleCubeOBB.CubeFace.RIGHT) {
                if (touchPoint.obbLocalPos().y < climbSkirt) {
                    continue;
                }
                blockingContacts++;
                Vec3 axesX = new Vec3(axes[0]).normalize();
                double d = velocity.dot(axesX);
                if (touchPoint.cubeFace() == VehicleCubeOBB.CubeFace.LEFT) {
                    if (d > 0) {
                        velocity = VectorUtil.projectToPlane(velocity, axes, 1, 2);
                        isStuck = true;
                        stuckFaces.add(touchPoint.cubeFace());
                    } else {
                        velocity = velocity.subtract(axesX.scale(d)).add(axesX.scale(-bounce));
                    }
                } else {
                    if (d < 0) {
                        velocity = VectorUtil.projectToPlane(velocity, axes, 1, 2);
                        isStuck = true;
                        stuckFaces.add(touchPoint.cubeFace());
                    } else {
                        velocity = velocity.subtract(axesX.scale(d)).add(axesX.scale(bounce));
                    }
                }
            } else if (touchPoint.cubeFace() == VehicleCubeOBB.CubeFace.FRONT || touchPoint.cubeFace() == VehicleCubeOBB.CubeFace.BACK) {
                if (touchPoint.obbLocalPos().y < climbSkirt) {
                    continue;
                }
                blockingContacts++;
                Vec3 axesZ = new Vec3(axes[2]).normalize();
                double d = velocity.dot(axesZ);
                if (touchPoint.cubeFace() == VehicleCubeOBB.CubeFace.FRONT) {
                    if (d > 0) {
                        velocity = VectorUtil.projectToPlane(velocity, axes, 0, 1);
                        isStuck = true;
                        stuckFaces.add(touchPoint.cubeFace());
                    } else {
                        velocity = velocity.subtract(axesZ.scale(d)).add(axesZ.scale(-bounce));
                    }
                } else {
                    if (d < 0) {
                        velocity = VectorUtil.projectToPlane(velocity, axes, 0, 1);
                        isStuck = true;
                        stuckFaces.add(touchPoint.cubeFace());
                    } else {
                        velocity = velocity.subtract(axesZ.scale(d)).add(axesZ.scale(bounce));
                    }
                }
            } else if (touchPoint.cubeFace() == VehicleCubeOBB.CubeFace.TOP || touchPoint.cubeFace() == VehicleCubeOBB.CubeFace.BOTTOM) {
                if (velocity.y > -0.1 && touchPoint.obbLocalPos().y < -physicsCube.obb().extents().y - 0.01) {
                    continue;
                }
                if (touchPoint.cubeFace() == VehicleCubeOBB.CubeFace.BOTTOM) {
                    bottomContacts++;
                }
                Vec3 axesY = new Vec3(axes[1]).normalize();
                double d = velocity.dot(axesY);
                if (touchPoint.cubeFace() == VehicleCubeOBB.CubeFace.TOP) {
                    if (d > 0) {
                        velocity = VectorUtil.projectToPlane(velocity, axes, 0, 2);
                    } else {
                        velocity = velocity.subtract(axesY.scale(d)).add(axesY.scale(-bounce));
                    }
                } else {
                    if (d < 0) {
                        velocity = VectorUtil.projectToPlane(velocity, axes, 0, 2);
                    }
                    if (!embedded) {
                        float offsetY = (float) (physicsCube().offset().y - physicsCube.height / 2);
                        // cachedWorldPos() hands back the point's shared buffer and Vector3f.add
                        // mutates in place, so adding to it directly corrupted the cached position
                        // for every later reader in the same tick — notably the half-block filter
                        // in rotAndFallByGravity, which runs after this and saw an inflated Y.
                        Vector3f cachedWorldPos = touchPoint.cachedWorldPos();
                        Vec3 testPos = new Vec3(cachedWorldPos.x, cachedWorldPos.y + 0.1f + offsetY, cachedWorldPos.z);
                        BlockPos testBlockPos = BlockPos.containing(testPos);
                        BlockState blockState = vehicle.level().getBlockState(testBlockPos);
                        if (blockState.isSolid()
                                && (!isHalfBlock(touchPoint.cubePointContext.blockState())
                                    || testPos.y < testBlockPos.getY() + 0.55)) {
                            embedded = true;
                        }
                    }
                }
            }
        }
        if (embedded) {
            // Lift the hull out of what it is buried in, once, sized by how far over it is.
            //
            // This used to add a per-contact nudge inside the loop, stopping once the accumulated
            // rise passed 0.1 — so with the old grid's hundreds of contacts it saturated on the
            // first few points every tick and the count never mattered. Cutting contacts by an
            // order of magnitude broke that: the lift became proportional to how many points the
            // query happened to generate, which varies tick to tick as a vehicle moves, and the
            // vehicle bounced. Applying the same total once removes the coupling.
            // Same story: pushing a buried hull out is the position solve's job now, and doing it
            // through velocity is precisely how a lift becomes a launch.
            Vec3 axesY = new Vec3(axes[1]).normalize();
            double tilt = Math.acos(Mth.clamp((float) axesY.y, -1, 1));
            double peakTilt = Math.toRadians(15);
            double zeroTilt = Math.toRadians(30);
            double tiltRatio = tilt <= peakTilt
                    ? tilt / peakTilt
                    : Math.max(0, (zeroTilt - tilt) / (zeroTilt - peakTilt));
            double target = Mth.lerp(tiltRatio, 0, MAX_SUPPORT_LIFT);
            double current = velocity.dot(axesY);
            if (current < target && !AllConfigs.common.planeSolverMovement.get()) {
                double before = velocity.y;
                velocity = velocity.add(axesY.scale(target - current));
                trace(PhysicsTrace.Source.SUPPORT_LIFT, velocity.y - before);
            }
        }
        if (!stuckFaces.isEmpty()) {
            destroyBlocks(physicsCube, stuckFaces, touchPoints);
        }
        Vec3 testPos = new Vec3(physicsCube.obb().center());
        BlockPos testBlockPos = BlockPos.containing(testPos);
        BlockState blockState = vehicle.level().getBlockState(testBlockPos);
        if (blockState.isSolid() && !AllConfigs.common.planeSolverMovement.get()) {
            velocity = velocity.add(0, 0.1, 0);
            trace(PhysicsTrace.Source.CENTRE_KICK, 0.1);
        }
        if (!isStuck) {
            stuckTick = Math.max(stuckTick - 1, 0);
        }
        if (trace != null) {
            // Whatever the contact loop did to height that the two named lifts above did not.
            trace.remainder(PhysicsTrace.Source.IMPACT, velocity.y - tracedVelocityY);
            trace.contacts(touchPoints.size(), bottomContacts, blockingContacts);
        }
        this.velocity = velocity.toVector3f();
        double velocityDiff = velocityO - velocity.length();
        if (velocityDiff > 0.5) {
            DamageSystem.impactHurt(velocityDiff, vehicle);
        }
        return velocity;
    }

    /**
     * Grinds through blocks a jammed face is pressed against.
     * <p>
     * Driven by the blocks actually in contact rather than by the hull's sample grid. The old
     * version counted sample points, so a densely sampled vehicle chewed through terrain faster
     * than a coarsely sampled one facing the same wall, and it read positions from every point on
     * the face whether or not that point touched anything. Counting distinct contacted block
     * cells is the same quantity measured properly, and it means the inverted and grid queries
     * agree on how fast a vehicle digs itself out.
     */
    private void destroyBlocks(VehicleCubeOBB physicsCube, Set<VehicleCubeOBB.CubeFace> stuckFaces,
                               List<VehicleCubeOBB.CubePoint> touchPoints) {
        Map<BlockPos, VehicleCubeOBB.CubeFace> contacted = new HashMap<>();
        double climbSkirt = physicsCube.climbSkirt();
        for (VehicleCubeOBB.CubePoint touchPoint : touchPoints) {
            // Strictly below, matching motionByImpact: a contact sitting exactly on the skirt is
            // one that blocked, so it is also one worth grinding through.
            if (!stuckFaces.contains(touchPoint.cubeFace())
                    || touchPoint.obbLocalPos().y < climbSkirt) {
                continue;
            }
            Vec3 blockPos = touchPoint.cubePointContext.blockPos();
            if (blockPos == null) {
                continue;
            }
            contacted.putIfAbsent(BlockPos.containing(blockPos.x, blockPos.y, blockPos.z), touchPoint.cubeFace());
        }
        if (contacted.isEmpty()) {
            return;
        }

        stuckTick += contacted.size();
        // The original tested for exact equality with the threshold while incrementing by the
        // number of contact points, so any tick that stepped over the value silently skipped the
        // trigger and the vehicle never dug free. Compare against it instead.
        if (stuckTick < STUCK_DESTROY_THRESHOLD) {
            return;
        }
        if (canDestroyBlock && AllConfigs.common.canDestroyBlock.get()) {
            Level level = vehicle.level();
            Vector3f[] axes = physicsCube.obb().getAxes();
            Set<BlockPos> blocksToDestroy = new HashSet<>();
            for (Map.Entry<BlockPos, VehicleCubeOBB.CubeFace> entry : contacted.entrySet()) {
                VehicleCubeOBB.CubeFace face = entry.getValue();
                Vector3f faceNormal = face == VehicleCubeOBB.CubeFace.LEFT || face == VehicleCubeOBB.CubeFace.RIGHT
                        ? axes[0] : axes[2];
                boolean normalAlongX = Math.abs(faceNormal.x) >= Math.abs(faceNormal.z);
                BlockPos blockPos = entry.getKey();
                for (int vertical = -1; vertical <= 1; vertical++) {
                    for (int horizontal = -1; horizontal <= 1; horizontal++) {
                        blocksToDestroy.add(blockPos.offset(
                                normalAlongX ? 0 : horizontal, vertical, normalAlongX ? horizontal : 0));
                    }
                }
            }
            for (BlockPos blockPos : blocksToDestroy) {
                BlockState blockState = level.getBlockState(blockPos);
                float hardness = blockState.getDestroySpeed(level, blockPos);
                if (!blockState.isAir() && hardness >= 0 && hardness < 50.0F) {
                    level.destroyBlock(blockPos, false, vehicle);
                }
            }
        }
        // Leave it just short of the threshold so a vehicle that stays jammed keeps digging at
        // the same cadence the old code produced.
        stuckTick = STUCK_DESTROY_THRESHOLD - 2;
    }

    /**
     * 阻力影响
     */
    public Vec3 decelerationByFriction(List<VehicleCubeOBB.CubePoint> touchPoints, Vec3 velocity) {
        if (!touchPoints.isEmpty()) {
            // 接触摩擦力
            double before = velocity.y;
            velocity = velocity.normalize().scale(Math.max(0, velocity.length() - friction / mass));
            trace(PhysicsTrace.Source.FRICTION, velocity.y - before);
        }
        this.velocity = velocity.toVector3f();
        return velocity;
    }

    /**
     * 受重力影响下的自由落体与三轴滚动
     */
    public Vec3 rotAndFallByGravity(List<VehicleCubeOBB.CubePoint> touchPoints, Vector3f[] axes, Vector3f force, Vector3f velocity) {
        var physicsCube = vehicle.getMainCubeOBB();
        // Tracks the hull's current attitude, so the tensor is right for this tick's rotation.
        refreshInertia();
        try {
            // 加速度使得重心偏移
            Vector3f a = new Vector3f(velocity).sub(this.velocityO);
            Vector3f gravityCenter = center.toVector3f();
            gravityCenter.add(a.mul((float) (physicsCube.height * 8)));
            // 升力影响
            if (force.y >= G * mass) {
                velocity.y -= G;
                trace(PhysicsTrace.Source.GRAVITY, -G);
                vehicle.setOnGround(false);
                return new Vec3(velocity);
            }
            // 无任何接触，因转动惯量而继续转动，因重力而自由落体
            if (touchPoints.isEmpty()) {
                centerRot(gravityCenter, axes);
                dampSpin(angularDampingAir);
                velocity.y -= G;
                trace(PhysicsTrace.Source.GRAVITY, -G);
                vehicle.setOnGround(false);
                return new Vec3(velocity);
            }
            vehicle.setOnGround(true);
            // 统计重力在三轴方向上的分力的出面上的接触点，取其局部坐标
            List<VehicleCubeOBB.CubeFace> faces = new ArrayList<>();
            Vector3f gWorldDirection = new Vector3f(0, -1, 0);
            if (gWorldDirection.dot(axes[0]) > 0) {
                faces.add(VehicleCubeOBB.CubeFace.LEFT);
            } else if (gWorldDirection.dot(axes[0]) < 0) {
                faces.add(VehicleCubeOBB.CubeFace.RIGHT);
            }
            if (gWorldDirection.dot(axes[1]) > 0) {
                faces.add(VehicleCubeOBB.CubeFace.TOP);
            }  else if (gWorldDirection.dot(axes[1]) < 0) {
                faces.add(VehicleCubeOBB.CubeFace.BOTTOM);
            }
            if (gWorldDirection.dot(axes[2]) > 0) {
                faces.add(VehicleCubeOBB.CubeFace.FRONT);
            }  else if (gWorldDirection.dot(axes[2]) < 0) {
                faces.add(VehicleCubeOBB.CubeFace.BACK);
            }
            List<Vector3f> localForcePoints = touchPoints.stream()
                    .filter(touchPoint -> faces.contains(touchPoint.cubeFace()))
                    .filter(touchPoint -> {
                        VehicleCubeOBB.CubePointContext context = touchPoint.cubePointContext;
                        Vector3f worldPos = touchPoint.cachedWorldPos();
                        double surfaceY = context.surfaceY();
                        if (!Double.isNaN(surfaceY)) {
                            // Collision boxes follow the real shape now, so a contact above the
                            // geometry cannot be generated in the first place and this only ever
                            // rejects a provider's conservative bound. The tolerance covers the
                            // outward offset the point is placed at.
                            return worldPos.y <= surfaceY + 0.1;
                        }
                        // No geometry reported: fall back to guessing a half block from the state,
                        // which is why the slab estimate is still here.
                        if (isHalfBlock(context.blockState())) {
                            return worldPos.y <= context.blockPos().y + 0.6f;
                        }
                        return true;
                    })
                    .map(VehicleCubeOBB.CubePoint::obbLocalPos)
                    .toList();
            // 重力方向在局部坐标系下的向量
            float gx = gWorldDirection.dot(axes[0]);
            float gy = gWorldDirection.dot(axes[1]);
            float gz = gWorldDirection.dot(axes[2]);
            Vector3f gLocalDirection = new Vector3f(gx, gy, gz);
            // 重力、受力点投影到重力为法向量的平面上
            Vector2f gc = getPlaneXY(gLocalDirection, gravityCenter);
            HashMap<Vector2f, Vector3f> points = new HashMap<>();
            for (Vector3f forcePoint : localForcePoints) {
                points.put(getPlaneXY(null, forcePoint), forcePoint);
            }
            if (localForcePoints.size() > 2) {
                localRotAxisStartO = localRotAxisStart;
                localRotAxisEndO = localRotAxisEnd;
                List<Vector2f> polygon = VectorUtil.convexHull(new ArrayList<>(points.keySet()));
                // 重心于支撑点闭包内，转动停止，自由落体停止
                if (VectorUtil.isPointInPolygon(gc, polygon)) {
                    PhysicsTrace supportTrace = vehicle.physicsTrace();
                    if (supportTrace != null) {
                        supportTrace.supported();
                        supportTrace.add(PhysicsTrace.Source.SUPPORT_CLAMP,
                                Math.max(0, velocity.y) - velocity.y);
                    }
                    velocity.y = Math.max(0, velocity.y);
                    clearSpin();
                    climb(touchPoints);
                    if (!localForcePoints.stream().allMatch(localForcePoint -> localForcePoint.y < -physicsCube.obb().extents().y - 0.01)) {
                        // 保持静态倾斜的理论极限角度是半格高垫起车身边，再小则自动补正
                        double angleWidth = Math.toDegrees(Math.atan2(0.5, physicsCube.getWidth()));
                        double angleDepth = Math.toDegrees(Math.atan2(0.5, physicsCube.getDepth()));
                        boolean shouldRotUpdate = false;
                        if (Mth.abs(vehicle.getZRot()) < angleWidth - MAGIC_NUMBER / 10) {
                            vehicle.setZRot(0);
                            shouldRotUpdate = true;
                        }
                        if (Mth.abs(vehicle.getXRot()) < angleDepth - MAGIC_NUMBER / 10) {
                            vehicle.setXRot(0);
                            shouldRotUpdate = true;
                        }
                        if (shouldRotUpdate && rotTick > 0) {
                            vehicle.triggerPosRotUpdate();
                            rotTick -= 1;
                        }
                    }
                    if (AllConfigs.common.selfRighting.get()) {
                        if (Mth.abs(vehicle.getXRot()) >= 75 || Mth.abs(vehicle.getZRot()) >= 75) {
                            vehicle.setXRot(0);
                            vehicle.setZRot(0);
                        }
                    }
                    return new Vec3(velocity);
                }
                float minDist = Float.MAX_VALUE;
                int minIdx = -1;
                for (int i = 0; i < polygon.size(); i++) {
                    int j = (i + 1) % polygon.size();
                    float d = VectorUtil.pointToSegmentDist(gc, polygon.get(i), polygon.get(j));
                    if (d < minDist) {
                        minDist = d;
                        minIdx = i;
                    }
                }
                if (minIdx == -1) {
                    return new Vec3(velocity);
                }
                localRotAxisStart = points.get(polygon.get(minIdx));
                localRotAxisEnd = points.get(polygon.get((minIdx + 1) % polygon.size()));
            } else if (localForcePoints.size() == 2) {
                localRotAxisStart = localForcePoints.get(0);
                localRotAxisEnd = localForcePoints.get(1);
            } else if (localForcePoints.size() == 1) {
                // 从接触点到重心的向量，投影到支撑平面上
                Vector3f v = new Vector3f(gravityCenter).sub(localForcePoints.get(0));
                Vector3f vProj = new Vector3f(v).sub(new Vector3f(gLocalDirection).mul(v.dot(gLocalDirection)));
                float len = vProj.length();
                if (len < 0.0001f) {
                    clearSpin();
                    return new Vec3(velocity);
                }
                // 旋转轴在支撑平面内，垂直于vProj：axis = gLocal × vProj
                Vector3f axisDir = new Vector3f(gLocalDirection).cross(vProj).normalize();
                float axisHalfLen = 0.5f;
                localRotAxisStart = new Vector3f(axisDir).mul(axisHalfLen).add(localForcePoints.get(0));
                localRotAxisEnd = new Vector3f(axisDir).mul(-axisHalfLen).add(localForcePoints.get(0));
            } else {
                // 重力在三轴方向上的分力所对应三面无接触点，则无支持力，因转动惯量而继续转动，因重力而自由落体
                dampSpin(angularDampingAir);
                centerRot(gravityCenter, axes);
                velocity.y -= G;
                trace(PhysicsTrace.Source.GRAVITY, -G);
                return new Vec3(velocity);
            }
            checkDirection(gravityCenter);
            rotLoss(gc);
            localRotAxisVec = new Vector3f(localRotAxisEnd).sub(localRotAxisStart);
            // 基于力矩和转动惯量计算角加速度
            // 合力 = 重力 + 外部推力（force在局部坐标系下的投影）
            Vector3f netForceLocal = new Vector3f(gLocalDirection).mul(G * mass);
            netForceLocal.add(force.dot(axes[0]), force.dot(axes[1]), force.dot(axes[2]));
            float torque = computeTorque(localRotAxisStart, localRotAxisEnd, gravityCenter, netForceLocal);
            float moi = computeMomentOfInertia(localRotAxisStart, localRotAxisEnd, physicsCube, mass, gravityCenter);
            float angularAccel = moi > 0.001f ? torqueScale * torque / moi : 0;
            // Pivot on an edge: the governing inertia is the one about that edge, which the
            // parallel-axis result above already gives, so the tensor is not the right quantity
            // here. The vector state still carries the result, which is what lets damping and the
            // tip-speed clamp act on a real angular velocity rather than on a bare number.
            float spin = Math.min(rotV * angularDampingGround + angularAccel, effectiveMaxRotV());
            setPivotSpin(axes, spin);
            rot(axes);
            return new Vec3(velocity);
        } catch (Exception exception) {
            exception.printStackTrace();
        } finally {
            this.velocity = velocity;
        }
        return new Vec3(velocity);
    }

    /**
     * 后坐力影响
     */
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
            // Recoil spins the hull about the axis just built, so it goes through the same
            // vector state as everything else rather than assigning the scalar behind its back.
            setPivotSpin(axes, Math.min(0.05f * recoil, effectiveMaxRotV()));
            Vec3 lastPosition = vehicle.position();
            rot(axes);
            vehicle.setPos(lastPosition);
            // 后坐力产生推移
            force = force.normalize();
            double motion = force.dot(new Vector3f(0, 0, 1)) * 0.03 * recoil;
            vehicle.setDeltaMovement(vehicle.getDeltaMovement().add(new Vec3(axes[2]).scale(motion)));
        }
    }

    public void climb(List<VehicleCubeOBB.CubePoint> touchPoints) {
        // Under the plane solver the ground constraint already puts the hull on top of whatever it
        // is standing on, continuously and in proportion to how far it drove. Running this as well
        // would step it twice.
        if (AllConfigs.common.planeSolverMovement.get()) {
            return;
        }
        List<VehicleCubeOBB.CubePoint> climbPoints = new ArrayList<>(touchPoints.stream().filter(p ->
                        p.cubeFace() == VehicleCubeOBB.CubeFace.FRONT
                                || p.cubeFace() == VehicleCubeOBB.CubeFace.BOTTOM
                                || p.cubeFace() == VehicleCubeOBB.CubeFace.BACK)
                .toList());
        if (climbPoints.isEmpty()) {
            return;
        }
        // Nose-up past this and the vehicle is standing on its tail, not driving up something.
        //
        // This used to be 15 degrees, which is shallower than a great many hills: a capture had a
        // wheeled vehicle pinned at exactly -20.87 for 133 ticks, and 98% of the substeps where it
        // was allowed no movement at all were ones where this guard had already refused to climb.
        // A vehicle on a slope is nose-up by definition, so a limit anywhere near the slopes it is
        // meant to drive up switches climbing off precisely when it is needed.
        //
        // The guard's real job — do not let a reared-up vehicle scale a wall — is now done by the
        // measurement instead. Rise is taken per contact against the surface that contact touched,
        // so a hull lying on a slope reports about zero however steeply it is pitched, and a hull
        // reared against a wall reports the wall and is refused by the step-height check below.
        if (vehicle.getXRot() < MAX_CLIMB_PITCH) {
            return;
        }
        // How far the worst-placed contact has to rise to stand on top of what it is touching.
        //
        // Measured per contact — each against the surface it personally hit — and not, as it used
        // to be, as the world-vertical gap between the highest contacted geometry and the lowest
        // contact anywhere on the hull. That global spread is what made blocky terrain read as a
        // wall: park a four-block vehicle on a one-block staircase and its front contact is three
        // blocks above its rear contact, so the spread is 3 and every step is "too tall to climb"
        // even though the vehicle is already lying on the slope. Per contact, a hull resting on a
        // matched slope reports a rise of about zero, which is the correct answer — there is
        // nothing to climb, it is already on the surface.
        //
        // Both ends still come from contacts, which is the invariant that matters. Every version
        // that measured the low end from the hull instead — the cube's underside, then the lowest
        // sample point, then that point through the OBB — was wrong in the same way: the reference
        // was a piece of the vehicle that need not be touching anything, so the error depended on
        // the vehicle's shape and attitude rather than its height and did not shrink when the
        // vehicle was lifted. Climb lifted, re-measured, got the same rise back, and lifted again,
        // forever. Both ends on contacts makes the rise fall one-for-one with the lift.
        double rise = 0;
        for (int i = 0, size = climbPoints.size(); i < size; i++) {
            VehicleCubeOBB.CubePoint point = climbPoints.get(i);
            Vector3f worldPos = point.cachedWorldPos();
            if (worldPos == null) {
                continue;
            }
            double top = contactTop(point);
            if (top != Double.NEGATIVE_INFINITY) {
                rise = java.lang.Math.max(rise, top - worldPos.y);
            }
        }
        // A resting vehicle sinks a hair into what holds it up — a contact only registers below
        // the surface, and gravity adds up to one tick of fall before it is cancelled. Without a
        // deadband that reads as a climbable millimetre every tick.
        if (rise < CLIMB_DEADBAND) {
            return;
        }

        // Above the step height it is a wall, and a wall stops the vehicle. This used to exempt a
        // perfectly level vehicle, which let one walk up a two-block face a step at a time — the
        // exemption is gone, so two blocks is a hard stop the way a two-block riser should be.
        if (rise > vehicle.maxUpStep()) {
            return;
        }

        // Ride it like a slope rather than teleporting onto it.
        //
        // The lift used to be applied in full the moment a step was detected, so a vehicle
        // creeping forward at a twentieth of a block rose a whole one. That is not climbing, it is
        // a launch, and the trace showed it as such: the largest single upward event in a run was
        // a full +1.0000 while the vehicle was barely moving. It also left the rear of the hull
        // hanging a block in the air, which is where the hopping came from.
        //
        // Capping the lift at how far the vehicle drives horizontally makes the same step a ramp:
        // ask for a tenth of a block forward and rise a tenth. Minecraft's geometry is still cubic
        // and it still looks a little odd up close, but a one-block staircase drives like the
        // slope it is meant to represent, and the attitude has time to follow because rotation
        // gets ticks to work with instead of a single frame.
        //
        // Measured from the movement the vehicle *asked* for this tick, not the movement it got.
        // Using the realised displacement deadlocks: the swept-hull backstop can legitimately
        // refuse the whole step, that leaves zero travel, zero travel means zero lift, and with no
        // lift the obstacle is still there next tick. A capture caught exactly that — 244 substeps
        // at one position, throttle open, time of impact zero every time. Driving into a slope is
        // what makes a vehicle climb it; whether the wheels are making progress is the consequence,
        // not the cause.
        Vec3 requested = vehicle.deltaMovementO;
        double travel = requested == null
                ? 0
                : java.lang.Math.sqrt(requested.x * requested.x + requested.z * requested.z);
        double toLift = java.lang.Math.min(rise, travel * climbGradient);
        if (toLift <= 1.0e-4) {
            return;
        }
        toLift = headroom(toLift);
        if (toLift <= 1.0e-4) {
            return;
        }
        vehicle.setPos(vehicle.position().x, vehicle.position().y + toLift, vehicle.position().z);
        trace(PhysicsTrace.Source.CLIMB, toLift);
    }

    /**
     * Trims a climb to what the space above the vehicle will actually take.
     * <p>
     * The lift is a {@code setPos}, and {@code setPos} on this entity is unconditional — it runs
     * outside {@code aiStep}, so the swept-hull backstop never sees it. That made climb the one
     * mover that could put the hull inside a block with nothing checking, and a play-test capture
     * caught it doing exactly that: a full one-block teleport, the largest single upward event in
     * the run, straight into geometry. The tick after, the hull starts its step already
     * overlapping, the sweep disables itself, and the vehicle rides through the wall.
     * <p>
     * Bisecting for the largest lift that stays clear keeps the climb — which the vehicle needs to
     * get up steps at all — while making it obey the same rule as every other mover.
     */
    private double headroom(double lift) {
        if (lift <= 0) {
            return 0;
        }
        AABB bounds = vehicle.getBoundingBox().expandTowards(0, lift, 0).inflate(1.0);
        ChunkCollisionCache cache = ChunkCollisionCache.of(vehicle.level());
        if (!cache.prepare(vehicle.level(), bounds)) {
            // Chunks are not loaded well enough to answer; the vehicle is about to be frozen by the
            // streaming layer anyway, so refusing to climb is both safe and short-lived.
            return 0;
        }
        climbBoxes.clear();
        cache.collectBoxes(bounds, climbBoxes);
        if (climbBoxes.isEmpty()) {
            return lift;
        }
        OBB hull = SweptHull.climbHull(physicsCube().obb(), vehicle.sweepSkirt(), climbHull);
        double free = SweptHull.timeOfImpact(hull, climbBoxes, new Vec3(0, lift, 0));
        return lift * free;
    }

    /**
     * World height a contact would have to be lifted to in order to stand on top of what it
     * touched.
     * <p>
     * Reported by the collision snapshot when the contact came from world geometry, so a slab is
     * half a block and a stair is a whole one — measured, not inferred from the block's
     * properties. Providers have no geometry to report, so those fall back to the old estimate:
     * a {@code HALF} property means half a block. That estimate is why stairs used to be climbed
     * as if they were half height and top slabs as if they were bottom ones.
     */
    private static double contactTop(VehicleCubeOBB.CubePoint point) {
        VehicleCubeOBB.CubePointContext context = point.cubePointContext;
        double surfaceY = context.surfaceY();
        if (!Double.isNaN(surfaceY)) {
            return surfaceY;
        }
        Vec3 blockPos = context.blockPos();
        if (blockPos == null) {
            return Double.NEGATIVE_INFINITY;
        }
        return blockPos.y + (isHalfBlock(context.blockState()) ? 0.5 : 1.0);
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

    private void centerRot(Vector3f center, Vector3f[] axes) {
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
            rot(axes);
        }
    }

    private void rot(Vector3f[] axes) {
        if (localRotAxisStart == null || localRotAxisEnd == null || rotV == 0) {
            return;
        }
        var physicsCube = vehicle.getMainCubeOBB();
        Vec3 pRot = new Vec3(rotateAroundAxis(vehicle.position().toVector3f(),
                physicsCube.obb().localToWorld(localRotAxisStart, axes),
                physicsCube.obb().localToWorld(localRotAxisEnd, axes),
                rotV));
        Quaternionf q = new Quaternionf(stepRot).mul(vehicle.rotYXZ());
        Vector3f as = new Vector3f();
        q.getEulerAnglesYXZ(as);
        if (Double.isNaN(as.x) || Double.isNaN(as.y) || Double.isNaN(as.z)) {
            return;
        }
        rotTick = 10;
        double beforeY = vehicle.getY();
        vehicle.setPos(pRot);
        trace(PhysicsTrace.Source.ROTATION, vehicle.getY() - beforeY);
        vehicle.setYRot(-(float) Math.toDegrees(as.y));
        vehicle.setXRot((float) Math.toDegrees(as.x));
        if (lockZRot) {
            vehicle.setZRot(0);
        } else {
            vehicle.setZRot((float) Math.toDegrees(as.z));
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
