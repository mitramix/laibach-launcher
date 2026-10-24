package dev.laibach.launcher;

import android.content.Intent;
import android.graphics.Insets;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.provider.AlarmClock;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * The rest notice that covers the home screen from 23:00 to 06:00, and its
 * drill. Five minutes before, the ticker counts down and the uniform darkens
 * minute by minute.
 */
final class Curfew extends HomePart {
    /** The minutes of warning before the curfew. */
    static final int WARNING = 5;
    View view;
    /** The dusk laid over the uniform while the curfew approaches. */
    View dusk;
    TextView note;
    TextView time;
    /** A drill is under way: the curfew shows whatever the hour. */
    boolean drill;

    Curfew(HomeActivity home) {
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

        LinearLayout top = row();
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = type(16, v.ink, 25, 1000);
        name.setText("LAIBACH");
        name.setLetterSpacing(0.2f);
        top.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        time = type(16, v.muted, 25, 900);
        top.addView(time);
        c.addView(top);
        c.addView(rule(v.accent), ruleParams(dp(3), dp(6), 0));

        LinearLayout middle = column();
        middle.setGravity(Gravity.CENTER);
        EmblemView emblem = new EmblemView(home, v);
        middle.addView(emblem, new LinearLayout.LayoutParams(dp(120), dp(120)));
        TextView head = glitch(34, v.ink, 25, 1000);
        head.setText(t("REST IS\nCOMPULSORY", "RUHE IST\nPFLICHT"));
        head.setGravity(Gravity.CENTER);
        head.setLetterSpacing(0.12f);
        head.setPadding(0, dp(28), 0, dp(12));
        middle.addView(head);
        note = type(12, v.muted, 80, 650);
        note.setGravity(Gravity.CENTER);
        note.setLetterSpacing(0.1f);
        note.setLineSpacing(0, 1.2f);
        middle.addView(note);
        middle.addView(rule(v.rule), ruleParams(Math.max(1, dp(1)), dp(28), dp(14)));
        TextView permitted = label(t("PERMITTED UNTIL 06:00", "ERLAUBT BIS 06:00 UHR"));
        permitted.setGravity(Gravity.CENTER);
        middle.addView(permitted, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout allowed = row();
        allowed.setGravity(Gravity.CENTER);
        allowed.addView(action(t("PHONE", "TELEFON"), new Intent(Intent.ACTION_DIAL)));
        allowed.addView(action(t("ALARM", "WECKER"), new Intent(AlarmClock.ACTION_SHOW_ALARMS)));
        allowed.addView(action(t("EMERGENCY", "NOTRUF"), new Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))));
        middle.addView(allowed);
        c.addView(middle, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        TextView hold = type(14, v.accent, 40, 900);
        hold.setText(t("HOLD TO REQUEST AN EXEMPTION", "HALTEN, UM EINE AUSNAHME ZU BEANTRAGEN"));
        hold.setLetterSpacing(0.12f);
        hold.setGravity(Gravity.CENTER);
        hold.setPadding(dp(12), dp(16), dp(12), dp(16));
        GradientDrawable frame = new GradientDrawable();
        frame.setStroke(Math.max(1, dp(1.5f)), v.accent);
        hold.setBackground(frame);
        hold.setOnClickListener(view -> Toast.makeText(home,
                t("Exemptions must be held for", "Ausnahmen wollen gehalten werden"), Toast.LENGTH_SHORT).show());
        hold.setOnLongClickListener(view -> {
            exempt();
            return true;
        });
        c.addView(hold);
        TextView remembered = type(10, v.muted, 80, 650);
        remembered.setText(t("EXEMPTIONS ARE GRANTED. THEY ARE ALSO REMEMBERED.",
                "AUSNAHMEN WERDEN GEWÄHRT. SIE WERDEN AUCH NICHT VERGESSEN."));
        remembered.setLetterSpacing(0.12f);
        remembered.setGravity(Gravity.CENTER);
        remembered.setPadding(0, dp(10), 0, 0);
        c.addView(remembered);
        view = c;
        return c;
    }

    /** The dusk: a shade over the screen, under the curfew itself, that deepens as the curfew nears. */
    View buildDusk() {
        dusk = new View(home);
        dusk.setBackgroundColor(0xFF000000);
        dusk.setVisibility(View.GONE);
        return dusk;
    }

    /** Minutes until the curfew, from {@link #WARNING} down to 1, or 0 when none is coming. */
    int warning() {
        if (!Citizen.curfewOn(home) || drill) return 0;
        Calendar now = Calendar.getInstance();
        return warning(now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE));
    }

    /** Minutes from the given minute of the day until 23:00, when that is within the warning; otherwise 0. */
    static int warning(int minuteOfDay) {
        int left = 23 * 60 - minuteOfDay;
        return left >= 1 && left <= WARNING ? left : 0;
    }

    TextView action(String text, Intent intent) {
        TextView a = type(16, v.ink, 40, 850);
        a.setText(text);
        a.setLetterSpacing(0.14f);
        a.setPadding(dp(12), dp(12), dp(12), dp(12));
        a.setOnClickListener(view -> open(intent));
        return a;
    }

    void update() {
        boolean on = drill || Citizen.curfew(home);
        if (on && !showing()) home.reset();
        view.setVisibility(on ? View.VISIBLE : View.GONE);
        int left = on ? 0 : warning();
        dusk.setVisibility(left > 0 ? View.VISIBLE : View.GONE);
        // From a tenth of the way dark, five minutes out, to more than half at the last minute.
        dusk.setAlpha(0.1f + 0.12f * (WARNING - left));
        if (!on) return;
        Calendar now = Calendar.getInstance();
        time.setText(new SimpleDateFormat("HH:mm", locale).format(now.getTime()));
        int since = (now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE) - 23 * 60 + 1440) % 1440;
        String began = drill
                ? t("THIS IS A CURFEW DRILL.", "DIES IST EINE SPERRSTUNDENÜBUNG.")
                : String.format(locale, t("CURFEW BEGAN %s AGO.", "DIE SPERRSTUNDE BEGANN VOR %s."), span(since * 60_000L));
        note.setText(began + "\n" + t("YOUR STANDING RISES WHILE YOU SLEEP.",
                "IHR ANSEHEN STEIGT, WÄHREND SIE SCHLAFEN."));
    }

    void startDrill() {
        drill = true;
        update();
    }

    void exempt() {
        String message;
        if (drill) {
            drill = false;
            Record.note(home, "CURFEW DRILL COMPLETED. COMPLIANCE EXEMPLARY.",
                    "SPERRSTUNDENÜBUNG ABGESCHLOSSEN. FOLGSAMKEIT VORBILDLICH.");
            message = t("Drill complete. Your compliance has been noted.",
                    "Übung beendet. Ihre Folgsamkeit wurde vermerkt.");
            Stamps.award(home, "drill");
        } else {
            long until = Citizen.exempt(home, 30 * 60_000L);
            String end = new SimpleDateFormat("HH:mm", Locale.ROOT).format(new Date(until));
            Record.note(home, "CURFEW EXEMPTION REQUESTED. GRANTED UNTIL " + end + ". IT IS A CONFESSION.",
                    "AUSNAHME VON DER SPERRSTUNDE BEANTRAGT. GEWÄHRT BIS " + end + ". SIE IST EIN GESTÄNDNIS.");
            message = String.format(locale, t("Exemption granted until %s. It has been noted.",
                    "Ausnahme gewährt bis %s. Sie wurde vermerkt."),
                    new SimpleDateFormat("HH:mm", locale).format(new Date(until)));
        }
        toast(message);
        update();
        home.ledger.update();
    }
}
