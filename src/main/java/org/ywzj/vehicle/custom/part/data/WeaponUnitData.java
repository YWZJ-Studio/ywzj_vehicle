package org.ywzj.vehicle.custom.part.data;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockBone;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockCube;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel;
import com.google.gson.annotations.SerializedName;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.ywzj.vehicle.vehicle.pojo.Bolt;
import org.ywzj.vehicle.vehicle.pojo.WeaponInfo;
import org.ywzj.vehicle.vehicle.structure.VehicleCubeGroup;
import org.ywzj.vehicle.vehicle.structure.VehicleCubeOBB;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WeaponUnitData extends RotatableUnitData {

    private List<Bolt> bolts;
    boolean singleBarrel;
    private FiringMode firingMode;
    private boolean parentWeaponUnitAim;
    private Vec3 opticalSightOffset;
    private Vec3 operatorViewOffset;
    private boolean operatorOnWeaponUnit;
    private OpticalSightType opticalSightType;
    private boolean withStabilizer;
    private float zoomMin;
    private float zoomMax;
    private FireControlSensorType fireControlSensorType;
    private FireControlLockType fireControlLockType;
    private CrosshairStyle crosshairStyle;
    private List<WeaponInfo> weapons;
    private VehicleCubeGroup xTurnGroup;

    public WeaponUnitData(String id) {
        super(id);
    }

    public WeaponUnitData(WeaponUnitPojo pojo) {
        super(pojo);
        this.bolts = pojo.bolts;
        this.firingMode = pojo.firingMode;
        this.parentWeaponUnitAim = pojo.parentWeaponUnitAim;
        this.opticalSightOffset = pojo.opticalSightOffset;
        this.operatorViewOffset = pojo.operatorViewOffset;
        this.operatorOnWeaponUnit = pojo.operatorOnWeaponUnit;
        this.fireControlSensorType = pojo.fireControlSensorType;
        this.fireControlLockType = pojo.fireControlLockType;
        this.opticalSightType = pojo.opticalSightType;
        this.withStabilizer = pojo.withStabilizer;
        this.zoomMin = pojo.zoomMin;
        this.zoomMax = pojo.zoomMax;
        this.crosshairStyle = pojo.crosshairStyle;
        this.weapons = pojo.weapons;
    }

    public List<Bolt> getBolts() {
        return bolts;
    }

    public FiringMode getFiringMode() {
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

    public FireControlSensorType getFireControlSensorType() {
        return fireControlSensorType;
    }

    public FireControlLockType getFireControlLockType() {
        return fireControlLockType;
    }

    public OpticalSightType getOpticalSightType() {
        return opticalSightType;
    }

    public boolean withStabilizer() {
        return withStabilizer;
    }

    public float getZoomMin() {
        return zoomMin;
    }

    public float getZoomMax() {
        return zoomMax;
    }

    public CrosshairStyle getCrosshairStyle() {
        return crosshairStyle;
    }

    public List<WeaponInfo> getWeapons() {
        return weapons;
    }

    public VehicleCubeGroup getRawXTurnGroup() {
        return xTurnGroup;
    }

    @Override
    public void initStructureModel(BedrockModel model, Map<BedrockBone, VehicleCubeGroup> vehiclePartGroups) {
        if (model == null) {
            return;
        }
        var yTurnBone = model.getBoneMap().get(this.structureBone);
        List<VehicleCubeOBB> yTurnUnitOBBs = new ArrayList<>();
        if (yTurnBone != null) {
            VehicleCubeGroup yTurnGroup = vehiclePartGroups.get(yTurnBone);
            if (yTurnGroup != null) {
                this.structureGroup = yTurnGroup;
                yTurnUnitOBBs.addAll(yTurnGroup.cubeOBBs);
                this.pivotOffset = yTurnGroup.globalTransform().offset();
            }
        }
        var xTurnBone = model.getBoneMap().get(this.structureBone + "_barrel");
        List<VehicleCubeOBB> xTurnUnitOBBs = new ArrayList<>();
        if (xTurnBone != null) {
            VehicleCubeGroup xTurnGroup = vehiclePartGroups.get(xTurnBone);
            if (xTurnGroup != null) {
                this.xTurnGroup = xTurnGroup;
                xTurnUnitOBBs.addAll(xTurnGroup.cubeOBBs);
            }
        } else if (yTurnBone != null) {
            this.xTurnGroup = vehiclePartGroups.get(yTurnBone);
        }
        if (xTurnBone == null || xTurnBone.getChildren().isEmpty() || xTurnBone.getChildren().size() == 1) {
            this.singleBarrel = true;
        }
        this.partCubeOBBs = new ArrayList<>();
        this.partCubeOBBs.addAll(xTurnUnitOBBs);
        this.partCubeOBBs.addAll(yTurnUnitOBBs);
        if (xTurnBone == null) {
            // 若未配置炮闩数据且仅有座圈结构模型，取座圈结构块的Z轴正方向的表面中心为唯一炮闩
            if ((this.bolts == null || this.bolts.isEmpty()) && !yTurnUnitOBBs.isEmpty()) {
                this.bolts = new ArrayList<>();
                this.bolts.add(new Bolt(Vec3.ZERO, (float) (yTurnUnitOBBs.get(0).depth / 2), 0, 0));
            }
            return;
        }
        // 若未配置炮闩数据，则从结构模型中推算
        if (this.bolts == null || this.bolts.isEmpty()) {
            this.bolts = new ArrayList<>();
            buildBolts(xTurnBone, Vec3.ZERO);
        }
    }

    private void buildBolts(BedrockBone bone, Vec3 offset) {
        for (BedrockCube cube : bone.cubes) {
            // 使用单个Cube来描述一根炮管
            // 以Cube的Z轴正方向作为炮管轴线，起始端对应炮闩位置，终止端对应炮口位置，Cube在该方向上的整体长度即为炮管长度。
            float x = cube.x() + cube.width() / 2;
            float y = cube.y() + cube.height() / 2;
            float z = cube.z();
            Vec3 boltOffset = new Vec3(bone.rotation.transform(new Vector3f(x, y, z)));
            boltOffset = boltOffset.add(offset);
            float barrelLength = cube.depth();
            Vector3f selfRot = new Vector3f();
            bone.rotation.getEulerAnglesYXZ(selfRot);
            if (singleBarrel) {
                // 若武器单元只有一个炮管，那其基础旋转视为武器单元朝向的默认值
                if (this.structureGroup != null) {
                    this.structureGroup.rotation.mul(new Quaternionf(bone.rotation)).getEulerAnglesYXZ(selfRot);
                    this.structureGroup.baseRotation = new Quaternionf();
                }
                this.rotInfo.xRot = (float) Math.toDegrees(selfRot.x);
                this.rotInfo.yRot = (float) Math.toDegrees(-selfRot.y);
                bone.rotation.set(new Quaternionf());
                this.bolts.add(new Bolt(boltOffset, barrelLength, 0, 0));
            } else {
                // 若武器单元有多个炮管且朝向各不相同，那它们都将自带基础旋转
                this.bolts.add(new Bolt(boltOffset, barrelLength, (float) Math.toDegrees(selfRot.x), (float) Math.toDegrees(-selfRot.y)));
            }
        }
        for (BedrockBone child : bone.getChildren()) {
            buildBolts(child, offset.add(child.x / 16, child.y / 16, child.z / 16));
        }
    }

    public enum FiringMode {
        // 轮射
        @SerializedName("ripple")
        RIPPLE,
        // 齐射
        @SerializedName("salvo")
        SALVO
    }

    public enum OpticalSightType {
        // 不能开镜
        @SerializedName("none")
        NONE,
        // 以操作员视角开镜
        @SerializedName("operator")
        OPERATOR,
        // 以观瞄视角开镜（光学瞄具）
        @SerializedName("optical_scope")
        OPTICAL_SCOPE,
        // 以观瞄视角开镜（模拟电视）
        @SerializedName("crt")
        CRT
    }

    public enum FireControlSensorType {
        // 未启用
        @SerializedName("none")
        NONE,
        // 红外
        @SerializedName("ir")
        IR,
        // 雷达
        @SerializedName("rf")
        RF,
        // 光电
        @SerializedName("eo")
        EO
    }

    public enum FireControlLockType {
        // 不能锁定
        @SerializedName("none")
        NONE,
        // 准心锁定
        @SerializedName("aim_hit")
        AIM_HIT,
        // 视锥内锁定
        @SerializedName("aim_frustum")
        AIM_FRUSTUM
    }

    public enum CrosshairStyle {
        @SerializedName("none")
        NONE,
        @SerializedName("circle")
        CIRCLE,
        @SerializedName("square")
        SQUARE,
        @SerializedName("reticle")
        RETICLE
    }

}
