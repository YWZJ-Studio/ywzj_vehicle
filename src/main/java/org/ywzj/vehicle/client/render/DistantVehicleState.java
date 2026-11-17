package org.ywzj.vehicle.client.render;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.network.message.ServerBroadcastEntities.BroadcastEntity;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

public final class DistantVehicleState {

    private final Deque<TimedSnapshot> snapshots = new ArrayDeque<>();
    private AbstractVehicle vehicle;
    private Vec3 correction = Vec3.ZERO;
    private Quaternionf rotationCorrection = new Quaternionf();
    private double correctionStart;
    private double correctionDuration = 1;
    private Pose trackedPose;
    private double trackedTime = Double.NEGATIVE_INFINITY;
    private boolean renderingRemote;
    private boolean creationFailed;
    private CompoundTag appliedData;
    private record TimedSnapshot(long tick, Snapshot snapshot) {}
    public record Pose(Vec3 position, Vec3 velocity, Quaternionf rotation) {}
    public record Snapshot(int id, UUID uuid, ResourceLocation type, ResourceLocation vehicleId,
                           ResourceLocation displayId, Vec3 position, Vec3 velocity,
                           Quaternionf rotation, float radius, boolean destroyed, CompoundTag data) {

        public static Snapshot of(BroadcastEntity entity) {
            var render = entity.renderData();
            return new Snapshot(entity.entityId(), render.uuid(), entity.entityType(), render.vehicleId(),
                    render.displayId(), entity.entityPosition(), entity.entityVelocity(),
                    render.rotation(), render.radius(), entity.data() != null && entity.data().getBoolean("Destroyed"), render.data());
        }

    }

    public Snapshot latest() {
        return snapshots.getLast().snapshot;
    }

    public void accept(long tick, Snapshot snapshot, double oldTime, double newTime, double now, int interval) {
        Pose previous = snapshots.isEmpty() ? null : sample(oldTime, now, interval);
        boolean teleport = false;
        if (!snapshots.isEmpty()) {
            TimedSnapshot last = snapshots.getLast();
            if (tick <= last.tick) {
                return;
            }
            double span = tick - last.tick;
            double expectedTravel = Math.max(last.snapshot.velocity().length(), snapshot.velocity().length()) * span;
            teleport = last.snapshot.position().distanceTo(snapshot.position()) > Math.max(128, expectedTravel * 2 + 32);
        }
        if (teleport) {
            snapshots.clear();
            trackedPose = null;
        }
        snapshots.addLast(new TimedSnapshot(tick, snapshot));
        while (snapshots.size() > 8) {
            snapshots.removeFirst();
        }
        correction = Vec3.ZERO;
        rotationCorrection.identity();
        if (previous != null && !teleport) {
            correctFrom(previous, sampleRaw(newTime, interval), now, interval);
        }
    }

    public void rememberTracked(AbstractVehicle tracked, float partialTick, double now) {
        Vec3 position = new Vec3(Mth.lerp(partialTick, tracked.xOld, tracked.getX()),
                Mth.lerp(partialTick, tracked.yOld, tracked.getY()), Mth.lerp(partialTick, tracked.zOld, tracked.getZ()));
        trackedPose = new Pose(position, tracked.getDeltaMovement(), tracked.rotYXZ(partialTick));
        trackedTime = now;
        renderingRemote = false;
    }

    public Pose renderPose(double time, double now, int interval) {
        if (!renderingRemote && trackedPose != null && now - trackedTime < 2) {
            correctFrom(trackedPose, sampleRaw(time, interval), now, Math.min(interval, 5));
        }
        renderingRemote = true;
        return sample(time, now, interval);
    }

    private void correctFrom(Pose previous, Pose next, double now, int interval) {
        correction = previous.position.subtract(next.position);
        rotationCorrection = new Quaternionf(previous.rotation).mul(new Quaternionf(next.rotation).conjugate());
        correctionStart = now;
        correctionDuration = Math.max(1, Math.min(interval, 10));
    }

    private Pose sample(double time, double now, int interval) {
        Pose pose = sampleRaw(time, interval);
        float progress = Mth.clamp((float) ((now - correctionStart) / correctionDuration), 0, 1);
        float weight = 1 - progress * progress * (3 - 2 * progress);
        Quaternionf rotation = new Quaternionf().slerp(rotationCorrection, weight).mul(pose.rotation);
        return new Pose(pose.position.add(correction.scale(weight)), pose.velocity, rotation);
    }

    private Pose sampleRaw(double time, int interval) {
        TimedSnapshot previous = snapshots.getFirst();
        if (time <= previous.tick) {
            return pose(previous.snapshot);
        }
        for (TimedSnapshot next : snapshots) {
            if (next.tick <= previous.tick) {
                continue;
            }
            if (time <= next.tick) {
                double span = next.tick - previous.tick;
                double t = Mth.clamp((time - previous.tick) / span, 0, 1);
                Vec3 delta = next.snapshot.position().subtract(previous.snapshot.position());
                Vec3 startTangent = tangent(previous.snapshot.velocity(), delta, span);
                Vec3 endTangent = tangent(next.snapshot.velocity(), delta, span);
                // 使用速度约束三次插值，避免低频同步时匀速航行忽快忽慢。
                double t2 = t * t;
                double t3 = t2 * t;
                Vec3 position = previous.snapshot.position()
                        .add(delta.scale(3 * t2 - 2 * t3))
                        .add(startTangent.scale(t3 - 2 * t2 + t))
                        .add(endTangent.scale(t3 - t2));
                Vec3 velocity = delta.scale(6 * t - 6 * t2)
                        .add(startTangent.scale(3 * t2 - 4 * t + 1))
                        .add(endTangent.scale(3 * t2 - 2 * t)).scale(1 / span);
                return new Pose(position, velocity,
                        new Quaternionf(previous.snapshot.rotation()).slerp(next.snapshot.rotation(), (float) t));
            }
            previous = next;
        }
        // 迟包时只做短时、逐渐减速的外推，避免断流后载具一直飞走。
        double limit = Math.min(interval, 10);
        double elapsed = Mth.clamp(time - previous.tick, 0, limit);
        double travel = elapsed - elapsed * elapsed / (2 * limit);
        Snapshot last = previous.snapshot;
        return new Pose(last.position().add(last.velocity().scale(travel)),
                last.velocity().scale(1 - elapsed / limit), new Quaternionf(last.rotation()));
    }

    private static Vec3 tangent(Vec3 velocity, Vec3 delta, double span) {
        Vec3 tangent = velocity.scale(span);
        double maxLength = delta.length() * 2;
        if (tangent.dot(delta) <= 0 || maxLength < 1.0E-6) {
            return Vec3.ZERO;
        }
        return tangent.length() > maxLength ? tangent.normalize().scale(maxLength) : tangent;
    }

    private static Pose pose(Snapshot snapshot) {
        return new Pose(snapshot.position(), snapshot.velocity(), new Quaternionf(snapshot.rotation()));
    }

    public AbstractVehicle vehicle(ClientLevel level) {
        Snapshot snapshot = latest();
        if (vehicle == null && !creationFailed) {
            creationFailed = true;
            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(snapshot.type());
            Entity entity = type == null ? null : type.create(level);
            if (entity instanceof AbstractVehicle distant) {
                distant.remote = true;
                distant.renderBeyondRenderDistance = true;
                distant.setId(snapshot.id());
                distant.setUUID(snapshot.uuid());
                distant.setVehicleId(snapshot.vehicleId());
                distant.setDisplayId(snapshot.displayId());
                distant.setPos(snapshot.position());
                distant.initData();
                if (!distant.isRemoved()) {
                    distant.initDisplayData();
                    vehicle = distant;
                }
            }
        }
        if (vehicle != null) {
            vehicle.getEntityData().set(AbstractVehicle.DESTROYED, snapshot.destroyed());
            if (!vehicle.getDisplayId().equals(snapshot.displayId()) || vehicle.getVehicleModelInstance() == null) {
                vehicle.setDisplayId(snapshot.displayId());
                vehicle.initDisplayData();
            }
            if (snapshot.data() != null && !snapshot.data().equals(appliedData)) {
                CompoundTag changedData = snapshot.data().copy();
                if (appliedData != null) {
                    for (String key : new String[]{"PartUnits", "DecorationUnits"}) {
                        if (snapshot.data().getCompound(key).equals(appliedData.getCompound(key))) {
                            changedData.remove(key);
                        }
                    }
                }
                vehicle.deserializeDistantRenderData(changedData);
                appliedData = snapshot.data();
            }
        }
        return vehicle;
    }

    public void applyPose(AbstractVehicle vehicle, Pose pose, int ticks) {
        vehicle.setPos(pose.position);
        vehicle.xOld = vehicle.xo = pose.position.x;
        vehicle.yOld = vehicle.yo = pose.position.y;
        vehicle.zOld = vehicle.zo = pose.position.z;
        vehicle.setDeltaMovement(pose.velocity);
        Vector3f angles = pose.rotation.getEulerAnglesYXZ(new Vector3f());
        vehicle.setXRot((float) Math.toDegrees(angles.x));
        vehicle.setYRot((float) -Math.toDegrees(angles.y));
        vehicle.setZRot((float) Math.toDegrees(angles.z));
        vehicle.xRotO = vehicle.getXRot();
        vehicle.yRotO = vehicle.getYRot();
        vehicle.zRotO = vehicle.getZRot();
        vehicle.tickCount = ticks;
    }

}
