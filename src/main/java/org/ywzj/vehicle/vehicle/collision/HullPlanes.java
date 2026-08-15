package org.ywzj.vehicle.vehicle.collision;

import org.joml.Matrix3f;
import org.joml.Vector3f;
import org.ywzj.vehicle.vehicle.structure.OBB;

/**
 * Turns nearby world boxes into the contact planes {@link PlaneSolver} consumes.
 * <p>
 * The equivalent of Box3D's {@code b3World_CollideMover} for our one shape pair: an oriented hull
 * against axis-aligned world boxes. For each box, separating-axis theorem over the fifteen
 * candidate axes gives the axis of greatest separation — or, when the two overlap, of least
 * penetration, which is the same maximum. That axis is the contact normal and the value along it
 * is the signed separation, which is exactly a half-space constraint.
 * <p>
 * <b>Boxes that are merely near, not touching, still produce planes.</b> This is Box3D's
 * speculative margin, and it is what makes the result stable: a plane that exists slightly before
 * contact lets the solver steer around an obstacle in the same step it would have hit it, instead
 * of penetrating and being pushed back out next step. Approach behaviour stops depending on where
 * a step boundary happened to fall.
 */
public final class HullPlanes {

    /**
     * How far ahead of contact a box still contributes a plane, in blocks. Box3D uses four times
     * its linear slop; ours is larger because a vehicle covers far more ground per step than a
     * walking character.
     */
    public static final float SPECULATIVE_MARGIN = 0.25f;

    /**
     * Normal Y above which a contact counts as holding the hull up rather than blocking it. Just
     * under 45 degrees, so a slope the vehicle rests on supports it and a near-vertical face does
     * not.
     */
    public static final float SUPPORT_NORMAL = 0.5f;

    private HullPlanes() {}

    /**
     * Appends a plane for every box within {@link #SPECULATIVE_MARGIN} of the hull.
     *
     * @param centreX,centreY,centreZ where the hull is being tested, which need not be where its
     *                                own centre currently sits — the mover loop advances a trial
     *                                position without disturbing the real one
     * @param rideHeight              how far a box top may stand above the hull's underside and
     *                                still be something to ride over rather than a wall. Planes
     *                                from such boxes get {@code ridePushLimit} instead of an
     *                                unbounded push and do not clip velocity — the climb skirt
     *                                expressed as a property of the contact
     * @param ridePushLimit           how far such a plane may move the hull
     */
    public static void collect(OBB hull, BoxBuffer boxes,
                               float centreX, float centreY, float centreZ,
                               double rideHeight, float ridePushLimit,
                               PlaneSolver.Planes out) {
        Matrix3f basis = hull.rotation().get(new Matrix3f());
        Vector3f extents = hull.extents();
        // World-space underside of the hull at this trial position. What makes a box rideable is
        // how far its top stands above this, which is the same quantity maxUpStep is written in.
        float halfHeight = Math.abs(basis.m01()) * extents.x
                + Math.abs(basis.m11()) * extents.y
                + Math.abs(basis.m21()) * extents.z;
        float hullBottom = centreY - halfHeight;

        for (int i = 0, n = boxes.size(); i < n; i++) {
            double boxMinX = boxes.minX(i);
            double boxMinY = boxes.minY(i);
            double boxMinZ = boxes.minZ(i);
            double boxMaxX = boxes.maxX(i);
            double boxMaxY = boxes.maxY(i);
            double boxMaxZ = boxes.maxZ(i);

            float boxCX = (float) ((boxMinX + boxMaxX) * 0.5);
            float boxCY = (float) ((boxMinY + boxMaxY) * 0.5);
            float boxCZ = (float) ((boxMinZ + boxMaxZ) * 0.5);
            float boxEX = (float) ((boxMaxX - boxMinX) * 0.5);
            float boxEY = (float) ((boxMaxY - boxMinY) * 0.5);
            float boxEZ = (float) ((boxMaxZ - boxMinZ) * 0.5);

            // Offset from hull to box. Normals are flipped to point back at the hull below, so the
            // solver always pushes the hull out of the box rather than into it.
            float dx = boxCX - centreX;
            float dy = boxCY - centreY;
            float dz = boxCZ - centreZ;

            float bestSeparation = -Float.MAX_VALUE;
            float bestX = 0;
            float bestY = 1;
            float bestZ = 0;

            // Three world axes, three hull axes, nine cross products. Face axes alone would miss
            // edge-on contacts, which for a rotated hull on a block grid are common.
            for (int axis = 0; axis < 15; axis++) {
                float ax;
                float ay;
                float az;
                if (axis < 3) {
                    ax = axis == 0 ? 1 : 0;
                    ay = axis == 1 ? 1 : 0;
                    az = axis == 2 ? 1 : 0;
                } else if (axis < 6) {
                    int h = axis - 3;
                    ax = hullAxis(basis, h, 0);
                    ay = hullAxis(basis, h, 1);
                    az = hullAxis(basis, h, 2);
                } else {
                    int w = (axis - 6) / 3;
                    int h = (axis - 6) % 3;
                    float wx = w == 0 ? 1 : 0;
                    float wy = w == 1 ? 1 : 0;
                    float wz = w == 2 ? 1 : 0;
                    float hx = hullAxis(basis, h, 0);
                    float hy = hullAxis(basis, h, 1);
                    float hz = hullAxis(basis, h, 2);
                    ax = wy * hz - wz * hy;
                    ay = wz * hx - wx * hz;
                    az = wx * hy - wy * hx;
                    float lengthSq = ax * ax + ay * ay + az * az;
                    if (lengthSq < 1.0e-6f) {
                        continue;
                    }
                    float inv = (float) (1.0 / Math.sqrt(lengthSq));
                    ax *= inv;
                    ay *= inv;
                    az *= inv;
                }

                float boxRadius = Math.abs(ax) * boxEX + Math.abs(ay) * boxEY + Math.abs(az) * boxEZ;
                float hullRadius =
                        Math.abs(ax * basis.m00() + ay * basis.m01() + az * basis.m02()) * extents.x
                                + Math.abs(ax * basis.m10() + ay * basis.m11() + az * basis.m12()) * extents.y
                                + Math.abs(ax * basis.m20() + ay * basis.m21() + az * basis.m22()) * extents.z;
                float distance = ax * dx + ay * dy + az * dz;
                float separation = Math.abs(distance) - (boxRadius + hullRadius);

                if (separation > bestSeparation) {
                    bestSeparation = separation;
                    // Point the normal from the box toward the hull.
                    float sign = distance > 0 ? -1.0f : 1.0f;
                    bestX = ax * sign;
                    bestY = ay * sign;
                    bestZ = az * sign;
                    if (separation > SPECULATIVE_MARGIN) {
                        // Already too far to matter and no other axis can reduce this, since we
                        // are maximising: bail out of the remaining axes for this box.
                        break;
                    }
                }
            }

            if (bestSeparation > SPECULATIVE_MARGIN) {
                continue;
            }

            // Two separate questions, and conflating them was a bug: is this geometry low enough to
            // drive over, and is this contact holding the hull up or pushing it back?
            //
            // A plane pointing mostly upward is support. It must always be free to push as far as
            // it needs, because that push *is* the hull being stood upon — capping it left a
            // vehicle unable to rise onto a step it had already driven into, which read in the
            // harness as a mover that stopped dead at the first riser. It clips velocity too,
            // since that is how the ground cancels gravity.
            //
            // A plane pointing mostly sideways, from geometry within the ride height, is a kerb. It
            // gets a small push so the hull cannot bury itself in it, and does not clip velocity,
            // so riding over it costs no speed. The same plane from anything taller is a wall.
            boolean supporting = bestY > SUPPORT_NORMAL;
            boolean ride = !supporting && boxMaxY - hullBottom <= rideHeight;
            out.add(bestX, bestY, bestZ, bestSeparation,
                    ride ? ridePushLimit : Float.MAX_VALUE, !ride);
        }
    }

    private static float hullAxis(Matrix3f basis, int axis, int component) {
        return switch (axis * 3 + component) {
            case 0 -> basis.m00();
            case 1 -> basis.m01();
            case 2 -> basis.m02();
            case 3 -> basis.m10();
            case 4 -> basis.m11();
            case 5 -> basis.m12();
            case 6 -> basis.m20();
            case 7 -> basis.m21();
            default -> basis.m22();
        };
    }

}
