package org.ywzj.vehicle.vehicle.structure;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.*;
import org.joml.Math;

import java.util.Optional;

/**
 * Codes based on @AnECanSaiTin's <a href="https://github.com/AnECanSaiTin/HitboxAPI">HitboxAPI</a>
 *
 * @param center   旋转中心
 * @param extents  三个轴向上的半长
 * @param rotation 旋转
 */
public record OBB(Vector3f center, Vector3f extents, Quaternionf rotation) {

    public void setCenter(Vector3f center) {
        this.center.set(center);
    }

    public void setExtents(Vector3f extents) {
        this.extents.set(extents);
    }

    public void setRotation(Quaternionf rotation) {
        this.rotation.set(rotation);
    }

    public float getSize() {
        return extents.x * 2 * extents.y * 2 * extents.z * 2;
    }

    /**
     * 获取OBB的8个顶点坐标
     *
     * @return 顶点坐标
     */
    public Vector3f[] getVertices() {
        Vector3f[] vertices = new Vector3f[8];

        Vector3f[] localVertices = new Vector3f[]{
                new Vector3f(-extents.x, -extents.y, -extents.z),
                new Vector3f(extents.x, -extents.y, -extents.z),
                new Vector3f(extents.x, extents.y, -extents.z),
                new Vector3f(-extents.x, extents.y, -extents.z),
                new Vector3f(-extents.x, -extents.y, extents.z),
                new Vector3f(extents.x, -extents.y, extents.z),
                new Vector3f(extents.x, extents.y, extents.z),
                new Vector3f(-extents.x, extents.y, extents.z)
        };

        for (int i = 0; i < 8; i++) {
            Vector3f vertex = localVertices[i];
            vertex.rotate(rotation);
            vertex.add(center);
            vertices[i] = vertex;
        }

        return vertices;
    }

    /**
     * 获取OBB的三个正交轴
     *
     * @return 正交轴
     */
    public Vector3f[] getAxes() {
        Vector3f[] axes = new Vector3f[]{
                new Vector3f(1, 0, 0),
                new Vector3f(0, 1, 0),
                new Vector3f(0, 0, 1)};
        rotation.transform(axes[0]);
        rotation.transform(axes[1]);
        rotation.transform(axes[2]);
        return axes;
    }

    /**
     * 判断两个OBB是否相撞
     */
    public static boolean isColliding(OBB obb, OBB other) {
        Vector3f[] axes1 = obb.getAxes();
        Vector3f[] axes2 = other.getAxes();
        return Intersectionf.testObOb(obb.center(), axes1[0], axes1[1], axes1[2], obb.extents(),
                other.center(), axes2[0], axes2[1], axes2[2], other.extents());
    }

    /**
     * 判断OBB和AABB是否相撞
     */
    public static boolean isColliding(OBB obb, AABB aabb) {
        Vector3f obbCenter = obb.center();
        Vector3f[] obbAxes = obb.getAxes();
        Vector3f obbHalfExtents = obb.extents();
        Vector3f aabbCenter = aabb.getCenter().toVector3f();
        Vector3f aabbHalfExtents = new Vector3f((float) (aabb.getXsize() / 2f), (float) (aabb.getYsize() / 2f), (float) (aabb.getZsize() / 2f));
        return Intersectionf.testObOb(
                obbCenter.x, obbCenter.y, obbCenter.z,
                obbAxes[0].x, obbAxes[0].y, obbAxes[0].z,
                obbAxes[1].x, obbAxes[1].y, obbAxes[1].z,
                obbAxes[2].x, obbAxes[2].y, obbAxes[2].z,
                obbHalfExtents.x, obbHalfExtents.y, obbHalfExtents.z,
                aabbCenter.x, aabbCenter.y, aabbCenter.z,
                1, 0, 0,
                0, 1, 0,
                0, 0, 1,
                aabbHalfExtents.x, aabbHalfExtents.y, aabbHalfExtents.z
        );
    }

    /**
     * 计算OBB上离待判定点最近的点
     *
     * @param point 待判定点
     * @param obb   OBB盒
     * @return 在OBB上离待判定点最近的点
     */
    public static Vector3f getClosestPointOBB(Vector3f point, OBB obb) {
        Vector3f nearP = new Vector3f(obb.center());
        Vector3f dist = point.sub(nearP, new Vector3f());

        float[] extents = new float[]{obb.extents().x, obb.extents().y, obb.extents().z};
        Vector3f[] axes = obb.getAxes();

        for (int i = 0; i < 3; i++) {
            float distance = dist.dot(axes[i]);
            distance = Math.clamp(distance, -extents[i], extents[i]);

            nearP.x += distance * axes[i].x;
            nearP.y += distance * axes[i].y;
            nearP.z += distance * axes[i].z;
        }

        return nearP;
    }

    public Optional<Vector3f> clip(Vector3f pFrom, Vector3f pTo) {
        // 计算OBB的局部坐标系基向量（世界坐标系中的方向）
        Vector3f[] axes = new Vector3f[3];
        axes[0] = rotation.transform(new Vector3f(1, 0, 0));
        axes[1] = rotation.transform(new Vector3f(0, 1, 0));
        axes[2] = rotation.transform(new Vector3f(0, 0, 1));

        // 将点转换到OBB局部坐标系
        Vector3f localFrom = worldToLocal(pFrom, axes);
        Vector3f localTo = worldToLocal(pTo, axes);

        // 射线方向（局部坐标系）
        Vector3f dir = new Vector3f(localTo).sub(localFrom);

        // Slab算法参数
        double tEnter = 0.0;      // 进入时间
        double tExit = 1.0;       // 离开时间

        // 在三个轴上执行Slab算法
        for (int i = 0; i < 3; i++) {
            double min = -extents.get(i);
            double max = extents.get(i);
            double origin = localFrom.get(i);
            double direction = dir.get(i);

            // 处理射线平行于轴的情况
            if (Math.abs(direction) < 1e-7f) {
                if (origin < min || origin > max) {
                    return Optional.empty();
                }
                continue;
            }

            // 计算与两个平面的交点参数
            double t1 = (min - origin) / direction;
            double t2 = (max - origin) / direction;

            // 确保tNear是近平面，tFar是远平面
            double tNear = Math.min(t1, t2);
            double tFar = Math.max(t1, t2);

            // 更新进入/离开时间
            if (tNear > tEnter) tEnter = tNear;
            if (tFar < tExit) tExit = tFar;

            // 检查是否提前退出（无交点）
            if (tEnter > tExit) {
                return Optional.empty();
            }
        }

        // 检查是否有有效交点
        // 计算局部坐标系中的交点
        Vector3f localHit = new Vector3f(dir).mul((float) tEnter).add(localFrom);
        // 转换回世界坐标系
        return Optional.of(localToWorld(localHit, axes));
    }

    // 世界坐标转局部坐标
    public Vector3f worldToLocal(Vector3f worldPoint, Vector3f[] axes) {
        Vector3f rel = new Vector3f(worldPoint).sub(center);
        return new Vector3f(
                rel.dot(axes[0]),
                rel.dot(axes[1]),
                rel.dot(axes[2])
        );
    }

    // 局部坐标转世界坐标
    public Vector3f localToWorld(Vector3f localPoint, Vector3f[] axes) {
        Vector3f result = new Vector3f(center);
        result.add(axes[0].mul(localPoint.x, new Vector3f()));
        result.add(axes[1].mul(localPoint.y, new Vector3f()));
        result.add(axes[2].mul(localPoint.z, new Vector3f()));
        return result;
    }

    public OBB inflate(float amount) {
        Vector3f newExtents = new Vector3f(extents).add(amount, amount, amount);
        return new OBB(center, newExtents, rotation);
    }

    public OBB inflate(float x, float y, float z) {
        Vector3f newExtents = new Vector3f(extents).add(x, y, z);
        return new OBB(center, newExtents, rotation);
    }

    public OBB move(Vec3 vec3) {
        Vector3f newCenter = new Vector3f((float) (center.x + vec3.x), (float) (center.y + vec3.y), (float) (center.z + vec3.z));
        return new OBB(newCenter, extents, rotation);
    }

    /**
     * 检查点是否在OBB内部
     *
     * @return 如果点在OBB内部则返回true，否则返回false
     */
    public boolean contains(Vec3 vec3) {
        // 计算点到OBB中心的向量
        Vector3f rel = new Vector3f(vec3.toVector3f()).sub(center);

        Vector3f[] axes = new Vector3f[3];
        axes[0] = rotation.transform(new Vector3f(1, 0, 0));
        axes[1] = rotation.transform(new Vector3f(0, 1, 0));
        axes[2] = rotation.transform(new Vector3f(0, 0, 1));

        // 将相对向量投影到OBB的三个轴上
        float projX = Math.abs(rel.dot(axes[0]));
        float projY = Math.abs(rel.dot(axes[1]));
        float projZ = Math.abs(rel.dot(axes[2]));

        // 检查投影值是否小于对应轴上的半长
        return projX <= extents.x &&
                projY <= extents.y &&
                projZ <= extents.z;
    }

    public double embeddingDepth(Vec3 vec3) {
        // 计算点到OBB中心的向量
        Vector3f rel = new Vector3f(vec3.toVector3f()).sub(center);

        // 将相对向量投影到OBB的三个轴上
        Vector3f[] axes = getAxes();
        float projX = Math.abs(rel.dot(axes[0]));
        float projY = Math.abs(rel.dot(axes[1]));
        float projZ = Math.abs(rel.dot(axes[2]));

        return Math.min(extents.x - projX, Math.min(extents.y - projY, extents.z - projZ));
    }

    public int embeddingFace(Vec3 vec3) {
        // 计算点到OBB中心的向量
        Vector3f rel = new Vector3f(vec3.toVector3f()).sub(center);

        // 将相对向量投影到OBB的三个轴上
        Vector3f[] axes = getAxes();
        float projX = Math.abs(rel.dot(axes[0]));
        float projY = Math.abs(rel.dot(axes[1]));
        float projZ = Math.abs(rel.dot(axes[2]));

        float min = Float.MAX_VALUE;
        int index = 0;
        float dx = extents.x - projX;
        float dy = extents.y - projY;
        float dz = extents.z - projZ;
        if (dx < min) {
            index = 1;
            min = dx;
        }
        if (dy < min) {
            index = 2;
            min = dy;
        }
        if (dz < min) {
            index = 3;
        }
        return (rel.dot(axes[index - 1]) < 0 ? -1 : 1) * index;
    }

    /**
     * 计算 AABB 与 OBB 之间的最小平移向量 (MTV)
     * 如果不碰撞，返回 (0, 0, 0)
     * 如果碰撞，返回的向量方向为 [从 OBB 指向 AABB]，长度为重叠深度
     */
    public Vector3f calculateMTV(AABB aabb) {
        Vector3f aabbCenter = aabb.getCenter().toVector3f();
        Vector3f aabbExtents = new Vector3f((float) aabb.getXsize() / 2f, (float) aabb.getYsize() / 2f, (float) aabb.getZsize() / 2f);

        Vector3f[] axesOBB = this.getAxes();
        Vector3f[] axesAABB = { new Vector3f(1, 0, 0), new Vector3f(0, 1, 0), new Vector3f(0, 0, 1) };

        float minOverlap = Float.MAX_VALUE;
        Vector3f mtvAxis = new Vector3f();

        // 15 条需要测试的轴
        Vector3f[] testAxes = new Vector3f[15];
        System.arraycopy(axesAABB, 0, testAxes, 0, 3);
        System.arraycopy(axesOBB, 0, testAxes, 3, 3);

        int count = 6;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                testAxes[count++] = new Vector3f(axesAABB[i]).cross(axesOBB[j]);
            }
        }

        for (Vector3f axis : testAxes) {
            if (axis.lengthSquared() < 1e-6f) continue; // 忽略平行产生的零向量
            axis.normalize();

            // 计算投影范围
            float overlap = getOverlap(axis, aabbCenter, aabbExtents, this.center(), this.extents(), axesOBB);

            if (overlap <= 0) return new Vector3f(0); // 发现分离轴，说明不碰撞

            if (overlap < minOverlap) {
                minOverlap = overlap;
                mtvAxis.set(axis);
            }
        }

        // 确保 MTV 方向是从 OBB 指向 AABB (即推开的方向)
        Vector3f direction = new Vector3f(aabbCenter).sub(this.center());
        if (direction.dot(mtvAxis) < 0) {
            mtvAxis.negate();
        }

        return mtvAxis.mul(minOverlap);
    }

    private float getOverlap(Vector3f axis, Vector3f c1, Vector3f e1, Vector3f c2, Vector3f e2, Vector3f[] axes2) {
        // AABB 的投影半径
        float r1 = Math.abs(axis.x) * e1.x + Math.abs(axis.y) * e1.y + Math.abs(axis.z) * e1.z;
        // OBB 的投影半径
        float r2 = Math.abs(axis.dot(axes2[0])) * e2.x + Math.abs(axis.dot(axes2[1])) * e2.y + Math.abs(axis.dot(axes2[2])) * e2.z;
        float distance = Math.abs(new Vector3f(c1).sub(c2).dot(axis));
        return (r1 + r2) - distance;
    }

    /**
     * 获取玩家看向的某个OBB
     */
//    @Nullable
//    public static OBB getLookingObb(Player player, double range) {
//        Entity lookingEntity = TraceTool.findLookingEntity(player, range);
//        if (!(lookingEntity instanceof OBBEntity obbEntity)) {
//            return null;
//        }
//
//        // 获取玩家视线信息
//        Vec3 eyePos = player.getEyePosition(1.0f);
//        Vec3 viewVec = player.getViewVector(1.0f);
//        Vec3 lookEnd = eyePos.add(viewVec.scale(range));
//
//        OBB closestOBB = null;
//        double minDistanceSq = Double.MAX_VALUE;
//
//        for (OBB obb : obbEntity.getOBBs()) {
//            // 使用精确的射线相交检测
//            Vec3 hitPos = rayIntersect(obb, eyePos, lookEnd);
//
//            if (hitPos != null) {
//                // 计算交点到眼睛的平方距离
//                double distanceSq = eyePos.distanceToSqr(hitPos);
//
//                if (distanceSq < minDistanceSq) {
//                    minDistanceSq = distanceSq;
//                    closestOBB = obb;
//                }
//            }
//        }
//
//        return closestOBB;
//    }

    @Nullable
    public static Vec3 rayIntersect(OBB obb, Vec3 start, Vec3 end) {
        // 获取 OBB 信息
        Vec3 center = new Vec3(obb.center());
        Vec3 extents = new Vec3(obb.extents());
        Quaternionf rotation = obb.rotation();

        // 计算逆旋转
        Quaternionf inverse = new Quaternionf(rotation).conjugate();

        // 转换起点和终点到局部坐标系
        Vector3f localStart = toLocal(obb, start);
        Vector3f localEnd = toLocal(obb, end);

        // 定义 OBB 的 AABB（在局部坐标系中）
        float minX = (float) -extents.x, minY = (float) -extents.y, minZ = (float) -extents.z;
        float maxX = (float) extents.x, maxY = (float) extents.y, maxZ = (float) extents.z;

        // 使用 JOML 的相交检测
        Vector2f result = new Vector2f();
        boolean intersects = Intersectionf.intersectRayAab(
                localStart.x, localStart.y, localStart.z,
                localEnd.x - localStart.x, localEnd.y - localStart.y, localEnd.z - localStart.z,
                minX, minY, minZ,
                maxX, maxY, maxZ,
                result
        );

        if (intersects) {
            float t = result.x; // 交点参数
            Vector3f localHit = new Vector3f(
                    localStart.x + t * (localEnd.x - localStart.x),
                    localStart.y + t * (localEnd.y - localStart.y),
                    localStart.z + t * (localEnd.z - localStart.z)
            );

            // 转换回世界坐标系
            rotation.transform(localHit);
            return new Vec3(localHit.x + center.x, localHit.y + center.y, localHit.z + center.z);
        }
        return null;
    }

    // 将世界坐标点转换到 OBB 局部坐标系
    private static Vector3f toLocal(OBB obb, Vec3 worldPoint) {
        // 获取 OBB 信息
        Vec3 center = new Vec3(obb.center());
        Quaternionf rotation = obb.rotation();
        Quaternionf inverse = new Quaternionf(rotation).conjugate();

        // 计算相对于中心的向量
        Vector3f relative = new Vector3f(
                (float) (worldPoint.x - center.x),
                (float) (worldPoint.y - center.y),
                (float) (worldPoint.z - center.z)
        );

        // 应用逆旋转（世界坐标 -> 局部坐标）
        inverse.transform(relative);
        return relative;
    }

    public OBB copy() {
        return new OBB(
                new Vector3f(center),
                new Vector3f(extents),
                new Quaternionf(rotation)
        );
    }
}
