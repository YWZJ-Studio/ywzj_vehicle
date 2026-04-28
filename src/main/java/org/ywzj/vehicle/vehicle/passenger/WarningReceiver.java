package org.ywzj.vehicle.vehicle.passenger;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.audio.VehicleSound;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.network.message.ServerVehicleWarn;
import org.ywzj.vehicle.util.VectorUtil;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.pojo.WarnType;

import java.util.concurrent.ConcurrentHashMap;

public class WarningReceiver {

    public AbstractVehicle vehicle;
    public VehicleSound radarLockWarnSound;
    public VehicleSound missileLaunchWarnSound;
    public ConcurrentHashMap<Integer, WarnTarget> targets = new ConcurrentHashMap<>();

    public WarningReceiver(AbstractVehicle vehicle) {
        this.vehicle = vehicle;
    }

    @OnlyIn(Dist.CLIENT)
    public static void handle(ServerVehicleWarn message) {
        Level level = Minecraft.getInstance().level;
        if (level.getEntity(message.toEntityId) instanceof AbstractVehicle toVehicle
                && toVehicle.equals(LocalVehiclePlayer.instance.getVehicle())) {
            // RWR接收角度限制
            Entity entity = level.getEntity(message.fromEntityId);
            if (entity == null) {
                LocalVehiclePlayer.ServerEntity serverEntity = LocalVehiclePlayer.instance.serverEntities.get(message.fromEntityId);
                if (serverEntity == null || serverEntity.entity == null) {
                    return;
                }
                entity = serverEntity.entity;
            }
            Vec3 direction = toVehicle.relativeRotDirection(entity.position().subtract(toVehicle.position()), true);
            if (Math.abs(VectorUtil.vecToRot(direction).x) > 45) {
                return;
            }
            WarningReceiver warningReceiver = toVehicle.warningReceiver;
            if (warningReceiver != null) {
                if (message.warnType == WarnType.RADAR_SEARCH) {
                    WarnTarget warnTarget = warningReceiver.targets.get(message.fromEntityId);
                    if (warnTarget != null && warnTarget.warnType != WarnType.RADAR_SEARCH) {
                        return;
                    }
                    new VehicleSound(AllSounds.RADAR_SEARCH_WARN.get(),
                            4f, 1f, 1f,
                            false, 50, false, false, toVehicle.getId())
                            .play();
                    warningReceiver.targets.put(message.fromEntityId, new WarnTarget(message.warnType, message.info, System.currentTimeMillis()));
                }
                warningReceiver.targets.put(message.fromEntityId, new WarnTarget(message.warnType, message.info, System.currentTimeMillis()));
            }
        }
    }

    public void tick() {
        targets.values().removeIf(warnTarget -> warnTarget.receivedTime + 500 < System.currentTimeMillis());
        if (targets.values().stream().anyMatch(warnTarget -> warnTarget.warnType == WarnType.MISSILE_LAUNCH)) {
            if (missileLaunchWarnSound == null) {
                missileLaunchWarnSound = new VehicleSound(AllSounds.MISSILE_LAUNCH_WARN.get(),
                        4f, 1f, 1f,
                        true, 50, false, true, vehicle.getId());
                missileLaunchWarnSound.play();
            }
        } else if (missileLaunchWarnSound != null) {
            missileLaunchWarnSound.stop();
            missileLaunchWarnSound = null;
        }
        if (targets.values().stream().anyMatch(warnTarget -> warnTarget.warnType == WarnType.RADAR_LOCK)) {
            if (radarLockWarnSound == null) {
                radarLockWarnSound = new VehicleSound(AllSounds.RADAR_LOCK_WARN.get(),
                        4f, 1f, 1f,
                        true, 50, false, true, vehicle.getId());
                radarLockWarnSound.play();
            }
        } else if (radarLockWarnSound != null) {
            radarLockWarnSound.stop();
            radarLockWarnSound = null;
        }
    }

    public void clear() {
        targets.clear();
        if (missileLaunchWarnSound != null) {
            missileLaunchWarnSound.stop();
        }
        if (radarLockWarnSound != null) {
            radarLockWarnSound.stop();
        }
    }

    public record WarnTarget(WarnType warnType, String info, Long receivedTime) {}

}
