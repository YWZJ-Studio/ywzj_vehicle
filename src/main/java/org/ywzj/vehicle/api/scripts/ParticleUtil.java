package org.ywzj.vehicle.api.scripts;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

public enum ParticleUtil {
    INSTANCE;

    public record ParticleOptionsWrapper(ParticleOptions options) {}

    public ParticleOptionsWrapper buildParticleOptions(String particleName, String params) {
        ResourceLocation location = new ResourceLocation(particleName);
        if (!ForgeRegistries.PARTICLE_TYPES.containsKey(location)) {
            return null;
        }
        var type = ForgeRegistries.PARTICLE_TYPES.getValue(new ResourceLocation(particleName));
        if (type == null) {
            return null;
        }
        try {
            return new ParticleOptionsWrapper(readParticle(params, type));
        } catch (CommandSyntaxException e) {
            return null;
        }
    }

    private static <T extends ParticleOptions> T readParticle(String params, ParticleType<T> pType) throws CommandSyntaxException {
        return pType.getDeserializer().fromCommand(pType, new StringReader(params));
    }
}
