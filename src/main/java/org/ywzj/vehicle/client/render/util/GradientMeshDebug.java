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
 * The ground as the climb logic reads it: a height field, and per cell the verdict on whether a
 * vehicle could drive off it.
 * <p>
 * The collision overlay next door draws the boxes physics collides against, which answers "what is
 * there". This answers the question that has actually been causing bugs — <em>what does the vehicle
 * think it can do about what is there</em> — because the two have repeatedly disagreed. A one-block
 * staircase and a two-block riser are the same green boxes; only the verdict tells them apart. Two
 * separate defects came down to terrain the vehicle was standing on being read as a wall, and
 * neither was visible in a box view.
 * <p>
 * Sampled from the same {@link ChunkCollisionCache} boxes physics queries, so the surface heights
 * are the merged collision geometry and a slab reads as half a block, not a whole one.
 */
@OnlyIn(Dist.CLIENT)
public final class GradientMeshDebug {

    /** Rise below which a cell is flat rather than a step, in blocks. */
    private static final double FLAT = 0.05;

    /** How far above the reference height a surface can be and still count as ground to drive on. */
    private static final double CEILING = 3.0;

    /** How far below to look before giving up on a column — beyond this it is a drop, not ground. */
    private static final double FLOOR = 8.0;

    public enum Verdict {

        /** Level enough to drive across without the climb path doing anything. */
        FLAT,
        /** A step the vehicle can ride up. How steep is carried separately, in the rise. */
        SLOPE,
        /** Taller than {@code maxUpStep}: climb refuses it and the sweep stops the hull. */
        WALL

    }

    /**
     * @param top   world Y of the drivable surface in this column
     * @param rise  tallest upward step to a neighbouring column, in blocks
     */
    public record Cell(int x, int z, double top, double rise, Verdict verdict) {}

    /**
     * @param sweepHull the hull actually swept against the world — everything below it is skirt
     *                  the vehicle rides over. Terrain poking into this box is what stops it.
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

        // Surface height per column, NaN where nothing was found. Built as a flat array first
        // because the verdict for a cell depends on its neighbours, which do not exist yet while
        // the heights are still being gathered.
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
                // Rise to the neighbours, upward only — driving off a ledge is never blocked, so a
                // drop is not something the vehicle needs warning about. Marking the low cell
                // rather than the high one is deliberate: the verdict answers "can I leave here",
                // which is the question asked by a vehicle standing on it.
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
