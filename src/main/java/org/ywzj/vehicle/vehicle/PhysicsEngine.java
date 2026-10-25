package org.ywzj.vehicle.vehicle;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import org.joml.Math;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.ywzj.vehicle.all.AllConfigs;
import org.ywzj.vehicle.api.event.VehicleCollectCollisionEvent;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.util.PhysicsHelper;
import org.ywzj.vehicle.util.VectorUtil;
import org.ywzj.vehicle.vehicle.part.SuspensionUnit;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;
import org.ywzj.vehicle.vehicle.pojo.PhysicsInfo;
import org.ywzj.vehicle.vehicle.structure.OBB;
import org.ywzj.vehicle.vehicle.structure.VehicleCubeOBB;

import java.util.*;
import java.util.function.Function;

public class PhysicsEngine {

    public static final double MAGIC_NUMBER = .943;
    public static float G = PhysicsHelper.GRAVITY / PhysicsHelper.TICKS_PER_SECOND_SQUARED;
    private static final double BODY_CONTACT_SKIN = 0.02;
    private static final double BODY_CONTACT_RECOVERY = 0.25;
    private static final double BODY_CONTACT_SLOP = 0.002;
    private static final int SUSPENSION_SUBSTEPS = 8;
    private static final double SPEED_EPSILON = 1.0E-6;
    public final AbstractVehicle vehicle;
    public PhysicsInfo physicsInfo;
    public float bounce = 0.05f;
    public float angularDampingGround = 0.88f;
    public float angularDampingAir = 0.96f;
    public float torqueScale = 4.0f;
    public float maxRotV = 0.3f;
    public float rotV = 0;
    public int rotTick;
    public Quaternionf stepRot;
    public Vector3f localRotAxisStart;
    public Vector3f localRotAxisStartO;
    public Vector3f localRotAxisEnd;
    public Vector3f localRotAxisEndO;
    public Vector3f localRotAxisVec;
    public Vector3f planeSupport;
    public Vector3f planeU;
    public Vector3f planeV;
    public Vector3f velocity = new Vector3f(0, 0, 0);
    public Vector3f velocityO = new Vector3f(0, 0, 0);
    public boolean lockZRot;
    public boolean lockCenterRot;
    private final Set<VehicleCubeOBB.CubePoint> vehicleContactPoints = new HashSet<>();
    private boolean suspensionGrounded;
    private float suspensionPitchVelocity;
    private float suspensionRollVelocity;
    private float submergedRatio;
    private double climbStartHeight = Double.NaN;
    private boolean climbBlocked;
    public int stuckTick;

    private record BodyContacts(List<VehicleCubeOBB.CubePoint> touchPoints, Map<VehicleCubeOBB.CubePoint, Double> supports) {}
    private record BodySupportContact(double pitchArm, double rollArm, double gap) {}
    private record SuspensionImpactContact(double pitchArm, double rollArm, double inverseEffectiveMass) {}
    private record SuspensionContact(SuspensionUnit<?> unit, double compression, double minCompression, double maxCompression, double weight, double verticalArm, double pitchArm, double rollArm) {}
    private record SupportCorrection(double verticalArm, double pitchArm, double rollArm, double gap, double maxSpeed) {}

    public PhysicsEngine(AbstractVehicle vehicle) {
        this.vehicle = vehicle;
        this.physicsInfo = new PhysicsInfo();
    }

    public void tick(Vec3 force) {
        force = force.scale(1.0 / PhysicsHelper.TICKS_PER_SECOND_SQUARED);
        VehicleCubeOBB cube = physicsCube();
        List<SuspensionUnit<?>> suspensions = vehicle.getSuspensionUnits();
        boolean hasSuspension = vehicle.hasSuspension();
        if (hasSuspension) {
            double movedY = Math.abs(vehicle.deltaMovementO.y) < 0.001 ? 0 : vehicle.deltaMovementO.y;
            vehicle.setPos(vehicle.getX(), vehicle.getY() - movedY, vehicle.getZ());
        }
        cube.update(vehicle);
        Vector3f[] axes = cube.obb().getAxes();
        climbBlocked = false;
        BodyContacts contacts = collectContacts(axes, hasSuspension && vehicle.collision);
        if (hasSuspension && vehicle.collision && climb(contacts.touchPoints)) {
            cube.update(vehicle);
            contacts = collectContacts(axes, true);
        }
        suspensionGrounded = false;
        velocityO.set(this.velocity);
        Vec3 velocity = vehicle.getDeltaMovement();
        this.velocity.set((float) velocity.x, (float) velocity.y, (float) velocity.z);
        for (SuspensionUnit<?> suspension : suspensions) {
            suspension.simulate();
            suspensionGrounded |= suspension.isGrounded();
        }
        if (vehicle.collision) {
            motionByImpact(contacts.touchPoints, axes);
        }
        decelerationByFriction(contacts.touchPoints);
        force = force.add(motionByBuoyancy());
        try {
            if (hasSuspension) {
                motionBySuspension(suspensions, contacts.supports);
            } else {
                rotAndFallByGravity(contacts.touchPoints, axes, force.toVector3f());
            }
        } finally {
            rightInLiquid();
        }
        vehicle.setDeltaMovement(new Vec3(this.velocity));
    }

    public void push(PhysicsEngine other) {
        if (!vehicle.collision || !other.vehicle.collision || vehicle.noPhysics || other.vehicle.noPhysics
                || vehicle.isPassengerOfSameVehicle(other.vehicle)) {
            return;
        }
        physicsCube().update(vehicle);
        other.physicsCube().update(other.vehicle);
        if (!OBB.isColliding(physicsCube().obb(), other.physicsCube().obb())) {
            return;
        }
        vehicle.impact(other.vehicle);
        if (vehicle.getId() > other.vehicle.getId() || !other.vehicle.isPushable()) {
            return;
        }
        Vec3 separation = other.vehicle.position().subtract(vehicle.position()).multiply(1, 0, 1);
        if (separation.horizontalDistanceSqr() < 1.0E-4) {
            return;
        }
        Vec3 normal = separation.normalize();
        double speed = other.vehicle.getDeltaMovement().subtract(vehicle.getDeltaMovement()).dot(normal);
        double separationSpeed = 0.01;
        if (speed < separationSpeed) {
            double mass = Math.max(physicsInfo.mass, 1.0E-3);
            double otherMass = Math.max(other.physicsInfo.mass, 1.0E-3);
            double velocityChange = separationSpeed - speed;
            setVelocity(vehicle.getDeltaMovement().subtract(normal.scale(velocityChange * otherMass / (mass + otherMass))));
            other.setVelocity(other.vehicle.getDeltaMovement().add(normal.scale(velocityChange * mass / (mass + otherMass))));
        }
    }

    /**
     * 以 tick 为时间单位分步积分升沉、俯仰和侧倾，降低隐式弹簧的数值耗散。
     * 每个子步更新预测压缩量及接触间隙，再耦合求解弹簧、行程限位和车体支撑。
     */
    private void motionBySuspension(List<SuspensionUnit<?>> suspensions,
                                    Map<VehicleCubeOBB.CubePoint, Double> bodySupports) {
        float mass = physicsInfo.mass;
        rotV = 0;
        VehicleCubeOBB cube = physicsCube();
        Quaternionf bodyRotation = vehicle.rotYXZ();
        Quaternionf yaw = new Quaternionf().rotateY((float) Math.toRadians(-vehicle.getYRot()));
        Quaternionf inverseYaw = new Quaternionf(yaw).invert();
        Vec3 centerLocal = getLocalCenterOfMass();
        Vector3f centerOffset = bodyRotation.transform(centerLocal.subtract(vehicle.centerOffset).toVector3f());
        Vec3 centerWorld = vehicle.position().add(vehicle.centerOffset).add(new Vec3(centerOffset));
        double inverseMass = 1.0 / mass;
        Vec3 center = physicsInfo.center;
        double inversePitchInertia = lockCenterRot ? 0 : 12.0 / (mass * Math.max(0.01,
                cube.height * cube.height + cube.depth * cube.depth + 12 * (center.y * center.y + center.z * center.z)));
        double inverseRollInertia = lockCenterRot || lockZRot ? 0 : 12.0 / (mass * Math.max(0.01,
                cube.width * cube.width + cube.height * cube.height + 12 * (center.x * center.x + center.y * center.y)));
        List<SuspensionContact> contacts = new ArrayList<>();
        double supportHeight = 0;
        double supportWeight = 0;
        for (SuspensionUnit<?> suspension : suspensions) {
            if (!suspension.isGrounded()) {
                continue;
            }
            double travelPerVertical = 1 / -suspension.getSuspensionDirection().y;
            for (SuspensionUnit.SupportContact point : suspension.getSupportContacts()) {
                Vec3 support = point.position();
                Vector3f arm = inverseYaw.transform(support.subtract(centerWorld).toVector3f());
                contacts.add(new SuspensionContact(suspension, point.compression(), point.minCompression(),
                        point.maxCompression(), point.weight(), travelPerVertical,
                        -arm.z * travelPerVertical, arm.x * travelPerVertical));
                supportHeight += (support.y - centerWorld.y) * point.weight();
                supportWeight += point.weight();
            }
        }
        // 接地后的衰减由悬挂阻尼控制，只有悬空时使用空气角阻尼。
        double angularRetention = contacts.isEmpty() ? angularDampingAir : 1;
        double pitchVelocity = inversePitchInertia == 0 ? 0 : suspensionPitchVelocity * angularRetention;
        double rollVelocity = inverseRollInertia == 0 ? 0 : suspensionRollVelocity * angularRetention;
        double pitchAcceleration = 0;
        double rollAcceleration = 0;
        if (!contacts.isEmpty()) {
            Vector3f acceleration = inverseYaw.transform(new Vector3f(velocity.x - velocityO.x, 0, velocity.z - velocityO.z));
            double leverY = supportHeight / supportWeight;
            // 簧载力臂设下限，避免低重心使行驶姿态响应反向。
            double responseLever = Math.max(-leverY, cube.height * 0.35);
            double rollStiffness = 0;
            double pitchStiffness = 0;
            for (SuspensionContact contact : contacts) {
                double stiffness = PhysicsHelper.forcePerTick(contact.unit.getData().getSpringStiffness()) * contact.weight;
                rollStiffness += stiffness * contact.rollArm * contact.rollArm;
                pitchStiffness += stiffness * contact.pitchArm * contact.pitchArm;
            }
            // 行驶力矩按静态偏角限幅，坡面支撑和碰撞不受此限。
            double maxDrivePitchTorque = pitchStiffness * Math.toRadians(6);
            double drivePitchTorque = Mth.clamp(-mass * acceleration.z * responseLever * physicsInfo.suspensionResponse,
                    -maxDrivePitchTorque, maxDrivePitchTorque);
            pitchAcceleration = drivePitchTorque * inversePitchInertia;
            double maxDriveRollTorque = rollStiffness * Math.toRadians(8);
            double driveRollTorque = Mth.clamp(mass * acceleration.x * responseLever * physicsInfo.suspensionResponse,
                    -maxDriveRollTorque, maxDriveRollTorque);
            rollAcceleration = driveRollTorque * inverseRollInertia;
        }
        double verticalVelocity = velocity.y;
        List<BodySupportContact> bodyContacts = new ArrayList<>();
        for (var entry : bodySupports.entrySet()) {
            Vec3 point = new Vec3(entry.getKey().cachedWorldPos());
            Vector3f arm = inverseYaw.transform(point.subtract(centerWorld).toVector3f());
            double gap = point.y - entry.getValue();
            bodyContacts.add(new BodySupportContact(-arm.z, arm.x, gap));
        }
        List<SupportCorrection> corrections = new ArrayList<>();
        for (SuspensionContact contact : contacts) {
            corrections.add(new SupportCorrection(contact.verticalArm, contact.pitchArm, contact.rollArm,
                    contact.maxCompression - contact.compression, 0.05));
        }
        for (BodySupportContact contact : bodyContacts) {
            corrections.add(new SupportCorrection(1, contact.pitchArm, contact.rollArm, contact.gap, 0.03));
        }
        double[] impulses = new double[contacts.size()];
        double[] stopImpulses = new double[contacts.size()];
        double[] bodyImpulses = new double[bodyContacts.size()];
        double[] correctionImpulses = new double[corrections.size()];
        double dt = 1.0 / SUSPENSION_SUBSTEPS;
        double verticalDisplacement = 0;
        double pitchDisplacement = 0;
        double rollDisplacement = 0;
        boolean bodyGrounded = false;
        for (int substep = 0; substep < SUSPENSION_SUBSTEPS; substep++) {
            verticalVelocity -= G * dt;
            // 仅在求解前限速，避免破坏求解后的不穿透约束。
            pitchVelocity = Mth.clamp(pitchVelocity + pitchAcceleration * dt, -0.08, 0.08);
            rollVelocity = Mth.clamp(rollVelocity + rollAcceleration * dt, -0.08, 0.08);
            Arrays.fill(impulses, 0);
            Arrays.fill(stopImpulses, 0);
            Arrays.fill(bodyImpulses, 0);
            for (int iteration = 0; iteration < 16; iteration++) {
                for (int i = 0; i < contacts.size(); i++) {
                    SuspensionContact contact = contacts.get(i);
                    double verticalArm = contact.verticalArm;
                    double pitchArm = contact.pitchArm;
                    double rollArm = contact.rollArm;
                    double stiffness = PhysicsHelper.forcePerTick(contact.unit.getData().getSpringStiffness()) * contact.weight;
                    double damping = PhysicsHelper.dampingPerTick(contact.unit.getData().getDamping()) * contact.weight;
                    double pointVelocity = verticalArm * verticalVelocity + pitchArm * pitchVelocity + rollArm * rollVelocity;
                    double inverseEffectiveMass = verticalArm * verticalArm * inverseMass + pitchArm * pitchArm * inversePitchInertia
                            + rollArm * rollArm * inverseRollInertia;
                    double contactCompression = contact.compression
                            - verticalArm * verticalDisplacement - pitchArm * pitchDisplacement - rollArm * rollDisplacement;
                    double compression = Mth.clamp(contactCompression,
                            contact.minCompression, contact.maxCompression);
                    // J = dt * (k * (compression - dt * v) - damping * v)。
                    double velocityResponse = dt * (damping + dt * stiffness);
                    double residual = dt * stiffness * compression - velocityResponse * pointVelocity - impulses[i];
                    double nextImpulse = contactCompression < contact.minCompression ? 0
                            : Math.max(0, impulses[i] + residual / (1 + velocityResponse * inverseEffectiveMass));
                    double deltaImpulse = nextImpulse - impulses[i];
                    impulses[i] = nextImpulse;
                    verticalVelocity += deltaImpulse * verticalArm * inverseMass;
                    pitchVelocity += deltaImpulse * pitchArm * inversePitchInertia;
                    rollVelocity += deltaImpulse * rollArm * inverseRollInertia;

                    // 行程限位
                    double minimumVelocity = Math.min(0,
                            (contactCompression - contact.maxCompression) / dt);
                    pointVelocity = verticalArm * verticalVelocity + pitchArm * pitchVelocity + rollArm * rollVelocity;
                    double nextStopImpulse = Math.max(0,
                            stopImpulses[i] + (minimumVelocity - pointVelocity) / inverseEffectiveMass);
                    double deltaStopImpulse = nextStopImpulse - stopImpulses[i];
                    stopImpulses[i] = nextStopImpulse;
                    verticalVelocity += deltaStopImpulse * verticalArm * inverseMass;
                    pitchVelocity += deltaStopImpulse * pitchArm * inversePitchInertia;
                    rollVelocity += deltaStopImpulse * rollArm * inverseRollInertia;
                }
                for (int i = 0; i < bodyContacts.size(); i++) {
                    BodySupportContact contact = bodyContacts.get(i);
                    double pointVelocity = verticalVelocity + contact.pitchArm * pitchVelocity + contact.rollArm * rollVelocity;
                    double inverseEffectiveMass = inverseMass + contact.pitchArm * contact.pitchArm * inversePitchInertia
                            + contact.rollArm * contact.rollArm * inverseRollInertia;
                    double gap = contact.gap + verticalDisplacement
                            + contact.pitchArm * pitchDisplacement + contact.rollArm * rollDisplacement;
                    double minimumVelocity = gap >= 0 ? -gap / dt : 0;
                    double nextImpulse = Math.max(0,
                            bodyImpulses[i] + (minimumVelocity - pointVelocity) / inverseEffectiveMass);
                    double deltaImpulse = nextImpulse - bodyImpulses[i];
                    bodyImpulses[i] = nextImpulse;
                    verticalVelocity += deltaImpulse * inverseMass;
                    pitchVelocity += deltaImpulse * contact.pitchArm * inversePitchInertia;
                    rollVelocity += deltaImpulse * contact.rollArm * inverseRollInertia;
                }
            }
            // 穿透修正
            double verticalCorrection = 0;
            double pitchCorrection = 0;
            double rollCorrection = 0;
            Arrays.fill(correctionImpulses, 0);
            for (int iteration = 0; iteration < 16; iteration++) {
                for (int i = 0; i < corrections.size(); i++) {
                    SupportCorrection contact = corrections.get(i);
                    double gap = contact.gap + contact.verticalArm * (verticalDisplacement + verticalVelocity * dt)
                            + contact.pitchArm * (pitchDisplacement + pitchVelocity * dt)
                            + contact.rollArm * (rollDisplacement + rollVelocity * dt);
                    double minimumVelocity = gap >= 0 ? -gap / dt
                            : Math.min(contact.maxSpeed, Math.max(0, -gap - BODY_CONTACT_SLOP) * 0.2 / dt);
                    double pointVelocity = contact.verticalArm * verticalCorrection
                            + contact.pitchArm * pitchCorrection + contact.rollArm * rollCorrection;
                    double inverseEffectiveMass = contact.verticalArm * contact.verticalArm * inverseMass
                            + contact.pitchArm * contact.pitchArm * inversePitchInertia
                            + contact.rollArm * contact.rollArm * inverseRollInertia;
                    double nextImpulse = Math.max(0,
                            correctionImpulses[i] + (minimumVelocity - pointVelocity) / inverseEffectiveMass);
                    double deltaImpulse = nextImpulse - correctionImpulses[i];
                    correctionImpulses[i] = nextImpulse;
                    verticalCorrection += deltaImpulse * contact.verticalArm * inverseMass;
                    pitchCorrection += deltaImpulse * contact.pitchArm * inversePitchInertia;
                    rollCorrection += deltaImpulse * contact.rollArm * inverseRollInertia;
                }
            }
            verticalDisplacement += (verticalVelocity + verticalCorrection) * dt;
            pitchDisplacement += (pitchVelocity + pitchCorrection) * dt;
            rollDisplacement += (rollVelocity + rollCorrection) * dt;
            for (double impulse : bodyImpulses) {
                bodyGrounded |= impulse > 1.0E-8;
            }
        }
        velocity.y = (float) (climbBlocked ? Math.min(verticalVelocity, 0) : verticalVelocity);
        suspensionPitchVelocity = (float) pitchVelocity;
        suspensionRollVelocity = (float) rollVelocity;
        Quaternionf step = new Quaternionf(yaw)
                .mul(new Quaternionf().rotationXYZ((float) pitchDisplacement, 0, (float) rollDisplacement))
                .mul(inverseYaw);
        Quaternionf rotation = step.mul(bodyRotation);
        setRotation(rotation);
        if (bodyGrounded && AllConfigs.common.selfRighting.get()
                && (Math.abs(Mth.wrapDegrees(vehicle.getXRot())) >= 75
                || Math.abs(Mth.wrapDegrees(vehicle.getZRot())) >= 75)) {
            vehicle.setXRot(0);
            vehicle.setZRot(0);
            suspensionPitchVelocity = 0;
            suspensionRollVelocity = 0;
        }
        // 补偿实体枢轴偏移，使角运动保持物理重心的位置。
        Vector3f rotatedCenter = vehicle.rotYXZ().transform(centerLocal.subtract(vehicle.centerOffset).toVector3f());
        vehicle.setPos(vehicle.position().add(new Vec3(centerOffset.sub(rotatedCenter)))
                .add(0, verticalDisplacement, 0));
        vehicle.setOnGround(suspensionGrounded || bodyGrounded);
    }

    /**
     * 受重力影响下的自由落体与三轴滚动
     */
    private void rotAndFallByGravity(List<VehicleCubeOBB.CubePoint> touchPoints, Vector3f[] axes, Vector3f force) {
        var physicsCube = vehicle.getMainCubeOBB();
        Vector3f velocity = this.velocity;
        try {
            // 加速度使得重心偏移
            Vector3f a = new Vector3f(velocity).sub(this.velocityO);
            Vector3f gravityCenter = physicsInfo.center.toVector3f();
            gravityCenter.add(a.mul((float) (physicsCube.height * 8)));
            // 升力影响
            if (force.y >= G * physicsInfo.mass) {
                velocity.y -= G;
                vehicle.setOnGround(false);
                return;
            }
            // 无任何接触，因转动惯量而继续转动，因重力而自由落体
            if (touchPoints.isEmpty()) {
                centerRot(gravityCenter, axes);
                rotV *= angularDampingAir;
                if (rotV < 0.001f) {
                    rotV = 0;
                }
                velocity.y -= G;
                vehicle.setOnGround(false);
                return;
            }
            vehicle.setOnGround(true);
            // 统计重力在三轴方向上的分力的出面上的接触点，取其局部坐标
            Vector3f gWorldDirection = new Vector3f(0, -1, 0);
            List<Vector3f> localForcePoints = touchPoints.stream()
                    .filter(point -> bodySupportAlignment(point, axes) > 0)
                    .filter(touchPoint -> {
                        BlockState blockState = touchPoint.cubePointContext.blockState();
                        if (isHalfBlock(blockState)) {
                            Vector3f worldPos = touchPoint.cachedWorldPos();
                            return worldPos.y <= touchPoint.cubePointContext.blockPos().y + 0.6f;
                        }
                        return true;
                    })
                    .map(VehicleCubeOBB.CubePoint::obbLocalPos)
                    .toList();
            // 重力方向在局部坐标系下的向量
            float gx = gWorldDirection.dot(axes[0]);
            float gy = gWorldDirection.dot(axes[1]);
            float gz = gWorldDirection.dot(axes[2]);
            Vector3f gLocalDirection = new Vector3f(gx, gy, gz);
            // 重力、受力点投影到重力为法向量的平面上
            Vector2f gc = getPlaneXY(gLocalDirection, gravityCenter);
            HashMap<Vector2f, Vector3f> points = new HashMap<>();
            for (Vector3f forcePoint : localForcePoints) {
                points.put(getPlaneXY(null, forcePoint), forcePoint);
            }
            if (localForcePoints.size() > 2) {
                localRotAxisStartO = localRotAxisStart;
                localRotAxisEndO = localRotAxisEnd;
                List<Vector2f> polygon = VectorUtil.convexHull(new ArrayList<>(points.keySet()));
                // 重心于支撑点闭包内，转动停止，自由落体停止
                if (VectorUtil.isPointInPolygon(gc, polygon)) {
                    velocity.y = Math.max(0, velocity.y);
                    rotV = 0;
                    climb(touchPoints);
                    if (!localForcePoints.stream().allMatch(localForcePoint -> localForcePoint.y < -physicsCube.obb().extents().y - 0.01)) {
                        Vector3f selfRot = new Vector3f();
                        physicsCube.selfRot().getEulerAnglesYXZ(selfRot);
                        // 保持静态倾斜的理论极限角度是半格高垫起车身边，再小则自动补正
                        double angleDepth = Math.toDegrees(Math.atan2(0.5, physicsCube.getDepth()));
                        double angleWidth = Math.toDegrees(Math.atan2(0.5, physicsCube.getWidth()));
                        boolean shouldRotUpdate = false;
                        if (Mth.abs(vehicle.getXRot()) < angleDepth - MAGIC_NUMBER / 10) {
                            vehicle.setXRot((float) Math.toDegrees(-selfRot.x));
                            shouldRotUpdate = true;
                        }
                        if (Mth.abs(vehicle.getZRot()) < angleWidth - MAGIC_NUMBER / 10) {
                            vehicle.setZRot((float) Math.toDegrees(-selfRot.z));
                            shouldRotUpdate = true;
                        }
                        if (shouldRotUpdate && rotTick > 0) {
                            vehicle.triggerPosRotUpdate();
                            rotTick -= 1;
                        }
                    }
                    if (AllConfigs.common.selfRighting.get()) {
                        if (Mth.abs(vehicle.getXRot()) >= 75 || Mth.abs(vehicle.getZRot()) >= 75) {
                            vehicle.setXRot(0);
                            vehicle.setZRot(0);
                        }
                    }
                    return;
                }
                float minDist = Float.MAX_VALUE;
                int minIdx = -1;
                for (int i = 0; i < polygon.size(); i++) {
                    int j = (i + 1) % polygon.size();
                    float d = VectorUtil.pointToSegmentDist(gc, polygon.get(i), polygon.get(j));
                    if (d < minDist) {
                        minDist = d;
                        minIdx = i;
                    }
                }
                if (minIdx == -1) {
                    return;
                }
                localRotAxisStart = points.get(polygon.get(minIdx));
                localRotAxisEnd = points.get(polygon.get((minIdx + 1) % polygon.size()));
            } else if (localForcePoints.size() == 2) {
                localRotAxisStart = localForcePoints.get(0);
                localRotAxisEnd = localForcePoints.get(1);
            } else if (localForcePoints.size() == 1) {
                // 从接触点到重心的向量，投影到支撑平面上
                Vector3f v = new Vector3f(gravityCenter).sub(localForcePoints.get(0));
                Vector3f vProj = new Vector3f(v).sub(new Vector3f(gLocalDirection).mul(v.dot(gLocalDirection)));
                float len = vProj.length();
                if (len < 0.0001f) {
                    rotV = 0;
                    return;
                }
                // 旋转轴在支撑平面内，垂直于vProj：axis = gLocal × vProj
                Vector3f axisDir = new Vector3f(gLocalDirection).cross(vProj).normalize();
                float axisHalfLen = 0.5f;
                localRotAxisStart = new Vector3f(axisDir).mul(axisHalfLen).add(localForcePoints.get(0));
                localRotAxisEnd = new Vector3f(axisDir).mul(-axisHalfLen).add(localForcePoints.get(0));
            } else {
                // 重力在三轴方向上的分力所对应三面无接触点，则无支持力，因转动惯量而继续转动，因重力而自由落体
                rotV *= angularDampingAir;
                if (rotV < 0.001f) rotV = 0;
                centerRot(gravityCenter, axes);
                velocity.y -= G;
                return;
            }
            checkDirection(gravityCenter);
            rotLoss(gc);
            localRotAxisVec = new Vector3f(localRotAxisEnd).sub(localRotAxisStart);
            // 基于力矩和转动惯量计算角加速度
            // 合力 = 重力 + 外部推力（force在局部坐标系下的投影）
            Vector3f netForceLocal = new Vector3f(gLocalDirection).mul(G * physicsInfo.mass);
            netForceLocal.add(force.dot(axes[0]), force.dot(axes[1]), force.dot(axes[2]));
            float torque = computeTorque(localRotAxisStart, localRotAxisEnd, gravityCenter, netForceLocal);
            float moi = computeMomentOfInertia(localRotAxisStart, localRotAxisEnd, physicsCube, physicsInfo.mass, gravityCenter);
            float angularAccel = moi > 0.001f ? torqueScale * torque / moi : 0;
            rotV = rotV * angularDampingGround + angularAccel;
            rotV = Math.min(rotV, maxRotV);
            rot(axes);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    /**
     * 载具的正朝向约定为自身Z轴正方向
     * 车体视作理想刚体，采样点受方块的力垂直于OBB面向内
     * 方块作用力将完全抵消载具速度在力反方向上的分速度
     * 追加一个模拟撞击力导致的力方向上的微小速度
     * 为助于攀爬方块，一定车体高度下的方块碰撞会被忽略
     * 车体底面若有陷地则会施加较大的向上速度
     */
    private void motionByImpact(List<VehicleCubeOBB.CubePoint> touchPoints, Vector3f[] axes) {
        VehicleCubeOBB physicsCube = vehicle.getMainCubeOBB();
        boolean hasSuspension = vehicle.hasSuspension();
        boolean isStuck = false;
        Vec3 velocity = new Vec3(this.velocity);
        List<VehicleCubeOBB.CubePoint> bottomContacts = new ArrayList<>();

        double velocityO = velocity.length();
        for (VehicleCubeOBB.CubePoint touchPoint : touchPoints) {
            VehicleCubeOBB.CubeFace face = touchPoint.cubeFace();
            if (face == VehicleCubeOBB.CubeFace.LEFT || face == VehicleCubeOBB.CubeFace.RIGHT
                    || face == VehicleCubeOBB.CubeFace.FRONT || face == VehicleCubeOBB.CubeFace.BACK) {
                if (vehicleContactPoints.contains(touchPoint)) {
                    continue;
                }
                if (!climbBlocked && touchPoint.obbLocalPos().y < -physicsCube.getHeight() / 2 + physicsCube.spaceY) {
                    continue;
                }
                boolean lateral = face == VehicleCubeOBB.CubeFace.LEFT || face == VehicleCubeOBB.CubeFace.RIGHT;
                Vec3 normal = new Vec3(axes[lateral ? 0 : 2]).normalize();
                if (climbBlocked) {
                    normal = new Vec3(normal.x, 0, normal.z).normalize();
                }
                double normalVelocity = velocity.dot(normal);
                int direction = face == VehicleCubeOBB.CubeFace.LEFT || face == VehicleCubeOBB.CubeFace.FRONT ? -1 : 1;
                if (normalVelocity * direction < 0) {
                    velocity = climbBlocked ? velocity.subtract(normal.scale(normalVelocity))
                            : lateral ? VectorUtil.projectToPlane(velocity, axes, 1, 2)
                            : VectorUtil.projectToPlane(velocity, axes, 0, 1);
                    isStuck = true;
                    stuckTick += 1;
                    // 方块破坏
                    if (stuckTick == 10) {
                        if (physicsInfo.canDestroyBlock && AllConfigs.common.canDestroyBlock.get()) {
                            Level level = vehicle.level();
                            Vector3f faceNormal = face == VehicleCubeOBB.CubeFace.LEFT || face == VehicleCubeOBB.CubeFace.RIGHT ? axes[0] : axes[2];
                            boolean normalAlongX = Math.abs(faceNormal.x) >= Math.abs(faceNormal.z);
                            Set<BlockPos> blocksToDestroy = new HashSet<>();
                            for (VehicleCubeOBB.CubePoint cubePoint : vehicle.getMainCubeOBB().cubePointsByFace.get(face)) {
                                if (cubePoint.obbLocalPos().y > -physicsCube.getHeight() / 2 + vehicle.getMainCubeOBB().spaceY) {
                                    Vec3 pos = new Vec3(cubePoint.cachedWorldPos());
                                    BlockPos blockPos = BlockPos.containing(pos);
                                    for (int vertical = -1; vertical <= 1; vertical++) {
                                        for (int horizontal = -1; horizontal <= 1; horizontal++) {
                                            blocksToDestroy.add(blockPos.offset(normalAlongX ? 0 : horizontal, vertical, normalAlongX ? horizontal : 0));
                                        }
                                    }
                                }
                            }
                            for (BlockPos blockPos : blocksToDestroy) {
                                BlockState blockState = level.getBlockState(blockPos);
                                float hardness = blockState.getDestroySpeed(level, blockPos);
                                if (!blockState.isAir() && hardness >= 0 && hardness < 50.0F) {
                                    level.destroyBlock(blockPos, false, vehicle);
                                }
                            }
                        }
                        stuckTick -= 2;
                    }
                } else if (!climbBlocked) {
                    velocity = velocity.subtract(normal.scale(normalVelocity)).add(normal.scale(direction * bounce));
                }
            } else if (touchPoint.cubeFace() == VehicleCubeOBB.CubeFace.TOP || touchPoint.cubeFace() == VehicleCubeOBB.CubeFace.BOTTOM) {
                if (velocity.y > -0.1 && touchPoint.obbLocalPos().y < -physicsCube.obb().extents().y - 0.01) {
                    continue;
                }
                if (hasSuspension && face == VehicleCubeOBB.CubeFace.BOTTOM) {
                    bottomContacts.add(touchPoint);
                    continue;
                }
                Vec3 axesY = new Vec3(axes[1]).normalize();
                double d = velocity.dot(axesY);
                if (touchPoint.cubeFace() == VehicleCubeOBB.CubeFace.TOP) {
                    if (d > 0) {
                        velocity = VectorUtil.projectToPlane(velocity, axes, 0, 2);
                    } else {
                        velocity = velocity.subtract(axesY.scale(d)).add(axesY.scale(-bounce));
                    }
                } else {
                    if (d < 0) {
                        velocity = VectorUtil.projectToPlane(velocity, axes, 0, 2);
                    }
                    if (!hasSuspension && velocity.dot(axesY) < 0.1f) {
                        float offsetY = (float) (physicsCube().offset().y - physicsCube.height / 2);
                        Vec3 testPos = new Vec3(touchPoint.cachedWorldPos().add(0, 0.1f + offsetY, 0));
                        BlockPos testBlockPos = BlockPos.containing(testPos);
                        BlockState blockState = vehicle.level().getBlockState(testBlockPos);
                        if (blockState.isSolid()) {
                            if (!isHalfBlock(touchPoint.cubePointContext.blockState()) || testPos.y < testBlockPos.getY() + 0.55) {
                                double tilt = Math.acos(Mth.clamp(axesY.y, -1, 1));
                                double peakTilt = Math.toRadians(15);
                                double zeroTilt = Math.toRadians(30);
                                double tiltRatio = tilt <= peakTilt
                                        ? tilt / peakTilt
                                        : Math.max(0, (zeroTilt - tilt) / (zeroTilt - peakTilt));
                                double supportVelocity = Mth.lerp(tiltRatio, 0, 0.01);
                                velocity = velocity.add(axesY.scale(supportVelocity));
                            }
                        }
                    }
                }
            }
        }
        velocity = motionBySuspensionImpact(velocity, bottomContacts, axes);
        if (climbBlocked) {
            // 底面冲量可能带有水平分量，重新满足已接触墙面的速度约束。
            for (VehicleCubeOBB.CubePoint point : touchPoints) {
                if (vehicleContactPoints.contains(point)) {
                    continue;
                }
                Vec3 normal = switch (point.cubeFace()) {
                    case FRONT -> new Vec3(axes[2]).scale(-1);
                    case BACK -> new Vec3(axes[2]);
                    case LEFT -> new Vec3(axes[0]).scale(-1);
                    case RIGHT -> new Vec3(axes[0]);
                    default -> Vec3.ZERO;
                };
                normal = new Vec3(normal.x, 0, normal.z).normalize();
                double outwardSpeed = velocity.dot(normal);
                if (outwardSpeed < 0) {
                    velocity = velocity.subtract(normal.scale(outwardSpeed));
                }
            }
        }
        Vec3 testPos = new Vec3(physicsCube.obb().center());
        BlockPos testBlockPos = BlockPos.containing(testPos);
        BlockState blockState = vehicle.level().getBlockState(testBlockPos);
        if (!hasSuspension && blockState.isSolid()) {
            velocity = velocity.add(0, 0.1, 0);
        }
        if (!isStuck) {
            stuckTick = Math.max(stuckTick - 1, 0);
        }
        this.velocity.set((float) velocity.x, (float) velocity.y, (float) velocity.z);
        double velocityDiff = velocityO - velocity.length();
        if (velocityDiff > 0.5) {
            DamageSystem.impactHurt(velocityDiff, vehicle);
        }
    }

    private Vec3 motionBySuspensionImpact(Vec3 velocity, List<VehicleCubeOBB.CubePoint> contacts, Vector3f[] axes) {
        if (contacts.isEmpty() || physicsInfo.mass <= 0) {
            return velocity;
        }
        VehicleCubeOBB cube = physicsCube();
        double mass = physicsInfo.mass;
        double inverseMass = 1 / mass;
        Vec3 center = physicsInfo.center;
        double inversePitchInertia = lockCenterRot ? 0 : 12.0 / (mass * Math.max(0.01,
                cube.height * cube.height + cube.depth * cube.depth + 12 * (center.y * center.y + center.z * center.z)));
        double inverseRollInertia = lockCenterRot || lockZRot ? 0 : 12.0 / (mass * Math.max(0.01,
                cube.width * cube.width + cube.height * cube.height + 12 * (center.x * center.x + center.y * center.y)));
        Quaternionf yaw = new Quaternionf().rotateY((float) Math.toRadians(-vehicle.getYRot()));
        Quaternionf inverseYaw = new Quaternionf(yaw).invert();
        Vec3 centerWorld = getCenterOfMass();
        Vec3 normal = new Vec3(axes[1]).normalize();
        List<SuspensionImpactContact> constraints = new ArrayList<>();
        for (VehicleCubeOBB.CubePoint contact : new LinkedHashSet<>(contacts)) {
            Vec3 arm = new Vec3(contact.cachedWorldPos()).subtract(centerWorld);
            Vector3f torque = inverseYaw.transform(arm.cross(normal).toVector3f());
            double inverseEffectiveMass = inverseMass + torque.x * torque.x * inversePitchInertia
                    + torque.z * torque.z * inverseRollInertia;
            constraints.add(new SuspensionImpactContact(torque.x, torque.z, inverseEffectiveMass));
        }
        double pitchVelocity = inversePitchInertia == 0 ? 0 : suspensionPitchVelocity;
        double rollVelocity = inverseRollInertia == 0 ? 0 : suspensionRollVelocity;
        double[] impulses = new double[constraints.size()];
        // 约束接触点速度，允许重心随车身绕支点回落。
        for (int iteration = 0; iteration < 32; iteration++) {
            double maxChange = 0;
            for (int i = 0; i < constraints.size(); i++) {
                SuspensionImpactContact contact = constraints.get(i);
                double pointVelocity = velocity.dot(normal)
                        + contact.pitchArm * pitchVelocity + contact.rollArm * rollVelocity;
                double nextImpulse = Math.max(0, impulses[i] - pointVelocity / contact.inverseEffectiveMass);
                double deltaImpulse = nextImpulse - impulses[i];
                impulses[i] = nextImpulse;
                velocity = velocity.add(normal.scale(deltaImpulse * inverseMass));
                pitchVelocity += deltaImpulse * contact.pitchArm * inversePitchInertia;
                rollVelocity += deltaImpulse * contact.rollArm * inverseRollInertia;
                maxChange = Math.max(maxChange, Math.abs(deltaImpulse) * contact.inverseEffectiveMass);
            }
            if (maxChange < 1.0E-7) {
                break;
            }
        }
        suspensionPitchVelocity = (float) pitchVelocity;
        suspensionRollVelocity = (float) rollVelocity;
        return velocity;
    }

    /**
     * 摩擦力影响
     */
    private void decelerationByFriction(List<VehicleCubeOBB.CubePoint> touchPoints) {
        Vec3 velocity = new Vec3(this.velocity);
        if (!touchPoints.isEmpty() || suspensionGrounded) {
            // 悬挂接地摩擦只作用于水平运动，竖直回弹由弹簧和接触约束处理。
            Vec3 frictionVelocity = suspensionGrounded ? new Vec3(velocity.x, 0, velocity.z) : velocity;
            Vec3 slowed = frictionVelocity.normalize()
                    .scale(Math.max(0, frictionVelocity.length() - PhysicsHelper.accelerationPerTick(physicsInfo.friction, physicsInfo.mass)));
            velocity = suspensionGrounded ? new Vec3(slowed.x, velocity.y, slowed.z) : slowed;
        }
        this.velocity.set((float) velocity.x, (float) velocity.y, (float) velocity.z);
    }

    /**
     * 浮力影响
     */
    private Vec3 motionByBuoyancy() {
        submergedRatio = 0;
        if (physicsInfo.density <= 0 || physicsInfo.mass <= 0) {
            return Vec3.ZERO;
        }
        Vec3 velocity = new Vec3(this.velocity);
        VehicleCubeOBB physicsCube = physicsCube();
        OBB obb = physicsCube.obb();
        Vector3f[] axes = obb.getAxes();
        Vector3f extents = obb.extents();
        int samplesX = buoyancySamples(extents.x * 2);
        int samplesY = 16;
        int samplesZ = buoyancySamples(extents.z * 2);
        int sampleCount = samplesX * samplesY * samplesZ;
        double physicsCubeVolume = physicsCube.volume();
        double sampleVolume = physicsCubeVolume / sampleCount;
        double displacedVolume = 0;
        double displacedFluidMass = 0;
        for (int x = 0; x < samplesX; x++) {
            float localX = sampleCoordinate(extents.x, x, samplesX);
            for (int y = 0; y < samplesY; y++) {
                float localY = sampleCoordinate(extents.y, y, samplesY);
                for (int z = 0; z < samplesZ; z++) {
                    float localZ = sampleCoordinate(extents.z, z, samplesZ);
                    Vector3f worldPos = obb.localToWorld(new Vector3f(localX, localY, localZ), axes);
                    BlockPos blockPos = BlockPos.containing(worldPos.x, worldPos.y, worldPos.z);
                    FluidState fluidState = vehicle.level().getFluidState(blockPos);
                    if (fluidState.isEmpty()) {
                        continue;
                    }
                    double fluidSurface = blockPos.getY() + fluidState.getHeight(vehicle.level(), blockPos);
                    if (worldPos.y < fluidSurface) {
                        int fluidDensity = fluidState.getFluidType().getDensity(fluidState, vehicle.level(), blockPos);
                        if (fluidDensity > 0) {
                            displacedVolume += sampleVolume;
                            displacedFluidMass += sampleVolume * fluidDensity;
                        }
                    }
                }
            }
        }
        double buoyancyForce = G * physicsInfo.mass * displacedFluidMass / (physicsInfo.density * 1000.0 * physicsCubeVolume);
        double buoyancyAcceleration = buoyancyForce / physicsInfo.mass;
        submergedRatio = (float) (displacedVolume / physicsCubeVolume);
        double damping = Mth.clamp(1 - physicsInfo.liquidDamping * submergedRatio, 0, 1);
        velocity = velocity.scale(damping);
        if (buoyancyForce > 0) {
            velocity = new Vec3(velocity.x, velocity.y * 0.5, velocity.z);
        }
        velocity = velocity.add(0, buoyancyAcceleration, 0);
        this.velocity.set((float) velocity.x, (float) velocity.y, (float) velocity.z);
        return new Vec3(0, buoyancyForce, 0);
    }

    private void rightInLiquid() {
        if (submergedRatio <= 0) {
            return;
        }
        // 浅吃水时仍保留足够的回正能力，同时在离水时连续衰减到零。
        float liquidInfluence = Mth.sqrt(Mth.clamp(submergedRatio, 0, 1));
        rotV *= 1 - liquidInfluence * 0.2f;
        if (rotV < 0.001f) {
            rotV = 0;
        }
        vehicle.setXRot(rightInLiquid(vehicle.getXRot(), liquidInfluence));
        if (lockZRot) {
            vehicle.setZRot(0);
        } else {
            vehicle.setZRot(rightInLiquid(vehicle.getZRot(), liquidInfluence));
        }
    }

    /**
     * 后坐力影响
     */
    public void recoil(WeaponUnit weaponUnit, float recoil) {
        Vec3 fireDirection = weaponUnit.worldVec();
        if (vehicle.hasSuspension()) {
            if (!lockCenterRot) {
                Vector3f localFireDirection = new Quaternionf()
                        .rotateY(Math.toRadians(vehicle.getYRot()))
                        .transform(fireDirection.normalize().toVector3f());
                float impulse = 0.01f * recoil;
                suspensionPitchVelocity -= localFireDirection.z * impulse;
                if (!lockZRot) {
                    suspensionRollVelocity += localFireDirection.x * impulse;
                }
            }
        } else {
            OBB obb = vehicle.getMainCubeOBB().obb();
            Vector3f[] axes = obb.getAxes();
            Vector3f forceStartLocal = obb.worldToLocal(weaponUnit.worldPivotPosition().add(fireDirection.scale(5)).toVector3f(), axes);
            Vector3f forcePointLocal = obb.worldToLocal(weaponUnit.worldPivotPosition().toVector3f(), axes);
            // 后坐力方向在局部坐标系下的矢量
            Vector3f force = new Vector3f(forcePointLocal).sub(forceStartLocal);
            getPlaneXY(force, forcePointLocal);
            Optional<Vector3f> forceEdge = obb.clip(new Vector3f(obb.center()).add(fireDirection.normalize().scale(16).toVector3f().negate()), obb.center());
            if (forceEdge.isPresent()) {
                Vector3f forceEdgeLocal = obb.worldToLocal(forceEdge.get(), axes);
                Vec3 axis = new Vec3(-force.z, 0, force.x).add(forceEdgeLocal.x, 0, forceEdgeLocal.z);
                localRotAxisStart = axis.normalize().scale(5).toVector3f();
                localRotAxisEnd = axis.normalize().scale(-5).toVector3f();
                checkDirection(forcePointLocal);
                rotV = 0.05f * recoil;
                Vec3 lastPosition = vehicle.position();
                rot(axes);
                vehicle.setPos(lastPosition);
                // 后坐力产生推移
                force = force.normalize();
                double motion = force.dot(new Vector3f(0, 0, 1)) * 0.03 * recoil;
                vehicle.setDeltaMovement(vehicle.getDeltaMovement().add(new Vec3(axes[2]).scale(motion)));
            }
        }
    }

    public boolean climb(List<VehicleCubeOBB.CubePoint> touchPoints) {
        touchPoints = touchPoints.stream().filter(point -> !vehicleContactPoints.contains(point)).toList();
        VehicleCubeOBB physicsCube = physicsCube();
        Vec3 bottomPosition = vehicle.relativeRotPos(physicsCube.offset()
                .add(new Vec3(0, physicsCube.bottomPoint.obbLocalPos().y + 0.01f, 0))
                .add(vehicle.position()), true);
        if (vehicle.hasSuspension() && !canSuspensionClimb(touchPoints, bottomPosition.y)) {
            return false;
        }
        List<VehicleCubeOBB.CubePoint> climbPoints = new ArrayList<>(touchPoints.stream().filter(p ->
                        p.cubeFace() == VehicleCubeOBB.CubeFace.FRONT
                                || p.cubeFace() == VehicleCubeOBB.CubeFace.BOTTOM
                                || p.cubeFace() == VehicleCubeOBB.CubeFace.BACK)
                .toList());
        if (climbPoints.isEmpty()) {
            return false;
        }
        if (vehicle.getXRot() < -15) {
            return false;
        }
        // 自动爬高
        DoubleSummaryStatistics stats = climbPoints.stream()
                .mapToDouble(p -> p.obbLocalPos().y)
                .summaryStatistics();
        double yRange = stats.getMax() - stats.getMin();
        double liftLimit = physicsCube.spaceY * 2;
        if (yRange >= liftLimit - BODY_CONTACT_SLOP || (yRange < physicsCube.spaceY - BODY_CONTACT_SLOP
                && !(Math.abs(vehicle.getXRot()) < 0.1 && Math.abs(vehicle.getZRot()) < 0.1))) {
            return false;
        }
        climbPoints.sort(Comparator.comparingDouble(p -> -p.cubePointContext.blockPos().y));
        VehicleCubeOBB.CubePoint liftPoint = climbPoints.get(0);
        double liftHeight = liftPoint.cubePointContext.blockPos().y + (isHalfBlock(liftPoint.cubePointContext.blockState()) ? 0.5f : 1f);
        double toLift = Mth.clamp(liftHeight - bottomPosition.y, 0, vehicle.maxUpStep());
        if (toLift <= 0) {
            return false;
        }
        vehicle.setPos(vehicle.position().x, vehicle.position().y + toLift, vehicle.position().z);
        return true;
    }

    private boolean canSuspensionClimb(List<VehicleCubeOBB.CubePoint> touchPoints, double bottomHeight) {
        List<VehicleCubeOBB.CubePoint> obstacles = touchPoints.stream()
                .filter(point -> point.cubeFace() != VehicleCubeOBB.CubeFace.TOP)
                .filter(point -> point.cubePointContext.blockPos().y
                        + (isHalfBlock(point.cubePointContext.blockState()) ? 0.5 : 1) > bottomHeight + BODY_CONTACT_SLOP)
                .toList();
        if (obstacles.isEmpty()) {
            // 只有离开障碍并重新接地才能开始下一次爬升，悬空时不能刷新额度。
            if (suspensionGrounded) {
                climbStartHeight = Double.NaN;
            }
            return true;
        }
        if (Double.isNaN(climbStartHeight)) {
            climbStartHeight = vehicle.getSuspensionUnits().stream()
                    .filter(SuspensionUnit::isGrounded)
                    .flatMap(unit -> unit.getSupportContacts().stream())
                    .mapToDouble(contact -> contact.position().y)
                    .min().orElse(Double.NaN);
            // 尚未接地时不能以悬空的底盘高度作为新的起跳平面。
            if (Double.isNaN(climbStartHeight)) {
                return false;
            }
        }
        double ceiling = climbStartHeight + Math.max(1, vehicle.getStructureLength() * 0.6);
        Map<BlockPos, Boolean> climbable = new HashMap<>();
        for (VehicleCubeOBB.CubePoint point : obstacles) {
            BlockPos pos = BlockPos.containing(point.cubePointContext.blockPos());
            if (climbable.computeIfAbsent(pos, block -> hasReachableClimbSurface(block, ceiling))) {
                continue;
            }
            climbBlocked = true;
            return false;
        }
        return true;
    }

    private boolean hasReachableClimbSurface(BlockPos pos, double ceiling) {
        double surface = pos.getY();
        // 沿同一列合并相连的实体块；墙体中间每一层的顶面都不是开放高平面。
        double clearance = Math.max(BODY_CONTACT_SKIN, physicsCube().height);
        AABB column = new AABB(pos.getX(), surface, pos.getZ(), pos.getX() + 1,
                Math.max(surface + 1, ceiling + clearance), pos.getZ() + 1);
        List<AABB> boxes = getBlockCollisionBoxes(column);
        boxes.sort(Comparator.comparingDouble(box -> box.minY));
        for (AABB box : boxes) {
            if (box.minY > surface + BODY_CONTACT_SLOP) {
                return box.minY >= surface + clearance - BODY_CONTACT_SLOP;
            }
            surface = Math.max(surface, box.maxY);
            if (surface > ceiling + BODY_CONTACT_SLOP) {
                return false;
            }
        }
        return surface > pos.getY();
    }

    private BodyContacts collectContacts(Vector3f[] axes, boolean withBodySupports) {
        vehicleContactPoints.clear();
        // 接触方块的采样点
        List<VehicleCubeOBB.CubePoint> touchPoints = new ArrayList<>();
        Map<BlockPos, List<AABB>> shapes = new HashMap<>();
        // 车体大OBB的表面采样点
        for (VehicleCubeOBB.CubePoint point : physicsCube().cubePoints()) {
            Vec3 position = new Vec3(point.worldPos(axes));
            BlockPos blockPos = BlockPos.containing(position);
            List<AABB> boxes = shapes.computeIfAbsent(blockPos, this::blockCollisionBoxes);
            for (AABB box : boxes) {
                if (box.contains(position)) {
                    point.cubePointContext.setBlockPos(Vec3.atBottomCenterOf(blockPos));
                    point.cubePointContext.setBlockState(vehicle.level().getBlockState(blockPos));
                    touchPoints.add(point);
                    break;
                }
            }
        }
        // 地形支撑
        Map<VehicleCubeOBB.CubePoint, Double> supports = withBodySupports
                ? collectBodySupports(axes) : new LinkedHashMap<>();
        // 载具支撑
        List<OBB> vehicleBoxes = getVehicleCollisionBoxes(OBB.toAABB(List.of(physicsCube().obb()))
                .inflate(BODY_CONTACT_RECOVERY));
        for (VehicleCubeOBB.CubePoint point : physicsCube().cubePoints()) {
            if (touchPoints.contains(point) || supports.containsKey(point)) {
                continue;
            }
            Vec3 position = new Vec3(point.cachedWorldPos());
            Optional<Vec3> support = bodySupportAlignment(point, axes) > 0
                    ? findVehicleSupport(vehicleBoxes, position.add(0, BODY_CONTACT_RECOVERY, 0),
                            position.add(0, -BODY_CONTACT_SKIN, 0)) : Optional.empty();
            if (support.isEmpty() && vehicleBoxes.stream().noneMatch(box -> box.contains(position))) {
                continue;
            }
            point.cubePointContext.setBlockPos(support.orElse(position).add(0, -1, 0));
            point.cubePointContext.setBlockState(null);
            touchPoints.add(point);
            vehicleContactPoints.add(point);
            if (withBodySupports && support.isPresent()) {
                supports.put(point, support.get().y);
            }
        }
        for (VehicleCubeOBB.CubePoint point : supports.keySet()) {
            if (!touchPoints.contains(point)) {
                touchPoints.add(point);
            }
        }
        Set<VehicleCubeOBB.CubePoint> blockContacts = new HashSet<>(touchPoints);
        // 兼容支撑
        MinecraftForge.EVENT_BUS.post(new VehicleCollectCollisionEvent(vehicle, touchPoints));
        supports.keySet().retainAll(touchPoints);
        if (withBodySupports) {
            for (VehicleCubeOBB.CubePoint point : touchPoints) {
                if (!blockContacts.contains(point) && bodySupportAlignment(point, axes) > 0) {
                    supports.putIfAbsent(point, (double) point.cachedWorldPos().y);
                }
            }
        }

        // 调试
//        touchPoints.forEach(p -> DebugUtil.particle(level(), new Vec3(p.worldPos(axes)), p.cubeFace()));
//        touchPoints.forEach(p -> {
//            BlockPos blockPos = p.cubePointContext.blockPos();
//            DebugUtil.particle(level(), new Vec3(blockPos.getX(), blockPos.getY(), blockPos.getZ()), p.cubeFace());
//        });

        return new BodyContacts(touchPoints, supports);
    }

    private Map<VehicleCubeOBB.CubePoint, Double> collectBodySupports(Vector3f[] axes) {
        Map<VehicleCubeOBB.CubePoint, Double> supports = new LinkedHashMap<>();
        Map<BlockPos, List<AABB>> shapes = new HashMap<>();
        for (VehicleCubeOBB.CubePoint point : physicsCube().cubePoints()) {
            if (bodySupportAlignment(point, axes) <= 0) {
                continue;
            }
            Vec3 position = new Vec3(point.worldPos(axes));
            BlockPos blockPos = BlockPos.containing(position.x, position.y - BODY_CONTACT_SKIN, position.z);
            List<AABB> boxes = shapes.computeIfAbsent(blockPos, this::blockCollisionBoxes);
            double supportHeight = Double.NEGATIVE_INFINITY;
            for (AABB box : boxes) {
                double gap = position.y - box.maxY;
                if (position.x >= box.minX && position.x <= box.maxX
                        && position.z >= box.minZ && position.z <= box.maxZ
                        && gap <= BODY_CONTACT_SKIN
                        && (gap >= -BODY_CONTACT_RECOVERY || point.cubeFace() == VehicleCubeOBB.CubeFace.BOTTOM)) {
                    supportHeight = Math.max(supportHeight, box.maxY);
                }
            }
            if (supportHeight != Double.NEGATIVE_INFINITY
                    && isOpenSupportSurface(new Vec3(position.x, supportHeight, position.z))) {
                supports.put(point, supportHeight);
                point.cubePointContext.setBlockPos(Vec3.atBottomCenterOf(blockPos));
                point.cubePointContext.setBlockState(vehicle.level().getBlockState(blockPos));
            }
        }
        return supports;
    }

    public List<AABB> getBlockCollisionBoxes(AABB bounds) {
        List<AABB> boxes = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ),
                BlockPos.containing(bounds.maxX, bounds.maxY, bounds.maxZ))) {
            for (AABB box : blockCollisionBoxes(pos)) {
                if (box.intersects(bounds)) {
                    boxes.add(box);
                }
            }
        }
        return boxes;
    }

    private List<OBB> getVehicleCollisionBoxes(AABB bounds) {
        if (!vehicle.collision || vehicle.noPhysics) {
            return List.of();
        }
        List<OBB> boxes = new ArrayList<>();
        for (AbstractVehicle other : vehicle.level().getEntitiesOfClass(AbstractVehicle.class, bounds,
                other -> other != vehicle && other.collision && !other.noPhysics
                        && !vehicle.isPassengerOfSameVehicle(other))) {
            other.getMainCubeOBB().update(other);
            boxes.add(other.getMainCubeOBB().obb());
        }
        return boxes;
    }

    public Optional<Vec3> findVehicleSupport(Vec3 start, Vec3 end) {
        return findVehicleSupport(getVehicleCollisionBoxes(new AABB(start, end).inflate(BODY_CONTACT_SKIN)), start, end);
    }

    private Optional<Vec3> findVehicleSupport(List<OBB> boxes, Vec3 start, Vec3 end) {
        Vec3 nearest = null;
        for (OBB box : boxes) {
            if (box.embeddingDepth(start) > BODY_CONTACT_SLOP) {
                continue;
            }
            Optional<Vector3f> hit = box.clip(start.toVector3f(), end.toVector3f());
            if (hit.isEmpty()) {
                continue;
            }
            Vec3 position = new Vec3(hit.get());
            int face = box.embeddingFace(position);
            Vec3 normal = new Vec3(box.getAxes()[Math.abs(face) - 1]).scale(face > 0 ? 1 : -1);
            if (normal.y <= 0.25 || end.subtract(start).dot(normal) >= 0 || !isOpenSupportSurface(position)) {
                continue;
            }
            if (nearest == null || start.distanceToSqr(position) < start.distanceToSqr(nearest)) {
                nearest = position;
            }
        }
        return Optional.ofNullable(nearest);
    }

    /**
     * 在高速端调用，按质量转移动能并平滑调整低速端的速度方向。
     */
    public void transferEnergyTo(PhysicsEngine follower, Vec3 pullDirection, double transferRate, double directionResponse) {
        double leaderMass = physicsInfo.mass;
        double followerMass = follower.physicsInfo.mass;
        if (!(leaderMass > 0) || !(followerMass > 0)
                || !Double.isFinite(leaderMass) || !Double.isFinite(followerMass)) {
            return;
        }
        Vec3 leaderVelocity = vehicle.getDeltaMovement();
        Vec3 followerVelocity = follower.vehicle.getDeltaMovement();
        double leaderSpeedSquared = leaderVelocity.lengthSqr();
        double followerSpeedSquared = followerVelocity.lengthSqr();
        double leaderSpeed = Math.sqrt(leaderSpeedSquared);
        double followerSpeed = Math.sqrt(followerSpeedSquared);
        if (leaderSpeed <= SPEED_EPSILON) {
            return;
        }

        // E = 1/2 m v²；每 tick 转移达到共同速率所需动能的固定比例。
        // 两端等量扣减/增加动能，速度平方差单调收敛，不会跨过平衡点。
        // 这里统一使用格/tick，换算到 m/s 的公共系数会在能量转移中抵消。
        double totalMass = leaderMass + followerMass;
        double speedSquaredGap = leaderSpeedSquared - followerSpeedSquared;
        double transferredEnergy = 0.5 * leaderMass * (followerMass / totalMass)
                * speedSquaredGap * transferRate;
        double nextLeaderSpeed = Math.sqrt(Math.max(0, leaderSpeedSquared - 2 * transferredEnergy / leaderMass));
        double nextFollowerSpeed = Math.sqrt(Math.max(0, followerSpeedSquared + 2 * transferredEnergy / followerMass));
        if (leaderSpeed - followerSpeed <= SPEED_EPSILON) {
            // 消除浮点尾差，确保最终速率一致；方向仍逐 tick 归正。
            double commonSpeed = Math.sqrt((leaderMass / totalMass) * leaderSpeedSquared
                    + (followerMass / totalMass) * followerSpeedSquared);
            nextLeaderSpeed = commonSpeed;
            nextFollowerSpeed = commonSpeed;
        }
        setVelocity(leaderVelocity.scale(nextLeaderSpeed / leaderSpeed));
        follower.setVelocity(steerDirection(followerVelocity, pullDirection, directionResponse).scale(nextFollowerSpeed));
    }

    /**
     * 按逆质量分摊两刚体之间的超长位置修正。
     */
    public void limitDistance(PhysicsEngine other, Vec3 anchor, Vec3 otherAnchor, double maxDistance) {
        Vec3 separation = otherAnchor.subtract(anchor);
        double distance = separation.length();
        if (distance <= maxDistance || distance <= SPEED_EPSILON) {
            return;
        }
        double mass = physicsInfo.mass;
        double otherMass = other.physicsInfo.mass;
        if (!(mass > 0) || !(otherMass > 0) || !Double.isFinite(mass) || !Double.isFinite(otherMass)) {
            return;
        }
        Vec3 correction = separation.scale((distance - maxDistance) / distance);
        applyPositionCorrection(correction.scale(otherMass / (mass + otherMass)));
        other.applyPositionCorrection(correction.scale(-mass / (mass + otherMass)));
    }

    /**
     * 有限速率地将刚体方向转向目标方向。
     */
    public void alignDirection(Vec3 pivot, Vec3 direction, Vec3 targetDirection, double response, double maxStep) {
        if (direction.lengthSqr() < 1.0E-4 || targetDirection.lengthSqr() < 1.0E-4) {
            return;
        }
        Vec3 from = direction.normalize();
        Vec3 to = targetDirection.normalize();
        double angle = Math.acos(Mth.clamp(from.dot(to), -1, 1));
        if (angle <= SPEED_EPSILON) {
            return;
        }
        Quaternionf target = new Quaternionf().rotationTo(from.toVector3f(), to.toVector3f());
        rotateAround(pivot, new Quaternionf().slerp(target, (float) Math.min(response, maxStep / angle)));
    }

    /**
     * 绕刚体上的世界坐标支点旋转，补偿实体原点偏移。
     */
    public void rotateAround(Vec3 pivot, Quaternionf step) {
        Quaternionf rotation = vehicle.rotYXZ();
        Vec3 origin = vehicle.position().add(vehicle.centerOffset);
        Vector3f localPivot = new Quaternionf(rotation).invert().transform(pivot.subtract(origin).toVector3f());
        float oldXRot = vehicle.getXRot();
        float oldYRot = vehicle.getYRot();
        float oldZRot = vehicle.getZRot();
        if (!setRotation(new Quaternionf(step).mul(rotation))) {
            return;
        }
        vehicle.afterVehicleRot(Mth.wrapDegrees(vehicle.getXRot() - oldXRot),
                Mth.wrapDegrees(vehicle.getYRot() - oldYRot), Mth.wrapDegrees(vehicle.getZRot() - oldZRot));
        Vec3 rotatedPivot = origin.add(new Vec3(vehicle.rotYXZ().transform(localPivot)));
        applyPositionCorrection(pivot.subtract(rotatedPivot));
    }

    private void centerRot(Vector3f center, Vector3f[] axes) {
        if (lockCenterRot) {
            return;
        }
        if (localRotAxisVec != null) {
            localRotAxisStart = new Vector3f(center).sub(localRotAxisVec);
            localRotAxisEnd = new Vector3f(center).add(localRotAxisVec);
            // 目前仅考虑重力
            Vector3f g = new Vector3f(0, -1, 0);
            // 重力方向在局部坐标系下的矢量
            float gx = g.dot(axes[0]);
            float gy = g.dot(axes[1]);
            float gz = g.dot(axes[2]);
            Vector3f gLocal = new Vector3f(gx, gy, gz);
            getPlaneXY(gLocal, center);
            checkDirection(center);
            rot(axes);
        }
    }

    private void rotLoss(Vector2f rotToPoint) {
        if (localRotAxisStartO == null || localRotAxisEndO == null) {
            return;
        }
        Vector2f v1 = getPlaneXY(null, localRotAxisStart);
        Vector2f v2 = getPlaneXY(null, localRotAxisEnd);
        Vector2f v3 = getPlaneXY(null, localRotAxisStartO);
        Vector2f v4 = getPlaneXY(null, localRotAxisEndO);
        Function<Vector2f[], Vector2f> getPerp = (arr) -> {
            Vector2f a = arr[0], b = arr[1];
            Vector2f ab = new Vector2f(b).sub(a);
            Vector2f ap = new Vector2f(rotToPoint).sub(a);
            float t = ap.dot(ab) / ab.dot(ab);
            Vector2f proj = new Vector2f(a).add(new Vector2f(ab).mul(t));
            return new Vector2f(rotToPoint).sub(proj);
        };
        Vector2f perp1 = getPerp.apply(new Vector2f[]{v1, v2});
        Vector2f perp2 = getPerp.apply(new Vector2f[]{v3, v4});
        if (perp1.lengthSquared() == 0 || perp2.lengthSquared() == 0) {
            return;
        }
        float cosTheta = perp1.dot(perp2) / (perp1.length() * perp2.length());
        cosTheta = Math.max(-1.0f, Math.min(1.0f, cosTheta));
        float angleRad = Math.acos(cosTheta);
        if (angleRad > Math.PI / 2) {
            rotV *= 0.5f;
        }
    }

    private void rot(Vector3f[] axes) {
        if (localRotAxisStart == null || localRotAxisEnd == null || rotV == 0) {
            return;
        }
        var physicsCube = vehicle.getMainCubeOBB();
        Vec3 pRot = new Vec3(rotateAroundAxis(vehicle.position().toVector3f(),
                physicsCube.obb().localToWorld(localRotAxisStart, axes),
                physicsCube.obb().localToWorld(localRotAxisEnd, axes),
                rotV));
        Quaternionf q = new Quaternionf(stepRot).mul(vehicle.rotYXZ());
        if (!setRotation(q)) {
            return;
        }
        rotTick = 10;
        vehicle.setPos(pRot);
    }

    private static Vec3 steerDirection(Vec3 velocity, Vec3 targetDirection, double directionResponse) {
        double speed = velocity.length();
        if (speed <= SPEED_EPSILON) {
            return targetDirection;
        }
        Vec3 direction = velocity.scale(1 / speed);
        double angle = Math.acos(Mth.clamp(direction.dot(targetDirection), -1, 1));
        if (angle <= SPEED_EPSILON) {
            return targetDirection;
        }
        Vec3 axis = direction.cross(targetDirection);
        if (axis.lengthSqr() < 1.0E-12) {
            // 完全反向时也能平滑转向；水平运动优先绕世界 Y 轴转动。
            axis = new Vec3(0, 1, 0).subtract(direction.scale(direction.y));
            if (axis.lengthSqr() < 1.0E-12) {
                axis = new Vec3(1, 0, 0);
            }
        }
        // Vec3.normalize 会把长度小于 1e-4 的向量归零，小夹角需显式归一化。
        axis = axis.scale(1 / axis.length());
        double step = angle * directionResponse;
        // 绕垂直于速度的轴旋转，只改变朝向，不额外增加或损失动能。
        return direction.scale(Math.cos(step)).add(axis.cross(direction).scale(Math.sin(step))).normalize();
    }

    /**
     * 计算长方体绕任意空间轴（过枢轴点）的转动惯量
     * 使用主轴转动惯量 + 平行轴定理
     */
    private float computeMomentOfInertia(Vector3f axisStart, Vector3f axisEnd, VehicleCubeOBB cube, float mass, Vector3f com) {
        float w = (float) cube.getWidth();
        float h = (float) cube.getHeight();
        float d = (float) cube.getDepth();
        float Ix = mass / 12f * (h * h + d * d);
        float Iy = mass / 12f * (w * w + d * d);
        float Iz = mass / 12f * (w * w + h * h);
        Vector3f axis = new Vector3f(axisEnd).sub(axisStart).normalize();
        float I_center = Ix * axis.x * axis.x + Iy * axis.y * axis.y + Iz * axis.z * axis.z;
        Vector3f r = new Vector3f(com).sub(axisStart);
        Vector3f projOnAxis = new Vector3f(axis).mul(r.dot(axis));
        Vector3f perp = new Vector3f(r).sub(projOnAxis);
        float distSq = perp.lengthSquared();
        return I_center + mass * distSq;
    }

    /**
     * 计算合力绕旋转轴的力矩标量
     * τ = (r × F) · axis
     */
    private float computeTorque(Vector3f axisStart, Vector3f axisEnd, Vector3f com, Vector3f netForceLocal) {
        Vector3f r = new Vector3f(com).sub(axisStart);
        Vector3f torqueVec = r.cross(netForceLocal);
        Vector3f axis = new Vector3f(axisEnd).sub(axisStart).normalize();
        return Math.abs(torqueVec.dot(axis));
    }

    private Vector3f rotateAroundAxis(Vector3f point, Vector3f a, Vector3f b, float radians) {
        Vector3f axis = new Vector3f(b).sub(a).normalize();
        stepRot = new Quaternionf().fromAxisAngleRad(axis, radians);
        Vector3f relative = new Vector3f(point).sub(a);
        stepRot.transform(relative);
        return relative.add(a);
    }

    private Vector2f getPlaneXY(Vector3f support, Vector3f point) {
        if (support != null) {
            // 以planeSupport为法向量的平面有planeU, planeV两轴
            planeSupport = new Vector3f(support).normalize();
            Vector3f tmp = new Vector3f(1, 0, 0);
            planeU = tmp.cross(planeSupport).normalize();
            planeV = new Vector3f(planeSupport).cross(planeU).normalize();
        }
        // 求point在平面上的投影点x, y
        Vector3f projected = new Vector3f(point).sub(new Vector3f(planeSupport).mul(point.dot(planeSupport)));
        return new Vector2f(projected.dot(planeU), projected.dot(planeV));
    }

    /**
     * 应用位置约束，并立即刷新碰撞盒和乘员位置。
     */
    public void applyPositionCorrection(Vec3 movement) {
        vehicle.setPos(vehicle.position().add(movement));
        vehicle.updateOBBs();
        vehicle.setBoundingBox(vehicle.makeBoundingBox());
        vehicle.getPassengers().forEach(vehicle::positionRider);
        vehicle.hasImpulse = true;
    }

    private List<AABB> blockCollisionBoxes(BlockPos pos) {
        BlockState state = vehicle.level().getBlockState(pos);
        if (!state.isSolid()) {
            return List.of();
        }
        double height = isHalfBlock(state) ? 0.5 : 1;
        return List.of(new AABB(pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1, pos.getY() + height, pos.getZ() + 1));
    }

    private static double bodySupportAlignment(VehicleCubeOBB.CubePoint point, Vector3f[] axes) {
        return switch (point.cubeFace()) {
            case LEFT -> -axes[0].y;
            case RIGHT -> axes[0].y;
            case FRONT -> -axes[2].y;
            case BACK -> axes[2].y;
            case TOP -> -axes[1].y;
            case BOTTOM -> axes[1].y;
        };
    }

    private void checkDirection(Vector3f localRotToPoint) {
        // 左手系下，拇指为rotAxisStart -> rotAxisEnd方向，四指为重力旋转方向
        Vector3f v1 = new Vector3f(localRotAxisStart).sub(localRotToPoint);
        Vector3f v2 = new Vector3f(localRotAxisEnd).sub(localRotToPoint);
        if (v1.cross(v2).dot(planeSupport) < 0) {
            Vector3f tmp = localRotAxisEnd;
            localRotAxisEnd = localRotAxisStart;
            localRotAxisStart = tmp;
        }
    }

    private static float rightInLiquid(float angle, float liquidInfluence) {
        // 大倾角快速回正，接近水平时减速；最小步长避免回正末段拖尾。
        float rightingStep = Mth.clamp(Math.abs(Mth.wrapDegrees(angle)) * 0.2f, 1.5f, 6.0f) * liquidInfluence;
        return Mth.approachDegrees(angle, 0, rightingStep);
    }

    public boolean isOpenSupportSurface(Vec3 position) {
        return getBlockCollisionBoxes(new AABB(position.x - BODY_CONTACT_SLOP,
                position.y + BODY_CONTACT_SLOP, position.z - BODY_CONTACT_SLOP,
                position.x + BODY_CONTACT_SLOP, position.y + BODY_CONTACT_SKIN,
                position.z + BODY_CONTACT_SLOP)).isEmpty();
    }

    public static boolean isHalfBlock(BlockState blockState) {
        if (blockState == null) {
            return false;
        }
        if (blockState.getBlock() instanceof SlabBlock) {
            return blockState.getValue(SlabBlock.TYPE) != SlabType.DOUBLE;
        }
        return blockState.hasProperty(BlockStateProperties.HALF);
    }

    private static int buoyancySamples(float size) {
        return Mth.clamp(Mth.ceil(size * 2), 1, 8);
    }

    private static float sampleCoordinate(float extent, int sample, int sampleCount) {
        return -extent + extent * 2 * (sample + 0.5f) / sampleCount;
    }

    public VehicleCubeOBB physicsCube() {
        return vehicle.getMainCubeOBB();
    }

    private Vec3 getLocalCenterOfMass() {
        VehicleCubeOBB cube = physicsCube();
        return cube.offset().add(new Vec3(cube.selfRot().transform(physicsInfo.center.toVector3f())));
    }

    public Vec3 getCenterOfMass() {
        return vehicle.position().add(vehicle.centerOffset).add(new Vec3(vehicle.rotYXZ()
                .transform(getLocalCenterOfMass().subtract(vehicle.centerOffset).toVector3f())));
    }

    /**
     * 同步外部约束修改后的实体速度和物理速度。
     */
    public void setVelocity(Vec3 velocity) {
        vehicle.setDeltaMovement(velocity);
        this.velocity.set(velocity.toVector3f());
        vehicle.hasImpulse = true;
    }

    private boolean setRotation(Quaternionf rotation) {
        Vector3f euler = rotation.getEulerAnglesYXZ(new Vector3f());
        if (!Float.isFinite(euler.x) || !Float.isFinite(euler.y) || !Float.isFinite(euler.z)) {
            return false;
        }
        vehicle.setXRot((float) Math.toDegrees(euler.x));
        vehicle.setYRot(-(float) Math.toDegrees(euler.y));
        vehicle.setZRot(lockZRot ? 0 : (float) Math.toDegrees(euler.z));
        return true;
    }

}
