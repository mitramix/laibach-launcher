package dev.laibach.launcher;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * The drill schedule: roll calls at hours the owner sets, each answered
 * present or absent like the morning's, for a citizen who keeps a habit by
 * them. The hours and the day's answers are kept in the preferences file
 * {@code drills}; an absence is written down but does not end the attendance.
 */
final class Drills extends HomePart {
    private static final String PREFS = "drills";
    /** Answered or not, a drill is a drill; eight a day is already a regime. */
    static final int MOST = 8;
    /** A drill's answer: present, absent, or neither yet. */
    static final char PRESENT = 'p', ABSENT = 'a', OPEN = '-';

    Drills(HomeActivity home) {
        super(home);
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String today() {
        return new SimpleDateFormat("yyyyMMdd", Locale.ROOT).format(new Date());
    }

    /** The drills' hours, as minutes of the day, earliest first. */
    static int[] times(Context c) {
        String s = prefs(c).getString("times", "");
        if (s.isEmpty()) return new int[0];
        String[] parts = s.split(",");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) out[i] = Integer.parseInt(parts[i]);
        java.util.Arrays.sort(out);
        return out;
    }

    /**
     * Sets the hours. Drills whose hour has passed today are not called until
     * tomorrow: a schedule made at noon does not summon the morning's.
     */
    static void setTimes(Context c, int[] times) {
        StringBuilder b = new StringBuilder();
        for (int t : times) {
            if (b.length() > 0) b.append(',');
            b.append(t);
        }
        prefs(c).edit().putString("times", b.toString()).putLong("since", System.currentTimeMillis()).apply();
    }

    static String time(int minutes) {
        return String.format(Locale.ROOT, "%02d:%02d", minutes / 60, minutes % 60);
    }

    /** The day's answers, as minute → 'p' or 'a'; yesterday's are forgotten. */
    private static List<int[]> answers(Context c) {
        List<int[]> out = new ArrayList<>();
        SharedPreferences p = prefs(c);
        if (!today().equals(p.getString("day", ""))) return out;
        for (String a : p.getString("answers", "").split(",")) {
            if (a.length() < 2) continue;
            out.add(new int[]{Integer.parseInt(a.substring(0, a.length() - 1)), a.charAt(a.length() - 1)});
        }
        return out;
    }

    /** How the drill at this minute was answered today, or OPEN. */
    static char answer(Context c, int minute) {
        for (int[] a : answers(c)) if (a[0] == minute) return (char) a[1];
        return OPEN;
    }

    /** The millisecond the drill at this minute is called today. */
    private static long at(int minute) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, minute / 60);
        c.set(Calendar.MINUTE, minute % 60);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    /** Whether the drill at this minute has been called today: its hour has come, after the schedule was set. */
    static boolean called(Context c, int minute) {
        long when = at(minute);
        return when <= System.currentTimeMillis() && when > prefs(c).getLong("since", 0);
    }

    /** The latest drill called and not answered, as minutes of the day; -1 when none waits. */
    static int due(Context c) {
        int[] times = times(c);
        for (int i = times.length - 1; i >= 0; i--) {
            if (!called(c, times[i])) continue;
            // Only the latest is called; one passed over while the phone lay unanswered stays missed.
            return answer(c, times[i]) == OPEN ? times[i] : -1;
        }
        return -1;
    }

    static void answer(Context c, int minute, boolean present) {
        SharedPreferences p = prefs(c);
        String day = today(), before = day.equals(p.getString("day", "")) ? p.getString("answers", "") : "";
        String add = minute + (present ? "p" : "a");
        p.edit().putString("day", day).putString("answers", before.isEmpty() ? add : before + "," + add).apply();
    }

    /** Drills answered present today. */
    static int present(Context c) {
        int n = 0;
        for (int[] a : answers(c)) if (a[1] == PRESENT) n++;
        return n;
    }

    // ---- The schedule's form ------------------------------------------------

    /** The schedule: each drill's hour, tapped to strike it; a new one added on the clock. */
    void edit() {
        int[] times = times(home);
        AlertDialog.Builder b = new AlertDialog.Builder(home, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(t("Drill schedule", "Übungsplan"))
                .setNegativeButton(t("Done", "Fertig"), null);
        if (times.length == 0) {
            b.setMessage(t("No drills are ordered. Each drill is a roll call at its hour, answered present or absent.",
                    "Keine Übungen angeordnet. Jede Übung ist ein Appell zu ihrer Stunde, beantwortet mit anwesend oder abwesend."));
        } else {
            String[] items = new String[times.length];
            for (int i = 0; i < times.length; i++) items[i] = time(times[i]) + "   ✕ " + t("strike", "streichen");
            b.setItems(items, (d, which) -> {
                int[] left = new int[times.length - 1];
                for (int i = 0, j = 0; i < times.length; i++) if (i != which) left[j++] = times[i];
                order(left);
                edit();
            });
        }
        if (times.length < MOST) b.setPositiveButton(t("Add a drill", "Übung hinzufügen"), (d, w) -> add(times));
        StateMenu.dress(b.show(), v);
    }

    private void add(int[] times) {
        Calendar now = Calendar.getInstance();
        TimePickerDialog d = new TimePickerDialog(home, android.R.style.Theme_DeviceDefault_Dialog_Alert,
                (picker, hour, minute) -> {
                    int at = hour * 60 + minute;
                    for (int t : times) {
                        if (t == at) {
                            edit();
                            return;
                        }
                    }
                    int[] more = java.util.Arrays.copyOf(times, times.length + 1);
                    more[times.length] = at;
                    java.util.Arrays.sort(more);
                    order(more);
                    edit();
                }, now.get(Calendar.HOUR_OF_DAY) + 1 < 24 ? now.get(Calendar.HOUR_OF_DAY) + 1 : 0, 0, true);
        d.setTitle(t("A drill at", "Eine Übung um"));
        d.show();
        StateMenu.dress(d, v);
    }

    private void order(int[] times) {
        setTimes(home, times);
        StringBuilder en = new StringBuilder();
        for (int t : times) en.append(en.length() > 0 ? ", " : "").append(time(t));
        if (times.length == 0) {
            Record.note(home, "DRILL SCHEDULE CANCELLED. THE CITIZEN DRILLS UNSUPERVISED.",
                    "ÜBUNGSPLAN AUFGEHOBEN. DER BÜRGER ÜBT UNBEAUFSICHTIGT.");
        } else {
            Record.note(home, "DRILL SCHEDULE ORDERED: " + en + ".", "ÜBUNGSPLAN ANGEORDNET: " + en + ".");
        }
        Tones.play(home, Tones.stamp());
        home.rollCall.update();
        home.regulations.refresh();
    }
}
