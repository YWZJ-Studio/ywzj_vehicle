package org.ywzj.vehicle.vehicle.solver;

import org.joml.Vector3f;

import java.util.List;

/**
 * Spring-damper running gear: the part of a vehicle that actually meets the ground.
 * <p>
 * The hull is a rigid box and a rigid box cannot drive up a one-block step — it hits the riser and
 * stops, which is correct for a box and useless for a vehicle. Real vehicles climb because their
 * running gear is below the hull and can move relative to it. Modelling that is what makes step
 * climbing physical instead of the teleport {@code climb()} used to do, and it is also where ride
 * height, weight transfer under braking, and leaning into a turn come from for free.
 * <p>
 * <b>The hull must be inset above the wheels.</b> A vehicle whose hull box reaches the ground has
 * no room for suspension to work in — the hull collides with the step before a wheel can rise over
 * it. The bottom {@code restLength + radius} of the vehicle belongs to the running gear and must
 * be excluded from the hull the contact solver sees. {@code LandingGearUnit} already does exactly
 * this for aircraft, trimming the main cube by the gear height; this is the same idea generalised.
 * <p>
 * <b>Why a sphere and not a ray.</b> A downward ray cannot see the vertical face of a step, so a
 * ray-wheel meeting a riser finds nothing until it is already past it and the vehicle jerks. A
 * sphere descending onto a step catches the top <em>edge</em> first and gets a normal pointing up
 * and back, which lifts and slows it in the right proportion — the vehicle rides up rather than
 * snapping up. Against the axis-aligned boxes this project already merges, that costs a clamp and
 * a square root.
 */
public final class WheelSuspension {

    /** Rest length is measured from the anchor down; this is the least it may compress to. */
    private static final float MIN_LENGTH = 0.01f;

    /** Suspension force is capped in units of the load it is designed to carry. */
    private static final float MAX_FORCE_FACTOR = 4.0f;

    public static final class Wheel {

        /** Attachment point on the hull, body-local. Suspension extends along body -Y from here. */
        public final Vector3f anchor = new Vector3f();
        /** Travel available below the anchor. */
        public float restLength = 0.5f;
        public float radius = 0.5f;
        /**
         * How many times the wheel's share of the vehicle's weight the spring pushes back with,
         * per block of compression. The vehicle therefore rests at {@code 1 / stiffness} blocks of
         * compression whatever it weighs, so the same number gives the same ride height on a jeep
         * and on a carrier. Pick it as {@code 1 / (desired resting compression)}.
         */
        public float stiffness = 3.0f;
        /**
         * Fraction of critical damping. 1 settles without overshoot, below that is bouncy, above
         * is sluggish. Expressed as a ratio because the absolute figure depends on stiffness and
         * gravity, and getting that by hand is how a suspension ends up oscillating.
         */
        public float damping = 0.9f;
        public boolean powered = true;
        public boolean steered;

        // Per-tick results, for rendering and for whoever wants to know.
        public boolean grounded;
        public float compression;
        public float load;
        public final Vector3f contactPoint = new Vector3f();
        public final Vector3f contactNormal = new Vector3f();

    }

    /** Whatever the wheels are rolling on. Implemented over the merged block boxes. */
    @FunctionalInterface
    public interface Ground {

        /**
         * Highest surface a sphere descending vertically at {@code (x, z)} would come to rest on.
         *
         * @param normalOut receives the surface normal at that contact
         * @return the height of the sphere's <em>centre</em> at rest, or
         *         {@link Float#NEGATIVE_INFINITY} if there is nothing under it
         */
        float support(float x, float z, float radius, float minY, float maxY, Vector3f normalOut);

    }

    private final Vector3f anchorWorld = new Vector3f();
    private final Vector3f rel = new Vector3f();
    private final Vector3f up = new Vector3f();
    private final Vector3f forward = new Vector3f();
    private final Vector3f right = new Vector3f();
    private final Vector3f normal = new Vector3f();
    private final Vector3f pointVelocity = new Vector3f();
    private final Vector3f scratch = new Vector3f();

    /**
     * Runs every wheel and applies its forces to the body. Call before
     * {@link ContactSolver#solve}, so the hull solver has the last word on anything the suspension
     * could not keep out of the ground.
     *
     * @param drive      forward force per powered wheel, already scaled by throttle
     * @param brake      0 to 1, scales the longitudinal grip used to stop rather than drive
     * @param gripLong   longitudinal friction coefficient — traction and braking
     * @param gripLat    lateral friction coefficient — how hard it resists sliding sideways
     * @return how many wheels found ground
     */
    public int apply(ContactSolver.Body body, List<Wheel> wheels, Ground ground, float gravity,
                     float drive, float brake, float gripLong, float gripLat) {
        body.refreshInertia();
        up.set(0, 1, 0);
        body.rotation.transform(up);
        forward.set(0, 0, 1);
        body.rotation.transform(forward);
        right.set(1, 0, 0);
        body.rotation.transform(right);

        // Nominal load per wheel, so stiffness is a ride-height figure rather than a mass-dependent
        // one and the same numbers work for a jeep and a carrier.
        float mass = body.invMass > 0 ? 1.0f / body.invMass : 0;
        float nominal = wheels.isEmpty() ? 0 : mass / wheels.size();
        // The per-tick impulse one wheel needs just to hold its share of the vehicle up. Every
        // force below is a multiple of this, which is what keeps the tuning mass-independent.
        float nominalLoad = nominal * gravity;
        int grounded = 0;

        for (int i = 0, size = wheels.size(); i < size; i++) {
            Wheel wheel = wheels.get(i);
            anchorWorld.set(wheel.anchor);
            body.rotation.transform(anchorWorld);
            anchorWorld.add(body.centre);

            // support() reports where the wheel's CENTRE would come to rest, and the centre hangs
            // at most restLength below the anchor — the radius is already inside that figure and
            // adding it again gave the spring a phantom block of travel.
            //
            // The search reaches ABOVE the anchor by a radius on purpose. A vehicle that has been
            // shoved down hard enough puts its wheel centre above its own anchor, and rejecting
            // that as out of range made the suspension disappear exactly when it was needed most:
            // the hull dropped onto its belly and, with no wheel force left to lift it, stayed
            // there. Bottomed out has to mean maximum force, not no force.
            float centreY = ground.support(anchorWorld.x, anchorWorld.z, wheel.radius,
                    anchorWorld.y - wheel.restLength, anchorWorld.y + wheel.radius, normal);
            if (centreY == Float.NEGATIVE_INFINITY) {
                wheel.grounded = false;
                wheel.compression = 0;
                wheel.load = 0;
                continue;
            }

            // Negative length means the wheel is jammed up past its anchor; compression clamps.
            float length = anchorWorld.y - centreY;
            if (length >= wheel.restLength) {
                wheel.grounded = false;
                wheel.compression = 0;
                wheel.load = 0;
                continue;
            }
            grounded++;
            wheel.grounded = true;
            wheel.compression = Math.clamp(wheel.restLength - Math.max(length, MIN_LENGTH - wheel.restLength), 0, wheel.restLength);
            wheel.contactPoint.set(anchorWorld.x, centreY - wheel.radius, anchorWorld.z);
            wheel.contactNormal.set(normal);

            rel.set(wheel.contactPoint).sub(body.centre);
            body.pointVelocity(rel, pointVelocity);

            // Spring along the body's own up axis rather than the surface normal: a spring that
            // follows the normal shoves the vehicle sideways on a slope, which reads as the hull
            // sliding downhill on its own.
            float compressionRate = -pointVelocity.dot(up);
            // Critical damping for this spring is 2ω per unit mass, with ω = sqrt(k/m) and k
            // expressed here as stiffness · gravity. Deriving it rather than exposing it stops the
            // two coefficients drifting out of step, which is exactly how a suspension oscillates.
            float omega = (float) Math.sqrt(gravity * wheel.stiffness);
            float force = nominalLoad * wheel.stiffness * wheel.compression
                    + nominal * 2.0f * wheel.damping * omega * compressionRate;
            force = Math.clamp(force, 0, MAX_FORCE_FACTOR * nominalLoad);
            wheel.load = force;
            body.applyImpulseAt(rel, up.x * force, up.y * force, up.z * force);

            // Tire forces act in the ground plane, and both are limited by how hard this wheel is
            // pressed into it — no load, no grip, which is what makes a wheel in the air free and
            // a vehicle light on its springs slide.
            body.pointVelocity(rel, pointVelocity);
            projectToPlane(pointVelocity, normal);

            float lateral = pointVelocity.dot(right);
            float lateralLimit = gripLat * force;
            float lateralImpulse = clamp(-lateral * nominal, -lateralLimit, lateralLimit);
            body.applyImpulseAt(rel,
                    right.x * lateralImpulse, right.y * lateralImpulse, right.z * lateralImpulse);

            float longitudinal = pointVelocity.dot(forward);
            float longLimit = gripLong * force;
            float longImpulse = wheel.powered ? drive : 0;
            longImpulse -= longitudinal * nominal * brake;
            longImpulse = clamp(longImpulse, -longLimit, longLimit);
            body.applyImpulseAt(rel,
                    forward.x * longImpulse, forward.y * longImpulse, forward.z * longImpulse);
        }
        return grounded;
    }

    private void projectToPlane(Vector3f vector, Vector3f planeNormal) {
        float along = vector.dot(planeNormal);
        vector.sub(planeNormal.x * along, planeNormal.y * along, planeNormal.z * along);
    }

    private static float clamp(float value, float min, float max) {
        return value < min ? min : Math.min(value, max);
    }

    /**
     * Where a descending sphere comes to rest on one axis-aligned box, exactly.
     * <p>
     * Over the top face the answer is just the face height plus the radius. Off the side it is the
     * top edge, and the sphere stops higher — that is the term that lifts a wheel over a step, so
     * it is worth being exact about rather than treating the box as inflated.
     *
     * @return centre height at rest, or {@link Float#NEGATIVE_INFINITY} if the sphere misses
     */
    public static float supportOnBox(float x, float z, float radius,
                                     float minX, float minZ, float maxX, float maxZ, float topY,
                                     Vector3f normalOut) {
        float dx = x < minX ? minX - x : (x > maxX ? x - maxX : 0);
        float dz = z < minZ ? minZ - z : (z > maxZ ? z - maxZ : 0);
        float horizontal = dx * dx + dz * dz;
        if (horizontal >= radius * radius) {
            return Float.NEGATIVE_INFINITY;
        }
        float lift = (float) Math.sqrt(radius * radius - horizontal);
        if (horizontal == 0) {
            normalOut.set(0, 1, 0);
        } else {
            // Resting on the top edge: the normal leans away from the box by however far off the
            // face the contact sits, so a wheel meeting a riser is pushed up and back, not just up.
            normalOut.set(x < minX ? -dx : (x > maxX ? dx : 0), lift,
                    z < minZ ? -dz : (z > maxZ ? dz : 0)).normalize();
        }
        return topY + lift;
    }

}
