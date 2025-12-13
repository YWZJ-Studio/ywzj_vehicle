package org.ywzj.vehicle.client.handler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.ywzj.vehicle.client.render.animation.item.RepairItemAnimationInstance;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.item.RepairToolItem;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class InventoryEventHandler {
    // 用于切枪逻辑
    private static int oldHotbarSelected = -1;
    private static ItemStack oldHotbarSelectItem = ItemStack.EMPTY;

    // 由于原版的ItemStack在客户端近乎无法跟踪是否是同一个（一旦nbt变化，就会在客户端重新构建一个新的实例）
    // 甚至于，因为创造复制物品和物品栏状态保存/检测使用了同一个copy函数，甚至没有可靠的办法为物品手动赋予唯一标识
    // 故此，除了切换格子和副手，当玩家在背包中直接替换手中相同id物品时（即使除了枪械id外的nbt不一样），我们也不认为发生了切枪
    @SubscribeEvent
    public static void onPlayerChangeSelect(TickEvent.ClientTickEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        Inventory inventory = player.getInventory();
        ItemStack currentItem = inventory.getItem(inventory.selected);
        // 玩家切换选中框的情况
        if (oldHotbarSelected != inventory.selected) {
//            if (Minecraft.getInstance().gameMode != null) {
//                Minecraft.getInstance().gameMode.ensureHasSentCarriedItem();
//            }
            if (currentItem.getItem() instanceof RepairToolItem item) {
                FirstPersonHandler.instance = new RepairItemAnimationInstance(
                        currentItem,
                        ClientAssetsManager.INSTANCE.getInternalAssets().getRepairToolAnimations()
                );
            } else {
                FirstPersonHandler.instance = null;
            }
            oldHotbarSelected = inventory.selected;
            oldHotbarSelectItem = inventory.getItem(inventory.selected).copy();
            return;
        }

        if (!ItemStack.matches(oldHotbarSelectItem, currentItem)) {
//            if (Minecraft.getInstance().gameMode != null) {
//                Minecraft.getInstance().gameMode.ensureHasSentCarriedItem();
//            }
            if (currentItem.getItem() instanceof RepairToolItem item) {
                FirstPersonHandler.instance = new RepairItemAnimationInstance(
                        currentItem,
                        ClientAssetsManager.INSTANCE.getInternalAssets().getRepairToolAnimations()
                );
            } else {
                FirstPersonHandler.instance = null;
            }
            oldHotbarSelected = inventory.selected;
            oldHotbarSelectItem = currentItem.copy();
        }
    }

//    @SubscribeEvent
//    public static void onPlayerSwapMainHand(SwapItemWithOffHand event) {
//        LocalPlayer player = Minecraft.getInstance().player;
//        if (player == null) {
//            return;
//        }
//        IClientPlayerGunOperator.fromLocalPlayer(player).draw(player.getMainHandItem());
//    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        // 离开游戏时重置客户端 draw 状态
        oldHotbarSelected = -1;
        oldHotbarSelectItem = ItemStack.EMPTY;
    }

}
