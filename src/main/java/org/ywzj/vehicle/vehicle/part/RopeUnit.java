package org.ywzj.vehicle.vehicle.part;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.api.custom.sync.SyncDataSerializers;
import org.ywzj.vehicle.custom.part.data.RopeUnitData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RopeUnit extends PartUnit<RopeUnitData> {

    private static final ResourceLocation ROPE_TEXTURE = YwzjVehicle.resourceLocation("minecraft:textures/block/chain.png");
    private static final float ROPE_WIDTH = 2.5f / 16f;
    private static final float CHAIN_U_STEP = 3f / 16f;
    private static final double PART_LENGTH = 1;
    private final List<RopeNode> ropeNodes = new ArrayList<>();
    private float length;
    private boolean autoDeploy = true;
    private RopeNode fixedRopeNode;

    public RopeUnit(int index, AbstractVehicle vehicle, RopeUnitData data) {
        super(index, vehicle, data);
        this.partCubeOBBs = new ArrayList<>();
        this.syncData.define(SyncDataSerializers.VEC3, this::setPivotOffset, this::getPivotOffset, this.pivotOffset);
        this.syncData.define(SyncDataSerializers.FLOAT, this::applySyncedLength, this::getLength, 0f);
        ensureFixedNode();
    }

    @Override
    public void tick() {
        updateFixedNode();
        if (!vehicle.level().isClientSide() && autoDeploy) {
            RopeNode lastNode = ropeNodes.get(ropeNodes.size() - 1);
            boolean airBelow = vehicle.level().getBlockState(
                    BlockPos.containing(lastNode.pos.relative(Direction.DOWN, 0.5))
            ).isAir();
            if (airBelow && length < data.getMaxLength()) {
                applyLength(Math.min(data.getMaxLength(), length + (float) PART_LENGTH));
            } else {
                autoDeploy = false;
            }
        }
        tickPhysics();
        super.tick();
    }

    public void deploy() {
        autoDeploy = true;
    }

    public void retract() {
        autoDeploy = false;
        applyLength(0);
    }

    public void setLength(float length) {
        autoDeploy = false;
        applyLength(length);
    }

    private void applySyncedLength(float length) {
        applyLength(length);
    }

    private void applyLength(float length) {
        this.length = Mth.clamp(length, 0, data.getMaxLength());
        ensureFixedNode();
        adjustNodeCount(this.length);
    }

    public float getLength() {
        return length;
    }

    public float getMaxLength() {
        return data.getMaxLength();
    }

    @Override
    public void setPivotOffset(Vec3 offset) {
        super.setPivotOffset(offset == null ? Vec3.ZERO : offset);
        if (fixedRopeNode != null) {
            updateFixedNode();
        }
    }

    public Vec3 getAnchorPosition() {
        return worldPosition(getPivotOffset());
    }

    public Vec3 getEndPosition() {
        return ropeNodes.size() <= 1 ? getAnchorPosition() : ropeNodes.get(ropeNodes.size() - 1).pos;
    }

    public List<RopeNode> getRopeNodes() {
        return Collections.unmodifiableList(ropeNodes);
    }

    private void ensureFixedNode() {
        if (fixedRopeNode == null) {
            fixedRopeNode = new RopeNode(getAnchorPosition());
            fixedRopeNode.fixed = true;
            ropeNodes.add(fixedRopeNode);
        }
    }

    private void updateFixedNode() {
        ensureFixedNode();
        fixedRopeNode.lastPos = fixedRopeNode.pos;
        fixedRopeNode.pos = getAnchorPosition();
    }

    private void adjustNodeCount(float length) {
        int targetNodeCount = (int) Math.ceil(length / PART_LENGTH) + 1;
        while (ropeNodes.size() > targetNodeCount) {
            ropeNodes.remove(ropeNodes.size() - 1);
        }
        while (ropeNodes.size() < targetNodeCount) {
            RopeNode lastNode = ropeNodes.get(ropeNodes.size() - 1);
            Vec3 lastPosition = lastNode.fixed ? getAnchorPosition() : lastNode.pos;
            ropeNodes.add(new RopeNode(lastPosition.add(0, -PART_LENGTH, 0)));
        }
    }

    private void tickPhysics() {
        double gravity = -0.05;
        double friction = 0.98;
        for (RopeNode node : ropeNodes) {
            if (node.fixed) {
                continue;
            }
            Vec3 velocity = node.pos.subtract(node.lastPos).scale(friction);
            node.lastPos = node.pos;
            node.pos = node.pos.add(velocity).add(0, gravity, 0);
        }
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < ropeNodes.size() - 1; j++) {
                RopeNode n1 = ropeNodes.get(j);
                RopeNode n2 = ropeNodes.get(j + 1);
                double currentDistance = n1.pos.distanceTo(n2.pos);
                if (currentDistance <= 0) {
                    continue;
                }
                double error = currentDistance - PART_LENGTH;
                Vec3 direction = n1.pos.subtract(n2.pos).normalize();
                Vec3 correction = direction.scale(error * 0.5);
                if (!n1.fixed) {
                    n1.pos = n1.pos.subtract(correction);
                }
                if (!n2.fixed) {
                    n2.pos = n2.pos.add(correction);
                }
            }
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (ropeNodes.size() < 2) {
            return;
        }
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
        Vec3 renderVehiclePosition = new Vec3(
                Mth.lerp(partialTick, vehicle.xo, vehicle.getX()),
                Mth.lerp(partialTick, vehicle.yo, vehicle.getY()),
                Mth.lerp(partialTick, vehicle.zo, vehicle.getZ())
        );
        Quaternionf vehicleRotation = new Quaternionf()
                .rotateY(org.joml.Math.toRadians(-vehicle.yRotO))
                .rotateX(org.joml.Math.toRadians(vehicle.xRotO))
                .rotateZ(org.joml.Math.toRadians(vehicle.zRotO))
                .slerp(vehicle.rotYXZ(), partialTick);
        Quaternionf inverseVehicleRotation = new Quaternionf(vehicleRotation).conjugate();
        Vector3f lookRight = new Vector3f(1, 0, 0)
                .rotate(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
        inverseVehicleRotation.transform(lookRight);

        VertexConsumer builder = bufferSource.getBuffer(RenderType.entityCutoutNoCull(ROPE_TEXTURE));
        poseStack.pushPose();
        Matrix4f matrix = poseStack.last().pose();
        for (int i = 0; i < ropeNodes.size() - 1; i++) {
            Vec3 p1 = interpolatedLocalPosition(ropeNodes.get(i), partialTick, renderVehiclePosition, inverseVehicleRotation);
            Vec3 p2 = interpolatedLocalPosition(ropeNodes.get(i + 1), partialTick, renderVehiclePosition, inverseVehicleRotation);
            renderSegment(builder, matrix, p1, p2, lookRight, packedLight);
        }
        poseStack.popPose();
    }

    @OnlyIn(Dist.CLIENT)
    private Vec3 interpolatedLocalPosition(RopeNode node, float partialTick, Vec3 renderVehiclePosition,
                                           Quaternionf inverseVehicleRotation) {
        Vec3 worldPosition = new Vec3(
                Mth.lerp(partialTick, node.lastPos.x, node.pos.x),
                Mth.lerp(partialTick, node.lastPos.y, node.pos.y),
                Mth.lerp(partialTick, node.lastPos.z, node.pos.z)
        );
        Vector3f centered = worldPosition.subtract(renderVehiclePosition).subtract(vehicle.centerOffset).toVector3f();
        inverseVehicleRotation.transform(centered);
        return new Vec3(centered).add(vehicle.centerOffset);
    }

    @OnlyIn(Dist.CLIENT)
    private static void renderSegment(VertexConsumer builder, Matrix4f matrix, Vec3 p1, Vec3 p2,
                                      Vector3f lookRight, int packedLight) {
        Vec3 segmentAxis = p2.subtract(p1);
        if (segmentAxis.lengthSqr() < 1.0E-8) {
            return;
        }
        segmentAxis = segmentAxis.normalize();

        Vec3 side = new Vec3(lookRight);
        side = side.subtract(segmentAxis.scale(side.dot(segmentAxis)));
        if (side.lengthSqr() < 1.0E-8) {
            Vec3 fallbackAxis = Math.abs(segmentAxis.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
            side = fallbackAxis.cross(segmentAxis);
        }
        side = side.normalize();
        Vec3 crossSide = segmentAxis.cross(side).normalize();

        float halfWidth = ROPE_WIDTH * 0.5f;
        renderPlane(builder, matrix, p1, p2, side.scale(halfWidth), 0, CHAIN_U_STEP, packedLight);
        renderPlane(builder, matrix, p1, p2, crossSide.scale(halfWidth), CHAIN_U_STEP, CHAIN_U_STEP * 2, packedLight);
    }

    @OnlyIn(Dist.CLIENT)
    private static void renderPlane(VertexConsumer builder, Matrix4f matrix, Vec3 p1, Vec3 p2,
                                    Vec3 offset, float uMin, float uMax, int packedLight) {
        addVertex(builder, matrix, p1.subtract(offset), uMin, 0, packedLight);
        addVertex(builder, matrix, p1.add(offset), uMax, 0, packedLight);
        addVertex(builder, matrix, p2.add(offset), uMax, 1, packedLight);
        addVertex(builder, matrix, p2.subtract(offset), uMin, 1, packedLight);
    }

    @OnlyIn(Dist.CLIENT)
    private static void addVertex(VertexConsumer builder, Matrix4f matrix, Vec3 position,
                                  float u, float v, int light) {
        builder.addVertex(matrix, (float) position.x, (float) position.y, (float) position.z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(0, 1, 0);
    }

    public static class RopeNode {
        public Vec3 pos;
        public Vec3 lastPos;
        public boolean fixed;

        public RopeNode(Vec3 pos) {
            this.pos = pos;
            this.lastPos = pos;
        }
    }

}
