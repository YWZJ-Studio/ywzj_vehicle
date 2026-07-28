package org.ywzj.vehicle.vehicle.part;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockBone;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockCubePerFace;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.INBTSerializable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.ywzj.vehicle.api.custom.sync.SyncDataSerializers;
import org.ywzj.vehicle.custom.CommonAssetsManager;
import org.ywzj.vehicle.custom.part.data.PartUnitData;
import org.ywzj.vehicle.custom.sync.PartUnitSyncData;
import org.ywzj.vehicle.custom.sync.SyncDataEntry;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.network.message.ClientVehicleAction;
import org.ywzj.vehicle.vehicle.pojo.PassengerPose;
import org.ywzj.vehicle.vehicle.structure.OBB;
import org.ywzj.vehicle.vehicle.structure.VehicleCubeGroup;
import org.ywzj.vehicle.vehicle.structure.VehicleCubeOBB;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 具有结构模型的载具部件基类<br/>
 * 任何乘位都应关联于一个载具部件，以计算座位与镜头位置
 */
public class PartUnit<T extends PartUnitData> implements INBTSerializable<CompoundTag> {

    protected final int index;
    protected final String id;
    protected final Component name;
    protected final AbstractVehicle vehicle;
    protected VehicleCubeGroup structureGroup;
    protected List<VehicleCubeOBB> partCubeOBBs;
    protected LivingEntity owner;
    protected int ownerId;
    protected boolean isSeat;
    protected float seatRot;
    protected Vec3 seatOffset = Vec3.ZERO;
    protected PassengerPose passengerPose;
    protected Vec3 ownerViewOffset = null;
    protected Vec3 pivotOffset = Vec3.ZERO;
    protected boolean renderModel;
    protected ResourceLocation displayId;
    protected Vec3 displayOffset;
    protected PartUnit<?> parentPartUnit;
    protected List<PartUnit<?>> subPartUnits = new ArrayList<>();
    protected T data;
    protected PartUnitSyncData syncData;

    public PartUnit(int index, AbstractVehicle vehicle, T data) {
        this.index = index;
        this.id = data.getId();
        this.name = Component.translatable(data.getName());
        this.vehicle = vehicle;
        this.data = data;
        this.isSeat = data.isSeat();
        this.seatRot = data.getSeatRot();
        this.seatOffset = data.getSeatOffset();
        this.passengerPose = data.getPassengerPose();
        this.ownerViewOffset = data.getOwnerViewOffset();
        this.pivotOffset = data.getPivotOffset();
        this.renderModel = data.isRenderModel();
        this.displayId = data.getDisplayId();
        this.displayOffset = data.getDisplayOffset();
        this.syncData = new PartUnitSyncData(this);
        this.syncData.define(SyncDataSerializers.VEC3, this::setSeatOffset, this::getSeatOffset, Vec3.ZERO);
    }

    public void buildStructure(Map<VehicleCubeGroup, VehicleCubeGroup> vehicleCubeGroupCopy) {
        this.partCubeOBBs = data.getRawPartCubeOBBs().stream().map(VehicleCubeOBB::new).collect(Collectors.toList());
        this.partCubeOBBs.forEach(cubeOBB -> cubeOBB.group = vehicleCubeGroupCopy.get(cubeOBB.group));
        this.structureGroup = vehicleCubeGroupCopy.get(data.getRawStructureGroup());
    }

    /**
     * 组合方法，在创建载具过程中，所有部件创建完成后调用依次对每个部件调用，用于部件间的关联或是进行初始化操作<br/>
     * 此阶段所有部件均已创建完成，但是尚未附加到载具上<br/>
     * 载具在此阶段尚未完成初始化和添加到世界上
     *
     * @param partUnitsView 载具所有部件的不可变视图
     * @param vehicle 所属载具
     */
    public void combineAndInit(@UnmodifiableView Map<String, PartUnit<?>> partUnitsView, AbstractVehicle vehicle) {
        for (String subPartUnitId : data.getSubPartUnitIds()) {
            PartUnit<?> subPartUnit = partUnitsView.get(subPartUnitId);
            if (subPartUnit != null) {
                addSubPartUnit(subPartUnit);
                subPartUnit.setParentPartUnit(this);
            }
        }
    }

    public void onRemoved() {}

    @OnlyIn(Dist.CLIENT)
    public void render(PoseStack pPoseStack, MultiBufferSource bufferSource, int pPackedLight) {}

    public void tick() {
        if (!this.getVehicle().level().isClientSide()) {
            syncData.tick();
        }
    }

    public boolean onInteract(Player player, InteractionHand hand) {
        return true;
    }

    public PartUnitSyncData getSyncData() {
        return syncData;
    }

    public List<VehicleCubeOBB> getPartCubeOBBs() {
        return partCubeOBBs;
    }

    public List<OBB> getOBBs() {
        return partCubeOBBs.stream().map(VehicleCubeOBB::obb).toList();
    }

    public Vec3 worldPivotPosition() {
        return worldPositionWithBaseRot(pivotOffset);
    }

    public Vec3 worldSeatPosition() {
        float eyeHeight = getOwner() == null ? 2 : owner.getEyeHeight();
        return worldPositionWithSelfRot(new Vec3(seatOffset.x, seatOffset.y  - eyeHeight, seatOffset.z));
    }

    public Vec3 worldOwnerViewPosition(float partialTick) {
        float eyeHeight = getOwner() == null ? 2 : owner.getEyeHeight();
        if (ownerViewOffset == null) {
            return worldPosition(new Vec3(0, eyeHeight, 0), partialTick);
        }
        return worldPosition(ownerViewOffset, partialTick);
    }

    /**
     * 计算车身、部件、附着部件都未旋转时某相对于载具枢轴的偏移xyz在经由车身、部件、附着部件旋转后的实际世界坐标
     */
    public Vec3 worldPosition(Vec3 offsetFromVehicle) {
        return worldPosition(offsetFromVehicle, 1.0F);
    }

    public Vec3 worldPositionWithBaseRot(Vec3 offsetFromVehicle) {
        return worldPositionWithBaseRot(offsetFromVehicle, 1.0F);
    }

    public Vec3 worldPositionWithSelfRot(Vec3 offsetFromVehicle) {
        return worldPositionWithSelfRot(offsetFromVehicle, 1.0F);
    }

    public Vec3 worldPosition(Vec3 offsetFromVehicle, float partialTick) {
        if (offsetFromVehicle == null) {
            return vehicle.position(partialTick);
        }
        return worldPositionWithSelfRot(offsetFromVehicle, partialTick);
    }

    public Vec3 worldPositionWithBaseRot(Vec3 offsetFromVehicle, float partialTick) {
        if (structureGroup == null || structureGroup.parent == null) {
            return worldPositionWithVehicleRot(offsetFromVehicle, partialTick);
        }
        return worldPositionWithGroupRot(offsetFromVehicle, structureGroup.parent, partialTick);
    }

    public Vec3 worldPositionWithSelfRot(Vec3 offsetFromVehicle, float partialTick) {
        if (structureGroup == null) {
            return worldPositionWithVehicleRot(offsetFromVehicle, partialTick);
        }
        return worldPositionWithGroupRot(offsetFromVehicle, structureGroup, partialTick);
    }

    public Vec3 worldPositionWithGroupRot(Vec3 offsetFromVehicle, VehicleCubeGroup group, float partialTick) {
        Vec3 rotatedOffset = group.globalTransform(offsetFromVehicle.subtract(group.pivotOffset), true,
                        currentGroup -> getViewGroupRotation(currentGroup, partialTick))
                .offset()
                .subtract(vehicle.centerOffset);
        Vector3f worldOffset = vehicle.rotYXZ(partialTick).transform(rotatedOffset.toVector3f());
        return vehicle.position(partialTick).add(vehicle.centerOffset)
                .add(worldOffset.x, worldOffset.y, worldOffset.z);
    }

    private Vec3 worldPositionWithVehicleRot(Vec3 offsetFromVehicle, float partialTick) {
        Vector3f worldOffset = vehicle.rotYXZ(partialTick)
                .transform(offsetFromVehicle.subtract(vehicle.centerOffset).toVector3f());
        return vehicle.position(partialTick).add(vehicle.centerOffset)
                .add(worldOffset.x, worldOffset.y, worldOffset.z);
    }

    protected Quaternionf getViewGroupRotation(VehicleCubeGroup group, float partialTick) {
        return new Quaternionf(group.rotation);
    }

    public void withVehicleRot(float dVehicleXRot, float dVehicleYRot, float dVehicleZRot) {}

    public Component getName() {
        return name;
    }

    public int getIndex() {
        return index;
    }

    @NotNull
    public AbstractVehicle getVehicle() {
        return vehicle;
    }

    public VehicleCubeGroup getStructureGroup() {
        return structureGroup;
    }

    public boolean isSeat() {
        return isSeat;
    }

    public LivingEntity getOwner() {
        if (this.owner == null && this.ownerId != -1) {
            if (this.vehicle.level().getEntity(this.ownerId) instanceof LivingEntity livingEntity) {
                this.owner = livingEntity;
            }
        }
        return this.owner;
    }

    public void setOwner(LivingEntity owner) {
        if (owner == null) {
            this.owner = null;
            this.ownerId = -1;
        } else {
            this.owner = owner;
            this.ownerId = owner.getId();
        }
    }

    public void setOwnerId(int ownerId) {
        this.ownerId = ownerId;
    }

    public Vec3 getOwnerViewOffset() {
        return ownerViewOffset;
    }

    public Vec3 getPivotOffset() {
        return pivotOffset;
    }

    public void setPivotOffset(Vec3 pivotOffset) {
        this.pivotOffset = pivotOffset;
    }

    public ResourceLocation getDisplayId() {
        return displayId;
    }

    public Vec3 getDisplayOffset() {
        return displayOffset;
    }

    public void setOwnerViewOffset(Vec3 ownerViewOffset) {
        this.ownerViewOffset = ownerViewOffset;
    }

    public float getSeatRot() {
        return vehicle.getYRot() + seatRot;
    }

    public void applySeatRot(LivingEntity passenger) {
        float takeSeatRot = getSeatRot();
        passenger.setYRot(takeSeatRot);
        passenger.setYBodyRot(takeSeatRot);
        passenger.setYHeadRot(takeSeatRot);
    }

    public Vec3 getSeatOffset() {
        return seatOffset;
    }

    public void setSeatOffset(Vec3 seatOffset) {
        this.seatOffset = seatOffset;
    }

    public PassengerPose getPassengerPose() {
        return passengerPose;
    }

    public void setPassengerPose(PassengerPose passengerPose) {
        this.passengerPose = passengerPose;
    }

    public void setParentPartUnit(PartUnit<?> parentPartUnit) {
        this.parentPartUnit = parentPartUnit;
    }

    public void addSubPartUnit(PartUnit<?> partUnit) {
        this.subPartUnits.add(partUnit);
    }

    public List<PartUnit<?>> getSubPartUnits() {
        return subPartUnits;
    }

    @OnlyIn(Dist.CLIENT)
    public void onUpdateReceived(List<SyncDataEntry<?>> entries) {
        syncData.onUpdateReceived(entries);
    }

    public void onClientMessageReceived(ClientVehicleAction message, Player player) {
        if (message.shoot) {
            vehicle.shoot(message.partUnitIndex, message.weaponIndex, message.aimContexts, player);
        } else {
            PartUnit<?> partUnit = vehicle.getPartUnits().get(message.partUnitIndex);
            if (partUnit instanceof RotatableUnit<?> rotatableUnit) {
                rotatableUnit.setXAimRot(message.xAimRot);
                rotatableUnit.setYAimRot(message.yAimRot);
            }
        }
    }

    public T getData() {
        return data;
    }

    @Override
    public CompoundTag serializeNBT() {
        return new CompoundTag();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {}

    @NotNull
    public String getId() {
        return id;
    }

    @Deprecated
    protected BedrockBone unitBone;

    /**
     * 你应该尽可能从数据包创建部件，而不是使用此构造函数手动创建部件<br/>
     * 仅供测试使用
     */
    @Deprecated
    public PartUnit(String id, int index, AbstractVehicle vehicle) {
        this.name = Component.translatable(id);
        this.id = id;
        this.index = index;
        this.syncData = new PartUnitSyncData(this);
        this.syncData.define(SyncDataSerializers.VEC3, this::setSeatOffset, this::getSeatOffset, Vec3.ZERO);
        this.vehicle = vehicle;
        this.structureGroup = null;
        this.partCubeOBBs = new ArrayList<>();
        this.initStructureModel(id);
        this.initOBBs();
    }

    @Deprecated
    protected void initStructureModel(String name) {
        CommonAssetsManager.structureModelManager().getStructureModel(vehicle.getStructureModel()).ifPresent(
                model -> {
                    this.unitBone = model.getBoneMap().get(name);
                    if (unitBone != null) {
                        this.pivotOffset = new Vec3(unitBone.x / 16, unitBone.y / 16, unitBone.z / 16);
                    }
                }
        );
    }

    @Deprecated
    protected void initOBBs() {
        if (unitBone != null) {
            VehicleCubeGroup parentGroup = new VehicleCubeGroup(null, unitBone.rotation, new Vec3(unitBone.x / 16, unitBone.y / 16, unitBone.z / 16));
            List<BedrockCubePerFace> cubes = new ArrayList<>(unitBone.cubes.stream().map(cube -> (BedrockCubePerFace) cube).toList());
            for (BedrockCubePerFace cube : cubes) {
                partCubeOBBs.add(VehicleCubeOBB.init(parentGroup, cube));
            }
            for (BedrockBone child : unitBone.getChildren()) {
                List<BedrockCubePerFace> childCubes = new ArrayList<>(child.cubes.stream().map(cube -> (BedrockCubePerFace) cube).toList());
                for (BedrockCubePerFace cube : childCubes) {
                    VehicleCubeGroup group = new VehicleCubeGroup(parentGroup, child.rotation, new Vec3(child.x / 16, child.y / 16, child.z / 16));
                    partCubeOBBs.add(VehicleCubeOBB.init(group, cube));
                }
            }
        }
    }

}
