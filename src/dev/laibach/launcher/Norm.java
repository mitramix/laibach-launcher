package dev.laibach.launcher;

import android.content.Context;

import java.util.Calendar;

/**
 * The norm: the day's work, set by the state and always exceeded. The figure
 * is never below 100 per cent, and the worse the citizen's standing, the more
 * gloriously it is exceeded; reports are written that way.
 */
final class Norm {
    private Norm() {}

    /** Today's fulfilment in per cent: 101 to 122, the same all day. */
    static int today(Context c) {
        Calendar now = Calendar.getInstance();
        return 101 + seed(now.get(Calendar.YEAR) * 1000L + now.get(Calendar.DAY_OF_YEAR)) % 8 + 3 * Citizen.grade(c);
    }

    /** This week's fulfilment in per cent, for the weekly report: 102 to 124. */
    static int week(Context c, int overdrawnDays) {
        Calendar now = Calendar.getInstance();
        return 102 + seed(now.get(Calendar.YEAR) * 100L + now.get(Calendar.WEEK_OF_YEAR)) % 9 + 2 * overdrawnDays;
    }

    /** A number from 0 to 999 that changes with its key and looks like nothing in particular. */
    static int seed(long key) {
        return (int) Math.floorMod(key * 2654435761L, 1000L);
    }
}
