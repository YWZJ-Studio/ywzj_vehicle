package org.ywzj.vehicle.vehicle.parenting;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.ywzj.vehicle.all.AllConfigs;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

import java.util.List;

/**
 * A vehicle parked on a carrier, with its physics switched off and its pose derived from the
 * carrier's instead
 */
public final class VehicleHarness {

    // How long a vehicle must be still on a deck before sleeping.
    private static final int DWELL_TICKS = 20;

    // Speed below which a vehicle counts as parked, in blocks and radians per tick.
    private static final double LINEAR_SLEEP = 0.02;
    private static final double ANGULAR_SLEEP = 0.002;

    // How far the deck under a sleeping vehicle may move before it is woken.
    private static final double SUPPORT_TOLERANCE = 0.1;

    // Ceiling on the velocity a waking vehicle inherits, matching the rider handoff.
    private static final double MAX_HANDOFF_SPEED = 4.0;

    // Displacement in one tick beyond which the carrier is read as having jumped rather than moved.
    private static final double MAX_CARRY_PER_TICK = 16.0;

    private final AbstractVehicle carrier;

    // The vehicle's own origin and orientation, in the carrier's frame. The authoritative pose.
    private final Vector3d localPos = new Vector3d();
    private final Quaternionf localRotation = new Quaternionf();
    private final Vector3d scratch = new Vector3d();
    private final Quaternionf scratchRotation = new Quaternionf();
    private final Matrix3f scratchBasis = new Matrix3f();
    private final float[] scratchEuler = new float[3];

    // Height of the deck surface this vehicle settled on, in the carrier's frame.
    private double localSupportY;
    // World displacement the last apply made, handed over as velocity when the harness lets go.
    private double lastCarryX, lastCarryY, lastCarryZ;
    // Set on the client when the pose the server reported has changed and local must be re-derived.
    private boolean localDirty = true;
    private VehicleHarness(AbstractVehicle carrier) {
        this.carrier = carrier;
    }

    //state
    // The carrier a vehicle is asleep on, or null.
    @Nullable
    public static AbstractVehicle carrierOf(AbstractVehicle vehicle) {
        VehicleHarness harness = vehicle.harness();
        return harness == null ? null : harness.carrier;
    }

    // Whether this vehicle's pose comes from a carrier rather than from its own physics.
    public static boolean isHarnessed(AbstractVehicle vehicle) {
        return vehicle.harness() != null;
    }

    public static void tick(AbstractVehicle vehicle) {
        if (vehicle.level().isClientSide()) {
            return;
        }
        VehicleHarness harness = vehicle.harness();
        if (harness != null) {
            if (harness.shouldWake(vehicle)) {
                detach(vehicle, true);
            }
            return;
        }
        if (!AllConfigs.Cached.deckHarness) {
            return;
        }
        if (!settled(vehicle)) {
            vehicle.setHarnessDwell(0);
            return;
        }
        if (vehicle.harnessDwell() + 1 < DWELL_TICKS) {
            vehicle.setHarnessDwell(vehicle.harnessDwell() + 1);
            return;
        }
        if (!attach(vehicle, vehicle.carrierLink().carrier())) {
            vehicle.setHarnessDwell(0);
        }
    }

    //tick

    // Whether a vehicle is a candidate for sleeping this tick.
    private static boolean settled(AbstractVehicle vehicle) {
        CarrierLink link = vehicle.carrierLink();
        AbstractVehicle carrier = link.carrier();
        if (carrier == null || !link.supported() || carrier.isRemoved()) {
            return false;
        }
        if (vehicle.isRemoved() || vehicle.noPhysics || !vehicle.collision) {
            return false;
        }
        if (vehicle.isEngineOn() || vehicle.getPower() > 0
                || vehicle.getControllingPassenger() != null) {
            return false;
        }
        Vec3 velocity = vehicle.getDeltaMovement();
        return velocity.lengthSqr() <= LINEAR_SLEEP * LINEAR_SLEEP
                && Math.abs(vehicle.physicsEngine.rotV) <= ANGULAR_SLEEP;
    }


    public static void applyChildren(AbstractVehicle carrier) {
        List<AbstractVehicle> children = carrier.harnessedVehicles();
        if (children.isEmpty()) {
            return;
        }
        DeckSnapshot deck = carrier.deckSnapshot();
        if (deck.count() == 0) {
            return;
        }
        for (int i = children.size() - 1; i >= 0; i--) {
            AbstractVehicle child = children.get(i);
            VehicleHarness harness = child.harness();
            if (harness == null || harness.carrier != carrier || child.isRemoved()
                    || child.level() != carrier.level()) {
                children.remove(i);
                if (harness != null && harness.carrier == carrier) {
                    detach(child, false);
                }
                continue;
            }
            harness.apply(child, deck);
        }
    }

    // Puts a vehicle to sleep on a carrier. Returns whether it took.
    private static boolean attach(AbstractVehicle vehicle, @Nullable AbstractVehicle carrier) {
        if (carrier == null || carrier == vehicle || carrier.isRemoved()) {
            return false;
        }
        if (isHarnessed(carrier)) {
            return false;
        }
        DeckSnapshot deck = carrier.deckSnapshot();
        if (deck.deckCount() == 0) {
            return false;
        }
        VehicleHarness harness = new VehicleHarness(carrier);
        harness.capture(vehicle, deck);
        if (Double.isNaN(harness.localSupportY) || !harness.supportedAt(deck)) {
            return false;
        }
        vehicle.setHarness(harness);
        vehicle.setHarnessDwell(0);
        vehicle.setDeltaMovement(Vec3.ZERO);
        vehicle.setOnGround(true);
        vehicle.carrierLink().clear();
        carrier.harnessedVehicles().add(vehicle);
        vehicle.setHarnessCarrierId(carrier.getId());
        return true;
    }

     // Wakes a vehicle, handing it the platform velocity it was being carried at.
    public static void detach(AbstractVehicle vehicle, boolean handoff) {
        VehicleHarness harness = vehicle.harness();
        if (harness == null) {
            return;
        }
        harness.carrier.harnessedVehicles().remove(vehicle);
        vehicle.setHarness(null);
        vehicle.setHarnessDwell(0);
        // Synced data is server only. The client reaches this through level removal,
        // where the value is meaningless.
        if (!vehicle.level().isClientSide()) {
            vehicle.setHarnessCarrierId(-1);
        }
        vehicle.carrierLink().clear();
        if (!handoff) {
            return;
        }
        double speed = Math.sqrt(harness.lastCarryX * harness.lastCarryX
                + harness.lastCarryY * harness.lastCarryY
                + harness.lastCarryZ * harness.lastCarryZ);
        if (speed > 1.0E-4) {
            double scale = speed > MAX_HANDOFF_SPEED ? MAX_HANDOFF_SPEED / speed : 1.0;
            vehicle.setDeltaMovement(vehicle.getDeltaMovement().add(
                    harness.lastCarryX * scale, harness.lastCarryY * scale,
                    harness.lastCarryZ * scale));
        }
    }

    // Wakes all vehicles sleeping on this carrier.
    public static void releaseAll(AbstractVehicle carrier, boolean handoff) {
        List<AbstractVehicle> children = carrier.harnessedVehicles();
        if (children.isEmpty()) {
            return;
        }
        for (int i = children.size() - 1; i >= 0; i--) {
            detach(children.get(i), handoff);
        }
        children.clear();
    }

    public static void wake(AbstractVehicle vehicle) {
        if (vehicle.harness() != null) {
            detach(vehicle, true);
        }
    }

    //attach and detach

    // How far from a vehicle's own origin its deck can be, in blocks.
    private static double hullReach(AbstractVehicle vehicle) {
        return vehicle.getMainCubeOBB() == null
                ? 4.0 : vehicle.getMainCubeOBB().obb().extents().y * 2.0 + 1.0;
    }

    // Places a sleeping vehicle on the client from its carrier's pose, and reports whether it did.
    public static boolean clientFollow(AbstractVehicle vehicle) {
        int carrierId = vehicle.harnessCarrierId();
        VehicleHarness harness = vehicle.harness();
        if (carrierId < 0) {
            if (harness != null) {
                harness.carrier.harnessedVehicles().remove(vehicle);
                vehicle.setHarness(null);
            }
            return false;
        }
        if (!(vehicle.level().getEntity(carrierId) instanceof AbstractVehicle carrier)
                || carrier.deckSnapshot().count() == 0) {
            return false;
        }
        if (harness == null || harness.carrier != carrier) {
            if (harness != null) {
                harness.carrier.harnessedVehicles().remove(vehicle);
            }
            harness = new VehicleHarness(carrier);
            vehicle.setHarness(harness);
            carrier.harnessedVehicles().add(vehicle);
        }
        DeckSnapshot deck = carrier.deckSnapshot();
        if (harness.localDirty) {
            harness.capture(vehicle, deck);
            return true;
        }
        harness.apply(vehicle, deck);
        return true;
    }

    public AbstractVehicle carrier() {
        return carrier;
    }

    private boolean shouldWake(AbstractVehicle vehicle) {
        if (!AllConfigs.Cached.deckHarness || vehicle.isRemoved() || vehicle.noPhysics
                || !vehicle.collision) {
            return true;
        }
        if (carrier.isRemoved() || carrier.level() != vehicle.level() || !carrier.collision) {
            return true;
        }
        if (vehicle.isEngineOn() || vehicle.getPower() > 0
                || vehicle.getControllingPassenger() != null) {
            return true;
        }
        DeckSnapshot deck = carrier.deckSnapshot();
        if (deck.deckCount() == 0) {
            return true;
        }
        return !supportedAt(deck);
    }

    // Whether the deck is still where this vehicle went to sleep on it.
    private boolean supportedAt(DeckSnapshot deck) {
        return DeckFrame.supportsAt(deck.deckBoxes(), deck.deckCount(),
                localPos.x, localPos.z, localSupportY, SUPPORT_TOLERANCE);
    }

    private void apply(AbstractVehicle child, DeckSnapshot deck) {
        if (localDirty) {
            return;
        }
        Vector3d target = DeckFrame.toWorld(deck.rotation(),
                deck.pivotX(), deck.pivotY(), deck.pivotZ(),
                localPos.x, localPos.y, localPos.z, scratch);
        double dx = target.x - child.getX();
        double dy = target.y - child.getY();
        double dz = target.z - child.getZ();
        if (dx * dx + dy * dy + dz * dz > MAX_CARRY_PER_TICK * MAX_CARRY_PER_TICK) {
            capture(child, deck);
            setLastCarry(0, 0, 0);
            return;
        }

        DeckFrame.toWorldRotation(deck.rotation(), localRotation, scratchRotation);
        DeckFrame.toEulerYXZ(scratchRotation, scratchBasis, scratchEuler);
        boolean moved = dx != 0 || dy != 0 || dz != 0;
        boolean turned = scratchEuler[0] != child.getYRot() || scratchEuler[1] != child.getXRot()
                || scratchEuler[2] != child.getZRot();
        setLastCarry(dx, dy, dz);
        if (!moved && !turned) {
            return;
        }
        if (moved) {
            child.setPos(target.x, target.y, target.z);
        }
        if (turned) {
            child.setYRot(scratchEuler[0]);
            child.setXRot(scratchEuler[1]);
            child.setZRot(scratchEuler[2]);
        }
        child.updateOBBs();
        VehicleParenting.reapply(child);
    }

    // Re-derives the stored local pose from where the vehicle currently is.
    private void capture(AbstractVehicle vehicle, DeckSnapshot deck) {
        DeckFrame.toLocal(deck.inverse(), deck.pivotX(), deck.pivotY(), deck.pivotZ(),
                vehicle.getX(), vehicle.getY(), vehicle.getZ(), localPos);
        DeckFrame.fromEulerYXZ(vehicle.getYRot(), vehicle.getXRot(), vehicle.getZRot(),
                scratchRotation);
        DeckFrame.toLocalRotation(deck.inverse(), scratchRotation, localRotation);
        localSupportY = DeckFrame.supportUnder(deck.deckBoxes(), deck.deckCount(),
                localPos.x, localPos.y, localPos.z, hullReach(vehicle));
        localDirty = false;
    }

    //client stuff

    private void setLastCarry(double x, double y, double z) {
        lastCarryX = x;
        lastCarryY = y;
        lastCarryZ = z;
    }

}
