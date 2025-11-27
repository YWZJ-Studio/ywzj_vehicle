package org.ywzj.vehicle.custom.part.data;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockCube;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.custom.pojo.Bolt;
import org.ywzj.vehicle.custom.pojo.WeaponInfo;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;
import org.ywzj.vehicle.vehicle.structure.VehicleBedrockCubeOBB;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class WeaponUnitData extends RotatableUnitData {

    private String base;
    private List<Bolt> bolts;
    private WeaponUnit.FiringMode firingMode;
    private boolean parentWeaponUnitAim;
    private Vec3 opticalSightOffset;
    private Vec3 operatorViewOffset;
    private boolean operatorOnWeaponUnit;
    private WeaponUnit.OpticalSightType opticalSightType;
    private float zoomMax;
    private List<WeaponInfo> weapons;

    private List<VehicleBedrockCubeOBB> yTurnUnitOBBs = List.of();
    private List<VehicleBedrockCubeOBB> xTurnUnitOBBs = List.of();

    public WeaponUnitData(String id) {
        super(id);
    }

    public WeaponUnitData(WeaponUnitPojo pojo) {
        super(pojo);
        this.base = pojo.base;
        this.bolts = pojo.bolts;
        this.firingMode = pojo.firingMode;
        this.parentWeaponUnitAim = pojo.parentWeaponUnitAim;
        this.opticalSightOffset = pojo.opticalSightOffset;
        this.operatorViewOffset = pojo.operatorViewOffset;
        this.operatorOnWeaponUnit = pojo.operatorOnWeaponUnit;
        this.opticalSightType = pojo.opticalSightType;
        this.zoomMax = pojo.zoomMax;
        this.weapons = pojo.weapons;
    }

    public String getBase() {
        return base;
    }

    public List<Bolt> getBolts() {
        return bolts;
    }

    public WeaponUnit.FiringMode getFiringMode() {
        return firingMode;
    }

    public boolean isParentWeaponUnitAim() {
        return parentWeaponUnitAim;
    }

    public Vec3 getOpticalSightOffset() {
        return opticalSightOffset;
    }

    public Vec3 getOperatorViewOffset() {
        return operatorViewOffset;
    }

    public boolean isOperatorOnWeaponUnit() {
        return operatorOnWeaponUnit;
    }

    public WeaponUnit.OpticalSightType getOpticalSightType() {
        return opticalSightType;
    }

    public float getZoomMax() {
        return zoomMax;
    }

    public List<WeaponInfo> getWeapons() {
        return weapons;
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
        if (model == null) {
            return;
        }
        var yTurnBone = model.getBoneMap().get(this.structureBone);
        if (yTurnBone != null) {
            this.yTurnUnitOBBs = collectOBBs(yTurnBone);
        } else if (this.base != null) {
            yTurnBone = model.getBoneMap().get(this.base);
            if (yTurnBone == null) {
                return;
            }
        } else {
            return;
        }
        this.pivotOffset = new Vec3(yTurnBone.x / 16, yTurnBone.y / 16, yTurnBone.z / 16);
        var xTurnBone = model.getBoneMap().get(this.structureBone + "_barrel");
        if (xTurnBone == null) {
            return;
        }
        this.xTurnUnitOBBs = collectOBBs(xTurnBone);
        // 若未配置炮闩数据，则从结构模型中推算
        if (this.bolts == null || this.bolts.isEmpty()) {
            this.bolts = new ArrayList<>();
            for (BedrockCube cube : xTurnBone.cubes) {
                float barrelLength = cube.depth();
                Vec3 boltOffset = new Vec3(xTurnBone.x / 16 + cube.x() + cube.width() / 2 - pivotOffset.x,
                        xTurnBone.y / 16 + cube.y() + cube.height() / 2 - pivotOffset.y,
                        xTurnBone.z / 16 + cube.z() - pivotOffset.z);
                this.bolts.add(new Bolt(boltOffset, barrelLength));
            }
        }
    }

}
