package org.ywzj.vehicle.vehicle.solver;

import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Sequential-impulse contact solver for one dynamic body against static world geometry.
 * <p>
 * Replaces the hand-written contact response in {@code PhysicsEngine} — the per-face velocity
 * cancellation, the climb skirt, the embedded-hull lift, the centre kick and the step-up teleport.
 * Those were five approximations of the two things this does properly: stop bodies passing through
 * each other, and hold up a body that is resting on something.
 * <p>
 * <b>Depenetration is split-impulse, deliberately.</b> The usual cheap alternative, Baumgarte
 * stabilisation, pushes overlapping bodies apart by adding to their velocity — the correction
 * outlives the contact and becomes kinetic energy. That is the precise mechanism behind every
 * bounce this system has suffered: something adds upward velocity to fix a position error, the
 * body leaves the ground, falls back, and the error returns. Here the position pass runs on
 * <em>pseudo-velocities</em> that move the body and are then discarded, so correcting an overlap
 * cannot launch anything. A resting body converges and stays converged.
 * <p>
 * <b>Nothing allocates after warm-up.</b> Contacts live in parallel primitive arrays that grow by
 * doubling and never shrink, all scratch vectors are fields, and the solve loop touches no object
 * headers. This matters more than the arithmetic: on a busy server GC pressure is what shows up as
 * tick lag, and a solver that churns is worse than one that is a little slower per contact.
 * <p>
 * <b>Contact points are stored relative to the body centre.</b> World coordinates in a Minecraft
 * level reach into the millions, where a float has whole-block precision — storing absolute
 * positions here would quantise contacts into uselessness far from spawn.
 *
 * @see <a href="https://box2d.org/files/ErinCatto_SequentialImpulses_GDC2006.pdf">Catto, Sequential Impulses</a>
 */
public final class ContactSolver {

    /**
     * Penetration left uncorrected. Contacts are never driven to exactly zero overlap, because a
     * body that is corrected to exactly touching separates next tick, loses its contact, falls,
     * and re-collides — the jitter that a naive solver is famous for. Holding a little overlap
     * keeps the manifold stable between ticks, which is also what makes warm starting work.
     */
    public static final float SLOP = 0.005f;

    /** Fraction of the remaining overlap corrected per tick. Under-relaxed for stability. */
    public static final float POSITION_RATE = 0.2f;

    /** Cap on positional correction per tick, so a deep overlap does not fling the body out. */
    public static final float MAX_CORRECTION = 0.2f;

    /**
     * Approach speed below which restitution is ignored. A resting body is always closing slightly
     * under gravity; bouncing off that is how a solver invents a permanent micro-hop.
     */
    public static final float RESTITUTION_THRESHOLD = 0.06f;

    /** One Minecraft tick. This codebase carries velocity as blocks per tick, so dt is 1. */
    public static final float DT = 1.0f;

    /** State the solver reads and writes. Owned by the caller and reused between ticks. */
    public static final class Body {

        public float invMass;
        /** Inverse principal moments, body-local. Zero on an axis pins rotation about it. */
        public final Vector3f invInertiaLocal = new Vector3f();
        public final Quaternionf rotation = new Quaternionf();
        /** Centre of mass, world. Contacts are stored relative to this. */
        public final Vector3f centre = new Vector3f();
        public final Vector3f velocity = new Vector3f();
        /** Radians per tick, world axes. */
        public final Vector3f angularVelocity = new Vector3f();

        /** Position-pass velocities. Integrated into the pose, then thrown away. */
        final Vector3f pseudoVelocity = new Vector3f();
        final Vector3f pseudoAngular = new Vector3f();

        /** I⁻¹ in world axes. Rebuilt once per tick by {@link #refreshInertia()}. */
        final Matrix3f invInertiaWorld = new Matrix3f();
        private final Matrix3f inertiaScratch = new Matrix3f();
        private final Vector3f impulseScratch = new Vector3f();

        /**
         * Rebuilds the world-space inverse inertia from the current pose: R · diag(invI) · Rᵀ.
         * Called once per tick, not per contact — it is the same matrix for every one of them.
         */
        public void refreshInertia() {
            inertiaScratch.set(rotation);
            invInertiaWorld.set(inertiaScratch);
            // Column-major, so scaling column i by the i-th inverse moment gives R · diag(invI).
            invInertiaWorld.m00 *= invInertiaLocal.x;
            invInertiaWorld.m01 *= invInertiaLocal.x;
            invInertiaWorld.m02 *= invInertiaLocal.x;
            invInertiaWorld.m10 *= invInertiaLocal.y;
            invInertiaWorld.m11 *= invInertiaLocal.y;
            invInertiaWorld.m12 *= invInertiaLocal.y;
            invInertiaWorld.m20 *= invInertiaLocal.z;
            invInertiaWorld.m21 *= invInertiaLocal.z;
            invInertiaWorld.m22 *= invInertiaLocal.z;
            inertiaScratch.transpose();
            invInertiaWorld.mul(inertiaScratch);
        }

        /**
         * Applies an impulse at a point offset from the centre of mass, so it both pushes and
         * turns the body. {@link #refreshInertia()} must have run this tick.
         *
         * @param rel offset from the centre of mass, world axes
         */
        public void applyImpulseAt(Vector3f rel, float ix, float iy, float iz) {
            velocity.add(ix * invMass, iy * invMass, iz * invMass);
            impulseScratch.set(
                    rel.y * iz - rel.z * iy,
                    rel.z * ix - rel.x * iz,
                    rel.x * iy - rel.y * ix);
            invInertiaWorld.transform(impulseScratch);
            angularVelocity.add(impulseScratch);
        }

        /** Velocity of the material point at {@code rel}, world axes. */
        public Vector3f pointVelocity(Vector3f rel, Vector3f dest) {
            return dest.set(
                    velocity.x + angularVelocity.y * rel.z - angularVelocity.z * rel.y,
                    velocity.y + angularVelocity.z * rel.x - angularVelocity.x * rel.z,
                    velocity.z + angularVelocity.x * rel.y - angularVelocity.y * rel.x);
        }

        /** Solid box inertia, the shape every vehicle cube already is. */
        public void setBoxInertia(float mass, float width, float height, float depth) {
            if (mass <= 0) {
                invMass = 0;
                invInertiaLocal.set(0);
                return;
            }
            invMass = 1.0f / mass;
            float ix = mass / 12.0f * (height * height + depth * depth);
            float iy = mass / 12.0f * (width * width + depth * depth);
            float iz = mass / 12.0f * (width * width + height * height);
            invInertiaLocal.set(ix > 0 ? 1 / ix : 0, iy > 0 ? 1 / iy : 0, iz > 0 ? 1 / iz : 0);
        }

    }

    private int count;
    private int capacity;

    // Contact geometry, body-relative. SoA so a solve iteration walks memory linearly.
    private float[] rx, ry, rz;
    private float[] nx, ny, nz;
    private float[] t1x, t1y, t1z;
    private float[] t2x, t2y, t2z;
    private float[] depth;
    // Anchor in body-local space, plus where it was in the world when the manifold was built.
    // Separation is recomputed from these every substep, which is what makes the solve temporal.
    private float[] alx, aly, alz;
    private float[] aw0x, aw0y, aw0z;
    private float[] baseSeparation;
    private float[] normalMass, tangent1Mass, tangent2Mass;
    private float[] restitutionBias;
    private float[] normalImpulse, tangent1Impulse, tangent2Impulse;
    private long[] id;

    // Previous tick's accumulated impulses, for warm starting.
    private long[] warmId = new long[0];
    private float[] warmNormal = new float[0];
    private float[] warmTangent1 = new float[0];
    private float[] warmTangent2 = new float[0];
    private int warmCount;

    private Matrix3f invInertiaWorld;
    private final Vector3f scratch = new Vector3f();
    private final Vector3f scratchB = new Vector3f();
    private final Quaternionf spin = new Quaternionf();

    public ContactSolver() {
        grow(64);
    }

    public int contactCount() {
        return count;
    }

    /** Drops last tick's contacts, keeping their impulses available for warm starting. */
    public void begin() {
        // Swap rather than copy: this tick's arrays become next tick's warm cache.
        long[] ids = warmId;
        float[] n = warmNormal, a = warmTangent1, b = warmTangent2;
        warmId = id;
        warmNormal = normalImpulse;
        warmTangent1 = tangent1Impulse;
        warmTangent2 = tangent2Impulse;
        warmCount = count;
        id = ids;
        normalImpulse = n;
        tangent1Impulse = a;
        tangent2Impulse = b;
        if (id.length < capacity) {
            id = new long[capacity];
            normalImpulse = new float[capacity];
            tangent1Impulse = new float[capacity];
            tangent2Impulse = new float[capacity];
        }
        count = 0;
    }

    /**
     * @param px,py,pz    contact point, world
     * @param nx,ny,nz    unit normal, pointing out of the static surface toward the body
     * @param penetration overlap depth, positive when overlapping
     * @param contactId   stable across ticks for the same feature pair; drives warm starting
     */
    public void addContact(Body body, float px, float py, float pz,
                           float nx, float ny, float nz, float penetration, long contactId) {
        if (count == capacity) {
            grow(capacity * 2);
        }
        int i = count++;
        this.rx[i] = px - body.centre.x;
        this.ry[i] = py - body.centre.y;
        this.rz[i] = pz - body.centre.z;
        this.nx[i] = nx;
        this.ny[i] = ny;
        this.nz[i] = nz;
        this.depth[i] = penetration;
        this.id[i] = contactId;
    }

    /**
     * Resolves every contact added this tick and integrates the body.
     *
     * @param friction           Coulomb coefficient
     * @param restitution        bounciness, 0 for a vehicle that should stay put
     * @param velocityIterations passes over the manifold; 4–8 is plenty when warm started
     * @param positionIterations passes for depenetration; 2–3 suffices
     */
    public void solve(Body body, float friction, float restitution,
                      int velocityIterations, int positionIterations) {
        solveSoft(body, friction, restitution, Math.max(1, positionIterations), velocityIterations);
    }

    /**
     * Substepped soft solve, after Box3D's TGS Soft ({@code src/solver.c}, {@code contact_solver.c}).
     * <p>
     * Per substep: solve with a soft bias, integrate the pose, then <b>relax</b> — solve again with
     * the bias switched off. The relax pass is the part that matters and the part split impulse was
     * standing in for. A bias injects energy to close overlap; re-solving without it removes
     * whatever of that energy is still in the velocity, so a body ends the step depenetrated but
     * not moving. Split impulse achieves the same by carrying a second velocity that is thrown
     * away, which costs an extra state vector and cannot be substepped.
     * <p>
     * Separation is recomputed every substep from the body's live pose rather than held at the
     * value the manifold was generated with. That is what makes this <em>temporal</em> Gauss-Seidel
     * and it is why substepping converges so much faster than iterating a fixed manifold: each
     * substep is solving the problem as it now is.
     * <p>
     * Contacts with positive separation are solved too, biased at {@code s / h} so the body may
     * approach at exactly the rate that closes the gap this substep and no faster. That is
     * speculation, and it means a contact does not have to be penetrating to stop something —
     * tunnelling stops being a category rather than being defended against.
     */
    public void solveSoft(Body body, float friction, float restitution,
                          int substeps, int relaxIterations) {
        if (count == 0) {
            body.refreshInertia();
            integrate(body);
            return;
        }
        body.refreshInertia();
        invInertiaWorld = body.invInertiaWorld;
        prepare(body, restitution);

        float h = DT / substeps;
        float invH = 1.0f / h;
        makeSoft(CONTACT_HERTZ, CONTACT_DAMPING, h);

        for (int substep = 0; substep < substeps; substep++) {
            body.refreshInertia();
            invInertiaWorld = body.invInertiaWorld;
            warmStart(body);
            solveContacts(body, friction, invH, true);
            integrateSubstep(body, h);
            for (int iteration = 0; iteration < relaxIterations; iteration++) {
                solveContacts(body, friction, invH, false);
            }
        }
        applyRestitution(body, restitution);
    }

    /** Frequency the contact spring runs at. Softer than rigid, stiff enough to look solid. */
    public static final float CONTACT_HERTZ = 30.0f;

    /** Contact damping ratio. Ten is heavily overdamped, which is what stops contacts ringing. */
    public static final float CONTACT_DAMPING = 10.0f;

    /** Fastest a contact may push a body out of an overlap, in blocks per tick. */
    public static final float MAX_CONTACT_SPEED = 0.3f;

    private float softBiasRate;
    private float softMassScale;
    private float softImpulseScale;

    /**
     * Turns a frequency and damping ratio into the three coefficients the solve needs, after
     * Box3D's {@code b3MakeSoft}. Implicit, so any stiffness is stable at any step size — the
     * property that lets the same numbers work for a go-kart and a carrier.
     */
    private void makeSoft(float hertz, float zeta, float h) {
        if (hertz <= 0) {
            softBiasRate = 0;
            softMassScale = 1;
            softImpulseScale = 0;
            return;
        }
        float omega = (float) (2.0 * Math.PI * hertz);
        float a1 = 2.0f * zeta + h * omega;
        float a2 = h * omega * a1;
        float a3 = 1.0f / (1.0f + a2);
        softBiasRate = omega / a1;
        softMassScale = a2 * a3;
        softImpulseScale = a3;
    }

    /** Effective masses, friction basis and restitution — everything constant across iterations. */
    private void prepare(Body body, float restitution) {
        int warmHint = 0;
        for (int i = 0; i < count; i++) {
            float rxi = rx[i], ryi = ry[i], rzi = rz[i];
            float nxi = nx[i], nyi = ny[i], nzi = nz[i];

            basis(nxi, nyi, nzi);
            t1x[i] = scratch.x; t1y[i] = scratch.y; t1z[i] = scratch.z;
            t2x[i] = scratchB.x; t2y[i] = scratchB.y; t2z[i] = scratchB.z;

            // Anchor bookkeeping for the temporal separation. The local form survives the body
            // rotating during the step; the world form is the reference the drift is measured from.
            // Slop folded in, so a body resting at exactly the allowed overlap reports zero
            // separation and the bias leaves it alone. Without this the spring treats the resting
            // slop as an overlap to remove and lifts every settled body by it, every tick —
            // measured at 1.46e-02 of late rise, against the zero this solver is required to hold.
            baseSeparation[i] = SLOP - depth[i];
            aw0x[i] = body.centre.x + rxi;
            aw0y[i] = body.centre.y + ryi;
            aw0z[i] = body.centre.z + rzi;
            scratch.set(rxi, ryi, rzi);
            body.rotation.transformInverse(scratch);
            alx[i] = scratch.x;
            aly[i] = scratch.y;
            alz[i] = scratch.z;

            normalMass[i] = effectiveMass(body, rxi, ryi, rzi, nxi, nyi, nzi);
            tangent1Mass[i] = effectiveMass(body, rxi, ryi, rzi, t1x[i], t1y[i], t1z[i]);
            tangent2Mass[i] = effectiveMass(body, rxi, ryi, rzi, t2x[i], t2y[i], t2z[i]);

            // Restitution from the approach speed before anything is applied, and only above a
            // threshold: a body settling under gravity closes at a fraction of a block per tick,
            // and bouncing off that is a permanent hop.
            float vn = relativeNormalVelocity(body, rxi, ryi, rzi, nxi, nyi, nzi);
            restitutionBias[i] = vn < -RESTITUTION_THRESHOLD ? -restitution * vn : 0;

            // One lookup for all three impulses. Three separate scans made this O(3n^2) and it
            // dominated the whole solve at high contact counts -- 256 contacts cost 24x what 16
            // did, against a linear 16x expected.
            int warm = findWarm(id[i], warmHint);
            if (warm >= 0) {
                normalImpulse[i] = warmNormal[warm];
                tangent1Impulse[i] = warmTangent1[warm];
                tangent2Impulse[i] = warmTangent2[warm];
                warmHint = warm + 1;
            } else {
                normalImpulse[i] = 0;
                tangent1Impulse[i] = 0;
                tangent2Impulse[i] = 0;
            }
        }
    }

    /**
     * Finds last tick's slot for a contact id, trying the expected position first.
     * <p>
     * Contacts are generated in a stable order, so a resting body hands back the same ids in the
     * same sequence every tick and the hint hits immediately. The scan is the fallback for the
     * tick where the manifold actually changes.
     */
    private int findWarm(long contactId, int hint) {
        if (hint < warmCount && warmId[hint] == contactId) {
            return hint;
        }
        for (int i = 0; i < warmCount; i++) {
            if (warmId[i] == contactId) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Replays last tick's impulses before iterating.
     * <p>
     * The single largest convergence win available. A resting body needs the same impulses every
     * tick; starting from them means the iterations correct a small error instead of rediscovering
     * the whole support force, and 4 warm iterations beat 20 cold ones. It is also what stops a
     * stack from sinking under its own weight as the solver runs out of iterations.
     */
    private void warmStart(Body body) {
        for (int i = 0; i < count; i++) {
            float ix = nx[i] * normalImpulse[i] + t1x[i] * tangent1Impulse[i] + t2x[i] * tangent2Impulse[i];
            float iy = ny[i] * normalImpulse[i] + t1y[i] * tangent1Impulse[i] + t2y[i] * tangent2Impulse[i];
            float iz = nz[i] * normalImpulse[i] + t1z[i] * tangent1Impulse[i] + t2z[i] * tangent2Impulse[i];
            applyImpulse(body, rx[i], ry[i], rz[i], ix, iy, iz, false);
        }
    }

    /**
     * One Gauss-Seidel sweep. With {@code useBias} the normal constraint pushes overlap out; the
     * relax pass runs the same sweep with it off, which is what takes the injected energy back.
     */
    private void solveContacts(Body body, float friction, float invH, boolean useBias) {
        for (int i = 0; i < count; i++) {
            float rxi = rx[i], ryi = ry[i], rzi = rz[i];

            // Separation as it is *now*, not as the manifold was built: the anchor has moved with
            // the body since, and using the stale value is what makes a non-substepped solver need
            // so many iterations to settle.
            float separation = currentSeparation(body, i);

            float bias = 0;
            float massScale = 1;
            float impulseScale = 0;
            if (separation > 0) {
                // Speculative: not touching yet. Allow approach at exactly the closing rate.
                bias = separation * invH;
            } else if (useBias) {
                bias = Math.max(softMassScale * softBiasRate * separation, -MAX_CONTACT_SPEED);
                massScale = softMassScale;
                impulseScale = softImpulseScale;
            }

            // Normal first: friction is clamped against the normal impulse, so it wants the
            // current value rather than last iteration's.
            float vn = relativeNormalVelocity(body, rxi, ryi, rzi, nx[i], ny[i], nz[i]);
            float lambda = -normalMass[i] * (massScale * vn + bias)
                    - impulseScale * normalImpulse[i];
            float previous = normalImpulse[i];
            // Clamp the ACCUMULATED impulse, not the increment. A contact may pull during an
            // iteration as long as the total it has applied stays a push; clamping increments
            // instead makes the manifold unable to redistribute and stacks sag.
            float total = Math.max(previous + lambda, 0);
            lambda = total - previous;
            normalImpulse[i] = total;
            applyImpulse(body, rxi, ryi, rzi,
                    nx[i] * lambda, ny[i] * lambda, nz[i] * lambda, false);

            if (useBias) {
                // Box3D skips friction on the biased pass: friction clamped against an impulse
                // that is partly depenetration would let a body be dragged sideways by being
                // pushed out of the floor.
                continue;
            }
            float limit = friction * normalImpulse[i];
            solveFriction(body, i, rxi, ryi, rzi, t1x[i], t1y[i], t1z[i],
                    tangent1Mass[i], limit, true);
            solveFriction(body, i, rxi, ryi, rzi, t2x[i], t2y[i], t2z[i],
                    tangent2Mass[i], limit, false);
        }
    }

    private void solveFriction(Body body, int i, float rxi, float ryi, float rzi,
                               float tx, float ty, float tz, float mass, float limit, boolean first) {
        float vt = relativeNormalVelocity(body, rxi, ryi, rzi, tx, ty, tz);
        float lambda = -vt * mass;
        float previous = first ? tangent1Impulse[i] : tangent2Impulse[i];
        float total = Math.max(-limit, Math.min(previous + lambda, limit));
        lambda = total - previous;
        if (first) {
            tangent1Impulse[i] = total;
        } else {
            tangent2Impulse[i] = total;
        }
        applyImpulse(body, rxi, ryi, rzi, tx * lambda, ty * lambda, tz * lambda, false);
    }

    /**
     * Separation for contact {@code i} at the body's current pose.
     * <p>
     * The anchor is carried in body-local space, so it follows the body as it rotates during the
     * step; how far it has drifted along the normal since the manifold was built is added to the
     * separation the manifold recorded. Box3D calls this the fixed-anchor form, and it is what
     * makes each substep solve the problem as it now stands rather than as it stood at the top of
     * the tick.
     */
    private float currentSeparation(Body body, int i) {
        scratch.set(alx[i], aly[i], alz[i]);
        body.rotation.transform(scratch);
        float dx = body.centre.x + scratch.x - aw0x[i];
        float dy = body.centre.y + scratch.y - aw0y[i];
        float dz = body.centre.z + scratch.z - aw0z[i];
        return dx * nx[i] + dy * ny[i] + dz * nz[i] + baseSeparation[i];
    }

    /**
     * Restitution, applied once after every substep rather than mixed into the solve.
     * <p>
     * Bouncing is a property of the impact, so it is resolved against the approach speed recorded
     * before anything touched the body. Folding it into the biased pass instead lets depenetration
     * masquerade as an impact, which is a permanent hop.
     */
    private void applyRestitution(Body body, float restitution) {
        if (restitution <= 0) {
            return;
        }
        for (int i = 0; i < count; i++) {
            if (restitutionBias[i] <= 0 || normalImpulse[i] == 0) {
                continue;
            }
            float rxi = rx[i], ryi = ry[i], rzi = rz[i];
            float vn = relativeNormalVelocity(body, rxi, ryi, rzi, nx[i], ny[i], nz[i]);
            if (vn > -RESTITUTION_THRESHOLD) {
                continue;
            }
            float lambda = -normalMass[i] * (vn + restitutionBias[i]);
            float previous = normalImpulse[i];
            float total = Math.max(previous + lambda, 0);
            lambda = total - previous;
            normalImpulse[i] = total;
            applyImpulse(body, rxi, ryi, rzi,
                    nx[i] * lambda, ny[i] * lambda, nz[i] * lambda, false);
        }
    }

    /** Advances the pose by one substep. Velocity only — there is no second velocity any more. */
    private void integrateSubstep(Body body, float h) {
        body.centre.add(body.velocity.x * h, body.velocity.y * h, body.velocity.z * h);
        float wx = body.angularVelocity.x;
        float wy = body.angularVelocity.y;
        float wz = body.angularVelocity.z;
        float speed = (float) Math.sqrt(wx * wx + wy * wy + wz * wz);
        if (speed > 1.0e-7f) {
            spin.fromAxisAngleRad(wx / speed, wy / speed, wz / speed, speed * h);
            body.rotation.premul(spin).normalize();
        }
    }

    private void integrate(Body body) {
        Vector3f v = body.pseudoVelocity;
        body.centre.add((body.velocity.x + v.x) * DT,
                (body.velocity.y + v.y) * DT,
                (body.velocity.z + v.z) * DT);
        float wx = body.angularVelocity.x + body.pseudoAngular.x;
        float wy = body.angularVelocity.y + body.pseudoAngular.y;
        float wz = body.angularVelocity.z + body.pseudoAngular.z;
        float speed = (float) Math.sqrt(wx * wx + wy * wy + wz * wz);
        if (speed > 1.0e-7f) {
            // Exponential map rather than a first-order add, so a fast spin cannot inflate the
            // quaternion and skew the body. Pre-multiplied because angular velocity is in world
            // axes: a world-frame rotation composes on the left of the body's current pose.
            float angle = speed * DT;
            spin.fromAxisAngleRad(wx / speed, wy / speed, wz / speed, angle);
            body.rotation.premul(spin).normalize();
        }
        body.pseudoVelocity.set(0);
        body.pseudoAngular.set(0);
    }

    // ---- inner loop helpers, all branch-light and allocation-free ----

    private float effectiveMass(Body body, float rxi, float ryi, float rzi,
                                float dx, float dy, float dz) {
        float cx = ryi * dz - rzi * dy;
        float cy = rzi * dx - rxi * dz;
        float cz = rxi * dy - ryi * dx;
        scratch.set(cx, cy, cz);
        invInertiaWorld.transform(scratch);
        float angular = scratch.x * cx + scratch.y * cy + scratch.z * cz;
        float k = body.invMass + angular;
        return k > 0 ? 1.0f / k : 0;
    }

    private float relativeNormalVelocity(Body body, float rxi, float ryi, float rzi,
                                         float dx, float dy, float dz) {
        Vector3f w = body.angularVelocity;
        float vx = body.velocity.x + w.y * rzi - w.z * ryi;
        float vy = body.velocity.y + w.z * rxi - w.x * rzi;
        float vz = body.velocity.z + w.x * ryi - w.y * rxi;
        return vx * dx + vy * dy + vz * dz;
    }

    private float pseudoNormalVelocity(Body body, float rxi, float ryi, float rzi,
                                       float dx, float dy, float dz) {
        Vector3f w = body.pseudoAngular;
        float vx = body.pseudoVelocity.x + w.y * rzi - w.z * ryi;
        float vy = body.pseudoVelocity.y + w.z * rxi - w.x * rzi;
        float vz = body.pseudoVelocity.z + w.x * ryi - w.y * rxi;
        return vx * dx + vy * dy + vz * dz;
    }

    private void applyImpulse(Body body, float rxi, float ryi, float rzi,
                              float ix, float iy, float iz, boolean pseudo) {
        Vector3f linear = pseudo ? body.pseudoVelocity : body.velocity;
        Vector3f angular = pseudo ? body.pseudoAngular : body.angularVelocity;
        linear.add(ix * body.invMass, iy * body.invMass, iz * body.invMass);
        float cx = ryi * iz - rzi * iy;
        float cy = rzi * ix - rxi * iz;
        float cz = rxi * iy - ryi * ix;
        scratch.set(cx, cy, cz);
        invInertiaWorld.transform(scratch);
        angular.add(scratch);
    }

    /** Two unit vectors perpendicular to n, chosen without a branch-heavy special case. */
    private void basis(float nxi, float nyi, float nzi) {
        if (Math.abs(nxi) >= 0.57735f) {
            scratch.set(nyi, -nxi, 0);
        } else {
            scratch.set(0, nzi, -nyi);
        }
        scratch.normalize();
        scratchB.set(
                nyi * scratch.z - nzi * scratch.y,
                nzi * scratch.x - nxi * scratch.z,
                nxi * scratch.y - nyi * scratch.x);
    }

    private void grow(int newCapacity) {
        rx = copy(rx, newCapacity); ry = copy(ry, newCapacity); rz = copy(rz, newCapacity);
        nx = copy(nx, newCapacity); ny = copy(ny, newCapacity); nz = copy(nz, newCapacity);
        t1x = copy(t1x, newCapacity); t1y = copy(t1y, newCapacity); t1z = copy(t1z, newCapacity);
        t2x = copy(t2x, newCapacity); t2y = copy(t2y, newCapacity); t2z = copy(t2z, newCapacity);
        depth = copy(depth, newCapacity);
        alx = copy(alx, newCapacity); aly = copy(aly, newCapacity); alz = copy(alz, newCapacity);
        aw0x = copy(aw0x, newCapacity); aw0y = copy(aw0y, newCapacity); aw0z = copy(aw0z, newCapacity);
        baseSeparation = copy(baseSeparation, newCapacity);
        normalMass = copy(normalMass, newCapacity);
        tangent1Mass = copy(tangent1Mass, newCapacity);
        tangent2Mass = copy(tangent2Mass, newCapacity);
        restitutionBias = copy(restitutionBias, newCapacity);
        normalImpulse = copy(normalImpulse, newCapacity);
        tangent1Impulse = copy(tangent1Impulse, newCapacity);
        tangent2Impulse = copy(tangent2Impulse, newCapacity);
        long[] grown = new long[newCapacity];
        if (id != null) {
            System.arraycopy(id, 0, grown, 0, Math.min(id.length, newCapacity));
        }
        id = grown;
        capacity = newCapacity;
    }

    private static float[] copy(float[] source, int newCapacity) {
        float[] grown = new float[newCapacity];
        if (source != null) {
            System.arraycopy(source, 0, grown, 0, Math.min(source.length, newCapacity));
        }
        return grown;
    }

}
