package org.ywzj.vehicle.mixin.client;

import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.ywzj.vehicle.client.render.util.PostPassesGetter;

import java.util.List;

@Mixin(PostChain.class)
public abstract class PostChainMixin implements PostPassesGetter {

    @Accessor
    public abstract List<PostPass> getPasses();

}
