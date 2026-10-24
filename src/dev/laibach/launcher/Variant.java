package dev.laibach.launcher;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Calendar;

/**
 * The uniforms, each cut after a Laibach album: its colours, the emblem on
 * every badge, the scene behind the home screen and a motto of the state's
 * own. Shared by the home screen, the lock screen and the gallery. All of the
 * art is drawn here, none of it is taken from the records.
 */
final class Variant {
    static final String PREFS = "laibach";
    static final String KEY = "variant";

    /** Scenes behind the home screen; see {@link Cyber.Backdrop}. */
    static final int CATHEDRAL = 0, MARKET = 1, ANTHEM = 2, LUNAR = 3, NETWORK = 4,
            TEMPLE = 5, TECHNO = 6, DAWN = 7, ALPINE = 8;

    static final Variant[] ALL = {
            // 1980s: black, bone and blood, banners in a nave, a bell that tolls.
            new Variant("OPUS DEI", 0xFF0A0A0A, 0xFFECE6D6, 0xFFC1121F, 0xFF7A746A, 0xFF262320, 0xFFE9DFC8, false, CATHEDRAL,
                    "OBEY THE BELL. THE BELL OBEYS NO ONE.", "GEHORCHE DER GLOCKE. DIE GLOCKE GEHORCHT NIEMANDEM."),
            new Variant("NOVA AKROPOLA", 0xFF141414, 0xFFDCD8CE, 0xFF8C1C13, 0xFF77736B, 0xFF2A2826, 0xFFC2A66E, false, TEMPLE,
                    "BUILD THE NEW ACROPOLIS ON THE OLD MOUNTAIN.", "BAUT DIE NEUE AKROPOLIS AUF DEM ALTEN BERG."),
            // 1990s: paper and ink, the market's chart, a chip for a seal.
            new Variant("KAPITAL", 0xFFE8E2D0, 0xFF111111, 0xFFB0101A, 0xFF6E675A, 0xFFC9C1AC, 0xFF1F6FB0, true, MARKET,
                    "EVERYTHING HAS A PRICE. YOU ARE ON SALE.", "ALLES HAT EINEN PREIS. SIE SIND IM ANGEBOT."),
            new Variant("WAT", 0xFF000000, 0xFFF4F4F4, 0xFFE10600, 0xFF6B6B6B, 0xFF1E1E1E, 0xFFFFFFFF, false, TECHNO,
                    "WE ARE TIME. YOU ARE ON THE CLOCK.", "WIR SIND DIE ZEIT. SIE SIND IM TAKT."),
            // 2000s and after.
            new Variant("VOLK", 0xFF8E0F14, 0xFFF2EBDD, 0xFF0A0A0A, 0xFFE0A49C, 0xFF6E0A0E, 0xFFFFD27A, false, ANTHEM,
                    "ONE ANTHEM FOR EVERY NATION. ONE NATION FOR EVERY ANTHEM.",
                    "EINE HYMNE FÜR JEDES VOLK. EIN VOLK FÜR JEDE HYMNE."),
            new Variant("IRON SKY", 0xFF0D1117, 0xFFD7D2C4, 0xFFC9A24A, 0xFF5E6672, 0xFF232A33, 0xFF7FB8FF, false, LUNAR,
                    "WE CAME BACK FROM THE DARK SIDE. WE BROUGHT IT WITH US.",
                    "WIR KAMEN VON DER DUNKLEN SEITE ZURÜCK. WIR HABEN SIE MITGEBRACHT."),
            new Variant("SPECTRE", 0xFF05070B, 0xFFE4ECF5, 0xFFFF2340, 0xFF5D6B7E, 0xFF16202C, 0xFF00E5FF, false, NETWORK,
                    "A SPECTRE IS HAUNTING YOUR SCREEN.", "EIN GESPENST GEHT UM IN IHREM BILDSCHIRM."),
            new Variant("ZARATHUSTRA", 0xFF141024, 0xFFF1E6D0, 0xFFE8752A, 0xFF7B7290, 0xFF2A2340, 0xFFFFC857, false, DAWN,
                    "THE SUN RISES AGAIN. AND AGAIN. AND AGAIN.", "DIE SONNE GEHT WIEDER AUF. UND WIEDER. UND WIEDER."),
            new Variant("SOUND OF MUSIC", 0xFFEDF1E8, 0xFF1F2A24, 0xFFC8102E, 0xFF7E8A80, 0xFFCBD3C7, 0xFF2E7D5B, true, ALPINE,
                    "THE MOUNTAINS LISTEN. SING ALONG.", "DIE BERGE HÖREN ZU. SINGEN SIE MIT."),
            // Not an album: the uniform for weak eyes. Pure black, pure white, a yellow that cannot be missed.
            new Variant("HIGH CONTRAST", 0xFF000000, 0xFFFFFFFF, 0xFFFFE600, 0xFFCFCFCF, 0xFF5A5A5A, 0xFFFFFFFF, false, TECHNO,
                    "SEE EVERYTHING. EVERYTHING SEES YOU.", "SIEH ALLES. ALLES SIEHT DICH."),
    };

    /** The five-year plan: a uniform for each week, or for each day of the week. */
    static final String KEY_PLAN = "plan", WEEKLY = "weekly", WEEKDAY = "weekday";
    /** The album uniforms the plan rotates through; HIGH CONTRAST is chosen, never issued. */
    private static final int ALBUMS = 9;

    /** The uniform issued with LAIBACH LIFE 2.0. */
    static final String SPECTRE = "SPECTRE";

    final String name;
    final int bg, ink, accent, muted, rule;
    /** The second neon of the effects: the other half of every glitch. */
    final int glow;
    final boolean light;
    /** The scene behind the home screen, and the shape of the emblem. */
    final int motif;
    private final String[] motto;

    private Variant(String name, int bg, int ink, int accent, int muted, int rule, int glow, boolean light,
                    int motif, String motto, String mottoDe) {
        this.name = name;
        this.bg = bg;
        this.ink = ink;
        this.accent = accent;
        this.muted = muted;
        this.rule = rule;
        this.glow = glow;
        this.light = light;
        this.motif = motif;
        this.motto = new String[]{motto, mottoDe};
    }

    String motto(boolean german) {
        return Lang.pick(german, motto);
    }

    static Variant named(String name) {
        for (Variant x : ALL) if (x.name.equals(name)) return x;
        return ALL[0];
    }

    /** The variant picked on the home screen. */
    static Variant current(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String plan = p.getString(KEY_PLAN, "");
        if (!plan.isEmpty()) {
            Calendar now = Calendar.getInstance();
            if (WEEKLY.equals(plan)) return ALL[now.get(Calendar.WEEK_OF_YEAR) % ALBUMS];
            if (WEEKDAY.equals(plan)) {
                // Monday the first, Sunday the seventh uniform of the wardrobe.
                return ALL[(now.get(Calendar.DAY_OF_WEEK) + 5) % 7];
            }
        }
        return named(p.getString(KEY, ALL[0].name));
    }

    static String plan(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_PLAN, "");
    }

    /** Sets the plan ("" for none: the uniform picked by hand). */
    static void setPlan(Context context, String plan) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_PLAN, plan).apply();
    }
}
