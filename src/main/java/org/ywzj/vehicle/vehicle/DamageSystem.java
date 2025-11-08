package org.ywzj.vehicle.vehicle;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.structure.OBB;

public class DamageSystem {

    public static void hurt(DamageSource damageSource, float amount, AbstractVehicle vehicle) {
        Vec3 hitPos = damageSource.getSourcePosition();
        double scale;
        if (damageSource.getDirectEntity() == null || hitPos == null) {
            scale = 0.2;
        } else {
            Vec3 hitVec = damageSource.getDirectEntity().getDeltaMovement();
            OBB obb = vehicle.getMainCubeOBB().obb();
            Vec3 corePos = vehicle.relativeRotPos(new Vec3(obb.center()), false);
            Vec3 diff = corePos.subtract(hitPos);
            Vec3 cross = diff.cross(hitVec);
            double distanceToCore = cross.length() / hitVec.length();
            double distanceMax = obb.extents().get(obb.extents().maxComponent()) * 2;
            scale = (distanceMax - distanceToCore) / distanceMax;
        }
        vehicle.setHealth((float) (vehicle.getHealth() - amount * scale));
    }

}
