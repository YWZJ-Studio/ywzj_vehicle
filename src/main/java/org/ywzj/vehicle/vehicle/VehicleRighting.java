package org.ywzj.vehicle.vehicle;

import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.ywzj.vehicle.all.AllConfigs;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.structure.OBB;
import org.ywzj.vehicle.vehicle.structure.VehicleCubeOBB;

import java.util.List;

/**
 * Puts tumbled vehicles back on their wheels by hand. Server side only, using only state the
 * server already knows, so there is nothing for the client to send or spoof.
 */
public final class VehicleRighting {

    /** How far outside the hull a player may stand and still be shoving it, in blocks. */
    private static final double REACH = 1.4;
    /** How square-on the shove has to be, as the cosine of look against direction to the hull. */
    private static final double FACING = 0.35;
    /** Ticks the push has to be held before the hull goes over. */
    private static final int HOLD_TICKS = 10;
    /** Ticks between the creaks that tell the pushers it is working. */
    private static final int STRAIN_INTERVAL = 4;
    /** Attitude past which the hull counts as tumbled and may be shoved back over. */
    private static final float TUMBLED_ANGLE = 75.0f;
    /** Speed above which the hull is still going somewhere and is not there to be pushed. */
    private static final double REST_SPEED = 0.08;
    /** Angular speed above which the hull is still rolling and has not finished falling over. */
    private static final float REST_SPIN = 0.02f;

    private VehicleRighting() {
    }

    /** Advances one vehicle's righting attempt. Tick thread, server side. */
    public static void tick(AbstractVehicle vehicle) {
        // Ordered so that the overwhelmingly common answer is reached first and cheapest. A
        // server with righting switched off leaves this method having read one config field; a
        // vehicle that is not lying on its side, which is every vehicle nearly always, leaves
        // it having read two rotation fields on top of that. Nothing walks any list until a hull
        // is genuinely over and has stopped moving.
        if (AllConfigs.Cached.rightingEffortPerPlayer <= 0
                || !canBeRighted(vehicle)
                || !pushed(vehicle)) {
            vehicle.rightingHold = 0;
            return;
        }
        if (++vehicle.rightingHold < HOLD_TICKS) {
            strain(vehicle);
            return;
        }
        vehicle.rightingHold = 0;
        right(vehicle);
    }

    /** The sound of the hull starting to give while the shove is being held. */
    private static void strain(AbstractVehicle vehicle) {
        // Offset so the first creak lands on the first tick of the shove, not four into it.
        // The answer to "is this working" is worth having straight away.
        if ((vehicle.rightingHold - 1) % STRAIN_INTERVAL != 0) {
            return;
        }
        vehicle.level().playSound(null, vehicle.getX(), vehicle.getY(), vehicle.getZ(),
                vehicle.deckSoundType().getHitSound(), SoundSource.NEUTRAL, 0.5f, 0.6f);
    }

    /** Whether the hull is over far enough, still enough, and solid enough to be worth shoving. */
    private static boolean canBeRighted(AbstractVehicle vehicle) {
        if (Mth.abs(vehicle.getXRot()) < TUMBLED_ANGLE && Mth.abs(vehicle.getZRot()) < TUMBLED_ANGLE) {
            return false;
        }
        if (vehicle.isRemoved() || !vehicle.collision || vehicle.getMainCubeOBB() == null) {
            return false;
        }
        // Still on the way over, or still being driven. Neither is a wreck waiting to be turned
        // back up, and letting a shove land mid-roll would mean pushing a vehicle that is already
        // moving under its own angular momentum.
        return vehicle.getDeltaMovement().lengthSqr() <= REST_SPEED * REST_SPEED
                && vehicle.physicsEngine.angularVelocity.lengthSquared() <= REST_SPIN * REST_SPIN;
    }

    /** Whether enough people are shoving this tick. */
    private static boolean pushed(AbstractVehicle vehicle) {
        int needed = playersNeeded(vehicle);
        if (needed <= 0) {
            return false;
        }
        // The level's own player list, not an area query.
        //
        // getEntitiesOfClass walks every entity section the bound overlaps and builds a list to
        // hand back, which for a wreck that nobody is ever coming back for is a section walk and
        // an allocation every tick for as long as its chunk stays loaded. Players are already a
        // plain list on the level and there are never many of them, so iterating that with a
        // squared-distance reject is both cheaper and allocation-free; the same trade
        // Level.getNearestPlayer makes.
        List<? extends Player> players = vehicle.level().players();
        if (players.isEmpty()) {
            return false;
        }
        OBB hull = vehicle.getMainCubeOBB().obb();
        Vector3f centre = hull.center();
        double radius = hull.extents().length() + REACH;
        double radiusSq = radius * radius;
        // Built once for the whole loop, and only if somebody gets close enough to need it.
        // getClosestPointOBB re-derives these from the quaternion on every call, which is three
        // vector allocations per candidate for a frame that is identical for all of them.
        Vector3f[] axes = null;
        int pushing = 0;
        for (int i = 0, size = players.size(); i < size; i++) {
            Player player = players.get(i);
            // Sneak leads: one shared-flag read, and false for essentially everyone online.
            if (!player.isShiftKeyDown() || player.isSpectator() || player.isPassenger()
                    || player.isRemoved()) {
                continue;
            }
            double eyeX = player.getX();
            double eyeY = player.getEyeY();
            double eyeZ = player.getZ();
            double offX = eyeX - centre.x;
            double offY = eyeY - centre.y;
            double offZ = eyeZ - centre.z;
            // Bounding sphere before the box. Anyone across the room is rejected by three
            // multiplies rather than by a clamped projection onto each hull axis.
            if (offX * offX + offY * offY + offZ * offZ > radiusSq) {
                continue;
            }
            if (axes == null) {
                axes = hull.getAxes();
            }
            if (!shoving(player, eyeX, eyeY, eyeZ, hull, axes)) {
                continue;
            }
            if (player.isCreative()) {
                return true;
            }
            if (++pushing >= needed) {
                return true;
            }
        }
        return false;
    }

    /** Whether this player is leaning on the hull right now. */
    private static boolean shoving(Player player, double eyeX, double eyeY, double eyeZ,
                                   OBB hull, Vector3f[] axes) {
        Vector3f centre = hull.center();
        Vector3f extents = hull.extents();
        double offX = eyeX - centre.x;
        double offY = eyeY - centre.y;
        double offZ = eyeZ - centre.z;
        // Nearest point on the box to the eye: project onto each hull axis, clamp to the extent,
        // walk back out. Same result as OBB.getClosestPointOBB with the axes handed in.
        double nearX = centre.x;
        double nearY = centre.y;
        double nearZ = centre.z;
        for (int a = 0; a < 3; a++) {
            Vector3f axis = axes[a];
            double limit = a == 0 ? extents.x : a == 1 ? extents.y : extents.z;
            double along = Mth.clamp(offX * axis.x + offY * axis.y + offZ * axis.z, -limit, limit);
            nearX += along * axis.x;
            nearY += along * axis.y;
            nearZ += along * axis.z;
        }
        double dx = nearX - eyeX;
        double dy = nearY - eyeY;
        double dz = nearZ - eyeZ;
        double distanceSq = dx * dx + dy * dy + dz * dz;
        if (distanceSq > REACH * REACH) {
            return false;
        }
        if (distanceSq < 1.0e-6) {
            // Standing inside the hull; an upturned vehicle is a shape you can end up under.
            // There is no direction to face, so being there at all counts.
            return true;
        }
        Vec3 look = player.getLookAngle();
        return (look.x * dx + look.y * dy + look.z * dz)
                / java.lang.Math.sqrt(distanceSq) >= FACING;
    }

    /** How many survival players it takes to right this vehicle, or 0 if hand righting is switched off. */
    private static int playersNeeded(AbstractVehicle vehicle) {
        double perPlayer = AllConfigs.Cached.rightingEffortPerPlayer;
        if (perPlayer <= 0) {
            return 0;
        }
        VehicleCubeOBB cube = vehicle.getMainCubeOBB();
        double volume = cube.getWidth() * cube.getHeight() * cube.getDepth();
        double effort = java.lang.Math.max(0, vehicle.physicsEngine.mass) * volume;
        // At least one: a hull small or light enough to fall below the per-player budget still
        // takes somebody to push it.
        return (int) java.lang.Math.max(1, java.lang.Math.ceil(effort / perPlayer));
    }

    /** Puts the hull back on its wheels. */
    private static void right(AbstractVehicle vehicle) {
        vehicle.setXRot(0);
        vehicle.setZRot(0);
        vehicle.physicsEngine.angularVelocity.zero();
        vehicle.physicsEngine.rotV = 0;
        vehicle.triggerPosRotUpdate();
        // The hull's own material, so a vehicle that has told us it is not made of steel does not
        // land like it is.
        vehicle.level().playSound(null, vehicle.getX(), vehicle.getY(), vehicle.getZ(),
                vehicle.deckSoundType().getPlaceSound(), SoundSource.NEUTRAL, 1.0f, 0.7f);
    }

}
