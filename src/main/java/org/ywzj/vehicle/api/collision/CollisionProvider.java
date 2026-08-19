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
 * A source of collision contacts for vehicle physics; world blocks, sub-levels, contraptions, or
 * anything else an addon wants a vehicle to bump into. Providers are called with pre-computed
 * world positions for sample points, so a single shared walk replaces per-listener sampling.
 * Providers must hold no per-vehicle state; Session holds mutable data per vehicle per tick.
 */
public interface CollisionProvider {

    /**
     * What a provider found at a sample point.
     *
     * @param blockPos the contacted block's reference position, conventionally the bottom center;
     *                 physics reads the y-coordinate as the block's floor for step height
     * @param state    the contacted state, or null if the provider has no block; null disables
     *                 downstream half-block handling
     */
    record Contact(Vec3 blockPos, @Nullable BlockState state) {}

    /**
     * Opens a pass over one vehicle's sample points.
     *
     * @param hullBounds the vehicle's bound, already widened by the sampling margin
     * @return a session, or null to skip this provider for this vehicle this tick; returning null
     *         on a cheap bounds test avoids costs when the provider cannot contribute
     */
    @Nullable
    Session begin(AbstractVehicle vehicle, AABB hullBounds);

    /** One provider's work for one vehicle for one tick; free to hold mutable state. */
    interface Session {

        /**
         * Describes this session's geometry as world-space boxes to generate contacts only
         * where they touch; decline and the whole hull surface is tested. Boxes may be looser
         * than the real geometry, but never tighter. Every point inside a box is tested via
         * contactAt.
         *
         * @param bounds the vehicle's bound, already widened by the sampling margin
         * @param out    append to this; cleared by the caller before use and may be reused
         * @return true if out now describes all the geometry this session could collide with
         */
        default boolean collectBoxes(AABB bounds, List<AABB> out) {
            return false;
        }

        /**
         * Tests one candidate point.
         *
         * @param worldPos the point's world position, owned by the caller and reused across
         *                 providers; read but never retain or mutate
         * @return the contact, or null if this provider has nothing at that point
         */
        @Nullable
        Contact contactAt(VehicleCubeOBB.CubePoint point, Vector3f worldPos);

        /**
         * Called once after every point has been tested.
         *
         * @param contacts all contacts collected this tick from all providers, in point order
         */
        default void end(List<VehicleCubeOBB.CubePoint> contacts) {}

    }

}
