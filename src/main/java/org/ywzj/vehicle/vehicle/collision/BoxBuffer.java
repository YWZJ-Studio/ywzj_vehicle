package org.ywzj.vehicle.vehicle.collision;

import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * A reusable list of world-space boxes held as primitives rather than {@link AABB} objects.
 * <p>
 * {@code AABB} is immutable, so every box the broad phase reports has to be a fresh object, and
 * those objects go straight into a list — which means they escape, and escape analysis cannot
 * elide them the way it does the short-lived vectors inside a collision test. Two call sites per
 * vehicle per tick each produce one per merged box, so a vehicle sitting on terrain turns over
 * hundreds of 64-byte objects a tick for data that never changes shape and is read a handful of
 * times before being thrown away.
 * <p>
 * Six doubles per box in one array, reused across ticks, removes all of it and reads sequentially
 * while it is at it — the sweep walks the whole set up to eight times per substep, so locality is
 * worth as much here as the allocation.
 * <p>
 * Not thread-safe and not meant to be: one per vehicle, cleared and refilled by its owner.
 */
public final class BoxBuffer {

    private static final int STRIDE = 6;

    private double[] data;
    private int count;

    public BoxBuffer() {
        this(64);
    }

    public BoxBuffer(int initialCapacity) {
        data = new double[Math.max(1, initialCapacity) * STRIDE];
    }

    public void clear() {
        count = 0;
    }

    public int size() {
        return count;
    }

    public boolean isEmpty() {
        return count == 0;
    }

    public void add(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        int base = count * STRIDE;
        if (base + STRIDE > data.length) {
            // Growth is amortised and the buffer is reused, so after the first few ticks a vehicle
            // never resizes again — the steady state really is zero allocation, not less of it.
            double[] grown = new double[Math.max(data.length * 2, base + STRIDE)];
            System.arraycopy(data, 0, grown, 0, data.length);
            data = grown;
        }
        data[base] = minX;
        data[base + 1] = minY;
        data[base + 2] = minZ;
        data[base + 3] = maxX;
        data[base + 4] = maxY;
        data[base + 5] = maxZ;
        count++;
    }

    public void add(AABB box) {
        add(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }

    /** Bridge for {@code CollisionProvider}, whose public API hands back real {@link AABB}s. */
    public void addAll(List<AABB> boxes) {
        for (int i = 0, size = boxes.size(); i < size; i++) {
            add(boxes.get(i));
        }
    }

    public double minX(int i) {
        return data[i * STRIDE];
    }

    public double minY(int i) {
        return data[i * STRIDE + 1];
    }

    public double minZ(int i) {
        return data[i * STRIDE + 2];
    }

    public double maxX(int i) {
        return data[i * STRIDE + 3];
    }

    public double maxY(int i) {
        return data[i * STRIDE + 4];
    }

    public double maxZ(int i) {
        return data[i * STRIDE + 5];
    }

    /** Materialises one box. For diagnostics and the debug overlay — not for the physics loops. */
    public AABB get(int i) {
        int base = i * STRIDE;
        return new AABB(data[base], data[base + 1], data[base + 2],
                data[base + 3], data[base + 4], data[base + 5]);
    }

}
