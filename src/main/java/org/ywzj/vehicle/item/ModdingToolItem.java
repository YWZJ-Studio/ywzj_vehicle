package org.ywzj.vehicle.item;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.ywzj.vehicle.client.screen.VehicleDisplayToolScreen;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

public class ModdingToolItem extends VehicleItem {

    public ModdingToolItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResult interactEntity(ItemStack stack, Player player, Entity target, InteractionHand pHand) {
        if (player.level().isClientSide) {
            if (pHand == InteractionHand.MAIN_HAND && !player.isShiftKeyDown()) {
                if (target instanceof AbstractVehicle vehicle) {
                    openScreen(vehicle);
                }
            }
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    @OnlyIn(Dist.CLIENT)
    public void openScreen(AbstractVehicle vehicle) {
        Minecraft.getInstance().setScreen(new VehicleDisplayToolScreen(vehicle));
    }

}
