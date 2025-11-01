package org.ywzj.vehicle.vehicle.parts;

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock.model.BedrockBone;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock.model.BedrockCubePerFace;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock.model.BedrockModel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.ywzj.vehicle.bedrock.model.BedrockModelLoader;
import org.ywzj.vehicle.custom.part.data.PartUnitData;
import org.ywzj.vehicle.custom.sync.PartUnitSyncData;
import org.ywzj.vehicle.custom.sync.SyncDataEntry;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.network.message.ClientVehicleAction;
import org.ywzj.vehicle.vehicle.passenger.PassengerPose;
import org.ywzj.vehicle.vehicle.structure.OBB;
import org.ywzj.vehicle.vehicle.structure.VehicleBedrockCubeOBB;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 具有结构模型的载具部件基类<br/>
 * 任何乘位都应关联于一个载具部件，以计算座位与镜头位置
 */
public class PartUnit<T extends PartUnitData> implements INBTSerializable<CompoundTag> {

    private final PartUnitSyncData syncData;

    protected final List<VehicleBedrockCubeOBB> unitBedrockCubeOBBs;
    protected final Component name;
    protected final int index;
    protected final AbstractVehicle vehicle;
    protected T data;

    protected LivingEntity owner;
    protected Vec3 ownerViewOffset;
    protected Vec3 seatOffset;
    protected Vec3 pivotOffset = Vec3.ZERO;
    protected BedrockBone unitBone;

    public PassengerPose passengerPose;

    @Deprecated
    public PartUnit(String name, int index, AbstractVehicle vehicle) {
        this.name = Component.translatable(name);
        this.syncData = new PartUnitSyncData(this);
        this.index = index;
        this.vehicle = vehicle;
        this.unitBedrockCubeOBBs = new ArrayList<>();
        this.initStructureModel(name);
        this.initOBBs();
    }

    public PartUnit(int index, AbstractVehicle vehicle, T data) {
        this.index = index;
        this.vehicle = vehicle;
        this.name = Component.translatable(data.getName());
        this.data = data;
        this.unitBedrockCubeOBBs = data.getUnitBedrockCubeOBBs();
        this.syncData = new PartUnitSyncData(this);
    }

    /**
     * 组合方法，在创建载具过程中，所有部件创建完成后调用依次对每个部件调用，用于部件间的关联或是进行初始化操作<br/>
     * 此阶段所有部件均已创建完成，但是尚未附加到载具上<br/>
     * 载具在此阶段尚未完成初始化和添加到世界上
     *
     * @param partUnitsView 载具所有部件的不可变视图
     * @param vehicle 所属载具
     */
    public void combineAndInit(Map<String, PartUnit<?>> partUnitsView, AbstractVehicle vehicle) {
    }

    public void tick() {
        updateOBBs();
        if (!this.getVehicle().level().isClientSide()) {
            syncData.tick();
        }
    }

    public PartUnitSyncData getSyncData() {
        return syncData;
    }

    protected void initStructureModel(String name) {
        BedrockModel model = BedrockModelLoader.getModel(vehicle.getVehicleType().getStructureBedrockModel());
        if (model != null) {
            this.unitBone = model.getBoneMap().get(name);
            if (unitBone != null) {
                this.pivotOffset = new Vec3(unitBone.x / 16, unitBone.y / 16, unitBone.z / 16);
            }

        }
    }

    protected void initOBBs() {
        if (unitBone != null) {
            List<BedrockCubePerFace> cubes = new ArrayList<>(unitBone.cubes.stream().map(cube -> (BedrockCubePerFace) cube).toList());
            for (BedrockCubePerFace cube : cubes) {
                unitBedrockCubeOBBs.add(VehicleBedrockCubeOBB.init(unitBone, cube));
            }
            for (BedrockBone child : unitBone.getChildren()) {
                List<BedrockCubePerFace> childCubes = new ArrayList<>(child.cubes.stream().map(cube -> (BedrockCubePerFace) cube).toList());
                for (BedrockCubePerFace cube : childCubes) {
                    unitBedrockCubeOBBs.add(VehicleBedrockCubeOBB.init(child, cube));
                }
            }
        }
    }

    public List<VehicleBedrockCubeOBB> getUnitBedrockCubeOBBs() {
        return unitBedrockCubeOBBs;
    }

    public List<OBB> getOBBs() {
        return unitBedrockCubeOBBs.stream().map(VehicleBedrockCubeOBB::obb).toList();
    }

    public void updateOBBs() {
        for (VehicleBedrockCubeOBB unitBedrockCubeOBB : unitBedrockCubeOBBs) {
            OBB obb = unitBedrockCubeOBB.obb();
            Vec3 center = unitBedrockCubeOBB.center(this.vehicle);
            Quaternionf selfRot = new Quaternionf(unitBedrockCubeOBB.selfRot());
            obb.setCenter(vehicle.relativeRotPos(center).toVector3f());
            obb.setRotation(vehicle.rotYXZ().mul(selfRot));
        }
    }

    /**
     * 计算车身未旋转时某相对于载具枢轴的偏移xyz在经由车身旋转后的实际世界坐标
     */
    public Vec3 worldPosition(Vec3 offsetFromVehicle) {
        if (offsetFromVehicle == null) {
            return vehicle.position();
        }
        return vehicle.relativeRotPos(vehicle.position().add(offsetFromVehicle));
    }

    public Vec3 worldOwnerViewPosition() {
        float eyeHeight = owner == null ? 2 : owner.getEyeHeight();
        if (ownerViewOffset == null) {
            return worldPosition(new Vec3(0, eyeHeight, 0));
        }
        return worldPosition(ownerViewOffset);
    }

    public Vec3 worldSeatPosition() {
        float eyeHeight = owner == null ? 2 : owner.getEyeHeight();
        Vec3 seatOffset = this.seatOffset;
        if (seatOffset == null) {
            seatOffset = new Vec3(0, eyeHeight, 0);
        }
        return vehicle.relativeRotPos(vehicle.position().add(seatOffset).subtract(new Vec3(0, eyeHeight, 0)));
    }

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

    public LivingEntity getOwner() {
        return owner;
    }

    public void setOwner(LivingEntity owner) {
        this.owner = owner;
    }

    public Vec3 getOwnerViewOffset() {
        return ownerViewOffset;
    }

    public void setOwnerViewOffset(Vec3 ownerViewOffset) {
        this.ownerViewOffset = ownerViewOffset;
    }

    public Vec3 getSeatOffset() {
        return seatOffset;
    }

    public void setSeatOffset(Vec3 seatOffset) {
        this.seatOffset = seatOffset;
    }

    @OnlyIn(Dist.CLIENT)
    public void onUpdateReceived(List<SyncDataEntry<?>> entries) {
        this.syncData.onUpdateReceived(entries);
    }

    public static void onClientMessageReceived(ClientVehicleAction message, Supplier<NetworkEvent.Context> ctxSupplier) {
        ServerPlayer serverPlayer = ctxSupplier.get().getSender();
        if (serverPlayer == null) {
            return;
        }

        Level level = serverPlayer.level();
        Entity entity = level.getEntity(message.vehicleEntityId);

        if (entity instanceof AbstractVehicle vehicle && message.partUnitIndex < vehicle.getPartUnits().size()) {
            if (message.shoot) {
                vehicle.shoot(message.partUnitIndex, message.ammoSpawnPositions, message.ammoXRot, message.ammoYRot);
            } else {
                PartUnit<?> partUnit = vehicle.getPartUnits().get(message.partUnitIndex);
                if (partUnit instanceof IRotatableUnit rotatableUnit) {
                    rotatableUnit.setXAimRot(message.xAimRot);
                    rotatableUnit.setYAimRot(message.yAimRot);
                }
            }
        }
    }

    @Override
    public CompoundTag serializeNBT() {
        return null;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
    }
}
