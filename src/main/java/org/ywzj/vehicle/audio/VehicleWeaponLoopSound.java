package org.ywzj.vehicle.audio;

import com.mojang.blaze3d.audio.OggAudioStream;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.vehicle.weapon.AbstractVehicleWeapon;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.CompletableFuture;

@OnlyIn(Dist.CLIENT)
public class VehicleWeaponLoopSound extends AbstractTickableSoundInstance {

    private static final float FIRE_VOLUME = 4f;
    private final AbstractVehicleWeapon<?> weapon;
    private final SoundEvent startEvent;
    private final SoundEvent endEvent;
    private Sound startSound;
    private Sound endSound;
    private long remainingTicks;
    private volatile boolean finishing;
    private volatile boolean cancelled;
    private volatile WeaponSoundStream stream;

    public VehicleWeaponLoopSound(SoundEvent event, AbstractVehicleWeapon<?> weapon) {
        super(event, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        this.weapon = weapon;
        var display = ClientAssetsManager.INSTANCE.getWeaponDisplay(weapon.getData().getWeaponId());
        this.startEvent = display.map(value -> value.getSoundEvents().get("fire_loop_start")).orElse(null);
        this.endEvent = display.map(value -> value.getSoundEvents().get("fire_loop_end")).orElse(null);
        this.volume = FIRE_VOLUME;
        this.pitch = 1f;
        this.looping = false;
        this.delay = 0;
        refresh();
    }

    @Override
    public WeighedSoundEvents resolve(SoundManager manager) {
        WeighedSoundEvents event = super.resolve(manager);
        if (sound != SoundManager.EMPTY_SOUND && sound != SoundManager.INTENTIONALLY_EMPTY_SOUND) {
            sound = new Sound(sound.getLocation().toString(), sound.getVolume(), sound.getPitch(),
                    sound.getWeight(), sound.getType(), true, sound.shouldPreload(), sound.getAttenuationDistance());
        }
        startSound = resolveOptional(manager, startEvent);
        endSound = resolveOptional(manager, endEvent);
        return event;
    }

    private Sound resolveOptional(SoundManager manager, SoundEvent event) {
        if (event == null) {
            return null;
        }
        WeighedSoundEvents sounds = manager.getSoundEvent(event.getLocation());
        if (sounds == null) {
            return null;
        }
        Sound selected = sounds.getSound(random);
        return selected == SoundManager.EMPTY_SOUND || selected == SoundManager.INTENTIONALLY_EMPTY_SOUND
                ? null : selected;
    }

    @Override
    public CompletableFuture<AudioStream> getStream(SoundBufferLibrary buffers, Sound sound, boolean looping) {
        ResourceProvider resources = Minecraft.getInstance().getResourceManager();
        Sound intro = startSound;
        Sound outro = endSound;
        return CompletableFuture.<AudioStream>supplyAsync(() -> {
            try {
                Pcm loop = load(resources, sound, null);
                short[] start = loadOptional(resources, intro, loop.format());
                short[] end = loadOptional(resources, outro, loop.format());
                WeaponSoundStream result = new WeaponSoundStream(loop.format(), start, loop.samples(), end);
                stream = result;
                if (cancelled) {
                    result.close();
                } else if (finishing) {
                    result.finish();
                }
                return result;
            } catch (IOException | RuntimeException exception) {
                YwzjVehicle.LOGGER.warn("Unable to load weapon sound sequence {}", getLocation(), exception);
                cancelled = true;
                Minecraft.getInstance().execute(this::cancel);
                WeaponSoundStream silence = new WeaponSoundStream(new AudioFormat(44100, 16, 1, true, false),
                        null, new short[]{0}, null);
                silence.close();
                return silence;
            }
        }, Util.backgroundExecutor());
    }

    private short[] loadOptional(ResourceProvider resources, Sound sound, AudioFormat format) {
        if (sound == null) {
            return null;
        }
        try {
            return load(resources, sound, format).samples();
        } catch (IOException | RuntimeException exception) {
            YwzjVehicle.LOGGER.warn("Unable to load optional weapon sound {}", sound.getPath(), exception);
            return null;
        }
    }

    private Pcm load(ResourceProvider resources, Sound sound, AudioFormat target) throws IOException {
        try (var input = resources.open(sound.getPath()); var audio = new OggAudioStream(input)) {
            AudioFormat format = audio.getFormat();
            ByteBuffer decoded = audio.readAll();
            byte[] bytes = new byte[decoded.remaining()];
            decoded.get(bytes);
            if (target != null && !format.matches(target)) {
                try (var source = new AudioInputStream(new ByteArrayInputStream(bytes), format,
                        bytes.length / format.getFrameSize());
                     var converted = AudioSystem.getAudioInputStream(target, source)) {
                    bytes = converted.readAllBytes();
                }
                format = target;
            }
            short[] samples = new short[bytes.length / Short.BYTES];
            ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(samples);
            return new Pcm(format, samples);
        }
    }

    private record Pcm(AudioFormat format, short[] samples) {}

    public void play() {
        Minecraft.getInstance().getSoundManager().play(this);
    }

    public void cancel() {
        cancelled = true;
        stop();
        if (stream != null) {
            stream.close();
        }
        Minecraft.getInstance().getSoundManager().stop(this);
    }

    public boolean isFinishing() {
        return finishing;
    }

    public void refresh() {
        remainingTicks = Math.max(1L, (long) Math.ceil(weapon.getShootInterval() / 50.0));
        updatePosition();
    }

    @Override
    public void tick() {
        if (isStopped()) {
            return;
        }
        if (weapon.getVehicle().isRemoved() || Minecraft.getInstance().level != weapon.getVehicle().level()) {
            cancel();
            return;
        }
        updatePosition();
        if (!finishing && remainingTicks-- <= 0) {
            finishing = true;
            if (stream != null) {
                stream.finish();
            }
        }
    }

    private void updatePosition() {
        Vec3 position = weapon.getWeaponUnit().worldPivotPosition();
        this.x = position.x;
        this.y = position.y;
        this.z = position.z;
    }

}
