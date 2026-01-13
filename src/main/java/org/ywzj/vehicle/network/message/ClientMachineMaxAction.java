package org.ywzj.vehicle.network.message;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import org.ywzj.vehicle.blockentity.MachineMaxBlockEntity;
import org.ywzj.vehicle.recipe.VehiclePrintingIngredient;
import org.ywzj.vehicle.recipe.VehiclePrintingRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class ClientMachineMaxAction {

    public BlockPos blockPos;
    public ResourceLocation craftingCustomId;
    public Action action;

    public ClientMachineMaxAction() {}

    public static ClientMachineMaxAction decode(FriendlyByteBuf buf) {
        ClientMachineMaxAction clientMachineMaxAction = new ClientMachineMaxAction();
        clientMachineMaxAction.blockPos = buf.readBlockPos();
        clientMachineMaxAction.craftingCustomId = buf.readResourceLocation();
        clientMachineMaxAction.action = buf.readEnum(Action.class);
        return clientMachineMaxAction;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(blockPos);
        buf.writeResourceLocation(craftingCustomId);
        buf.writeEnum(action);
    }

    public static void onClientMessageReceived(ClientMachineMaxAction message, Supplier<NetworkEvent.Context> ctxSupplier) {
        ctxSupplier.get().enqueueWork(() -> {
            NetworkEvent.Context context = ctxSupplier.get();
            ServerPlayer serverPlayer = context.getSender();
            if (serverPlayer == null) {
                return;
            }

            context.setPacketHandled(true);
            Level level = serverPlayer.level();
            if (level.getBlockEntity(message.blockPos) instanceof MachineMaxBlockEntity machineMaxBlockEntity) {
                if (message.action == Action.CRAFT) {
                    if (machineMaxBlockEntity.isCrafting() || machineMaxBlockEntity.hasProduct()) {
                        return;
                    }
                    Optional<? extends Recipe<?>> recipeOptional = level.getRecipeManager().byKey(message.craftingCustomId);
                    if (!recipeOptional.isPresent()) {
                        return;
                    }
                    if (recipeOptional.get() instanceof VehiclePrintingRecipe vehiclePrintingRecipe) {
                        if (hasIngredients(serverPlayer, vehiclePrintingRecipe)) {
                            consumeIngredients(serverPlayer, vehiclePrintingRecipe);
                            machineMaxBlockEntity.craft(message.craftingCustomId, vehiclePrintingRecipe);
                        }
                    }
                }
            }
        });
    }

    private static boolean hasIngredients(ServerPlayer player, VehiclePrintingRecipe recipe) {
        List<ItemStack> inventoryCopy = new ArrayList<>();
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty()) {
                inventoryCopy.add(stack.copy());
            }
        }
        for (VehiclePrintingIngredient input : recipe.getInputs()) {
            int needed = input.count();
            for (ItemStack stack : inventoryCopy) {
                if (input.ingredient().test(stack)) {
                    int take = Math.min(stack.getCount(), needed);
                    stack.shrink(take);
                    needed -= take;
                }
                if (needed <= 0) break;
            }
            if (needed > 0) {
                return false;
            }
        }
        return true;
    }

    private static void consumeIngredients(ServerPlayer player, VehiclePrintingRecipe recipe) {
        for (VehiclePrintingIngredient input : recipe.getInputs()) {
            int remainingToConsume = input.count();
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (!stack.isEmpty() && input.ingredient().test(stack)) {
                    if (stack.getCount() > remainingToConsume) {
                        stack.shrink(remainingToConsume);
                        remainingToConsume = 0;
                    } else {
                        remainingToConsume -= stack.getCount();
                        player.getInventory().setItem(i, ItemStack.EMPTY);
                    }
                }
                if (remainingToConsume <= 0) {
                    break;
                }
            }
        }
        player.containerMenu.broadcastChanges();
    }

    public enum Action {
        CRAFT
    }

}
