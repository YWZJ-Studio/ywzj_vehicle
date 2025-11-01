package org.ywzj.vehicle.util;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class VectorUtil {

    public static double fov = 70;
    public static Matrix4f modelViewMatrix = new Matrix4f();
    public static Matrix4f projectionMatrix = new Matrix4f();

    // 感谢 Minecraft-Ping-Wheel 开源
    // https://github.com/LukenSkyne/Minecraft-Ping-Wheel/blob/138295954dab9d2451ad19e16d8d413ef018a2d8/common/src/main/java/nx/pingwheel/common/helper/MathUtils.java#L15
    public static Vec3 worldToScreen(Vec3 pos) {
        var mc = Minecraft.getInstance();
        var window = mc.getWindow();
        var camera = mc.gameRenderer.getMainCamera();
        var worldPosRel = new Vector4f(camera.getPosition().reverse().add(pos).toVector3f(), 1f);
        worldPosRel.mul(modelViewMatrix);
        worldPosRel.mul(projectionMatrix);

        var depth = worldPosRel.w;

        if (depth != 0) {
            worldPosRel.div(depth);
        }

        return new Vec3(
                window.getGuiScaledWidth() * (0.5f + worldPosRel.x * 0.5f),
                window.getGuiScaledHeight() * (0.5f - worldPosRel.y * 0.5f),
                depth
        );
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void captureFov(ViewportEvent.ComputeFov event) {
        if (event.usedConfiguredFov()) {
            fov = event.getFOV();
        }
    }

    public static Vec3 hitPosition(Entity shooter, Vec3 start, Vec3 end) {
        Level level = shooter.level();
        ClipContext blockContext = new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter);
        BlockHitResult blockHit = level.clip(blockContext);
        Vec3 blockHitPos = blockHit.getLocation();
        double blockDistance = blockHitPos.distanceTo(start);
        Vec3 direction = end.subtract(start);
        AABB aabb = shooter.getBoundingBox().expandTowards(direction).inflate(1.0);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level, shooter, start, end, aabb, entity ->
                entity.isPickable()
                && !entity.isSpectator()
                && entity != shooter
                && entity != shooter.getVehicle()
                && entity.getVehicle() != shooter.getVehicle()
                && !shooter.getPassengers().contains(entity));
        Vec3 hitPoint = blockHitPos;
        if (entityHit != null) {
            AABB targetBox = entityHit.getEntity().getBoundingBox();
            Optional<Vec3> intersectOptional = targetBox.clip(start, end);
            if (intersectOptional.isPresent()) {
                Vec3 intersectPos = intersectOptional.get();
                double entityDistance = intersectPos.distanceTo(start);
                if (entityDistance < blockDistance) {
                    hitPoint = intersectPos;
                }
            }
        }
        return hitPoint;
    }

    public static Vec3 calculateViewVector(float pXRot, float pYRot) {
        float f = pXRot * ((float)Math.PI / 180F);
        float f1 = -pYRot * ((float)Math.PI / 180F);
        float f2 = Mth.cos(f1);
        float f3 = Mth.sin(f1);
        float f4 = Mth.cos(f);
        float f5 = Mth.sin(f);
        return new Vec3(f3 * f4, -f5, f2 * f4);
    }

    public static Vec3 project(Vec3 a, Vec3 b) {
        double dot = a.dot(b); // 点积
        double len2 = b.lengthSqr(); // |b|^2
        if (len2 == 0) return Vec3.ZERO; // 防止除零
        return b.scale(dot / len2);
    }

    public static Vec3 projectToPlane(Vec3 v, Vector3f[] axes, int axis1, int axis2) {
        Vector3f a = axes[axis1];
        Vector3f b = axes[axis2];

        Vec3 va = new Vec3(a.x(), a.y(), a.z());
        Vec3 vb = new Vec3(b.x(), b.y(), b.z());

        // 投影到 a
        double dotA = v.dot(va);
        double lenA2 = va.lengthSqr();
        Vec3 projA = va.scale(dotA / lenA2);

        // 投影到 b
        double dotB = v.dot(vb);
        double lenB2 = vb.lengthSqr();
        Vec3 projB = vb.scale(dotB / lenB2);

        // 平面投影 = 两个方向的和
        return projA.add(projB);
    }

    /**
     * 从一系列点构建最大凸包
     */
    public static List<Vector2f> convexHull(List<Vector2f> points) {
        if (points.size() <= 1) return new ArrayList<>(points);

        // 按 x 排序（若相等则按 y）
        List<Vector2f> sorted = new ArrayList<>(points);
        sorted.sort((a, b) -> {
            if (a.x == b.x) return Float.compare(a.y, b.y);
            return Float.compare(a.x, b.x);
        });

        List<Vector2f> lower = new ArrayList<>();
        for (Vector2f p : sorted) {
            while (lower.size() >= 2 &&
                    cross(lower.get(lower.size() - 2), lower.get(lower.size() - 1), p) <= 0) {
                lower.remove(lower.size() - 1);
            }
            lower.add(p);
        }

        List<Vector2f> upper = new ArrayList<>();
        for (int i = sorted.size() - 1; i >= 0; i--) {
            Vector2f p = sorted.get(i);
            while (upper.size() >= 2 &&
                    cross(upper.get(upper.size() - 2), upper.get(upper.size() - 1), p) <= 0) {
                upper.remove(upper.size() - 1);
            }
            upper.add(p);
        }

        // 拼接时去掉首尾重复点
        lower.remove(lower.size() - 1);
        upper.remove(upper.size() - 1);
        lower.addAll(upper);

        return lower;
    }

    /**
     * 叉积 (p1 -> p2) × (p1 -> p3)
     */
    public static float cross(Vector2f p1, Vector2f p2, Vector2f p3) {
        return (p2.x - p1.x) * (p3.y - p1.y) - (p2.y - p1.y) * (p3.x - p1.x);
    }

    /**
     * 点是否在闭包内
     */
    public static boolean isPointInPolygon(Vector2f p, List<Vector2f> polygon) {
        boolean inside = false;
        // 特殊情况：只有两个点 -> 判断点是否在这条线段上
        if (polygon.size() == 2) {
            Vector2f a = polygon.get(0);
            Vector2f b = polygon.get(1);
            // 向量叉积是否接近0（共线）
            float cross = (p.y - a.y) * (b.x - a.x) - (p.x - a.x) * (b.y - a.y);
            if (Math.abs(cross) > 1e-6) { // 不共线
                return false;
            }
            // 判断投影是否在 [a, b] 之间
            float dot = (p.x - a.x) * (b.x - a.x) + (p.y - a.y) * (b.y - a.y);
            if (dot < 0) {
                return false;
            }
            float lenSq = (b.x - a.x) * (b.x - a.x) + (b.y - a.y) * (b.y - a.y);
            return !(dot > lenSq);
        }
        // 常规情况：点在多边形内判定（射线法）
        for (int i = 0, j = polygon.size() - 1; i < polygon.size(); j = i++) {
            Vector2f vi = polygon.get(i);
            Vector2f vj = polygon.get(j);
            if (((vi.y > p.y) != (vj.y > p.y)) &&
                    (p.x < (vj.x - vi.x) * (p.y - vi.y) / (vj.y - vi.y) + vi.x)) {
                inside = !inside;
            }
        }
        return inside;
    }

    /**
     * 点p到点a与点b构成的线段的距离
     */
    public static float pointToSegmentDist(Vector2f p, Vector2f a, Vector2f b) {
        Vector2f ab = new Vector2f(b).sub(a);
        Vector2f ap = new Vector2f(p).sub(a);
        float t = ap.dot(ab) / ab.lengthSquared();
        t = Math.max(0, Math.min(1, t));
        Vector2f proj = new Vector2f(a).fma(t, ab);
        return p.distance(proj);
    }

}
