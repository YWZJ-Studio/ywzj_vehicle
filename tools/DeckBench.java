import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.ywzj.vehicle.vehicle.parenting.DeckClip;

import java.util.Random;

/**
 * Cost of per-rider deck path, measured empirically.
 * Reproduces DeckCollision.clip with frame conversions, sweep, and correction through scratch buffers.
 */
public class DeckBench {

    static final double HX = 0.3, HY = 0.9, HZ = 0.3, STEP = 0.6;

    /** A hull of n boxes: deck plus random superstructure in vehicle-local space. */
    static float[] hull(int cubes) {
        Random r = new Random(11L);
        float[] boxes = new float[cubes * 6];
        // Box 0 is the deck.
        boxes[0] = -20; boxes[1] = -1; boxes[2] = -75; boxes[3] = 20; boxes[4] = 0; boxes[5] = 75;
        for (int i = 1; i < cubes; i++) {
            int o = i * 6;
            float cx = (r.nextFloat() - 0.5f) * 40;
            float cy = r.nextFloat() * 14;
            float cz = (r.nextFloat() - 0.5f) * 150;
            float ex = 0.5f + r.nextFloat() * 3;
            float ey = 0.5f + r.nextFloat() * 3;
            float ez = 0.5f + r.nextFloat() * 3;
            boxes[o] = cx - ex; boxes[o + 1] = cy - ey; boxes[o + 2] = cz - ez;
            boxes[o + 3] = cx + ex; boxes[o + 4] = cy + ey; boxes[o + 5] = cz + ez;
        }
        return boxes;
    }

    // Scratch buffers, as used by DeckAttachment.
    static final Vector3f sCentre = new Vector3f();
    static final Vector3f sMove = new Vector3f();
    static final Vector3f sGrounded = new Vector3f();
    static final Vector3d sWorld = new Vector3d();
    static final double[] sOut = new double[3];
    static float[] sNear = new float[8 * 6];

    static double sink;

    /** Full deck-path simulation for one rider move. */
    static void clipOnce(float[] boxes, int count, Quaternionf rot, Quaternionf inv,
                         double pivotX, double pivotY, double pivotZ,
                         double wx, double wy, double wz,
                         double mx, double my, double mz) {
        sCentre.set((float) (wx - pivotX), (float) (wy - pivotY), (float) (wz - pivotZ));
        inv.transform(sCentre);
        inv.transform(sMove.set((float) mx, (float) my, (float) mz));
        inv.transform(sGrounded.set((float) mx, 0, (float) mz));
        if (sNear.length < count * 6)
            sNear = new float[count * 6];
        int nearCount = DeckClip.narrow(boxes, count, sCentre.x, sCentre.y, sCentre.z, HX, HY, HZ,
                sMove.x, sMove.y, sMove.z, sGrounded.x, sGrounded.z, STEP, sNear);
        if (nearCount == 0) return;
        DeckClip.sweep(sNear, nearCount, sCentre.x, sCentre.y, sCentre.z, HX, HY, HZ,
                sMove.x, sMove.y, sMove.z, sGrounded.x, sGrounded.z, STEP, true, sOut);
        if (sOut[0] != sMove.x || sOut[1] != sMove.y || sOut[2] != sMove.z) {
            rot.transform(sWorld.set(sOut[0] - sMove.x, sOut[1] - sMove.y, sOut[2] - sMove.z));
            sink += sWorld.y;
        }
    }

    static double bench(int cubes, int riders, int iterations) {
        float[] boxes = hull(cubes);
        Quaternionf rot = new Quaternionf()
                .rotateY((float) Math.toRadians(-31.7))
                .rotateX((float) Math.toRadians(3))
                .rotateZ((float) Math.toRadians(2));
        Quaternionf inv = new Quaternionf(rot).conjugate();
        double pivotX = 1_500_000.5, pivotY = 96, pivotZ = -820_000.25;

        // Riders spread across deck, all standing on it.
        double[] lx = new double[riders], lz = new double[riders];
        Random r = new Random(5L);
        for (int i = 0; i < riders; i++) {
            lx[i] = (r.nextDouble() - 0.5) * 38;
            lz[i] = (r.nextDouble() - 0.5) * 148;
        }

        long best = Long.MAX_VALUE;
        for (int pass = 0; pass < 7; pass++) {
            long t0 = System.nanoTime();
            for (int it = 0; it < iterations; it++) {
                for (int i = 0; i < riders; i++) {
                    Vector3f local = new Vector3f((float) lx[i], (float) HY, (float) lz[i]);
                    rot.transform(local);
                    clipOnce(boxes, cubes, rot, inv, pivotX, pivotY, pivotZ,
                            pivotX + local.x, pivotY + local.y, pivotZ + local.z,
                            0.1, -0.0784, 0.1);
                }
            }
            long dt = System.nanoTime() - t0;
            if (dt < best) best = dt;
        }
        return (double) best / (iterations * (double) riders);
    }

    public static void main(String[] args) {
        System.out.println("ns per rider per move, best of 7\n");
        System.out.printf("%-34s %10s %10s %10s%n", "hull", "1 rider", "16 riders", "64 riders");
        int[][] hulls = {{2, 0}, {9, 0}, {27, 0}, {150, 0}};
        String[] names = {"Motorcycle (2 cubes)", "ZTZ99A (9 cubes)", "J-10C (27 cubes)",
                "Hypothetical carrier (150)"};
        for (int h = 0; h < hulls.length; h++) {
            int cubes = hulls[h][0];
            System.out.printf("%-34s %10.1f %10.1f %10.1f%n", names[h],
                    bench(cubes, 1, 200_000),
                    bench(cubes, 16, 20_000),
                    bench(cubes, 64, 5_000));
        }
        System.out.printf("%n(sink %g — keeps the JIT honest)%n", sink);

        // Full deck cost per tick.
        System.out.println("\nper-tick cost of the whole deck, 150-cube carrier:");
        for (int riders : new int[]{1, 8, 32, 64, 128}) {
            double ns = bench(150, riders, Math.max(500, 100_000 / riders)) * riders;
            System.out.printf("  %3d riders  %8.1f us/tick   %5.2f%% of a 50 ms tick%n",
                    riders, ns / 1000.0, ns / 50_000_000.0 * 100);
        }
    }

}
