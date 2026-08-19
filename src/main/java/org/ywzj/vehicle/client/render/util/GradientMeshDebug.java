package org.ywzj.vehicle.client.render.util;

import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.collision.ChunkCollisionCache;
import org.ywzj.vehicle.vehicle.collision.SweptHull;
import org.ywzj.vehicle.vehicle.structure.OBB;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The ground as the climb logic reads it: surface height per column and traversability verdict.
 * A one-block staircase and two-block riser show as identical collision boxes but different verdicts.
 * Heights are sampled from the collision cache, so slabs read as half a block.
 */
@OnlyIn(Dist.CLIENT)
public final class GradientMeshDebug {

    /** Rise threshold below which a cell is flat rather than a step, in blocks. */
    private static final double FLAT = 0.05;

    /** Distance above reference height where a surface still counts as ground. */
    private static final double CEILING = 3.0;

    /** Maximum search depth below reference height before giving up on finding ground. */
    private static final double FLOOR = 8.0;

    public enum Verdict {

        /** Level enough to drive across without the climb path doing anything. */
        FLAT,
        /** A step the vehicle can ride up. How steep is carried separately, in the rise. */
        SLOPE,
        /** Taller than maxUpStep; climb refuses it. */
        WALL

    }

    /**
     * @param top the drivable surface height
     * @param rise tallest upward step to a neighbouring column
     */
    public record Cell(int x, int z, double top, double rise, Verdict verdict) {}

    /**
     * @param sweepHull the hull swept against the world; everything below is skirt the vehicle rides over
     */
    public record Field(List<Cell> cells, OBB sweepHull, double maxUpStep, double climbGradient) {}

    private GradientMeshDebug() {}

    /**
     * Builds the field around a vehicle.
     *
     * @param radius columns either side of the vehicle to sample
     */
    public static Field around(Level level, AbstractVehicle vehicle, int radius) {
        if (vehicle.getMainCubeOBB() == null) {
            return null;
        }
        double refY = vehicle.getY();
        int centreX = Mth.floor(vehicle.getX());
        int centreZ = Mth.floor(vehicle.getZ());
        int span = radius * 2 + 1;

        AABB region = new AABB(centreX - radius, refY - FLOOR, centreZ - radius,
                centreX + radius + 1, refY + CEILING, centreZ + radius + 1);
        ChunkCollisionCache cache = ChunkCollisionCache.of(level);
        List<AABB> boxes = new ArrayList<>();
        cache.collectBoxes(region, boxes);

        // Build heights as a flat array first; verdicts depend on neighbours' heights.
        double[] tops = new double[span * span];
        Arrays.fill(tops, Double.NaN);
        for (AABB box : boxes) {
            if (box.maxY > refY + CEILING || box.maxY < refY - FLOOR) {
                continue;
            }
            int fromX = Math.max(centreX - radius, Mth.floor(box.minX));
            int toX = Math.min(centreX + radius, Mth.floor(box.maxX - 1.0e-6));
            int fromZ = Math.max(centreZ - radius, Mth.floor(box.minZ));
            int toZ = Math.min(centreZ + radius, Mth.floor(box.maxZ - 1.0e-6));
            for (int x = fromX; x <= toX; x++) {
                for (int z = fromZ; z <= toZ; z++) {
                    int i = index(x - centreX + radius, z - centreZ + radius, span);
                    if (Double.isNaN(tops[i]) || box.maxY > tops[i]) {
                        tops[i] = box.maxY;
                    }
                }
            }
        }

        double maxUpStep = vehicle.maxUpStep();
        List<Cell> cells = new ArrayList<>();
        for (int lx = 0; lx < span; lx++) {
            for (int lz = 0; lz < span; lz++) {
                double top = tops[index(lx, lz, span)];
                if (Double.isNaN(top)) {
                    continue;
                }
                // Rise to neighbours, upward only; the verdict marks the low cell asking "can I leave here".
                double rise = 0;
                rise = Math.max(rise, step(tops, lx - 1, lz, span, top));
                rise = Math.max(rise, step(tops, lx + 1, lz, span, top));
                rise = Math.max(rise, step(tops, lx, lz - 1, span, top));
                rise = Math.max(rise, step(tops, lx, lz + 1, span, top));
                Verdict verdict = rise > maxUpStep ? Verdict.WALL
                        : rise >= FLAT ? Verdict.SLOPE
                        : Verdict.FLAT;
                cells.add(new Cell(centreX - radius + lx, centreZ - radius + lz, top, rise, verdict));
            }
        }

        OBB sweepHull = SweptHull.climbHull(vehicle.getMainCubeOBB().obb(), vehicle.sweepSkirt(),
                new OBB(new org.joml.Vector3f(), new org.joml.Vector3f(), new org.joml.Quaternionf()));
        return new Field(cells, sweepHull, maxUpStep, vehicle.physicsEngine.climbGradient);
    }

    private static double step(double[] tops, int lx, int lz, int span, double from) {
        if (lx < 0 || lz < 0 || lx >= span || lz >= span) {
            return 0;
        }
        double neighbour = tops[index(lx, lz, span)];
        return Double.isNaN(neighbour) ? 0 : Math.max(0, neighbour - from);
    }

    private static int index(int lx, int lz, int span) {
        return lx * span + lz;
    }

}
