package org.ywzj.vehicle.audio;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.network.message.ServerSoundEvent;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class SoundManager {

    private final static ConcurrentHashMap<Integer, HashMap<String, VehicleSound>> SOUND_INSTANCE = new ConcurrentHashMap<>();

    public static void onServerMessageReceived(ServerSoundEvent message, Supplier<NetworkEvent.Context> ctxSupplier) {
        if (message.on) {
            play(message);
        } else {
            stop(message);
        }
        ctxSupplier.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void play(ServerSoundEvent message) {
        stop(message);
        LocalPlayer localPlayer = Minecraft.getInstance().player;
        if (localPlayer == null) {
            return;
        }
        SoundEvent event = AllSounds.SOUNDS.get(message.soundName).get();
        VehicleSound instance = new VehicleSound(event,
                message.volume,
                1f,
                1f,
                false,
                50,
                false,
                false,
                message.entityId);
        Minecraft.getInstance().execute(() -> Minecraft.getInstance().getSoundManager().play(instance));
        SOUND_INSTANCE.computeIfAbsent(message.entityId, k -> new HashMap<>()).put(message.soundName, instance);
    }

    @OnlyIn(Dist.CLIENT)
    private static void stop(ServerSoundEvent message) {
        if (SOUND_INSTANCE.containsKey(message.entityId) && SOUND_INSTANCE.get(message.entityId).containsKey(message.soundName)) {
            SOUND_INSTANCE.get(message.entityId).get(message.soundName).stop();
            SOUND_INSTANCE.get(message.entityId).remove(message.soundName);
        }
    }

    public static void play(SoundEvent soundEvent) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(soundEvent, 1.0F, 1.0F));
    }

}
