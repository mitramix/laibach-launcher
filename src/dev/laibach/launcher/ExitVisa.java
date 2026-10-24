package dev.laibach.launcher;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.util.Locale;

/**
 * The exit visa: a programme leaves the terminal, redacted or banished, only
 * on a stamped form. Each visa is numbered, and each defection goes on the
 * record. The count is kept in the preferences file {@code visas}.
 */
final class ExitVisa {
    private static final String PREFS = "visas";
    private static final String[][] REASONS = {
            {"INSUFFICIENT LOYALTY", "UNZUREICHENDE TREUE"},
            {"AN EXCESS OF JOY", "EIN ÜBERMASS AN FREUDE"},
            {"A BETTER PROGRAMME SERVES THE SAME PURPOSE", "EIN BESSERES PROGRAMM DIENT DEMSELBEN ZWECK"},
            {"NO REASON. (THIS IS NOTED.)", "KEIN GRUND. (DAS WIRD VERMERKT.)"},
    };

    private ExitVisa() {}

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Visas issued so far: the citizen's defections. */
    static int issued(Context c) {
        return prefs(c).getInt("issued", 0);
    }

    /**
     * Lays the form before the citizen. Stamped, the visa is numbered and
     * noted, and then {@code granted} lets the programme go.
     */
    static void apply(StateActivity a, String label, Ministry ministry, boolean banish, Runnable granted) {
        Variant v = a.v;
        LinearLayout form = a.column();
        form.setPadding(a.dp(24), a.dp(4), a.dp(24), 0);
        TextView kind = a.label(a.t("FORM 7-B · ONE PROGRAMME, ONE WAY", "FORMULAR 7-B · EIN PROGRAMM, EINE RICHTUNG"));
        form.addView(kind);
        form.addView(field(a, a.t("APPLICANT", "ANTRAGSTELLER"), label.toUpperCase(a.locale)));
        form.addView(field(a, a.t("MINISTRY", "MINISTERIUM"), ministry.title(a.german)));
        form.addView(field(a, a.t("DESTINATION", "ZIEL"), banish
                ? a.t("OBLIVION (UNINSTALL)", "VERGESSENHEIT (DEINSTALLATION)")
                : a.t("THE ARCHIVE (REDACTION)", "DAS ARCHIV (SCHWÄRZUNG)")));
        TextView why = a.label(a.t("REASON FOR LEAVING", "AUSREISEGRUND"));
        why.setPadding(0, a.dp(12), 0, 0);
        form.addView(why);
        RadioGroup reasons = new RadioGroup(a);
        for (int i = 0; i < REASONS.length; i++) {
            RadioButton r = new RadioButton(a);
            r.setId(View.generateViewId());
            r.setTag(i);
            r.setText(Lang.pick(a.german, REASONS[i]));
            r.setTextColor(v.ink);
            r.setButtonTintList(android.content.res.ColorStateList.valueOf(v.accent));
            reasons.addView(r);
            if (i == REASONS.length - 1) r.setChecked(true);
        }
        form.addView(reasons);

        // The stamp waits above the form until the visa is granted.
        FrameLayout sheet = new FrameLayout(a);
        sheet.addView(form);
        TextView stamp = a.type(30, v.accent, 40, 1000);
        stamp.setText(a.t("APPROVED", "GENEHMIGT"));
        stamp.setLetterSpacing(0.16f);
        stamp.setPadding(a.dp(14), a.dp(4), a.dp(14), a.dp(4));
        GradientDrawable frame = new GradientDrawable();
        frame.setStroke(a.dp(3), v.accent);
        frame.setCornerRadius(a.dp(4));
        stamp.setBackground(frame);
        stamp.setRotation(-14);
        stamp.setVisibility(View.INVISIBLE);
        sheet.addView(stamp, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.END | Gravity.TOP));

        AlertDialog d = StateMenu.dress(new AlertDialog.Builder(a, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(a.t("Application for an exit visa", "Antrag auf ein Ausreisevisum"))
                .setView(sheet)
                .setPositiveButton(a.t("Stamp", "Stempeln"), null)
                .setNegativeButton(a.t("Withdraw the application", "Antrag zurückziehen"), null)
                .show(), v);
        // The stamp comes down before the dialog goes, so the positive button is wired by hand.
        d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            view.setEnabled(false);
            int reason = (int) reasons.findViewById(reasons.getCheckedRadioButtonId()).getTag();
            stamp.setVisibility(View.VISIBLE);
            stamp.setScaleX(2.4f);
            stamp.setScaleY(2.4f);
            stamp.setAlpha(0);
            stamp.animate().scaleX(1).scaleY(1).alpha(1).setDuration(260)
                    .setInterpolator(new OvershootInterpolator(1.4f)).start();
            Tones.play(a, Tones.stamp());
            sheet.postDelayed(() -> {
                d.dismiss();
                issue(a, label, banish, reason);
                granted.run();
            }, 900);
        });
    }

    private static View field(StateActivity a, String name, String value) {
        LinearLayout f = a.column();
        f.setPadding(0, a.dp(10), 0, 0);
        TextView l = a.type(9, a.v.muted, 80, 800);
        l.setText(name);
        l.setLetterSpacing(0.2f);
        f.addView(l);
        TextView val = a.type(15, a.v.ink, 50, 850);
        val.setText(value);
        val.setLetterSpacing(0.04f);
        f.addView(val);
        return f;
    }

    private static void issue(StateActivity a, String label, boolean banish, int reason) {
        int number = issued(a) + 1;
        prefs(a).edit().putInt("issued", number).apply();
        String no = String.format(Locale.ROOT, "%04d", number);
        String en = label.toUpperCase(Locale.ROOT), de = label.toUpperCase(Locale.GERMAN);
        Record.note(a, "EXIT VISA № " + no + " ISSUED TO " + en + (banish ? " (BANISHMENT)" : " (REDACTION)")
                        + ". REASON: " + REASONS[reason][0] + ". DEFECTION RECORDED.",
                "AUSREISEVISUM NR. " + no + " FÜR " + de + (banish ? " (VERBANNUNG)" : " (SCHWÄRZUNG)")
                        + ". GRUND: " + REASONS[reason][1] + ". REPUBLIKFLUCHT VERMERKT.");
        Stamps.award(a, "visa");
        a.toast(String.format(a.locale, a.t("Exit visa № %s issued. The defection has been recorded.",
                "Ausreisevisum Nr. %s erteilt. Die Republikflucht wurde vermerkt."), no));
    }
}
