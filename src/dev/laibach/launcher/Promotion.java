package dev.laibach.launcher;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Promotions: when the standing rises during the day (a forgiven confession
 * lifts it by a grade), and when attendance reaches 7, 30, 100 and 365
 * days, the citizen is decorated with a ceremony. The standing last seen is
 * kept in the preferences file {@code promotion}.
 */
final class Promotion {
    private static final String PREFS = "promotion";
    private static final int[] ORDERS = {7, 30, 100, 365};

    private Promotion() {}

    private static String today() {
        return new SimpleDateFormat("yyyyMMdd", Locale.ROOT).format(new Date());
    }

    /** Compares the standing with the one last seen today, and decorates a rise. */
    static void check(StateActivity a) {
        SharedPreferences p = a.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int grade = Citizen.grade(a);
        boolean sameDay = today().equals(p.getString("day", ""));
        int before = p.getInt("grade", grade);
        p.edit().putString("day", today()).putInt("grade", grade).apply();
        if (!sameDay || grade >= before) return;
        String[] standing = Citizen.STANDING[grade];
        Record.note(a, "PROMOTED TO " + standing[0] + ". THE STATE IS WATCHING YOU IMPROVE.",
                "BEFÖRDERT ZU " + standing[1] + ". DER STAAT SIEHT IHRE BESSERUNG.");
        Stamps.award(a, "promotion");
        Ceremony.show(a, StampArt.STAR, a.t("PROMOTION", "BEFÖRDERUNG"),
                a.t("YOU ARE RAISED TO", "SIE WERDEN ERHOBEN ZU") + "\n" + Lang.pick(a.german, standing));
    }

    /** Decorates attendance on the days the state counts: 7, 30, 100 and 365 without absence. */
    static void attendance(StateActivity a, int days) {
        if (days >= 7) Stamps.award(a, "week");
        if (days >= 30) Stamps.award(a, "month");
        for (int order : ORDERS) {
            if (days != order) continue;
            Record.note(a, "DECORATED: ORDER OF ATTENDANCE, " + days + " DAYS.",
                    "AUSGEZEICHNET: ORDEN DER ANWESENHEIT, " + days + " TAGE.");
            Ceremony.show(a, days == 7 ? StampArt.SEVEN : days == 30 ? StampArt.THIRTY : StampArt.STAR,
                    a.t("ORDER OF\nATTENDANCE", "ORDEN DER\nANWESENHEIT"),
                    String.format(a.locale, a.t("%d DAYS WITHOUT ABSENCE.\nTHE COLLECTIVE COUNTED EVERY ONE.",
                            "%d TAGE OHNE FEHLEN.\nDAS KOLLEKTIV HAT JEDEN GEZÄHLT."), days));
        }
    }
}
