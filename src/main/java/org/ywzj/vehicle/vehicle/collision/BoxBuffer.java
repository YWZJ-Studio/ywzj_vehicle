package org.ywzj.vehicle.vehicle.collision;

import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Holds world-space boxes as six doubles per box in a single array, reused across ticks.
 * Avoids allocation pressure from immutable AABB objects and improves cache locality.
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
            // Growth is amortised; after the first few ticks a vehicle never resizes again.
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

    /** Adds all boxes from a list, converting from AABB objects to primitives. */
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

    /** Materialises one box as an AABB object; for diagnostics and debug only, not for physics. */
    public AABB get(int i) {
        int base = i * STRIDE;
        return new AABB(data[base], data[base + 1], data[base + 2],
                data[base + 3], data[base + 4], data[base + 5]);
    }

}
