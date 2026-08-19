import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.ywzj.vehicle.vehicle.parenting.DeckClip;
import org.ywzj.vehicle.vehicle.parenting.DeckSnapshot;

import java.util.Random;

/**
 * Standalone verification of deck parenting geometry.
 * Calls DeckClip and DeckSnapshot directly against a real snapshot to test shipped code.
 * Reproduces frame conversions with published inverse rotation.
 *
 * Run:
 *   JOML=$(find ~/.gradle ~/.local/share/gradle -name 'joml-*.jar' ! -name '*sources*' | head -1)
 *   javac -d /tmp/deckcheck -cp "$JOML" \
 *       src/main/java/org/ywzj/vehicle/vehicle/parenting/DeckClip.java \
 *       src/main/java/org/ywzj/vehicle/vehicle/parenting/DeckSnapshot.java tools/DeckCheck.java
 *   java -cp "/tmp/deckcheck:$JOML" DeckCheck
 */
public class DeckCheck {

    /** Minimal stand-in for net.minecraft.world.phys.Vec3, which is not on this classpath. */
    record Vec3d(double x, double y, double z) {}


    static int failures = 0;

    // ---- a deck: 12 x 1 x 30 slab centred on the local origin, top at y = 0 ----
    static float[] deck() {
        return new float[]{-6, -1, -15, 6, 0, 15};
    }

    static float[] deckWithStep(float stepTop) {
        return new float[]{
                -6, -1, -15, 6, 0, 15,      // deck, top at 0
                -6, 0, 5, 6, stepTop, 15};  // riser occupying z >= 5
    }

    // Player-ish box
    static final double HX = 0.3, HY = 0.9, HZ = 0.3;
    static final double STEP = 0.6;

    public static void main(String[] args) {
        restingBoxGainsNothing();
        landsOnSurface();
        stepsOverLowRiser();
        refusesWall();
        wallStopsOnlyTheAxisIntoIt();
        overlappingBoxStillMoves();
        depenetrationConverges();
        carryIsExactUnderArbitraryPose();
        carryRoundTripIsIdentity();
        restingRiderGainsExactlyZeroHeightOverAThousandTicks();
        noGravityCreepOnATiltedDeck();
        walkingFollowsTheSlope();
        narrowingChangesNothing();
        publishedSnapshotIsSelfConsistent();
        republishLeavesNothingBehind();
        renderChordErrorIsSubVisible();
        System.out.println(failures == 0 ? "\nALL CHECKS PASSED" : "\n" + failures + " CHECK(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    // ------------------------------------------------------------------ clip behaviour

    /** A box already resting on the deck must not be lifted, and must not sink. */
    static void restingBoxGainsNothing() {
        float[] boxes = deck();
        double cy = HY;                       // feet exactly at deck top (y = 0)
        double[] out = new double[3];
        double worstRise = 0;
        double worstSink = 0;
        for (int tick = 0; tick < 200; tick++) {
            boolean down = DeckClip.sweep(boxes, 1, 0, cy, 0, HX, HY, HZ,
                    0, -0.0784, 0, 0, 0, STEP, true, out);
            check(down, "resting box reports support at tick " + tick);
            cy += out[1];
            double feet = cy - HY;
            worstRise = Math.max(worstRise, feet);
            worstSink = Math.min(worstSink, feet);
        }
        report("rest: no rise", worstRise <= 0,
                String.format("rise %.3e", worstRise));
        report("rest: no sink", worstSink >= -DeckClip.EPSILON,
                String.format("sink %.3e", worstSink));
    }

    /** Falling onto the deck stops at the surface, not through it and not above it. */
    static void landsOnSurface() {
        float[] boxes = deck();
        double cy = HY + 4.0;
        double vy = 0;
        double[] out = new double[3];
        boolean landed = false;
        for (int tick = 0; tick < 200 && !landed; tick++) {
            vy = (vy - 0.08) * 0.98;
            landed = DeckClip.sweep(boxes, 1, 0, cy, 0, HX, HY, HZ, 0, vy, 0, 0, 0, STEP, false, out);
            cy += out[1];
            if (landed) vy = 0;
        }
        double feet = cy - HY;
        report("land: comes to rest on the surface", landed && Math.abs(feet) < 1e-6,
                String.format("feet %.3e", feet));
    }

    /** A 0.5 riser is climbed; the box ends on top of it having kept its forward motion. */
    static void stepsOverLowRiser() {
        float[] boxes = deckWithStep(0.5f);
        double cy = HY, cz = 0;
        double[] out = new double[3];
        for (int tick = 0; tick < 60; tick++) {
            DeckClip.sweep(boxes, 2, 0, cy, cz, HX, HY, HZ, 0, -0.0784, 0.2, 0, 0.2, STEP, true, out);
            cy += out[1];
            cz += out[2];
        }
        double feet = cy - HY;
        report("step: climbs a 0.5 riser", Math.abs(feet - 0.5) < 1e-6,
                String.format("feet %.4f", feet));
        report("step: kept going forward", cz > 5.5, String.format("z %.2f", cz));
    }

    /** A 1.5 wall is refused: the box stops against it at the deck height it started on. */
    static void refusesWall() {
        float[] boxes = deckWithStep(1.5f);
        double cy = HY, cz = 0;
        double[] out = new double[3];
        for (int tick = 0; tick < 60; tick++) {
            DeckClip.sweep(boxes, 2, 0, cy, cz, HX, HY, HZ, 0, -0.0784, 0.2, 0, 0.2, STEP, true, out);
            cy += out[1];
            cz += out[2];
        }
        double feet = cy - HY;
        report("wall: not climbed", Math.abs(feet) < 1e-6, String.format("feet %.4f", feet));
        report("wall: stopped against it", Math.abs(cz - (5 - HZ)) < 1e-5,
                String.format("z %.5f, expected %.5f", cz, 5 - HZ));
    }

    /** A wall on Z must not cancel motion on X. That weld is what split the legs upstream. */
    static void wallStopsOnlyTheAxisIntoIt() {
        float[] boxes = deckWithStep(3.0f);
        double[] out = new double[3];
        // Pressed against the riser, asking to move both into it and along it.
        DeckClip.sweep(boxes, 2, 0, HY, 5 - HZ - 1e-4, HX, HY, HZ,
                0.2, -0.0784, 0.2, 0.2, 0.2, STEP, true, out);
        report("wall: Z blocked", out[2] < 0.2 - 1e-6, String.format("z %.5f", out[2]));
        report("wall: X untouched", Math.abs(out[0] - 0.2) < 1e-12, String.format("x %.5f", out[0]));
    }

    /** Vanilla's rule: an already-overlapping box is never locked in place by the clip. */
    static void overlappingBoxStillMoves() {
        float[] boxes = deck();
        double[] out = new double[3];
        // Buried half a block into the deck.
        DeckClip.sweep(boxes, 1, 0, HY - 0.5, 0, HX, HY, HZ, 0.2, 0, 0.2, 0.2, 0.2, STEP, false, out);
        report("embedded: still free to move",
                Math.abs(out[0] - 0.2) < 1e-12 && Math.abs(out[2] - 0.2) < 1e-12,
                String.format("(%.4f, %.4f)", out[0], out[2]));
    }

    /** The safety net has to finish the job, and finish it upward. */
    static void depenetrationConverges() {
        float[] boxes = deck();
        double cy = HY - 0.5;
        double[] out = new double[3];
        boolean pushed = DeckClip.depenetrate(boxes, 1, 0, cy, 0, HX, HY, HZ, 4, out);
        cy += out[1];
        report("depenetrate: pushed", pushed, "");
        report("depenetrate: upward", out[1] > 0, String.format("dy %.4f", out[1]));
        report("depenetrate: clear in one call",
                !DeckClip.depenetrate(boxes, 1, 0, cy, 0, HX, HY, HZ, 4, new double[3]),
                String.format("feet %.3e", cy - HY));

        // Deeply buried: prefers the side, and still converges.
        double cz = 14.9;
        double cy2 = -0.5;
        double[] out2 = new double[3];
        DeckClip.depenetrate(boxes, 1, 0, cy2, cz, HX, HY, HZ, 4, out2);
        report("depenetrate: converges from inside",
                !DeckClip.depenetrate(boxes, 1, out2[0], cy2 + out2[1], cz + out2[2],
                        HX, HY, HZ, 4, new double[3]), "");
    }

    // ------------------------------------------------------------------ the parent frame

    /** DeckCollision / VehicleParenting frame conversions, run through a real published snapshot. */
    static Quaternionf rotYXZ(float yaw, float pitch, float roll) {
        return new Quaternionf()
                .rotateY((float) Math.toRadians(-yaw))
                .rotateX((float) Math.toRadians(pitch))
                .rotateZ((float) Math.toRadians(roll));
    }

    /** What AbstractVehicle.updateOBBs() publishes at the end of a tick. */
    static DeckSnapshot publish(Vector3d pivot, Quaternionf rot) {
        DeckSnapshot deck = new DeckSnapshot();
        deck.set(rot, 0, pivot.x, pivot.y, pivot.z, 0, 0);
        return deck;
    }

    static Vector3d toLocal(Vector3d pivot, Quaternionf rot, Vector3d world) {
        return toLocal(publish(pivot, rot), world);
    }

    static Vector3d toWorld(Vector3d pivot, Quaternionf rot, Vector3d local) {
        return toWorld(publish(pivot, rot), local);
    }

    /** VehicleParenting.toLocal logic, tested against the snapshot's inverse. */
    static Vector3d toLocal(DeckSnapshot deck, Vector3d world) {
        Vector3d dest = new Vector3d(world.x - deck.pivotX(),
                world.y - deck.pivotY(),
                world.z - deck.pivotZ());
        deck.inverse().transform(dest);
        return dest;
    }

    /** VehicleParenting.toWorld logic. */
    static Vector3d toWorld(DeckSnapshot deck, Vector3d local) {
        Vector3d p = deck.rotation().transform(new Vector3d(local));
        return new Vector3d(deck.pivotX() + p.x, deck.pivotY() + p.y, deck.pivotZ() + p.z);
    }

    /**
     * A rider stationary in the deck frame stays stationary in world space, regardless of vehicle motion.
     */
    static void carryIsExactUnderArbitraryPose() {
        Random random = new Random(20260816L);
        double worst = 0;
        for (int trial = 0; trial < 20000; trial++) {
            Vector3d pivotA = new Vector3d(
                    (random.nextDouble() - 0.5) * 60000,
                    random.nextDouble() * 300,
                    (random.nextDouble() - 0.5) * 60000);
            Quaternionf rotA = rotYXZ(random.nextFloat() * 720 - 360,
                    random.nextFloat() * 60 - 30, random.nextFloat() * 60 - 30);
            // Rider on a carrier-sized deck.
            Vector3d local = new Vector3d(
                    (random.nextDouble() - 0.5) * 40,
                    (random.nextDouble() - 0.5) * 15,
                    (random.nextDouble() - 0.5) * 150);

            Vector3d worldA = toWorld(pivotA, rotA, local);

            // Vehicle translates and rotates; up to 100 blocks per tick.
            Vector3d pivotB = new Vector3d(pivotA).add(
                    (random.nextDouble() - 0.5) * 200,
                    (random.nextDouble() - 0.5) * 200,
                    (random.nextDouble() - 0.5) * 200);
            Quaternionf rotB = rotYXZ(random.nextFloat() * 720 - 360,
                    random.nextFloat() * 60 - 30, random.nextFloat() * 60 - 30);

            // Capture at pose A, apply at pose B; the tick contract.
            Vector3d captured = toLocal(pivotA, rotA, worldA);
            Vector3d worldB = toWorld(pivotB, rotB, captured);
            Vector3d expected = toWorld(pivotB, rotB, local);
            worst = Math.max(worst, worldB.distance(expected));
        }
        report("carry: exact through capture and apply", worst < 2e-3,
                String.format("worst %.3e blocks over 20000 poses", worst));
    }

    /** Capture and apply with no motion must preserve position, or riders drift. */
    static void carryRoundTripIsIdentity() {
        Random random = new Random(7L);
        double worst = 0;
        for (int trial = 0; trial < 20000; trial++) {
            Vector3d pivot = new Vector3d(
                    (random.nextDouble() - 0.5) * 60000,
                    random.nextDouble() * 300,
                    (random.nextDouble() - 0.5) * 60000);
            Quaternionf rot = rotYXZ(random.nextFloat() * 720 - 360,
                    random.nextFloat() * 60 - 30, random.nextFloat() * 60 - 30);
            Vector3d world = new Vector3d(pivot).add(
                    (random.nextDouble() - 0.5) * 40,
                    (random.nextDouble() - 0.5) * 15,
                    (random.nextDouble() - 0.5) * 150);
            Vector3d back = toWorld(pivot, rot, toLocal(pivot, rot, world));
            worst = Math.max(worst, back.distance(world));
        }
        report("carry: round trip is identity", worst < 2e-3,
                String.format("worst %.3e blocks over 20000 poses", worst));
    }

    /**
     * A stationary rider on a moving hull gains no height over 1000 ticks.
     * Capture at old pose, move vehicle, apply at new pose, clip gravity in new frame.
     */
    static void restingRiderGainsExactlyZeroHeightOverAThousandTicks() {
        float[] boxes = deck();
        Vector3d pivot = new Vector3d(1_500_000.5, 96.0, -820_000.25);
        float yaw = 31.7f, pitch = 0, roll = 0;
        Quaternionf rot = rotYXZ(yaw, pitch, roll);

        // Rider standing on the deck, 12 blocks forward of the pivot.
        Vector3d local = new Vector3d(2.5, 0, 12.0);
        Vector3d world = toWorld(pivot, rot, local);
        double vy = 0;
        double worstRise = 0, worstSink = 0, worstLateral = 0;
        double[] out = new double[3];

        for (int tick = 0; tick < 1000; tick++) {
            // Top of vehicle tick: capture.
            Vector3d captured = toLocal(pivot, rot, world);

            // Vehicle moves: carrier under way, turning, pitching.
            pivot.add(0.9 * Math.cos(tick * 0.01), 0.05 * Math.sin(tick * 0.03), 1.4 * Math.sin(tick * 0.01));
            yaw += 0.6f;
            pitch = 4.0f * (float) Math.sin(tick * 0.02);
            roll = 3.0f * (float) Math.cos(tick * 0.017);
            rot = rotYXZ(yaw, pitch, roll);

            // Bottom of vehicle tick: apply.
            world = toWorld(pivot, rot, captured);

            // Rider's own tick: gravity, clipped in the vehicle frame.
            // Transform world move in, sweep, add correction back in world space.
            vy = (vy - 0.08) * 0.98;
            Vec3d worldMove = new Vec3d(0, vy, 0);
            Vector3d riderLocal = toLocal(pivot, rot, world);
            Quaternionf inverse = new Quaternionf(rot).conjugate();
            Vector3f move = inverse.transform(
                    new Vector3f((float) worldMove.x, (float) worldMove.y, (float) worldMove.z));
            Vector3f grounded = inverse.transform(
                    new Vector3f((float) worldMove.x, 0, (float) worldMove.z));
            boolean supported = DeckClip.sweep(boxes, 1,
                    riderLocal.x, riderLocal.y + HY, riderLocal.z, HX, HY, HZ,
                    move.x, move.y, move.z, grounded.x, grounded.z, STEP, true, out);
            if (supported) vy = 0;
            Vector3d correction = rot.transform(new Vector3d(
                    out[0] - move.x, out[1] - move.y, out[2] - move.z));
            world.add(worldMove.x + correction.x,
                    worldMove.y + correction.y,
                    worldMove.z + correction.z);

            Vector3d settled = toLocal(pivot, rot, world);
            worstRise = Math.max(worstRise, settled.y - local.y);
            worstSink = Math.min(worstSink, settled.y - local.y);
            worstLateral = Math.max(worstLateral,
                    Math.hypot(settled.x - local.x, settled.z - local.z));
        }
        report("ride: no rise over 1000 ticks", worstRise < 1e-3,
                String.format("rise %.3e", worstRise));
        report("ride: no sink over 1000 ticks", worstSink > -1e-3,
                String.format("sink %.3e", worstSink));
        report("ride: no lateral drift over 1000 ticks", worstLateral < 5e-3,
                String.format("drift %.3e", worstLateral));
    }

    /**
     * Sweep takes horizontal intent twice to stop gravity's tangential component from sliding riders.
     */
    static void noGravityCreepOnATiltedDeck() {
        float[] boxes = deck();
        Quaternionf rot = rotYXZ(0, 8, 5);        // 8 degrees pitch, 5 degrees roll
        Vector3d pivot = new Vector3d(0, 100, 0);
        Vector3d local = new Vector3d(0, 0, 6);
        Vector3d world = toWorld(pivot, rot, local);
        double vy = 0;
        double[] out = new double[3];
        for (int tick = 0; tick < 600; tick++) {
            vy = (vy - 0.08) * 0.98;
            Vec3d worldMove = new Vec3d(0, vy, 0);   // Standing still; no input.
            Vector3d riderLocal = toLocal(pivot, rot, world);
            Quaternionf inverse = new Quaternionf(rot).conjugate();
            Vector3f move = inverse.transform(new Vector3f(
                    (float) worldMove.x, (float) worldMove.y, (float) worldMove.z));
            Vector3f grounded = inverse.transform(new Vector3f(
                    (float) worldMove.x, 0, (float) worldMove.z));
            boolean supported = DeckClip.sweep(boxes, 1,
                    riderLocal.x, riderLocal.y + HY, riderLocal.z, HX, HY, HZ,
                    move.x, move.y, move.z, grounded.x, grounded.z, STEP, true, out);
            if (supported) vy = 0;
            Vector3d correction = rot.transform(new Vector3d(
                    out[0] - move.x, out[1] - move.y, out[2] - move.z));
            world.add(worldMove.x + correction.x, worldMove.y + correction.y,
                    worldMove.z + correction.z);
        }
        Vector3d settled = toLocal(pivot, rot, world);
        double slide = Math.hypot(settled.x - local.x, settled.z - local.z);
        report("tilt: no creep down an 8-degree deck", slide < 1e-3,
                String.format("slid %.4f blocks in 600 ticks", slide));
        report("tilt: still standing on it", Math.abs(settled.y - local.y) < 1e-3,
                String.format("dy %.3e", settled.y - local.y));
    }

    /** Rider input must track the deck slope. */
    static void walkingFollowsTheSlope() {
        float[] boxes = deck();
        double pitchDeg = 8;
        Quaternionf rot = rotYXZ(0, (float) pitchDeg, 0);
        Vector3d pivot = new Vector3d(0, 100, 0);
        Vector3d world = toWorld(pivot, rot, new Vector3d(0, 0, -10));
        double startWorldY = world.y;
        double vy = 0;
        double[] out = new double[3];
        for (int tick = 0; tick < 60; tick++) {
            vy = (vy - 0.08) * 0.98;
            Vec3d worldMove = new Vec3d(0, vy, 0.2);      // Walking +Z in world.
            Vector3d riderLocal = toLocal(pivot, rot, world);
            Quaternionf inverse = new Quaternionf(rot).conjugate();
            Vector3f move = inverse.transform(new Vector3f(
                    (float) worldMove.x, (float) worldMove.y, (float) worldMove.z));
            Vector3f grounded = inverse.transform(new Vector3f(
                    (float) worldMove.x, 0, (float) worldMove.z));
            boolean supported = DeckClip.sweep(boxes, 1,
                    riderLocal.x, riderLocal.y + HY, riderLocal.z, HX, HY, HZ,
                    move.x, move.y, move.z, grounded.x, grounded.z, STEP, true, out);
            if (supported) vy = 0;
            Vector3d correction = rot.transform(new Vector3d(
                    out[0] - move.x, out[1] - move.y, out[2] - move.z));
            world.add(worldMove.x + correction.x, worldMove.y + correction.y,
                    worldMove.z + correction.z);
        }
        Vector3d settled = toLocal(pivot, rot, world);
        double travelled = settled.z - (-10);
        double rise = world.y - startWorldY;
        // rotYXZ(0, +pitch, 0) tips the deck; +Z local goes down in world.
        double expected = -travelled * Math.sin(Math.toRadians(pitchDeg));
        report("slope: rider stays on the deck", Math.abs(settled.y) < 1e-3,
                String.format("local y %.3e", settled.y));
        report("slope: height follows the deck", Math.abs(rise - expected) < 2e-3,
                String.format("rose %.4f, deck gives %.4f over %.2f blocks",
                        rise, expected, travelled));
    }

    /**
     * Narrowing pass must be bit-for-bit invisible to sweep results. Tests random hulls, poses, movement.
     */
    static void narrowingChangesNothing() {
        Random random = new Random(4242L);
        double[] full = new double[3];
        double[] narrowed = new double[3];
        int mismatches = 0;
        int contacts = 0;
        for (int trial = 0; trial < 200000; trial++) {
            int count = 1 + random.nextInt(24);
            float[] boxes = new float[count * 6];
            for (int i = 0; i < count; i++) {
                int o = i * 6;
                float cx = (random.nextFloat() - 0.5f) * 12;
                float cy = (random.nextFloat() - 0.5f) * 6;
                float cz = (random.nextFloat() - 0.5f) * 12;
                float ex = 0.2f + random.nextFloat() * 2;
                float ey = 0.2f + random.nextFloat() * 2;
                float ez = 0.2f + random.nextFloat() * 2;
                boxes[o] = cx - ex; boxes[o + 1] = cy - ey; boxes[o + 2] = cz - ez;
                boxes[o + 3] = cx + ex; boxes[o + 4] = cy + ey; boxes[o + 5] = cz + ez;
            }
            double cx = (random.nextDouble() - 0.5) * 12;
            double cy = (random.nextDouble() - 0.5) * 6;
            double cz = (random.nextDouble() - 0.5) * 12;
            double mx = (random.nextDouble() - 0.5) * 1.2;
            double my = (random.nextDouble() - 0.5) * 1.2;
            double mz = (random.nextDouble() - 0.5) * 1.2;
            double gx = (random.nextDouble() - 0.5) * 1.2;
            double gz = (random.nextDouble() - 0.5) * 1.2;
            boolean onGround = random.nextBoolean();

            boolean downFull = DeckClip.sweep(boxes, count, cx, cy, cz, HX, HY, HZ,
                    mx, my, mz, gx, gz, STEP, onGround, full);

            float[] near = new float[count * 6];
            int nearCount = DeckClip.narrow(boxes, count, cx, cy, cz, HX, HY, HZ,
                    mx, my, mz, gx, gz, STEP, near);
            boolean downNarrow;
            if (nearCount == 0) {
                downNarrow = false;
                narrowed[0] = mx; narrowed[1] = my; narrowed[2] = mz;
            } else {
                downNarrow = DeckClip.sweep(near, nearCount, cx, cy, cz, HX, HY, HZ,
                        mx, my, mz, gx, gz, STEP, onGround, narrowed);
            }
            if (full[0] != mx || full[1] != my || full[2] != mz) {
                contacts++;
            }
            if (downFull != downNarrow || full[0] != narrowed[0]
                    || full[1] != narrowed[1] || full[2] != narrowed[2]) {
                mismatches++;
            }
        }
        report("narrow: identical to the full box set", mismatches == 0,
                String.format("%d mismatches over 200000 trials, %d with contact",
                        mismatches, contacts));
    }

    /**
     * Render lerp of rider position and slerp of hull rotation create sagitta error (chord vs arc).
     * This calculates whether render-side derivation justifies its implementation cost.
     */
    static void renderChordErrorIsSubVisible() {
        // RotV clamps: maxRotV 0.3 rad/tick, tightened by maxTipSpeed 3.6 / cornerRadius.
        double[] radii = {6.6, 10.2, 11.8, 46.5, 78.0};
        String[] names = {"Tank 8x3x10", "Jet 12x4x16", "Big jet 14x6x18",
                "Cargo 60x12x70", "Carrier 40x15x150"};
        double worstWalkable = 0;
        System.out.println("  chord-vs-arc error at the rotation clamp:");
        for (int i = 0; i < radii.length; i++) {
            double radius = radii[i];
            double theta = Math.min(0.3, 3.6 / radius);
            double sagitta = radius * (1 - Math.cos(theta / 2));
            System.out.printf("    %-20s r=%5.1f  rotV=%.3f  sagitta %.4f blocks%n",
                    names[i], radius, theta, sagitta);
            // Only big hulls are walkable; jets at 17 deg/tick are not decks.
            // Claim must hold for walkable hulls only.
            if (radius > 20 && sagitta > worstWalkable) {
                worstWalkable = sagitta;
            }
        }
        // Sagitta scales with the square of per-tick rotation; at quarter rate it is one-sixteenth.
        // These are ceilings, not typical cases.
        report("render: chord error on walkable hulls", worstWalkable < 0.05,
                String.format("worst %.4f blocks, at the clamp; falls off as rotV squared",
                        worstWalkable));
    }

    // ------------------------------------------------------------------ the publish

    /**
     * Published inverse rotation must undo published rotation, or every capture location moves.
     */
    static void publishedSnapshotIsSelfConsistent() {
        Random random = new Random(6060842L);
        double worst = 0;
        for (int trial = 0; trial < 20000; trial++) {
            Quaternionf rot = rotYXZ(random.nextFloat() * 720 - 360,
                    random.nextFloat() * 60 - 30, random.nextFloat() * 60 - 30);
            Vector3d pivot = new Vector3d(
                    (random.nextDouble() - 0.5) * 60000,
                    random.nextDouble() * 300,
                    (random.nextDouble() - 0.5) * 60000);
            float yaw = random.nextFloat() * 720 - 360;

            DeckSnapshot deck = new DeckSnapshot();
            deck.set(rot, yaw, pivot.x, pivot.y, pivot.z, 0, 0);

            check(deck.yaw() == yaw, "snapshot keeps the yaw it was published with");
            check(deck.pivotX() == pivot.x && deck.pivotY() == pivot.y && deck.pivotZ() == pivot.z,
                    "snapshot keeps the pivot it was published with");

            Vector3d world = new Vector3d(pivot).add(
                    (random.nextDouble() - 0.5) * 300,
                    (random.nextDouble() - 0.5) * 30,
                    (random.nextDouble() - 0.5) * 300);
            Vector3d back = toWorld(deck, toLocal(deck, world));
            worst = Math.max(worst, back.distance(world));
        }
        report("publish: stored inverse undoes stored rotation", worst < 1e-3,
                String.format("worst round trip %.3e blocks over 20000 poses", worst));
    }

    /**
     * Double buffering refills every other tick; survived fields would hold old hull geometry.
     */
    static void republishLeavesNothingBehind() {
        DeckSnapshot deck = new DeckSnapshot();

        Quaternionf poseA = rotYXZ(37, 4, -3);
        float[] boxesA = deck.boxBuffer(3);
        java.util.Arrays.fill(boxesA, 7.0f);
        deck.set(poseA, 37, 1000, 64, -2000, 3, 0);

        Quaternionf poseB = rotYXZ(-118, -9, 12);
        float[] boxesB = deck.boxBuffer(1);
        java.util.Arrays.fill(boxesB, 0, 6, -1.0f);
        deck.set(poseB, -118, -30, 200, 5, 1, 0);

        check(deck.count() == 1, "republish resets the box count");
        check(deck.yaw() == -118f, "republish resets the yaw");
        check(deck.pivotX() == -30 && deck.pivotY() == 200 && deck.pivotZ() == 5,
                "republish resets the pivot");
        check(deck.rotation().equals(poseB, 1e-6f), "republish resets the rotation");
        check(deck.boxes()[0] == -1.0f, "republish overwrites the boxes it was given");
        // Buffer is kept and reused, not reallocated.
        check(deck.boxes().length >= 18, "republish keeps the grown buffer");

        Vector3d probe = new Vector3d(-25, 202, 9);
        Vector3d back = toWorld(deck, toLocal(deck, probe));
        report("publish: a refilled snapshot carries nothing over",
                back.distance(probe) < 1e-6,
                String.format("round trip after refill %.3e blocks", back.distance(probe)));
    }

    // ------------------------------------------------------------------ harness

    static void check(boolean ok, String what) {
        if (!ok) {
            failures++;
            System.out.println("  FAIL  " + what);
        }
    }

    static void report(String name, boolean ok, String detail) {
        System.out.printf("%-46s %s   %s%n", name, ok ? "ok  " : "FAIL", detail);
        if (!ok) failures++;
    }

}
