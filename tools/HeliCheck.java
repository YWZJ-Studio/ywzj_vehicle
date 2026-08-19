import org.joml.Vector3f;
import org.ywzj.vehicle.vehicle.solver.AircraftAerodynamics;

/**
 * Standalone verification of rotary-wing flight envelope.
 * Calls AircraftAerodynamics directly and reproduces RotaryWingVehicle.tickMove arithmetic.
 * Validates flight envelope against Superb Warfare's AH-6 reference data.
 *
 * Run:
 *   JOML=$(find ~/.gradle ~/.local/share/gradle -name 'joml-*.jar' ! -name '*sources*' | head -1)
 *   javac -d /tmp/helicheck -cp "$JOML" \
 *       src/main/java/org/ywzj/vehicle/vehicle/solver/AircraftAerodynamics.java tools/HeliCheck.java
 *   java -cp "/tmp/helicheck:$JOML" HeliCheck
 */
public class HeliCheck {

    /** PhysicsEngine.G */
    static final double G = 9.8 / 400;

    static int failures = 0;
    static int checks = 0;

    /** One vehicle's tuning, as the pack JSON supplies it. */
    record Heli(String name, double rotorForce, double maxAirSpeed) {

        double thrustToWeight() {
            return rotorForce / G;
        }
    }

    /** Where a held attitude ends up: ground speed in blocks/s, and height gained over the run. */
    record Flight(double speed, double verticalVelocity, double heightChange, double collective) {}

    static final Heli AH_6 = new Heli("ah_6", 0.0343, 1.2);
    static final Heli Z10 = new Heli("z10", 0.0392, 1.5);

    public static void main(String[] args) {
        AircraftAerodynamics stock = new AircraftAerodynamics();

        System.out.println("=== defaults ===");
        System.out.printf("  pitchCouplingFull   %.1f deg%n", stock.pitchCouplingFull);
        System.out.printf("  discTiltGain        %.2f%n", stock.discTiltGain);
        System.out.printf("  altitudeHoldDeadband %.4f b/t%n", stock.altitudeHoldDeadband);
        System.out.printf("  ah_6 thrust/weight  %.2f%n", AH_6.thrustToWeight());

        holdsAltitude();
        holdSettlesWithoutBobbing();
        acceleratesOnALean();
        leanBuysSpeedMonotonically();
        climbGainsHeight();
        engineOffFalls();
        collectiveMargin();
        regressionAgainstTheOldTuning();

        System.out.println();
        System.out.println(checks + " checks");
        if (failures == 0) {
            System.out.println("All rotary-wing checks passed.");
        } else {
            System.out.println(failures + " FAILED");
            System.exit(1);
        }
    }

    // ---------------------------------------------------------------------------------- checks

    /** Level flight, hands off collective, for 60 seconds. */
    static void holdsAltitude() {
        System.out.println();
        System.out.println("=== hover, hands off, 60 s ===");
        for (Heli heli : new Heli[]{AH_6, Z10}) {
            Flight f = fly(heli, aero(), 0, 1200);
            System.out.printf("  %-5s dY %+8.3f blocks   vy %+.5f b/t%n",
                    heli.name(), f.heightChange(), f.verticalVelocity());
            check(heli.name() + " hover holds height", Math.abs(f.heightChange()) < 0.5);
            check(heli.name() + " hover vy near zero", Math.abs(f.verticalVelocity()) < 0.01);
        }
    }

    /**
     * Shallow lean accelerates the helicopter. Compared to Superb Warfare's AH-6 within broad tolerance.
     */
    static void acceleratesOnALean() {
        System.out.println();
        System.out.println("=== held lean, 30 s   (superb AH-6 in brackets) ===");
        double[][] reference = {{5, 13.6}, {10, 17.9}, {15, 21.5}};
        for (double[] r : reference) {
            Flight f = fly(AH_6, aero(), r[0], 600);
            System.out.printf("  %4.0f deg  %6.2f b/s  [%5.2f]   dY %+8.2f%n",
                    r[0], f.speed(), r[1], f.heightChange());
            check(String.format("ah_6 %.0f deg within 30%% of superb", r[0]),
                    f.speed() > r[1] * 0.7 && f.speed() < r[1] * 1.3);
        }
        // 5-15 degrees is the band pilots fly in.
        check("ah_6 holds height on a 10 deg lean",
                Math.abs(fly(AH_6, aero(), 10, 600).heightChange()) < 2.0);
    }

    /** More lean, more speed, with no fold-over where the coupling ramp meets the clamp. */
    static void leanBuysSpeedMonotonically() {
        System.out.println();
        System.out.println("=== speed rises with lean ===");
        double previous = -1;
        for (double pitch : new double[]{0, 2, 5, 10, 15, 20}) {
            Flight f = fly(Z10, aero(), pitch, 600);
            System.out.printf("  %4.0f deg  %6.2f b/s%n", pitch, f.speed());
            check(String.format("z10 %.0f deg faster than the lean below it", pitch),
                    f.speed() > previous);
            previous = f.speed();
        }
    }

    /** Nose up bleeds speed. It must not also cost height. */
    static void climbGainsHeight() {
        System.out.println();
        System.out.println("=== nose up 10 deg, 30 s ===");
        Flight f = fly(AH_6, aero(), -10, 600);
        System.out.printf("  dY %+8.2f blocks   vy %+.5f b/t%n", f.heightChange(), f.verticalVelocity());
        check("nose up does not sink", f.heightChange() > -2.0);
    }

    /** No power, no lift. The trim must not fly the machine on the pilot's behalf. */
    static void engineOffFalls() {
        System.out.println();
        System.out.println("=== engine off ===");
        Flight f = fly(AH_6, aero(), 0, 100, 0.0);
        System.out.printf("  dY %+8.2f blocks over 5 s   vy %+.4f b/t%n",
                f.heightChange(), f.verticalVelocity());
        check("engine off falls", f.heightChange() < -10);
        check("engine off reaches terminal fall", f.verticalVelocity() < -0.4);
    }

    /**
     * Altitude hold must converge and stop, not bob.
     * Deadband hides sink rate; collective is synched, so unsettled trim sends constant packets.
     */
    static void holdSettlesWithoutBobbing() {
        System.out.println();
        System.out.println("=== hold settles ===");
        AircraftAerodynamics a = aero();
        System.out.printf("  deadband %.4f b/t = %.3f blocks/s it will never correct%n",
                a.altitudeHoldDeadband, a.altitudeHoldDeadband * 20);
        check("deadband under 0.01 blocks/s", a.altitudeHoldDeadband * 20 < 0.01);

        Flight near = fly(AH_6, aero(), 0, 1200);
        Flight far = fly(AH_6, aero(), 0, 3600);
        System.out.printf("  collective at 60 s %.4f, at 180 s %.4f%n", near.collective(), far.collective());
        check("collective stops moving", Math.abs(near.collective() - far.collective()) < 1e-4);

        // Lean from trimmed hover: initial dip cues the pilot; no second dip prevents sluggishness.
        double peak = 0;
        int crossings = 0;
        double previous = 0;
        for (int t = 1; t <= 400; t++) {
            double vy = fly(AH_6, aero(), 10, t).verticalVelocity();
            peak = Math.min(peak, vy);
            if (t > 20 && previous < 0 && vy >= 0) {
                crossings++;
            }
            previous = vy;
        }
        System.out.printf("  10 deg snap: dips to %+.4f b/t, %d overshoot(s)%n", peak, crossings);
        check("snap dip stays under half a block a second", peak > -0.025);
        check("hold does not bob", crossings <= 1);
    }

    /**
     * Maximum lean angle before running out of collective.
     * Pack rotor force determines this limit; property worth knowing when choosing a helicopter.
     */
    static void collectiveMargin() {
        System.out.println();
        System.out.println("=== collective margin ===");
        for (Heli heli : new Heli[]{AH_6, Z10}) {
            double last = 0;
            for (double pitch = 1; pitch <= 45; pitch += 1) {
                if (fly(heli, aero(), pitch, 600).collective() >= 99.9) {
                    break;
                }
                last = pitch;
            }
            System.out.printf("  %-5s T/W %.2f  holds height to %2.0f deg of lean%n",
                    heli.name(), heli.thrustToWeight(), last);
            check(heli.name() + " holds height through the 10 deg the report was about", last >= 10);
        }
    }

    /**
     * Old tuning with current flight model shows the improvement from new constants.
     * Tests pitch trade and altitude hold changes; not a faithful replay due to structural changes.
     */
    static void regressionAgainstTheOldTuning() {
        System.out.println();
        System.out.println("=== the old constants, on the current model ===");
        AircraftAerodynamics old = aero();
        old.pitchCouplingFull = 15.0f;
        old.discTiltGain = 1.0f;
        old.altitudeHoldDeadband = 0.02f;
        old.altitudeHoldGain = 1.0e9f;
        Flight hover = fly(AH_6, old, 0, 1200, 1.0, true);
        Flight lean = fly(AH_6, old, 10, 600, 1.0, true);
        System.out.printf("  hover 60 s      dY %+8.2f blocks%n", hover.heightChange());
        System.out.printf("  10 deg lean     %6.2f b/s   dY %+8.2f%n", lean.speed(), lean.heightChange());
        check("old tuning did sink in the hover", hover.heightChange() < -20);
        check("old tuning was under half speed at 10 deg", lean.speed() < 9);

        Flight now = fly(AH_6, aero(), 10, 600);
        System.out.printf("  now             %6.2f b/s   dY %+8.2f%n", now.speed(), now.heightChange());
        check("10 deg is at least twice as fast now", now.speed() > lean.speed() * 2);
    }

    // ----------------------------------------------------------------------------------- model

    static AircraftAerodynamics aero() {
        return new AircraftAerodynamics();
    }

    static Flight fly(Heli heli, AircraftAerodynamics aero, double pitchDeg, int ticks) {
        return fly(heli, aero, pitchDeg, ticks, 1.0, false);
    }

    static Flight fly(Heli heli, AircraftAerodynamics aero, double pitchDeg, int ticks, double power) {
        return fly(heli, aero, pitchDeg, ticks, power, false);
    }

    /**
     * One vehicle at fixed attitude with collective on trim, yaw zero (nose +Z).
     *
     * @param legacyHoldSample samples vertical velocity before gravity subtraction
     */
    static Flight fly(Heli heli, AircraftAerodynamics aero, double pitchDeg, int ticks,
                      double power, boolean legacyHoldSample) {
        double theta = Math.toRadians(pitchDeg);
        // Pitch is nose-down positive, so the body up axis leans forward onto +Z by sin(pitch).
        double upY = Math.cos(theta);
        double upZ = Math.sin(theta);

        double collective = power > 0 ? 100.0 * G / heli.rotorForce() : 0;
        Vector3f v = new Vector3f();
        double height = 0;

        for (int tick = 0; tick < ticks; tick++) {
            double scale = power * (collective / 100.0);

            // Airspeed along rotor axis: climb costs efficiency, descent restores it.
            double axial = v.y * upY + v.z * upZ;
            if (axial > 0) {
                scale *= Math.min(1, 8 / (axial * 20));
            } else if (axial < 0) {
                scale *= Math.min(2, Math.max(1, -axial / 0.05));
            }

            double tilt = aero.liftTiltCompensation((float) upY);
            double thrust = scale * heli.rotorForce() * tilt;
            v.y += (float) (thrust * upY);
            v.z += (float) (thrust * upZ * aero.discTiltGain);

            aero.applyToVelocity(v, 0, (float) pitchDeg, false);

            if (v.length() >= heli.maxAirSpeed()) {
                v.normalize((float) heli.maxAirSpeed());
            }

            double sample = legacyHoldSample ? v.y : v.y - G;
            float delta = aero.altitudeHoldDelta((float) sample);
            if (delta != 0 && power > 0) {
                collective = clamp(collective + delta * 100.0, 0, 100);
            }

            v.y -= (float) G;
            height += v.y;
        }
        return new Flight(Math.hypot(v.x, v.z) * 20, v.y, height, collective);
    }

    static double clamp(double value, double min, double max) {
        return value < min ? min : Math.min(value, max);
    }

    static void check(String what, boolean ok) {
        checks++;
        if (!ok) {
            failures++;
            System.out.println("    FAIL: " + what);
        }
    }

}
