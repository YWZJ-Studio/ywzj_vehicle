package org.ywzj.vehicle.vehicle.collision;

import java.util.Arrays;

/**
 * A bounding-volume hierarchy over boxes for broad-phase queries that skip whole subtrees in one test.
 * Implemented as an AVL-balanced binary tree stored in parallel primitive arrays, so querying
 * allocates nothing per box.
 */
public final class DynamicTree {

    public static final int NULL_NODE = -1;

    /** Bounds are fattened so small movements do not force a re-insert. */
    private static final double AABB_MARGIN = 0.1;

    private double[] minX, minY, minZ, maxX, maxY, maxZ;
    private int[] parent, child1, child2, height;
    private int[] userData;

    private int root = NULL_NODE;
    private int nodeCount;
    private int capacity;
    private int freeList;

    public DynamicTree() {
        this(64);
    }

    public DynamicTree(int initialCapacity) {
        capacity = Math.max(4, initialCapacity);
        allocate(capacity);
        resetFreeList(0);
    }

    private void allocate(int size) {
        minX = grow(minX, size); minY = grow(minY, size); minZ = grow(minZ, size);
        maxX = grow(maxX, size); maxY = grow(maxY, size); maxZ = grow(maxZ, size);
        parent = grow(parent, size); child1 = grow(child1, size);
        child2 = grow(child2, size); height = grow(height, size);
        userData = grow(userData, size);
    }

    private static double[] grow(double[] a, int size) {
        return a == null ? new double[size] : Arrays.copyOf(a, size);
    }

    private static int[] grow(int[] a, int size) {
        return a == null ? new int[size] : Arrays.copyOf(a, size);
    }

    /** Links every unused slot into the free list, starting at from. */
    private void resetFreeList(int from) {
        for (int i = from; i < capacity - 1; i++) {
            parent[i] = i + 1;
            height[i] = -1;
        }
        parent[capacity - 1] = NULL_NODE;
        height[capacity - 1] = -1;
        freeList = from;
    }

    public void clear() {
        root = NULL_NODE;
        nodeCount = 0;
        resetFreeList(0);
    }

    public int size() {
        return nodeCount;
    }

    /** Height of the tree. */
    public int treeHeight() {
        return root == NULL_NODE ? 0 : height[root];
    }

    private int allocateNode() {
        if (freeList == NULL_NODE) {
            int old = capacity;
            capacity *= 2;
            allocate(capacity);
            resetFreeList(old);
        }
        int node = freeList;
        freeList = parent[node];
        parent[node] = NULL_NODE;
        child1[node] = NULL_NODE;
        child2[node] = NULL_NODE;
        height[node] = 0;
        nodeCount++;
        return node;
    }

    private void freeNode(int node) {
        parent[node] = freeList;
        height[node] = -1;
        freeList = node;
        nodeCount--;
    }

    /**
     * Inserts a box and returns its proxy id.
     *
     * @param data caller's handle for this box, returned by queries
     */
    public int createProxy(double bMinX, double bMinY, double bMinZ,
                           double bMaxX, double bMaxY, double bMaxZ, int data) {
        int proxy = allocateNode();
        minX[proxy] = bMinX - AABB_MARGIN;
        minY[proxy] = bMinY - AABB_MARGIN;
        minZ[proxy] = bMinZ - AABB_MARGIN;
        maxX[proxy] = bMaxX + AABB_MARGIN;
        maxY[proxy] = bMaxY + AABB_MARGIN;
        maxZ[proxy] = bMaxZ + AABB_MARGIN;
        userData[proxy] = data;
        height[proxy] = 0;
        insertLeaf(proxy);
        return proxy;
    }


    private void insertLeaf(int leaf) {
        if (root == NULL_NODE) {
            root = leaf;
            parent[leaf] = NULL_NODE;
            return;
        }

        int index = root;
        while (height[index] > 0) {
            int c1 = child1[index];
            int c2 = child2[index];
            double area = area(index);
            double combined = combinedArea(index, leaf);
            double cost = 2.0 * combined;
            double inheritance = 2.0 * (combined - area);
            double cost1 = descentCost(c1, leaf, inheritance);
            double cost2 = descentCost(c2, leaf, inheritance);
            if (cost < cost1 && cost < cost2) {
                break;
            }
            index = cost1 < cost2 ? c1 : c2;
        }

        int sibling = index;
        int oldParent = parent[sibling];
        int newParent = allocateNode();
        parent[newParent] = oldParent;
        userData[newParent] = -1;
        merge(newParent, leaf, sibling);
        height[newParent] = height[sibling] + 1;

        if (oldParent != NULL_NODE) {
            if (child1[oldParent] == sibling) {
                child1[oldParent] = newParent;
            } else {
                child2[oldParent] = newParent;
            }
        } else {
            root = newParent;
        }
        child1[newParent] = sibling;
        child2[newParent] = leaf;
        parent[sibling] = newParent;
        parent[leaf] = newParent;

        refit(parent[leaf]);
    }

    private double descentCost(int child, int leaf, double inheritance) {
        double combined = combinedArea(child, leaf);
        if (height[child] == 0) {
            return combined + inheritance;
        }
        return combined - area(child) + inheritance;
    }

    /** Walks up from a node, rebalancing and re-bounding every ancestor. */
    private void refit(int index) {
        while (index != NULL_NODE) {
            index = balance(index);
            int c1 = child1[index];
            int c2 = child2[index];
            height[index] = 1 + Math.max(height[c1], height[c2]);
            merge(index, c1, c2);
            index = parent[index];
        }
    }

    /**
     * One AVL rotation if this node's children differ in height by more than one.
     * Without balancing, boxes inserted in scan order degenerate the tree into a linked list.
     */
    private int balance(int a) {
        if (height[a] < 2) {
            return a;
        }
        int b = child1[a];
        int c = child2[a];
        int diff = height[c] - height[b];
        if (diff > 1) {
            return rotate(a, c, b);
        }
        if (diff < -1) {
            return rotate(a, b, c);
        }
        return a;
    }

    private int rotate(int a, int heavy, int light) {
        int f = child1[heavy];
        int g = child2[heavy];

        child1[heavy] = a;
        parent[heavy] = parent[a];
        parent[a] = heavy;

        if (parent[heavy] != NULL_NODE) {
            if (child1[parent[heavy]] == a) {
                child1[parent[heavy]] = heavy;
            } else {
                child2[parent[heavy]] = heavy;
            }
        } else {
            root = heavy;
        }

        boolean keepF = height[f] > height[g];
        int high = keepF ? f : g;
        int low = keepF ? g : f;
        child2[heavy] = high;
        if (child1[a] == heavy) {
            child1[a] = low;
        } else {
            child2[a] = low;
        }
        parent[low] = a;

        merge(a, light, low);
        merge(heavy, a, high);
        height[a] = 1 + Math.max(height[light], height[low]);
        height[heavy] = 1 + Math.max(height[a], height[high]);
        return heavy;
    }


    /** Called once per proxy overlapping the query with its user data. */
    public interface Visitor {
        void accept(int data);
    }

    /**
     * Visits every proxy overlapping the given box using an explicit stack to avoid recursion.
     */
    public void query(double qMinX, double qMinY, double qMinZ,
                      double qMaxX, double qMaxY, double qMaxZ, Visitor visitor) {
        if (root == NULL_NODE) {
            return;
        }
        int[] stack = stackScratch;
        int top = 0;
        stack[top++] = root;
        while (top > 0) {
            int node = stack[--top];
            if (maxX[node] < qMinX || minX[node] > qMaxX
                    || maxY[node] < qMinY || minY[node] > qMaxY
                    || maxZ[node] < qMinZ || minZ[node] > qMaxZ) {
                continue;
            }
            if (height[node] == 0) {
                visitor.accept(userData[node]);
                continue;
            }
            if (top + 2 > stack.length) {
                stack = stackScratch = Arrays.copyOf(stack, stack.length * 2);
            }
            stack[top++] = child1[node];
            stack[top++] = child2[node];
        }
    }

    private int[] stackScratch = new int[64];

    /**
     * Reusable traversal stack and result buffer owned by the caller.
     * Allows concurrent queries against the shared tree without allocations or corruption.
     */
    public static final class QueryScratch {

        int[] stack = new int[64];
        int[] out = new int[64];

        /** Proxy user data from the last query result. */
        public int result(int i) {
            return out[i];
        }

    }

    /**
     * Collects every proxy overlapping the query into scratch and returns the count.
     * Allocation-free after warm-up; safe to run concurrently provided each caller owns its scratch.
     */
    public int query(QueryScratch scratch, double qMinX, double qMinY, double qMinZ,
                     double qMaxX, double qMaxY, double qMaxZ) {
        if (root == NULL_NODE) {
            return 0;
        }
        int[] stack = scratch.stack;
        int[] out = scratch.out;
        int top = 0;
        int count = 0;
        stack[top++] = root;
        while (top > 0) {
            int node = stack[--top];
            if (maxX[node] < qMinX || minX[node] > qMaxX
                    || maxY[node] < qMinY || minY[node] > qMaxY
                    || maxZ[node] < qMinZ || minZ[node] > qMaxZ) {
                continue;
            }
            if (height[node] == 0) {
                if (count == out.length) {
                    out = scratch.out = Arrays.copyOf(out, out.length * 2);
                }
                out[count++] = userData[node];
                continue;
            }
            if (top + 2 > stack.length) {
                stack = scratch.stack = Arrays.copyOf(stack, stack.length * 2);
            }
            stack[top++] = child1[node];
            stack[top++] = child2[node];
        }
        return count;
    }

    /** Fills out with boxes from source whose proxies overlap the query. */
    public void query(BoxBuffer source, double qMinX, double qMinY, double qMinZ,
                      double qMaxX, double qMaxY, double qMaxZ, BoxBuffer out) {
        query(qMinX, qMinY, qMinZ, qMaxX, qMaxY, qMaxZ, i ->
                out.add(source.minX(i), source.minY(i), source.minZ(i),
                        source.maxX(i), source.maxY(i), source.maxZ(i)));
    }

    /** Builds a tree over every box in the buffer, with each proxy's data as its index. */
    public void build(BoxBuffer boxes) {
        clear();
        for (int i = 0, n = boxes.size(); i < n; i++) {
            createProxy(boxes.minX(i), boxes.minY(i), boxes.minZ(i),
                    boxes.maxX(i), boxes.maxY(i), boxes.maxZ(i), i);
        }
    }

    /**
     * Builds a tree over a section snapshot's packed merged boxes in section-relative coordinates.
     * Each proxy's data is its index into the packed array.
     */
    public void build(long[] packed) {
        clear();
        for (int i = 0; i < packed.length; i++) {
            long box = packed[i];
            double x0 = SectionCollision.boxMinX(box);
            double y0 = SectionCollision.boxMinY(box);
            double z0 = SectionCollision.boxMinZ(box);
            createProxy(x0, y0, z0,
                    x0 + SectionCollision.boxSizeX(box),
                    y0 + SectionCollision.boxSizeY(box),
                    z0 + SectionCollision.boxSizeZ(box), i);
        }
    }

    // ---- bounds arithmetic ----

    private void merge(int dest, int a, int b) {
        minX[dest] = Math.min(minX[a], minX[b]);
        minY[dest] = Math.min(minY[a], minY[b]);
        minZ[dest] = Math.min(minZ[a], minZ[b]);
        maxX[dest] = Math.max(maxX[a], maxX[b]);
        maxY[dest] = Math.max(maxY[a], maxY[b]);
        maxZ[dest] = Math.max(maxZ[a], maxZ[b]);
    }

    private double area(int node) {
        double dx = maxX[node] - minX[node];
        double dy = maxY[node] - minY[node];
        double dz = maxZ[node] - minZ[node];
        return dx * dy + dy * dz + dz * dx;
    }

    private double combinedArea(int a, int b) {
        double dx = Math.max(maxX[a], maxX[b]) - Math.min(minX[a], minX[b]);
        double dy = Math.max(maxY[a], maxY[b]) - Math.min(minY[a], minY[b]);
        double dz = Math.max(maxZ[a], maxZ[b]) - Math.min(minZ[a], minZ[b]);
        return dx * dy + dy * dz + dz * dx;
    }

}
