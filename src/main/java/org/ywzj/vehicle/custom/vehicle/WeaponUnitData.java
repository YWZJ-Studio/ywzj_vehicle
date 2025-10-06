package org.ywzj.vehicle.custom.vehicle;

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock.model.BedrockBone;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock.model.BedrockCubePerFace;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock.model.BedrockModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.vehicle.VehicleBedrockCubeOBB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 武器站配置，包含已经计算完成的obb缓存等
 */
public class WeaponUnitData {
    private WeaponUnitPojo.RotInfo rotInfo = new WeaponUnitPojo.RotInfo();
    private String id;
    private WeaponUnitPojo.PartType partType = WeaponUnitPojo.PartType.WEAPON;
    private String name = "part.ywzj_vehicle.default_name";
    private String structureBone = null;
    private String parent = null;
    private Vec3 operatorOffset = Vec3.ZERO;
    private Vec3 seatOffset = Vec3.ZERO;
    private List<ResourceLocation> weapons = List.of();
    private Vec3 boltOffset = Vec3.ZERO;
    private float barrelLength = 0.0f;
    private List<VehicleBedrockCubeOBB> yTurnUnitOBBs = List.of();
    private List<VehicleBedrockCubeOBB> xTurnUnitOBBs = List.of();

    public static WeaponUnitData of(WeaponUnitPojo pojo, BedrockModel model) {
        var data = new WeaponUnitData();
        data.rotInfo = pojo.rotInfo;
        data.id = pojo.id;
        data.partType = pojo.partType;
        data.name = pojo.name;
        data.structureBone = pojo.structureBone;
        data.parent = pojo.parent;
        if (pojo.operatorOffset != null) data.operatorOffset = new Vec3(pojo.operatorOffset.x, pojo.operatorOffset.y, pojo.operatorOffset.z);
        if (pojo.seatOffset != null) data.seatOffset = new Vec3(pojo.seatOffset.x, pojo.seatOffset.y, pojo.seatOffset.z);
        if (pojo.weapons != null) data.weapons = pojo.weapons;

        data.initStructureModel(pojo.structureBone, model);
        return data;
    }

    public WeaponUnitPojo.RotInfo getRotInfo() {
        return rotInfo;
    }

    public String getId() {
        return id;
    }

    public WeaponUnitPojo.PartType getPartType() {
        return partType;
    }

    public String getName() {
        return name;
    }

    public String getStructureBone() {
        return structureBone;
    }

    public String getParent() {
        return parent;
    }

    public Vec3 getOperatorOffset() {
        return operatorOffset;
    }

    public Vec3 getSeatOffset() {
        return seatOffset;
    }

    public List<ResourceLocation> getWeapons() {
        return weapons;
    }

    public Vec3 getBoltOffset() {
        return boltOffset;
    }

    public float getBarrelLength() {
        return barrelLength;
    }

    // 两个轴上的载具结构块的副本
    public List<VehicleBedrockCubeOBB> getYTurnUnitOBBs() {
        return yTurnUnitOBBs.stream().map(VehicleBedrockCubeOBB::new).collect(Collectors.toList());
    }

    public List<VehicleBedrockCubeOBB> getXTurnUnitOBBs() {
        return xTurnUnitOBBs.stream().map(VehicleBedrockCubeOBB::new).collect(Collectors.toList());
    }

    public List<VehicleBedrockCubeOBB> getUnitBedrockCubeOBBs() {
        List<VehicleBedrockCubeOBB> unitBedrockCubeOBBs = new ArrayList<>(yTurnUnitOBBs.size() + xTurnUnitOBBs.size());
        unitBedrockCubeOBBs.addAll(yTurnUnitOBBs);
        unitBedrockCubeOBBs.addAll(xTurnUnitOBBs);
        return unitBedrockCubeOBBs;
    }

    /**
     * 从载具结构模型中初始化武器站信息
     * @param name 结构骨骼名称
     * @param model 载具结构模型
     */
    private void initStructureModel(String name, BedrockModel model) {
        if (model == null) return;

        var yTurnBone = model.getBoneMap().get(name);
        var xTurnBone = model.getBoneMap().get(name + "_barrel");
        if (yTurnBone != null && xTurnBone != null) {
            this.boltOffset = new Vec3(yTurnBone.x / 16, xTurnBone.y / 16, yTurnBone.z / 16);
            var cubes = xTurnBone.cubes.stream().map(c -> (BedrockCubePerFace) c).toList();
            var barrelCube = cubes.stream()
                    .max(Comparator.comparingDouble(c -> c.getDepth() * c.getWidth() * c.getHeight()))
                    .orElse(null);
            if (barrelCube != null) {
                double barrelHalfLength = new Vec3(
                        xTurnBone.x / 16 - yTurnBone.x / 16 + barrelCube.getX() + barrelCube.getWidth() / 2,
                        barrelCube.getY() + barrelCube.getHeight() / 2,
                        xTurnBone.z / 16 - yTurnBone.z / 16 + barrelCube.getZ() + barrelCube.getDepth() / 2
                ).length();
                this.barrelLength = (float) (barrelHalfLength * 2);
            }
        }

        this.yTurnUnitOBBs = collectOBBs(yTurnBone);
        this.xTurnUnitOBBs = collectOBBs(xTurnBone);
    }

    /**
     * 为指定骨骼收集OBB
     * @param bone 骨骼
     * @return OBB列表
     */
    private List<VehicleBedrockCubeOBB> collectOBBs(BedrockBone bone) {
        if (bone == null) return List.of();
        List<VehicleBedrockCubeOBB> obbs = new ArrayList<>();
        bone.cubes.stream()
                .map(c -> (BedrockCubePerFace) c)
                .forEach(cube -> obbs.add(VehicleBedrockCubeOBB.init(bone, cube)));
        bone.getChildren().forEach(child ->
                child.cubes.stream()
                        .map(c -> (BedrockCubePerFace) c)
                        .forEach(cube -> obbs.add(VehicleBedrockCubeOBB.init(child, cube)))
        );
        return obbs;
    }
}
