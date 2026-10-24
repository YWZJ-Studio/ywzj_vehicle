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

import java.util.*;

public class TrackUnit extends PartUnit<TrackUnitData> {

    private static final double MAX_FRAME_SECONDS = 0.25;
    private static final double CURVE_SAMPLES_PER_METER = 16.0;
    private final List<TrackPath> tracks;
    private long lastRenderNanos;

    public TrackUnit(int index, AbstractVehicle vehicle, TrackUnitData data) {
        super(index, vehicle, data);
        this.tracks = createPaths(data.getTrackDefinitions());
    }

    @Override
    public void combineAndInit(Map<String, PartUnit<?>> partUnitsView, AbstractVehicle vehicle) {
        super.combineAndInit(partUnitsView, vehicle);
        for (TrackPath track : tracks) {
            List<SuspensionUnit<?>> suspensions = new ArrayList<>();
            for (String suspensionUnitId : track.suspensionUnitIds()) {
                PartUnit<?> partUnit = partUnitsView.get(suspensionUnitId);
                if (partUnit instanceof SuspensionUnit<?> suspension) {
                    suspensions.add(suspension);
                }
            }
            track.setSuspensions(suspensions);
        }
    }

    private static List<TrackPath> createPaths(List<TrackUnitData.TrackDefinition> definitions) {
        return definitions.stream()
                .map(definition -> new TrackPath(definition.points(), definition.suspensionUnitIds()))
                .filter(TrackPath::isValid)
                .toList();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, float partialTick) {
        if (isDetached() || tracks.isEmpty()) {
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
            track.updateFloatingPoints(partialTick);
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
    private record OrderedPoint(double distance, int order, Vec3 point) {}
    private record SuspensionPoint(SuspensionUnit<?> suspension, Vec3 reference, double distance) {}

    private static final class TrackPath {

        private final List<Vec3> basePoints;
        private final List<String> suspensionUnitIds;
        private final List<PathSegment> baseSegments;
        private final double[] basePointDistances;
        private final double[] baseSegmentDistances;
        private final double baseLength;
        private final Vec3 planeNormal;
        private List<PathSegment> segments;
        private double length;
        private int linkCount;
        private double spacing;
        private double lateralOffset;
        private double phase;
        private List<SuspensionPoint> suspensionPoints = List.of();
        private double floatingMinZ;
        private double floatingMaxZ;
        private List<Vec3> floatingPoints = List.of();
        private boolean floatingPathActive;

        private TrackPath(List<Vec3> points, List<String> suspensionUnitIds) {
            this.basePoints = List.copyOf(points);
            this.suspensionUnitIds = List.copyOf(suspensionUnitIds);
            this.baseSegments = buildSegments(basePoints);
            this.basePointDistances = buildPointDistances(basePoints);
            this.baseSegmentDistances = buildSegmentDistances(baseSegments);
            this.baseLength = baseSegments.stream().mapToDouble(PathSegment::length).sum();
            this.planeNormal = buildPlaneNormal(basePoints);
            rebuild(basePoints);
        }

        private static Vec3 buildPlaneNormal(List<Vec3> points) {
            if (points.size() < 3) {
                return new Vec3(1, 0, 0);
            }
            Vec3 origin = points.get(0);
            Vec3 normal = Vec3.ZERO;
            for (int index = 1; index < points.size() - 1; index++) {
                normal = normal.add(points.get(index).subtract(origin)
                        .cross(points.get(index + 1).subtract(origin)));
            }
            // 由绑定路径的绕行方向固定内侧，避免下垂或悬挂起伏时翻面。
            return normal.lengthSqr() > 1.0E-12 ? normal.normalize() : new Vec3(1, 0, 0);
        }

        private void setSuspensions(List<SuspensionUnit<?>> suspensions) {
            List<SuspensionUnit<?>> validSuspensions = suspensions.stream()
                    .filter(suspension -> Double.isFinite(suspension.getTrackCenterZ()))
                    .toList();
            SuspensionUnit<?> positiveEndSuspension = validSuspensions.stream()
                    .max(Comparator.comparingDouble(SuspensionUnit::getTrackCenterZ)).orElse(null);
            SuspensionUnit<?> negativeEndSuspension = validSuspensions.stream()
                    .min(Comparator.comparingDouble(SuspensionUnit::getTrackCenterZ)).orElse(null);
            List<SuspensionPoint> points = new ArrayList<>();
            for (SuspensionUnit<?> suspension : validSuspensions) {
                Vec3 reference = suspension.getTrackFloatingReference(lateralOffset);
                if (reference == null) {
                    continue;
                }
                double distance = projectDistance(new Vec3(reference.x, trackAxisYAtZ(reference.z), reference.z));
                points.add(new SuspensionPoint(suspension, reference, distance));
                int zDirection = suspension == positiveEndSuspension ? 1 : suspension == negativeEndSuspension ? -1 : 0;
                if (zDirection != 0) {
                    // 首尾保留底面中心点，并额外接管前方或后方最近的下行段结构点。
                    int nearestIndex = nearestLowerPointIndex(reference, zDirection);
                    if (nearestIndex >= 0) {
                        points.add(new SuspensionPoint(suspension, basePoints.get(nearestIndex),
                                basePointDistances[nearestIndex]));
                    }
                }
            }
            suspensionPoints = List.copyOf(points);
            floatingMinZ = points.stream().mapToDouble(point -> point.reference().z).min().orElse(0);
            floatingMaxZ = points.stream().mapToDouble(point -> point.reference().z).max().orElse(0);
        }

        private int nearestLowerPointIndex(Vec3 reference, int zDirection) {
            int nearestIndex = -1;
            double nearestDistance = Double.POSITIVE_INFINITY;
            for (int index = 0; index < basePoints.size(); index++) {
                Vec3 point = basePoints.get(index);
                if ((point.z - reference.z) * zDirection <= 1.0E-6
                        || point.y > trackAxisYAtZ(point.z) + 1.0E-6) {
                    continue;
                }
                double distance = point.distanceToSqr(reference);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestIndex = index;
                }
            }
            return nearestIndex;
        }

        private List<String> suspensionUnitIds() {
            return suspensionUnitIds;
        }

        @OnlyIn(Dist.CLIENT)
        private void updateFloatingPoints(float partialTick) {
            if (baseLength <= 0) {
                return;
            }
            List<Vec3> floatingPoints = new ArrayList<>();
            List<OrderedPoint> ordered = new ArrayList<>(basePoints.size() + suspensionPoints.size());
            for (SuspensionPoint binding : suspensionPoints) {
                Vec3 point = binding.suspension().getTrackFloatingPoint(partialTick, binding.reference());
                if (point != null) {
                    ordered.add(new OrderedPoint(binding.distance(), basePoints.size() + floatingPoints.size(), point));
                    floatingPoints.add(point);
                }
            }
            if (floatingPoints.isEmpty()) {
                if (floatingPathActive) {
                    rebuild(basePoints);
                    this.floatingPoints = List.of();
                    floatingPathActive = false;
                }
                return;
            }
            if (samePoints(this.floatingPoints, floatingPoints)) {
                return;
            }
            this.floatingPoints = List.copyOf(floatingPoints);
            floatingPathActive = true;
            for (int index = 0; index < basePoints.size(); index++) {
                Vec3 point = basePoints.get(index);
                // 下行段由悬挂点接管；同一 Z 范围内的上方回程路径仍由结构模型定义。
                if (point.z >= floatingMinZ - 1.0E-6 && point.z <= floatingMaxZ + 1.0E-6
                        && point.y <= trackAxisYAtZ(point.z) + 1.0E-6) {
                    continue;
                }
                ordered.add(new OrderedPoint(basePointDistances[index], index, point));
            }
            ordered.sort(Comparator.comparingDouble(OrderedPoint::distance)
                    .thenComparingInt(OrderedPoint::order));
            List<Vec3> points = new ArrayList<>(ordered.stream().map(OrderedPoint::point).toList());
            if (basePoints.get(0).distanceToSqr(basePoints.get(basePoints.size() - 1)) <= 1.0E-12
                    && points.get(0).distanceToSqr(points.get(points.size() - 1)) > 1.0E-12) {
                points.add(points.get(0));
            }
            rebuild(points);
        }

        private double trackAxisYAtZ(double z) {
            double bestY = Double.POSITIVE_INFINITY;
            double bestError = Double.POSITIVE_INFINITY;
            for (PathSegment segment : baseSegments) {
                double zDelta = segment.end().z - segment.start().z;
                if (Math.abs(zDelta) <= 1.0E-6) {
                    double error = Math.abs(z - segment.start().z);
                    if (error < bestError + 1.0E-6) {
                        if (error < bestError - 1.0E-6) {
                            bestY = Double.POSITIVE_INFINITY;
                            bestError = error;
                        }
                        bestY = Math.min(bestY, Math.min(segment.start().y, segment.end().y));
                    }
                    continue;
                }
                double progress = Math.max(0, Math.min(1, (z - segment.start().z) / zDelta));
                Vec3 point = segment.start().lerp(segment.end(), progress);
                double error = Math.abs(z - point.z);
                if (error < bestError - 1.0E-6) {
                    bestError = error;
                    bestY = point.y;
                } else if (error < bestError + 1.0E-6) {
                    bestY = Math.min(bestY, point.y);
                }
            }
            if (Double.isFinite(bestY)) {
                return bestY;
            }
            return basePoints.stream()
                    .min(Comparator.comparingDouble(point -> Math.abs(point.z - z)))
                    .map(Vec3::y)
                    .orElse(0.0);
        }

        private void rebuild(List<Vec3> points) {
            double phaseRatio = length > 1.0E-6 ? phase / length : 0;
            List<Vec3> smoothedPoints = smoothPoints(points);
            List<PathSegment> builtSegments = new ArrayList<>();
            double totalLength = 0;
            double weightedX = 0;
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
                }
            }
            segments = List.copyOf(builtSegments);
            length = totalLength;
            lateralOffset = totalLength > 0 ? weightedX / totalLength : 0;
            phase = totalLength > 0 ? positiveModulo(phaseRatio * totalLength, totalLength) : 0;
        }

        private static List<PathSegment> buildSegments(List<Vec3> points) {
            List<PathSegment> result = new ArrayList<>();
            for (int index = 0; index < points.size() - 1; index++) {
                Vec3 start = points.get(index);
                Vec3 end = points.get(index + 1);
                Vec3 offset = end.subtract(start);
                double segmentLength = offset.length();
                if (segmentLength > 1.0E-6) {
                    result.add(new PathSegment(start, end, offset.scale(1 / segmentLength), segmentLength));
                }
            }
            return List.copyOf(result);
        }

        private static double[] buildPointDistances(List<Vec3> points) {
            double[] distances = new double[points.size()];
            for (int index = 1; index < points.size(); index++) {
                distances[index] = distances[index - 1] + points.get(index - 1).distanceTo(points.get(index));
            }
            return distances;
        }

        private static double[] buildSegmentDistances(List<PathSegment> segments) {
            double[] distances = new double[segments.size()];
            double distance = 0;
            for (int index = 0; index < segments.size(); index++) {
                distances[index] = distance;
                distance += segments.get(index).length();
            }
            return distances;
        }

        private static boolean samePoints(List<Vec3> previous, List<Vec3> current) {
            if (previous.size() != current.size()) {
                return false;
            }
            for (int index = 0; index < previous.size(); index++) {
                if (previous.get(index).distanceToSqr(current.get(index)) > 1.0E-8) {
                    return false;
                }
            }
            return true;
        }

        private double projectDistance(Vec3 point) {
            double bestDistance = 0;
            double bestError = Double.POSITIVE_INFINITY;
            for (int index = 0; index < baseSegments.size(); index++) {
                PathSegment segment = baseSegments.get(index);
                Vec3 offset = point.subtract(segment.start());
                double progress = Math.max(0, Math.min(segment.length(), offset.dot(segment.tangent())))
                        / segment.length();
                Vec3 projected = segment.start().lerp(segment.end(), progress);
                double error = projected.distanceToSqr(point);
                if (error < bestError) {
                    bestError = error;
                    bestDistance = baseSegmentDistances[index] + progress * segment.length();
                }
            }
            return bestDistance;
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
            boolean closed = keyPoints.get(0).distanceToSqr(keyPoints.get(keyPoints.size() - 1)) <= 1.0E-12;
            for (int index = 0; index < keyPoints.size(); index++) {
                tangents.add(tangentAt(keyPoints, index, closed));
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

        private static Vec3 tangentAt(List<Vec3> points, int index, boolean closed) {
            if (!closed && index == 0) {
                return points.get(1).subtract(points.get(0));
            }
            if (!closed && index == points.size() - 1) {
                return points.get(index).subtract(points.get(index - 1));
            }
            Vec3 previousPoint = points.get(index == 0 ? points.size() - 2 : index - 1);
            Vec3 nextPoint = points.get(index == points.size() - 1 ? 1 : index + 1);
            Vec3 previous = points.get(index).subtract(previousPoint);
            Vec3 next = nextPoint.subtract(points.get(index));
            Vec3 direction = nextPoint.subtract(previousPoint);
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
            Vec3 inwardNormal = planeNormal.cross(tangent);
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
            linkCount = Math.max(1, (int) Math.round(length / moduleLength));
            spacing = length / linkCount;
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
