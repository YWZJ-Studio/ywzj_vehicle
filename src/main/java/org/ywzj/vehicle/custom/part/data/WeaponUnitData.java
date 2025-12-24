package org.ywzj.vehicle.custom.part.data;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockBone;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockCube;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel;
import com.google.gson.annotations.SerializedName;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.ywzj.vehicle.vehicle.pojo.Bolt;
import org.ywzj.vehicle.vehicle.pojo.WeaponInfo;
import org.ywzj.vehicle.vehicle.structure.VehicleBedrockCubeOBB;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class WeaponUnitData extends RotatableUnitData {

    private List<Bolt> bolts;
    private FiringMode firingMode;
    private boolean parentWeaponUnitAim;
    private Vec3 opticalSightOffset;
    private Vec3 operatorViewOffset;
    private boolean operatorOnWeaponUnit;
    private OpticalSightType opticalSightType;
    private float zoomMin;
    private float zoomMax;
    private FireControlLockType fireControlLockType;
    private CrosshairStyle crosshairStyle;
    private List<WeaponInfo> weapons;

    private List<VehicleBedrockCubeOBB> yTurnUnitOBBs = List.of();
    private List<VehicleBedrockCubeOBB> xTurnUnitOBBs = List.of();

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
        this.fireControlLockType = pojo.fireControlLockType;
        this.opticalSightType = pojo.opticalSightType;
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

    public FireControlLockType getFireControlLockType() {
        return fireControlLockType;
    }

    public OpticalSightType getOpticalSightType() {
        return opticalSightType;
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
        unitBedrockCubeOBBs.addAll(yTurnUnitOBBs.stream().map(VehicleBedrockCubeOBB::new).toList());
        unitBedrockCubeOBBs.addAll(xTurnUnitOBBs.stream().map(VehicleBedrockCubeOBB::new).toList());
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
            this.pivotOffset = new Vec3(yTurnBone.x / 16, yTurnBone.y / 16, yTurnBone.z / 16);
        } else {
            this.pivotOffset = Vec3.ZERO;
        }
        var xTurnBone = model.getBoneMap().get(this.structureBone + "_barrel");
        if (xTurnBone == null) {
            // 若未配置炮闩数据且仅有座圈结构模型，取座圈结构块的Z轴正方向的表面中心为唯一炮闩
            if ((this.bolts == null || this.bolts.isEmpty()) && !yTurnUnitOBBs.isEmpty()) {
                this.bolts = new ArrayList<>();
                this.bolts.add(new Bolt(Vec3.ZERO, (float) (yTurnUnitOBBs.get(0).depth / 2), 0, 0));
            }
            return;
        }
        this.xTurnUnitOBBs = collectOBBs(xTurnBone);
        // 若未配置炮闩数据，则从结构模型中推算
        if (this.bolts == null || this.bolts.isEmpty()) {
            this.bolts = new ArrayList<>();
            buildBolts(xTurnBone);
        }
    }

    private void buildBolts(BedrockBone bone) {
        for (BedrockCube cube : bone.cubes) {
            // 以单个Cube描述一根炮管
            Vec3 boltOffset = new Vec3(bone.x / 16 + cube.x() + cube.width() / 2 - pivotOffset.x,
                    bone.y / 16 + cube.y() + cube.height() / 2 - pivotOffset.y,
                    bone.z / 16 - pivotOffset.z);
            float barrelLength = cube.z() + cube.depth();
            Vector3f selfRot = new Vector3f();
            bone.rotation.getEulerAnglesYXZ(selfRot);
            this.bolts.add(new Bolt(boltOffset, barrelLength, (float) Math.toDegrees(selfRot.x), (float) Math.toDegrees(selfRot.y)));
        }
        for (BedrockBone child : bone.getChildren()) {
            buildBolts(child);
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
        NONE,
        // 红外
        IR,
        // 雷达
        RF
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
