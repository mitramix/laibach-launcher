package dev.laibach.launcher;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputFilter;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * The dead drop: a note the owner leaves for themself, kept on the home
 * screen as a sealed decree with every letter blacked out. Held down, it is
 * unsealed for a few seconds, then sealed again. The note stays on the phone
 * and its words never go on the record.
 */
final class DeadDrop extends HomePart {
    private static final String KEY = "deadDrop";
    private static final int LONGEST = 140;
    private static final long OPEN = 6_000;
    private LinearLayout view;
    private TextView seal, body;
    private final Runnable reseal = this::update;

    DeadDrop(HomeActivity home) {
        super(home);
    }

    static String note(Context c) {
        return c.getSharedPreferences(Variant.PREFS, Context.MODE_PRIVATE).getString(KEY, "");
    }

    private static void setNote(Context c, String note) {
        c.getSharedPreferences(Variant.PREFS, Context.MODE_PRIVATE).edit().putString(KEY, note).apply();
    }

    View build() {
        view = column();
        view.setPadding(0, 0, 0, dp(12));
        seal = type(10, v.accent, 80, 850);
        seal.setLetterSpacing(0.16f);
        seal.setGravity(home.formation.gravity);
        view.addView(seal);
        body = type(14, v.ink, 60, 700);
        body.setLetterSpacing(0.04f);
        body.setGravity(home.formation.gravity);
        body.setMaxLines(4);
        body.setPadding(0, dp(3), 0, 0);
        view.addView(body);
        view.setOnClickListener(x -> Toast.makeText(home, t("Sealed. Hold to unseal.", "Versiegelt. Halten zum Entsiegeln."),
                Toast.LENGTH_SHORT).show());
        view.setOnLongClickListener(x -> {
            unseal();
            return true;
        });
        update();
        return view;
    }

    /** The note as it lies on the screen: every letter under a bar, the spaces between left as they are. */
    static String barred(String note) {
        StringBuilder b = new StringBuilder(note.length());
        for (int i = 0; i < note.length(); i++) {
            char ch = note.charAt(i);
            b.append(Character.isWhitespace(ch) ? ch : '█');
        }
        return b.toString();
    }

    void update() {
        view.removeCallbacks(reseal);
        String note = note(home);
        view.setVisibility(note.isEmpty() ? View.GONE : View.VISIBLE);
        seal.setText(t("■ SEALED DECREE · HOLD TO UNSEAL", "■ VERSIEGELTER ERLASS · HALTEN ZUM ENTSIEGELN"));
        body.setText(barred(note));
    }

    void unseal() {
        String note = note(home);
        if (note.isEmpty()) return;
        Record.noteDaily(home, "deadDrop", "THE DEAD DROP WAS OPENED. THE OPENING WAS NOTED; THE CONTENTS WERE NOT.",
                "DER TOTE BRIEFKASTEN WURDE GEÖFFNET. DAS ÖFFNEN WURDE VERMERKT, DER INHALT NICHT.");
        Tones.play(home, Tones.relay());
        seal.setText(t("■ UNSEALED · IT WILL BE SEALED AGAIN", "■ ENTSIEGELT · WIRD WIEDER VERSIEGELT"));
        body.setText(note);
        view.removeCallbacks(reseal);
        view.postDelayed(reseal, OPEN);
    }

    /** The form for leaving a note, or taking it away. */
    void edit() {
        String note = note(home);
        EditText field = new EditText(home);
        field.setText(note);
        field.setSelection(field.getText().length());
        field.setTextColor(v.ink);
        field.setHintTextColor(v.muted);
        field.setHint(t("A note for yourself alone", "Eine Notiz nur für Sie"));
        field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        field.setFilters(new InputFilter[]{new InputFilter.LengthFilter(LONGEST)});
        LinearLayout box = column();
        box.setPadding(dp(22), dp(4), dp(22), 0);
        TextView why = type(12, v.muted, 90, 450);
        why.setText(t("Kept on the home screen with every letter blacked out. Hold it to read it.",
                "Auf dem Startbildschirm mit jedem Buchstaben geschwärzt. Halten, um sie zu lesen."));
        box.addView(why);
        box.addView(field);
        AlertDialog.Builder b = new AlertDialog.Builder(home, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(t("Dead drop", "Toter Briefkasten"))
                .setView(box)
                .setPositiveButton(t("Seal", "Versiegeln"), (d, w) -> leave(field.getText().toString().trim()))
                .setNegativeButton(t("Cancel", "Abbrechen"), null);
        if (!note.isEmpty()) b.setNeutralButton(t("Empty it", "Leeren"), (d, w) -> leave(""));
        StateMenu.dress(b.show(), v);
    }

    private void leave(String note) {
        String before = note(home);
        if (note.equals(before)) return;
        setNote(home, note);
        if (note.isEmpty()) {
            Record.note(home, "THE DEAD DROP WAS EMPTIED.", "DER TOTE BRIEFKASTEN WURDE GELEERT.");
            toast(t("The dead drop is empty.", "Der tote Briefkasten ist leer."));
        } else {
            Record.note(home, "A NOTE WAS LEFT IN THE DEAD DROP. ITS CONTENTS ARE SEALED.",
                    "EINE NOTIZ WURDE IM TOTEN BRIEFKASTEN HINTERLEGT. IHR INHALT IST VERSIEGELT.");
            Tones.play(home, Tones.stamp());
            toast(t("Sealed. It waits on the home screen.", "Versiegelt. Sie wartet auf dem Startbildschirm."));
        }
        update();
        home.regulations.refresh();
    }
}
