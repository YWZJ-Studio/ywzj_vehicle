package org.ywzj.vehicle.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public abstract class VehicleItem extends Item {

    public VehicleItem(Properties pProperties) {
        super(pProperties);
    }

    public abstract InteractionResult interactEntity(ItemStack itemStack, Player player, Entity target, InteractionHand pHand);

}
