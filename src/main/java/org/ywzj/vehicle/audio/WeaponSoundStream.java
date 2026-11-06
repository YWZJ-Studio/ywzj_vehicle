package org.ywzj.vehicle.audio;

import net.minecraft.client.sounds.AudioStream;

import javax.sound.sampled.AudioFormat;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

final class WeaponSoundStream implements AudioStream {

    private enum Phase { START, LOOP, END, FADE, DONE }
    private final AudioFormat format;
    private final short[] start;
    private final short[] loop;
    private final short[] end;
    private final int channels;
    private final int fadeFrames;
    private final int bufferFrames;
    private Phase phase;
    private int position;
    private int loopFrames;
    private int fadePosition;
    private float fadeStartGain = 1f;
    private short[] fadingSamples;
    private volatile boolean finishing;
    private volatile boolean closed;

    WeaponSoundStream(AudioFormat format, short[] start, short[] loop, short[] end) {
        this.format = format;
        this.channels = format.getChannels();
        if (format.getSampleSizeInBits() != 16 || format.isBigEndian()
                || !AudioFormat.Encoding.PCM_SIGNED.equals(format.getEncoding())
                || loop.length == 0 || loop.length % channels != 0
                || (start != null && start.length % channels != 0)
                || (end != null && end.length % channels != 0)) {
            throw new IllegalArgumentException("Expected complete signed 16-bit little-endian PCM frames");
        }
        this.start = start != null && start.length > 0 ? start : null;
        this.loop = loop;
        this.end = end != null && end.length > 0 ? end : null;
        this.phase = this.start == null ? Phase.LOOP : Phase.START;
        this.fadeFrames = Math.max(2, Math.round(format.getSampleRate() * 0.1f));
        this.bufferFrames = Math.max(1, Math.round(format.getSampleRate() * 0.025f));
    }

    void finish() {
        finishing = true;
    }

    @Override
    public AudioFormat getFormat() {
        return format;
    }

    @Override
    public ByteBuffer read(int size) {
        int frames = Math.min(bufferFrames, size / (channels * Short.BYTES));
        ByteBuffer output = ByteBuffer.allocateDirect(frames * channels * Short.BYTES)
                .order(ByteOrder.LITTLE_ENDIAN);
        for (int frame = 0; frame < frames && !closed && phase != Phase.DONE; frame++) {
            if (finishing && (phase == Phase.START || phase == Phase.LOOP)) {
                if (end != null) {
                    phase = Phase.END;
                    position = 0;
                } else {
                    fadeStartGain = currentGain();
                    fadingSamples = phase == Phase.START ? start : loop;
                    phase = Phase.FADE;
                    fadePosition = 0;
                }
            }
            short[] samples = phase == Phase.START ? start : phase == Phase.END ? end : loop;
            if (phase == Phase.FADE) {
                samples = fadingSamples;
            }
            float gain = currentGain();
            for (int channel = 0; channel < channels; channel++) {
                output.putShort((short) Math.round(samples[position++] * gain));
            }
            if (phase == Phase.LOOP) {
                loopFrames = Math.min(loopFrames + 1, fadeFrames);
            }
            if (phase == Phase.FADE && ++fadePosition >= fadeFrames) {
                phase = Phase.DONE;
            }
            if (position >= samples.length) {
                position = 0;
                if (phase == Phase.START) {
                    phase = Phase.LOOP;
                } else if (phase == Phase.END || (phase == Phase.FADE && samples == start)) {
                    phase = Phase.DONE;
                }
            }
        }
        return output.flip();
    }

    private float currentGain() {
        if (phase == Phase.FADE) {
            float remaining = 1f - (float) fadePosition / (fadeFrames - 1);
            return fadeStartGain * remaining * remaining;
        }
        if (phase == Phase.LOOP && start == null && loopFrames < fadeFrames) {
            float elapsed = (float) loopFrames / (fadeFrames - 1);
            return elapsed * elapsed;
        }
        return 1f;
    }

    @Override
    public void close() {
        closed = true;
    }

}
