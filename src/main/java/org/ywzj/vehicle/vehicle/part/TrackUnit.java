package org.ywzj.vehicle.vehicle.part;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.client.resource.vehicle.TrackConfig;
import org.ywzj.vehicle.client.resource.vehicle.TrackedVehicleDisplay;
import org.ywzj.vehicle.client.resource.vehicle.VehicleBedrockModel;
import org.ywzj.vehicle.custom.part.data.TrackUnitData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.vehicle.TrackedVehicle;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

public class TrackUnit extends PartUnit<TrackUnitData> {

    private static final double MAX_FRAME_SECONDS = 0.25;
    private static final double CURVE_SAMPLES_PER_METER = 16.0;
    private final List<TrackPath> tracks;
    private long lastRenderNanos;

    public TrackUnit(int index, AbstractVehicle vehicle, TrackUnitData data) {
        super(index, vehicle, data);
        this.tracks = createPaths(data.getTracks());
        this.partCubeOBBs = new ArrayList<>();
    }

    private static List<TrackPath> createPaths(List<List<Vec3>> rawTracks) {
        return rawTracks.stream()
                .map(TrackPath::new)
                .filter(TrackPath::isValid)
                .toList();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (tracks.isEmpty()) {
            return;
        }
        TrackedVehicleDisplay display = getTrackedVehicleDisplay();
        if (display == null) {
            return;
        }
        TrackConfig config = display.getTrackConfig();
        VehicleBedrockModel model = display.getTrackModel();
        var texture = display.getTrackTexture();
        if (config == null || config.moduleLength <= 0 || model == null || texture == null) {
            return;
        }
        for (TrackPath track : tracks) {
            track.setModuleLength(config.moduleLength);
        }
        advanceAnimation();
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityCutout(texture));
        int modelLight = vehicle.isDestroyed() ? 64 : packedLight;
        for (TrackPath track : tracks) {
            renderTrack(track, model, poseStack, buffer, modelLight);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private TrackedVehicleDisplay getTrackedVehicleDisplay() {
        var displayOptional = ClientAssetsManager.INSTANCE.getVehicleDisplay(vehicle.getDisplayId());
        if (displayOptional.isPresent() && displayOptional.get() instanceof TrackedVehicleDisplay trackedVehicleDisplay) {
            return trackedVehicleDisplay;
        }
        return null;
    }

    public OptionalDouble getFirstLeftTrackLateralOffset() {
        return firstTrackLateralOffset(true);
    }

    public OptionalDouble getFirstRightTrackLateralOffset() {
        return firstTrackLateralOffset(false);
    }

    private OptionalDouble firstTrackLateralOffset(boolean left) {
        return tracks.stream()
                .mapToDouble(TrackPath::lateralOffset)
                .filter(offset -> left ? offset > 0 : offset < 0)
                .findFirst();
    }

    @OnlyIn(Dist.CLIENT)
    private void advanceAnimation() {
        long now = System.nanoTime();
        if (lastRenderNanos == 0) {
            lastRenderNanos = now;
            return;
        }
        double deltaSeconds = Math.min((now - lastRenderNanos) / 1_000_000_000.0, MAX_FRAME_SECONDS);
        lastRenderNanos = now;
        if (!(vehicle instanceof TrackedVehicle trackedVehicle)) {
            return;
        }
        double forwardSpeed = trackedVehicle.getForwardSpeed() * 20.0;
        double turnSpeed = trackedVehicle.getTurnSpeed();
        for (TrackPath track : tracks) {
            double linearSpeed = forwardSpeed + turnSpeed * track.lateralOffset();
            track.advance(linearSpeed * deltaSeconds);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void renderTrack(TrackPath track, VehicleBedrockModel model, PoseStack poseStack,
                                    VertexConsumer buffer, int packedLight) {
        for (int index = 0; index < track.linkCount(); index++) {
            PathSample sample = track.sample(track.phase() + index * track.spacing());
            poseStack.pushPose();
            poseStack.translate(sample.position().x, sample.position().y, sample.position().z);
            poseStack.mulPose(trackRotation(sample));
            model.renderToBuffer(poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
    }

    private static Quaternionf trackRotation(PathSample sample) {
        Vec3 forward = sample.tangent().scale(-1);
        Vec3 up = sample.inwardNormal();
        Vec3 right = up.cross(forward).normalize();
        Matrix3f rotation = new Matrix3f();
        rotation.setColumn(0, right.toVector3f());
        rotation.setColumn(1, up.toVector3f());
        rotation.setColumn(2, forward.toVector3f());
        return new Quaternionf().setFromNormalized(rotation);
    }

    private record PathSample(Vec3 position, Vec3 tangent, Vec3 inwardNormal) {}
    private record PathSegment(Vec3 start, Vec3 end, Vec3 tangent, double length) {}
    private record PositionSample(Vec3 position, Vec3 tangent) {}

    private static final class TrackPath {

        private final List<PathSegment> segments;
        private final double length;
        private final Vec3 center;
        private int linkCount;
        private double spacing;
        private final double lateralOffset;
        private double phase;

        private TrackPath(List<Vec3> points) {
            List<Vec3> smoothedPoints = smoothPoints(points);
            List<PathSegment> builtSegments = new ArrayList<>();
            double totalLength = 0;
            double weightedX = 0;
            Vec3 weightedCenter = Vec3.ZERO;
            if (smoothedPoints.size() >= 2) {
                for (int index = 0; index < smoothedPoints.size() - 1; index++) {
                    Vec3 start = smoothedPoints.get(index);
                    Vec3 end = smoothedPoints.get(index + 1);
                    Vec3 offset = end.subtract(start);
                    double segmentLength = offset.length();
                    if (segmentLength <= 1.0E-6) {
                        continue;
                    }
                    builtSegments.add(new PathSegment(start, end, offset.scale(1 / segmentLength), segmentLength));
                    totalLength += segmentLength;
                    weightedX += (start.x + end.x) * 0.5 * segmentLength;
                    weightedCenter = weightedCenter.add(start.add(end).scale(0.5 * segmentLength));
                }
            }
            this.segments = List.copyOf(builtSegments);
            this.length = totalLength;
            this.center = totalLength > 0 ? weightedCenter.scale(1 / totalLength) : Vec3.ZERO;
            this.lateralOffset = totalLength > 0 ? weightedX / totalLength : 0;
        }

        private static List<Vec3> smoothPoints(List<Vec3> points) {
            List<Vec3> keyPoints = new ArrayList<>();
            for (Vec3 point : points) {
                if (keyPoints.isEmpty() || point.distanceToSqr(keyPoints.get(keyPoints.size() - 1)) > 1.0E-12) {
                    keyPoints.add(point);
                }
            }
            if (keyPoints.size() < 3) {
                return List.copyOf(keyPoints);
            }

            List<Vec3> tangents = new ArrayList<>(keyPoints.size());
            for (int index = 0; index < keyPoints.size(); index++) {
                tangents.add(tangentAt(keyPoints, index));
            }

            List<Vec3> result = new ArrayList<>();
            result.add(keyPoints.get(0));
            for (int index = 0; index < keyPoints.size() - 1; index++) {
                Vec3 start = keyPoints.get(index);
                Vec3 end = keyPoints.get(index + 1);
                int subdivisions = Math.max(4,
                        (int) Math.ceil(start.distanceTo(end) * CURVE_SAMPLES_PER_METER));
                for (int step = 1; step <= subdivisions; step++) {
                    double progress = (double) step / subdivisions;
                    result.add(hermite(start, end, tangents.get(index), tangents.get(index + 1), progress));
                }
            }
            return List.copyOf(result);
        }

        private static Vec3 tangentAt(List<Vec3> points, int index) {
            if (index == 0) {
                return points.get(1).subtract(points.get(0));
            }
            if (index == points.size() - 1) {
                return points.get(index).subtract(points.get(index - 1));
            }
            Vec3 previous = points.get(index).subtract(points.get(index - 1));
            Vec3 next = points.get(index + 1).subtract(points.get(index));
            Vec3 direction = points.get(index + 1).subtract(points.get(index - 1));
            if (direction.lengthSqr() <= 1.0E-12) {
                direction = next;
            }
            return direction.normalize().scale(Math.min(previous.length(), next.length()));
        }

        private static Vec3 hermite(Vec3 start, Vec3 end, Vec3 startTangent, Vec3 endTangent,
                                    double progress) {
            double progressSqr = progress * progress;
            double progressCubed = progressSqr * progress;
            double startFactor = 2 * progressCubed - 3 * progressSqr + 1;
            double startTangentFactor = progressCubed - 2 * progressSqr + progress;
            double endFactor = -2 * progressCubed + 3 * progressSqr;
            double endTangentFactor = progressCubed - progressSqr;
            return start.scale(startFactor)
                    .add(startTangent.scale(startTangentFactor))
                    .add(end.scale(endFactor))
                    .add(endTangent.scale(endTangentFactor));
        }

        private boolean isValid() {
            return !segments.isEmpty() && length > 0;
        }

        private PathSample sample(double distance) {
            PositionSample center = positionAt(distance);
            Vec3 previous = positionAt(distance - spacing * 0.5).position();
            Vec3 next = positionAt(distance + spacing * 0.5).position();
            Vec3 tangent = next.subtract(previous);
            if (tangent.lengthSqr() <= 1.0E-8) {
                tangent = center.tangent();
            } else {
                tangent = tangent.normalize();
            }
            Vec3 inwardNormal = this.center.subtract(center.position());
            inwardNormal = inwardNormal.subtract(tangent.scale(inwardNormal.dot(tangent)));
            if (inwardNormal.lengthSqr() <= 1.0E-8) {
                Vec3 fallback = Math.abs(tangent.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(0, 0, 1);
                inwardNormal = fallback.subtract(tangent.scale(fallback.dot(tangent)));
            }
            return new PathSample(center.position(), tangent, inwardNormal.normalize());
        }

        private PositionSample positionAt(double distance) {
            double remaining = positiveModulo(distance, length);
            for (PathSegment segment : segments) {
                if (remaining < segment.length()) {
                    double progress = remaining / segment.length();
                    return new PositionSample(segment.start().lerp(segment.end(), progress), segment.tangent());
                }
                remaining -= segment.length();
            }
            PathSegment last = segments.get(segments.size() - 1);
            return new PositionSample(last.end(), last.tangent());
        }

        private void advance(double distance) {
            phase = positiveModulo(phase + distance, length);
        }

        private void setModuleLength(double moduleLength) {
            int count = Math.max(1, (int) Math.round(length / moduleLength));
            if (count != linkCount) {
                linkCount = count;
                spacing = length / linkCount;
            }
        }

        private static double positiveModulo(double value, double modulus) {
            double result = value % modulus;
            return result < 0 ? result + modulus : result;
        }

        private int linkCount() {
            return linkCount;
        }

        private double spacing() {
            return spacing;
        }

        private double lateralOffset() {
            return lateralOffset;
        }

        private double phase() {
            return phase;
        }

    }

}
