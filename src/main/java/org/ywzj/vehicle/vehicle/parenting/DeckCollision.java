package org.ywzj.vehicle.vehicle.parenting;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;

/**
 * Entity-vs-hull collision, run in the vehicle's own reference frame.
 */
public final class DeckCollision {

    /**
     * How far the depenetration net may push in one call, and so how much slack its box gather
     * needs. A correction deeper than this means an entity is buried well inside a hull, where one
     * more pass is not what is going to help.
     */
    private static final double MAX_CORRECTION = 4.0;

    private DeckCollision() {}

    /**
     * Clips movement against the hull in world space. Movement is already
     * settled by vanilla block collision.
     * @param attachment marked supported when downward movement was stopped.
     */
    public static Vec3 clip(Entity entity, DeckSnapshot deck, Vec3 worldMove,
                            DeckAttachment attachment) {
        float[] boxes = deck.boxes();
        int count = deck.count();
        if (count == 0) {
            return worldMove;
        }
        // Read-only shares, not copies. This runs for every rider on every move, and building two
        // quaternions to conjugate one of them was the largest single allocator in the system.
        Quaternionf rotation = deck.rotation();
        Quaternionf inverse = deck.inverse();

        AABB bounds = entity.getBoundingBox();
        Vector3f centre = VehicleParenting.toLocal(deck,
                (bounds.minX + bounds.maxX) * 0.5,
                (bounds.minY + bounds.maxY) * 0.5,
                (bounds.minZ + bounds.maxZ) * 0.5,
                attachment.scratchCentre);
        Vector3f move = inverse.transform(attachment.scratchMove.set(
                (float) worldMove.x, (float) worldMove.y, (float) worldMove.z));
        // The same movement with its world-vertical part removed: the entity's own horizontal
        // intent, expressed in the frame. See DeckClip.sweep for what a grounded rider does with it.
        Vector3f grounded = inverse.transform(attachment.scratchGrounded.set(
                (float) worldMove.x, 0, (float) worldMove.z));

        double hx = (bounds.maxX - bounds.minX) * 0.5;
        double hy = (bounds.maxY - bounds.minY) * 0.5;
        double hz = (bounds.maxZ - bounds.minZ) * 0.5;
        // Everything the sweep could touch, gathered once. The seven axis passes below then cost
        // what the contact is rather than what the hull is.
        float[] near = attachment.nearBuffer(count);
        int nearCount = DeckClip.narrow(boxes, count, centre.x, centre.y, centre.z, hx, hy, hz,
                move.x, move.y, move.z, grounded.x, grounded.z, entity.maxUpStep(), near);
        if (nearCount == 0) {
            return worldMove;
        }

        double[] allowed = attachment.scratchOut;
        boolean blockedDown = DeckClip.sweep(near, nearCount,
                centre.x, centre.y, centre.z, hx, hy, hz,
                move.x, move.y, move.z,
                grounded.x, grounded.z,
                entity.maxUpStep(), entity.onGround(),
                allowed);

        if (blockedDown) {
            attachment.markSupported();
            // Deck-relative, which is the only reading that means anything: a rider walking on the
            // spot while a carrier runs at two blocks a tick has not taken a step, and one walking
            // aft against the ship's motion has. Decided here, where the frame is; emitted from the
            // tick thread by DeckEffects.
            DeckEffects.walked(entity, attachment, allowed[0], allowed[2]);
        }
        if (allowed[0] == move.x && allowed[1] == move.y && allowed[2] == move.z) {
            return worldMove;
        }
        // Returned as a correction to the movement rather than a re-transform of it, so an axis
        // nothing touched comes back bit-identical. Entity.move decides verticalCollision with
        // an exact comparison "wanted.y != collided.y" and zeroes fall velocity when true, so
        // any Y perturbation from rotation would break that. Round-trip through float rotation
        // perturbs Y by about 1e-8 whenever anything is clipped, which would stop falling.
        // On a hull with no pitch or roll, vertical correction rotates to pure vertical, so
        // horizontal components survive untouched.
        Vector3d correction = rotation.transform(attachment.scratchWorld.set(
                allowed[0] - move.x, allowed[1] - move.y, allowed[2] - move.z));
        return new Vec3(worldMove.x + correction.x,
                worldMove.y + correction.y,
                worldMove.z + correction.z);
    }

    /**
     * Probes whether hull geometry is close enough beneath to hold this entity.
     */
    public static boolean supportedBelow(Entity entity, DeckSnapshot deck,
                                        DeckAttachment attachment) {
        float[] boxes = deck.boxes();
        int count = deck.count();
        if (count == 0) {
            return false;
        }
        AABB bounds = entity.getBoundingBox();
        Vector3f centre = VehicleParenting.toLocal(deck,
                (bounds.minX + bounds.maxX) * 0.5,
                (bounds.minY + bounds.maxY) * 0.5,
                (bounds.minZ + bounds.maxZ) * 0.5,
                attachment.scratchCentre);
        double reach = -(DeckClip.SKIN + 0.02);
        return DeckClip.clipY(boxes, count, centre.x, centre.y, centre.z,
                (bounds.maxX - bounds.minX) * 0.5,
                (bounds.maxY - bounds.minY) * 0.5,
                (bounds.maxZ - bounds.minZ) * 0.5,
                reach) > reach;
    }

    /**
     * Probes whether the deck supports an offset box, answering the question
     * Player.canFallAtLeast asks of the world.
     */
    public static boolean supportsOffset(Entity entity, DeckSnapshot deck,
                                         DeckAttachment attachment,
                                         double dx, double dz, double depth) {
        float[] boxes = deck.boxes();
        int count = deck.count();
        if (count == 0) {
            return false;
        }
        AABB bounds = entity.getBoundingBox();
        // The thin slab vanilla probes with: the footprint, offset, spanning depth below the feet.
        Vector3f centre = VehicleParenting.toLocal(deck,
                (bounds.minX + bounds.maxX) * 0.5 + dx,
                bounds.minY - depth * 0.5,
                (bounds.minZ + bounds.maxZ) * 0.5 + dz,
                attachment.scratchCentre);
        return DeckClip.anyBoxNear(boxes, count, centre.x, centre.y, centre.z,
                (bounds.maxX - bounds.minX) * 0.5,
                depth * 0.5,
                (bounds.maxZ - bounds.minZ) * 0.5,
                0, 0, 0);
    }

    /**
     * Pushes an entity out of hull geometry when it arrives on top. Safety net,
     * not the main clip mechanism.
     * @return true when correction was applied.
     */
    public static boolean depenetrate(Entity entity, DeckSnapshot deck,
                                      DeckAttachment attachment, int maxPasses) {
        float[] boxes = deck.boxes();
        int count = deck.count();
        if (count == 0) {
            return false;
        }
        Quaternionf rotation = deck.rotation();

        AABB bounds = entity.getBoundingBox();
        Vector3f centre = VehicleParenting.toLocal(deck,
                (bounds.minX + bounds.maxX) * 0.5,
                (bounds.minY + bounds.maxY) * 0.5,
                (bounds.minZ + bounds.maxZ) * 0.5,
                attachment.scratchCentre);

        double hx = (bounds.maxX - bounds.minX) * 0.5;
        double hy = (bounds.maxY - bounds.minY) * 0.5;
        double hz = (bounds.maxZ - bounds.minZ) * 0.5;
        // Grown by the deepest correction a pass can make, so a box the entity is pushed towards is
        // still in the set when the next pass looks for it.
        float[] near = attachment.nearBuffer(count);
        int nearCount = DeckClip.narrow(boxes, count, centre.x, centre.y, centre.z,
                hx + MAX_CORRECTION, hy + MAX_CORRECTION, hz + MAX_CORRECTION,
                0, 0, 0, 0, 0, 0, near);
        if (nearCount == 0) {
            return false;
        }

        double[] correction = attachment.scratchOut;
        if (!DeckClip.depenetrate(near, nearCount,
                centre.x, centre.y, centre.z, hx, hy, hz,
                maxPasses, correction)) {
            return false;
        }
        rotation.transform(attachment.scratchWorld.set(correction[0], correction[1], correction[2]));
        return true;
    }

}
