package dev.laibach.launcher;

import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Process;
import android.provider.Settings;

import java.security.SecureRandom;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * The citizen's papers: a number, a daily ration of attention and a standing
 * that follows from how much of the ration was used. Obedience is restraint:
 * the less time spent in other programmes, the better the standing. Time on
 * this home screen is time with the Collective and is not charged.
 *
 * With usage access (granted by the owner in Settings) the time comes from the
 * system's own usage events; without it, the home screen estimates it from the
 * time it spends out of sight while the screen is on.
 */
final class Citizen {
    static final long RATION = 2 * 60 * 60_000L;
    private static final String PREFS = "citizen";

    private Citizen() {}

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String today() {
        Calendar now = Calendar.getInstance();
        return now.get(Calendar.YEAR) + "-" + now.get(Calendar.DAY_OF_YEAR);
    }

    private static long startOfDay() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    /** Damm's table: one check digit catches every wrong digit and every swapped neighbour. */
    private static final int[][] DAMM = {
            {0, 3, 1, 7, 5, 9, 8, 6, 4, 2}, {7, 0, 9, 2, 1, 5, 4, 8, 6, 3}, {4, 2, 0, 6, 8, 7, 1, 3, 5, 9},
            {1, 7, 5, 0, 9, 8, 3, 4, 2, 6}, {6, 1, 2, 3, 0, 4, 5, 9, 7, 8}, {3, 6, 7, 4, 2, 0, 9, 5, 8, 1},
            {5, 8, 6, 9, 7, 2, 0, 1, 3, 4}, {8, 9, 4, 5, 3, 6, 2, 0, 1, 7}, {9, 4, 3, 8, 6, 1, 7, 2, 0, 5},
            {2, 5, 8, 1, 4, 3, 6, 7, 9, 0}};

    private static String number;

    /**
     * The citizen's number: the phone's app-scoped Android ID in full, 64 bits
     * as 20 digits, and a Damm check digit: 0438-2177-6512-0941-3378-7. Two
     * phones share it only if Android gave them the same ID. The ID survives a
     * reinstall, so the number does too.
     */
    static String number(Context c) {
        if (number != null) return number;
        String digits = String.format(Locale.ROOT, "%20s", Long.toUnsignedString(idBits(c))).replace(' ', '0');
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < 20; i += 4) out.append(digits, i, i + 4).append('-');
        return number = out.append(damm(digits)).toString();
    }

    /** The Android ID as 64 bits; a phone without one is issued a random number, kept from then on. */
    private static long idBits(Context c) {
        String id = Settings.Secure.getString(c.getContentResolver(), Settings.Secure.ANDROID_ID);
        // 9774d56d682e549c: the ID a whole run of early phones shared.
        if (id != null && !id.isEmpty() && !id.equalsIgnoreCase("9774d56d682e549c")) {
            try {
                return Long.parseUnsignedLong(id, 16);
            } catch (NumberFormatException ignored) {
                // Not the hex Android promises; issue a number instead.
            }
        }
        SharedPreferences p = prefs(c);
        if (!p.contains("number")) p.edit().putLong("number", new SecureRandom().nextLong()).commit();
        return p.getLong("number", 0);
    }

    /** The Damm check digit of a string of digits. */
    private static int damm(String digits) {
        int interim = 0;
        for (int i = 0; i < digits.length(); i++) interim = DAMM[interim][digits.charAt(i) - '0'];
        return interim;
    }

    @SuppressWarnings("deprecation")
    static boolean usageAccess(Context c) {
        AppOpsManager ops = c.getSystemService(AppOpsManager.class);
        return ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(),
                c.getPackageName()) == AppOpsManager.MODE_ALLOWED;
    }

    /** Attention used today in other programmes, in milliseconds. */
    static long used(Context c) {
        if (usageAccess(c)) {
            long measured = measured(c, startOfDay(), System.currentTimeMillis());
            if (measured >= 0) return measured;
        }
        SharedPreferences p = prefs(c);
        return today().equals(p.getString("day", "")) ? p.getLong("used", 0) : 0;
    }

    /** Foreground time of every other app between from and now, from the usage events; -1 if unreadable. */
    private static long measured(Context c, long from, long now) {
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
                    if (current != null && !current.equals(own)) total += e.getTimeStamp() - since;
                    current = e.getPackageName();
                    since = e.getTimeStamp();
                    break;
                case UsageEvents.Event.ACTIVITY_PAUSED:
                case UsageEvents.Event.ACTIVITY_STOPPED:
                    if (current != null && current.equals(e.getPackageName())) {
                        if (!current.equals(own)) total += e.getTimeStamp() - since;
                        current = null;
                    }
                    break;
                case UsageEvents.Event.SCREEN_NON_INTERACTIVE:
                case UsageEvents.Event.DEVICE_SHUTDOWN:
                    if (current != null && !current.equals(own)) total += e.getTimeStamp() - since;
                    current = null;
                    break;
                default:
                    break;
            }
        }
        if (current != null && !current.equals(own)) total += now - since;
        return total;
    }

    /**
     * Attention spent on each of the last days, oldest first, today last; -1
     * for a day that cannot be read. Needs usage access.
     */
    static long[] attentionByDay(Context c, int days) {
        long[] out = new long[days];
        Calendar day = Calendar.getInstance();
        day.setTimeInMillis(startOfDay());
        day.add(Calendar.DAY_OF_YEAR, -(days - 1));
        for (int i = 0; i < days; i++) {
            long from = day.getTimeInMillis();
            day.add(Calendar.DAY_OF_YEAR, 1);
            long to = Math.min(day.getTimeInMillis(), System.currentTimeMillis());
            out[i] = usageAccess(c) ? measured(c, from, to) : -1;
        }
        return out;
    }

    /** How often the phone was unlocked in the last days (the keyguard went away); -1 if unreadable. */
    static int unlocks(Context c, int days) {
        if (!usageAccess(c)) return -1;
        Calendar from = Calendar.getInstance();
        from.setTimeInMillis(startOfDay());
        from.add(Calendar.DAY_OF_YEAR, -(days - 1));
        UsageEvents events;
        try {
            events = c.getSystemService(UsageStatsManager.class).queryEvents(from.getTimeInMillis(),
                    System.currentTimeMillis());
        } catch (SecurityException e) {
            return -1;
        }
        if (events == null) return -1;
        int n = 0;
        UsageEvents.Event e = new UsageEvents.Event();
        while (events.getNextEvent(e)) if (e.getEventType() == UsageEvents.Event.KEYGUARD_HIDDEN) n++;
        return n;
    }

    // ---- Rationed and essential programmes -------------------------------------

    /** Programmes that warn before opening once the day's ration is spent; keys as in the home screen. */
    static Set<String> rationed(Context c) {
        return new HashSet<>(prefs(c).getStringSet("rationed", Set.of()));
    }

    /** Programmes that stay on the home screen during mobilisation. */
    static Set<String> essential(Context c) {
        return new HashSet<>(prefs(c).getStringSet("essential", Set.of()));
    }

    /** Puts key on or off one of the two lists ("rationed" or "essential"); returns whether it is on now. */
    static boolean toggle(Context c, String list, String key) {
        Set<String> set = new HashSet<>(prefs(c).getStringSet(list, Set.of()));
        boolean on = set.add(key) || !set.remove(key);
        prefs(c).edit().putStringSet(list, set).apply();
        return on;
    }

    /** When mobilisation ends, or 0 when the citizen is not mobilised. */
    static long mobilisedUntil(Context c) {
        long until = prefs(c).getLong("mobilisedUntil", 0);
        return until > System.currentTimeMillis() ? until : 0;
    }

    static void mobilise(Context c, long ms) {
        prefs(c).edit().putLong("mobilisedUntil", ms <= 0 ? 0 : System.currentTimeMillis() + ms).apply();
    }

    /** Adds estimated time away from the home screen (used without usage access). */
    static void charge(Context c, long ms) {
        if (ms <= 0) return;
        SharedPreferences p = prefs(c);
        long used = today().equals(p.getString("day", "")) ? p.getLong("used", 0) : 0;
        p.edit().putString("day", today()).putLong("used", used + ms).apply();
    }

    /** Exemptions from the curfew granted today. */
    static int exemptions(Context c) {
        SharedPreferences p = prefs(c);
        return today().equals(p.getString("exemptDay", "")) ? p.getInt("exemptions", 0) : 0;
    }

    /** Grants an exemption from the curfew for the given time and returns when it ends. */
    static long exempt(Context c, long ms) {
        long until = System.currentTimeMillis() + ms;
        prefs(c).edit().putString("exemptDay", today()).putInt("exemptions", exemptions(c) + 1)
                .putLong("exemptUntil", until).apply();
        return until;
    }

    static boolean exempted(Context c) {
        return System.currentTimeMillis() < prefs(c).getLong("exemptUntil", 0);
    }

    /**
     * Standing from A (exemplary) to F: one grade lower for every exemption and
     * every joy taken on credit, one higher for the day's forgiven self-criticism.
     */
    static int grade(Context c) {
        float share = used(c) / (float) RATION;
        int g = share <= 0.25f ? 0 : share <= 0.5f ? 1 : share <= 0.75f ? 2 : share <= 1f ? 3 : share <= 1.5f ? 4 : 5;
        return Math.max(0, Math.min(5, g + exemptions(c) + joyDebts(c) - (forgiven(c) ? 1 : 0)));
    }

    // ---- Joy ------------------------------------------------------------------

    /** Joy coupons issued each day, one for every opening of a Ministry of Joy programme. */
    static final int JOY_COUPONS = 5;

    private static int today(SharedPreferences p, String key) {
        return today().equals(p.getString(key + "Day", "")) ? p.getInt(key, 0) : 0;
    }

    private static void count(Context c, String key) {
        SharedPreferences p = prefs(c);
        p.edit().putString(key + "Day", today()).putInt(key, today(p, key) + 1).apply();
    }

    static int joyLeft(Context c) {
        return Math.max(0, JOY_COUPONS - today(prefs(c), "joySpent"));
    }

    /** Spends a coupon and returns how many remain. */
    static int spendJoy(Context c) {
        count(c, "joySpent");
        return joyLeft(c);
    }

    /** Joy taken today after the coupons ran out. */
    static int joyDebts(Context c) {
        return today(prefs(c), "joyDebts");
    }

    static void overdrawJoy(Context c) {
        count(c, "joyDebts");
    }

    // ---- The sealed terminal ---------------------------------------------------

    /** Counts a submission at the sealed terminal and returns today's count. */
    static int unseal(Context c) {
        count(c, "unseals");
        return unseals(c);
    }

    static int unseals(Context c) {
        return today(prefs(c), "unseals");
    }

    // ---- Roll call ------------------------------------------------------------

    /** Days since 1970 in local time, so that yesterday is today minus one. */
    private static long dayNumber() {
        Calendar now = Calendar.getInstance();
        return (now.getTimeInMillis() + now.get(Calendar.ZONE_OFFSET) + now.get(Calendar.DST_OFFSET)) / 86_400_000L;
    }

    /** Whether the morning roll call is due: after 06:00, and not answered since. */
    static boolean rollCallDue(Context c) {
        Calendar six = Calendar.getInstance();
        if (six.get(Calendar.HOUR_OF_DAY) < 6) return false;
        six.set(Calendar.HOUR_OF_DAY, 6);
        six.set(Calendar.MINUTE, 0);
        six.set(Calendar.SECOND, 0);
        six.set(Calendar.MILLISECOND, 0);
        return prefs(c).getLong("rollCallAt", 0) < six.getTimeInMillis();
    }

    /** Answers the roll call; present extends the attendance streak, absent ends it. */
    static void answerRollCall(Context c, boolean present) {
        SharedPreferences p = prefs(c);
        long today = dayNumber();
        SharedPreferences.Editor e = p.edit().putLong("rollCallAt", System.currentTimeMillis());
        if (present) {
            long last = p.getLong("present", -1);
            if (last != today) e.putInt("streak", last == today - 1 ? p.getInt("streak", 0) + 1 : 1);
            e.putLong("present", today);
        } else {
            e.putInt("streak", 0).remove("present");
        }
        e.apply();
    }

    /** Days in a row the citizen answered present. */
    static int streak(Context c) {
        SharedPreferences p = prefs(c);
        long last = p.getLong("present", -1);
        return last >= dayNumber() - 1 ? p.getInt("streak", 0) : 0;
    }

    // ---- Self-criticism -------------------------------------------------------

    /** Files a self-criticism; returns true if it is the first today, which is forgiven. */
    static boolean confess(Context c) {
        SharedPreferences p = prefs(c);
        boolean first = !forgiven(c);
        p.edit().putInt("confessions", p.getInt("confessions", 0) + 1)
                .putString("forgivenDay", today()).apply();
        return first;
    }

    static boolean forgiven(Context c) {
        return today().equals(prefs(c).getString("forgivenDay", ""));
    }

    static int confessions(Context c) {
        return prefs(c).getInt("confessions", 0);
    }

    static final String[][] STANDING = {
            {"A · EXEMPLARY", "A · VORBILDLICH"},
            {"B · OBEDIENT", "B · GEHORSAM"},
            {"C · ADEQUATE", "C · AUSREICHEND"},
            {"D · UNDER REVIEW", "D · IN PRÜFUNG"},
            {"E · UNDER OBSERVATION", "E · UNTER BEOBACHTUNG"},
            {"F · RE-EDUCATION SCHEDULED", "F · UMERZIEHUNG ANGESETZT"},
    };

    /** Whether the curfew (23:00 to 06:00) is on and holds now. */
    static boolean curfew(Context c) {
        if (!curfewOn(c)) return false;
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        return (h >= 23 || h < 6) && !exempted(c);
    }

    static boolean curfewOn(Context c) {
        return prefs(c).getBoolean("curfew", true);
    }

    static void setCurfewOn(Context c, boolean on) {
        prefs(c).edit().putBoolean("curfew", on).apply();
    }
}
