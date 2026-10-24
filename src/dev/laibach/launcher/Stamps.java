package dev.laibach.launcher;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * The stamp album: one stamp for each first deed the state wishes to see
 * again. A stamp is issued once and kept with its date in the preferences
 * file {@code stamps}; nothing else is stored.
 */
final class Stamps {
    private static final String PREFS = "stamps";

    /** A stamp in the album: its key, its name in English and German, and the glyph it bears. */
    static final class Stamp {
        final String key;
        final String[] name;
        final int glyph;

        Stamp(String key, String en, String de, int glyph) {
            this.key = key;
            this.name = new String[]{en, de};
            this.glyph = glyph;
        }
    }

    static final Stamp[] ALL = {
            new Stamp("roll", "FIRST ROLL CALL", "ERSTER APPELL", StampArt.CHECK),
            new Stamp("week", "SEVEN DAYS PRESENT", "SIEBEN TAGE ANWESEND", StampArt.SEVEN),
            new Stamp("month", "THIRTY DAYS PRESENT", "DREISSIG TAGE ANWESEND", StampArt.THIRTY),
            new Stamp("confession", "FIRST CONFESSION", "ERSTES GESTÄNDNIS", StampArt.EYE),
            new Stamp("promotion", "PROMOTED", "BEFÖRDERT", StampArt.STAR),
            new Stamp("drill", "CURFEW DRILL", "SPERRSTUNDENÜBUNG", StampArt.MOON),
            new Stamp("self", "OWN PAPERS CHECKED", "EIGENE PAPIERE GEPRÜFT", StampArt.KEY),
            new Stamp("forgery", "FORGERY CONFISCATED", "FÄLSCHUNG EINGEZOGEN", StampArt.CROSS),
            new Stamp("visa", "EXIT VISA ISSUED", "AUSREISEVISUM ERTEILT", StampArt.ARROW),
            new Stamp("censor", "TEXT CENSORED", "TEXT ZENSIERT", StampArt.BARS),
            new Stamp("darkroom", "PRINT DEVELOPED", "ABZUG ENTWICKELT", StampArt.SUN),
            new Stamp("report", "WEEKLY REPORT READ", "WOCHENBERICHT GELESEN", StampArt.GEAR),
            new Stamp("foundation", "FOUNDATION DAY", "GRÜNDUNGSTAG", StampArt.EMBLEM),
    };

    private Stamps() {}

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static Stamp find(String key) {
        for (Stamp s : ALL) if (s.key.equals(key)) return s;
        throw new IllegalArgumentException(key);
    }

    /** When the stamp was issued, or 0 while it has not been. */
    static long issued(Context c, String key) {
        return prefs(c).getLong(key, 0);
    }

    static int count(Context c) {
        int n = 0;
        for (Stamp s : ALL) if (issued(c, s.key) > 0) n++;
        return n;
    }

    /** Issues the stamp unless it was issued before; true when it is new. */
    static boolean award(Context c, String key) {
        if (issued(c, key) > 0) return false;
        Stamp s = find(key);
        prefs(c).edit().putLong(key, System.currentTimeMillis()).apply();
        Record.note(c, "STAMP ISSUED: " + s.name[0] + ".", "MARKE AUSGEGEBEN: " + s.name[1] + ".");
        return true;
    }
}
