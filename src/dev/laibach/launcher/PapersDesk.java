package dev.laibach.launcher;

import android.app.AlertDialog;
import android.content.Intent;
import android.widget.TextView;
import android.widget.Toast;

/**
 * The desk of the papers: the article of the day and the whole constitution,
 * self-criticism, and the counter where numbers are checked and comrades registered.
 */
final class PapersDesk extends PapersPart {
    /** Faults a citizen may confess to, English and German in step. */
    private static final String[][] FAULTS = {
            {"I scrolled without purpose.", "Ich habe ohne Zweck gescrollt."},
            {"I enjoyed a programme more than was planned.", "Ich habe ein Programm mehr genossen als vorgesehen."},
            {"I ignored a decree.", "Ich habe einen Erlass übergangen."},
            {"I thought a thought that was not on the list.", "Ich habe einen Gedanken gedacht, der nicht auf der Liste stand."},
            {"I charged my phone before it was necessary.", "Ich habe mein Telefon geladen, bevor es nötig war."},
            {"I looked at the clock during the curfew.", "Ich habe während der Sperrstunde auf die Uhr geschaut."},
            {"I doubted the art of the day.", "Ich habe an der Kunst des Tages gezweifelt."},
            {"I have nothing to confess. (This is noted.)", "Ich habe nichts zu gestehen. (Das wird vermerkt.)"},
    };

    PapersDesk(PapersActivity papers) {
        super(papers);
    }

    void fill() {
        // The article of the day; tap for the whole constitution.
        heading(t("ARTICLE OF THE DAY", "ARTIKEL DES TAGES"));
        int a = Constitution.ofTheDay();
        TextView article = serif(15, v.ink);
        article.setText(t("ARTICLE ", "ARTIKEL ") + Constitution.numeral(a + 1) + ". "
                + Lang.pick(german, Constitution.ARTICLES[a]).toUpperCase(locale));
        article.setOnClickListener(view -> showConstitution());
        content.addView(article);
        TextView whole = type(11, v.accent, 80, 800);
        whole.setText(t("■ READ THE WHOLE CONSTITUTION", "■ DIE GANZE VERFASSUNG LESEN"));
        whole.setLetterSpacing(0.16f);
        whole.setPadding(0, dp(10), 0, dp(4));
        whole.setOnClickListener(view -> showConstitution());
        content.addView(whole);

        // Self-criticism.
        heading(t("SELF-CRITICISM", "SELBSTKRITIK"));
        TextView why = type(11, v.muted, 80, 650);
        why.setText(Citizen.forgiven(papers)
                ? t("TODAY'S FORGIVENESS HAS BEEN ISSUED. FURTHER CONFESSIONS ARE FILED, NOT FORGIVEN.",
                        "DIE HEUTIGE VERGEBUNG WURDE ERTEILT. WEITERE GESTÄNDNISSE WERDEN ABGELEGT, NICHT VERGEBEN.")
                : t("ONE CONFESSION A DAY RAISES YOUR STANDING BY ONE GRADE.",
                        "EIN GESTÄNDNIS PRO TAG HEBT IHR ANSEHEN UM EINE STUFE."));
        why.setLetterSpacing(0.1f);
        why.setPadding(0, 0, 0, dp(10));
        content.addView(why);
        content.addView(button(t("SUBMIT SELF-CRITICISM", "SELBSTKRITIK EINREICHEN"), view -> confess()),
                fullWidth());

        // The counter checks anyone's papers.
        heading(t("THE COUNTER", "DER SCHALTER"));
        content.addView(button(t("CHECK A CITIZEN NUMBER", "BÜRGERNUMMER PRÜFEN"), view -> NumberCheck.show(papers, v)),
                fullWidth());
        content.addView(button(t("REGISTER A COMRADE", "EINEN GENOSSEN REGISTRIEREN"),
                view -> papers.startActivity(new Intent(papers, ExchangeActivity.class))), spaced());

    }

    private void confess() {
        String[] items = new String[FAULTS.length];
        for (int i = 0; i < FAULTS.length; i++) items[i] = Lang.pick(german, FAULTS[i]);
        boolean[] chosen = new boolean[FAULTS.length];
        StateMenu.dress(new AlertDialog.Builder(papers, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(t("I confess that", "Ich gestehe, dass"))
                .setMultiChoiceItems(items, chosen, (d, which, checked) -> chosen[which] = checked)
                .setPositiveButton(t("Confess", "Gestehen"), (d, which) -> {
                    int faults = 0;
                    for (boolean c : chosen) if (c) faults++;
                    boolean forgiven = Citizen.confess(papers);
                    Record.note(papers, "SELF-CRITICISM SUBMITTED: " + (faults == 0
                                    ? "NO FAULTS. SILENCE IS ALSO A CONFESSION." : faults + " FAULT(S).")
                                    + (forgiven ? " FORGIVEN." : " FILED."),
                            "SELBSTKRITIK EINGEREICHT: " + (faults == 0
                                    ? "KEINE VERFEHLUNGEN. AUCH SCHWEIGEN IST EIN GESTÄNDNIS." : faults + " VERFEHLUNG(EN).")
                                    + (forgiven ? " VERGEBEN." : " ABGELEGT."));
                    Toast.makeText(papers, forgiven
                            ? t("YOU ARE FORGIVEN. YOU ARE NOT FORGOTTEN.", "IHNEN WIRD VERGEBEN. SIE WERDEN NICHT VERGESSEN.")
                            : t("Your confession has been filed. Forgiveness is rationed: one a day.",
                                    "Ihr Geständnis wurde abgelegt. Vergebung ist rationiert: eine pro Tag."),
                            Toast.LENGTH_LONG).show();
                    Stamps.award(papers, "confession");
                    papers.fill();
                    Promotion.check(papers);
                })
                .setNegativeButton(t("I have nothing to say", "Ich habe nichts zu sagen"), (d, which) ->
                        Toast.makeText(papers, t("Your silence has been recorded.", "Ihr Schweigen wurde aufgezeichnet."),
                                Toast.LENGTH_SHORT).show())
                .show(), v);
    }

    void showConstitution() {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < Constitution.ARTICLES.length; i++) {
            if (i > 0) s.append("\n\n");
            s.append(t("Article ", "Artikel ")).append(Constitution.numeral(i + 1)).append(". ")
                    .append(Lang.pick(german, Constitution.ARTICLES[i]));
        }
        StateMenu.dress(new AlertDialog.Builder(papers, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(t("The Constitution of the Laibach Collective", "Die Verfassung des Laibach-Kollektivs"))
                .setMessage(s)
                .setPositiveButton(t("I agree", "Ich stimme zu"), null)
                .setNegativeButton(t("I do not agree", "Ich stimme nicht zu"), (d, which) -> {
                    Record.note(papers, "CITIZEN DID NOT AGREE WITH THE CONSTITUTION. DISAGREEMENT IS PERMITTED.",
                            "BÜRGER STIMMTE DER VERFASSUNG NICHT ZU. WIDERSPRUCH IST ERLAUBT.");
                    Toast.makeText(papers, t("Your disagreement has been noted. You agree.",
                            "Ihr Widerspruch wurde vermerkt. Sie stimmen zu."), Toast.LENGTH_LONG).show();
                    papers.fill();
                })
                .show(), v);
    }
}
