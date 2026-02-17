package org.ywzj.vehicle.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.ywzj.vehicle.all.AllItems;

public class AmmoItem extends Item {

    private final AmmoType ammoType;

    public enum AmmoType {
        MACHINE_GUN, AUTO_CANNON, GRENADE, ARTILLERY, ROCKET, AERIAL_BOMB, MISSILE, MISC
    }

    public AmmoItem(Properties pProperties, AmmoType ammoType) {
        super(pProperties);
        this.ammoType = ammoType;
    }

    public AmmoType getAmmoType() {
        return ammoType;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        if (stack.getItem() == AllItems.AMMO_CREATIVE.get()) {
            return true;
        }
        return super.isFoil(stack);
    }

}
