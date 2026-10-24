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
 */
final class Record {
    private static final String PREFS = "record";
    private static final int KEEP = 100;

    private Record() {}

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Adds an entry with the time it was made. */
    static void note(Context c, String en, String de) {
        SharedPreferences p = prefs(c);
        List<String> lines = new ArrayList<>(List.of(p.getString("lines", "").split("\n")));
        lines.removeIf(String::isEmpty);
        lines.add(System.currentTimeMillis() + "\t" + clean(en) + "\t" + clean(de));
        while (lines.size() > KEEP) lines.remove(0);
        p.edit().putString("lines", String.join("\n", lines)).apply();
    }

    /** Adds an entry only the first time key is noted today. */
    static void noteDaily(Context c, String key, String en, String de) {
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
