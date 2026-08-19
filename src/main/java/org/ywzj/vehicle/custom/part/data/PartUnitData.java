package org.ywzj.vehicle.custom.part.data;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockBone;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.vehicle.pojo.DefenseStats;
import org.ywzj.vehicle.vehicle.pojo.PassengerPose;
import org.ywzj.vehicle.vehicle.structure.VehicleCubeGroup;
import org.ywzj.vehicle.vehicle.structure.VehicleCubeOBB;

import java.util.List;
import java.util.Map;

/**
 * 预初始化的载具部件数据
 */
public class PartUnitData {

    protected final String id;
    protected String name;
    protected float maxHealth;
    protected DefenseStats defenseStats = new DefenseStats();
    protected String renderBone;
    protected String structureBone;
    protected boolean detachable;
    protected boolean isSeat;
    protected float seatRot;
    protected Vec3 seatOffset = Vec3.ZERO;
    protected Vec3 dismountOffset;
    protected PassengerPose passengerPose;
    protected boolean passengerCanUseItem;
    protected Vec3 ownerViewOffset = null;
    protected Vec3 pivotOffset = Vec3.ZERO;
    protected boolean renderModel;
    protected ResourceLocation displayId;
    protected Vec3 displayOffset;
    protected List<String> subPartUnitIds;
    protected VehicleCubeGroup structureGroup;
    protected List<VehicleCubeOBB> partCubeOBBs;

    /**
     * 供子类使用，需要手动完成数据初始化流程
     * @param id 部件ID
     */
    protected PartUnitData(String id) {
        this.id = id;
    }

    public PartUnitData(PartUnitPojo pojo) {
        this.id = pojo.id;
        this.name = pojo.name;
        this.maxHealth = pojo.maxHealth;
        this.defenseStats = pojo.defenseStats;
        this.renderBone = pojo.renderBone;
        this.structureBone = pojo.structureBone;
        this.detachable = pojo.detachable;
        this.isSeat = pojo.isSeat;
        this.seatRot = pojo.seatRot;
        this.seatOffset = pojo.seatOffset;
        this.dismountOffset = pojo.dismountOffset;
        this.passengerPose = pojo.passengerPose;
        this.passengerCanUseItem = pojo.passengerCanUseItem;
        this.ownerViewOffset = pojo.ownerViewOffset;
        this.renderModel = pojo.renderModel;
        this.displayId = pojo.displayId;
        this.displayOffset = pojo.displayOffset;
        this.subPartUnitIds = pojo.subPartUnitIds;
        this.initData(pojo);
    }

    public void initData(PartUnitPojo pojo) {
        if (pojo.seatOffset != null) {
            this.seatOffset = new Vec3(pojo.seatOffset.x, pojo.seatOffset.y, pojo.seatOffset.z);
        }
        if (pojo.ownerViewOffset != null) {
            this.ownerViewOffset = new Vec3(pojo.ownerViewOffset.x, pojo.ownerViewOffset.y, pojo.ownerViewOffset.z);
        }
    }

    /**
     * 尝试根据载具的结构模型初始化部件的结构数据，在初始化载具数据流程中手动调用
     * @param model 结构模型
     */
    public void initStructureModel(BedrockModel model, Map<BedrockBone, VehicleCubeGroup> vehiclePartGroups) {
        if (model != null) {
            var bone = model.getBoneMap().get(this.structureBone);
            if (bone == null) {
                return;
            }
            structureGroup = vehiclePartGroups.get(bone);
            if (structureGroup != null) {
                partCubeOBBs = structureGroup.cubeOBBs;
                this.pivotOffset = structureGroup.globalTransform().offset();
            } else {
                this.pivotOffset = Vec3.ZERO;
            }
        }
    }

    public List<VehicleCubeOBB> getRawPartCubeOBBs() {
        if (partCubeOBBs == null) {
            return List.of();
        }
        return partCubeOBBs;
    }

    public VehicleCubeGroup getRawStructureGroup() {
        return structureGroup;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public float getMaxHealth() {
        return maxHealth;
    }

    public void setMaxHealth(float maxHealth) {
        this.maxHealth = maxHealth;
    }

    public DefenseStats getDefenseStats() {
        return defenseStats;
    }

    public String getRenderBone() {
        return renderBone;
    }

    public void setRenderBone(String renderBone) {
        this.renderBone = renderBone;
    }

    public String getStructureBone() {
        return structureBone;
    }

    public boolean isDetachable() {
        return detachable;
    }

    public boolean isSeat() {
        return isSeat;
    }

    public float getSeatRot() {
        return seatRot;
    }

    public Vec3 getSeatOffset() {
        return seatOffset;
    }

    public Vec3 getDismountOffset() {
        return dismountOffset;
    }

    public PassengerPose getPassengerPose() {
        if (passengerPose == null) {
            return null;
        }
        return new PassengerPose(passengerPose);
    }

    public boolean passengerCanUseItem() {
        return passengerCanUseItem;
    }

    public Vec3 getOwnerViewOffset() {
        return ownerViewOffset;
    }

    public Vec3 getPivotOffset() {
        return pivotOffset;
    }

    public boolean isRenderModel() {
        return renderModel;
    }

    public ResourceLocation getDisplayId() {
        return displayId;
    }

    public Vec3 getDisplayOffset() {
        return displayOffset;
    }

    public List<String> getSubPartUnitIds() {
        return subPartUnitIds;
    }

}
