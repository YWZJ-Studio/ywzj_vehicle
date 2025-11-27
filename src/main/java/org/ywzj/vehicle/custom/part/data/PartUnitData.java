package org.ywzj.vehicle.custom.part.data;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockBone;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockCubePerFace;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.vehicle.structure.VehicleBedrockCubeOBB;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 预初始化的载具部件数据
 */
public class PartUnitData {

    protected final String id;
    protected boolean isSeat;
    protected String name;
    protected String structureBone;

    protected Vec3 pivotOffset = Vec3.ZERO;
    protected Vec3 seatOffset = Vec3.ZERO;
    protected Vec3 ownerViewOffset = null;

    protected List<VehicleBedrockCubeOBB> unitBedrockCubeOBBs;

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
    public void initStructureModel(BedrockModel model) {
        if (model != null) {
            var bone = model.getBoneMap().get(this.structureBone);
            if (bone == null) {
                return;
            }
            this.pivotOffset = new Vec3(bone.x / 16, bone.y / 16, bone.z / 16);
            unitBedrockCubeOBBs = collectOBBs(bone);
        }
    }

    /**
     * 获取原始OBB列表，你不应该直接修改它，而是使用 {@link #getUnitBedrockCubeOBBs()} 获取副本
     * @return 原始OBB列表
     */
    public List<VehicleBedrockCubeOBB> getRawUnitBedrockCubeOBBs() {
        return unitBedrockCubeOBBs;
    }

    /**
     * 获取OBB的副本
     * @return OBB列表
     */
    public List<VehicleBedrockCubeOBB> getUnitBedrockCubeOBBs() {
        if (unitBedrockCubeOBBs == null) {
            return List.of();
        }
        return unitBedrockCubeOBBs.stream().map(VehicleBedrockCubeOBB::new).collect(Collectors.toList());
    }

    public Vec3 getSeatOffset() {
        return seatOffset;
    }

    public Vec3 getPivotOffset() {
        return pivotOffset;
    }

    public Vec3 getOwnerViewOffset() {
        return ownerViewOffset;
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

    /**
     * 为指定骨骼收集OBB
     * @param bone 骨骼
     * @return OBB列表
     */
    public static List<VehicleBedrockCubeOBB> collectOBBs(BedrockBone bone) {
        if (bone == null) return List.of();
        List<VehicleBedrockCubeOBB> obbs = new ArrayList<>();
        bone.cubes.stream()
                .map(cube -> (BedrockCubePerFace) cube)
                .forEach(cube -> obbs.add(VehicleBedrockCubeOBB.init(bone, cube)));
        bone.getChildren().forEach(child ->
                child.cubes.stream()
                        .map(cube -> (BedrockCubePerFace) cube)
                        .forEach(cube -> obbs.add(VehicleBedrockCubeOBB.init(child, cube)))
        );
        return obbs;
    }
}
