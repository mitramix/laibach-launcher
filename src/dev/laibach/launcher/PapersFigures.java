package dev.laibach.launcher;

import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Calendar;
import java.util.Map;

/** The citizen's statistics of the day, each ministry's quota, and the day's drills. */
final class PapersFigures extends PapersPart {
    PapersFigures(PapersActivity papers) {
        super(papers);
    }

    /** The day's figures, two to a row. */
    void fill() {
        heading(t("STATISTICS OF THE DAY", "STATISTIK DES TAGES"));
        int day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        int year = Calendar.getInstance().get(Calendar.YEAR);
        int streak = Citizen.streak(papers);
        figures(t("ATTENDANCE", "ANWESENHEIT"),
                streak + (streak == 1 ? t(" DAY", " TAG") : t(" DAYS", " TAGE")),
                t("ATTENTION USED", "AUFMERKSAMKEIT"), span(Citizen.used(papers)));
        figures(t("EXEMPTIONS", "AUSNAHMEN"), String.valueOf(Citizen.exemptions(papers)),
                t("JOY COUPONS", "FREUDENMARKEN"), Citizen.joyLeft(papers) + " / " + Citizen.coupons(papers));
        figures(t("SELF-CRITICISMS", "SELBSTKRITIKEN"), String.valueOf(Citizen.confessions(papers)),
                t("RECORD ENTRIES", "AKTENEINTRÄGE"), Record.on(papers) ? String.valueOf(Record.entries(papers).size()) : t("NOT KEPT", "NICHT GEFÜHRT"));
        figures(t("DEFECTIONS", "REPUBLIKFLUCHTEN"), String.valueOf(ExitVisa.issued(papers)),
                t("STAMPS", "MARKEN"), Stamps.count(papers) + " / " + Stamps.ALL.length);
        figures(t("ENTRY VISAS", "EINREISEVISA"), String.valueOf(Passport.count(papers)),
                t("COMRADES", "GENOSSEN"), String.valueOf(Comrades.all(papers).size()));
        // Harmony is always nearly perfect; the norm is always exceeded.
        int seed = Norm.seed(year * 1000L + day);
        figures(t("COLLECTIVE HARMONY", "KOLLEKTIVE HARMONIE"),
                String.format(locale, "99.%02d%%", seed % 100).replace('.', german ? ',' : '.'),
                t("NORM FULFILMENT", "NORMERFÜLLUNG"), Norm.today(papers) + "%");

        quotas();
        drills();
    }

    /** Each ministry's allowance for the day, and which are closed for inventory. */
    private void quotas() {
        heading(t("MINISTRY QUOTAS", "KONTINGENTE DER MINISTERIEN"));
        if (!Quotas.on(papers) || !Citizen.usageAccess(papers)) {
            TextView off = type(11, v.muted, 80, 650);
            off.setText(!Quotas.on(papers)
                    ? t("SUSPENDED BY THE OWNER. THEY ARE ORDERED IN THE REGULATIONS.",
                            "VOM BESITZER AUSGESETZT. SIE WERDEN IN DEN VORSCHRIFTEN ANGEORDNET.")
                    : t("UNMEASURED. GRANT USAGE ACCESS AND THE MINISTRIES WILL KNOW.",
                            "UNGEMESSEN. GEWÄHREN SIE NUTZUNGSZUGRIFF, DANN WISSEN ES DIE MINISTERIEN."));
            off.setLetterSpacing(0.1f);
            content.addView(off);
            return;
        }
        Map<Ministry, Long> spent = Quotas.spent(papers);
        for (Ministry m : Ministry.values()) {
            long allowance = Quotas.allowance(m);
            if (allowance == 0) continue;
            boolean closed = Quotas.closed(papers, m, spent);
            LinearLayout r = row();
            r.setPadding(0, dp(6), 0, dp(6));
            TextView name = type(12, v.ink, 70, 800);
            name.setText(m.title(german));
            name.setLetterSpacing(0.06f);
            r.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            TextView state = type(12, closed ? v.accent : v.muted, 70, 850);
            state.setText(span(spent.getOrDefault(m, 0L)) + " / " + span(allowance) + "  "
                    + (closed ? t("CLOSED FOR INVENTORY", "WEGEN INVENTUR GESCHLOSSEN")
                    : Quotas.reopened(papers, m) ? t("REOPENED", "WIEDER GEÖFFNET") : t("OPEN", "GEÖFFNET")));
            state.setLetterSpacing(0.08f);
            r.addView(state);
            content.addView(r);
            content.addView(rule(v.rule), ruleParams(Math.max(1, dp(1)), 0, 0));
        }
    }

    /** The day's drills, each with its answer; left out while none is scheduled. */
    private void drills() {
        int[] times = Drills.times(papers);
        if (times.length == 0) return;
        heading(t("DRILL SCHEDULE", "ÜBUNGSPLAN"));
        for (int at : times) {
            char answer = Drills.answer(papers, at);
            String state = answer == Drills.PRESENT ? t("PRESENT", "ANWESEND")
                    : answer == Drills.ABSENT ? t("ABSENT", "ABWESEND")
                    : Drills.called(papers, at) ? t("MISSED", "VERSÄUMT") : t("PENDING", "AUSSTEHEND");
            LinearLayout r = row();
            r.setPadding(0, 0, 0, dp(6));
            TextView hour = type(15, v.ink, 25, 850);
            hour.setText(Drills.time(at));
            r.addView(hour, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            TextView mark = type(11, answer == Drills.PRESENT ? v.accent : v.muted, 80, 850);
            mark.setText(state);
            mark.setLetterSpacing(0.18f);
            r.addView(mark);
            content.addView(r);
        }
    }
}
