package dev.laibach.launcher;

import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioManager;

import java.util.Calendar;

/**
 * The anthem of the hour: on the hour, while the home screen is open, the
 * state's chime. It keeps quiet when the phone is silent or set to vibrate,
 * when do not disturb holds, during a call, through the curfew and from
 * 22:00 to 07:00. It is off until the owner turns it on, and only heard while
 * the sounds are on.
 */
final class Hourly {
    private static final String KEY = "hourly";
    /** The hour last chimed, so a clock set back or forward does not chime twice. */
    private static int last = -1;

    private Hourly() {}

    static boolean on(Context c) {
        return c.getSharedPreferences(Variant.PREFS, Context.MODE_PRIVATE).getBoolean(KEY, false);
    }

    static void setOn(Context c, boolean on) {
        c.getSharedPreferences(Variant.PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY, on).apply();
    }

    /** Called each minute the home screen is open: chimes if the minute is the hour's first. */
    static void tick(Context c) {
        Calendar now = Calendar.getInstance();
        int hour = now.get(Calendar.HOUR_OF_DAY);
        if (now.get(Calendar.MINUTE) != 0 || hour == last) return;
        last = hour;
        if (!on(c) || quiet(c, hour)) return;
        Tones.play(c, Tones.hourly(hour));
    }

    /** Whether the state holds its tongue at this hour. */
    static boolean quiet(Context c, int hour) {
        if (hour >= 22 || hour < 7 || Citizen.curfew(c)) return true;
        AudioManager audio = c.getSystemService(AudioManager.class);
        if (audio.getRingerMode() != AudioManager.RINGER_MODE_NORMAL || audio.getMode() != AudioManager.MODE_NORMAL) {
            return true;
        }
        int filter = c.getSystemService(NotificationManager.class).getCurrentInterruptionFilter();
        return filter != NotificationManager.INTERRUPTION_FILTER_ALL && filter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN;
    }
}
