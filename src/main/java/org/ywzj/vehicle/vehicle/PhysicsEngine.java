package org.ywzj.vehicle.vehicle;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.joml.Math;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.ywzj.vehicle.all.AllConfigs;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.util.VectorUtil;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;
import org.ywzj.vehicle.vehicle.structure.VehicleBedrockCubeOBB;

import java.util.*;
import java.util.function.Function;

public class PhysicsEngine {

    public static final double MAGIC_NUMBER = .943;
    public final AbstractVehicle vehicle;
    public final VehicleBedrockCubeOBB physicsCube;
    public float mass = 1;
    public float bounce = 0.02f;
    public float rotA = 0.01f;
    public float rotV = 0;
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
    public float gravityA = 1f / 20;
    public float gravityVMax = 0.7f;
    public Vector3f velocity = new Vector3f(0, 0, 0);
    public boolean lockZRot;
    public boolean lockCenterRot;

    public PhysicsEngine(AbstractVehicle vehicle, VehicleBedrockCubeOBB physicsCube) {
        this.vehicle = vehicle;
        this.physicsCube = physicsCube;
    }

    /**
     * 载具的正朝向约定为自身Z轴正方向
     * 车体视作理想刚体，采样点受方块的力垂直于OBB面向内
     * 方块作用力将完全抵消载具速度在力反方向上的分速度
     * 追加一个模拟撞击力导致的力方向上的微小速度
     * 为助于攀爬方块，一定车体高度下的方块碰撞会被忽略
     * 车体底面若有陷地则会施加较大的向上速度
     */
    public Vec3 motionByImpact(List<VehicleBedrockCubeOBB.CubePoint> touchPoints, Vector3f[] axes, Vec3 velocity) {
        for (VehicleBedrockCubeOBB.CubePoint touchPoint : touchPoints) {
            if (touchPoint.cubeFace() == VehicleBedrockCubeOBB.CubeFace.LEFT || touchPoint.cubeFace() == VehicleBedrockCubeOBB.CubeFace.RIGHT) {
                if (touchPoint.obbLocalPos().y < -physicsCube.getHeight() / 2 + vehicle.getMainCubeOBB().spaceY) {
                    continue;
                }
                Vec3 axesX = new Vec3(axes[0]).normalize();
                double d = velocity.dot(axesX);
                if (touchPoint.cubeFace() == VehicleBedrockCubeOBB.CubeFace.LEFT) {
                    if (d > 0) {
                        velocity = VectorUtil.projectToPlane(velocity, axes, 1, 2);
                    } else {
                        velocity = velocity.subtract(axesX.scale(d)).add(axesX.scale(-bounce));
                    }
                } else {
                    if (d < 0) {
                        velocity = VectorUtil.projectToPlane(velocity, axes, 1, 2);
                    } else {
                        velocity = velocity.subtract(axesX.scale(d)).add(axesX.scale(bounce));
                    }
                }
            } else if (touchPoint.cubeFace() == VehicleBedrockCubeOBB.CubeFace.FRONT || touchPoint.cubeFace() == VehicleBedrockCubeOBB.CubeFace.BACK) {
                if (touchPoint.obbLocalPos().y < -physicsCube.getHeight() / 2 + vehicle.getMainCubeOBB().spaceY) {
                    continue;
                }
                Vec3 axesZ = new Vec3(axes[2]).normalize();
                double d = velocity.dot(axesZ);
                if (touchPoint.cubeFace() == VehicleBedrockCubeOBB.CubeFace.FRONT) {
                    if (d > 0) {
                        velocity = VectorUtil.projectToPlane(velocity, axes, 0, 1);
                    } else {
                        velocity = velocity.subtract(axesZ.scale(d)).add(axesZ.scale(-bounce));
                    }
                } else {
                    if (d < 0) {
                        velocity = VectorUtil.projectToPlane(velocity, axes, 0, 1);
                    } else {
                        velocity = velocity.subtract(axesZ.scale(d)).add(axesZ.scale(bounce));
                    }
                }
            } else if (touchPoint.cubeFace() == VehicleBedrockCubeOBB.CubeFace.TOP || touchPoint.cubeFace() == VehicleBedrockCubeOBB.CubeFace.BOTTOM) {
                Vec3 axesY = new Vec3(axes[1]).normalize();
                double d = velocity.dot(axesY);
                if (touchPoint.cubeFace() == VehicleBedrockCubeOBB.CubeFace.TOP) {
                    if (d > 0) {
                        velocity = VectorUtil.projectToPlane(velocity, axes, 0, 2);
                    } else {
                        velocity = velocity.subtract(axesY.scale(d)).add(axesY.scale(-bounce));
                    }
                } else {
                    if (d < 0) {
                        velocity = VectorUtil.projectToPlane(velocity, axes, 0, 2);
                    }
                    if (vehicle.level().getBlockState(BlockPos.containing(new Vec3(touchPoint.cachedWorldPos().add(0, 0.1f, 0)))).isSolid()) {
                        velocity = velocity.add(0, 0.001f, 0);
                    }
                }
            }
        }
        if (vehicle.level().getBlockState(BlockPos.containing(new Vec3(physicsCube.obb().center()))).isSolid()) {
            velocity = new Vec3(velocity.x, 1, velocity.y);
        }
        this.velocity = velocity.toVector3f();
        return velocity;
    }

    /**
     * 阻力影响
     */
    public Vec3 decelerationByFriction(List<VehicleBedrockCubeOBB.CubePoint> touchPoints, Vec3 velocity) {
        if (!touchPoints.isEmpty()) {
            // 接触摩擦力
            velocity = velocity.normalize().scale(Math.max(0, velocity.length() - friction / mass));
        } else {
            // 空气阻力
            velocity = velocity.normalize().scale(velocity.length() - velocity.length() * friction / mass);
        }
        this.velocity = velocity.toVector3f();
        return velocity;
    }

    /**
     * 受重力影响下的自由落体与三轴滚动
     */
    public Vec3 rotAndFallByGravity(List<VehicleBedrockCubeOBB.CubePoint> touchPoints, Vector3f gravityCenter, Vector3f[] axes, Vector3f force, Vector3f velocity) {
        try {
            // 升力影响
            if (force.y >= gravityA * mass) {
                velocity.y -= gravityA;
                return new Vec3(velocity);
            }

            // 无任何接触，因转动惯量而继续转动，因重力而自由落体
            if (touchPoints.isEmpty()) {
                centerRot(gravityCenter, axes);
                rotV = Math.max(0, rotV - rotA / 3);
                velocity.y -= gravityA;
                return new Vec3(velocity);
            }

            // 统计重力在三轴方向上的分力的出面上的接触点，取其局部坐标
            List<VehicleBedrockCubeOBB.CubeFace> faces = new ArrayList<>();
            Vector3f gWorldDirection = new Vector3f(0, -1, 0);
            if (gWorldDirection.dot(axes[0]) > 0) {
                faces.add(VehicleBedrockCubeOBB.CubeFace.LEFT);
            } else if (gWorldDirection.dot(axes[0]) < 0) {
                faces.add(VehicleBedrockCubeOBB.CubeFace.RIGHT);
            }
            if (gWorldDirection.dot(axes[1]) > 0) {
                faces.add(VehicleBedrockCubeOBB.CubeFace.TOP);
            }  else if (gWorldDirection.dot(axes[1]) < 0) {
                faces.add(VehicleBedrockCubeOBB.CubeFace.BOTTOM);
            }
            if (gWorldDirection.dot(axes[2]) > 0) {
                faces.add(VehicleBedrockCubeOBB.CubeFace.FRONT);
            }  else if (gWorldDirection.dot(axes[2]) < 0) {
                faces.add(VehicleBedrockCubeOBB.CubeFace.BACK);
            }
            List<Vector3f> localForcePoints = touchPoints.stream()
                    .filter(touchPoint -> faces.contains(touchPoint.cubeFace()))
                    .filter(touchPoint -> {
                        if (touchPoint.cubePointContext.blockState().hasProperty(BlockStateProperties.HALF)
                                || touchPoint.cubePointContext.blockState().getBlock() instanceof SlabBlock) {
                            Vector3f worldPos = touchPoint.cachedWorldPos();
                            return worldPos.y <= BlockPos.containing(new Vec3(worldPos)).getY() + 0.7f;
                        }
                        return true;
                    })
                    .map(VehicleBedrockCubeOBB.CubePoint::obbLocalPos)
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
                    if (vehicle.getZRot() != 0 && Mth.abs(vehicle.getZRot()) < angleWidth - MAGIC_NUMBER / 10) {
                        vehicle.setZRot(0);
                        vehicle.triggerRotUpdate();
                    }
                    if (vehicle.getXRot() != 0 && Mth.abs(vehicle.getXRot()) < angleDepth - MAGIC_NUMBER / 10) {
                        vehicle.setXRot(0);
                        vehicle.triggerRotUpdate();
                    }
                    if (AllConfigs.common.selfRighting.get()) {
                        if (Mth.abs(vehicle.getXRot()) >= 90 || Mth.abs(vehicle.getZRot()) >= 90) {
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
                Vector3f v = new Vector3f(gravityCenter).sub(localForcePoints.get(0));
                if (v.x == 0 && v.z == 0) {
                    rotV = 0;
                    return new Vec3(velocity);
                }
                v = new Vector3f(v.z, 0, v.x).normalize();
                localRotAxisStart = new Vector3f(localForcePoints.get(0)).add(v);
                localRotAxisEnd = localForcePoints.get(0);
            } else {
                // 重力在三轴方向上的分力所对应三面无接触点，则无支持力，因转动惯量而继续转动，因重力而自由落体
                centerRot(gravityCenter, axes);
                velocity.y -= gravityA;
                return new Vec3(velocity);
            }

            checkDirection(gravityCenter);
            rotLoss(gc);
            localRotAxisVec = new Vector3f(localRotAxisEnd).sub(localRotAxisStart);
            rotV = Math.min(rotV + rotA, 0.3f);
            rot(axes);
            return new Vec3(velocity);
        } catch (Exception exception) {
            exception.printStackTrace();
        } finally {
            velocity.y = Math.max(velocity.y, -gravityVMax);
            this.velocity = velocity;
        }
        return new Vec3(velocity);
    }

    /**
     * 后坐力影响
     */
    public void recoil(WeaponUnit weaponUnit) {
        Vec3 fireDirection = weaponUnit.worldVec();
        Vector3f[] axes = vehicle.getMainCubeOBB().obb().getAxes();
        Vector3f forceStartLocal = vehicle.getMainCubeOBB().obb().worldToLocal(weaponUnit.worldPivotPosition().add(fireDirection.scale(5)).toVector3f(), axes);
        Vector3f forcePointLocal = vehicle.getMainCubeOBB().obb().worldToLocal(weaponUnit.worldPivotPosition().toVector3f(), axes);
        // 后坐力方向在局部坐标系下的矢量
        Vector3f force = new Vector3f(forcePointLocal).sub(forceStartLocal);
        // 简化旋转为都从炮闩下的载具中心处产生转轴，该转轴平行于底面
        Vec3 axis = new Vec3(-force.z, 0, force.x).add(forcePointLocal.x, 0, forcePointLocal.z);
        localRotAxisStart = axis.normalize().scale(5).toVector3f();
        localRotAxisEnd = axis.normalize().scale(-5).toVector3f();
        checkDirection(forcePointLocal);
        rotV = 0.05f;
        rot(axes);
        // 后坐力产生推移
        force = force.normalize();
        double motion = force.dot(new Vector3f(0, 0, 1)) * 0.05;
        vehicle.setDeltaMovement(vehicle.getDeltaMovement().add(new Vec3(axes[2]).scale(motion)));
    }

    public void climb(List<VehicleBedrockCubeOBB.CubePoint> touchPoints) {
        List<VehicleBedrockCubeOBB.CubePoint> climbPoints = new ArrayList<>(touchPoints.stream().filter(p ->
                        p.cubeFace() == VehicleBedrockCubeOBB.CubeFace.FRONT
                                || p.cubeFace() == VehicleBedrockCubeOBB.CubeFace.BOTTOM
                                || p.cubeFace() == VehicleBedrockCubeOBB.CubeFace.BACK)
                .toList());
        if (climbPoints.isEmpty()) {
            return;
        }
        // 自动爬高
        DoubleSummaryStatistics stats = climbPoints.stream()
                .mapToDouble(p -> p.obbLocalPos().y)
                .summaryStatistics();
        double yRange = stats.getMax() - stats.getMin();
        double liftLimit = vehicle.getMainCubeOBB().spaceY * 2;
        if (yRange >= liftLimit) {
            return;
        }
        if (yRange >= vehicle.getMainCubeOBB().spaceY || (vehicle.getXRot() == 0 && vehicle.getZRot() == 0)) {
            climbPoints.sort(Comparator.comparingInt(p -> -p.cubePointContext.blockPos().getY()));
            VehicleBedrockCubeOBB.CubePoint liftPoint = climbPoints.get(0);
            double liftHeight = liftPoint.cubePointContext.blockPos().getY() +
                    ((liftPoint.cubePointContext.blockState().hasProperty(BlockStateProperties.HALF)
                            || liftPoint.cubePointContext.blockState().getBlock() instanceof SlabBlock) ? 0.7f : 1f);
            double toLift = liftHeight - vehicle.position().y;
            vehicle.setPos(new Vec3(vehicle.position().x, vehicle.position().y + Mth.clamp(toLift, 0, vehicle.maxUpStep()), vehicle.position().z));
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
        Vector3f pc = vehicle.relativeRotPos(physicsCube.center(this.vehicle), false).toVector3f();
        Vector3f p1 = rotateAroundAxis(physicsCube.obb().worldToLocal(pc, axes), localRotAxisStart, localRotAxisEnd, rotV);
        Vector3f p2 = physicsCube.obb().localToWorld(p1, axes);
        Vector3f p3 = vehicle.relativeRotPos(new Vec3(p2), true).toVector3f();
        Vec3 pRot = new Vec3(p3).subtract(physicsCube.offset());

//        DebugUtil.particle(vehicle.level(), new Vec3(p2));

        Quaternionf q = vehicle.rotYXZ();
        q.mul(stepRot);
        Vector3f as = new Vector3f();
        q.getEulerAnglesYXZ(as);
        if (Double.isNaN(as.x) || Double.isNaN(as.y) || Double.isNaN(as.z)) {
            return;
        }
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

}
