package dev.laibach.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import java.util.Calendar;
import java.util.EnumMap;
import java.util.Map;

/**
 * Each ministry's daily allowance of attention. A ministry that has spent its
 * allowance is closed for inventory until midnight; the owner may open it
 * again for the rest of the day, and that is noted. Labour and the
 * administration are never closed. Off until the owner orders it, and only
 * enforced with usage access, which is how the allowance is measured.
 */
final class Quotas {
    private static final String PREFS = "quotas";
    private static final long MINUTE = 60_000L;

    private Quotas() {}

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static boolean on(Context c) {
        return prefs(c).getBoolean("on", false);
    }

    static void setOn(Context c, boolean on) {
        prefs(c).edit().putBoolean("on", on).apply();
    }

    /** A ministry's daily allowance; 0 for one that is never closed. */
    static long allowance(Ministry m) {
        switch (m) {
            case COMMUNICATION: return 60 * MINUTE;
            case ENLIGHTENMENT: return 60 * MINUTE;
            case JOY: return 45 * MINUTE;
            case MOVEMENT: return 30 * MINUTE;
            case PROSPERITY: return 20 * MINUTE;
            default: return 0;
        }
    }

    /** What each ministry has spent today; empty without usage access. */
    static Map<Ministry, Long> spent(Context c) {
        Map<Ministry, Long> out = new EnumMap<>(Ministry.class);
        PackageManager pm = c.getPackageManager();
        for (Map.Entry<String, Long> e : Attention.byPackage(c, Citizen.startOfDay()).entrySet()) {
            Ministry m;
            try {
                m = Ministry.of(c, pm.getApplicationInfo(e.getKey(), 0));
            } catch (PackageManager.NameNotFoundException gone) {
                continue;
            }
            out.merge(m, e.getValue(), Long::sum);
        }
        return out;
    }

    /** Whether the ministry is closed for inventory right now. */
    static boolean closed(Context c, Ministry m) {
        return closed(c, m, null);
    }

    /** As {@link #closed(Context, Ministry)}, from spending already read. */
    static boolean closed(Context c, Ministry m, Map<Ministry, Long> spent) {
        long allowance = allowance(m);
        if (allowance == 0 || !on(c) || !Citizen.usageAccess(c) || reopened(c, m)) return false;
        if (spent == null) spent = spent(c);
        Long used = spent.get(m);
        return used != null && used >= allowance;
    }

    /** The owner opens the ministry again until midnight. */
    static void reopen(Context c, Ministry m) {
        prefs(c).edit().putString("reopened." + m.name(), today()).apply();
    }

    static boolean reopened(Context c, Ministry m) {
        return today().equals(prefs(c).getString("reopened." + m.name(), ""));
    }

    private static String today() {
        Calendar now = Calendar.getInstance();
        return now.get(Calendar.YEAR) + "-" + now.get(Calendar.DAY_OF_YEAR);
    }
}
