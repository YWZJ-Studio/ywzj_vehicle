package org.ywzj.vehicle.vehicle;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Math;
import org.joml.Quaternionf;
import org.joml.Vector4d;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.custom.VehicleWeaponManager;
import org.ywzj.vehicle.custom.vehicle.WeaponUnitData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.misc.weapon.AbstractVehicleWeapon;
import org.ywzj.vehicle.network.Channel;
import org.ywzj.vehicle.network.message.ClientVehicleAction;
import org.ywzj.vehicle.util.VectorUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 武器站是一种有方向机与高低机，可发射多类武器的载具可动部件
 * 其中方向机与高低机的结构模型可相互独立运动
 * 多个武器站可以纵向堆叠，并在方向机上联动旋转
 */
public class WeaponUnit extends RotatableUnit {

    // 炮闩偏移，为武器枢轴相对于载具枢轴的偏移
    private final Vec3 boltOffset;
    // 炮管长度，为发射物生成位置与炮闩位置的距离
    private final float barrelLength;
    // 武器站光瞄偏移，为开镜视角下玩家的摄像机相对于炮闩的偏移
    private final Vec3 opticalSightOffset;
    // 武器站操作员镜头偏移，为操作员视角下玩家的摄像机相对于炮闩的偏移
    private final Vec3 operatorOffset;
    // 武器站操作员座位偏移，为操作玩家的乘坐位置相对于炮闩的偏移
    private Vec3 seatOffset;
    // 开镜类型
    private final OpticalSightType opticalSightType;
    // 最大开镜缩放倍率
    private final float zoomMax;
    // 当前开镜缩放倍率
    private float zoom;
    // 本武器站所附着于的武器站
    private WeaponUnit baseWeaponUnit;

    private boolean stabilizer;
    private Vec3 aimLockPosition;
    private Entity aimLockEntity;

    private List<VehicleBedrockCubeOBB> yTurnUnitOBBs = List.of();
    private List<VehicleBedrockCubeOBB> xTurnUnitOBBs = List.of();
    private final List<AbstractVehicleWeapon<?>> weapons = new ArrayList<>();
    private int currentWeaponIndex = -1;

    public enum OpticalSightType {
        // 不能开镜
        NONE,
        // 以操作员视角开镜
        OPERATOR,
        // 以观瞄视角开镜（光学瞄具）
        OPTICAL_SCOPE,
        // 以观瞄视角开镜（模拟电视）
        CRT
    }

    public WeaponUnit(WeaponUnitData data, int index, AbstractVehicle vehicle) {
        super(Component.translatable(data.getName()), index, vehicle);
        this.boltOffset = data.getBoltOffset();
        this.barrelLength = data.getBarrelLength();
        this.opticalSightOffset = data.getOpticalSightOffset();
        this.operatorOffset = data.getOperatorOffset();
        this.seatOffset = data.getSeatOffset();

        this.yTurnUnitOBBs = data.getYTurnUnitOBBs();
        this.xTurnUnitOBBs = data.getXTurnUnitOBBs();
        var rotInfo = data.getRotInfo();
        this.xRotSpeed = rotInfo.getXRotSpeed();
        this.yRotSpeed = rotInfo.getYRotSpeed();
        this.xRotMax = rotInfo.getXRotMax();
        this.xRotMin = rotInfo.getXRotMin();

        this.zoomMax = 8;
        this.zoom = 1;
//        this.yRotMin = 0;
//        this.yRotMax = 0;
        this.opticalSightType = OpticalSightType.CRT;

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
    public WeaponUnit(String name, int index, AbstractVehicle vehicle,
                      Vec3 boltOffset, float barrelLength,
                      Vec3 opticalSightOffset, Vec3 operatorOffset, Vec3 seatOffset, WeaponUnit baseWeaponUnit) {
        super(name, index, vehicle);

        this.zoomMax = 8;
        this.zoom = 1;

        this.boltOffset = boltOffset;
        this.barrelLength = barrelLength;
        this.opticalSightOffset = opticalSightOffset;
        this.operatorOffset = operatorOffset;
        this.seatOffset = seatOffset;
        this.baseWeaponUnit = baseWeaponUnit;

        this.opticalSightType = OpticalSightType.CRT;

        VehicleWeaponManager.get().getIndex(new ResourceLocation(YwzjVehicle.MOD_ID, "cannon")).ifPresent(
                i -> weapons.add(i.create(vehicle, 0))
        );
        currentWeaponIndex = 0;
    }

    @Override
    public void tick() {
        if (vehicle.level().isClientSide) {
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
            BlockHitResult result = vehicle.level().clip(new ClipContext(worldBoltPosition(), aimLockPosition, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, vehicle));
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
            Vec2 rot = aim(aimLockPosition);
            if (xAimRot != rot.x || yAimRot != rot.y) {
                xAimRot = rot.x;
                yAimRot = rot.y;
                ClientVehicleAction control = new ClientVehicleAction();
                control.vehicleEntityId = vehicle.getId();
                control.weaponIndex = getIndex();
                control.xAimRot = rot.x;
                control.yAimRot = rot.y;
                Channel.CHANNEL.sendToServer(control);
            }
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
            weapon.shoot(ammoSpawnPosition, ammoXRot, ammoYRot, owner);
        });
    }

    public Vec2 aim(Vec3 worldPos) {
        Vec3 boltWorldPos = worldBoltPosition();
        Vec3 worldAim = new Vec3(worldPos.x - boltWorldPos.x, worldPos.y - boltWorldPos.y, worldPos.z - boltWorldPos.z);
        return vecToRot(worldAim);
    }

    public Vec3 aimHitPosition() {
        Vec3 start = worldBoltPosition();
        Vec3 end = start.add(worldVec().normalize().scale(256));
        return vehicle.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, vehicle)).getLocation();
    }

    public Vec3 ammoSpawnPosition() {
        Vec3 barrelOffset = worldVec().normalize().scale(barrelLength);
        return worldBoltPosition().add(barrelOffset);
    }

    public Vec3 worldBoltPosition() {
        Vector4d offset = rotatedOffsetWithBaseRot(this, this.boltOffset.x, this.boltOffset.z);
        Vec3 boltPosition = vehicle.position().add(new Vec3(offset.x, boltOffset.y, offset.y));
        return vehicle.relativeRotPos(boltPosition);
    }

    public Vec3 worldOpticalSightPosition() {
        if (opticalSightOffset == null) {
            return worldOwnerViewPosition();
        }
        return worldPosition(boltOffset.add(opticalSightOffset));
    }

    @Override
    public Vec3 worldOwnerViewPosition() {
        if (operatorOffset == null) {
            float eyeHeight = owner == null ? 2 : owner.getEyeHeight();
            return worldPosition(boltOffset.add(new Vec3(0, eyeHeight, 0)));
        }
        return worldPosition(boltOffset.add(operatorOffset));
    }

    @Override
    public Vec3 worldSeatPosition() {
        float eyeHeight = owner == null ? 2 : owner.getEyeHeight();
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
    @Override
    public Vec3 worldPosition(Vec3 offsetFromVehicle) {
        if (offsetFromVehicle == null) {
            return vehicle.position();
        }
        return vehicle.relativeRotPos(vehicle.position().add(rotatedOffsetWithSelfRot(offsetFromVehicle)));
    }

    public Vec2 worldRot() {
        return worldRot(xRot, yRot);
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

    public OpticalSightType getOpticalSightType() {
        return opticalSightType;
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
        if (LocalVehiclePlayer.instance.viewType == LocalVehiclePlayer.ViewType.THIRD_PERSON) {
            aimLockPosition = LocalVehiclePlayer.instance.cameraAimHit(-10, 0).getLocation();
        } else if (LocalVehiclePlayer.instance.viewType == LocalVehiclePlayer.ViewType.SCOPE) {
            aimLockPosition = LocalVehiclePlayer.instance.cameraAimHit(0, 0).getLocation();
        }
        Vec3 worldBoltPosition = worldBoltPosition();
        Vec3 aimDirection = aimLockPosition.subtract(worldBoltPosition);
        AABB aabb = vehicle.getBoundingBox()
                .expandTowards(aimDirection)
                .inflate(1.0D, 1.0D, 1.0D);
        EntityHitResult entityHitResult = ProjectileUtil.getEntityHitResult(vehicle, worldBoltPosition, aimLockPosition, aabb,
                entity -> !entity.isSpectator() && !vehicle.getPassengers().contains(entity),
                java.lang.Math.pow(Minecraft.getInstance().options.renderDistance().get() * 16, 2));
        if (entityHitResult != null) {
            aimLockEntity = entityHitResult.getEntity();
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

    public void setSeatOffset(Vec3 seatOffset) {
        this.seatOffset = seatOffset;
    }

}
