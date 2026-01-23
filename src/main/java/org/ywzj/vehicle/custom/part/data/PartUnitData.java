package org.ywzj.vehicle.custom.part.data;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockBone;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.vehicle.structure.VehicleCubeGroup;
import org.ywzj.vehicle.vehicle.structure.VehicleCubeOBB;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 预初始化的载具部件数据
 */
public class PartUnitData {

    protected final String id;
    protected String name;
    protected String structureBone;
    protected boolean isSeat;
    protected Vec3 seatOffset = Vec3.ZERO;
    protected Vec3 ownerViewOffset = null;
    protected Vec3 pivotOffset = Vec3.ZERO;
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
        this.structureBone = pojo.structureBone;
        this.isSeat = pojo.isSeat;
        this.seatOffset = pojo.seatOffset;
        this.ownerViewOffset = pojo.ownerViewOffset;
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
            }
            this.pivotOffset = new Vec3(bone.x / 16, bone.y / 16, bone.z / 16);
        }
    }

    public VehicleCubeGroup getStructureGroup() {
        return structureGroup;
    }

    /**
     * 获取原始OBB列表，你不应该直接修改它，而是使用 {@link #getPartCubeOBBs()} 获取副本
     * @return 原始OBB列表
     */
    public List<VehicleCubeOBB> getRawPartCubeOBBs() {
        return partCubeOBBs;
    }

    /**
     * 获取OBB的副本
     * @return OBB列表
     */
    public List<VehicleCubeOBB> getPartCubeOBBs() {
        if (partCubeOBBs == null) {
            return List.of();
        }
        return partCubeOBBs.stream().map(VehicleCubeOBB::new).collect(Collectors.toList());
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getStructureBone() {
        return structureBone;
    }

    public boolean isSeat() {
        return isSeat;
    }

    public Vec3 getSeatOffset() {
        return seatOffset;
    }

    public Vec3 getOwnerViewOffset() {
        return ownerViewOffset;
    }

    public Vec3 getPivotOffset() {
        return pivotOffset;
    }

    public List<String> getSubPartUnitIds() {
        return subPartUnitIds;
    }

}
