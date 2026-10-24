package dev.laibach.launcher;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.util.Calendar;

/**
 * Reveille: the state's own alarm, once a day at the chosen time, rung with a
 * bell or the state's fanfare and stopped only by answering the roll call.
 * Set in the menu (Public order → reveille). It is an alarm clock to Android,
 * so the phone shows it in the status bar and wakes for it in Doze.
 */
public final class Alarm {
    /** Minutes after midnight, or -1 when no reveille is set. */
    private static final String KEY = "alarm";
    private static final String KEY_TONE = "alarmTone";
    private static final String KEY_SNOOZE = "alarmSnooze";
    static final String BELL = "bell", ANTHEM = "anthem";

    private Alarm() {}

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(Variant.PREFS, Context.MODE_PRIVATE);
    }

    static int minutes(Context c) {
        return prefs(c).getInt(KEY, -1);
    }

    static String tone(Context c) {
        return prefs(c).getString(KEY_TONE, BELL);
    }

    static void setTone(Context c, String tone) {
        prefs(c).edit().putString(KEY_TONE, tone).apply();
    }

    /** Sets the reveille at hh:mm every day, or turns it off with -1. */
    static void set(Context c, int minutes) {
        prefs(c).edit().putInt(KEY, minutes).apply();
        schedule(c);
    }

    /** Whether Android lets the state ring at an exact time. */
    static boolean permitted(Context c) {
        return c.getSystemService(AlarmManager.class).canScheduleExactAlarms();
    }

    /** The next time the reveille rings, or 0. */
    static long next(Context c) {
        int m = minutes(c);
        if (m < 0) return 0;
        Calendar at = Calendar.getInstance();
        at.set(Calendar.HOUR_OF_DAY, m / 60);
        at.set(Calendar.MINUTE, m % 60);
        at.set(Calendar.SECOND, 0);
        at.set(Calendar.MILLISECOND, 0);
        if (at.getTimeInMillis() <= System.currentTimeMillis()) at.add(Calendar.DAY_OF_YEAR, 1);
        return at.getTimeInMillis();
    }

    /** Puts the next reveille (or none) in Android's hands. */
    static void schedule(Context c) {
        AlarmManager alarms = c.getSystemService(AlarmManager.class);
        PendingIntent ring = ring(c);
        alarms.cancel(ring);
        long at = next(c), snoozed = prefs(c).getLong(KEY_SNOOZE, 0);
        if (snoozed > System.currentTimeMillis() && (at == 0 || snoozed < at)) at = snoozed;
        if (at == 0 || !alarms.canScheduleExactAlarms()) return;
        PendingIntent show = PendingIntent.getActivity(c, 1, new Intent(c, HomeActivity.class),
                PendingIntent.FLAG_IMMUTABLE);
        alarms.setAlarmClock(new AlarmManager.AlarmClockInfo(at, show), ring);
    }

    /** Rings again in five minutes, once. */
    static void snooze(Context c) {
        prefs(c).edit().putLong(KEY_SNOOZE, System.currentTimeMillis() + 5 * 60_000L).apply();
        schedule(c);
    }

    /** Forgets a snooze once the reveille has rung. */
    static void rang(Context c) {
        prefs(c).edit().remove(KEY_SNOOZE).apply();
        schedule(c);
    }

    private static PendingIntent ring(Context c) {
        return PendingIntent.getActivity(c, 0, new Intent(c, AlarmActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /** Sets the reveille again after a restart, an update or a change of time. */
    public static final class Reschedule extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            schedule(context);
        }
    }
}
