package org.ywzj.vehicle.client.render.animation.context;

import net.minecraft.resources.ResourceLocation;

@FunctionalInterface
public interface SoundResolver {
    void playSound(ResourceLocation soundLocation, float volume, float pitch);
}
