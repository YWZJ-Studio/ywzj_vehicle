package org.ywzj.vehicle.custom.part.data;

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock.model.BedrockCubePerFace;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock.model.BedrockModel;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.custom.pojo.WeaponInfo;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;
import org.ywzj.vehicle.vehicle.structure.VehicleBedrockCubeOBB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class WeaponUnitData extends RotatableUnitData {

    private String base;
    private Vec3 opticalSightOffset;
    private Vec3 operatorOffset;
    private List<WeaponInfo> weapons;

    private float barrelLength = 0.0f;

    private List<VehicleBedrockCubeOBB> yTurnUnitOBBs = List.of();
    private List<VehicleBedrockCubeOBB> xTurnUnitOBBs = List.of();
    private boolean operatorOnWeaponUnit;
    private WeaponUnit.OpticalSightType opticalSightType;
    private float zoomMax;

    public WeaponUnitData(String id) {
        super(id);
    }

    public WeaponUnitData(WeaponUnitPojo pojo) {
        super(pojo);
        this.base = pojo.base;
        this.opticalSightOffset = pojo.opticalSightOffset;
        this.operatorOffset = pojo.operatorOffset;
        this.weapons = pojo.weapons;
        this.operatorOnWeaponUnit = pojo.operatorOnWeaponUnit;
        this.opticalSightType = pojo.opticalSightType;
        this.zoomMax = pojo.zoomMax;
    }

    public String getBase() {
        return base;
    }

    public Vec3 getOpticalSightOffset() {
        return opticalSightOffset;
    }

    public Vec3 getOperatorOffset() {
        return operatorOffset;
    }

    public List<WeaponInfo> getWeapons() {
        return weapons;
    }

    public float getBarrelLength() {
        return barrelLength;
    }

    public float getZoomMax() {
        return zoomMax;
    }

    public WeaponUnit.OpticalSightType getOpticalSightType() {
        return opticalSightType;
    }

    public boolean isOperatorOnWeaponUnit() {
        return operatorOnWeaponUnit;
    }

    // 两个轴上的载具结构块的副本
    public List<VehicleBedrockCubeOBB> getYTurnUnitOBBs() {
        return yTurnUnitOBBs.stream().map(VehicleBedrockCubeOBB::new).collect(Collectors.toList());
    }

    public List<VehicleBedrockCubeOBB> getXTurnUnitOBBs() {
        return xTurnUnitOBBs.stream().map(VehicleBedrockCubeOBB::new).collect(Collectors.toList());
    }

    @Override
    public List<VehicleBedrockCubeOBB> getUnitBedrockCubeOBBs() {
        List<VehicleBedrockCubeOBB> unitBedrockCubeOBBs = new ArrayList<>(yTurnUnitOBBs.size() + xTurnUnitOBBs.size());
        unitBedrockCubeOBBs.addAll(yTurnUnitOBBs);
        unitBedrockCubeOBBs.addAll(xTurnUnitOBBs);
        return unitBedrockCubeOBBs;
    }

    @Override
    public void initStructureModel(BedrockModel model) {
        if (model == null) return;

        var yTurnBone = model.getBoneMap().get(this.structureBone);
        var xTurnBone = model.getBoneMap().get(this.structureBone + "_barrel");
        if (yTurnBone != null && xTurnBone != null) {
            this.pivotOffset = new Vec3(yTurnBone.x / 16, xTurnBone.y / 16, yTurnBone.z / 16);
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
}
