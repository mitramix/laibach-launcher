package dev.laibach.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * The weekly report: attention spent on each of the last seven days against
 * the ration, unlocks, attendance and the record's growth. Issued on Sunday,
 * the first time the home screen is seen that day, and on request from the
 * menu or the papers. Read from the phone's own usage statistics; nothing
 * leaves the phone.
 */
final class Report {
    private static final String KEY_ISSUED = "reportWeek";

    private Report() {}

    /** Whether today is Sunday and this week's report has not been issued yet. */
    static boolean due(Context c) {
        Calendar now = Calendar.getInstance();
        return now.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
                && !week().equals(prefs(c).getString(KEY_ISSUED, ""));
    }

    /** Shows the report in the uniform and marks this week's as issued. */
    static void show(Activity a, Variant v) {
        boolean german = Lang.german(a);
        prefs(a).edit().putString(KEY_ISSUED, week()).apply();
        StateMenu.dress(new AlertDialog.Builder(a, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(title(german))
                .setMessage(String.join("\n", lines(a, german)))
                .setPositiveButton(t(german, "Acknowledged", "Zur Kenntnis genommen"), null)
                .show(), v);
    }

    static String title(boolean german) {
        return t(german, "Weekly report · week ", "Wochenbericht · Woche ")
                + Calendar.getInstance().get(Calendar.WEEK_OF_YEAR);
    }

    /** The report as lines of text, for the dialog and the case file. */
    static List<String> lines(Context c, boolean german) {
        Locale locale = Lang.locale(german);
        List<String> out = new ArrayList<>();
        long[] days = Citizen.attentionByDay(c, 7);
        int overdrawn = 0;
        if (days[0] < 0) {
            out.add(t(german, "ATTENTION: UNREADABLE. GRANT USAGE ACCESS AND THE STATE WILL KNOW.",
                    "AUFMERKSAMKEIT: UNLESBAR. GEWÄHREN SIE NUTZUNGSZUGRIFF, DANN WEISS ES DER STAAT."));
        } else {
            out.add(t(german, "ATTENTION SPENT (RATION 2H A DAY)", "VERBRAUCHTE AUFMERKSAMKEIT (ZUTEILUNG 2 STD. AM TAG)"));
            SimpleDateFormat day = new SimpleDateFormat("EEE d", locale);
            Calendar at = Calendar.getInstance();
            at.add(Calendar.DAY_OF_YEAR, -6);
            long total = 0;
            int over = 0;
            for (long ms : days) {
                total += ms;
                if (ms > Citizen.RATION) over++;
                out.add("  " + day.format(at.getTime()).toUpperCase(locale) + "  " + span(ms)
                        + (ms > Citizen.RATION ? t(german, "  ▲ OVERDRAWN", "  ▲ ÜBERZOGEN") : ""));
                at.add(Calendar.DAY_OF_YEAR, 1);
            }
            out.add(t(german, "TOTAL ", "GESAMT ") + span(total) + t(german, " · DAILY AVERAGE ", " · TAGESMITTEL ")
                    + span(total / 7));
            Object[] most = Attention.most(c, Citizen.startOfDay() - 6 * 24 * 60 * 60_000L);
            if (most != null) {
                out.add(String.format(locale, t(german, "THE INFORMANT REPORTS: MOST OF THE RATION WENT TO %s (%s).",
                                "DER INFORMANT MELDET: DER GRÖSSTE TEIL DER ZUTEILUNG GING AN %s (%s)."),
                        Attention.label(c, (String) most[0]).toUpperCase(locale), span((long) most[1])));
            }
            overdrawn = over;
            out.add("");
            out.add(over == 0
                    ? t(german, "THE RATION WAS RESPECTED ON EVERY DAY. THIS IS NOTED WITH SATISFACTION.",
                            "DIE ZUTEILUNG WURDE AN JEDEM TAG EINGEHALTEN. DIES WIRD MIT GENUGTUUNG VERMERKT.")
                    : String.format(locale, t(german, "THE RATION WAS OVERDRAWN ON %d DAY(S). THIS IS NOTED.",
                            "DIE ZUTEILUNG WURDE AN %d TAG(EN) ÜBERZOGEN. DIES WIRD VERMERKT."), over));
        }
        out.add(String.format(locale, t(german, "THE NORM WAS FULFILLED AT %d%%. THE NEXT NORM HAS BEEN RAISED ACCORDINGLY.",
                "DIE NORM WURDE ZU %d%% ERFÜLLT. DIE NÄCHSTE NORM WURDE ENTSPRECHEND ERHÖHT."), Norm.week(c, overdrawn)));
        int unlocks = Citizen.unlocks(c, 7);
        out.add(t(german, "UNLOCKS THIS WEEK: ", "ENTSPERRUNGEN DIESE WOCHE: ")
                + (unlocks < 0 ? t(german, "UNREADABLE", "UNLESBAR") : String.valueOf(unlocks)));
        int streak = Citizen.streak(c);
        out.add(t(german, "ATTENDANCE: ", "ANWESENHEIT: ") + streak
                + (streak == 1 ? t(german, " DAY IN A ROW", " TAG IN FOLGE") : t(german, " DAYS IN A ROW", " TAGE IN FOLGE")));
        long since = System.currentTimeMillis() - 7 * 24 * 60 * 60_000L;
        int noted = 0;
        for (String[] e : Record.entries(c)) {
            try {
                if (Long.parseLong(e[0]) >= since) noted++;
            } catch (NumberFormatException ignored) {
                // An entry without a time is not counted.
            }
        }
        out.add(t(german, "NEW RECORD ENTRIES: ", "NEUE AKTENEINTRÄGE: ") + noted);
        out.add(t(german, "CURFEW: ", "SPERRSTUNDE: ") + (Citizen.curfewOn(c)
                ? t(german, "IN FORCE, 23:00–06:00", "IN KRAFT, 23:00–06:00")
                : t(german, "SUSPENDED", "AUSGESETZT")));
        out.add(t(german, "STANDING: ", "ANSEHEN: ") + Lang.pick(german, Citizen.STANDING[Citizen.grade(c)]));
        return out;
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(Variant.PREFS, Context.MODE_PRIVATE);
    }

    private static String week() {
        Calendar now = Calendar.getInstance();
        return now.get(Calendar.YEAR) + "-" + now.get(Calendar.WEEK_OF_YEAR);
    }

    static String span(long ms) {
        long minutes = Math.max(0, ms) / 60_000;
        return minutes < 60 ? minutes + "M" : minutes / 60 + "H " + minutes % 60 + "M";
    }

    private static String t(boolean german, String en, String de) {
        return german ? de : Lang.en(en);
    }
}
