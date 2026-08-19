package org.ywzj.vehicle.vehicle.parenting;

import org.joml.Quaternionf;

/**
 * One tick's view of a vehicle as its riders see it: pivot position, rotation, and local boxes
 * they stand on, published as a single object. Snapshot ensures riders see position and rotation
 * from the same frame, which parallel physics requires; atomically published via volatile write
 * and safe to hold only within the call that obtained it.
 */
public final class DeckSnapshot {

    /** Default snapshot for vehicles with no walkable geometry; riders never see null. */
    public static final DeckSnapshot EMPTY = new DeckSnapshot();

    private final Quaternionf rotation = new Quaternionf();
    private final Quaternionf inverse = new Quaternionf();

    private double pivotX;
    private double pivotY;
    private double pivotZ;
    private float yaw;

    private float[] boxes = new float[0];
    private int count;

    /**
     * The subset of boxes declared as landing surface, in the same frame. A separate array rather
     * than a mask, so both riders and sweep casts can walk only the data they need. Empty for
     * vehicles that do not declare a deck bone.
     */
    private float[] deckBoxes = new float[0];
    private int deckCount;

    /**
     * The vehicle-local frame's origin in world space; riders and geometry share one coordinate
     * system.
     */
    public double pivotX() {
        return pivotX;
    }

    public double pivotY() {
        return pivotY;
    }

    public double pivotZ() {
        return pivotZ;
    }

    /** The hull's orientation; rotation() and inverse() form a unit quaternion. */
    public Quaternionf rotation() {
        return rotation;
    }

    public Quaternionf inverse() {
        return inverse;
    }

    /**
     * The vehicle's yaw when this snapshot was taken; riders turn by the difference between
     * snapshots.
     */
    public float yaw() {
        return yaw;
    }

    /** Structure cubes as vehicle-local axis-aligned boxes; six floats per cube. */
    public float[] boxes() {
        return boxes;
    }

    public int count() {
        return count;
    }

    /** Declared landing surface in the same frame as boxes; six floats per cube. */
    public float[] deckBoxes() {
        return deckBoxes;
    }

    /** Number of filled deck boxes; zero for vehicles with no deck. */
    public int deckCount() {
        return deckCount;
    }

    /**
     * The fill buffer, grown to fit the hull; only the vehicle writing this snapshot may touch it
     * before the publish.
     */
    public float[] boxBuffer(int cubes) {
        if (boxes.length < cubes * 6) {
            boxes = new float[cubes * 6];
        }
        return boxes;
    }

    /** Fill buffer for the deck subset. */
    public float[] deckBoxBuffer(int cubes) {
        if (deckBoxes.length < cubes * 6) {
            deckBoxes = new float[cubes * 6];
        }
        return deckBoxes;
    }

    /**
     * Closes the fill and publishes atomically; the volatile write makes all prior field sets
     * visible to readers.
     */
    public void set(Quaternionf hullRotation, float hullYaw,
                    double originX, double originY, double originZ, int boxCount, int deckBoxCount) {
        this.rotation.set(hullRotation);
        // Cache the conjugate (inverse) since riders need it on every clip.
        hullRotation.conjugate(this.inverse);
        this.yaw = hullYaw;
        this.pivotX = originX;
        this.pivotY = originY;
        this.pivotZ = originZ;
        this.count = boxCount;
        this.deckCount = deckBoxCount;
    }

}
