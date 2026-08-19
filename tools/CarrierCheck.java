import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.ywzj.vehicle.vehicle.parenting.DeckFrame;
import org.ywzj.vehicle.vehicle.parenting.DeckSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Standalone verification of the carrier-deck geometry: frame arithmetic for parked-vehicle positioning.
 * Calls DeckFrame and DeckSnapshot directly to test the code as shipped.
 * Checks box bounds in the carrier frame (not world space) and Euler angle convention.
 *
 * Run:
 *   JOML=$(find ~/.gradle ~/.local/share/gradle -name 'joml-*.jar' ! -name '*sources*' | head -1)
 *   javac -d /tmp/carriercheck -cp "$JOML" \
 *       src/main/java/org/ywzj/vehicle/vehicle/parenting/DeckFrame.java \
 *       src/main/java/org/ywzj/vehicle/vehicle/parenting/DeckSnapshot.java tools/CarrierCheck.java
 *   java -cp "/tmp/carriercheck:$JOML" CarrierCheck
 */
public class CarrierCheck {

    static int failures = 0;

    /** A flight deck: 40 wide, 1 thick, 150 long, top at local y = 0. */
    static float[] flightDeck() {
        return new float[]{-20, -1, -75, 20, 0, 75};
    }

    /** The deck plus an island to starboard, the thing an aircraft crashes into rather than lands on. */
    static float[] deckAndIsland() {
        return new float[]{
                -20, -1, -75, 20, 0, 75,     // deck
                12, 0, -20, 20, 12, 20};     // island, 12 blocks tall
    }

    public static void main(String[] args) {
        boxesAreNotInflatedByPitch();
        localRoundTripIsIdentity();
        eulerRoundTripMatchesRotYXZ();
        eulerMatchesTheVehicleConvention();
        rotationComposesThroughTheCarrier();
        parkedPoseIsExactOverAThousandTicks();
        selectFindsOnlyWhatOverlaps();
        selectIsFrameExactUnderRoll();
        supportPicksTheDeckYouAreOn();
        supportSurvivesTheShipAndNotTheLift();
        snapshotKeepsDeckAndRiderBoxesApart();
        snapshotResetsBetweenFills();

        System.out.println(failures == 0
                ? "\nAll carrier-deck checks passed."
                : "\n" + failures + " check(s) FAILED.");
        if (failures != 0) {
            System.exit(1);
        }
    }

    // ------------------------------------------------------------------ checks

    /**
     * World-space bounds of a pitched deck cube bloat to metres thick; the frame stays exact.
     */
    static void boxesAreNotInflatedByPitch() {
        float[] deck = flightDeck();
        Quaternionf rotation = new Quaternionf();
        DeckFrame.fromEulerYXZ(0, 5, 0, rotation);

        // World-space bound half-height, for reference.
        double halfThickness = worldBoundHalfHeight(deck, rotation);

        // Frame-space test: a point above the deck stays at the same local height.

        DeckSnapshot deck1 = snapshot(rotation, 1000, 64, -2000, new float[0], deck);
        Vector3d world = DeckFrame.toWorld(deck1.rotation(),
                deck1.pivotX(), deck1.pivotY(), deck1.pivotZ(), 0, 1.0, 70, new Vector3d());
        Vector3d back = DeckFrame.toLocal(deck1.inverse(),
                deck1.pivotX(), deck1.pivotY(), deck1.pivotZ(), world.x, world.y, world.z,
                new Vector3d());

        report("world bound of a 150-block deck at 5 deg pitch is a slab",
                halfThickness > 6.0, "half-height " + fmt(halfThickness) + " blocks");
        report("frame keeps the same point 1.000 above the deck",
                Math.abs(back.y - 1.0) < 1e-4 && Math.abs(back.z - 70) < 1e-4,
                "y " + fmt(back.y) + ", z " + fmt(back.z));
    }

    /** Every point survives a trip into the carrier's frame and back, far from the origin. */
    static void localRoundTripIsIdentity() {
        Random random = new Random(20260816L);
        double worst = 0;
        Quaternionf rotation = new Quaternionf();
        Vector3d local = new Vector3d();
        Vector3d back = new Vector3d();
        for (int i = 0; i < 20000; i++) {
            DeckFrame.fromEulerYXZ(random.nextFloat() * 720 - 360,
                    random.nextFloat() * 60 - 30, random.nextFloat() * 60 - 30, rotation);
            double px = (random.nextDouble() - 0.5) * 60_000;
            double py = (random.nextDouble() - 0.5) * 400;
            double pz = (random.nextDouble() - 0.5) * 60_000;
            DeckSnapshot deck = snapshot(rotation, px, py, pz, new float[0], new float[0]);
            double x = px + (random.nextDouble() - 0.5) * 300;
            double y = py + (random.nextDouble() - 0.5) * 60;
            double z = pz + (random.nextDouble() - 0.5) * 300;
            DeckFrame.toLocal(deck.inverse(), deck.pivotX(), deck.pivotY(), deck.pivotZ(),
                    x, y, z, local);
            DeckFrame.toWorld(deck.rotation(), deck.pivotX(), deck.pivotY(), deck.pivotZ(),
                    local.x, local.y, local.z, back);
            worst = Math.max(worst, Math.max(Math.abs(back.x - x),
                    Math.max(Math.abs(back.y - y), Math.abs(back.z - z))));
        }
        report("point round trip, 20,000 poses to +/-30 km", worst < 1e-3,
                "worst " + fmt(worst) + " blocks");
    }

    /**
     * Euler composition and decomposition must invert each other.
     * Precision is limited by JOML's cosFromSin near 180 degrees; the per-tick compose path is tight.
     */
    static void eulerRoundTripMatchesRotYXZ() {
        Random random = new Random(4242L);
        Quaternionf rotation = new Quaternionf();
        Matrix3f basis = new Matrix3f();
        float[] out = new float[3];
        double worst = 0;
        double worstAwayFromHalfTurn = 0;
        for (int i = 0; i < 20000; i++) {
            float yaw = random.nextFloat() * 360 - 180;
            float pitch = random.nextFloat() * 160 - 80;
            float roll = random.nextFloat() * 360 - 180;
            DeckFrame.fromEulerYXZ(yaw, pitch, roll, rotation);
            DeckFrame.toEulerYXZ(rotation, basis, out);
            double error = Math.max(wrapped(out[0] - yaw),
                    Math.max(wrapped(out[1] - pitch), wrapped(out[2] - roll)));
            worst = Math.max(worst, error);
            if (Math.abs(yaw) < 170 && Math.abs(roll) < 170) {
                worstAwayFromHalfTurn = Math.max(worstAwayFromHalfTurn, error);
            }
        }
        report("euler round trip, 20,000 attitudes to +/-80 deg pitch", worst < 0.05,
                "worst " + fmt(worst) + " degrees");
        report("  ... away from a half turn, where JOML's cosFromSin is exact",
                worstAwayFromHalfTurn < 0.002, "worst " + fmt(worstAwayFromHalfTurn) + " degrees");
    }

    /**
     * Angles follow vehicle convention: yaw 90 faces west, positive pitch puts nose down.
     * Wrong signs mirror or invert parked aircraft silently.
     */
    static void eulerMatchesTheVehicleConvention() {
        Quaternionf rotation = new Quaternionf();
        DeckFrame.fromEulerYXZ(90, 0, 0, rotation);
        org.joml.Vector3f nose = rotation.transform(new org.joml.Vector3f(0, 0, 1));
        report("yaw 90 points the nose along -X, as vanilla yaw does",
                Math.abs(nose.x + 1) < 1e-5 && Math.abs(nose.z) < 1e-5,
                "nose (" + fmt(nose.x) + ", " + fmt(nose.y) + ", " + fmt(nose.z) + ")");

        DeckFrame.fromEulerYXZ(0, 30, 0, rotation);
        org.joml.Vector3f pitched = rotation.transform(new org.joml.Vector3f(0, 0, 1));
        report("pitch 30 puts the nose down, as vanilla xRot does",
                pitched.y < -0.49 && pitched.y > -0.51, "nose y " + fmt(pitched.y));

        DeckFrame.fromEulerYXZ(0, 0, 30, rotation);
        org.joml.Vector3f wing = rotation.transform(new org.joml.Vector3f(1, 0, 0));
        report("roll 30 lifts the +X wing", wing.y > 0.49 && wing.y < 0.51,
                "wing y " + fmt(wing.y));
    }

    /**
     * Local rotation composed through the carrier frame reproduces world attitude exactly.
     */
    static void rotationComposesThroughTheCarrier() {
        Random random = new Random(99L);
        Quaternionf carrier = new Quaternionf();
        Quaternionf child = new Quaternionf();
        Quaternionf local = new Quaternionf();
        Quaternionf back = new Quaternionf();
        Matrix3f basis = new Matrix3f();
        float[] out = new float[3];
        double worst = 0;
        double worstWingtip = 0;
        for (int i = 0; i < 20000; i++) {
            DeckFrame.fromEulerYXZ(random.nextFloat() * 360 - 180,
                    random.nextFloat() * 16 - 8, random.nextFloat() * 16 - 8, carrier);
            float yaw = random.nextFloat() * 360 - 180;
            float pitch = random.nextFloat() * 30 - 15;
            float roll = random.nextFloat() * 30 - 15;
            DeckFrame.fromEulerYXZ(yaw, pitch, roll, child);
            DeckFrame.toLocalRotation(new Quaternionf(carrier).conjugate(), child, local);
            DeckFrame.toWorldRotation(carrier, local, back);
            DeckFrame.toEulerYXZ(back, basis, out);
            worst = Math.max(worst, Math.max(wrapped(out[0] - yaw),
                    Math.max(wrapped(out[1] - pitch), wrapped(out[2] - roll))));
            // In blocks, at the tip of a 6-block wing.
            org.joml.Vector3f wanted = child.transform(new org.joml.Vector3f(6, 0, 0));
            org.joml.Vector3f got = DeckFrame.fromEulerYXZ(out[0], out[1], out[2], new Quaternionf())
                    .transform(new org.joml.Vector3f(6, 0, 0));
            worstWingtip = Math.max(worstWingtip, wanted.distance(got));
        }
        // JOML precision limit near half-turn angles on ship or aircraft.
        report("attitude through the carrier frame, 20,000 pairs", worst < 0.05,
                "worst " + fmt(worst) + " degrees");
        report("  ... at the tip of a 6-block wing", worstWingtip < 0.005,
                "worst " + fmt(worstWingtip) + " blocks");
    }

    /**
     * A parked vehicle gains and loses exactly nothing over 1000 ticks of carrier motion.
     * Pose derives from a static local position; nothing accumulates.
     */
    static void parkedPoseIsExactOverAThousandTicks() {
        // Parked 30 blocks forward of the pivot, 12 to port, sitting on the deck.
        Vector3d local = new Vector3d(-12, 0.0, 30);
        Quaternionf localRotation = new Quaternionf();
        DeckFrame.fromEulerYXZ(90, 0, 0, localRotation);

        Quaternionf rotation = new Quaternionf();
        Quaternionf world = new Quaternionf();
        Matrix3f basis = new Matrix3f();
        float[] euler = new float[3];
        Vector3d placed = new Vector3d();
        Vector3d recovered = new Vector3d();

        double worstDrift = 0;
        double worstHeight = 0;
        double worstWingtip = 0;
        double lastWingtip = 0;
        double px = 1_500_000, py = 63, pz = -1_500_000;
        for (int tick = 0; tick < 1000; tick++) {
            float yaw = tick * 0.6f;
            float pitch = (float) (4 * Math.sin(tick * 0.05));
            float roll = (float) (3 * Math.cos(tick * 0.037));
            DeckFrame.fromEulerYXZ(yaw, pitch, roll, rotation);
            px += 1.6 * Math.sin(Math.toRadians(yaw));
            pz += 1.6 * Math.cos(Math.toRadians(yaw));
            DeckSnapshot deck = snapshot(rotation, px, py, pz, new float[0], flightDeck());

            DeckFrame.toWorld(deck.rotation(), deck.pivotX(), deck.pivotY(), deck.pivotZ(),
                    local.x, local.y, local.z, placed);
            // Recover local position; drift shows as motion from the original.
            DeckFrame.toLocal(deck.inverse(), deck.pivotX(), deck.pivotY(), deck.pivotZ(),
                    placed.x, placed.y, placed.z, recovered);
            worstDrift = Math.max(worstDrift, Math.hypot(recovered.x - local.x, recovered.z - local.z));
            worstHeight = Math.max(worstHeight, Math.abs(recovered.y - local.y));

            // Attitude as visible error: wingtip distance from exact composition.
            // Folds in Euler round-trip through stored fields.
            DeckFrame.toWorldRotation(deck.rotation(), localRotation, world);
            DeckFrame.toEulerYXZ(world, basis, euler);
            org.joml.Vector3f exact = world.transform(new org.joml.Vector3f(6, 0, 0));
            org.joml.Vector3f stored = DeckFrame.fromEulerYXZ(euler[0], euler[1], euler[2],
                    new Quaternionf()).transform(new org.joml.Vector3f(6, 0, 0));
            lastWingtip = exact.distance(stored);
            worstWingtip = Math.max(worstWingtip, lastWingtip);
        }
        report("parked jet, 1,000 ticks, carrier at 1.5M turning and rolling",
                worstDrift < 1e-3 && worstHeight < 1e-3,
                "drift " + fmt(worstDrift) + ", height " + fmt(worstHeight) + " blocks");
        report("  ... wingtip error is a floor, not a drift",
                worstWingtip < 0.005 && lastWingtip <= worstWingtip,
                "worst " + fmt(worstWingtip) + ", final " + fmt(lastWingtip) + " blocks");
    }

    /** The narrowing is a filter: everything overlapping, nothing else, unchanged. */
    static void selectFindsOnlyWhatOverlaps() {
        float[] boxes = deckAndIsland();
        List<double[]> got = collect(boxes, 2, -25, -5, -80, 25, 5, -30);
        report("query below the island height finds the deck alone", got.size() == 1,
                got.size() + " boxes");

        got = collect(boxes, 2, 10, 0, -25, 25, 14, 25);
        report("query at the island finds both", got.size() == 2, got.size() + " boxes");

        got = collect(boxes, 2, -25, 40, -80, 25, 50, 80);
        report("query well above the ship finds nothing", got.isEmpty(), got.size() + " boxes");

        got = collect(boxes, 2, -100, -100, -100, 100, 100, 100);
        report("selected boxes come back byte-for-byte",
                got.size() == 2 && got.get(0)[0] == -20 && got.get(0)[4] == 0
                        && got.get(1)[1] == 0 && got.get(1)[4] == 12,
                "first box " + fmt(got.get(0)[0]) + ".." + fmt(got.get(0)[4]));
    }

    /**
     * Query on a rolled ship still finds deck boxes exactly one block thick.
     * Frame transforms the query, not the geometry.
     */
    static void selectIsFrameExactUnderRoll() {
        float[] boxes = flightDeck();
        Quaternionf rotation = new Quaternionf();
        DeckFrame.fromEulerYXZ(20, 6, 8, rotation);
        DeckSnapshot deck = snapshot(rotation, 0, 64, 0, new float[0], boxes);

        // A hull sitting a block above the deck at the stern, in world space.
        Vector3d hull = DeckFrame.toWorld(deck.rotation(), deck.pivotX(), deck.pivotY(),
                deck.pivotZ(), 0, 1.5, -60, new Vector3d());
        Vector3d local = DeckFrame.toLocal(deck.inverse(), deck.pivotX(), deck.pivotY(),
                deck.pivotZ(), hull.x, hull.y, hull.z, new Vector3d());
        List<double[]> got = collect(boxes, 1,
                local.x - 4, local.y - 2, local.z - 6,
                local.x + 4, local.y + 2, local.z + 6);
        report("rolled carrier: the deck under the hull is found", got.size() == 1,
                got.size() + " boxes");
        report("rolled carrier: the deck is still 1.000 thick",
                got.size() == 1 && Math.abs((got.get(0)[4] - got.get(0)[1]) - 1.0) < 1e-6,
                got.isEmpty() ? "none" : fmt(got.get(0)[4] - got.get(0)[1]));
    }

    /**
     * Support must pick the correct deck under a multi-deck carrier, not a surface far below.
     */
    static void supportPicksTheDeckYouAreOn() {
        // Flight deck top at 0, hangar deck top at -8, both spanning the same footprint.
        float[] decks = {
                -20, -1, -75, 20, 0, 75,
                -20, -9, -60, 20, -8, 60};
        double reach = 5;  // a hull about four blocks tall

        double top = DeckFrame.supportUnder(decks, 2, 0, 0.1, 20, reach);
        report("aircraft on the flight deck reads the flight deck", Math.abs(top) < 1e-6,
                "top " + fmt(top));

        top = DeckFrame.supportUnder(decks, 2, 0, -7.9, 20, reach);
        report("aircraft in the hangar reads the hangar", Math.abs(top + 8) < 1e-6,
                "top " + fmt(top));

        // Off the side of the ship entirely.
        top = DeckFrame.supportUnder(decks, 2, 30, 0.1, 20, reach);
        report("off the beam reads nothing", Double.isNaN(top), "top " + fmt(top));

        // Above the deck by more than a hull: in the air over the ship, not parked on it.
        top = DeckFrame.supportUnder(decks, 2, 0, 40, 20, reach);
        report("in the air above the deck reads nothing", Double.isNaN(top), "top " + fmt(top));

        // Past the bow, where the flight deck ends but the hangar has already ended too.
        top = DeckFrame.supportUnder(decks, 2, 0, 0.1, 70, reach);
        report("forward of the hangar still reads the flight deck", Math.abs(top) < 1e-6,
                "top " + fmt(top));
    }

    /**
     * Support must survive ship motion but wake a vehicle when deck geometry moves.
     */
    static void supportSurvivesTheShipAndNotTheLift() {
        float[] deck = flightDeck();
        boolean held = DeckFrame.supportsAt(deck, 1, -12, 30, 0, 0.1);
        report("sleeping aircraft is still supported", held, held ? "" : "NOT HELD");

        // Ship motion transforms to the frame, leaving stored numbers unchanged.
        Quaternionf rotation = new Quaternionf();
        DeckFrame.fromEulerYXZ(217, 7, 5, rotation);
        DeckSnapshot moved = snapshot(rotation, 900_000, 71, -430_000, new float[0], deck);
        report("... after the ship has moved 900 km and rolled",
                DeckFrame.supportsAt(moved.deckBoxes(), moved.deckCount(), -12, 30, 0, 0.1),
                "");

        // Lift lowers deck; aircraft must wake to follow.
        float[] lowered = {-20, -1, -75, 20, 0, 20, -20, -3, 20, 20, -2, 40};
        report("... but not once the lift under it goes down",
                !DeckFrame.supportsAt(lowered, 2, -12, 30, 0, 0.1), "");
        report("... and the aircraft beside the lift stays asleep",
                DeckFrame.supportsAt(lowered, 2, -12, 10, 0, 0.1), "");
    }

    /** Riders walk the whole hull; vehicles land only on what is declared. */
    static void snapshotKeepsDeckAndRiderBoxesApart() {
        DeckSnapshot deck = snapshot(new Quaternionf(), 0, 0, 0,
                new float[]{-1, -1, -1, 1, 1, 1, -2, -2, -2, 2, 2, 2},  // two hull cubes
                flightDeck());                                          // one of them declared
        report("rider boxes carry the whole hull", deck.count() == 2, "count " + deck.count());
        report("deck boxes carry only the declaration", deck.deckCount() == 1,
                "deckCount " + deck.deckCount());
        report("the two sets are separate arrays", deck.boxes() != deck.deckBoxes(), "");
    }

    /** Refilled buffers must not carry over state from the previous vehicle. */
    static void snapshotResetsBetweenFills() {
        DeckSnapshot deck = snapshot(new Quaternionf(), 0, 0, 0,
                new float[]{-1, -1, -1, 1, 1, 1}, flightDeck());
        report("filled once", deck.count() == 1 && deck.deckCount() == 1,
                deck.count() + "/" + deck.deckCount());
        // Refilled with a vehicle that has no deck.
        deck.set(new Quaternionf(), 0, 0, 0, 0, 1, 0);
        report("a vehicle with no deck reads as none", deck.deckCount() == 0,
                "deckCount " + deck.deckCount());
    }

    // ------------------------------------------------------------------ helpers

    static DeckSnapshot snapshot(Quaternionf rotation, double px, double py, double pz,
                                 float[] hullBoxes, float[] deckBoxes) {
        DeckSnapshot deck = new DeckSnapshot();
        int hullCount = hullBoxes.length / 6;
        int deckCount = deckBoxes.length / 6;
        if (hullCount > 0) {
            System.arraycopy(hullBoxes, 0, deck.boxBuffer(hullCount), 0, hullBoxes.length);
        }
        if (deckCount > 0) {
            System.arraycopy(deckBoxes, 0, deck.deckBoxBuffer(deckCount), 0, deckBoxes.length);
        }
        deck.set(rotation, 0, px, py, pz, hullCount, deckCount);
        return deck;
    }

    static List<double[]> collect(float[] boxes, int count, double minX, double minY, double minZ,
                                  double maxX, double maxY, double maxZ) {
        List<double[]> out = new ArrayList<>();
        DeckFrame.select(boxes, count, minX, minY, minZ, maxX, maxY, maxZ,
                (a, b, c, d, e, f) -> out.add(new double[]{a, b, c, d, e, f}));
        return out;
    }

    /** Half-height of the world-space bound of a deck cube, showing the cost of naive implementation. */
    static double worldBoundHalfHeight(float[] box, Quaternionf rotation) {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < 8; i++) {
            org.joml.Vector3f corner = new org.joml.Vector3f(
                    (i & 1) == 0 ? box[0] : box[3],
                    (i & 2) == 0 ? box[1] : box[4],
                    (i & 4) == 0 ? box[2] : box[5]);
            rotation.transform(corner);
            min = Math.min(min, corner.y);
            max = Math.max(max, corner.y);
        }
        return (max - min) * 0.5;
    }

    static double wrapped(double degrees) {
        double d = degrees % 360;
        if (d > 180) {
            d -= 360;
        }
        if (d < -180) {
            d += 360;
        }
        return Math.abs(d);
    }

    static String fmt(double value) {
        return String.format(java.util.Locale.ROOT, "%.6f", value);
    }

    static void report(String name, boolean passed, String detail) {
        if (!passed) {
            failures++;
        }
        System.out.printf("%-4s %-58s %s%n", passed ? "ok" : "FAIL", name, detail);
    }

}
