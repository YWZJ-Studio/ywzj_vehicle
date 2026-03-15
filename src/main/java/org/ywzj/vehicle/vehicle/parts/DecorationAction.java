package org.ywzj.vehicle.vehicle.parts;

import net.minecraft.world.phys.Vec3;

public class DecorationAction {

    public Action action;
    public String decorationDisplayId;
    public int vehicleId;
    public String decorationUnitId;
    public String baseBoneName;
    public float scale;
    public float selfXRot;
    public float selfYRot;
    public float selfZRot;
    public Vec3 offsetFromBone;

    public enum Action {
        UPDATE_ITEM, SET, REMOVE
    }

}
