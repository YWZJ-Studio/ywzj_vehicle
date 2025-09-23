package org.ywzj.vehicle.vehicle;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.util.VectorUtil;

import java.util.*;

public class PhysicsEngine {

    public final AbstractVehicle vehicle;
    public final VehicleBedrockCubeOBB physicsCube;
    public float bounce = 0.02f;
    public float rotA = 0.01f;
    public float rotV = 0;
    public Vector3f localRotAxisStart;
    public Vector3f localRotAxisEnd;
    public Vector3f localRotAxisVec;
    public Vector3f planeSupport;
    public Vector3f planeU;
    public Vector3f planeV;
    public float gA = 1f / 20;
    public float gV = 0;
    public Quaternionf stepRot;
    public boolean lockZRot;

    public PhysicsEngine(AbstractVehicle vehicle, VehicleBedrockCubeOBB physicsCube) {
        this.vehicle = vehicle;
        this.physicsCube = physicsCube;
    }

    /**
     * 载具的正朝向约定为自身Z轴正方向
     * 车体视作理想刚体，采样点受方块的力垂直于OBB面向内
     * 方块作用力将完全抵消载具速度在力反方向上的分速度
     * 若受力点在车身一格以上，则追加一个模拟撞击力导致的力方向上的微小速度
     */
    public Vec3 impact(List<VehicleBedrockCubeOBB.CubePoint> touchPoints, Vector3f[] axes, Vec3 velocity) {
        for (VehicleBedrockCubeOBB.CubePoint touchPoint : touchPoints) {
            if (touchPoint.cubeFace() == VehicleBedrockCubeOBB.CubeFace.LEFT || touchPoint.cubeFace() == VehicleBedrockCubeOBB.CubeFace.RIGHT) {
                if (touchPoint.obbLocalPos().y < -physicsCube.cube().getHeight() / 2 + 1) {
                    continue;
                }
                velocity = VectorUtil.projectToPlane(velocity, axes, 1, 2);
                if (touchPoint.cubeFace() == VehicleBedrockCubeOBB.CubeFace.LEFT) {
                    velocity = velocity.subtract(new Vec3(axes[0]).scale(bounce));
                } else {
                    velocity = velocity.add(new Vec3(axes[0]).scale(bounce));
                }
            } else if (touchPoint.cubeFace() == VehicleBedrockCubeOBB.CubeFace.FRONT || touchPoint.cubeFace() == VehicleBedrockCubeOBB.CubeFace.BACK) {
                if (touchPoint.obbLocalPos().y < -physicsCube.cube().getHeight() / 2 + 1) {
                    continue;
                }
                velocity = VectorUtil.projectToPlane(velocity, axes, 0, 1);
                if (touchPoint.cubeFace() == VehicleBedrockCubeOBB.CubeFace.FRONT) {
                    velocity = velocity.subtract(new Vec3(axes[2]).scale(bounce));
                } else {
                    velocity = velocity.add(new Vec3(axes[2]).scale(bounce));
                }
            } else if (touchPoint.cubeFace() == VehicleBedrockCubeOBB.CubeFace.TOP || touchPoint.cubeFace() == VehicleBedrockCubeOBB.CubeFace.BOTTOM) {
                velocity = VectorUtil.projectToPlane(velocity, axes, 0, 2);
                if (touchPoint.cubeFace() == VehicleBedrockCubeOBB.CubeFace.TOP) {
                    velocity = velocity.subtract(new Vec3(axes[1]).scale(bounce));
                }
            }
        }
        return velocity;
    }

    /**
     * 受重力影响下的自由落体与三轴滚动
     */
    public Vec3 rotAndFallByGravity(List<VehicleBedrockCubeOBB.CubePoint> touchPoints, Vector3f gravityCenter, Vector3f[] axes, Vec3 velocity) {
        // 无任何接触，因转动惯量而继续转动，因重力而自由落体
        if (touchPoints.isEmpty()) {
            centerRot(gravityCenter, axes);
            gV += gA;
            return velocity.add(new Vec3(0, -gV, 0));
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
                .map(touchPoint -> {
                    if (lockZRot) {
                        return new Vector3f(0, touchPoint.obbLocalPos().y, touchPoint.obbLocalPos().z);
                    }
                    return touchPoint.obbLocalPos();
                })
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
            List<Vector2f> polygon = VectorUtil.convexHull(new ArrayList<>(points.keySet()));
            // 重心于支撑点闭包内，转动停止，自由落体停止
            if (VectorUtil.isPointInPolygon(gc, polygon)) {
                gV = 0;
                rotV = 0;
                velocity = new Vec3(velocity.x, Math.max(gV, velocity.y), velocity.z);
                // 自动爬高
                DoubleSummaryStatistics stats = touchPoints.stream()
                        .mapToDouble(p -> p.obbLocalPos().y)
                        .summaryStatistics();
                double yRange = stats.getMax() - stats.getMin();
                if (yRange < vehicle.maxUpStep() + 1) {
                    if (yRange >= vehicle.maxUpStep() || (vehicle.getXRot() == 0 && vehicle.getZRot() == 0)) {
                        touchPoints.sort(Comparator.comparingInt(p -> -p.cubePointContext.blockPos().getY()));
                        VehicleBedrockCubeOBB.CubePoint liftPoint = touchPoints.get(0);
                        double liftHeight = liftPoint.cubePointContext.blockPos().getY() +
                                ((liftPoint.cubePointContext.blockState().hasProperty(BlockStateProperties.HALF)
                                        || liftPoint.cubePointContext.blockState().getBlock() instanceof SlabBlock) ? 0.7f : 1f);
                        if (liftHeight > vehicle.position().y) {
                            vehicle.setPos(new Vec3(vehicle.position().x, liftHeight, vehicle.position().z));
                        }
                    }
                }
                // 保持静态倾斜的理论极限角度是半格高垫起车身边，再小则自动补正
                if (Mth.abs(vehicle.getZRot()) < Math.toDegrees(Math.atan(0.5 / physicsCube.cube().getWidth())) - 1) {
                    vehicle.setZRot(0);
                }
                if (Mth.abs(vehicle.getXRot()) < Math.toDegrees(Math.atan(0.5 / physicsCube.cube().getDepth())) - 1) {
                    vehicle.setXRot(0);
                    vehicle.hurtMarked = true;
                }
                return velocity;
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
                return velocity;
            }
            localRotAxisStart = points.get(polygon.get(minIdx));
            localRotAxisEnd = points.get(polygon.get((minIdx + 1) % polygon.size()));
        } else if (localForcePoints.size() == 2) {
            localRotAxisStart = localForcePoints.get(0);
            localRotAxisEnd = localForcePoints.get(1);
        } else if (localForcePoints.size() == 1) {
            Vector3f v = new Vector3f(gravityCenter).sub(localForcePoints.get(0));
            if (v.x == 0 && v.z == 0) {
                gV = 0;
                rotV = 0;
                velocity = new Vec3(velocity.x, Math.max(gV, velocity.y), velocity.z);
                return velocity;
            }
            v = new Vector3f(v.z, 0, v.x).normalize();
            localRotAxisStart = new Vector3f(localForcePoints.get(0)).add(v);
            localRotAxisEnd = localForcePoints.get(0);
        } else {
            // 重力在三轴方向上的分力所对应三面无接触点，则无支持力，因转动惯量而继续转动，因重力而自由落体
            centerRot(gravityCenter, axes);
            gV += gA;
            return velocity.add(new Vec3(0, -gV, 0));
        }

        // 右手系下，拇指为rotAxisStart -> rotAxisEnd方向，四指为重力旋转方向
        checkDirection(gc);
        localRotAxisVec = new Vector3f(localRotAxisEnd).sub(localRotAxisStart);
        rotV = Math.min(rotV + rotA, 0.3f);
        rot(axes);
        gV = 0;
        velocity = new Vec3(velocity.x, Math.max(gV, velocity.y), velocity.z);
        return velocity;
    }

    private void checkDirection(Vector2f rotToPoint) {
        Vector2f v1 = getPlaneXY(null, localRotAxisStart);
        Vector2f v2 = getPlaneXY(null, localRotAxisEnd);
        if ((v2.x - v1.x) * (rotToPoint.y - v1.y) - (v2.y - v1.y) * (rotToPoint.x - v1.x) < 0) {
            Vector3f tmp = localRotAxisEnd;
            localRotAxisEnd = localRotAxisStart;
            localRotAxisStart = tmp;
        }
    }

    private void centerRot(Vector3f center, Vector3f[] axes) {
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

            // 重力、受力点投影到重力为法向量的平面上
            Vector2f forcePoint = getPlaneXY(gLocal, center);
            checkDirection(forcePoint);
            rot(axes);
        }
    }

    private void rot(Vector3f[] axes) {
        if (localRotAxisStart == null || localRotAxisEnd == null || rotV == 0) {
            return;
        }
        Vec3 localVehiclePos = vehicle.position().subtract(physicsCube.center());
        Vector3f p1 = rotateAroundAxis(localVehiclePos.toVector3f(), localRotAxisStart, localRotAxisEnd, rotV);
        Vector3f p2 = physicsCube.obb().localToWorld(p1, axes);
        vehicle.setPos(new Vec3(p2));

//        DebugUtil.particle(vehicle.level(), new Vec3(p2));

        Vector3f as = new Vector3f();
        stepRot.getEulerAnglesYXZ(as);
        vehicle.setYRot((float) (vehicle.getYRot() + Math.toDegrees(as.y)));
        vehicle.setXRot((float) (vehicle.getXRot() + Math.toDegrees(as.x)));
        if (lockZRot) {
            vehicle.setZRot(0);
        } else {
            vehicle.setZRot((float) (vehicle.getZRot() + Math.toDegrees(as.z)));
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
            Vector3f tmp;
            if (Math.abs(planeSupport.x) <= Math.abs(planeSupport.y) && Math.abs(planeSupport.x) <= Math.abs(planeSupport.z)) {
                tmp = new Vector3f(1, 0, 0);
            } else if (Math.abs(planeSupport.y) <= Math.abs(planeSupport.z)) {
                tmp = new Vector3f(0, 1, 0);
            } else {
                tmp = new Vector3f(0, 0, 1);
            }
            planeU = tmp.cross(planeSupport).normalize();
            planeV = new Vector3f(planeSupport).cross(planeU).normalize();
        }
        // 求point在平面上的投影点x, y
        Vector3f projected = new Vector3f(point).sub(new Vector3f(planeSupport).mul(point.dot(planeSupport)));
        return new Vector2f(projected.dot(planeU), projected.dot(planeV));
    }

}
