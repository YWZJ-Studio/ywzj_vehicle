package org.ywzj.vehicle.vehicle;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import org.joml.Vector2f;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.weapon.BulletEntity;
import org.ywzj.vehicle.network.Channel;
import org.ywzj.vehicle.network.message.ClientWeaponUnitControl;
import org.ywzj.vehicle.network.message.ServerWeaponUnitRot;
import org.ywzj.vehicle.util.EntityUtil;
import org.ywzj.vehicle.util.VectorUtil;

import java.util.function.Supplier;

public class WeaponUnit {

    private final String name;
    private final int index;
    private final AbstractVehicle vehicle;
    private final Vec3 boltOffset;
    private final float barrelLength;
    private LivingEntity operator;
    public float xAimRot;
    public float yAimRot;
    public float xRot;
    public float yRot;
    public float xRotO;
    public float yRotO;
    public float xRotSpeed;
    public float yRotSpeed;
    public float xRotMax;
    public float xRotMin;

    public WeaponUnit(String name, int index, AbstractVehicle vehicle, Vec3 boltOffset, float barrelLength) {
        this.name = name;
        this.index = index;
        this.vehicle = vehicle;
        this.boltOffset = boltOffset;
        this.barrelLength = barrelLength;
    }

    public Vec3 boltPosition() {
        return vehicle.relativeRotPos(vehicle.position().add(boltOffset));
    }

    public Vec3 ammoSpawnPosition() {
        Vec3 barrelOffset = VectorUtil.calculateViewVector(xRot, yRot).normalize().scale(barrelLength);
        Vec3 basePos = vehicle.position().add(boltOffset).add(barrelOffset);
        return vehicle.relativeRotPos(basePos);
    }

    public Vector2f worldRot() {
        return toWorldRot(xRot, yRot);
    }

    private Vector2f toWorldRot(float xRot, float yRot) {
        Vec3 v = vehicle.relativeRotDirection(VectorUtil.calculateViewVector(xRot, yRot), false);
        float yaw = (float) Math.toDegrees(Math.atan2(v.x, v.z));
        float pitch = (float) Math.toDegrees(Math.atan2(-v.y, Math.hypot(v.x, v.z)));
        return new Vector2f(pitch, yaw);
    }

    public void shoot(Vec3 ammoSpawnPosition, float ammoXRot, float ammoYRot) {
        BulletEntity bulletEntity = new BulletEntity(vehicle.level(), operator, ammoSpawnPosition);
        bulletEntity.shootFromRotation(vehicle, ammoXRot, ammoYRot, 0, 10.0f, 1f);
        bulletEntity.setDamage(25);
        bulletEntity.setHeadShot(1.5f);
        vehicle.level().addFreshEntity(bulletEntity);
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

    public int getIndex() {
        return index;
    }

    public AbstractVehicle getVehicle() {
        return vehicle;
    }

}
