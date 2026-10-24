package dev.laibach.launcher;

import android.graphics.Insets;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

/** The morning roll call: once a day after 06:00 the citizen reports present. */
final class RollCall extends HomePart {
    View view;
    TextView streak;
    /** The roll call was summoned from the menu rather than by the morning. */
    boolean summoned;

    RollCall(HomeActivity home) {
        super(home);
    }

    boolean showing() {
        return view.getVisibility() == View.VISIBLE;
    }

    View build() {
        LinearLayout c = column();
        c.setBackgroundColor(v.bg);
        c.setClickable(true);
        c.setVisibility(View.GONE);
        c.setOnApplyWindowInsetsListener((view, insets) -> {
            Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            view.setPadding(bars.left + dp(28), bars.top + dp(10), bars.right + dp(28), bars.bottom + dp(28));
            return insets;
        });

        TextView name = type(16, v.ink, 25, 1000);
        name.setText(t("LAIBACH · ADMINISTRATION", "LAIBACH · VERWALTUNG"));
        name.setLetterSpacing(0.2f);
        c.addView(name);
        c.addView(rule(v.accent), ruleParams(dp(3), dp(6), 0));

        LinearLayout middle = column();
        middle.setGravity(Gravity.CENTER);
        middle.addView(new EmblemView(home, v), new LinearLayout.LayoutParams(dp(96), dp(96)));
        TextView head = glitch(34, v.ink, 25, 1000);
        head.setText(t("MORNING\nROLL CALL", "MORGEN-\nAPPELL"));
        head.setGravity(Gravity.CENTER);
        head.setLetterSpacing(0.12f);
        head.setPadding(0, dp(28), 0, dp(12));
        middle.addView(head);
        TextView who = type(14, v.accent, 40, 900);
        who.setText(t("CITIZEN № ", "BÜRGER NR. ").trim() + "\n" + Citizen.number(home));
        who.setLetterSpacing(0.16f);
        who.setGravity(Gravity.CENTER);
        middle.addView(who);
        TextView note = type(12, v.muted, 80, 650);
        note.setText(t("THE COLLECTIVE IS COUNTING.\nPLEASE BE COUNTED.", "DAS KOLLEKTIV ZÄHLT.\nBITTE LASSEN SIE SICH ZÄHLEN."));
        note.setGravity(Gravity.CENTER);
        note.setLetterSpacing(0.1f);
        note.setLineSpacing(0, 1.2f);
        note.setPadding(0, dp(14), 0, 0);
        middle.addView(note);
        c.addView(middle, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        streak = type(11, v.muted, 80, 700);
        streak.setGravity(Gravity.CENTER);
        streak.setLetterSpacing(0.12f);
        streak.setPadding(0, 0, 0, dp(12));
        c.addView(streak);
        TextView present = type(22, v.bg, 40, 1000);
        present.setText(t("PRESENT", "ANWESEND"));
        present.setLetterSpacing(0.2f);
        present.setGravity(Gravity.CENTER);
        present.setPadding(dp(12), dp(18), dp(12), dp(18));
        present.setBackgroundColor(v.accent);
        present.setOnClickListener(view -> answer(true));
        c.addView(present);
        TextView absent = type(10, v.muted, 80, 650);
        absent.setText(t("ABSENCE IS ALSO AN ANSWER. IT IS WRITTEN DOWN.",
                "AUCH ABWESENHEIT IST EINE ANTWORT. SIE WIRD NOTIERT."));
        absent.setLetterSpacing(0.12f);
        absent.setGravity(Gravity.CENTER);
        absent.setPadding(0, dp(10), 0, 0);
        c.addView(absent);
        view = c;
        return c;
    }

    /** Calls the roll now, whatever the hour. */
    void summon() {
        summoned = true;
        update();
    }

    void update() {
        boolean on = (summoned || Citizen.rollCallDue(home)) && !home.curfew.showing();
        view.setVisibility(on ? View.VISIBLE : View.GONE);
        if (!on) return;
        int days = Citizen.streak(home);
        streak.setText(days == 0
                ? t("ATTENDANCE: NO RECORD OF YOU YET. THIS CAN BE CORRECTED.",
                        "ANWESENHEIT: NOCH KEIN NACHWEIS. DAS LÄSST SICH ÄNDERN.")
                : String.format(locale, t("ATTENDANCE: %d DAY(S) WITHOUT ABSENCE", "ANWESENHEIT: %d TAG(E) OHNE FEHLEN"), days));
    }

    void answer(boolean present) {
        Citizen.answerRollCall(home, present);
        summoned = false;
        String time = new SimpleDateFormat("HH:mm", locale).format(new Date());
        if (present) {
            int days = Citizen.streak(home);
            Record.note(home, "ROLL CALL " + time + ": PRESENT. DAY " + days + " OF UNBROKEN ATTENDANCE.",
                    "APPELL " + time + ": ANWESEND. TAG " + days + " LÜCKENLOSER ANWESENHEIT.");
            toast(String.format(locale, t("Counted. Day %d. The Collective is complete.",
                    "Gezählt. Tag %d. Das Kollektiv ist vollzählig."), days));
        } else {
            Record.note(home, "ROLL CALL " + time + ": ABSENT IN SPIRIT. ATTENDANCE RESET.",
                    "APPELL " + time + ": GEISTIG ABWESEND. ANWESENHEIT ZURÜCKGESETZT.");
            toast(t("Absence noted. Your attendance begins again tomorrow.",
                    "Abwesenheit vermerkt. Ihre Anwesenheit beginnt morgen von vorn."));
        }
        update();
    }
}
