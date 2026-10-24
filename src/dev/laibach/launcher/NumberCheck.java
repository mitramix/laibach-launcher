package dev.laibach.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * The number check at the counter: any citizen number typed in is marked
 * VALID or FORGED by its Damm check digit, which catches every wrong digit
 * and every swapped pair of neighbours.
 */
final class NumberCheck {
    static final int DIGITS = 21;

    private NumberCheck() {}

    /** The digits of a number as typed, without the dashes and spaces. */
    static String digits(CharSequence typed) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < typed.length(); i++) {
            char ch = typed.charAt(i);
            if (ch >= '0' && ch <= '9') out.append(ch);
        }
        return out.toString();
    }

    /** Whether 21 digits carry a correct check digit: Damm over all of them comes to 0. */
    static boolean valid(String digits) {
        return digits.length() == DIGITS && Citizen.damm(digits) == 0;
    }

    /** Opens the counter's check. */
    static void show(Activity a, Variant v) {
        boolean german = Lang.german(a);
        float density = a.getResources().getDisplayMetrics().density;
        int pad = Math.round(24 * density);
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(pad, Math.round(8 * density), pad, 0);
        TextView note = new TextView(a);
        note.setText(t(german, "Type a citizen number. The check digit will tell.",
                "Geben Sie eine Bürgernummer ein. Die Prüfziffer wird es sagen."));
        note.setTextColor(v.muted);
        box.addView(note);
        EditText field = new EditText(a);
        field.setInputType(InputType.TYPE_CLASS_PHONE);
        field.setHint("0000-0000-0000-0000-0000-0");
        field.setTextColor(v.ink);
        field.setHintTextColor(v.muted);
        field.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        field.setLetterSpacing(0.06f);
        field.setSingleLine(true);
        box.addView(field);
        TextView verdict = new TextView(a);
        verdict.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
        verdict.setTypeface(android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD));
        verdict.setLetterSpacing(0.16f);
        verdict.setGravity(Gravity.CENTER);
        verdict.setPadding(0, Math.round(14 * density), 0, Math.round(4 * density));
        box.addView(verdict);
        TextView reason = new TextView(a);
        reason.setGravity(Gravity.CENTER);
        reason.setTextColor(v.muted);
        box.addView(reason);

        String own = digits(Citizen.number(a));
        // The last verdict given, so a forgery is noted once, not once per keystroke.
        String[] judged = {""};
        TextWatcher check = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String d = digits(s);
                if (d.length() != DIGITS) {
                    verdict.setTextColor(v.muted);
                    verdict.setText("— — —");
                    reason.setText(String.format(Lang.locale(german), t(german, "%d OF 21 DIGITS", "%d VON 21 ZIFFERN"),
                            d.length()));
                    judged[0] = "";
                    return;
                }
                boolean ok = valid(d);
                verdict.setTextColor(ok ? v.ink : v.accent);
                verdict.setText(ok ? t(german, "VALID", "GÜLTIG") : t(german, "FORGED", "GEFÄLSCHT"));
                reason.setText(d.equals(own) ? t(german, "THIS IS YOU. YOU MAY PASS.", "DAS SIND SIE. SIE DÜRFEN PASSIEREN.")
                        : ok ? t(german, "THE CHECK DIGIT AGREES. A CITIZEN OF THE STATE.",
                                "DIE PRÜFZIFFER STIMMT. EIN BÜRGER DES STAATES.")
                        : t(german, "THE CHECK DIGIT DOES NOT AGREE. THE PAPERS ARE CONFISCATED.",
                                "DIE PRÜFZIFFER STIMMT NICHT. DIE PAPIERE WERDEN EINGEZOGEN."));
                if (d.equals(judged[0])) return;
                judged[0] = d;
                Tones.play(a, Tones.stamp());
                if (ok && d.equals(own)) Stamps.award(a, "self");
                if (!ok) Stamps.award(a, "forgery");
                if (!ok) Record.note(a, "A FORGED NUMBER WAS PRESENTED AT THE COUNTER. IT HAS BEEN CONFISCATED.",
                        "AM SCHALTER WURDE EINE GEFÄLSCHTE NUMMER VORGELEGT. SIE WURDE EINGEZOGEN.");
            }
        };
        field.addTextChangedListener(check);
        check.afterTextChanged(field.getText());
        StateMenu.dress(new AlertDialog.Builder(a, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(t(german, "Number check", "Nummernprüfung"))
                .setView(box)
                .setPositiveButton(t(german, "Done", "Fertig"), null)
                .show(), v);
    }

    private static String t(boolean german, String en, String de) {
        return german ? de : Lang.en(en);
    }
}
