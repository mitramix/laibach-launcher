package dev.laibach.launcher;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * The citizen's permanent record: what the home screen noticed, in English and
 * German, newest last. It keeps the last 100 entries and nothing else; no app
 * content, no notification text, only what the citizen did on this home screen.
 * It is kept only once the owner asks for it in the regulations; until then,
 * and after it is closed again, nothing is written and nothing is left.
 */
final class Record {
    private static final String PREFS = "record";
    private static final int KEEP = 100;

    private Record() {}

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Whether the record is kept: off until the owner turns it on. */
    static boolean on(Context c) {
        return prefs(c).getBoolean("kept", false);
    }

    /** Opens or closes the record; closed, its entries are destroyed. */
    static void setOn(Context c, boolean on) {
        SharedPreferences.Editor e = prefs(c).edit().putBoolean("kept", on);
        if (!on) e.remove("lines");
        e.apply();
    }

    /** A record kept before it could be closed is destroyed on the first look, unless it is kept. */
    static void forgetIfClosed(Context c) {
        if (!on(c) && prefs(c).contains("lines")) prefs(c).edit().remove("lines").apply();
    }

    /** Adds an entry with the time it was made, if the record is kept. */
    static void note(Context c, String en, String de) {
        if (!on(c)) return;
        SharedPreferences p = prefs(c);
        List<String> lines = new ArrayList<>(List.of(p.getString("lines", "").split("\n")));
        lines.removeIf(String::isEmpty);
        lines.add(System.currentTimeMillis() + "\t" + clean(en) + "\t" + clean(de));
        while (lines.size() > KEEP) lines.remove(0);
        p.edit().putString("lines", String.join("\n", lines)).apply();
    }

    /** Adds an entry only the first time key is noted today. */
    static void noteDaily(Context c, String key, String en, String de) {
        if (!on(c)) return;
        SharedPreferences p = prefs(c);
        Calendar now = Calendar.getInstance();
        String today = now.get(Calendar.YEAR) + "-" + now.get(Calendar.DAY_OF_YEAR);
        if (today.equals(p.getString("daily." + key, ""))) return;
        p.edit().putString("daily." + key, today).apply();
        note(c, en, de);
    }

    /** Entries newest first, as {time in ms, English, German}. */
    static List<String[]> entries(Context c) {
        List<String[]> out = new ArrayList<>();
        if (!on(c)) return out;
        for (String line : prefs(c).getString("lines", "").split("\n")) {
            String[] f = line.split("\t", -1);
            if (f.length == 3) out.add(0, f);
        }
        return out;
    }

    /** Deletes the record, which is itself recorded. */
    static void clear(Context c) {
        prefs(c).edit().remove("lines").apply();
        note(c, "RECORD DELETED. THE DELETION HAS BEEN NOTED.",
                "AKTE GELÖSCHT. DIE LÖSCHUNG WURDE VERMERKT.");
    }

    private static String clean(String s) {
        return s.replace('\t', ' ').replace('\n', ' ');
    }
}
