package dev.laibach.launcher;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;

/**
 * Who puts the sealed terminal (LockActivity) up, and whether it is up: the
 * owner's switch, the moment the screen goes off, and the home screen's hook
 * for when the citizen is let back in.
 */
final class Terminal {
    private static final String KEY_SEAL = "seal";

    /** True while the terminal is on screen, so the home screen does not charge the time spent here. */
    static boolean sealed;
    /** Called once the citizen is let back in; the home screen starts counting from there. */
    static Runnable onUnseal;

    private Terminal() {}

    static boolean enabled(Context c) {
        return c.getSharedPreferences(Variant.PREFS, Context.MODE_PRIVATE).getBoolean(KEY_SEAL, true);
    }

    static void setEnabled(Context c, boolean on) {
        c.getSharedPreferences(Variant.PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_SEAL, on).apply();
    }

    /** Puts the terminal up as the screen goes off, unless a call is on (the screen also goes off at the ear). */
    static void seal(Context c) {
        if (!enabled(c)) return;
        int mode = c.getSystemService(AudioManager.class).getMode();
        if (mode == AudioManager.MODE_IN_CALL || mode == AudioManager.MODE_IN_COMMUNICATION
                || mode == AudioManager.MODE_RINGTONE) return;
        try {
            c.startActivity(new Intent(c, LockActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION));
        } catch (RuntimeException e) {
            // The system may refuse; the phone's own lock is there either way.
        }
    }
}
