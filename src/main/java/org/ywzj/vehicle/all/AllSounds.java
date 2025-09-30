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
    public static final RegistryObject<SoundEvent> CANNON_125_MM_SHOT = registerSoundEvent("cannon_125mm_shot", new ResourceLocation(YwzjVehicle.MOD_ID, "cannon_125mm_shot"));
    public static final RegistryObject<SoundEvent> CANNON_SHELL_DROP = registerSoundEvent("cannon_shell_drop", new ResourceLocation(YwzjVehicle.MOD_ID, "cannon_shell_drop"));
    public static final RegistryObject<SoundEvent> TURRET_TURN_SERVO_V = registerSoundEvent("turret_turn_servo_v", new ResourceLocation(YwzjVehicle.MOD_ID, "turret_turn_servo_v"));
    public static final RegistryObject<SoundEvent> TURRET_TURN_SERVO_H = registerSoundEvent("turret_turn_servo_h", new ResourceLocation(YwzjVehicle.MOD_ID, "turret_turn_servo_h"));

    public static final RegistryObject<SoundEvent> LAV150_ENGINE_START = registerSoundEvent("lav150_engine_start", new ResourceLocation(YwzjVehicle.MOD_ID, "lav150_engine_start"));
    public static final RegistryObject<SoundEvent> LAV150_ENGINE_IDLE = registerSoundEvent("lav150_engine_idle", new ResourceLocation(YwzjVehicle.MOD_ID, "lav150_engine_idle"));
    public static final RegistryObject<SoundEvent> LAV150_ENGINE_RUN = registerSoundEvent("lav150_engine_run", new ResourceLocation(YwzjVehicle.MOD_ID, "lav150_engine_run"));
    public static final RegistryObject<SoundEvent> LAV150_SHOOT = registerSoundEvent("entity.lav150.shoot", new ResourceLocation(YwzjVehicle.MOD_ID, "entity.lav150.shoot"));
    public static final RegistryObject<SoundEvent> ZTZ99A_ENGINE_START = registerSoundEvent("ztz99a_engine_start", new ResourceLocation(YwzjVehicle.MOD_ID, "ztz99a_engine_start"));
    public static final RegistryObject<SoundEvent> ZTZ99A_ENGINE_IDLE = registerSoundEvent("ztz99a_engine_idle", new ResourceLocation(YwzjVehicle.MOD_ID, "ztz99a_engine_idle"));
    public static final RegistryObject<SoundEvent> ZTZ99A_ENGINE_RUN = registerSoundEvent("ztz99a_engine_run", new ResourceLocation(YwzjVehicle.MOD_ID, "ztz99a_engine_run"));
    public static final RegistryObject<SoundEvent> Z10_ENGINE_START = registerSoundEvent("z10_engine_start", new ResourceLocation(YwzjVehicle.MOD_ID, "z10_engine_start"));
    public static final RegistryObject<SoundEvent> Z10_ENGINE_STOP = registerSoundEvent("z10_engine_stop", new ResourceLocation(YwzjVehicle.MOD_ID, "z10_engine_stop"));
    public static final RegistryObject<SoundEvent> Z10_ENGINE_RUN = registerSoundEvent("z10_engine_run", new ResourceLocation(YwzjVehicle.MOD_ID, "z10_engine_run"));
    public static final RegistryObject<SoundEvent> MOTORCYCLE_ENGINE_START = registerSoundEvent("motorcycle_engine_start", new ResourceLocation(YwzjVehicle.MOD_ID, "motorcycle_engine_start"));
    public static final RegistryObject<SoundEvent> MOTORCYCLE_ENGINE_IDLE = registerSoundEvent("motorcycle_engine_idle", new ResourceLocation(YwzjVehicle.MOD_ID, "motorcycle_engine_idle"));
    public static final RegistryObject<SoundEvent> MOTORCYCLE_ENGINE_RUN = registerSoundEvent("motorcycle_engine_run", new ResourceLocation(YwzjVehicle.MOD_ID, "motorcycle_engine_run"));

    private static RegistryObject<SoundEvent> registerSoundEvent(String name, ResourceLocation soundResourceLocation) {
        RegistryObject<SoundEvent> soundEventRegistryObject = SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(soundResourceLocation));
        SOUNDS.put(name, soundEventRegistryObject);
        return soundEventRegistryObject;
    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }

}
