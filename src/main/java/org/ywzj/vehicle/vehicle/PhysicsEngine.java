package org.ywzj.vehicle.vehicle;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.joml.Math;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.ywzj.vehicle.all.AllConfigs;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.util.VectorUtil;
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
    public float rotV = 0;
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
    public Vector3f velocity = new Vector3f(0, 0, 0);
    public Vector3f velocityO = new Vector3f(0, 0, 0);
    public boolean lockZRot;
    public boolean lockCenterRot;
    public boolean canDestroyBlock;
    public int stuckTick;

    public PhysicsEngine(AbstractVehicle vehicle) {
        this.vehicle = vehicle;
    }

    public VehicleCubeOBB physicsCube() {
        return vehicle.getMainCubeOBB();
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

        double velocityO = velocity.length();
        for (VehicleCubeOBB.CubePoint touchPoint : touchPoints) {
            if (touchPoint.cubeFace() == VehicleCubeOBB.CubeFace.LEFT || touchPoint.cubeFace() == VehicleCubeOBB.CubeFace.RIGHT) {
                if (touchPoint.obbLocalPos().y < -physicsCube.getHeight() / 2 + vehicle.getMainCubeOBB().spaceY) {
                    continue;
                }
                Vec3 axesX = new Vec3(axes[0]).normalize();
                double d = velocity.dot(axesX);
                if (touchPoint.cubeFace() == VehicleCubeOBB.CubeFace.LEFT) {
                    if (d > 0) {
                        velocity = VectorUtil.projectToPlane(velocity, axes, 1, 2);
                        isStuck = true;
                        destroyBlocks(physicsCube, touchPoint.cubeFace());
                    } else {
                        velocity = velocity.subtract(axesX.scale(d)).add(axesX.scale(-bounce));
                    }
                } else {
                    if (d < 0) {
                        velocity = VectorUtil.projectToPlane(velocity, axes, 1, 2);
                        isStuck = true;
                        destroyBlocks(physicsCube, touchPoint.cubeFace());
                    } else {
                        velocity = velocity.subtract(axesX.scale(d)).add(axesX.scale(bounce));
                    }
                }
            } else if (touchPoint.cubeFace() == VehicleCubeOBB.CubeFace.FRONT || touchPoint.cubeFace() == VehicleCubeOBB.CubeFace.BACK) {
                if (touchPoint.obbLocalPos().y < -physicsCube.getHeight() / 2 + vehicle.getMainCubeOBB().spaceY) {
                    continue;
                }
                Vec3 axesZ = new Vec3(axes[2]).normalize();
                double d = velocity.dot(axesZ);
                if (touchPoint.cubeFace() == VehicleCubeOBB.CubeFace.FRONT) {
                    if (d > 0) {
                        velocity = VectorUtil.projectToPlane(velocity, axes, 0, 1);
                        isStuck = true;
                        destroyBlocks(physicsCube, touchPoint.cubeFace());
                    } else {
                        velocity = velocity.subtract(axesZ.scale(d)).add(axesZ.scale(-bounce));
                    }
                } else {
                    if (d < 0) {
                        velocity = VectorUtil.projectToPlane(velocity, axes, 0, 1);
                        isStuck = true;
                        destroyBlocks(physicsCube, touchPoint.cubeFace());
                    } else {
                        velocity = velocity.subtract(axesZ.scale(d)).add(axesZ.scale(bounce));
                    }
                }
            } else if (touchPoint.cubeFace() == VehicleCubeOBB.CubeFace.TOP || touchPoint.cubeFace() == VehicleCubeOBB.CubeFace.BOTTOM) {
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
                    float offsetY = (float) (physicsCube().offset().y - physicsCube.height / 2);
                    Vec3 testPos = new Vec3(touchPoint.cachedWorldPos().add(0, 0.1f + offsetY, 0));
                    BlockPos testBlockPos = BlockPos.containing(testPos);
                    BlockState blockState = vehicle.level().getBlockState(testBlockPos);
                    if (blockState.isSolid()) {
                        if (!isHalfBlock(touchPoint.cubePointContext.blockState()) || testPos.y < testBlockPos.getY() + 0.55) {
                            velocity = new Vec3(velocity.x, Math.min(0.1, velocity.y + 0.01f), velocity.z);
                        }
                    }
                }
            }
        }
        if (!isStuck) {
            stuckTick = Math.max(stuckTick - 1, 0);
        }
        this.velocity = velocity.toVector3f();
        double velocityDiff = velocityO - velocity.length();
        if (velocityDiff > 0.5) {
            DamageSystem.impactHurt(velocityDiff, vehicle);
        }
        return velocity;
    }

    private void destroyBlocks(VehicleCubeOBB physicsCube, VehicleCubeOBB.CubeFace cubeFace) {
        stuckTick += 1;
        if (stuckTick == 10) {
            if (canDestroyBlock && AllConfigs.common.canDestroyBlock.get()) {
                Level level = vehicle.level();
                for (VehicleCubeOBB.CubePoint cubePoint : vehicle.getMainCubeOBB().cubePointsByFace.get(cubeFace)) {
                    if (cubePoint.obbLocalPos().y > -physicsCube.getHeight() / 2 + vehicle.getMainCubeOBB().spaceY) {
                        Vec3 pos = new Vec3(cubePoint.cachedWorldPos());
                        BlockPos bp = BlockPos.containing(pos);
                        BlockState state = level.getBlockState(bp);
                        float hardness = state.getDestroySpeed(level, bp);
                        if (hardness >= 0 && hardness < 50.0F) {
                            level.destroyBlock(bp, false, vehicle);
                        }
                    }
                }
            }
            stuckTick -= 2;
        }
    }

    /**
     * 阻力影响
     */
    public Vec3 decelerationByFriction(List<VehicleCubeOBB.CubePoint> touchPoints, Vec3 velocity) {
        if (!touchPoints.isEmpty()) {
            // 接触摩擦力
            velocity = velocity.normalize().scale(Math.max(0, velocity.length() - friction / mass));
        }
        this.velocity = velocity.toVector3f();
        return velocity;
    }

    /**
     * 受重力影响下的自由落体与三轴滚动
     */
    public Vec3 rotAndFallByGravity(List<VehicleCubeOBB.CubePoint> touchPoints, Vector3f[] axes, Vector3f force, Vector3f velocity) {
        var physicsCube = vehicle.getMainCubeOBB();
        try {
            // 加速度使得重心偏移
            Vector3f a = new Vector3f(velocity).sub(this.velocityO);
            Vector3f gravityCenter = center.toVector3f();
            gravityCenter.add(a.mul((float) (physicsCube.height * 8)));
            // 升力影响
            if (force.y >= G * mass) {
                velocity.y -= G;
                vehicle.setOnGround(false);
                return new Vec3(velocity);
            }
            // 无任何接触，因转动惯量而继续转动，因重力而自由落体
            if (touchPoints.isEmpty()) {
                centerRot(gravityCenter, axes);
                rotV *= angularDampingAir;
                if (rotV < 0.001f) {
                    rotV = 0;
                }
                velocity.y -= G;
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
                        BlockState blockState = touchPoint.cubePointContext.blockState();
                        if (isHalfBlock(blockState)) {
                            Vector3f worldPos = touchPoint.cachedWorldPos();
                            return worldPos.y <= touchPoint.cubePointContext.blockPos().y + 0.6f;
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
                    velocity.y = Math.max(0, velocity.y);
                    rotV = 0;
                    climb(touchPoints);
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
                        vehicle.triggerRotUpdate();
                        rotTick -= 1;
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
                    rotV = 0;
                    return new Vec3(velocity);
                }
                // 旋转轴在支撑平面内，垂直于vProj：axis = gLocal × vProj
                Vector3f axisDir = new Vector3f(gLocalDirection).cross(vProj).normalize();
                float axisHalfLen = 0.5f;
                localRotAxisStart = new Vector3f(axisDir).mul(axisHalfLen).add(localForcePoints.get(0));
                localRotAxisEnd = new Vector3f(axisDir).mul(-axisHalfLen).add(localForcePoints.get(0));
            } else {
                // 重力在三轴方向上的分力所对应三面无接触点，则无支持力，因转动惯量而继续转动，因重力而自由落体
                rotV *= angularDampingAir;
                if (rotV < 0.001f) rotV = 0;
                centerRot(gravityCenter, axes);
                velocity.y -= G;
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
            rotV = rotV * angularDampingGround + angularAccel;
            rotV = Math.min(rotV, maxRotV);
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
            rotV = 0.05f * recoil;
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
        List<VehicleCubeOBB.CubePoint> climbPoints = new ArrayList<>(touchPoints.stream().filter(p ->
                        p.cubeFace() == VehicleCubeOBB.CubeFace.FRONT
                                || p.cubeFace() == VehicleCubeOBB.CubeFace.BOTTOM
                                || p.cubeFace() == VehicleCubeOBB.CubeFace.BACK)
                .toList());
        if (climbPoints.isEmpty()) {
            return;
        }
        if (vehicle.getXRot() < -15) {
            return;
        }
        VehicleCubeOBB mainCubeOBB = vehicle.getMainCubeOBB();
        // 自动爬高
        DoubleSummaryStatistics stats = climbPoints.stream()
                .mapToDouble(p -> p.obbLocalPos().y)
                .summaryStatistics();
        double yRange = stats.getMax() - stats.getMin();
        double liftLimit = mainCubeOBB.spaceY * 2;
        if (yRange >= liftLimit) {
            return;
        }
        if (yRange >= mainCubeOBB.spaceY || (vehicle.getXRot() == 0 && vehicle.getZRot() == 0)) {
            climbPoints.sort(Comparator.comparingDouble(p -> -p.cubePointContext.blockPos().y));
            VehicleCubeOBB.CubePoint liftPoint = climbPoints.get(0);
            double liftHeight = liftPoint.cubePointContext.blockPos().y + (isHalfBlock(liftPoint.cubePointContext.blockState()) ? 0.5f : 1f);
            Vec3 offset = mainCubeOBB.offset().subtract(0, mainCubeOBB.height / 2, 0);
            Vec3 bottomPos = vehicle.relativeRotPos(vehicle.position().add(offset), false);
            double toLift = Mth.clamp(liftHeight - bottomPos.y, 0, vehicle.maxUpStep());
            vehicle.setPos(vehicle.position().x, vehicle.position().y + toLift, vehicle.position().z);
        }
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
            rotV *= 0.5f;
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
        vehicle.setPos(pRot);
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
