package org.ywzj.vehicle.vehicle.part;

import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BakedModelInstance;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SuspensionUnit<T extends SuspensionUnitData> extends PartUnit<T> {

    private static final double CONTACT_EPSILON = 0.001;
    private static final double CONTACT_RECOVERY = 0.25;
    private static final double SUPPORT_SPACING = 1.0;
    private float restLength;
    private float maxCompression;
    private float compression;
    private float remoteCompression;
    private boolean grounded;
    private double contactCompression;
    private List<Vec3> supportOffsets = List.of(Vec3.ZERO);
    private double[] supportRestLengths;
    private double[] supportMaxCompressions;
    private final List<SupportContact> supportContacts = new ArrayList<>();

    private record Contact(double length, Vec3 offset, int index) {}
    private record CalibrationPoint(SuspensionUnit<?> unit, int index, Vec3 bottom, Vector3d arm) {}
    public record SupportContact(Vec3 position, double compression, double minCompression, double maxCompression, double weight) {}

    public SuspensionUnit(int index, AbstractVehicle vehicle, T data) {
        super(index, vehicle, data);
        restLength = data.getRestLength();
        maxCompression = data.getMaxCompression();
        supportRestLengths = new double[]{restLength};
        supportMaxCompressions = new double[]{maxCompression};
        compression = remoteCompression = getMinCompression();
        contactCompression = compression;
        syncData.define(SyncDataSerializers.FLOAT, value -> remoteCompression = value, this::getCompression, compression);
        syncData.define(SyncDataSerializers.BOOLEAN, value -> grounded = value, this::isGrounded, false);
    }

    @Override
    public void buildStructure(Map<VehicleCubeGroup, VehicleCubeGroup> groups) {
        super.buildStructure(groups);
        supportOffsets = buildSupportOffsets();
        supportRestLengths = new double[supportOffsets.size()];
        supportMaxCompressions = new double[supportOffsets.size()];
        Arrays.fill(supportRestLengths, restLength);
        Arrays.fill(supportMaxCompressions, maxCompression);
        supportContacts.clear();
    }

    private List<Vec3> buildSupportOffsets() {
        if (structureGroup == null) {
            return List.of(Vec3.ZERO);
        }
        Quaternionf parentRotation = structureGroup.parent == null ? new Quaternionf()
                : structureGroup.parent.globalTransform().rotation();
        Quaternionf inverseParentRotation = new Quaternionf(parentRotation).invert();
        // 以轮底采样，兼容轮心和轮底枢轴。
        double bottom = 0;
        List<AABB> bounds = new ArrayList<>();
        AABB totalBounds = null;
        for (VehicleCubeOBB cube : partCubeOBBs) {
            var transform = cube.group.globalTransform();
            Vector3f min = new Vector3f(Float.POSITIVE_INFINITY);
            Vector3f max = new Vector3f(Float.NEGATIVE_INFINITY);
            for (int corner = 0; corner < 8; corner++) {
                Vector3f point = new Vector3f((float) (cube.x + ((corner & 1) == 0 ? 0 : cube.width)),
                        (float) (cube.y + ((corner & 2) == 0 ? 0 : cube.height)),
                        (float) (cube.z + ((corner & 4) == 0 ? 0 : cube.depth)));
                transform.rotation().transform(point);
                Vec3 relativePoint = transform.offset().add(new Vec3(point)).subtract(pivotOffset);
                Vector3f localPoint = inverseParentRotation.transform(relativePoint.toVector3f());
                min.min(localPoint);
                max.max(localPoint);
                bottom = Math.min(bottom, localPoint.y);
            }
            AABB box = new AABB(min.x, min.y, min.z, max.x, max.y, max.z);
            bounds.add(box);
            totalBounds = totalBounds == null ? box : totalBounds.minmax(box);
        }
        if (totalBounds == null) {
            return List.of(new Vec3(0, bottom, 0));
        }
        double splitLength = Math.max(SUPPORT_SPACING, totalBounds.getYsize() * 1.5);
        if (Math.max(totalBounds.getXsize(), totalBounds.getZsize()) <= splitLength) {
            return List.of(new Vec3(0, bottom, 0));
        }
        List<Vec3> offsets = new ArrayList<>();
        for (AABB box : bounds) {
            int countX = box.getXsize() > splitLength ? (int) Math.ceil(box.getXsize() / SUPPORT_SPACING) : 1;
            int countZ = box.getZsize() > splitLength ? (int) Math.ceil(box.getZsize() / SUPPORT_SPACING) : 1;
            for (int x = 0; x < countX; x++) {
                for (int z = 0; z < countZ; z++) {
                    Vec3 offset = new Vec3(box.minX + box.getXsize() * (x + 0.5) / countX,
                            box.minY, box.minZ + box.getZsize() * (z + 0.5) / countZ);
                    if (offsets.stream().noneMatch(existing -> existing.distanceToSqr(offset) < 1.0E-6)) {
                        offsets.add(offset);
                    }
                }
            }
        }
        return List.copyOf(offsets);
    }

    /** 初始化时按空载重力及力矩平衡标定自由长度，不随燃油或轮组损坏重新标定。 */
    public static void calibrateRestLengths(AbstractVehicle vehicle) {
        List<SuspensionUnit<?>> suspensionUnits = vehicle.getSuspensionUnits().stream()
                .filter(suspensionUnit -> suspensionUnit.structureGroup != null && suspensionUnit.data.getSpringStiffness() > 0).toList();
        if (suspensionUnits.isEmpty()) {
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
        List<CalibrationPoint> points = new ArrayList<>();
        // 微小正则项兼容两轮、共线支撑以及锁定旋转的情形。
        Matrix3d stiffness = new Matrix3d().scaling(1.0E-9);
        for (SuspensionUnit<?> suspensionUnit : suspensionUnits) {
            Quaternionf parentRotation = suspensionUnit.structureGroup.parent == null ? new Quaternionf()
                    : suspensionUnit.structureGroup.parent.globalTransform().rotation();
            Vec3 direction = new Vec3(parentRotation.transform(new Vector3f(0, -1, 0)));
            if (direction.y >= -0.25) {
                return;
            }
            List<Vec3> offsets = suspensionUnit.getSupportOffsets();
            double k = PhysicsHelper.forcePerTick(suspensionUnit.data.getSpringStiffness()) / offsets.size();
            for (int i = 0; i < offsets.size(); i++) {
                Vec3 bottom = suspensionUnit.pivotOffset.add(new Vec3(parentRotation.transform(offsets.get(i).toVector3f())));
                ground = Math.min(ground, bottom.y);
                Vec3 arm = bottom.subtract(center);
                Vector3d a = new Vector3d(1, physics.lockCenterRot ? 0 : -arm.z,
                        physics.lockCenterRot || physics.lockZRot ? 0 : arm.x).div(-direction.y);
                points.add(new CalibrationPoint(suspensionUnit, i, bottom, a));
                stiffness.add(new Matrix3d(
                        k * a.x * a.x, k * a.x * a.y, k * a.x * a.z,
                        k * a.y * a.x, k * a.y * a.y, k * a.y * a.z,
                        k * a.z * a.x, k * a.z * a.y, k * a.z * a.z));
            }
        }
        Vector3d deflection = stiffness.invert().transform(new Vector3d(vehicle.curbWeight * PhysicsEngine.G, 0, 0));
        double[] staticCompression = new double[points.size()];
        for (int i = 0; i < points.size(); i++) {
            staticCompression[i] = points.get(i).arm().dot(deflection);
            // 无法仅靠向上支撑平衡的布局继续使用包内参数。
            if (!Double.isFinite(staticCompression[i]) || staticCompression[i] < 0) {
                return;
            }
        }
        // 留 0.01 格间隙，避免绑定轮底或车体恰好贴地时重复托底。
        ground -= 0.01;
        for (int i = 0; i < points.size(); i++) {
            CalibrationPoint point = points.get(i);
            SuspensionUnit<?> suspensionUnit = point.unit();
            suspensionUnit.supportRestLengths[point.index()] = staticCompression[i] + (point.bottom().y - ground) * point.arm().x;
            // 保留配置的压缩行程，但至少保证空载标定点位于行程内。
            suspensionUnit.supportMaxCompressions[point.index()] = Math.max(suspensionUnit.data.getMaxCompression(), staticCompression[i] + 0.01);
        }
        for (SuspensionUnit<?> suspensionUnit : suspensionUnits) {
            suspensionUnit.restLength = (float) Arrays.stream(suspensionUnit.supportRestLengths).average().orElse(suspensionUnit.restLength);
            suspensionUnit.maxCompression = (float) Arrays.stream(suspensionUnit.supportMaxCompressions).average().orElse(suspensionUnit.maxCompression);
            suspensionUnit.compression = suspensionUnit.clampCompression(suspensionUnit.compression);
            suspensionUnit.remoteCompression = suspensionUnit.clampCompression(suspensionUnit.remoteCompression);
            suspensionUnit.contactCompression = suspensionUnit.compression;
        }
    }

    public float getMaxCompression() {
        return Math.max(maxCompression, getMinCompression());
    }

    public float getMinCompression() {
        // 压缩量以自由长度为零点；总下垂量则以模型绑定位置为零点。
        return (float) getMinCompression(restLength);
    }

    private double getMinCompression(double length) {
        return Math.max(-data.getMaxExtension(), length - data.getMaxDroop());
    }

    protected boolean isActive() {
        return structureGroup != null && !isDetached() && !isDestroyed();
    }

    /** 绑定姿态下相对安装枢轴的接地采样点，使用父组坐标系。 */
    protected List<Vec3> getSupportOffsets() {
        return supportOffsets;
    }

    public void simulate() {
        Vec3 mountWorld = worldPositionWithBaseRot(pivotOffset);
        grounded = false;
        supportContacts.clear();
        compression = getMinCompression();
        contactCompression = compression;
        Vec3 direction = getSuspensionDirection();
        if (!isActive() || direction.y >= -0.25) {
            return;
        }
        List<Contact> contacts = findContacts(mountWorld, direction, CONTACT_RECOVERY, false, 1);
        double weight = 1.0 / getSupportOffsets().size();
        for (Contact point : contacts) {
            double pointRestLength = supportRestLengths[point.index()];
            double minCompression = getMinCompression(pointRestLength);
            supportContacts.add(new SupportContact(mountWorld.add(point.offset()).add(direction.scale(point.length())),
                    pointRestLength - point.length(), minCompression,
                    Math.max(supportMaxCompressions[point.index()], minCompression), weight));
        }
        Contact contact = nearestContact(contacts);
        double contactLength = contact.length();
        if (contactLength != Double.POSITIVE_INFINITY) {
            // 限位求解保留穿透量，轮组变换限制在机械行程内。
            contactCompression = restLength - contactLength;
            compression = clampCompression((float) contactCompression);
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
        double height = 0;
        for (Contact contact : findContacts(mount, direction, maxStep / -direction.y, true, 1)) {
            double pointRestLength = supportRestLengths[contact.index()];
            double maxCompression = Math.max(supportMaxCompressions[contact.index()], getMinCompression(pointRestLength));
            double penetration = (pointRestLength - maxCompression - contact.length()) * -direction.y;
            if (penetration > CONTACT_RECOVERY && penetration <= maxStep + CONTACT_EPSILON) {
                height = Math.max(height, penetration);
            }
        }
        return height;
    }

    private Contact findContact(Vec3 mount, Vec3 direction, double recovery, boolean stepProbe, float partialTick) {
        return nearestContact(findContacts(mount, direction, recovery, stepProbe, partialTick));
    }

    private Contact nearestContact(List<Contact> contacts) {
        double length = Double.POSITIVE_INFINITY;
        Vec3 offset = Vec3.ZERO;
        int count = 0;
        for (Contact contact : contacts) {
            if (contact.length() < length - 1.0E-6) {
                length = contact.length();
                offset = contact.offset();
                count = 1;
            } else if (Math.abs(contact.length() - length) <= 1.0E-6) {
                length = Math.min(length, contact.length());
                offset = offset.add(contact.offset());
                count++;
            }
        }
        return new Contact(length, count > 0 ? offset.scale(1.0 / count) : Vec3.ZERO, -1);
    }

    private List<Contact> findContacts(Vec3 mount, Vec3 direction, double recovery, boolean stepProbe, float partialTick) {
        List<Contact> contacts = new ArrayList<>();
        Quaternionf rotation = getSuspensionRotation(partialTick);
        List<Vec3> offsets = getSupportOffsets();
        for (int i = 0; i < offsets.size(); i++) {
            double pointRestLength = supportRestLengths[i];
            double minCompression = getMinCompression(pointRestLength);
            double maxLength = pointRestLength - minCompression;
            double minLength = pointRestLength - Math.max(supportMaxCompressions[i], minCompression);
            // 允许恢复轻微穿透，避免触底后误判悬空。
            double probeMinLength = minLength - recovery;
            double contactLength = Double.POSITIVE_INFINITY;
            Vec3 worldOffset = new Vec3(rotation.transform(offsets.get(i).toVector3f()));
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
                    contactLength = Math.min(contactLength, length);
                }
            }
            if (contactLength != Double.POSITIVE_INFINITY) {
                contacts.add(new Contact(contactLength, worldOffset, i));
            }
        }
        return contacts;
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

    public List<SupportContact> getSupportContacts() {
        return List.copyOf(supportContacts);
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
    public CompoundTag serializeNBT() {
        CompoundTag tag = super.serializeNBT();
        tag.putFloat("Compression", compression);
        tag.putBoolean("Grounded", grounded);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        super.deserializeNBT(tag);
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
