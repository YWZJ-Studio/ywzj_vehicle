package org.ywzj.vehicle.vehicle;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.collision.SweptHull;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

/**
 * Per-tick ledger of everything that moves a vehicle vertically, and who moved it. Bouncing
 * diagnosis requires tracking each contributor separately: gravity, contact cancellation,
 * embedded-hull lift, step-up teleport, and rotation all write to the same height in one tick.
 * The ledger closes against actual movement; Source.MOVE is whatever no source claimed.
 */
public final class PhysicsTrace {

    /** Ticks kept. Two minutes at 20Hz; long enough to see a period, short enough to hold. */
    public static final int MAX_TICKS = 2400;

    /** Substeps kept. A tick can produce up to sixteen, so this is a few thousand ticks' worth. */
    public static final int MAX_SWEEPS = 8000;

    /** Overlap deep enough to call penetration rather than contact slop, in blocks. */
    private static final double PENETRATION_THRESHOLD = 0.05;

    /** Requested movement above which a substep counts as "trying to go somewhere". */
    private static final double STALL_REQUEST = 0.005;

    /** Fraction of a step below which the vehicle may as well have been allowed nothing. */
    private static final double STALL_TOI = 0.02;

    /**
     * Upward movement in one tick, while touching something, that counts as a launch rather than
     * settling noise. Gravity is 0.0245 per tick, so this is under one tick of fall.
     */
    private static final double LAUNCH_THRESHOLD = 0.02;

    /** A tick is "settled" when it is in contact and barely moved. */
    private static final double SETTLED_THRESHOLD = 0.001;

    public enum Source {

        // Contact velocity cancellation and bounce from impact.
        IMPACT(false),
        /** The lift given to a hull judged to be buried in geometry. */
        SUPPORT_LIFT(false),
        /** The flat 0.1 applied when the hull's centre is inside a solid block. */
        CENTRE_KICK(false),
        /** Free fall. */
        GRAVITY(false),
        /** Downward velocity clamped away because the centre of gravity is supported. */
        SUPPORT_CLAMP(false),
        /** Contact friction scaling the whole velocity vector, which tilts the vertical part. */
        FRICTION(false),
        /**
         * Velocity written outside the physics stages: engine thrust, aiStep drag, mod compat.
         * Derived like MOVE; it is whatever the tick's real velocity change was that none of
         * the sources above claimed.
         */
        EXTERNAL(false),
        /** climb()'s step-up teleport; the tick-level lift, measured from contacts. */
        CLIMB(true),
        /** stepUp()'s lift; the substep-level retry of a clipped horizontal leg. */
        STEP_UP(true),
        /** Position carried by rotating about a support edge. */
        ROTATION(true),
        /** Ordinary movement, plus anything no other source claimed. */
        MOVE(true);

        /** Whether this source writes position directly rather than velocity. */
        public final boolean position;

        Source(boolean position) {
            this.position = position;
        }

    }

    /**
     * @param bySource vertical contribution per Source, indexed by ordinal
     */
    public record Sample(long gameTime, double y, double vy, double yDelta, double vyDelta,
                         boolean onGround, boolean supported, float xRot, float zRot, float rotV,
                         int contacts, int bottomContacts, int blockingContacts,
                         double[] bySource) {

        double of(Source source) {
            return bySource[source.ordinal()];
        }

    }

    /**
     * One movement substep and what the swept-hull backstop did about it. The vertical ledger
     * answers "what lifted it"; this answers whether the hull ended inside a block and whether
     * the backstop was consulted. Sampled per substep because a tick can take sixteen of them,
     * and a hull clear at the end may have passed through geometry in the middle.
     *
     * @param mx          the step the vehicle asked for, not the one it got; multiply by toi for
     *                    what was actually allowed. Recording taken step alone hid a deadlock:
     *                    with toi zero every row read as motionless.
     * @param toi         fraction of the substep actually taken
     * @param penetration deepest overlap with the world after the substep, in blocks
     */
    public record Sweep(long gameTime, int substep, int substeps,
                        double x, double y, double z,
                        double mx, double my, double mz,
                        double toi, SweptHull.Outcome outcome, int boxes,
                        double penetration, double axisX, double axisY, double axisZ,
                        double blockerX, double blockerY, double blockerZ,
                        float xRot, float yRot, float zRot, float rotV) {

        public boolean penetrating() {
            return penetration >= PENETRATION_THRESHOLD;
        }

        /** Asked to move and allowed essentially nothing; the signature of being wedged. */
        public boolean stalled() {
            return Math.sqrt(mx * mx + my * my + mz * mz) > STALL_REQUEST && toi < STALL_TOI;
        }

    }

    private static final Source[] SOURCES = Source.values();

    private static final SweptHull.Outcome[] OUTCOMES = SweptHull.Outcome.values();

    /** Sources that can throw a vehicle upward off something it is already touching. */
    private static final Source[] LAUNCH_SOURCES =
            {Source.CLIMB, Source.STEP_UP, Source.SUPPORT_LIFT, Source.CENTRE_KICK, Source.ROTATION};

    private final int vehicleId;
    private final String vehicleName;
    private final Deque<Sample> samples = new ArrayDeque<>();
    private final Deque<Sweep> sweeps = new ArrayDeque<>();

    /** Reused across every substep of every tick, so tracing allocates nothing per substep. */
    private final SweptHull.Probe probe = new SweptHull.Probe();

    private boolean recording = true;
    private int remainingTicks;

    private final double[] bySource = new double[SOURCES.length];
    private double attributedSinceMark;
    private double startY;
    private double startVy;
    private boolean supported;
    private int contacts;
    private int bottomContacts;
    private int blockingContacts;

    public PhysicsTrace(AbstractVehicle vehicle, int ticks) {
        this.vehicleId = vehicle.getId();
        this.vehicleName = vehicle.getType().toShortString();
        this.remainingTicks = ticks;
    }

    public boolean isRecording() {
        return recording;
    }

    public void stop() {
        recording = false;
    }

    public int size() {
        return samples.size();
    }

    // ---- recording, called from the physics tick ----

    public void beginTick(AbstractVehicle vehicle) {
        if (!recording) {
            return;
        }
        Arrays.fill(bySource, 0);
        attributedSinceMark = 0;
        startY = vehicle.getY();
        startVy = vehicle.getDeltaMovement().y;
        supported = false;
        contacts = 0;
        bottomContacts = 0;
        blockingContacts = 0;
    }

    public void endTick(AbstractVehicle vehicle) {
        if (!recording) {
            return;
        }
        double y = vehicle.getY();
        double vy = vehicle.getDeltaMovement().y;
        double claimedPosition = 0;
        double claimedVelocity = 0;
        for (Source source : SOURCES) {
            if (source.position) {
                claimedPosition += bySource[source.ordinal()];
            } else {
                claimedVelocity += bySource[source.ordinal()];
            }
        }
        // Close both ledgers against actual movement. Position normally resolves to the previous
        // tick's velocity; velocity to engine thrust. Anything else is a mover this file does
        // not know about, which is why these are derived rather than declared.
        bySource[Source.MOVE.ordinal()] += (y - startY) - claimedPosition;
        bySource[Source.EXTERNAL.ordinal()] += (vy - startVy) - claimedVelocity;

        samples.addLast(new Sample(vehicle.level().getGameTime(), y, vy, y - startY, vy - startVy,
                vehicle.onGround(), supported, vehicle.getXRot(), vehicle.getZRot(),
                vehicle.physicsEngine.rotV, contacts, bottomContacts, blockingContacts,
                bySource.clone()));
        while (samples.size() > MAX_TICKS) {
            samples.removeFirst();
        }
        if (remainingTicks > 0 && --remainingTicks == 0) {
            recording = false;
        }
    }

    /** Resets the "already attributed" running total, so remainder() can close a stage. */
    public void mark() {
        attributedSinceMark = 0;
    }

    public void add(Source source, double amount) {
        bySource[source.ordinal()] += amount;
        attributedSinceMark += amount;
    }

    /**
     * Attributes to source whatever part of total has not already been claimed since the last
     * mark(). Lets a stage be measured as a whole while still crediting individual parts.
     */
    public void remainder(Source source, double total) {
        add(source, total - attributedSinceMark);
    }

    public void supported() {
        supported = true;
    }

    public void contacts(int total, int bottom, int blocking) {
        contacts = total;
        bottomContacts = bottom;
        blockingContacts = blocking;
    }

    /** The probe to hand to SweptHull, so the sweep can report what it decided. */
    public SweptHull.Probe probe() {
        return probe;
    }

    /**
     * Records one movement substep. Call after the working hull is advanced, with probe()
     * still holding the sweep's answer and penetration measurement. Pose comes from the rig,
     * not the entity; the pipeline moves its shadow pose and flushes it afterwards.
     */
    public void sweep(AbstractVehicle vehicle, PhysicsRig rig, int substep, int substeps, Vec3 requested) {
        if (!recording) {
            return;
        }
        AABB blocker = probe.blocker != null ? probe.blocker : probe.penetrator;
        Vec3 at = blocker != null ? blocker.getCenter() : Vec3.ZERO;
        sweeps.addLast(new Sweep(vehicle.level().getGameTime(), substep, substeps,
                rig.x, rig.y, rig.z,
                requested.x, requested.y, requested.z,
                probe.toi, probe.outcome, probe.boxes,
                probe.penetration, probe.penetrationAxis.x, probe.penetrationAxis.y,
                probe.penetrationAxis.z, at.x, at.y, at.z,
                rig.getXRot(), rig.getYRot(), rig.getZRot(),
                vehicle.physicsEngine.rotV));
        while (sweeps.size() > MAX_SWEEPS) {
            sweeps.removeFirst();
        }
    }

    // ---- reporting ----

    private record Stat(int ticks, double total, double max) {}

    private Stat stat(Source source) {
        int ticks = 0;
        double total = 0;
        double max = 0;
        for (Sample sample : samples) {
            double value = sample.of(source);
            if (value == 0) {
                continue;
            }
            ticks++;
            total += value;
            if (Math.abs(value) > Math.abs(max)) {
                max = value;
            }
        }
        return new Stat(ticks, total, max);
    }

    /** How much a tick threw the vehicle upward while it was already touching something. */
    private static double launch(Sample sample) {
        if (sample.contacts() == 0) {
            return 0;
        }
        double up = 0;
        for (Source source : LAUNCH_SOURCES) {
            double value = sample.of(source);
            if (value > 0) {
                up += value;
            }
        }
        return up;
    }

    /** Answers one question: is this vehicle settling, and if not, what is lifting it? */
    public List<String> report() {
        List<String> lines = new ArrayList<>();
        if (samples.isEmpty()) {
            lines.add("physics trace: " + label() + " — no ticks recorded yet");
            return lines;
        }
        int total = samples.size();
        lines.add(String.format(Locale.ROOT, "physics trace: %s — %d ticks (%.1fs)%s",
                label(), total, total / 20.0, recording ? "" : ", stopped"));

        int airborne = 0;
        int settled = 0;
        int launches = 0;
        double launchTotal = 0;
        double launchMax = 0;
        long firstLaunch = -1;
        long lastLaunch = -1;
        List<Integer> airborneRuns = new ArrayList<>();
        int run = 0;
        for (Sample sample : samples) {
            if (sample.contacts() == 0) {
                airborne++;
                run++;
            } else {
                if (run > 0) {
                    airborneRuns.add(run);
                }
                run = 0;
                if (Math.abs(sample.yDelta()) < SETTLED_THRESHOLD) {
                    settled++;
                }
            }
            double up = launch(sample);
            if (up >= LAUNCH_THRESHOLD) {
                launches++;
                launchTotal += up;
                launchMax = Math.max(launchMax, up);
                if (firstLaunch < 0) {
                    firstLaunch = sample.gameTime();
                }
                lastLaunch = sample.gameTime();
            }
        }
        if (run > 0) {
            airborneRuns.add(run);
        }

        lines.add(String.format(Locale.ROOT, "  airborne %d ticks (%d%%), settled %d ticks (%d%%)",
                airborne, airborne * 100 / total, settled, settled * 100 / total));
        if (!airborneRuns.isEmpty()) {
            int sum = 0;
            int longest = 0;
            for (int length : airborneRuns) {
                sum += length;
                longest = Math.max(longest, length);
            }
            // Short hops and one long flight read very differently.
            lines.add(String.format(Locale.ROOT, "  airborne runs: %d, mean %.1f ticks, longest %d",
                    airborneRuns.size(), (double) sum / airborneRuns.size(), longest));
        }

        if (launches == 0) {
            lines.add("  no launches: nothing threw it upward while it was in contact");
        } else {
            String interval = launches > 1 && lastLaunch > firstLaunch
                    ? String.format(Locale.ROOT, ", every ~%.0f ticks",
                            (double) (lastLaunch - firstLaunch) / (launches - 1))
                    : "";
            lines.add(String.format(Locale.ROOT,
                    "  launches while in contact: %d%s, mean +%.4f, max +%.4f",
                    launches, interval, launchTotal / launches, launchMax));
            for (Source source : LAUNCH_SOURCES) {
                int ticks = 0;
                double sum = 0;
                for (Sample sample : samples) {
                    if (launch(sample) >= LAUNCH_THRESHOLD && sample.of(source) > 0) {
                        ticks++;
                        sum += sample.of(source);
                    }
                }
                if (ticks > 0) {
                    lines.add(String.format(Locale.ROOT, "    %-13s %3d/%d launches, total +%.4f",
                            source, ticks, launches, sum));
                }
            }
        }

        appendLedger(lines, "  vertical velocity, by source", false);
        appendLedger(lines, "  vertical position, by source", true);
        appendSweeps(lines);
        return lines;
    }

    /**
     * What the tunnelling backstop did and how deep the hull got anyway. Read ALREADY_INSIDE
     * first; that outcome means the sweep declined to clip the step because the hull was
     * overlapping before it began. Ramps keep the hull in permanent light overlap, which
     * switches the backstop off precisely where the vehicle is moving fastest.
     */
    private void appendSweeps(List<String> lines) {
        if (sweeps.isEmpty()) {
            lines.add("  sweeps: none recorded (vehicle never moved, or collision is off)");
            return;
        }
        int[] byOutcome = new int[OUTCOMES.length];
        int penetrating = 0;
        int stalled = 0;
        int longestStall = 0;
        int stallRun = 0;
        Sweep stallAt = null;
        Sweep deepest = null;
        double stepTotal = 0;
        int multiStep = 0;
        for (Sweep sweep : sweeps) {
            byOutcome[sweep.outcome().ordinal()]++;
            if (sweep.stalled()) {
                stalled++;
                if (++stallRun > longestStall) {
                    longestStall = stallRun;
                    stallAt = sweep;
                }
            } else {
                stallRun = 0;
            }
            if (sweep.penetrating()) {
                penetrating++;
            }
            if (deepest == null || sweep.penetration() > deepest.penetration()) {
                deepest = sweep;
            }
            stepTotal += Math.sqrt(sweep.mx() * sweep.mx() + sweep.my() * sweep.my()
                    + sweep.mz() * sweep.mz());
            if (sweep.substeps() > 1) {
                multiStep++;
            }
        }
        int total = sweeps.size();
        lines.add(String.format(Locale.ROOT,
                "  sweeps: %d substeps, mean step %.3f blocks, %d%% of them subdivided",
                total, stepTotal / total, multiStep * 100 / total));
        for (SweptHull.Outcome outcome : OUTCOMES) {
            int count = byOutcome[outcome.ordinal()];
            if (count == 0) {
                continue;
            }
            String note = switch (outcome) {
                case ALREADY_INSIDE -> "  <-- backstop disabled, no tunnelling guarantee";
                case NO_BOXES -> "  (nothing near the path)";
                case NO_MOTION -> "  (substep carried no movement, nothing to cast)";
                default -> "";
            };
            lines.add(String.format(Locale.ROOT, "    %-14s %5d (%2d%%)%s",
                    outcome, count, count * 100 / total, note));
        }
        // Wedged and parked look identical elsewhere, so this gets its own line. A long run here
        // is not a slow vehicle; it is a vehicle that cannot move at all.
        if (stalled > 0) {
            lines.add(String.format(Locale.ROOT,
                    "  STALLED substeps (asked to move, allowed none): %d (%d%%), longest run %d",
                    stalled, stalled * 100 / total, longestStall));
            if (stallAt != null) {
                lines.add(String.format(Locale.ROOT,
                        "    worst at tick %d, hull (%.1f %.1f %.1f), pitch %.1f roll %.1f,"
                                + " wanted (%.3f %.3f %.3f), blocked by (%.1f %.1f %.1f)",
                        stallAt.gameTime(), stallAt.x(), stallAt.y(), stallAt.z(),
                        stallAt.xRot(), stallAt.zRot(), stallAt.mx(), stallAt.my(), stallAt.mz(),
                        stallAt.blockerX(), stallAt.blockerY(), stallAt.blockerZ()));
            }
        }
        lines.add(String.format(Locale.ROOT,
                "  penetrating substeps (>= %.2f deep): %d (%d%%)",
                PENETRATION_THRESHOLD, penetrating, penetrating * 100 / total));
        if (deepest != null && deepest.penetration() > 0) {
            lines.add(String.format(Locale.ROOT,
                    "  deepest %.3f at tick %d, hull (%.1f %.1f %.1f), block (%.1f %.1f %.1f)",
                    deepest.penetration(), deepest.gameTime(), deepest.x(), deepest.y(),
                    deepest.z(), deepest.blockerX(), deepest.blockerY(), deepest.blockerZ()));
            lines.add(String.format(Locale.ROOT,
                    "    pitch %.1f roll %.1f rotV %.3f, step (%.3f %.3f %.3f) x%d, toi %.2f, %s",
                    deepest.xRot(), deepest.zRot(), deepest.rotV(), deepest.mx(), deepest.my(),
                    deepest.mz(), deepest.substeps(), deepest.toi(), deepest.outcome()));
        }
    }

    private void appendLedger(List<String> lines, String heading, boolean position) {
        lines.add(heading);
        int listed = 0;
        for (Source source : SOURCES) {
            if (source.position != position) {
                continue;
            }
            Stat stat = stat(source);
            if (stat.ticks() == 0) {
                continue;
            }
            listed++;
            lines.add(String.format(Locale.ROOT, "    %-13s %4d ticks  total %+.4f  largest %+.4f",
                    source, stat.ticks(), stat.total(), stat.max()));
        }
        if (listed == 0) {
            // Not the same as "no data": every source was zero the whole window; for position
            // ledger, the vehicle never moved at all.
            lines.add("    (nothing moved it)");
        }
    }

    /**
     * Writes one row per tick, every column, for analysis outside the game. Per-source columns
     * make periods visible; a launch source that fires on a cadence is a feedback loop, once is a landing.
     */
    public Path dump(Path directory) throws IOException {
        Files.createDirectories(directory);
        Path file = directory.resolve(String.format(Locale.ROOT, "physics-%s-%d-%d.csv",
                vehicleName, vehicleId, samples.isEmpty() ? 0 : samples.getLast().gameTime()));
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write("tick,y,vy,dy,dvy,onGround,supported,xRot,zRot,rotV,"
                    + "contacts,bottomContacts,blockingContacts");
            for (Source source : SOURCES) {
                writer.write("," + source.name().toLowerCase(Locale.ROOT));
            }
            writer.write('\n');
            for (Sample sample : samples) {
                writer.write(String.format(Locale.ROOT,
                        "%d,%.5f,%.5f,%.5f,%.5f,%b,%b,%.2f,%.2f,%.4f,%d,%d,%d",
                        sample.gameTime(), sample.y(), sample.vy(), sample.yDelta(), sample.vyDelta(),
                        sample.onGround(), sample.supported(), sample.xRot(), sample.zRot(),
                        sample.rotV(), sample.contacts(), sample.bottomContacts(),
                        sample.blockingContacts()));
                for (Source source : SOURCES) {
                    writer.write(String.format(Locale.ROOT, ",%.5f", sample.of(source)));
                }
                writer.write('\n');
            }
        }
        return file;
    }

    /**
     * One row per movement substep: pose, step asked for, sweep allowance, and hull penetration
     * into the world. Sort by penetration descending to find frames that matter.
     */
    public Path dumpSweeps(Path directory) throws IOException {
        Files.createDirectories(directory);
        Path file = directory.resolve(String.format(Locale.ROOT, "sweeps-%s-%d-%d.csv",
                vehicleName, vehicleId, sweeps.isEmpty() ? 0 : sweeps.getLast().gameTime()));
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write("tick,substep,substeps,x,y,z,mx,my,mz,toi,outcome,boxes,"
                    + "penetration,axisX,axisY,axisZ,blockX,blockY,blockZ,xRot,yRot,zRot,rotV\n");
            for (Sweep sweep : sweeps) {
                writer.write(String.format(Locale.ROOT,
                        "%d,%d,%d,%.4f,%.4f,%.4f,%.5f,%.5f,%.5f,%.4f,%s,%d,"
                                + "%.5f,%.4f,%.4f,%.4f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.4f\n",
                        sweep.gameTime(), sweep.substep(), sweep.substeps(),
                        sweep.x(), sweep.y(), sweep.z(), sweep.mx(), sweep.my(), sweep.mz(),
                        sweep.toi(), sweep.outcome(), sweep.boxes(),
                        sweep.penetration(), sweep.axisX(), sweep.axisY(), sweep.axisZ(),
                        sweep.blockerX(), sweep.blockerY(), sweep.blockerZ(),
                        sweep.xRot(), sweep.yRot(), sweep.zRot(), sweep.rotV()));
            }
        }
        return file;
    }

    public int sweepCount() {
        return sweeps.size();
    }

    private String label() {
        return vehicleName + " #" + vehicleId;
    }

    /** The trace attached to an entity, or null if it is not a vehicle or is not being traced. */
    public static PhysicsTrace of(Entity entity) {
        return entity instanceof AbstractVehicle vehicle ? vehicle.physicsTrace() : null;
    }

}
