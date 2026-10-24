package dev.laibach.launcher;

import android.content.Context;
import android.content.pm.PackageManager;

import java.util.Calendar;

/**
 * Foundation Day: the anniversary of the day this terminal was installed.
 * The state issues an extra joy coupon and holds a ceremony, once a year.
 * A terminal founded on 29 February keeps its day on 28 February in other
 * years.
 */
final class Foundation {
    private static final String PREFS = "foundation";

    private Foundation() {}

    /** When the terminal was first installed. */
    static long founded(Context c) {
        try {
            return c.getPackageManager().getPackageInfo(c.getPackageName(), 0).firstInstallTime;
        } catch (PackageManager.NameNotFoundException e) {
            return System.currentTimeMillis();
        }
    }

    /** Whole years from the founding to now. */
    static int years(long founded, long now) {
        Calendar f = Calendar.getInstance(), n = Calendar.getInstance();
        f.setTimeInMillis(founded);
        n.setTimeInMillis(now);
        int years = n.get(Calendar.YEAR) - f.get(Calendar.YEAR);
        int fm = f.get(Calendar.MONTH), nm = n.get(Calendar.MONTH);
        if (nm < fm || nm == fm && n.get(Calendar.DAY_OF_MONTH) < observed(f, n.get(Calendar.YEAR))) years--;
        return Math.max(0, years);
    }

    /** Whether now falls on an anniversary of the founding, not counting the founding day itself. */
    static boolean anniversary(long founded, long now) {
        Calendar f = Calendar.getInstance(), n = Calendar.getInstance();
        f.setTimeInMillis(founded);
        n.setTimeInMillis(now);
        int year = n.get(Calendar.YEAR);
        return year > f.get(Calendar.YEAR)
                && n.get(Calendar.MONTH) == f.get(Calendar.MONTH)
                && n.get(Calendar.DAY_OF_MONTH) == observed(f, year);
    }

    /** The founding's day of the month as observed in a given year: 29 February falls back to the 28th. */
    private static int observed(Calendar founded, int year) {
        int day = founded.get(Calendar.DAY_OF_MONTH);
        if (founded.get(Calendar.MONTH) == Calendar.FEBRUARY && day == 29) {
            Calendar y = Calendar.getInstance();
            y.clear();
            y.set(year, Calendar.FEBRUARY, 1);
            return Math.min(day, y.getActualMaximum(Calendar.DAY_OF_MONTH));
        }
        return day;
    }

    static boolean today(Context c) {
        return anniversary(founded(c), System.currentTimeMillis());
    }

    /** Holds the year's ceremony the first time the terminal is seen on Foundation Day. */
    static void celebrate(StateActivity a) {
        if (!today(a)) return;
        int year = Calendar.getInstance().get(Calendar.YEAR);
        if (a.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt("held", 0) == year) return;
        a.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt("held", year).apply();
        int years = years(founded(a), System.currentTimeMillis());
        Record.note(a, "FOUNDATION DAY: YEAR " + years + " OF CITIZENSHIP. AN EXTRA JOY COUPON WAS ISSUED.",
                "GRÜNDUNGSTAG: JAHR " + years + " DER BÜRGERSCHAFT. EINE ZUSÄTZLICHE FREUDENMARKE WURDE AUSGEGEBEN.");
        Stamps.award(a, "foundation");
        Ceremony.show(a, StampArt.EMBLEM, a.t("FOUNDATION\nDAY", "GRÜNDUNGS-\nTAG"),
                String.format(a.locale, a.t("YEAR %d OF YOUR CITIZENSHIP.\nAN EXTRA JOY COUPON HAS BEEN ISSUED.",
                        "JAHR %d IHRER BÜRGERSCHAFT.\nEINE ZUSÄTZLICHE FREUDENMARKE WURDE AUSGEGEBEN."), years));
    }
}
