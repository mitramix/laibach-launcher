package dev.laibach.launcher;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.PackageManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Attention read from the system's usage events: how long each programme
 * other than the launcher was in front. Needs usage access; it stays on the
 * phone and is only ever added up.
 */
final class Attention {
    private Attention() {}

    /**
     * Foreground time of every other app between from and now; -1 if the
     * events cannot be read. perPackage (if any) receives each app's share.
     */
    static long measure(Context c, long from, long now, Map<String, Long> perPackage) {
        UsageStatsManager usm = c.getSystemService(UsageStatsManager.class);
        UsageEvents events;
        try {
            events = usm.queryEvents(from, now);
        } catch (SecurityException e) {
            return -1;
        }
        if (events == null) return -1;
        String own = c.getPackageName();
        String current = null;
        long since = 0, total = 0;
        UsageEvents.Event e = new UsageEvents.Event();
        while (events.getNextEvent(e)) {
            switch (e.getEventType()) {
                case UsageEvents.Event.ACTIVITY_RESUMED:
                    total += charge(perPackage, own, current, e.getTimeStamp() - since);
                    current = e.getPackageName();
                    since = e.getTimeStamp();
                    break;
                case UsageEvents.Event.ACTIVITY_PAUSED:
                case UsageEvents.Event.ACTIVITY_STOPPED:
                    if (current != null && current.equals(e.getPackageName())) {
                        total += charge(perPackage, own, current, e.getTimeStamp() - since);
                        current = null;
                    }
                    break;
                case UsageEvents.Event.SCREEN_NON_INTERACTIVE:
                case UsageEvents.Event.DEVICE_SHUTDOWN:
                    total += charge(perPackage, own, current, e.getTimeStamp() - since);
                    current = null;
                    break;
                default:
                    break;
            }
        }
        return total + charge(perPackage, own, current, now - since);
    }

    /** The time a spell in front costs: nothing for the launcher or for no app at all. */
    private static long charge(Map<String, Long> perPackage, String own, String pkg, long ms) {
        if (pkg == null || pkg.equals(own)) return 0;
        if (perPackage != null) perPackage.merge(pkg, ms, Long::sum);
        return ms;
    }

    /** Each app's attention since from; empty when it cannot be read. */
    static Map<String, Long> byPackage(Context c, long from) {
        Map<String, Long> out = new HashMap<>();
        if (!Citizen.usageAccess(c) || measure(c, from, System.currentTimeMillis(), out) < 0) out.clear();
        return out;
    }

    /** The app that took the most attention since from, as {package, ms}; null if none or unreadable. */
    static Object[] most(Context c, long from) {
        String top = null;
        long ms = 0;
        for (Map.Entry<String, Long> e : byPackage(c, from).entrySet()) {
            if (e.getValue() > ms) {
                top = e.getKey();
                ms = e.getValue();
            }
        }
        return top == null ? null : new Object[]{top, ms};
    }

    /** An app's name as the phone shows it, or its package if it has gone. */
    static String label(Context c, String pkg) {
        PackageManager pm = c.getPackageManager();
        try {
            return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return pkg;
        }
    }
}
