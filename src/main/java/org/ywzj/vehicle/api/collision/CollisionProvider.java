package org.ywzj.vehicle.api.collision;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.structure.VehicleCubeOBB;

import java.util.List;

/**
 * A source of collision contacts for vehicle physics — world blocks, sub-levels, contraptions,
 * or anything else an addon wants a vehicle to bump into.
 * <p>
 * This exists because the older {@link org.ywzj.vehicle.api.event.VehicleCollectCollisionEvent}
 * hands each listener a mutable list and nothing else, so every listener had to walk the hull's
 * sample points itself and transform each one into world space. With two listeners installed the
 * hull was sampled three times per tick over the same points.
 * <p>
 * A provider is driven through one shared pass instead: {@link #begin} once per vehicle, then
 * {@link Session#contactAt} per candidate point with the world position already computed, then
 * {@link Session#end}. The event still fires afterwards with the accumulated contacts, so
 * existing listeners keep working unchanged.
 * <p>
 * Which points are candidates depends on {@link Session#collectBoxes}. Describe your geometry as
 * boxes and only the area genuinely in contact is tested; decline and the vehicle's whole surface
 * grid is walked, which is correct but costs the same whether you contribute anything or not.
 * <p>
 * A provider is a singleton and may be consulted for several vehicles at once, so it must hold
 * no per-vehicle state. Anything that varies per vehicle belongs in the {@link Session}.
 */
public interface CollisionProvider {

    /**
     * What a provider found at a sample point.
     *
     * @param blockPos the contacted block's reference position, by convention
     *                 {@code Vec3.atBottomCenterOf} of the block — physics reads {@code .y} as
     *                 the block's floor when deciding step height
     * @param state    the contacted state, or {@code null} if the provider has no block to
     *                 report. A {@code null} state disables the half-block handling downstream.
     */
    record Contact(Vec3 blockPos, @Nullable BlockState state) {}

    /**
     * Opens a pass over one vehicle's sample points.
     *
     * @param hullBounds the vehicle's bound, already widened by the sampling margin
     * @return a session, or {@code null} to skip this provider for this vehicle this tick.
     *         Returning {@code null} on a cheap bounds test is the main way a provider avoids
     *         costing anything when it cannot contribute.
     */
    @Nullable
    Session begin(AbstractVehicle vehicle, AABB hullBounds);

    /**
     * One provider's work for one vehicle for one tick. Never shared between vehicles, so it is
     * free to hold mutable state.
     */
    interface Session {

        /**
         * Describes this session's geometry as world-space boxes, so the vehicle can generate
         * contacts where they actually touch rather than walking its whole surface asking.
         * <p>
         * Implement this and the hull grid is never built for you: the core clips each box
         * against the hull, generates points only over the overlap, and calls
         * {@link #contactAt} on those. A hull with 3000 sample points and one contraption under
         * one corner drops from 3000 tests to a few dozen. Decline it — the default — and the
         * grid is walked as before, which still works but costs the same regardless of how
         * little of the hull is in contact.
         * <p>
         * Boxes may be <b>looser</b> than the real geometry. A rotated sub-level has no exact
         * axis-aligned form, so bounding its blocks is expected and correct; every point
         * generated inside a box is still put to {@link #contactAt}, which is what decides.
         * Boxes must never be <b>tighter</b>, because geometry outside every box is geometry the
         * vehicle will pass through.
         *
         * @param bounds the vehicle's bound, already widened by the sampling margin
         * @param out    append to this; it is not cleared for you and may be reused after the call
         * @return true if {@code out} now describes everything this session could collide with
         */
        default boolean collectBoxes(AABB bounds, List<AABB> out) {
            return false;
        }

        /**
         * Tests one candidate point.
         *
         * @param worldPos the point's world position. Owned by the caller and reused across
         *                 providers — read it, do not retain or mutate it.
         * @return the contact, or {@code null} if this provider has nothing at that point
         */
        @Nullable
        Contact contactAt(VehicleCubeOBB.CubePoint point, Vector3f worldPos);

        /**
         * Called once after every point has been tested.
         *
         * @param contacts every contact collected this tick, from all providers, in point order
         */
        default void end(List<VehicleCubeOBB.CubePoint> contacts) {}

    }

}
