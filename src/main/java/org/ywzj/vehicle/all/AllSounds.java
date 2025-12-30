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

    public static final RegistryObject<SoundEvent> VEHICLE_HURT = registerSoundEvent("vehicle_hurt", new ResourceLocation(YwzjVehicle.MOD_ID, "vehicle_hurt"));
    public static final RegistryObject<SoundEvent> VEHICLE_HIT_SMALL = registerSoundEvent("vehicle_hit_small", new ResourceLocation(YwzjVehicle.MOD_ID, "vehicle_hit_small"));
    public static final RegistryObject<SoundEvent> VEHICLE_HIT_MED = registerSoundEvent("vehicle_hit_med", new ResourceLocation(YwzjVehicle.MOD_ID, "vehicle_hit_med"));
    public static final RegistryObject<SoundEvent> VEHICLE_HIT_BIG = registerSoundEvent("vehicle_hit_big", new ResourceLocation(YwzjVehicle.MOD_ID, "vehicle_hit_big"));
    public static final RegistryObject<SoundEvent> IR_TRACK_ALARM = registerSoundEvent("ir_track_alarm", new ResourceLocation(YwzjVehicle.MOD_ID, "ir_track_alarm"));
    public static final RegistryObject<SoundEvent> RADAR_LOCK_WARN = registerSoundEvent("radar_lock_warn", new ResourceLocation(YwzjVehicle.MOD_ID, "radar_lock_warn"));
    public static final RegistryObject<SoundEvent> MISSILE_LAUNCH_WARN = registerSoundEvent("missile_launch_warn", new ResourceLocation(YwzjVehicle.MOD_ID, "missile_launch_warn"));
    public static final RegistryObject<SoundEvent> GUN_7_62MM_SHOT = registerSoundEvent("gun_7.62mm_shot", new ResourceLocation(YwzjVehicle.MOD_ID, "gun_7.62mm_shot"));
    public static final RegistryObject<SoundEvent> GUN_12_7MM_SHOT = registerSoundEvent("gun_12.7mm_shot", new ResourceLocation(YwzjVehicle.MOD_ID, "gun_12.7mm_shot"));
    public static final RegistryObject<SoundEvent> GUN_14_5MM_SHOT = registerSoundEvent("gun_14.5mm_shot", new ResourceLocation(YwzjVehicle.MOD_ID, "gun_14.5mm_shot"));
    public static final RegistryObject<SoundEvent> AUTO_CANNON_SHOT = registerSoundEvent("auto_cannon_shot", new ResourceLocation(YwzjVehicle.MOD_ID, "auto_cannon_shot"));
    public static final RegistryObject<SoundEvent> CANNON_125_MM_SHOT = registerSoundEvent("cannon_125mm_shot", new ResourceLocation(YwzjVehicle.MOD_ID, "cannon_125mm_shot"));
    public static final RegistryObject<SoundEvent> ROCKET_LAUNCH = registerSoundEvent("rocket_launch", new ResourceLocation(YwzjVehicle.MOD_ID, "rocket_launch"));
    public static final RegistryObject<SoundEvent> MISSILE_LAUNCH = registerSoundEvent("missile_launch", new ResourceLocation(YwzjVehicle.MOD_ID, "missile_launch"));
    public static final RegistryObject<SoundEvent> ROCKET_FLYING = registerSoundEvent("rocket_flying", new ResourceLocation(YwzjVehicle.MOD_ID, "rocket_flying"));
    public static final RegistryObject<SoundEvent> BOMB_DROP = registerSoundEvent("bomb_drop", new ResourceLocation(YwzjVehicle.MOD_ID, "bomb_drop"));
    public static final RegistryObject<SoundEvent> BOMB_WHISTLE = registerSoundEvent("bomb_whistle", new ResourceLocation(YwzjVehicle.MOD_ID, "bomb_whistle"));
    public static final RegistryObject<SoundEvent> BOMBS_INCOMING = registerSoundEvent("bombs_incoming", new ResourceLocation(YwzjVehicle.MOD_ID, "bombs_incoming"));
    public static final RegistryObject<SoundEvent> CANNON_SHELL_DROP = registerSoundEvent("cannon_shell_drop", new ResourceLocation(YwzjVehicle.MOD_ID, "cannon_shell_drop"));
    public static final RegistryObject<SoundEvent> SMOKE_GRENADE_LAUNCH = registerSoundEvent("smoke_grenade_launch", new ResourceLocation(YwzjVehicle.MOD_ID, "smoke_grenade_launch"));
    public static final RegistryObject<SoundEvent> DECOY_FLARE_LAUNCH = registerSoundEvent("decoy_flare_launch", new ResourceLocation(YwzjVehicle.MOD_ID, "decoy_flare_launch"));
    public static final RegistryObject<SoundEvent> DECOY_FLARE_TAIL = registerSoundEvent("decoy_flare_tail", new ResourceLocation(YwzjVehicle.MOD_ID, "decoy_flare_tail"));
    public static final RegistryObject<SoundEvent> SMOKE_GRENADE_EXPLOSION = registerSoundEvent("smoke_grenade_explosion", new ResourceLocation(YwzjVehicle.MOD_ID, "smoke_grenade_explosion"));
    public static final RegistryObject<SoundEvent> TURRET_TURN_SERVO_V = registerSoundEvent("turret_turn_servo_v", new ResourceLocation(YwzjVehicle.MOD_ID, "turret_turn_servo_v"));
    public static final RegistryObject<SoundEvent> TURRET_TURN_SERVO_H = registerSoundEvent("turret_turn_servo_h", new ResourceLocation(YwzjVehicle.MOD_ID, "turret_turn_servo_h"));
    public static final RegistryObject<SoundEvent> TIRE_SQUEAL = registerSoundEvent("tire_squeal", new ResourceLocation(YwzjVehicle.MOD_ID, "tire_squeal"));

    public static final RegistryObject<SoundEvent> LAV150_ENGINE_START = registerSoundEvent("lav150_engine_start", new ResourceLocation(YwzjVehicle.MOD_ID, "lav150_engine_start"));
    public static final RegistryObject<SoundEvent> LAV150_ENGINE_IDLE = registerSoundEvent("lav150_engine_idle", new ResourceLocation(YwzjVehicle.MOD_ID, "lav150_engine_idle"));
    public static final RegistryObject<SoundEvent> LAV150_ENGINE_RUN = registerSoundEvent("lav150_engine_run", new ResourceLocation(YwzjVehicle.MOD_ID, "lav150_engine_run"));
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
