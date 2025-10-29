package org.ywzj.vehicle.item;

import net.minecraft.world.item.Item;

public class AmmoItem extends Item {

    private final AmmoType ammoType;

    public enum AmmoType {
        MACHINE_GUN, AUTO_CANNON, ARTILLERY, ROCKET, AERIAL_BOMB, MISSILE
    }

    public AmmoItem(Properties pProperties, AmmoType ammoType) {
        super(pProperties);
        this.ammoType = ammoType;
    }

    public AmmoType getAmmoType() {
        return ammoType;
    }

}
