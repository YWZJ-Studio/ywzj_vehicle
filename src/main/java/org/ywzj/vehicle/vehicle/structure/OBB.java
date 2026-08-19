package org.ywzj.vehicle.vehicle.structure;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockBone;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockCube;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.*;
import org.joml.Math;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

import java.util.*;

/**
 * Codes based on @AnECanSaiTin's <a href="https://github.com/AnECanSaiTin/HitboxAPI">HitboxAPI</a>
 *
 * <p>The centre is held in doubles and mirrored into a float vector. A hull's centre is a world
 * coordinate, and float carries about 0.008 blocks of resolution at 100k and 0.0625 at a million,
 * which is coarser than the sweep's own skin and contact margin. Reads of {@link #center()} stay
 * float because callers use it for hull-relative work, but everything that turns a hull position
 * into a world query goes through the doubles.
 *
 * <p>The float vector is a mirror, not a second copy of the truth: write through
 * {@link #setCenter} or {@link #translate}, never into the vector {@link #center()} hands back.
 */
public final class OBB {

    private final Vector3f center;
    private final Vector3f extents;
    private final Quaternionf rotation;

    /** Authoritative centre. The float vector above is kept in step with these. */
    private double cx, cy, cz;

    public OBB(Vector3f center, Vector3f extents, Quaternionf rotation) {
        this.center = center;
        this.extents = extents;
        this.rotation = rotation;
        this.cx = center.x;
        this.cy = center.y;
        this.cz = center.z;
    }

    public Vector3f center() {
        return center;
    }

    public Vector3f extents() {
        return extents;
    }

    public Quaternionf rotation() {
        return rotation;
    }

    public double centerX() {
        return cx;
    }

    public double centerY() {
        return cy;
    }

    public double centerZ() {
        return cz;
    }

    public void setCenter(Vector3f center) {
        setCenter(center.x, center.y, center.z);
    }

    public void setCenter(Vec3 center) {
        setCenter(center.x, center.y, center.z);
    }

    public void setCenter(double x, double y, double z) {
        this.cx = x;
        this.cy = y;
        this.cz = z;
        this.center.set((float) x, (float) y, (float) z);
    }

    /** Copies another hull's centre at full precision. */
    public void setCenter(OBB source) {
        setCenter(source.cx, source.cy, source.cz);
    }

    /** Shifts the centre; substeps accumulate here rather than in the float mirror. */
    public void translate(double dx, double dy, double dz) {
        setCenter(cx + dx, cy + dy, cz + dz);
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

    public static AABB toAABB(List<OBB> obbs) {
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
        for (OBB obb : obbs) {
            Vector3f[] vertices = obb.getVertices();
            for (Vector3f v : vertices) {
                if (v.x < minX) minX = v.x;
                if (v.y < minY) minY = v.y;
                if (v.z < minZ) minZ = v.z;
                if (v.x > maxX) maxX = v.x;
                if (v.y > maxY) maxY = v.y;
                if (v.z > maxZ) maxZ = v.z;
            }
        }
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * Separating axis test against an axis-aligned box, given the OBB rotation as a pre-extracted matrix.
     */
    public static boolean intersectsBox(Matrix3f basis, float cx, float cy, float cz,
                                        Vector3f extents,
                                        double minX, double minY, double minZ,
                                        double maxX, double maxY, double maxZ) {
        return Intersectionf.testObOb(
                cx, cy, cz,
                basis.m00(), basis.m01(), basis.m02(),
                basis.m10(), basis.m11(), basis.m12(),
                basis.m20(), basis.m21(), basis.m22(),
                extents.x, extents.y, extents.z,
                (float) ((minX + maxX) * 0.5), (float) ((minY + maxY) * 0.5),
                (float) ((minZ + maxZ) * 0.5),
                1, 0, 0,
                0, 1, 0,
                0, 0, 1,
                (float) ((maxX - minX) * 0.5), (float) ((maxY - minY) * 0.5),
                (float) ((maxZ - minZ) * 0.5));
    }

    /**
     * Per-pose precomputation for OBB-versus-AABB separating axis test; reused across multiple boxes.
     */
    public static final class SatFrame {

        private static final float EPS = 1.0e-6f;

        private final Matrix3f basis = new Matrix3f();

        /** Rows of the hull's rotation: r[i][j] is component i of hull axis j. */
        public float r00, r01, r02, r10, r11, r12, r20, r21, r22;
        /** Absolute values of the above, nudged by an epsilon so parallel axes stay conservative. */
        public float a00, a01, a02, a10, a11, a12, a20, a21, a22;

        public SatFrame set(Quaternionf rotation) {
            Matrix3f m = rotation.get(basis);
            r00 = m.m00(); r01 = m.m10(); r02 = m.m20();
            r10 = m.m01(); r11 = m.m11(); r12 = m.m21();
            r20 = m.m02(); r21 = m.m12(); r22 = m.m22();
            a00 = Math.abs(r00) + EPS; a01 = Math.abs(r01) + EPS; a02 = Math.abs(r02) + EPS;
            a10 = Math.abs(r10) + EPS; a11 = Math.abs(r11) + EPS; a12 = Math.abs(r12) + EPS;
            a20 = Math.abs(r20) + EPS; a21 = Math.abs(r21) + EPS; a22 = Math.abs(r22) + EPS;
            return this;
        }

        public Matrix3f basis() {
            return basis;
        }

    }


    public static boolean intersectsBox(SatFrame f, double cx, double cy, double cz,
                                        Vector3f extents,
                                        double minX, double minY, double minZ,
                                        double maxX, double maxY, double maxZ) {
        float h0 = (float) ((maxX - minX) * 0.5);
        float h1 = (float) ((maxY - minY) * 0.5);
        float h2 = (float) ((maxZ - minZ) * 0.5);
        // Subtract in double, narrow after. Both terms are world coordinates and their difference
        // is hull-sized, so doing it the other way round spends the whole float mantissa on a
        // magnitude that cancels out.
        float t0 = (float) ((minX + maxX) * 0.5 - cx);
        float t1 = (float) ((minY + maxY) * 0.5 - cy);
        float t2 = (float) ((minZ + maxZ) * 0.5 - cz);
        float e0 = extents.x;
        float e1 = extents.y;
        float e2 = extents.z;

        // World axes.
        if (Math.abs(t0) > h0 + e0 * f.a00 + e1 * f.a01 + e2 * f.a02) return false;
        if (Math.abs(t1) > h1 + e0 * f.a10 + e1 * f.a11 + e2 * f.a12) return false;
        if (Math.abs(t2) > h2 + e0 * f.a20 + e1 * f.a21 + e2 * f.a22) return false;

        // Hull axes.
        if (Math.abs(t0 * f.r00 + t1 * f.r10 + t2 * f.r20)
                > e0 + h0 * f.a00 + h1 * f.a10 + h2 * f.a20) return false;
        if (Math.abs(t0 * f.r01 + t1 * f.r11 + t2 * f.r21)
                > e1 + h0 * f.a01 + h1 * f.a11 + h2 * f.a21) return false;
        if (Math.abs(t0 * f.r02 + t1 * f.r12 + t2 * f.r22)
                > e2 + h0 * f.a02 + h1 * f.a12 + h2 * f.a22) return false;

        // Cross axes, world axis i by hull axis j.
        if (Math.abs(t2 * f.r10 - t1 * f.r20)
                > h1 * f.a20 + h2 * f.a10 + e1 * f.a02 + e2 * f.a01) return false;
        if (Math.abs(t2 * f.r11 - t1 * f.r21)
                > h1 * f.a21 + h2 * f.a11 + e2 * f.a00 + e0 * f.a02) return false;
        if (Math.abs(t2 * f.r12 - t1 * f.r22)
                > h1 * f.a22 + h2 * f.a12 + e0 * f.a01 + e1 * f.a00) return false;
        if (Math.abs(t0 * f.r20 - t2 * f.r00)
                > h2 * f.a00 + h0 * f.a20 + e1 * f.a12 + e2 * f.a11) return false;
        if (Math.abs(t0 * f.r21 - t2 * f.r01)
                > h2 * f.a01 + h0 * f.a21 + e2 * f.a10 + e0 * f.a12) return false;
        if (Math.abs(t0 * f.r22 - t2 * f.r02)
                > h2 * f.a02 + h0 * f.a22 + e0 * f.a11 + e1 * f.a10) return false;
        if (Math.abs(t1 * f.r00 - t0 * f.r10)
                > h0 * f.a10 + h1 * f.a00 + e1 * f.a22 + e2 * f.a21) return false;
        if (Math.abs(t1 * f.r01 - t0 * f.r11)
                > h0 * f.a11 + h1 * f.a01 + e2 * f.a20 + e0 * f.a22) return false;
        return !(Math.abs(t1 * f.r02 - t0 * f.r12)
                > h0 * f.a12 + h1 * f.a02 + e0 * f.a21 + e1 * f.a20);
    }

    public record CubeOBB(BedrockBone bone, BedrockCube cube, OBB obb) {}

    public static List<CubeOBB> getOBBsFromBone(BedrockBone bone, AbstractVehicle vehicle, HashSet<BedrockBone> namedBones) {
        if (bone == null) {
            return Collections.emptyList();
        }
        List<CubeOBB> cubeOBBS = new ArrayList<>();

        Matrix4f globalMatrix = new Matrix4f();
        for(BedrockBone parent = bone; parent != null; parent = parent.parent) {
            globalMatrix.scaleLocal(parent.xScale, parent.yScale, parent.zScale);
            globalMatrix.rotateLocal(parent.rotation);
            globalMatrix.translateLocal(parent.x / 16.0F, parent.y / 16.0F, parent.z / 16.0F);
        }
        globalMatrix.rotateLocal(vehicle.rotYXZ());
        globalMatrix.translateLocal((float) vehicle.position().x, (float) vehicle.position().y, (float) vehicle.position().z);

        Quaternionf globalRotation = new Quaternionf();
        globalMatrix.getUnnormalizedRotation(globalRotation);
        Vector3f globalScale = new Vector3f();
        globalMatrix.getScale(globalScale);

        for (BedrockCube cube : bone.cubes) {
            float lx = cube.x();
            float ly = cube.y();
            float lz = cube.z();
            float lw = cube.width();
            float lh = cube.height();
            float ld = cube.depth();
            Vector3f localCenter = new Vector3f(
                    lx + lw / 2.0f,
                    ly + lh / 2.0f,
                    lz + ld / 2.0f
            );
            Vector3f worldCenter = new Vector3f();
            globalMatrix.transformPosition(localCenter, worldCenter);
            Vector3f worldExtents = new Vector3f(
                    (lw / 2.0f) * java.lang.Math.abs(globalScale.x),
                    (lh / 2.0f) * java.lang.Math.abs(globalScale.y),
                    (ld / 2.0f) * java.lang.Math.abs(globalScale.z)
            );
            cubeOBBS.add(new CubeOBB(bone, cube, new OBB(worldCenter, worldExtents, new Quaternionf(globalRotation))));
        }

        bone.getChildren().stream()
                .filter(child -> !namedBones.contains(child))
                .forEach(child -> cubeOBBS.addAll(getOBBsFromBone(child, vehicle, namedBones)));
        return cubeOBBS;
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
        return worldToLocal(worldPoint, axes, new Vector3f());
    }

    // World to local, writing into dest to avoid allocation; dest may alias worldPoint.
    public Vector3f worldToLocal(Vector3f worldPoint, Vector3f[] axes, Vector3f dest) {
        float rx = worldPoint.x - center.x;
        float ry = worldPoint.y - center.y;
        float rz = worldPoint.z - center.z;
        dest.set(
                rx * axes[0].x + ry * axes[0].y + rz * axes[0].z,
                rx * axes[1].x + ry * axes[1].y + rz * axes[1].z,
                rx * axes[2].x + ry * axes[2].y + rz * axes[2].z);
        return dest;
    }

    // 局部坐标转世界坐标
    public Vector3f localToWorld(Vector3f localPoint, Vector3f[] axes) {
        return localToWorld(localPoint, axes, new Vector3f());
    }

    /**
     * Local to world at the centre's full precision, into a caller-owned length-3 scratch.
     * The rotated offset stays float because it is hull-sized; only the sum needs the range.
     */
    public void localToWorld(Vector3f localPoint, Vector3f[] axes, double[] dest) {
        float lx = localPoint.x, ly = localPoint.y, lz = localPoint.z;
        dest[0] = cx + (axes[0].x * lx + axes[1].x * ly + axes[2].x * lz);
        dest[1] = cy + (axes[0].y * lx + axes[1].y * ly + axes[2].y * lz);
        dest[2] = cz + (axes[0].z * lx + axes[1].z * ly + axes[2].z * lz);
    }

    /** World to local from a double world point; the relative vector is formed before narrowing. */
    public Vector3f worldToLocal(double wx, double wy, double wz, Vector3f[] axes, Vector3f dest) {
        float rx = (float) (wx - cx);
        float ry = (float) (wy - cy);
        float rz = (float) (wz - cz);
        dest.set(
                rx * axes[0].x + ry * axes[0].y + rz * axes[0].z,
                rx * axes[1].x + ry * axes[1].y + rz * axes[1].z,
                rx * axes[2].x + ry * axes[2].y + rz * axes[2].z);
        return dest;
    }

    // Local to world, writing into dest.
    // avoids the 4 temporary Vector3f allocations the other one makes per call.
    public Vector3f localToWorld(Vector3f localPoint, Vector3f[] axes, Vector3f dest) {
        float lx = localPoint.x, ly = localPoint.y, lz = localPoint.z;
        dest.set(
                center.x + axes[0].x * lx + axes[1].x * ly + axes[2].x * lz,
                center.y + axes[0].y * lx + axes[1].y * ly + axes[2].y * lz,
                center.z + axes[0].z * lx + axes[1].z * ly + axes[2].z * lz
        );
        return dest;
    }

    public OBB inflate(float amount) {
        Vector3f newExtents = new Vector3f(extents).add(amount, amount, amount);
        OBB inflated = new OBB(new Vector3f(center), newExtents, rotation);
        inflated.setCenter(cx, cy, cz);
        return inflated;
    }

    public OBB inflate(float x, float y, float z) {
        Vector3f newExtents = new Vector3f(extents).add(x, y, z);
        OBB inflated = new OBB(new Vector3f(center), newExtents, rotation);
        inflated.setCenter(cx, cy, cz);
        return inflated;
    }

    public OBB move(Vec3 vec3) {
        OBB moved = new OBB(new Vector3f(center), extents, rotation);
        moved.setCenter(cx + vec3.x, cy + vec3.y, cz + vec3.z);
        return moved;
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

    public static float mtv(SatFrame f, double cx, double cy, double cz, Vector3f extents,
                            double minX, double minY, double minZ,
                            double maxX, double maxY, double maxZ, Vector3f dest) {
        float h0 = (float) ((maxX - minX) * 0.5);
        float h1 = (float) ((maxY - minY) * 0.5);
        float h2 = (float) ((maxZ - minZ) * 0.5);
        float t0 = (float) ((minX + maxX) * 0.5 - cx);
        float t1 = (float) ((minY + maxY) * 0.5 - cy);
        float t2 = (float) ((minZ + maxZ) * 0.5 - cz);
        float e0 = extents.x;
        float e1 = extents.y;
        float e2 = extents.z;

        float best = Float.MAX_VALUE;
        float bestX = 0, bestY = 0, bestZ = 0;

        // World axes: unit length, depth is the raw overlap.
        float o = h0 + e0 * f.a00 + e1 * f.a01 + e2 * f.a02 - Math.abs(t0);
        if (o <= 0) { dest.set(0); return 0; }
        if (o < best) { best = o; bestX = 1; bestY = 0; bestZ = 0; }
        o = h1 + e0 * f.a10 + e1 * f.a11 + e2 * f.a12 - Math.abs(t1);
        if (o <= 0) { dest.set(0); return 0; }
        if (o < best) { best = o; bestX = 0; bestY = 1; bestZ = 0; }
        o = h2 + e0 * f.a20 + e1 * f.a21 + e2 * f.a22 - Math.abs(t2);
        if (o <= 0) { dest.set(0); return 0; }
        if (o < best) { best = o; bestX = 0; bestY = 0; bestZ = 1; }

        // Hull axes: also unit length.
        o = e0 + h0 * f.a00 + h1 * f.a10 + h2 * f.a20
                - Math.abs(t0 * f.r00 + t1 * f.r10 + t2 * f.r20);
        if (o <= 0) { dest.set(0); return 0; }
        if (o < best) { best = o; bestX = f.r00; bestY = f.r10; bestZ = f.r20; }
        o = e1 + h0 * f.a01 + h1 * f.a11 + h2 * f.a21
                - Math.abs(t0 * f.r01 + t1 * f.r11 + t2 * f.r21);
        if (o <= 0) { dest.set(0); return 0; }
        if (o < best) { best = o; bestX = f.r01; bestY = f.r11; bestZ = f.r21; }
        o = e2 + h0 * f.a02 + h1 * f.a12 + h2 * f.a22
                - Math.abs(t0 * f.r02 + t1 * f.r12 + t2 * f.r22);
        if (o <= 0) { dest.set(0); return 0; }
        if (o < best) { best = o; bestX = f.r02; bestY = f.r12; bestZ = f.r22; }

        // Cross axes: depth is overlap divided by axis length.
        for (int j = 0; j < 3; j++) {
            float r0j = j == 0 ? f.r00 : j == 1 ? f.r01 : f.r02;
            float r1j = j == 0 ? f.r10 : j == 1 ? f.r11 : f.r12;
            float r2j = j == 0 ? f.r20 : j == 1 ? f.r21 : f.r22;
            float a0j = j == 0 ? f.a00 : j == 1 ? f.a01 : f.a02;
            float a1j = j == 0 ? f.a10 : j == 1 ? f.a11 : f.a12;
            float a2j = j == 0 ? f.a20 : j == 1 ? f.a21 : f.a22;
            int j1 = (j + 1) % 3;
            int j2 = (j + 2) % 3;
            float ej1 = j1 == 0 ? e0 : j1 == 1 ? e1 : e2;
            float ej2 = j2 == 0 ? e0 : j2 == 1 ? e1 : e2;

            // i = 0: axis (0, -u_j.z, u_j.y)
            float lenSq = r1j * r1j + r2j * r2j;
            if (lenSq > 1.0e-6f) {
                float aj2 = rowAbs(f, 0, j2);
                float aj1 = rowAbs(f, 0, j1);
                o = h1 * a2j + h2 * a1j + ej1 * aj2 + ej2 * aj1 - Math.abs(t2 * r1j - t1 * r2j);
                if (o <= 0) { dest.set(0); return 0; }
                float len = (float) Math.sqrt(lenSq);
                float depth = o / len;
                if (depth < best) { best = depth; bestX = 0; bestY = -r2j / len; bestZ = r1j / len; }
            }
            // i = 1: axis (u_j.z, 0, -u_j.x)
            lenSq = r0j * r0j + r2j * r2j;
            if (lenSq > 1.0e-6f) {
                float aj2 = rowAbs(f, 1, j2);
                float aj1 = rowAbs(f, 1, j1);
                o = h2 * a0j + h0 * a2j + ej1 * aj2 + ej2 * aj1 - Math.abs(t0 * r2j - t2 * r0j);
                if (o <= 0) { dest.set(0); return 0; }
                float len = (float) Math.sqrt(lenSq);
                float depth = o / len;
                if (depth < best) { best = depth; bestX = r2j / len; bestY = 0; bestZ = -r0j / len; }
            }
            // i = 2: axis (-u_j.y, u_j.x, 0)
            lenSq = r0j * r0j + r1j * r1j;
            if (lenSq > 1.0e-6f) {
                float aj2 = rowAbs(f, 2, j2);
                float aj1 = rowAbs(f, 2, j1);
                o = h0 * a1j + h1 * a0j + ej1 * aj2 + ej2 * aj1 - Math.abs(t1 * r0j - t0 * r1j);
                if (o <= 0) { dest.set(0); return 0; }
                float len = (float) Math.sqrt(lenSq);
                float depth = o / len;
                if (depth < best) { best = depth; bestX = -r1j / len; bestY = r0j / len; bestZ = 0; }
            }
        }

        // Point from the OBB toward the box, matching calculateMTV's convention.
        if (t0 * bestX + t1 * bestY + t2 * bestZ < 0) {
            bestX = -bestX;
            bestY = -bestY;
            bestZ = -bestZ;
        }
        dest.set(bestX * best, bestY * best, bestZ * best);
        return best;
    }

    private static float rowAbs(SatFrame f, int i, int j) {
        return i == 0 ? (j == 0 ? f.a00 : j == 1 ? f.a01 : f.a02)
                : i == 1 ? (j == 0 ? f.a10 : j == 1 ? f.a11 : f.a12)
                : (j == 0 ? f.a20 : j == 1 ? f.a21 : f.a22);
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
        OBB copy = new OBB(
                new Vector3f(center),
                new Vector3f(extents),
                new Quaternionf(rotation)
        );
        copy.setCenter(cx, cy, cz);
        return copy;
    }

}
