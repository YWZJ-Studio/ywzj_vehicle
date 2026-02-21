package org.ywzj.vehicle.client.render.animation.util;

import net.minecraft.resources.ResourceLocation;

@FunctionalInterface
public interface SoundResolver {
    void playSound(ResourceLocation soundLocation, float volume, float pitch);
}
