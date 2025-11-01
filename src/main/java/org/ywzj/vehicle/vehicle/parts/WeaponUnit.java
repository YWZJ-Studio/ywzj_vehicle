package org.ywzj.vehicle.vehicle.parts;

import com.google.gson.annotations.SerializedName;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Math;
import org.joml.Quaternionf;
import org.joml.Vector4d;
import org.ywzj.vehicle.api.custom.sync.SyncDataSerializers;
import org.ywzj.vehicle.custom.VehicleWeaponManager;
import org.ywzj.vehicle.custom.part.data.WeaponUnitData;
import org.ywzj.vehicle.custom.pojo.Bolt;
import org.ywzj.vehicle.custom.sync.SyncDataHolder;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.network.Channel;
import org.ywzj.vehicle.network.message.ClientVehicleAction;
import org.ywzj.vehicle.util.VectorUtil;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.structure.OBB;
import org.ywzj.vehicle.vehicle.structure.VehicleBedrockCubeOBB;
import org.ywzj.vehicle.vehicle.weapon.AbstractVehicleWeapon;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 默认武器站实现<br/>
 * 武器站是一种有方向机与高低机，可发射多类武器的载具可动部件<br/>
 * 其中方向机与高低机的结构模型可相互独立运动<br/>
 * 一个武器站可关联有多个子武器站，并在火控上联动<br/>
 * 多个武器站可纵向堆叠，并在方向机上联动旋转<br/>
 */
public class WeaponUnit extends RotatableUnit<WeaponUnitData> {

    // 武器站枢轴偏移，为武器站枢轴相对于载具枢轴的偏移
    private final Vec3 pivotOffset;
    // 武器站炮闩
    private final List<Bolt> bolts = new ArrayList<>();
    // 当前使用的炮闩
    private int currentBoltIndex;
    // 发射模式
    private FiringMode firingMode;
    // 武器站光瞄偏移，为开镜视角下玩家的摄像机相对于武器站枢轴的偏移
    private final Vec3 opticalSightOffset;
    // 武器站操作员镜头偏移，为操作员视角下玩家的摄像机相对于武器站枢轴的偏移
    private final Vec3 operatorViewOffset;
    // 操作员视角是否随武器站转动
    private boolean operatorOnWeaponUnit = true;
    // 开镜类型
    public OpticalSightType opticalSightType;
    // 最大开镜缩放倍率
    private final float zoomMax;
    // 当前开镜缩放倍率
    private float zoom;
    // 本武器站从属的父武器站
    private WeaponUnit parentWeaponUnit;
    // 本武器站附属的子武器站
    private final List<WeaponUnit> subWeaponUnits = new ArrayList<>();
    // 本武器站所附着于的武器站
    private WeaponUnit baseWeaponUnit;
    // 光瞄火控
    private boolean stabilizer;
    private Vec3 aimLockPosition;
    private Entity aimLockEntity;
    public boolean parentWeaponUnitAim;
    // 第三人称准心样式
    public CrosshairStyle crosshairStyle = CrosshairStyle.CIRCLE;

    public enum FiringMode {
        // 轮射
        RIPPLE,
        // 齐射
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

    public enum CrosshairStyle {
        CIRCLE,
        SQUARE,
        RETICLE
    }

    private List<VehicleBedrockCubeOBB> yTurnUnitOBBs = List.of();
    private List<VehicleBedrockCubeOBB> xTurnUnitOBBs = List.of();
    public final List<AbstractVehicleWeapon<?>> weapons = new ArrayList<>();
    private int currentWeaponIndex = -1;

    public SyncDataHolder<Integer> currentWeaponIndexHolder;

    public void switchWeapon(boolean next) {
        int size = weapons.size();
        this.getCurrentWeapon().ifPresent(
                AbstractVehicleWeapon::onSwitchFrom
        );
        this.currentWeaponIndex = (this.currentWeaponIndex + (next ? 1 : size - 1)) % size;
        this.getCurrentWeapon().ifPresent(
                AbstractVehicleWeapon::onSwitchTo
        );
    }

    public WeaponUnit(int index, AbstractVehicle vehicle, WeaponUnitData data) {
        super(index, vehicle, data);
        this.pivotOffset = data.getPivotOffset();
        this.bolts.add(new Bolt(Vec3.ZERO, data.getBarrelLength()));
        this.firingMode = FiringMode.RIPPLE;
        this.opticalSightOffset = data.getOpticalSightOffset();
        this.operatorViewOffset = data.getOperatorOffset();
        this.seatOffset = data.getSeatOffset();

        this.yTurnUnitOBBs = data.getYTurnUnitOBBs();
        this.xTurnUnitOBBs = data.getXTurnUnitOBBs();

        var rotInfo = data.getRotInfo();
        this.xRotSpeed = rotInfo.xRotSpeed;
        this.yRotSpeed = rotInfo.yRotSpeed;
        this.xRotMax = rotInfo.xRotMax;
        this.xRotMin = rotInfo.xRotMin;
        this.yRotMax = rotInfo.yRotMax;
        this.yRotMin = rotInfo.yRotMin;

        this.zoomMax = data.getZoomMax();
        this.opticalSightType = data.getOpticalSightType();

        this.zoom = 1;

        this.currentWeaponIndexHolder = this.getSyncData().define(
                SyncDataSerializers.INT,
                this::setCurrentWeaponIndex,
                this::getCurrentWeaponIndex,
                currentWeaponIndex
        );
        currentWeaponIndex = 0;
    }

    @Deprecated
    public WeaponUnit(String name, int index, AbstractVehicle vehicle,
                      Vec3 pivotOffset, float barrelLength,
                      Vec3 opticalSightOffset, Vec3 operatorViewOffset, Vec3 seatOffset, WeaponUnit baseWeaponUnit) {
        super(name, index, vehicle);

        this.zoomMax = 8;
        this.zoom = 1;

        this.pivotOffset = pivotOffset;
        this.bolts.add(new Bolt(Vec3.ZERO, barrelLength));
        this.firingMode = FiringMode.RIPPLE;
        this.opticalSightOffset = opticalSightOffset;
        this.operatorViewOffset = operatorViewOffset;
        this.seatOffset = seatOffset;
        this.baseWeaponUnit = baseWeaponUnit;

        this.opticalSightType = OpticalSightType.CRT;

        currentWeaponIndex = 0;
    }

    @Override
    public void combineAndInit(Map<String, PartUnit<?>> partUnitsView, AbstractVehicle vehicle) {
        if (data.getBase() != null) {
            PartUnit<?> basePart = partUnitsView.get(data.getBase());
            if (basePart instanceof WeaponUnit base) {
                this.setBaseWeaponUnit(base);
            }
        }
        int i = 0;
        for (var weaponInfo : data.getWeapons()) {
            var index = VehicleWeaponManager.get().getIndex(weaponInfo.id).orElse(null);
            if (index != null) {
                var parent = this;
                if (weaponInfo.partUnit != null) {
                    PartUnit<?> basePart = partUnitsView.get(weaponInfo.partUnit);
                    if (basePart instanceof WeaponUnit weaponUnit) {
                        parent = weaponUnit;
                    }
                }
                var weapon = index.create(vehicle, parent, i);
                this.weapons.add(weapon);
                weapon.defineSyncData(this.getSyncData());
                i++;
            }
        }
    }

    @Override
    public void tick() {
        if (vehicle.level().isClientSide()) {
            tickStabilizer();
        }
        super.tick();
        this.getCurrentWeapon().ifPresent(AbstractVehicleWeapon::tick);
        updateOBBs(yTurnUnitOBBs, false);
        updateOBBs(xTurnUnitOBBs, true);
    }



    @OnlyIn(Dist.CLIENT)
    public void tickStabilizer() {
        if (stabilizer && aimLockPosition != null) {
            BlockHitResult result = vehicle.level().clip(new ClipContext(worldPivotPosition(), aimLockPosition, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, vehicle));
            if (result.getType() != HitResult.Type.MISS) {
                aimLockEntity = null;
            }
            if (aimLockEntity != null) {
                if (!aimLockEntity.isAlive()) {
                    aimLockEntity = null;
                    stabilizer = false;
                    return;
                }
                AABB aabb = aimLockEntity.getBoundingBox();
                aimLockPosition = aabb.getCenter();
            }
            aim(aimLockPosition);
        }
    }

    @Override
    public List<VehicleBedrockCubeOBB> getUnitBedrockCubeOBBs() {
        List<VehicleBedrockCubeOBB> unitBedrockCubeOBBs = new ArrayList<>(yTurnUnitOBBs.size() + xTurnUnitOBBs.size());
        unitBedrockCubeOBBs.addAll(yTurnUnitOBBs);
        unitBedrockCubeOBBs.addAll(xTurnUnitOBBs);
        return unitBedrockCubeOBBs;
    }

    @Override
    public List<OBB> getOBBs() {
        List<OBB> unitOBBs = new ArrayList<>(yTurnUnitOBBs.size() + xTurnUnitOBBs.size());
        yTurnUnitOBBs.forEach(unitOBB -> unitOBBs.add(unitOBB.obb()));
        xTurnUnitOBBs.forEach(unitOBB -> unitOBBs.add(unitOBB.obb()));
        return unitOBBs;
    }

    public void updateOBBs(List<VehicleBedrockCubeOBB> unitOBBs, boolean isBarrel) {
        for (VehicleBedrockCubeOBB unitBedrockCubeOBB : unitOBBs) {
            OBB obb = unitBedrockCubeOBB.obb();
            Quaternionf rotSelf = new Quaternionf(unitBedrockCubeOBB.selfRot());
            rotSelf.rotateY(Math.toRadians(-combineYRot()));
            if (isBarrel) {
                Vec3 barrelCenterOffset = rotatedOffsetWithSelfRot(unitBedrockCubeOBB.offset());
                Vec3 barrelPivotOffset = rotatedOffsetWithSelfRot(new Vec3(unitBedrockCubeOBB.boneX / 16, unitBedrockCubeOBB.boneY / 16, unitBedrockCubeOBB.boneZ / 16));
                Vec3 rel = barrelCenterOffset.subtract(barrelPivotOffset);
                double len = rel.length();
                float xRotR = Math.toRadians(xRot);
                double cos = Math.cos(xRotR);
                double sin = Math.sin(xRotR);
                Vec3 v = rel.scale(cos);
                double x = v.x;
                double y = -len * sin;
                double z = v.z;
                obb.setCenter(vehicle.relativeRotPos(vehicle.position().add(new Vec3(x, y, z).add(barrelPivotOffset)), false).toVector3f());
                rotSelf.rotateX(Math.toRadians(180 + xRot));
            } else {
                obb.setCenter(worldPosition(unitBedrockCubeOBB.offset()).toVector3f());
            }
            obb.setRotation(vehicle.rotYXZ().mul(rotSelf));
        }
    }

    public void shoot(List<Vec3> ammoSpawnPositions, float ammoXRot, float ammoYRot) {
        this.getCurrentWeapon().ifPresent(weapon -> {
            weapon.shoot(ammoSpawnPositions, ammoXRot, ammoYRot, owner);
        });
    }

    public void aim(Vec3 worldPos) {
        Vec2 rot = aimRot(worldPos);
        if (xAimRot != rot.x || yAimRot != rot.y) {
            xAimRot = rot.x;
            yAimRot = rot.y;
            if (vehicle.level().isClientSide) {
                ClientVehicleAction control = new ClientVehicleAction();
                control.vehicleEntityId = vehicle.getId();
                control.partUnitIndex = index;
                control.xAimRot = rot.x;
                control.yAimRot = rot.y;
                Channel.CHANNEL.sendToServer(control);
            }
        }
        subWeaponUnits.forEach(weaponUnit -> weaponUnit.aim(worldPos));
    }

    public Vec2 aimRot(Vec3 worldPos) {
        Vec3 pivotWorldPos = worldPivotPosition();
        Vec3 worldAim = new Vec3(worldPos.x - pivotWorldPos.x, worldPos.y - pivotWorldPos.y, worldPos.z - pivotWorldPos.z);
        return vecToRot(worldAim);
    }

    public Vec3 aimHitPosition() {
        Vec3 start = worldPivotPosition();
        Vec3 direction = worldVec().normalize();
        Vec3 end = start.add(direction.scale(256));
        return VectorUtil.hitPosition(vehicle, start, end);
    }

    public List<Vec3> ammoSpawnPositions() {
        List<Vec3> positions = new ArrayList<>();
        for (int boltIndex = 0; boltIndex < bolts.size(); boltIndex += 1) {
            positions.add(ammoSpawnPosition(boltIndex));
        }
        return positions;
    }

    public Vec3 ammoSpawnPosition() {
        return ammoSpawnPosition(currentBoltIndex);
    }

    public Vec3 ammoSpawnPosition(int boltIndex) {
        Bolt bolt = bolts.get(boltIndex < 0 || boltIndex >= bolts.size() ? currentBoltIndex : boltIndex);
        Vec3 barrelOffset = worldVec().normalize().scale(bolt.barrelLength());
        return worldPosition(pivotOffset.add(bolt.offset())).add(barrelOffset);
    }

    public Vec3 worldPivotPosition() {
        Vector4d offset = rotatedOffsetWithBaseRot(this, this.pivotOffset.x, this.pivotOffset.z);
        Vec3 boltPosition = vehicle.position().add(new Vec3(offset.x, pivotOffset.y, offset.y));
        return vehicle.relativeRotPos(boltPosition, false);
    }

    public Vec3 worldOpticalSightPosition() {
        if (opticalSightOffset == null) {
            return worldOwnerViewPosition();
        }
        return worldPosition(pivotOffset.add(opticalSightOffset));
    }

    @Override
    public Vec3 worldOwnerViewPosition() {
        if (!operatorOnWeaponUnit) {
            Vec3 offsetFromVehicle = pivotOffset.add(operatorViewOffset);
            Vector4d offsetWithBaseRot = rotatedOffsetWithBaseRot(this, offsetFromVehicle.x, offsetFromVehicle.z);
            return vehicle.relativeRotPos(vehicle.position().add(new Vec3(offsetWithBaseRot.z, offsetFromVehicle.y, offsetWithBaseRot.w)), false);
        }
        if (operatorViewOffset == null) {
            float eyeHeight = owner == null ? 2 : owner.getEyeHeight();
            return worldPosition(pivotOffset.add(new Vec3(0, eyeHeight, 0)));
        }
        return worldPosition(pivotOffset.add(operatorViewOffset));
    }

    @Override
    public Vec3 worldSeatPosition() {
        float eyeHeight = owner == null ? 2 : owner.getEyeHeight();
        if (!operatorOnWeaponUnit) {
            Vector4d offset = rotatedOffsetWithBaseRot(this, seatOffset.x, seatOffset.z);
            return vehicle.relativeRotPos(vehicle.position().add(offset.z, seatOffset.y - eyeHeight, offset.w), false);
        }
        Vec3 offset = rotatedOffsetWithSelfRot(this.seatOffset);
        return vehicle.relativeRotPos(vehicle.position().add(offset.x, seatOffset.y - eyeHeight, offset.z), false);
    }

    /**
     * 计算车身、武器、附着武器都未旋转时某相对于载具枢轴的偏移xyz在经由车身、武器、附着武器旋转后的实际世界坐标
     */
    @Override
    public Vec3 worldPosition(Vec3 offsetFromVehicle) {
        if (offsetFromVehicle == null) {
            return vehicle.position();
        }
        return vehicle.relativeRotPos(vehicle.position().add(rotatedOffsetWithSelfRot(offsetFromVehicle)), false);
    }

    public Vec2 worldRot() {
        return worldRot(getXRot(), getYRot());
    }

    public Vec2 worldRot(float xRot, float yRot) {
        Vec3 worldVec = vehicle.relativeRotDirection(VectorUtil.calculateViewVector(xRot,
                (baseWeaponUnit != null ? baseWeaponUnit.combineYRot() : 0) + yRot),
                false);
        float pitch = (float) Math.toDegrees(Math.atan2(-worldVec.y, Math.sqrt(worldVec.x * worldVec.x + worldVec.z * worldVec.z)));
        float yaw = (float) Math.toDegrees(-Math.atan2(worldVec.x, worldVec.z));
        return new Vec2(pitch, yaw);
    }

    public Vec3 worldVec() {
        Vec2 rot = worldRot();
        return VectorUtil.calculateViewVector(rot.x, rot.y);
    }

    public Vec2 vecToRot(Vec3 worldVec) {
        Vec3 vehicleVec = vehicle.relativeRotDirection(worldVec, true);
        float pitch = (float) Math.toDegrees(Math.atan2(-vehicleVec.y, Math.sqrt(worldVec.x * worldVec.x + worldVec.z * worldVec.z)));
        float yaw = (float) Math.toDegrees(-Math.atan2(vehicleVec.x, vehicleVec.z));
        yaw -= combineYRot() - getYRot();
        return new Vec2(pitch, yaw);
    }

    public float combineYRot() {
        if (baseWeaponUnit == null) {
            return getYRot();
        }
        return getYRot() + baseWeaponUnit.combineYRot();
    }

    /**
     * 多层武器站发生依次旋转，计算某相对于载具枢轴的偏移xz因其中某层武器下所有武器旋转而所在的新偏移x'z'
     * @param weaponUnit 目标层武器
     * @param offsetX 相对于载具枢轴的偏移x
     * @param offsetZ 相对于载具枢轴的偏移z
     * @return 下一层武器的枢轴偏移xz，本层计算得新偏移xz
     */
    private Vector4d rotatedOffsetWithBaseRot(WeaponUnit weaponUnit, double offsetX, double offsetZ) {
        if (weaponUnit.baseWeaponUnit == null) {
            return new Vector4d(weaponUnit.pivotOffset.x, weaponUnit.pivotOffset.z, offsetX, offsetZ);
        }
        Vector4d pivotAndTargetOffset = rotatedOffsetWithBaseRot(weaponUnit.baseWeaponUnit, offsetX, offsetZ);
        float rot = Math.toRadians(weaponUnit.baseWeaponUnit.getYRot());
        float cos = Math.cos(rot);
        float sin = Math.sin(rot);
        float dx1 = (float) (weaponUnit.pivotOffset.x - pivotAndTargetOffset.x);
        float dy1 = (float) (weaponUnit.pivotOffset.z - pivotAndTargetOffset.y);
        float dx2 = (float) (pivotAndTargetOffset.z - pivotAndTargetOffset.x);
        float dy2 = (float) (pivotAndTargetOffset.w - pivotAndTargetOffset.y);
        return new Vector4d(pivotAndTargetOffset.x + dx1 * cos - dy1 * sin,
                pivotAndTargetOffset.y + dx1 * sin + dy1 * cos,
                pivotAndTargetOffset.x + dx2 * cos - dy2 * sin,
                pivotAndTargetOffset.y + dx2 * sin + dy2 * cos);
    }

    protected Vec3 rotatedOffsetWithSelfRot(Vec3 offsetFromVehicle) {
        Vector4d offset = rotatedOffsetWithBaseRot(this, offsetFromVehicle.x, offsetFromVehicle.z);
        float rot = Math.toRadians(getYRot());
        float cos = Math.cos(rot);
        float sin = Math.sin(rot);
        float dx = (float) (offset.z - offset.x);
        float dy = (float) (offset.w - offset.y);
        return new Vec3(offset.x + dx * cos - dy * sin, offsetFromVehicle.y, offset.y + dx * sin + dy * cos);
    }

    public List<Bolt> getBolts() {
        return bolts;
    }

    public FiringMode getFiringMode() {
        return firingMode;
    }

    public void setFiringMode(FiringMode firingMode) {
        this.firingMode = firingMode;
    }

    public void countFire(int times) {
        this.currentBoltIndex = (this.currentBoltIndex + times) % bolts.size();
    }

    public OpticalSightType getOpticalSightType() {
        return opticalSightType;
    }

    public float getZoom() {
        return zoom;
    }

    public void switchZoom() {
        if (zoom == zoomMax) {
            zoom = 1;
        } else {
            zoom = zoomMax;
        }
    }

    public boolean isStabilizerOn() {
        return stabilizer;
    }

    @OnlyIn(Dist.CLIENT)
    public void switchStabilizer() {
        stabilizer = !stabilizer;
        if (stabilizer) {
            fireControlLock();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void fireControlLock() {
        if (aimLockEntity != null) {
            aimLockEntity = null;
            stabilizer = false;
            return;
        }
        if (LocalVehiclePlayer.instance.viewType == LocalVehiclePlayer.ViewType.THIRD_PERSON
                || LocalVehiclePlayer.instance.viewType == LocalVehiclePlayer.ViewType.OPERATOR) {
            aimLockPosition = LocalVehiclePlayer.instance.cameraAimHit(-LocalVehiclePlayer.CAMERA_UPWARD_ANGLE, 0);
        } else if (LocalVehiclePlayer.instance.viewType == LocalVehiclePlayer.ViewType.SCOPE) {
            aimLockPosition = LocalVehiclePlayer.instance.cameraAimHit(0, 0);
        }
        List<Entity> entities = vehicle.level().getEntities(vehicle, AABB.ofSize(aimLockPosition, 8, 8, 8));
        entities = entities.stream()
                .filter(entity -> !entity.isSpectator() && !vehicle.getPassengers().contains(entity))
                .toList();
        if (!entities.isEmpty()) {
            aimLockEntity = entities.get(0);
            stabilizer = true;
        } else {
            aimLockEntity = null;
        }
    }

    public Vec3 getAimLockPosition() {
        return aimLockPosition;
    }

    public void setAimLockPosition(Vec3 aimLockPosition) {
        this.aimLockPosition = aimLockPosition;
    }

    public Entity getAimLockEntity() {
        return aimLockEntity;
    }

    @Override
    public LivingEntity getOwner() {
        if (parentWeaponUnit != null) {
            return parentWeaponUnit.owner;
        }
        return owner;
    }

    public boolean isOperatorOnWeaponUnit() {
        return operatorOnWeaponUnit;
    }

    public void setOperatorOnWeaponUnit(boolean operatorOnWeaponUnit) {
        this.operatorOnWeaponUnit = operatorOnWeaponUnit;
    }

    public WeaponUnit getParentWeaponUnit() {
        return parentWeaponUnit;
    }

    public void setParentWeaponUnit(WeaponUnit parentWeaponUnit) {
        this.parentWeaponUnit = parentWeaponUnit;
    }

    public void addSubWeaponUnit(WeaponUnit subWeaponUnit) {
        this.subWeaponUnits.add(subWeaponUnit);
    }

    public List<WeaponUnit> getSubWeaponUnits() {
        return subWeaponUnits;
    }

    public WeaponUnit getBaseWeaponUnit() {
        return baseWeaponUnit;
    }

    public void setBaseWeaponUnit(WeaponUnit baseWeaponUnit) {
        this.baseWeaponUnit = baseWeaponUnit;
    }

    public Optional<AbstractVehicleWeapon<?>> getCurrentWeapon() {
        if (currentWeaponIndex >= 0 && currentWeaponIndex < weapons.size()) {
            return Optional.of(weapons.get(currentWeaponIndex));
        }
        return Optional.empty();
    }

    public void setCurrentWeaponIndex(int index) {
        this.currentWeaponIndex = index;
    }

    public int getCurrentWeaponIndex() {
        return currentWeaponIndex;
    }

}
