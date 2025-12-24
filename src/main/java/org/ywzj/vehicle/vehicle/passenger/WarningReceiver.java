package org.ywzj.vehicle.vehicle.passenger;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.audio.VehicleSound;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.network.message.ServerVehicleWarn;
import org.ywzj.vehicle.vehicle.pojo.WarnType;

public class WarningReceiver {

    public VehicleSound radarLockWarnSound;
    public boolean radarLockWarn;
    public VehicleSound missileLaunchWarnSound;
    public boolean missileLaunchWarn;

    @OnlyIn(Dist.CLIENT)
    public static void handle(ServerVehicleWarn message) {
        Level level = Minecraft.getInstance().level;
        Entity fromVehicle = level.getEntity(message.fromVehicleId);
        if (level.getEntity(message.toVehicleId) instanceof AbstractVehicle toVehicle) {
            WarningReceiver warningReceiver = toVehicle.warningReceiver;
            if (warningReceiver != null) {
                if (message.warnType == WarnType.RADAR_LOCK) {
                    if (warningReceiver.radarLockWarnSound != null) {
                        warningReceiver.radarLockWarnSound.stop();
                    }
                    if (message.on) {
                        warningReceiver.radarLockWarnSound = new VehicleSound(AllSounds.RADAR_LOCK_WARN.get(), 4f, 4f, 1f, true, 50, false, true, toVehicle.getId());
                        warningReceiver.radarLockWarnSound.play();
                    }
                    warningReceiver.radarLockWarn = message.on;
                } else if  (message.warnType == WarnType.MISSILE_LAUNCH) {
                    if (warningReceiver.missileLaunchWarnSound != null) {
                        warningReceiver.missileLaunchWarnSound.stop();
                    }
                    if (message.on) {
                        warningReceiver.missileLaunchWarnSound = new VehicleSound(AllSounds.MISSILE_LAUNCH_WARN.get(), 4f, 4f, 1f, true, 50, false, true, toVehicle.getId());
                        warningReceiver.missileLaunchWarnSound.play();
                    }
                    warningReceiver.missileLaunchWarn = message.on;
                }
            }
        }
    }

}
