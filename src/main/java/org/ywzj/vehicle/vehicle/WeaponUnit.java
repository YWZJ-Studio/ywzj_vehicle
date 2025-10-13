package org.ywzj.vehicle.vehicle;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Math;
import org.joml.Quaternionf;
import org.joml.Vector4d;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.custom.VehicleWeaponManager;
import org.ywzj.vehicle.custom.vehicle.WeaponUnitData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.misc.weapon.AbstractVehicleWeapon;
import org.ywzj.vehicle.util.VectorUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 载具武器站，对应一个座位<br/>
 * 不包含实际的武器实现
 */
public class WeaponUnit extends PartUnit {

    private List<VehicleBedrockCubeOBB> yTurnUnitOBBs = List.of();
    private List<VehicleBedrockCubeOBB> xTurnUnitOBBs = List.of();
    private float zoomMax;
    private float zoom;
    private Vec3 boltOffset;
    private float barrelLength;
    private final Vec3 operatorOffset;
    private Vec3 seatOffset;
    private WeaponUnit baseWeaponUnit;

    private final List<AbstractVehicleWeapon<?>> weapons = new ArrayList<>();
    private int currentWeaponIndex = -1;

    public WeaponUnit(WeaponUnitData data, int index, AbstractVehicle vehicle) {
        super(Component.translatable(data.getName()), index, vehicle);
        this.boltOffset = data.getBoltOffset();
        this.barrelLength = data.getBarrelLength();
        this.operatorOffset = data.getOperatorOffset();
        this.seatOffset = data.getSeatOffset();
        this.yTurnUnitOBBs = data.getYTurnUnitOBBs();
        this.xTurnUnitOBBs = data.getXTurnUnitOBBs();
        var rotInfo = data.getRotInfo();
        this.xRotSpeed = rotInfo.getXRotSpeed();
        this.yRotSpeed = rotInfo.getYRotSpeed();
        this.xRotMax = rotInfo.getXRotMax();
        this.xRotMin = rotInfo.getXRotMin();

        int cnt = 0;
        for (var weaponRes : data.getWeapons()) {
            VehicleWeaponManager.get().getIndex(weaponRes).ifPresent(
                    i -> weapons.add(i.create(vehicle, cnt))
            );
        }
        if (!weapons.isEmpty()) {
            currentWeaponIndex = Math.min(cnt, weapons.size() - 1);
        }
    }

    @Deprecated
    public WeaponUnit(String name, int index, AbstractVehicle vehicle, Vec3 boltOffset, float barrelLength,
                      Vec3 operatorOffset, Vec3 seatOffset, WeaponUnit baseWeaponUnit) {
        super(name, index, vehicle);

        this.zoomMax = 2;
        this.zoom = 1;

        if (this.boltOffset == null) {
            // 炮闩偏移，为武器枢轴相对于载具枢轴的偏移
            this.boltOffset = boltOffset;
        }
        if (this.barrelLength == 0) {
            // 炮管长度，为发射物生成位置与炮闩位置的距离
            this.barrelLength = barrelLength;
        }
        // 武器站操作员镜头偏移，为操作玩家的摄像机相对于炮闩的偏移
        this.operatorOffset = operatorOffset;
        // 武器站操作员座位偏移，为操作玩家的乘坐位置相对于炮闩的偏移
        this.seatOffset = seatOffset;
        // 本武器站所附着于的武器站
        this.baseWeaponUnit = baseWeaponUnit;

        VehicleWeaponManager.get().getIndex(new ResourceLocation(YwzjVehicle.MOD_ID, "cannon")).ifPresent(
                i -> weapons.add(i.create(vehicle, 0))
        );
        currentWeaponIndex = 0;
    }

    public Optional<AbstractVehicleWeapon<?>> getCurrentWeapon() {
        if (currentWeaponIndex >= 0 && currentWeaponIndex < weapons.size()) {
            return Optional.of(weapons.get(currentWeaponIndex));
        }
        return Optional.empty();
    }

    public void setBaseWeaponUnit(WeaponUnit baseWeaponUnit) {
        this.baseWeaponUnit = baseWeaponUnit;
    }

    @Override
    public void tick() {
        super.tick();
        this.getCurrentWeapon().ifPresent(AbstractVehicleWeapon::tick);
        updateOBBs(yTurnUnitOBBs, false);
        updateOBBs(xTurnUnitOBBs, true);
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
                obb.setCenter(vehicle.relativeRotPos(vehicle.position().add(new Vec3(x, y, z).add(barrelPivotOffset))).toVector3f());
                rotSelf.rotateX(Math.toRadians(180 + xRot));
            } else {
                obb.setCenter(worldPosition(unitBedrockCubeOBB.offset()).toVector3f());
            }
            obb.setRotation(vehicle.rotYXZ().mul(rotSelf));
        }
    }

    public void shoot(Vec3 ammoSpawnPosition, float ammoXRot, float ammoYRot) {
        shoot(ammoSpawnPosition, ammoXRot, ammoYRot, false);
    }

    public void shoot(Vec3 ammoSpawnPosition, float ammoXRot, float ammoYRot, boolean explosion) {
        this.getCurrentWeapon().ifPresent(weapon -> {
            weapon.shoot(ammoSpawnPosition, ammoXRot, ammoYRot, operator);
        });
    }

    public Vec2 aim(Vec3 worldPos) {
        Vec3 boltWorldPos = worldBoltPosition();
        Vec3 worldAim = new Vec3(worldPos.x - boltWorldPos.x, worldPos.y - boltWorldPos.y, worldPos.z - boltWorldPos.z);
        return vecToRot(worldAim);
    }

    public Vec3 ammoSpawnPosition() {
        Vec2 rot = worldRot();
        Vec3 barrelOffset = VectorUtil.calculateViewVector(rot.x, rot.y).normalize().scale(barrelLength);
        return worldBoltPosition().add(barrelOffset);
    }

    public Vec3 worldBoltPosition() {
        Vector4d offset = rotatedOffsetWithBaseRot(this, this.boltOffset.x, this.boltOffset.z);
        Vec3 boltPosition = vehicle.position().add(new Vec3(offset.x, boltOffset.y, offset.y));
        return vehicle.relativeRotPos(boltPosition);
    }

    public Vec3 worldOperatorPosition() {
        if (operatorOffset == null) {
            float eyeHeight = operator == null ? 2 : operator.getEyeHeight();
            return worldPosition(boltOffset.add(new Vec3(0, eyeHeight, 0)));
        }
        return worldPosition(operatorOffset.add(boltOffset));
    }

    public Vec3 worldSeatPosition() {
        float eyeHeight = operator == null ? 2 : operator.getEyeHeight();
        Vec3 seatOffset = this.seatOffset;
        if (seatOffset == null) {
            seatOffset = boltOffset.subtract(new Vec3(0, eyeHeight, 0));
        }
        Vector4d offset = rotatedOffsetWithBaseRot(this, boltOffset.x + seatOffset.x, boltOffset.z + seatOffset.z);
        return vehicle.relativeRotPos(vehicle.position().add(offset.z, boltOffset.y + seatOffset.y - eyeHeight, offset.w));
    }

    /**
     * 计算车身、武器、附着武器都未旋转时某相对于载具枢轴的偏移xyz在经由车身、武器、附着武器旋转后的实际世界坐标
     */
    public Vec3 worldPosition(Vec3 offsetFromVehicle) {
        if (offsetFromVehicle == null) {
            return vehicle.position();
        }
        return vehicle.relativeRotPos(vehicle.position().add(rotatedOffsetWithSelfRot(offsetFromVehicle)));
    }

    public Vec2 worldRot() {
        return worldRot(xRot, baseWeaponUnit != null ? baseWeaponUnit.combineYRot() : 0 + yRot);
    }

    public Vec2 worldRot(float xRot, float yRot) {
        Vec3 worldVec = vehicle.relativeRotDirection(VectorUtil.calculateViewVector(xRot, baseWeaponUnit != null ? baseWeaponUnit.combineYRot() : 0 + yRot), false);
        float pitch = (float) Math.toDegrees(Math.atan2(-worldVec.y, Math.sqrt(worldVec.x * worldVec.x + worldVec.z * worldVec.z)));
        float yaw = (float) Math.toDegrees(-Math.atan2(worldVec.x, worldVec.z));
        return new Vec2(pitch, yaw);
    }

    public Vec2 vecToRot(Vec3 worldVec) {
        Vec3 vehicleVec = vehicle.relativeRotDirection(worldVec, true);
        float pitch = (float) Math.toDegrees(Math.atan2(-vehicleVec.y, Math.sqrt(worldVec.x * worldVec.x + worldVec.z * worldVec.z)));
        float yaw = (float) Math.toDegrees(-Math.atan2(vehicleVec.x, vehicleVec.z));
        yaw -= combineYRot() - yRot;
        return new Vec2(pitch, yaw);
    }

    public float combineYRot() {
        if (baseWeaponUnit == null) {
            return yRot;
        }
        return yRot + baseWeaponUnit.combineYRot();
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
            return new Vector4d(weaponUnit.boltOffset.x, weaponUnit.boltOffset.z, offsetX, offsetZ);
        }
        Vector4d pivotAndTargetOffset = rotatedOffsetWithBaseRot(weaponUnit.baseWeaponUnit, offsetX, offsetZ);
        float rot = weaponUnit.baseWeaponUnit.yRot;
        float cos = Math.cos(Math.toRadians(rot));
        float sin = Math.sin(Math.toRadians(rot));
        float dx1 = (float) (weaponUnit.boltOffset.x - pivotAndTargetOffset.x);
        float dy1 = (float) (weaponUnit.boltOffset.z - pivotAndTargetOffset.y);
        float dx2 = (float) (pivotAndTargetOffset.z - pivotAndTargetOffset.x);
        float dy2 = (float) (pivotAndTargetOffset.w - pivotAndTargetOffset.y);
        return new Vector4d(pivotAndTargetOffset.x + dx1 * cos - dy1 * sin,
                pivotAndTargetOffset.y + dx1 * sin + dy1 * cos,
                pivotAndTargetOffset.x + dx2 * cos - dy2 * sin,
                pivotAndTargetOffset.y + dx2 * sin + dy2 * cos);
    }

    private Vec3 rotatedOffsetWithSelfRot(Vec3 offsetFromVehicle) {
        Vector4d offset = rotatedOffsetWithBaseRot(this, offsetFromVehicle.x, offsetFromVehicle.z);
        float rot = yRot;
        float cos = Math.cos(Math.toRadians(rot));
        float sin = Math.sin(Math.toRadians(rot));
        float dx = (float) (offset.z - offset.x);
        float dy = (float) (offset.w - offset.y);
        return new Vec3(offset.x + dx * cos - dy * sin, offsetFromVehicle.y, offset.y + dx * sin + dy * cos);
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

    public void setSeatOffset(Vec3 seatOffset) {
        this.seatOffset = seatOffset;
    }

}
