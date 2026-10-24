package dev.laibach.launcher;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The register of comrades: the citizen numbers exchanged with other
 * citizens, each with when it was registered, kept in the preferences file
 * {@code comrades}.
 */
final class Comrades {
    private static final String PREFS = "comrades";

    private Comrades() {}

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** The registered comrades as {digits, when registered}, the latest first. */
    static List<String[]> all(Context c) {
        List<String[]> out = new ArrayList<>();
        for (Map.Entry<String, ?> e : prefs(c).getAll().entrySet()) {
            if (e.getValue() instanceof Long) out.add(new String[]{e.getKey(), String.valueOf(e.getValue())});
        }
        out.sort((a, b) -> Long.compare(Long.parseLong(b[1]), Long.parseLong(a[1])));
        return out;
    }

    static boolean registered(Context c, String digits) {
        return prefs(c).contains(digits);
    }

    static void add(Context c, String digits) {
        prefs(c).edit().putLong(digits, System.currentTimeMillis()).apply();
    }

    static void remove(Context c, String digits) {
        prefs(c).edit().remove(digits).apply();
    }
}
