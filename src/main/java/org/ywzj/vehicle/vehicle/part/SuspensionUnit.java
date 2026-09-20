package org.ywzj.vehicle.vehicle.part;

import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BakedModelInstance;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix3d;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.ywzj.vehicle.api.custom.sync.SyncDataSerializers;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.client.resource.vehicle.VehicleBedrockModel;
import org.ywzj.vehicle.custom.part.data.SuspensionUnitData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.util.PhysicsHelper;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.PhysicsEngine;
import org.ywzj.vehicle.vehicle.structure.VehicleCubeGroup;
import org.ywzj.vehicle.vehicle.structure.VehicleCubeOBB;

import java.util.List;
import java.util.Map;

public class SuspensionUnit<T extends SuspensionUnitData> extends PartUnit<T> {

    private static final double CONTACT_EPSILON = 0.001;
    private static final double CONTACT_RECOVERY = 0.25;
    private float restLength;
    private float maxCompression;
    private float compression;
    private float remoteCompression;
    private boolean grounded;
    private double supportDrop;
    private double contactCompression;
    private Vec3 mountWorld = Vec3.ZERO;
    private Vec3 supportOffsetWorld = Vec3.ZERO;

    private record Contact(double length, Vec3 offset) {}

    public SuspensionUnit(int index, AbstractVehicle vehicle, T data) {
        super(index, vehicle, data);
        restLength = data.getRestLength();
        maxCompression = data.getMaxCompression();
        compression = remoteCompression = getMinCompression();
        contactCompression = compression;
        syncData.define(SyncDataSerializers.FLOAT, value -> remoteCompression = value,
                this::getCompression, compression);
        syncData.define(SyncDataSerializers.BOOLEAN, value -> grounded = value, this::isGrounded, false);
    }

    @Override
    public void buildStructure(Map<VehicleCubeGroup, VehicleCubeGroup> groups) {
        super.buildStructure(groups);
        if (structureGroup == null) {
            return;
        }
        Quaternionf parentRotation = structureGroup.parent == null ? new Quaternionf()
                : structureGroup.parent.globalTransform().rotation();
        Vec3 up = new Vec3(parentRotation.transform(new Vector3f(0, 1, 0)));
        // 以轮底采样，兼容轮心和轮底枢轴。
        double bottom = 0;
        for (VehicleCubeOBB cube : partCubeOBBs) {
            var transform = cube.group.globalTransform();
            for (int corner = 0; corner < 8; corner++) {
                Vector3f point = new Vector3f((float) (cube.x + ((corner & 1) == 0 ? 0 : cube.width)),
                        (float) (cube.y + ((corner & 2) == 0 ? 0 : cube.height)),
                        (float) (cube.z + ((corner & 4) == 0 ? 0 : cube.depth)));
                transform.rotation().transform(point);
                Vec3 relativePoint = transform.offset().add(new Vec3(point)).subtract(pivotOffset);
                bottom = Math.min(bottom, relativePoint.dot(up));
            }
        }
        supportDrop = -bottom;
    }

    /** 初始化时按空载重力及力矩平衡标定自由长度，不随燃油或轮组损坏重新标定。 */
    public static void calibrateRestLengths(AbstractVehicle vehicle) {
        List<SuspensionUnit<?>> units = vehicle.getSuspensionUnits().stream()
                .filter(unit -> unit.structureGroup != null && unit.data.getSpringStiffness() > 0).toList();
        if (units.isEmpty()) {
            return;
        }
        var physics = vehicle.physicsEngine;
        var cube = vehicle.getMainCubeOBB();
        Vec3 center = cube.offset().add(new Vec3(cube.selfRot().transform(physics.physicsInfo.center.toVector3f())));
        Quaternionf bodyRotation = cube.group.globalTransform().rotation();
        double bodyHalfHeight = (Math.abs(bodyRotation.transform(new Vector3f(1, 0, 0)).y) * cube.width
                + Math.abs(bodyRotation.transform(new Vector3f(0, 1, 0)).y) * cube.height
                + Math.abs(bodyRotation.transform(new Vector3f(0, 0, 1)).y) * cube.depth) / 2;
        double ground = cube.offset().y - bodyHalfHeight;
        Vec3[] bottoms = new Vec3[units.size()];
        Vector3d[] arms = new Vector3d[units.size()];
        // 微小正则项兼容两轮、共线支撑以及锁定旋转的情形。
        Matrix3d stiffness = new Matrix3d().scaling(1.0E-9);
        for (int i = 0; i < units.size(); i++) {
            SuspensionUnit<?> unit = units.get(i);
            Quaternionf parentRotation = unit.structureGroup.parent == null ? new Quaternionf()
                    : unit.structureGroup.parent.globalTransform().rotation();
            Vec3 direction = new Vec3(parentRotation.transform(new Vector3f(0, -1, 0)));
            if (direction.y >= -0.25) {
                return;
            }
            bottoms[i] = unit.pivotOffset.add(direction.scale(unit.supportDrop));
            ground = Math.min(ground, bottoms[i].y);
            Vec3 arm = bottoms[i].subtract(center);
            Vector3d a = new Vector3d(1, physics.lockCenterRot ? 0 : -arm.z,
                    physics.lockCenterRot || physics.lockZRot ? 0 : arm.x).div(-direction.y);
            arms[i] = a;
            double k = PhysicsHelper.forcePerTick(unit.data.getSpringStiffness());
            stiffness.add(new Matrix3d(
                    k * a.x * a.x, k * a.x * a.y, k * a.x * a.z,
                    k * a.y * a.x, k * a.y * a.y, k * a.y * a.z,
                    k * a.z * a.x, k * a.z * a.y, k * a.z * a.z));
        }
        Vector3d deflection = stiffness.invert().transform(new Vector3d(vehicle.curbWeight * PhysicsEngine.G, 0, 0));
        double[] staticCompression = new double[units.size()];
        for (int i = 0; i < units.size(); i++) {
            staticCompression[i] = arms[i].dot(deflection);
            // 无法仅靠向上支撑平衡的布局继续使用包内参数。
            if (!Double.isFinite(staticCompression[i]) || staticCompression[i] < 0) {
                return;
            }
        }
        // 留 0.01 格间隙，避免绑定轮底或车体恰好贴地时重复托底。
        ground -= 0.01;
        for (int i = 0; i < units.size(); i++) {
            SuspensionUnit<?> unit = units.get(i);
            unit.restLength = (float) (staticCompression[i] + (bottoms[i].y - ground) * arms[i].x);
            // 保留配置的压缩行程，但至少保证空载标定点位于行程内。
            unit.maxCompression = Math.max(unit.data.getMaxCompression(), (float) staticCompression[i] + 0.01f);
            unit.compression = unit.clampCompression(unit.compression);
            unit.remoteCompression = unit.clampCompression(unit.remoteCompression);
            unit.contactCompression = unit.compression;
        }
    }

    public float getMaxCompression() {
        return Math.max(maxCompression, getMinCompression());
    }

    public float getMinCompression() {
        // 压缩量以自由长度为零点；总下垂量则以模型绑定位置为零点。
        return Math.max(-data.getMaxExtension(), restLength - data.getMaxDroop());
    }

    protected boolean isActive() {
        return structureGroup != null && !isDetached() && !isDestroyed();
    }

    /** 绑定姿态下相对安装枢轴的接地采样点，使用父组坐标系。 */
    protected List<Vec3> getSupportOffsets() {
        return List.of(new Vec3(0, -supportDrop, 0));
    }

    public void simulate() {
        mountWorld = worldPositionWithBaseRot(pivotOffset);
        grounded = false;
        compression = getMinCompression();
        contactCompression = compression;
        Vec3 direction = getSuspensionDirection();
        if (!isActive() || direction.y >= -0.25) {
            return;
        }
        Contact contact = findContact(mountWorld, direction, CONTACT_RECOVERY, false, 1);
        double contactLength = contact.length();
        if (contactLength != Double.POSITIVE_INFINITY) {
            // 限位求解保留穿透量，轮组变换限制在机械行程内。
            contactCompression = restLength - contactLength;
            compression = clampCompression((float) contactCompression);
            supportOffsetWorld = contact.offset();
            grounded = true;
        }
    }

    public double getStepUpHeight(double maxStep) {
        if (!isActive()) {
            return 0;
        }
        Vec3 direction = getSuspensionDirection();
        if (direction.y >= -0.5) {
            return 0;
        }
        Vec3 mount = worldPositionWithBaseRot(pivotOffset);
        double length = findContact(mount, direction, maxStep / -direction.y, true, 1).length();
        double penetration = (restLength - getMaxCompression() - length) * -direction.y;
        return penetration > CONTACT_RECOVERY
                && penetration <= maxStep + CONTACT_EPSILON ? penetration : 0;
    }

    private Contact findContact(Vec3 mount, Vec3 direction, double recovery, boolean stepProbe, float partialTick) {
        double maxLength = restLength - getMinCompression();
        double minLength = restLength - getMaxCompression();
        // 允许恢复轻微穿透，避免触底后误判悬空。
        double probeMinLength = minLength - recovery;
        double contactLength = Double.POSITIVE_INFINITY;
        Vec3 contactOffset = Vec3.ZERO;
        int contactCount = 0;
        Quaternionf rotation = getSuspensionRotation(partialTick);
        for (Vec3 offset : getSupportOffsets()) {
            Vec3 worldOffset = new Vec3(rotation.transform(offset.toVector3f()));
            Vec3 origin = mount.add(worldOffset);
            double compressedBottomY = origin.y + direction.y * minLength;
            Vec3 start = origin.add(direction.scale(probeMinLength));
            Vec3 end = origin.add(direction.scale(maxLength));
            AABB probe = new AABB(start, end).inflate(CONTACT_EPSILON);
            for (AABB box : vehicle.physicsEngine.getBlockCollisionBoxes(probe)) {
                if (stepProbe && box.minY > compressedBottomY + CONTACT_EPSILON) {
                    continue;
                }
                double length = (box.maxY - origin.y) / direction.y;
                Vec3 contact = origin.add(direction.scale(length));
                if (contact.x >= box.minX - CONTACT_EPSILON && contact.x <= box.maxX + CONTACT_EPSILON
                        && contact.z >= box.minZ - CONTACT_EPSILON && contact.z <= box.maxZ + CONTACT_EPSILON
                        && length >= probeMinLength && length <= maxLength + CONTACT_EPSILON) {
                    if (length < contactLength - 1.0E-6) {
                        contactLength = length;
                        contactOffset = worldOffset;
                        contactCount = 1;
                    } else if (Math.abs(length - contactLength) <= 1.0E-6) {
                        // 共面接触取平均支撑点，避免平地上总是偏向第一条履带。
                        contactLength = Math.min(contactLength, length);
                        contactOffset = contactOffset.add(worldOffset);
                        contactCount++;
                    }
                }
            }
        }
        return new Contact(contactLength, contactCount > 0 ? contactOffset.scale(1.0 / contactCount) : Vec3.ZERO);
    }

    @Override
    public void tick() {
        if (vehicle.level().isClientSide()) {
            // 按插值车身重建轮组几何，接地状态仍由服务端同步。
            compression = getVisualCompression(1, Mth.lerp(0.5f, compression, remoteCompression));
        } else {
            // 角积分后重新采样安装轴。
            simulate();
        }
        updateTransform();
        super.tick();
    }

    private float clampCompression(float value) {
        return Mth.clamp(value, getMinCompression(), getMaxCompression());
    }

    private void updateTransform() {
        if (structureGroup == null) {
            return;
        }
        structureGroup.offsetO = structureGroup.offset;
        structureGroup.rotationO = new Quaternionf(structureGroup.rotation);
        setOffset(new Vec3(0, compression - restLength, 0));
        structureGroup.rotation = new Quaternionf(structureGroup.baseRotation);
    }

    public Vec3 getSuspensionDirection() {
        return getSuspensionDirection(1);
    }

    private Vec3 getSuspensionDirection(float partialTick) {
        return new Vec3(getSuspensionRotation(partialTick).transform(new Vector3f(0, -1, 0))).normalize();
    }

    private Quaternionf getSuspensionRotation(float partialTick) {
        Quaternionf rotation = vehicle.rotYXZ(partialTick);
        if (structureGroup != null && structureGroup.parent != null) {
            rotation.mul(structureGroup.parent.globalTransform(Vec3.ZERO, false,
                    group -> getViewGroupRotation(group, partialTick), group -> group.offset).rotation());
        }
        return rotation;
    }

    public float getCompression() {
        return compression;
    }

    // 未截断的压缩量用于求解压缩端限位。
    public double getContactCompression() {
        return contactCompression;
    }

    private float getVisualCompression(float partialTick, float fallback) {
        Vec3 direction = getSuspensionDirection(partialTick);
        if (!isActive() || direction.y >= -0.25) {
            return clampCompression(fallback);
        }
        Vec3 mount = worldPositionWithBaseRot(pivotOffset, partialTick);
        double length = findContact(mount, direction, CONTACT_RECOVERY, false, partialTick).length();
        return clampCompression(length != Double.POSITIVE_INFINITY ? (float) (restLength - length) : fallback);
    }

    public boolean isGrounded() {
        return grounded && isActive();
    }

    @Override
    public Vec3 worldPivotPosition() {
        return worldPositionWithSelfRot(pivotOffset);
    }

    public Vec3 getSupportWorld() {
        return mountWorld.add(supportOffsetWorld).add(getSuspensionDirection().scale(restLength - contactCompression));
    }

    @Override
    public boolean rendersBone() {
        return structureGroup != null;
    }

    @OnlyIn(Dist.CLIENT)
    protected Vec3 getVisualOffset(float partialTick) {
        if (structureGroup == null) {
            return Vec3.ZERO;
        }
        float interpolatedCompression = (float) Mth.lerp(partialTick,
                structureGroup.offsetO.y + restLength, compression);
        float visualCompression = getVisualCompression(partialTick, interpolatedCompression);
        Vec3 visualOffset = new Vec3(0, visualCompression - restLength, 0);
        Vec3 position = structureGroup.globalTransform(Vec3.ZERO, false,
                group -> getViewGroupRotation(group, partialTick),
                group -> group == structureGroup ? visualOffset
                        : group.offsetO.lerp(group.offset, partialTick)).offset();
        Vec3 basePosition = structureGroup.globalTransform(Vec3.ZERO, false,
                group -> getViewGroupRotation(group, partialTick), group -> Vec3.ZERO).offset();
        return position.subtract(basePosition);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, float partialTick) {
        if (isDetached() || structureGroup == null || renderBoneName == null) {
            return;
        }
        var display = ClientAssetsManager.INSTANCE.getVehicleDisplay(vehicle.getDisplayId()).orElse(null);
        if (display == null) {
            return;
        }
        boolean isCabinView = LocalVehiclePlayer.instance.vehicle == vehicle
                && LocalVehiclePlayer.instance.viewType == LocalVehiclePlayer.ViewType.OPERATOR
                && display.getCabinDisplay() != null;
        VehicleBedrockModel model = isCabinView ? display.getCabinDisplay().getModel() : display.getModel();
        ResourceLocation texture = isCabinView ? display.getCabinDisplay().getTexture() : display.getTexture();
        if (model == null || texture == null || !model.hasBakedModel()) {
            return;
        }
        BakedModelInstance modelInstance = isCabinView ? vehicle.getCabinModelInstance() : vehicle.getVehicleModelInstance();
        if (modelInstance == null) {
            modelInstance = model.getDefaultModelInstance();
        }
        int boneIndex = modelInstance.getIndex(renderBoneName);
        if (boneIndex < 0) {
            return;
        }
        poseStack.pushPose();
        try {
            // 仅补结构链的动态平移，姿态与转向沿用车身和骨骼动画。
            Vec3 offset = getVisualOffset(partialTick);
            poseStack.translate(offset.x, offset.y, offset.z);
            model.renderPart(modelInstance, boneIndex, poseStack, bufferSource, texture, packedLight);
        } finally {
            poseStack.popPose();
        }
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = super.serializeNBT(provider);
        tag.putFloat("Compression", compression);
        tag.putBoolean("Grounded", grounded);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        super.deserializeNBT(provider, tag);
        if (tag.contains("Compression", Tag.TAG_ANY_NUMERIC)) {
            compression = remoteCompression = clampCompression(tag.getFloat("Compression"));
        }
        contactCompression = compression;
        grounded = tag.getBoolean("Grounded");
        updateTransform();
        if (structureGroup != null) {
            structureGroup.offsetO = structureGroup.offset;
            structureGroup.rotationO = new Quaternionf(structureGroup.rotation);
        }
    }

}
