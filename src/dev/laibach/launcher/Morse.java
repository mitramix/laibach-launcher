package dev.laibach.launcher;

import android.content.Context;
import android.os.VibrationEffect;
import android.os.Vibrator;

/**
 * The state taps its name on the citizen's hand: L A I B A C H in Morse, as
 * a vibration. Off until the owner turns it on (Public order → Morse). It is
 * felt when new decrees arrive and when the alarm rings; Android lets no app
 * but the phone app choose how calls vibrate, so calls keep their own.
 */
final class Morse {
    private static final String KEY = "morse";
    private static final long UNIT = 70;
    static final String CODE = ".-.. .- .. -... .- -.-. ....";

    private Morse() {}

    static boolean on(Context c) {
        return c.getSharedPreferences(Variant.PREFS, Context.MODE_PRIVATE).getBoolean(KEY, false);
    }

    static void setOn(Context c, boolean on) {
        c.getSharedPreferences(Variant.PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY, on).apply();
    }

    /** The name as off/on timings: dot one unit, dash three, a gap of one, three between letters. */
    static long[] timings() {
        long[] t = new long[CODE.length() * 2 + 1];
        int n = 0;
        t[n++] = 0;
        for (int i = 0; i < CODE.length(); i++) {
            char ch = CODE.charAt(i);
            if (ch == ' ') {
                // Lengthen the last gap to three units.
                t[n - 1] += 2 * UNIT;
                continue;
            }
            t[n++] = ch == '-' ? 3 * UNIT : UNIT;
            t[n++] = UNIT;
        }
        long[] out = new long[n];
        System.arraycopy(t, 0, out, 0, n);
        return out;
    }

    /** Taps the name once, if the owner wants it, or always when forced (the alarm). */
    static void tap(Context c, boolean force) {
        if (!force && !on(c)) return;
        Vibrator vibrator = c.getSystemService(Vibrator.class);
        if (vibrator == null || !vibrator.hasVibrator()) return;
        vibrator.vibrate(VibrationEffect.createWaveform(timings(), -1));
    }
}
