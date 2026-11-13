package com.fonseca.novafrontier.systems;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.AudioDevice;

import java.util.Random;

/**
 * Sintetizador de efeitos 8-bit/16-bit: todos os sons sao gerados em memoria na inicializacao
 * (ondas quadrada/triangular/ruido em buffers PCM) e tocados por um mixer proprio em cima de
 * um AudioDevice. Nenhum arquivo de audio e usado. Sem Gdx.audio (testes headless) tudo vira no-op.
 */
public final class SoundManager {

    public enum Sfx {
        SHOOT_BLASTER(0.03f), SHOOT_SCATTER(0.05f), SHOOT_LASER(0.06f), SHOOT_PLASMA(0.05f),
        HIT(0.04f), SHIELD_BREAK(0.2f), DASH(0.1f), PICKUP(0.04f), SHOP(0.05f),
        BOSS_ALERT(0.5f), ACHIEVEMENT(0.5f);

        /** Intervalo minimo entre duas execucoes (evita saturar o mixer). */
        final float minGap;

        Sfx(float minGap) { this.minGap = minGap; }
    }

    private static final int RATE = 22050;
    private static final int CHUNK = 512;
    private static final int VOICES = 20;
    private static SoundManager instance;

    private final short[][] bank = new short[Sfx.values().length][];
    private final long[] lastPlay = new long[Sfx.values().length];

    // mixer
    private final Object lock = new Object();
    private final short[][] voiceData = new short[VOICES][];
    private final int[] voicePos = new int[VOICES];
    private final float[] voiceVol = new float[VOICES];
    private int active;
    private volatile boolean running = true;
    private AudioDevice device;
    private Thread thread;

    private SoundManager() {
        synthesize();
        try {
            device = Gdx.audio.newAudioDevice(RATE, true);
            thread = new Thread(this::mixLoop, "nova-frontier-audio");
            thread.setDaemon(true);
            thread.start();
        } catch (Throwable t) {
            device = null; // sem dispositivo de audio: jogo segue mudo
        }
    }

    /** Cria o sintetizador (chamar uma vez, com o contexto de audio do libGDX ativo). */
    public static synchronized void init() {
        if (instance == null && Gdx.audio != null) instance = new SoundManager();
    }

    public static synchronized void shutdown() {
        if (instance != null) {
            instance.dispose();
            instance = null;
        }
    }

    /** Toca um efeito (no-op se o audio nao existe). */
    public static void play(Sfx s) {
        SoundManager m = instance;
        if (m != null) m.playInternal(s);
    }

    // ------------------------------------------------------------------ reproducao
    private void playInternal(Sfx s) {
        if (device == null) return;
        float vol = ProfileManager.get().masterVolume() * ProfileManager.get().sfxVolume();
        if (vol <= 0.001f) return;
        long now = System.nanoTime();
        if ((now - lastPlay[s.ordinal()]) < (long) (s.minGap * 1e9f)) return;
        lastPlay[s.ordinal()] = now;
        synchronized (lock) {
            int slot = -1;
            for (int i = 0; i < VOICES; i++) {
                if (voiceData[i] == null) { slot = i; break; }
            }
            if (slot < 0) slot = 0; // sobrescreve a voz mais antiga
            else active++;
            voiceData[slot] = bank[s.ordinal()];
            voicePos[slot] = 0;
            voiceVol[slot] = vol;
            lock.notifyAll();
        }
    }

    private void mixLoop() {
        float[] acc = new float[CHUNK];
        short[] out = new short[CHUNK];
        while (running) {
            synchronized (lock) {
                while (running && active == 0) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        return;
                    }
                }
                if (!running) return;
                java.util.Arrays.fill(acc, 0f);
                for (int v = 0; v < VOICES; v++) {
                    short[] d = voiceData[v];
                    if (d == null) continue;
                    int pos = voicePos[v];
                    int n = Math.min(CHUNK, d.length - pos);
                    float vol = voiceVol[v];
                    for (int i = 0; i < n; i++) acc[i] += d[pos + i] * vol;
                    voicePos[v] = pos + n;
                    if (voicePos[v] >= d.length) {
                        voiceData[v] = null;
                        active--;
                    }
                }
            }
            for (int i = 0; i < CHUNK; i++) {
                float s = acc[i];
                out[i] = (short) Math.max(-32767f, Math.min(32767f, s));
            }
            try {
                device.writeSamples(out, 0, CHUNK);
            } catch (Throwable t) {
                return;
            }
        }
    }

    private void dispose() {
        running = false;
        synchronized (lock) { lock.notifyAll(); }
        if (thread != null) {
            try { thread.join(300); } catch (InterruptedException ignored) { }
        }
        try { if (device != null) device.dispose(); } catch (Throwable ignored) { }
    }

    // ------------------------------------------------------------------ sintese
    private enum Wave { SQUARE, TRIANGLE, NOISE }

    private void synthesize() {
        // tiros (um timbre por arma)
        bank[Sfx.SHOOT_BLASTER.ordinal()] = tone(Wave.SQUARE, 0.5f, 0.09f, 900f, 300f, 0.30f, 2.0f);
        bank[Sfx.SHOOT_SCATTER.ordinal()] = mix(
                tone(Wave.NOISE, 0.5f, 0.16f, 3500f, 900f, 0.34f, 1.6f),
                tone(Wave.SQUARE, 0.25f, 0.12f, 300f, 90f, 0.24f, 1.8f));
        bank[Sfx.SHOOT_LASER.ordinal()] = tone(Wave.SQUARE, 0.25f, 0.07f, 1800f, 1100f, 0.20f, 1.4f);
        bank[Sfx.SHOOT_PLASMA.ordinal()] = mix(
                tone(Wave.TRIANGLE, 0.5f, 0.28f, 160f, 520f, 0.38f, 1.2f),
                tone(Wave.NOISE, 0.5f, 0.18f, 1500f, 400f, 0.14f, 2.0f));
        // eventos
        bank[Sfx.HIT.ordinal()] = tone(Wave.NOISE, 0.5f, 0.05f, 4000f, 1500f, 0.22f, 1.5f);
        bank[Sfx.SHIELD_BREAK.ordinal()] = mix(
                tone(Wave.NOISE, 0.5f, 0.35f, 2500f, 300f, 0.40f, 1.2f),
                tone(Wave.SQUARE, 0.5f, 0.30f, 700f, 80f, 0.30f, 1.3f));
        bank[Sfx.DASH.ordinal()] = tone(Wave.NOISE, 0.5f, 0.16f, 500f, 3000f, 0.26f, 0.9f);
        bank[Sfx.PICKUP.ordinal()] = seq(Wave.SQUARE, 0.25f, 0.30f, 0.05f, 880f, 1320f);
        bank[Sfx.SHOP.ordinal()] = seq(Wave.TRIANGLE, 0.5f, 0.40f, 0.07f, 523f, 659f, 784f);
        bank[Sfx.BOSS_ALERT.ordinal()] = seq(Wave.SQUARE, 0.5f, 0.38f, 0.16f, 196f, 147f, 196f, 147f);
        bank[Sfx.ACHIEVEMENT.ordinal()] = seq(Wave.SQUARE, 0.25f, 0.34f, 0.09f, 523f, 659f, 784f, 1047f);
    }

    /** Uma nota com varredura de frequencia f0 -> f1 e envelope exponencial. */
    private static short[] tone(Wave wave, float duty, float dur, float f0, float f1, float vol, float decay) {
        int n = (int) (RATE * dur);
        short[] out = new short[n];
        double phase = 0;
        Random rng = new Random(1234 + (long) (f0 * 7));
        float noise = 0f;
        int noiseHold = 0;
        for (int i = 0; i < n; i++) {
            float t = i / (float) n;
            double f = f0 + (f1 - f0) * t;
            phase += f / RATE;
            phase -= Math.floor(phase);
            float s;
            switch (wave) {
                case SQUARE: s = phase < duty ? 1f : -1f; break;
                case TRIANGLE: s = (float) (phase < 0.5 ? phase * 4 - 1 : 3 - phase * 4); break;
                default:
                    // ruido "sample and hold": a frequencia controla o quao granulado ele soa
                    if (noiseHold-- <= 0) {
                        noise = rng.nextFloat() * 2f - 1f;
                        noiseHold = (int) Math.max(1, RATE / Math.max(200f, (float) f));
                    }
                    s = noise;
            }
            float env = (float) Math.pow(1f - t, decay);
            float attack = Math.min(1f, i / 40f); // evita estalo no inicio
            out[i] = (short) (s * env * attack * vol * 32767f);
        }
        return out;
    }

    /** Sequencia de notas curtas. */
    private static short[] seq(Wave wave, float duty, float vol, float noteLen, float... freqs) {
        int per = (int) (RATE * noteLen);
        short[] out = new short[per * freqs.length];
        for (int k = 0; k < freqs.length; k++) {
            short[] note = tone(wave, duty, noteLen, freqs[k], freqs[k], vol, 1.0f);
            System.arraycopy(note, 0, out, k * per, Math.min(per, note.length));
        }
        return out;
    }

    private static short[] mix(short[] a, short[] b) {
        short[] out = new short[Math.max(a.length, b.length)];
        for (int i = 0; i < out.length; i++) {
            int s = (i < a.length ? a[i] : 0) + (i < b.length ? b[i] : 0);
            out[i] = (short) Math.max(-32767, Math.min(32767, s));
        }
        return out;
    }
}
