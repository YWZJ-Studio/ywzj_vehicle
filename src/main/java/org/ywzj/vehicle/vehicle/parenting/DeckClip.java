package org.ywzj.vehicle.vehicle.parenting;

/**
 * The box arithmetic behind deck walking, using only primitives.
 * Follows vanilla's axis collision including its rule that overlapping boxes do not block.
 */
public final class DeckClip {

    /**
     * Gap left between a rider and what it lands on. Vanilla's shape collision uses the same value.
     */
    public static final double EPSILON = 1.0E-7;

    /**
     * How far inside a deck box a rider may be and still be treated as resting on it.
     * Compensates for frame coordinate noise from float quaternion conversions on large hulls.
     * A box inside the skin has downward movement clipped to zero without being lifted.
     */
    public static final double SKIN = 0.005;

    /** Movement below this on an axis is not worth a pass over the boxes. */
    private static final double MIN_MOVE = 1.0E-9;

    private DeckClip() {}

    /**
     * Compacts the deck boxes a moving box could possibly touch into dest.
     * Filters to the swept box grown upward by lift, accounting for grounded movement on tilted hulls.
     * @return the number of boxes written, six floats each
     */
    public static int narrow(float[] boxes, int count,
                             double cx, double cy, double cz,
                             double hx, double hy, double hz,
                             double mx, double my, double mz,
                             double groundedX, double groundedZ,
                             double lift, float[] dest) {
        // Compute bounds in double to match clip precision; SKIN accounts for
        // clip reach.
        double reachMinX = Math.min(Math.min(mx, groundedX), 0);
        double reachMaxX = Math.max(Math.max(mx, groundedX), 0);
        double reachMinZ = Math.min(Math.min(mz, groundedZ), 0);
        double reachMaxZ = Math.max(Math.max(mz, groundedZ), 0);
        double minX = cx - hx + reachMinX - SKIN;
        double maxX = cx + hx + reachMaxX + SKIN;
        double minY = cy - hy + Math.min(my, 0) - SKIN;
        double maxY = cy + hy + Math.max(my, 0) + lift + SKIN;
        double minZ = cz - hz + reachMinZ - SKIN;
        double maxZ = cz + hz + reachMaxZ + SKIN;
        int found = 0;
        for (int i = 0; i < count; i++) {
            int o = i * 6;
            if (maxX > boxes[o] && minX < boxes[o + 3]
                    && maxY > boxes[o + 1] && minY < boxes[o + 4]
                    && maxZ > boxes[o + 2] && minZ < boxes[o + 5]) {
                System.arraycopy(boxes, o, dest, found * 6, 6);
                found++;
            }
        }
        return found;
    }

    /**
     * Clips movement against the deck. Vertical first, then horizontal;
     * step-up via lift and retry. Grounded movement on downward block.
     * @param wantX full local movement
     * @param groundedX horizontal intent excluding gravity
     * @param out allowed movement as {x, y, z}
     * @return true when downward blocked
     */
    public static boolean sweep(float[] boxes, int count,
                                double cx, double cy, double cz,
                                double hx, double hy, double hz,
                                double wantX, double wantY, double wantZ,
                                double groundedX, double groundedZ,
                                double maxUpStep, boolean onGround,
                                double[] out) {
        out[0] = wantX;
        out[1] = wantY;
        out[2] = wantZ;
        if (count == 0 || !anyBoxNear(boxes, count, cx, cy, cz, hx, hy, hz, wantX, wantY, wantZ)) {
            return false;
        }

        double gotY = clipY(boxes, count, cx, cy, cz, hx, hy, hz, wantY);
        boolean blockedDown = wantY < 0 && gotY > wantY;
        if (blockedDown) {
            wantX = groundedX;
            wantZ = groundedZ;
        }
        double runX = cx;
        double runY = cy + gotY;
        double runZ = cz;
        double gotX;
        double gotZ;
        if (Math.abs(wantX) >= Math.abs(wantZ)) {
            gotX = clipX(boxes, count, runX, runY, runZ, hx, hy, hz, wantX);
            runX += gotX;
            gotZ = clipZ(boxes, count, runX, runY, runZ, hx, hy, hz, wantZ);
        } else {
            gotZ = clipZ(boxes, count, runX, runY, runZ, hx, hy, hz, wantZ);
            runZ += gotZ;
            gotX = clipX(boxes, count, runX, runY, runZ, hx, hy, hz, wantX);
        }

        boolean blockedSide = gotX != wantX || gotZ != wantZ;

        if (blockedSide && maxUpStep > 0 && (blockedDown || onGround)) {
            double baseY = cy + gotY;
            double lift = clipY(boxes, count, cx, baseY, cz, hx, hy, hz, maxUpStep);
            if (lift > EPSILON) {
                double stepX;
                double stepZ;
                double sx = cx;
                double sy = baseY + lift;
                double sz = cz;
                if (Math.abs(wantX) >= Math.abs(wantZ)) {
                    stepX = clipX(boxes, count, sx, sy, sz, hx, hy, hz, wantX);
                    sx += stepX;
                    stepZ = clipZ(boxes, count, sx, sy, sz, hx, hy, hz, wantZ);
                    sz += stepZ;
                } else {
                    stepZ = clipZ(boxes, count, sx, sy, sz, hx, hy, hz, wantZ);
                    sz += stepZ;
                    stepX = clipX(boxes, count, sx, sy, sz, hx, hy, hz, wantX);
                    sx += stepX;
                }
                if (stepX * stepX + stepZ * stepZ > gotX * gotX + gotZ * gotZ) {
                    // Step-up succeeded; drop back and apply the result.
                    double drop = clipY(boxes, count, sx, sy, sz, hx, hy, hz, -lift);
                    gotX = stepX;
                    gotZ = stepZ;
                    gotY += lift + drop;
                    blockedDown = blockedDown || drop > -lift;
                }
            }
        }

        out[0] = gotX;
        out[1] = gotY;
        out[2] = gotZ;
        return blockedDown;
    }

    /**
     * Pushes a box out of any deck geometry it is inside, along the shallowest axis.
     * Resolves along the shallowest overlap, preferring up when vertical and horizontal are equal.
     * @param out receives the correction as {x, y, z}
     * @return true when a correction was needed
     */
    public static boolean depenetrate(float[] boxes, int count,
                                      double cx, double cy, double cz,
                                      double hx, double hy, double hz,
                                      int maxPasses, double[] out) {
        out[0] = 0;
        out[1] = 0;
        out[2] = 0;
        for (int pass = 0; pass < maxPasses; pass++) {
            int worst = -1;
            double worstDepth = 0;
            double pushX = 0;
            double pushY = 0;
            double pushZ = 0;
            for (int i = 0; i < count; i++) {
                int o = i * 6;
                double overlapX = Math.min(cx + hx, boxes[o + 3]) - Math.max(cx - hx, boxes[o]);
                if (overlapX <= 0) continue;
                double overlapY = Math.min(cy + hy, boxes[o + 4]) - Math.max(cy - hy, boxes[o + 1]);
                if (overlapY <= 0) continue;
                double overlapZ = Math.min(cz + hz, boxes[o + 5]) - Math.max(cz - hz, boxes[o + 2]);
                if (overlapZ <= 0) continue;
                // Resolve deepest overlap first; shallower ones often vanish after.
                double depth = Math.min(overlapY, Math.min(overlapX, overlapZ));
                // Don't correct within SKIN; clip tolerates riders there.
                if (depth <= SKIN || depth <= worstDepth) continue;
                worst = i;
                worstDepth = depth;
                if (overlapY <= overlapX && overlapY <= overlapZ) {
                    // Up wins ties; push vertical first.
                    boolean up = cy >= (boxes[o + 1] + boxes[o + 4]) * 0.5;
                    pushX = 0;
                    pushY = up ? overlapY + EPSILON : -(overlapY + EPSILON);
                    pushZ = 0;
                } else if (overlapX <= overlapZ) {
                    boolean plus = cx >= (boxes[o] + boxes[o + 3]) * 0.5;
                    pushX = plus ? overlapX + EPSILON : -(overlapX + EPSILON);
                    pushY = 0;
                    pushZ = 0;
                } else {
                    boolean plus = cz >= (boxes[o + 2] + boxes[o + 5]) * 0.5;
                    pushX = 0;
                    pushY = 0;
                    pushZ = plus ? overlapZ + EPSILON : -(overlapZ + EPSILON);
                }
            }
            if (worst < 0) {
                break;
            }
            cx += pushX;
            cy += pushY;
            cz += pushZ;
            out[0] += pushX;
            out[1] += pushY;
            out[2] += pushZ;
        }
        return out[0] != 0 || out[1] != 0 || out[2] != 0;
    }

    /** True when any deck box lies within the box's swept extent. */
    public static boolean anyBoxNear(float[] boxes, int count,
                                     double cx, double cy, double cz,
                                     double hx, double hy, double hz,
                                     double mx, double my, double mz) {
        double minX = cx - hx + Math.min(mx, 0);
        double maxX = cx + hx + Math.max(mx, 0);
        double minY = cy - hy + Math.min(my, 0);
        double maxY = cy + hy + Math.max(my, 0);
        double minZ = cz - hz + Math.min(mz, 0);
        double maxZ = cz + hz + Math.max(mz, 0);
        for (int i = 0; i < count; i++) {
            int o = i * 6;
            if (maxX > boxes[o] && minX < boxes[o + 3]
                    && maxY > boxes[o + 1] && minY < boxes[o + 4]
                    && maxZ > boxes[o + 2] && minZ < boxes[o + 5]) {
                return true;
            }
        }
        return false;
    }

    public static double clipY(float[] boxes, int count, double cx, double cy, double cz,
                               double hx, double hy, double hz, double move) {
        if (Math.abs(move) < MIN_MOVE) {
            return move;
        }
        double minX = cx - hx, maxX = cx + hx;
        double minZ = cz - hz, maxZ = cz + hz;
        double minY = cy - hy, maxY = cy + hy;
        for (int i = 0; i < count; i++) {
            int o = i * 6;
            if (maxX <= boxes[o] || minX >= boxes[o + 3] || maxZ <= boxes[o + 2] || minZ >= boxes[o + 5]) {
                continue;
            }
            if (move > 0 && maxY <= boxes[o + 1] + SKIN) {
                double gap = boxes[o + 1] - maxY - EPSILON;
                if (gap < move) move = Math.max(gap, 0);
            } else if (move < 0 && minY >= boxes[o + 4] - SKIN) {
                double gap = boxes[o + 4] - minY + EPSILON;
                if (gap > move) move = Math.min(gap, 0);
            }
        }
        return move;
    }

    public static double clipX(float[] boxes, int count, double cx, double cy, double cz,
                               double hx, double hy, double hz, double move) {
        if (Math.abs(move) < MIN_MOVE) {
            return move;
        }
        double minY = cy - hy, maxY = cy + hy;
        double minZ = cz - hz, maxZ = cz + hz;
        double minX = cx - hx, maxX = cx + hx;
        for (int i = 0; i < count; i++) {
            int o = i * 6;
            if (maxY <= boxes[o + 1] || minY >= boxes[o + 4] || maxZ <= boxes[o + 2] || minZ >= boxes[o + 5]) {
                continue;
            }
            if (move > 0 && maxX <= boxes[o] + SKIN) {
                double gap = boxes[o] - maxX - EPSILON;
                if (gap < move) move = Math.max(gap, 0);
            } else if (move < 0 && minX >= boxes[o + 3] - SKIN) {
                double gap = boxes[o + 3] - minX + EPSILON;
                if (gap > move) move = Math.min(gap, 0);
            }
        }
        return move;
    }

    public static double clipZ(float[] boxes, int count, double cx, double cy, double cz,
                               double hx, double hy, double hz, double move) {
        if (Math.abs(move) < MIN_MOVE) {
            return move;
        }
        double minX = cx - hx, maxX = cx + hx;
        double minY = cy - hy, maxY = cy + hy;
        double minZ = cz - hz, maxZ = cz + hz;
        for (int i = 0; i < count; i++) {
            int o = i * 6;
            if (maxX <= boxes[o] || minX >= boxes[o + 3] || maxY <= boxes[o + 1] || minY >= boxes[o + 4]) {
                continue;
            }
            if (move > 0 && maxZ <= boxes[o + 2] + SKIN) {
                double gap = boxes[o + 2] - maxZ - EPSILON;
                if (gap < move) move = Math.max(gap, 0);
            } else if (move < 0 && minZ >= boxes[o + 5] - SKIN) {
                double gap = boxes[o + 5] - minZ + EPSILON;
                if (gap > move) move = Math.min(gap, 0);
            }
        }
        return move;
    }

}
