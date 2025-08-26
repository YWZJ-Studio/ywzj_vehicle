package org.ywzj.vehicle.vehicle;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import org.joml.Vector2f;
import org.joml.Vector4d;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.weapon.BulletEntity;
import org.ywzj.vehicle.network.Channel;
import org.ywzj.vehicle.network.message.ClientWeaponUnitControl;
import org.ywzj.vehicle.network.message.ServerWeaponUnitRot;
import org.ywzj.vehicle.util.EntityUtil;
import org.ywzj.vehicle.util.VectorUtil;

import java.util.function.Supplier;

public class WeaponUnit {

    private final Component name;
    private final int index;
    private final AbstractVehicle vehicle;
    private final Vec3 boltOffset;
    private final float barrelLength;
    private final Vec3 operatorOffset;
    private final WeaponUnit baseWeaponUnit;
    private LivingEntity operator;
    public float xAimRot;
    public float yAimRot;
    public float xRot; // 在载具坐标系下的高低机轴
    public float yRot; // 在载具或附着武器坐标系下的方向机轴
    public float xRotO;
    public float yRotO;
    public float xRotSpeed;
    public float yRotSpeed;
    public float xRotMax;
    public float xRotMin;

    public WeaponUnit(String name, int index, AbstractVehicle vehicle, Vec3 boltOffset, float barrelLength, Vec3 operatorOffset, WeaponUnit baseWeaponUnit) {
        this.name = Component.translatable(name);
        this.index = index;
        this.vehicle = vehicle;
        // 炮闩偏移，为武器枢轴相对于载具枢轴的偏移
        this.boltOffset = boltOffset;
        // 炮管长度，为发射物生成位置与炮闩位置的距离
        this.barrelLength = barrelLength;
        // 武器操作员偏移，为操作玩家的摄像机相对于炮闩的偏移
        this.operatorOffset = operatorOffset;
        // 本武器所附着于的武器
        this.baseWeaponUnit = baseWeaponUnit;
    }

    public void shoot(Vec3 ammoSpawnPosition, float ammoXRot, float ammoYRot) {
        BulletEntity bulletEntity = new BulletEntity(vehicle.level(), operator, ammoSpawnPosition);
        bulletEntity.shootFromRotation(vehicle, ammoXRot, ammoYRot, 0, 10.0f, 1f);
        bulletEntity.setDamage(25);
        bulletEntity.setHeadShot(1.5f);
        vehicle.level().addFreshEntity(bulletEntity);
    }

    public Vec2 aim(Vec3 worldPos) {
        Vec3 breechBoltWorldPos = worldBoltPosition();
        Vec3 worldAim = new Vec3(worldPos.x - breechBoltWorldPos.x, worldPos.y - breechBoltWorldPos.y, worldPos.z - breechBoltWorldPos.z);
        return vecToRot(worldAim);
    }

    public Vec3 ammoSpawnPosition() {
        Vector2f rot = worldRot();
        Vec3 barrelOffset = VectorUtil.calculateViewVector(rot.x, rot.y).normalize().scale(barrelLength);
        return worldBoltPosition().add(barrelOffset);
    }

    public Vec3 worldBoltPosition() {
        Vector4d offset = rotatedOffset(this, this.boltOffset.x, this.boltOffset.z);
        Vec3 boltPosition = vehicle.position().add(new Vec3(offset.x, boltOffset.y, offset.y));
        return vehicle.relativeRotPos(boltPosition);
    }

    public Vec3 worldOperatorPosition() {
        if (operatorOffset == null) {
            return worldBoltPosition().add(vehicle.getCameraOffset());
        }
        Vector4d offset = rotatedOffset(this, boltOffset.x + operatorOffset.x, boltOffset.z + operatorOffset.z);
        float rot = yRot;
        float cos = (float) Math.cos(Math.toRadians(rot));
        float sin = (float) Math.sin(Math.toRadians(rot));
        float dx = (float) (offset.z - offset.x);
        float dy = (float) (offset.w - offset.y);
        Vec3 operatorPosition = vehicle.position().add(new Vec3(offset.x + dx * cos - dy * sin, boltOffset.y + operatorOffset.y, offset.y + dx * sin + dy * cos));
        return vehicle.relativeRotPos(operatorPosition);
    }

    public Vector2f worldRot() {
        Vec3 worldVec = vehicle.relativeRotDirection(VectorUtil.calculateViewVector(xRot, yRot + (baseWeaponUnit == null ? 0 : baseWeaponUnit.yRot)), false);
        float pitch = (float) Math.toDegrees(Math.atan2(-worldVec.y, Math.hypot(worldVec.x, worldVec.z)));
        float yaw = (float) Math.toDegrees(Math.atan2(worldVec.x, worldVec.z));
        return new Vector2f(pitch, yaw);
    }

    public Vec2 vecToRot(Vec3 worldVec) {
        Vec3 vehicleVec = vehicle.relativeRotDirection(worldVec, true);
        float pitch = (float) Math.toDegrees(Math.atan2(-vehicleVec.y, Math.hypot(vehicleVec.x, vehicleVec.z)));
        float yaw = (float) Math.toDegrees(Math.atan2(vehicleVec.x, vehicleVec.z));
        yaw -= baseWeaponUnit == null ? 0 : baseWeaponUnit.yRot;
        return new Vec2(pitch, yaw);
    }

    /**
     * 多层武器站发生依次旋转，计算某相对于载具枢轴的偏移xz因其中某层武器下所有武器旋转而所在的新偏移x'z'
     * @param weaponUnit 目标层武器
     * @param offsetX 相对于载具枢轴的偏移x
     * @param offsetZ 相对于载具枢轴的偏移z
     * @return 下一层武器的枢轴偏移xz，本层计算得新偏移xz
     */
    private Vector4d rotatedOffset(WeaponUnit weaponUnit, double offsetX, double offsetZ) {
        if (weaponUnit.baseWeaponUnit == null) {
            return new Vector4d(weaponUnit.boltOffset.x, weaponUnit.boltOffset.z, offsetX, offsetZ);
        }
        Vector4d pivotAndTargetOffset = rotatedOffset(weaponUnit.baseWeaponUnit, offsetX, offsetZ);
        float rot = weaponUnit.baseWeaponUnit.yRot;
        float cos = (float) Math.cos(Math.toRadians(rot));
        float sin = (float) Math.sin(Math.toRadians(rot));
        float dx1 = (float) (weaponUnit.boltOffset.x - pivotAndTargetOffset.x);
        float dy1 = (float) (weaponUnit.boltOffset.z - pivotAndTargetOffset.y);
        float dx2 = (float) (pivotAndTargetOffset.z - pivotAndTargetOffset.x);
        float dy2 = (float) (pivotAndTargetOffset.w - pivotAndTargetOffset.y);
        return new Vector4d(pivotAndTargetOffset.x + dx1 * cos - dy1 * sin,
                pivotAndTargetOffset.y + dx1 * sin + dy1 * cos,
                pivotAndTargetOffset.x + dx2 * cos - dy2 * sin,
                pivotAndTargetOffset.y + dx2 * sin + dy2 * cos);
    }

    public void setOperator(LivingEntity operator) {
        this.operator = operator;
    }

    public void tick() {
        this.xRotO = this.xRot;
        this.yRotO = this.yRot;

        float xDiff = Mth.wrapDegrees(this.xAimRot - this.xRot);
        float yDiff = Mth.wrapDegrees(this.yAimRot - this.yRot);

        if (Math.abs(xDiff) > xRotSpeed) {
            this.xRot += Math.signum(xDiff) * xRotSpeed;
        } else {
            this.xRot = this.xAimRot;
        }
        this.xRot = Math.max(Math.min(this.xRot, xRotMax), xRotMin);

        if (Math.abs(yDiff) > yRotSpeed) {
            this.yRot += Math.signum(yDiff) * yRotSpeed;
        } else {
            this.yRot = this.yAimRot;
        }

        if (!vehicle.level().isClientSide()) {
            if (xDiff != 0 || yDiff != 0) {
                vehicle.level().players().stream()
                        .filter(player -> EntityUtil.withinBroadcastRange(vehicle, player) && vehicle.getOwnWeaponUnit(player) != this)
                        .forEach(player ->
                                Channel.CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), new ServerWeaponUnitRot(this)));
            }
        }
    }

    public static void onClientMessageReceived(ClientWeaponUnitControl message, Supplier<NetworkEvent.Context> ctxSupplier) {
        if (ctxSupplier.get().getSender() != null) {
            Level level = ctxSupplier.get().getSender().level();
            Entity entity = level.getEntity(message.vehicleEntityId);
            if (entity instanceof AbstractVehicle vehicle) {
                if (message.weaponIndex < vehicle.weaponUnits.size()) {
                    if (message.shoot) {
                        vehicle.shoot(message.weaponIndex, new Vec3(message.ammoX, message.ammoY, message.ammoZ), message.ammoXRot, message.ammoYRot);
                    } else {
                        WeaponUnit serverWeaponUnit = vehicle.weaponUnits.get(message.weaponIndex);
                        serverWeaponUnit.xAimRot = message.xAimRot;
                        serverWeaponUnit.yAimRot = message.yAimRot % 360;
                    }
                }
            }
        }
    }

    public Component getName() {
        return name;
    }

    public int getIndex() {
        return index;
    }

    public AbstractVehicle getVehicle() {
        return vehicle;
    }

}
