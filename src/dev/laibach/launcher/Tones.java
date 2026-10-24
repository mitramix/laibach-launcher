package dev.laibach.launcher;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

/**
 * The state's sounds, synthesised here and nowhere recorded: a bell, a short
 * fanfare of the state's own, a chime for the power ration, a boot chime
 * pitched for each uniform, a call of each uniform's own, the click of a
 * relay, the rubber stamp, the shutter, the decree in Morse, the anthem of the
 * hour and the typewriter's keys. All are off until the owner turns them on in the
 * menu (Public order → sounds); the alarm rings whatever that setting says.
 */
final class Tones {
    private static final String KEY = "sounds";
    private static final String KEY_TYPEWRITER = "typewriter";
    private static final int RATE = 22_050;

    private Tones() {}

    static boolean on(Context c) {
        return c.getSharedPreferences(Variant.PREFS, Context.MODE_PRIVATE).getBoolean(KEY, false);
    }

    static void setOn(Context c, boolean on) {
        c.getSharedPreferences(Variant.PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY, on).apply();
    }

    /** Whether the typewriter is heard at the censor's desk and on the memo (and the sounds are on). */
    static boolean typewriter(Context c) {
        return c.getSharedPreferences(Variant.PREFS, Context.MODE_PRIVATE).getBoolean(KEY_TYPEWRITER, false);
    }

    static void setTypewriter(Context c, boolean on) {
        c.getSharedPreferences(Variant.PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_TYPEWRITER, on).apply();
    }

    /** Plays a typewriter's sound, if the owner wants the typewriter heard. */
    static void type(Context c, short[] pcm) {
        if (typewriter(c)) play(c, pcm);
    }

    /** A struck bell: inharmonic partials, each dying away at its own pace. */
    static short[] bell(double seconds) {
        double[] ratio = {0.5, 1.0, 1.183, 1.506, 2.0, 2.514, 3.011};
        double[] level = {0.35, 0.6, 0.35, 0.25, 0.3, 0.15, 0.1};
        double[] decay = {1.2, 2.2, 2.6, 3.4, 4.0, 5.5, 7.0};
        return render(seconds, t -> {
            double s = 0;
            for (int i = 0; i < ratio.length; i++) {
                s += level[i] * Math.exp(-decay[i] * t) * Math.sin(2 * Math.PI * 220 * ratio[i] * t);
            }
            return s * Math.min(1, t * 400) * 0.45;
        });
    }

    /** The state's fanfare: a rising fourth, a fifth and the tonic held. Original, eight bars short of an anthem. */
    static short[] anthem() {
        double[][] notes = {{196, 0.30}, {261.6, 0.30}, {261.6, 0.15}, {293.7, 0.15}, {329.6, 0.45},
                {392, 0.30}, {329.6, 0.30}, {261.6, 0.90}};
        double total = 0;
        for (double[] n : notes) total += n[1];
        double[] starts = new double[notes.length];
        double at = 0;
        for (int i = 0; i < notes.length; i++) {
            starts[i] = at;
            at += notes[i][1];
        }
        return render(total + 0.4, t -> {
            int i = notes.length - 1;
            while (i > 0 && t < starts[i]) i--;
            double local = t - starts[i], f = notes[i][0];
            double env = Math.min(1, local * 60) * Math.exp(-1.6 * local);
            // Brass-ish: a fundamental and its odd and even partials.
            double s = Math.sin(2 * Math.PI * f * t) + 0.5 * Math.sin(4 * Math.PI * f * t)
                    + 0.3 * Math.sin(6 * Math.PI * f * t) + 0.15 * Math.sin(8 * Math.PI * f * t);
            return s * env * 0.22;
        });
    }

    /**
     * The anthem of the hour: brass climbing a fifth, stepping back to the
     * fourth and landing on the octave. Each hour of the clock has its own key,
     * a semitone above the hour before. Original, and two seconds long.
     */
    static short[] hourly(int hour) {
        double base = 174.6 * Math.pow(2, (hour % 12) / 12.0);
        return notes(Tones::brassAt, 2.2, base, new double[][]{
                {0, 0.24, 0}, {0.27, 0.24, 7}, {0.54, 0.2, 5}, {0.78, 1.1, 12}});
    }

    /** A typewriter's key: the type bar striking the platen, a little different each time. */
    static short[] key() {
        java.util.Random noise = new java.util.Random();
        double pitch = 1700 + noise.nextInt(500);
        return render(0.07, t -> (noise.nextDouble() * 2 - 1) * Math.exp(-180 * t) * 0.6
                + Math.sin(2 * Math.PI * pitch * t) * Math.exp(-90 * t) * 0.25
                + Math.sin(2 * Math.PI * 140 * t) * Math.exp(-60 * t) * 0.5, 0.45);
    }

    /** A line typed: keys at a typist's uneven pace, then the bell at the margin and the carriage return. */
    static short[] typing(int keys) {
        java.util.Random pace = new java.util.Random();
        java.util.List<short[]> strikes = new java.util.ArrayList<>();
        double[] at = new double[keys];
        double t = 0;
        for (int i = 0; i < keys; i++) {
            at[i] = t;
            strikes.add(key());
            t += 0.08 + pace.nextDouble() * 0.09;
        }
        double bell = t + 0.05, carriage = bell + 0.25;
        short[] ding = render(0.5, x -> Math.sin(2 * Math.PI * 2490 * x) * Math.exp(-7 * x)
                + 0.4 * Math.sin(2 * Math.PI * 4980 * x) * Math.exp(-11 * x), 0.35);
        java.util.Random noise = new java.util.Random(5);
        short[] zip = render(0.35, x -> (noise.nextDouble() * 2 - 1) * Math.min(1, x * 30) * Math.exp(-9 * x)
                * (0.6 + 0.4 * Math.sin(2 * Math.PI * 38 * x)), 0.3);
        int length = (int) ((carriage + 0.35) * RATE);
        int[] mix = new int[length];
        for (int i = 0; i < keys; i++) add(mix, strikes.get(i), at[i]);
        add(mix, ding, bell);
        add(mix, zip, carriage);
        short[] out = new short[length];
        for (int i = 0; i < length; i++) out[i] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, mix[i]));
        return out;
    }

    private static void add(int[] mix, short[] sound, double at) {
        int from = (int) (at * RATE);
        for (int i = 0; i < sound.length && from + i < mix.length; i++) mix[from + i] += sound[i];
    }

    /** Three rising pings: the power ration replenished. */
    static short[] charging() {
        double[] f = {523.3, 659.3, 784};
        return render(0.75, t -> {
            int i = Math.min(2, (int) (t / 0.18));
            double local = t - i * 0.18;
            return Math.sin(2 * Math.PI * f[i] * t) * Math.min(1, local * 200) * Math.exp(-7 * local) * 0.3;
        });
    }

    /** The boot chime: a low fifth, pitched a step higher for each uniform in the wardrobe. */
    static short[] boot(int uniform) {
        double base = 110 * Math.pow(2, uniform / 12.0);
        return render(1.1, t -> {
            double env = Math.min(1, t * 30) * Math.exp(-3 * t);
            return (Math.sin(2 * Math.PI * base * t) + 0.7 * Math.sin(2 * Math.PI * base * 1.5 * t)
                    + 0.35 * Math.sin(2 * Math.PI * base * 2 * t)) * env * 0.2;
        });
    }

    /** A relay closing: the click of a menu opening. */
    static short[] relay() {
        java.util.Random noise = new java.util.Random(7);
        return render(0.05, t -> (noise.nextDouble() * 2 - 1) * Math.exp(-260 * t) * 0.5
                + Math.sin(2 * Math.PI * 2900 * t) * Math.exp(-140 * t) * 0.35, 0.35);
    }

    /** The rubber stamp coming down: a thump with the pitch falling, and the slap of the paper. */
    static short[] stamp() {
        java.util.Random noise = new java.util.Random(11);
        double[] low = {0};
        return render(0.4, t -> {
            double thump = Math.sin(2 * Math.PI * (55 * t + 60 * (1 - Math.exp(-30 * t)) / 30)) * Math.exp(-11 * t);
            // The slap: noise, smoothed a little, gone in a few hundredths of a second.
            low[0] += 0.35 * ((noise.nextDouble() * 2 - 1) - low[0]);
            return thump + low[0] * Math.exp(-45 * t) * 1.4;
        }, 0.8);
    }

    /** A camera shutter: the blind opening and closing, with a little ring of the spring. */
    static short[] shutter() {
        java.util.Random noise = new java.util.Random(3);
        return render(0.22, t -> {
            double s = 0;
            for (double at : new double[]{0, 0.085}) {
                double local = t - at;
                if (local < 0) continue;
                s += (noise.nextDouble() * 2 - 1) * Math.exp(-120 * local)
                        + 0.4 * Math.sin(2 * Math.PI * 1850 * local) * Math.exp(-60 * local);
            }
            return s;
        }, 0.6);
    }

    /** The decree: L A I B A C H in Morse, on a telegraph's tone. */
    static short[] decree() {
        double unit = 0.06;
        java.util.List<double[]> beeps = new java.util.ArrayList<>();
        double at = 0;
        for (char ch : Morse.CODE.toCharArray()) {
            if (ch == ' ') {
                at += 2 * unit;
                continue;
            }
            double length = ch == '-' ? 3 * unit : unit;
            beeps.add(new double[]{at, length});
            at += length + unit;
        }
        return render(at + 0.05, t -> {
            for (double[] b : beeps) {
                double local = t - b[0];
                if (local >= 0 && local < b[1]) {
                    double edge = Math.min(1, Math.min(local, b[1] - local) * 400);
                    return Math.sin(2 * Math.PI * 820 * t) * edge;
                }
            }
            return 0;
        }, 0.6);
    }

    /**
     * The uniform's own call, a few notes long, played when the uniform is put
     * on. Each is written for this launcher in the spirit of its album; none
     * quotes a melody.
     */
    static short[] signature(String uniform) {
        switch (uniform) {
            case "OPUS DEI":
                // Three bells, falling to the octave below.
                return notes(Tones::bellAt, 2.8, 220, new double[][]{{0, 2.2, 0}, {0.5, 2.0, -5}, {1.0, 1.8, -12}});
            case "NOVA AKROPOLA":
                // Brass on the steps of the temple: the tonic twice, a minor third, a fifth held.
                return notes(Tones::brassAt, 2.0, 146.8, new double[][]{{0, 0.22, 0}, {0.25, 0.22, 0}, {0.5, 0.28, 3}, {0.8, 1.1, 7}});
            case "KAPITAL":
                // The till: a quick square-wave arpeggio up the major chord.
                return notes(Tones::squareAt, 1.0, 523.3, new double[][]{{0, 0.09, 0}, {0.1, 0.09, 4}, {0.2, 0.09, 7}, {0.3, 0.09, 12}, {0.42, 0.45, 16}});
            case "WAT":
                return wat();
            case "VOLK":
                // Voices entering one after another: tonic, fourth, fifth, octave.
                return notes(Tones::choirAt, 2.8, 196, new double[][]{{0, 2.6, 0}, {0.45, 2.15, 5}, {0.9, 1.7, 7}, {1.35, 1.25, 12}});
            case "IRON SKY":
                return theremin();
            case "SPECTRE":
                return spectre();
            case "ZARATHUSTRA":
                // A horn climbing out of the dark: tonic, third, sixth, the ninth held.
                return notes(Tones::brassAt, 2.6, 130.8, new double[][]{{0, 0.42, 0}, {0.45, 0.42, 4}, {0.9, 0.5, 9}, {1.45, 1.1, 14}});
            case "SOUND OF MUSIC":
                // An alphorn's natural notes, the fourth to the eighth harmonic of a low F.
                return notes(Tones::hornAt, 2.6, 87.3, new double[][]{{0, 0.55, 24}, {0.6, 0.55, 31.02}, {1.2, 0.4, 27.86}, {1.65, 0.9, 36}});
            default:
                // Plain signals: two short, one long.
                return notes(Tones::beepAt, 0.85, 880, new double[][]{{0, 0.1, 0}, {0.18, 0.1, 0}, {0.36, 0.38, 0}});
        }
    }

    /** One note's sound at time t into it, for a note of length seconds at frequency f. */
    private interface Voice {
        double at(double t, double length, double f);
    }

    /** Plays notes {start, length, semitones above base} in voice, peak-normalised. */
    private static short[] notes(Voice voice, double seconds, double base, double[][] notes) {
        return render(seconds, t -> {
            double s = 0;
            for (double[] n : notes) {
                double local = t - n[0];
                if (local >= 0) s += voice.at(local, n[1], base * Math.pow(2, n[2] / 12));
            }
            return s;
        }, 0.8);
    }

    /** Rises in a few milliseconds, holds for length, then dies away over release seconds. */
    private static double envelope(double t, double length, double attack, double release) {
        double rise = Math.min(1, t / attack);
        return t <= length ? rise : rise * Math.exp(-(t - length) / release);
    }

    private static double bellAt(double t, double length, double f) {
        double[] ratio = {0.5, 1.0, 1.183, 1.506, 2.0, 2.514};
        double[] decay = {1.0, 1.8, 2.4, 3.0, 3.8, 5.0};
        double s = 0;
        for (int i = 0; i < ratio.length; i++) s += Math.exp(-decay[i] * t) * Math.sin(2 * Math.PI * f * ratio[i] * t) / (i + 1);
        return s * Math.min(1, t * 400);
    }

    private static double brassAt(double t, double length, double f) {
        double vibrato = 1 + 0.004 * Math.sin(2 * Math.PI * 5.5 * t) * Math.min(1, t * 2);
        double s = 0;
        for (int h = 1; h <= 7; h++) s += Math.sin(2 * Math.PI * f * h * vibrato * t) / Math.pow(h, 1.1);
        return s * envelope(t, length, 0.04, 0.12);
    }

    private static double squareAt(double t, double length, double f) {
        double s = 0;
        for (int h = 1; h <= 9; h += 2) s += Math.sin(2 * Math.PI * f * h * t) / h;
        return s * envelope(t, length, 0.003, 0.05);
    }

    private static double choirAt(double t, double length, double f) {
        double s = 0;
        for (double detune : new double[]{-0.006, 0, 0.007}) {
            for (int h = 1; h <= 4; h++) {
                // An "ah": the second and third harmonics stronger than a plain tone.
                double weight = h == 1 ? 1 : h == 2 ? 0.8 : h == 3 ? 0.6 : 0.2;
                s += weight * Math.sin(2 * Math.PI * f * (1 + detune) * h * t + detune * 300);
            }
        }
        return s * envelope(t, length, 0.25, 0.4);
    }

    private static double hornAt(double t, double length, double f) {
        double s = 0;
        for (int h = 1; h <= 5; h++) s += Math.sin(2 * Math.PI * f * h * t) / Math.pow(h, 1.6);
        return s * envelope(t, length, 0.12, 0.35);
    }

    private static double beepAt(double t, double length, double f) {
        return Math.sin(2 * Math.PI * f * t) * (t < length ? Math.min(1, Math.min(t, length - t) * 300) : 0);
    }

    /** WAT: a kick on each beat under a sawtooth stab, the sound of the hall. */
    private static short[] wat() {
        double beat = 0.4, base = 164.8;
        double[] stab = {0, 0, 3, -2};
        return render(1.9, t -> {
            int i = Math.min(3, (int) (t / beat));
            double local = t - i * beat;
            double kick = Math.sin(2 * Math.PI * (45 * local + 90 * (1 - Math.exp(-35 * local)) / 35)) * Math.exp(-9 * local);
            double f = base * Math.pow(2, stab[i] / 12), saw = 0;
            for (int h = 1; h <= 10; h++) saw += Math.sin(2 * Math.PI * f * h * local) / h;
            double length = i == 3 ? 0.6 : 0.16;
            return kick * 1.2 + saw * 0.35 * envelope(local, length, 0.005, 0.06);
        }, 0.8);
    }

    /** IRON SKY: a theremin rising from the far side of the moon, wavering, and settling. */
    private static short[] theremin() {
        double[] phase = {0};
        return render(2.4, t -> {
            double glide = t < 1.2 ? 330 * Math.pow(2, t / 1.2) : 660 * Math.pow(2, -(t - 1.2) / 2.1);
            double f = glide * (1 + 0.012 * Math.sin(2 * Math.PI * 6 * t));
            phase[0] += 2 * Math.PI * f / RATE;
            double s = Math.sin(phase[0]) + 0.15 * Math.sin(2 * phase[0]);
            return s * Math.min(1, t * 4) * Math.min(1, (2.4 - t) * 3);
        }, 0.7);
    }

    /** SPECTRE: a detuned minor-seventh arpeggio through a coarse converter. */
    private static short[] spectre() {
        double base = 130.8, step = 0.12;
        double[] up = {0, 3, 7, 10, 12};
        double[] held = {0};
        return render(1.5, t -> {
            int i = Math.min(up.length - 1, (int) (t / step));
            double local = t - i * step, f = base * Math.pow(2, up[i] / 12), s = 0;
            for (double detune : new double[]{-0.008, 0.008}) {
                double p = f * (1 + detune) * local;
                s += 2 * (p - Math.floor(p + 0.5));
            }
            double length = i == up.length - 1 ? 0.7 : step;
            s *= 0.5 * envelope(local, length, 0.004, 0.08);
            // Five bits, at a quarter of the rate.
            if (((int) (t * RATE)) % 4 == 0) held[0] = Math.round(s * 16) / 16.0;
            return held[0];
        }, 0.75);
    }

    /** A sound played times over, with a pause between, for a ringtone or an alarm. */
    static short[] repeat(short[] once, int times, double pause) {
        int gap = (int) (pause * RATE);
        short[] out = new short[times * (once.length + gap)];
        for (int i = 0; i < times; i++) System.arraycopy(once, 0, out, i * (once.length + gap), once.length);
        return out;
    }

    /** The sound as a WAV file: 16-bit mono PCM at the sounds' own rate. */
    static byte[] wav(short[] pcm) {
        java.nio.ByteBuffer b = java.nio.ByteBuffer.allocate(44 + pcm.length * 2).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        b.put("RIFF".getBytes(java.nio.charset.StandardCharsets.US_ASCII)).putInt(36 + pcm.length * 2)
                .put("WAVEfmt ".getBytes(java.nio.charset.StandardCharsets.US_ASCII)).putInt(16)
                .putShort((short) 1).putShort((short) 1).putInt(RATE).putInt(RATE * 2)
                .putShort((short) 2).putShort((short) 16)
                .put("data".getBytes(java.nio.charset.StandardCharsets.US_ASCII)).putInt(pcm.length * 2);
        for (short s : pcm) b.putShort(s);
        return b.array();
    }

    interface Wave {
        double at(double t);
    }

    /** Renders wave and scales it so its loudest point is at peak. */
    private static short[] render(double seconds, Wave wave, double peak) {
        double[] raw = new double[(int) (seconds * RATE)];
        double max = 1e-9;
        for (int i = 0; i < raw.length; i++) {
            raw[i] = wave.at(i / (double) RATE);
            max = Math.max(max, Math.abs(raw[i]));
        }
        short[] pcm = new short[raw.length];
        for (int i = 0; i < raw.length; i++) pcm[i] = (short) Math.round(raw[i] / max * peak * 32767);
        return pcm;
    }

    private static short[] render(double seconds, Wave wave) {
        short[] pcm = new short[(int) (seconds * RATE)];
        for (int i = 0; i < pcm.length; i++) {
            double s = Math.max(-1, Math.min(1, wave.at(i / (double) RATE)));
            pcm[i] = (short) (s * 32767);
        }
        return pcm;
    }

    /** Plays pcm once as a notification sound, if the owner turned the sounds on. */
    static void play(Context c, short[] pcm) {
        if (!on(c)) return;
        AudioTrack track = track(pcm, AudioAttributes.USAGE_NOTIFICATION_EVENT, AudioTrack.MODE_STATIC);
        track.write(pcm, 0, pcm.length);
        track.setNotificationMarkerPosition(pcm.length - 1);
        track.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
            @Override
            public void onMarkerReached(AudioTrack t) {
                t.release();
            }

            @Override
            public void onPeriodicNotification(AudioTrack t) {
            }
        });
        track.play();
    }

    static AudioTrack track(short[] pcm, int usage, int mode) {
        return new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder().setUsage(usage)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                .setAudioFormat(new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(pcm.length * 2)
                .setTransferMode(mode)
                .build();
    }

    /** The alarm: the chosen tone over and over, rising from a whisper, until stopped. */
    static final class Ringer {
        private final AudioTrack track;
        private final Thread loop;
        private volatile boolean stopped;

        Ringer(boolean anthem) {
            short[] once = anthem ? anthem() : bell(2.6);
            track = track(once, AudioAttributes.USAGE_ALARM, AudioTrack.MODE_STREAM);
            long start = System.currentTimeMillis();
            loop = new Thread(() -> {
                track.play();
                while (!stopped && System.currentTimeMillis() - start < 10 * 60_000L) {
                    // From 5% to full over about a minute.
                    float gain = Math.min(1f, 0.05f + (System.currentTimeMillis() - start) / 60_000f);
                    track.setVolume(gain);
                    track.write(once, 0, once.length);
                }
                track.stop();
                track.release();
            }, "alarm");
            track.setVolume(0.05f);
        }

        void start() {
            loop.start();
        }

        void stop() {
            stopped = true;
            try {
                track.pause();
                track.flush();
            } catch (IllegalStateException ignored) {
                // Already released.
            }
        }
    }
}
