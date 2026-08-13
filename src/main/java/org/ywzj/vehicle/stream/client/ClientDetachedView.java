package org.ywzj.vehicle.stream.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.ywzj.vehicle.api.entity.DetachedBodyVehicle;

@OnlyIn(Dist.CLIENT)
public final class ClientDetachedView {

    private ClientDetachedView() {
    }

    public static Entity viewedVehicle() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return null;
        }
        Entity vehicle = player.getVehicle();
        if (vehicle instanceof DetachedBodyVehicle detached
                && detached.isDetachedBodyActive()
                && detached.getDetachedBodyAnchor(player) != null) {
            return vehicle;
        }
        return null;
    }

}
