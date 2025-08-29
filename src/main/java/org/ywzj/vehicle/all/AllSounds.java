package org.ywzj.vehicle.all;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.ywzj.vehicle.YwzjVehicle;

import java.util.HashMap;

public class AllSounds {

    public static final HashMap<String, RegistryObject<SoundEvent>> SOUNDS = new HashMap<>();
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, YwzjVehicle.MOD_ID);

    public static final RegistryObject<SoundEvent> BULLET_HIT_OUTSIDE = registerSoundEvent("bullet_hit_outside", new ResourceLocation(YwzjVehicle.MOD_ID, "bullet_hit_outside"));

    public static final RegistryObject<SoundEvent> LAV_150_ENGINE_START = registerSoundEvent("lav150_engine_start", new ResourceLocation(YwzjVehicle.MOD_ID, "lav150_engine_start"));
    public static final RegistryObject<SoundEvent> LAV_150_ENGINE_IDLE = registerSoundEvent("lav150_engine_idle", new ResourceLocation(YwzjVehicle.MOD_ID, "lav150_engine_idle"));
    public static final RegistryObject<SoundEvent> LAV_150_ENGINE_RUN = registerSoundEvent("lav150_engine_run", new ResourceLocation(YwzjVehicle.MOD_ID, "lav150_engine_run"));
    public static final RegistryObject<SoundEvent> LAV_150_SHOOT = registerSoundEvent("entity.lav150.shoot", new ResourceLocation(YwzjVehicle.MOD_ID, "entity.lav150.shoot"));

    private static RegistryObject<SoundEvent> registerSoundEvent(String name, ResourceLocation soundResourceLocation) {
        RegistryObject<SoundEvent> soundEventRegistryObject = SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(soundResourceLocation));
        SOUNDS.put(name, soundEventRegistryObject);
        return soundEventRegistryObject;
    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }

}
