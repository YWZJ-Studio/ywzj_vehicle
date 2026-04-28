package org.ywzj.vehicle.item;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.ywzj.vehicle.client.render.item.PlainTextureItemRenderer;

import java.util.function.Consumer;

public class PlainTextureItem extends Item {

    public PlainTextureItem(Properties pProperties) {
        super(pProperties);
    }

    public ItemStack createInstance(ResourceLocation textureLocation) {
        ItemStack itemStack = new ItemStack(this);
        CompoundTag tag = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putString("textureLocation", textureLocation.toString());
        itemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return itemStack;
    }

    @Override
    @SuppressWarnings("removal")
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {

            private PlainTextureItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                Minecraft minecraft = Minecraft.getInstance();
                if (renderer == null) {
                    renderer = new PlainTextureItemRenderer(minecraft.getBlockEntityRenderDispatcher(), minecraft.getEntityModels());
                }
                return renderer;
            }

        });
    }

}
