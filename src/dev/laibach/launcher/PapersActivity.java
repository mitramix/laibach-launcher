package dev.laibach.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Insets;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The citizen's papers: identity card, the day's statistics, the article of the
 * day, self-criticism, and the permanent record. Opened from the papers line on
 * the home screen; with EXTRA_CONSTITUTION it opens on the full constitution.
 */
public class PapersActivity extends StateActivity {
    static final String EXTRA_CONSTITUTION = "constitution";

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

    private LinearLayout content;
    private View card;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        // The papers are read through the same glass as the home screen.
        FrameLayout glass = new FrameLayout(this);
        glass.addView(buildUi());
        glass.addView(new Crt(this, v));
        wear(glass);
        if (state == null && getIntent().getBooleanExtra(EXTRA_CONSTITUTION, false)) showConstitution();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fill();
    }

    // ---- Layout -------------------------------------------------------------

    private View buildUi() {
        LinearLayout root = column();
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            view.setPadding(bars.left + dp(20), bars.top + dp(6), bars.right + dp(20), 0);
            content.setPadding(0, 0, 0, bars.bottom + dp(24));
            return WindowInsets.CONSUMED;
        });

        LinearLayout head = row();
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = type(26, v.accent, 25, 1000);
        back.setText("◀");
        back.setPadding(0, 0, dp(12), 0);
        back.setOnClickListener(view -> finish());
        head.addView(back);
        TextView title = type(30, v.ink, 25, 1000);
        title.setText(t("PAPERS", "PAPIERE"));
        title.setLetterSpacing(0.14f);
        head.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView seal = type(14, v.accent, 40, 900);
        seal.setText(t("CONFIDENTIAL", "VERTRAULICH"));
        seal.setLetterSpacing(0.16f);
        head.addView(seal);
        root.addView(head);
        root.addView(rule(v.accent), ruleParams(dp(5), dp(6), 0));

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        content = column();
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        return root;
    }

    /** Builds the papers afresh from the current state. */
    private void fill() {
        content.removeAllViews();
        card = card();
        content.addView(card);

        // The day's figures, two to a row.
        heading(t("STATISTICS OF THE DAY", "STATISTIK DES TAGES"));
        int day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        int year = Calendar.getInstance().get(Calendar.YEAR);
        int streak = Citizen.streak(this);
        figures(t("ATTENDANCE", "ANWESENHEIT"),
                streak + (streak == 1 ? t(" DAY", " TAG") : t(" DAYS", " TAGE")),
                t("ATTENTION USED", "AUFMERKSAMKEIT"), span(Citizen.used(this)));
        figures(t("EXEMPTIONS", "AUSNAHMEN"), String.valueOf(Citizen.exemptions(this)),
                t("JOY COUPONS", "FREUDENMARKEN"), Citizen.joyLeft(this) + " / " + Citizen.coupons(this));
        figures(t("SELF-CRITICISMS", "SELBSTKRITIKEN"), String.valueOf(Citizen.confessions(this)),
                t("RECORD ENTRIES", "AKTENEINTRÄGE"), Record.on(this) ? String.valueOf(Record.entries(this).size()) : t("NOT KEPT", "NICHT GEFÜHRT"));
        figures(t("DEFECTIONS", "REPUBLIKFLUCHTEN"), String.valueOf(ExitVisa.issued(this)),
                t("STAMPS", "MARKEN"), Stamps.count(this) + " / " + Stamps.ALL.length);
        figures(t("ENTRY VISAS", "EINREISEVISA"), String.valueOf(Passport.count(this)),
                t("COMRADES", "GENOSSEN"), String.valueOf(ExchangeActivity.comrades(this).size()));
        // Harmony is always nearly perfect; the norm is always exceeded.
        int seed = Norm.seed(year * 1000L + day);
        figures(t("COLLECTIVE HARMONY", "KOLLEKTIVE HARMONIE"),
                String.format(locale, "99.%02d%%", seed % 100).replace('.', german ? ',' : '.'),
                t("NORM FULFILMENT", "NORMERFÜLLUNG"), Norm.today(this) + "%");

        quotas();
        drills();
        album();
        passport();

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
        why.setText(Citizen.forgiven(this)
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
        content.addView(button(t("CHECK A CITIZEN NUMBER", "BÜRGERNUMMER PRÜFEN"), view -> NumberCheck.show(this, v)),
                fullWidth());
        content.addView(button(t("REGISTER A COMRADE", "EINEN GENOSSEN REGISTRIEREN"),
                view -> startActivity(new Intent(this, ExchangeActivity.class))), spaced());

        // Papers that leave the launcher.
        heading(t("PAPERS FOR EXPORT", "PAPIERE ZUR AUSFUHR"));
        content.addView(button(t("WEEKLY REPORT", "WOCHENBERICHT"), view -> Report.show(this, v)), fullWidth());
        content.addView(button(t("SAVE THE CARD AS A PICTURE", "AUSWEIS ALS BILD SPEICHERN"), view -> saveCard()),
                spaced());
        content.addView(button(t("SHARE THE CARD (SPECIMEN)", "AUSWEIS TEILEN (MUSTER)"), view -> shareCard()),
                spaced());
        content.addView(button(t("PRINT THE CASE FILE (PDF)", "FALLAKTE DRUCKEN (PDF)"), view -> caseFile()), spaced());
        content.addView(button(t("BACK UP THE SETTINGS", "EINSTELLUNGEN SICHERN"), view -> backup()), spaced());
        content.addView(button(t("RESTORE THE SETTINGS", "EINSTELLUNGEN WIEDERHERSTELLEN"), view -> restore()),
                spaced());
        TextView where = type(10, v.muted, 80, 650);
        where.setText(t("SAVED IN PICTURES/LAIBACH AND DOWNLOAD/LAIBACH ON THIS PHONE. THE CARD IS A SPECIMEN, NOT A DOCUMENT.",
                "GESPEICHERT IN PICTURES/LAIBACH UND DOWNLOAD/LAIBACH AUF DIESEM TELEFON. DER AUSWEIS IST EIN MUSTER, KEIN DOKUMENT."));
        where.setLetterSpacing(0.1f);
        where.setPadding(0, dp(10), 0, 0);
        content.addView(where);

        // The permanent record, newest first, if the owner keeps one.
        List<String[]> entries = Record.entries(this);
        heading(t("PERMANENT RECORD", "STÄNDIGE AKTE"));
        if (!Record.on(this)) {
            TextView closed = serif(13, v.muted);
            closed.setText(t("THE RECORD IS NOT KEPT. NOTHING YOU DO IN THE LAUNCHER IS WRITTEN DOWN.",
                    "DIE AKTE WIRD NICHT GEFÜHRT. NICHTS, WAS SIE IM LAUNCHER TUN, WIRD AUFGESCHRIEBEN."));
            content.addView(closed);
            TextView keep = button(t("KEEP THE RECORD", "DIE AKTE FÜHREN"), view -> {
                Record.setOn(this, true);
                Record.note(this, "PERMANENT RECORD OPENED. FROM NOW ON, EVERYTHING IS NOTED.",
                        "STÄNDIGE AKTE ANGELEGT. AB JETZT WIRD ALLES VERMERKT.");
                fill();
            });
            LinearLayout.LayoutParams lp = fullWidth();
            lp.topMargin = dp(20);
            content.addView(keep, lp);
            return;
        }
        if (entries.isEmpty()) {
            TextView none = serif(13, v.muted);
            none.setText(t("NOTHING HAS BEEN RECORDED. THIS IS SUSPICIOUS.",
                    "ES WURDE NICHTS AUFGEZEICHNET. DAS IST VERDÄCHTIG."));
            content.addView(none);
        }
        SimpleDateFormat when = new SimpleDateFormat(t("d MMM HH:mm", "d. MMM HH:mm"), locale);
        for (String[] e : entries) {
            String time;
            try {
                time = when.format(new Date(Long.parseLong(e[0]))).toUpperCase(locale);
            } catch (NumberFormatException ex) {
                time = "—";
            }
            SpannableStringBuilder s = new SpannableStringBuilder(time);
            s.setSpan(new ForegroundColorSpan(v.accent), 0, s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            s.append("  ").append((german ? e[2] : Lang.en(e[1])));
            TextView line = type(12, v.ink, 70, 700);
            line.setText(s);
            line.setLetterSpacing(0.06f);
            line.setPadding(0, dp(7), 0, dp(7));
            content.addView(line);
            content.addView(rule(v.rule), ruleParams(Math.max(1, dp(1)), 0, 0));
        }
        TextView delete = button(t("HOLD TO DELETE THE RECORD", "HALTEN, UM DIE AKTE ZU LÖSCHEN"), view ->
                Toast.makeText(this, t("The record is permanent. Hold to test this.",
                        "Die Akte ist dauerhaft. Halten Sie, um das zu prüfen."), Toast.LENGTH_SHORT).show());
        delete.setTextColor(v.muted);
        ((GradientDrawable) delete.getBackground()).setStroke(Math.max(1, dp(1)), v.muted);
        delete.setOnLongClickListener(view -> {
            Record.clear(this);
            Toast.makeText(this, t("Record deleted. The deletion has been noted.",
                    "Akte gelöscht. Die Löschung wurde vermerkt."), Toast.LENGTH_LONG).show();
            fill();
            return true;
        });
        LinearLayout.LayoutParams lp = fullWidth();
        lp.topMargin = dp(20);
        content.addView(delete, lp);
    }

    /** The identity card: emblem, number, standing, dates and a barcode. */
    private View card() {
        LinearLayout card = column();
        GradientDrawable frame = new GradientDrawable();
        frame.setStroke(Math.max(1, dp(1.5f)), v.accent);
        frame.setCornerRadius(dp(4));
        card.setBackground(frame);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));

        TextView of = type(10, v.accent, 80, 800);
        of.setText(t("CITIZEN OF THE LAIBACH COLLECTIVE", "BÜRGER DES LAIBACH-KOLLEKTIVS"));
        of.setLetterSpacing(0.2f);
        card.addView(of);

        LinearLayout middle = row();
        middle.setGravity(Gravity.CENTER_VERTICAL);
        middle.setPadding(0, dp(12), 0, dp(12));
        middle.addView(new EmblemView(this, v), new LinearLayout.LayoutParams(dp(72), dp(72)));
        LinearLayout who = column();
        who.setPadding(dp(16), 0, 0, 0);
        // The number is 26 characters long: it takes the largest size that fits on one line.
        TextView number = type(26, v.ink, 25, 1000);
        number.setText("№ " + Citizen.number(this));
        number.setLetterSpacing(0.06f);
        number.setMaxLines(1);
        number.setAutoSizeTextTypeUniformWithConfiguration(10, 26, 1, TypedValue.COMPLEX_UNIT_SP);
        who.addView(number, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(30)));
        int grade = Citizen.grade(this);
        TextView standing = type(12, grade >= 3 ? v.accent : v.ink, 70, 850);
        standing.setText(t("STANDING ", "ANSEHEN ") + Lang.pick(german, Citizen.STANDING[grade]));
        standing.setLetterSpacing(0.1f);
        who.addView(standing);
        middle.addView(who, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        card.addView(middle);

        long issued;
        try {
            issued = getPackageManager().getPackageInfo(getPackageName(), 0).firstInstallTime;
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            issued = System.currentTimeMillis();
        }
        LinearLayout dates = row();
        dates.addView(field(t("ISSUED", "AUSGESTELLT"),
                new SimpleDateFormat(t("d MMM yyyy", "d. MMM yyyy"), locale).format(new Date(issued)).toUpperCase(locale)),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        dates.addView(field(t("VALID", "GÜLTIG"), t("PERMANENTLY", "DAUERHAFT")),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        dates.addView(field(t("ISSUED BY", "BEHÖRDE"), t("THE STATE", "DER STAAT")),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        card.addView(dates);

        LinearLayout.LayoutParams code = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
        code.topMargin = dp(14);
        card.addView(new Barcode(this, Citizen.number(this).replace("-", "")), code);
        TextView foot = type(9, v.muted, 80, 700);
        foot.setText(t("THIS CARD IS THE PROPERTY OF THE COLLECTIVE. SO IS THE BEARER.",
                "DIESER AUSWEIS IST EIGENTUM DES KOLLEKTIVS. DER INHABER AUCH."));
        foot.setLetterSpacing(0.12f);
        foot.setPadding(0, dp(8), 0, 0);
        card.addView(foot);
        return card;
    }

    /** Each ministry's allowance for the day, and which are closed for inventory. */
    private void quotas() {
        heading(t("MINISTRY QUOTAS", "KONTINGENTE DER MINISTERIEN"));
        if (!Quotas.on(this) || !Citizen.usageAccess(this)) {
            TextView off = type(11, v.muted, 80, 650);
            off.setText(!Quotas.on(this)
                    ? t("SUSPENDED BY THE OWNER. THEY ARE ORDERED IN THE REGULATIONS.",
                            "VOM BESITZER AUSGESETZT. SIE WERDEN IN DEN VORSCHRIFTEN ANGEORDNET.")
                    : t("UNMEASURED. GRANT USAGE ACCESS AND THE MINISTRIES WILL KNOW.",
                            "UNGEMESSEN. GEWÄHREN SIE NUTZUNGSZUGRIFF, DANN WISSEN ES DIE MINISTERIEN."));
            off.setLetterSpacing(0.1f);
            content.addView(off);
            return;
        }
        Map<Ministry, Long> spent = Quotas.spent(this);
        for (Ministry m : Ministry.values()) {
            long allowance = Quotas.allowance(m);
            if (allowance == 0) continue;
            boolean closed = Quotas.closed(this, m, spent);
            LinearLayout r = row();
            r.setPadding(0, dp(6), 0, dp(6));
            TextView name = type(12, v.ink, 70, 800);
            name.setText(m.title(german));
            name.setLetterSpacing(0.06f);
            r.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            TextView state = type(12, closed ? v.accent : v.muted, 70, 850);
            state.setText(span(spent.getOrDefault(m, 0L)) + " / " + span(allowance) + "  "
                    + (closed ? t("CLOSED FOR INVENTORY", "WEGEN INVENTUR GESCHLOSSEN")
                    : Quotas.reopened(this, m) ? t("REOPENED", "WIEDER GEÖFFNET") : t("OPEN", "GEÖFFNET")));
            state.setLetterSpacing(0.08f);
            r.addView(state);
            content.addView(r);
            content.addView(rule(v.rule), ruleParams(Math.max(1, dp(1)), 0, 0));
        }
    }

    private View field(String label, String value) {
        LinearLayout f = column();
        TextView l = type(9, v.accent, 80, 800);
        l.setText(label);
        l.setLetterSpacing(0.2f);
        f.addView(l);
        TextView val = type(13, v.ink, 50, 850);
        val.setText(value);
        val.setLetterSpacing(0.04f);
        f.addView(val);
        return f;
    }

    private void heading(String text) {
        content.addView(rule(v.accent), ruleParams(dp(2), dp(24), dp(8)));
        TextView h = type(16, v.ink, 25, 1000);
        h.setText(text);
        h.setLetterSpacing(0.24f);
        h.setPadding(0, 0, 0, dp(10));
        content.addView(h);
    }

    private void figures(String l1, String v1, String l2, String v2) {
        LinearLayout r = row();
        r.setPadding(0, 0, 0, dp(12));
        r.addView(figure(l1, v1), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        r.addView(figure(l2, v2), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        content.addView(r);
    }

    private View figure(String label, String value) {
        LinearLayout f = column();
        TextView l = type(10, v.accent, 80, 800);
        l.setText(label);
        l.setLetterSpacing(0.22f);
        f.addView(l);
        TextView val = type(26, v.ink, 25, 850);
        val.setText(value);
        val.setIncludeFontPadding(false);
        f.addView(val);
        return f;
    }

    // ---- Actions ------------------------------------------------------------

    private void confess() {
        String[] items = new String[FAULTS.length];
        for (int i = 0; i < FAULTS.length; i++) items[i] = Lang.pick(german, FAULTS[i]);
        boolean[] chosen = new boolean[FAULTS.length];
        StateMenu.dress(new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(t("I confess that", "Ich gestehe, dass"))
                .setMultiChoiceItems(items, chosen, (d, which, checked) -> chosen[which] = checked)
                .setPositiveButton(t("Confess", "Gestehen"), (d, which) -> {
                    int faults = 0;
                    for (boolean c : chosen) if (c) faults++;
                    boolean forgiven = Citizen.confess(this);
                    Record.note(this, "SELF-CRITICISM SUBMITTED: " + (faults == 0
                                    ? "NO FAULTS. SILENCE IS ALSO A CONFESSION." : faults + " FAULT(S).")
                                    + (forgiven ? " FORGIVEN." : " FILED."),
                            "SELBSTKRITIK EINGEREICHT: " + (faults == 0
                                    ? "KEINE VERFEHLUNGEN. AUCH SCHWEIGEN IST EIN GESTÄNDNIS." : faults + " VERFEHLUNG(EN).")
                                    + (forgiven ? " VERGEBEN." : " ABGELEGT."));
                    Toast.makeText(this, forgiven
                            ? t("YOU ARE FORGIVEN. YOU ARE NOT FORGOTTEN.", "IHNEN WIRD VERGEBEN. SIE WERDEN NICHT VERGESSEN.")
                            : t("Your confession has been filed. Forgiveness is rationed: one a day.",
                                    "Ihr Geständnis wurde abgelegt. Vergebung ist rationiert: eine pro Tag."),
                            Toast.LENGTH_LONG).show();
                    Stamps.award(this, "confession");
                    fill();
                    Promotion.check(this);
                })
                .setNegativeButton(t("I have nothing to say", "Ich habe nichts zu sagen"), (d, which) ->
                        Toast.makeText(this, t("Your silence has been recorded.", "Ihr Schweigen wurde aufgezeichnet."),
                                Toast.LENGTH_SHORT).show())
                .show(), v);
    }

    private void saveCard() {
        boolean ok = Exports.saveCard(this, card, v);
        if (ok) Record.note(this, "CITIZEN CARD COPIED AS A SPECIMEN.", "BÜRGERAUSWEIS ALS MUSTER KOPIERT.");
        Toast.makeText(this, ok ? t("Specimen saved in Pictures/Laibach.", "Muster in Pictures/Laibach gespeichert.")
                : t("The card could not be copied.", "Der Ausweis konnte nicht kopiert werden."), Toast.LENGTH_LONG).show();
    }

    private void shareCard() {
        Uri uri = Exports.shareCard(this, card, v);
        if (uri == null) {
            Toast.makeText(this, t("The card could not be copied.", "Der Ausweis konnte nicht kopiert werden."),
                    Toast.LENGTH_LONG).show();
            return;
        }
        Record.note(this, "CITIZEN CARD SHARED AS A SPECIMEN. IT CONFERS NO RIGHTS.",
                "BÜRGERAUSWEIS ALS MUSTER GETEILT. ER VERLEIHT KEINE RECHTE.");
        Exports.send(this, uri, t("SHARE THE SPECIMEN", "DAS MUSTER TEILEN"));
    }

    /** The day's drills, each with its answer; left out while none is scheduled. */
    private void drills() {
        int[] times = Drills.times(this);
        if (times.length == 0) return;
        heading(t("DRILL SCHEDULE", "ÜBUNGSPLAN"));
        for (int at : times) {
            char answer = Drills.answer(this, at);
            String state = answer == Drills.PRESENT ? t("PRESENT", "ANWESEND")
                    : answer == Drills.ABSENT ? t("ABSENT", "ABWESEND")
                    : Drills.called(this, at) ? t("MISSED", "VERSÄUMT") : t("PENDING", "AUSSTEHEND");
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

    /** The passport's latest page: the entry visas, newest first. The networks are never named. */
    private void passport() {
        heading(t("PASSPORT", "REISEPASS"));
        TextView note = type(11, v.muted, 80, 650);
        int n = Passport.count(this);
        note.setText(n == 0
                ? t("NO ENTRY VISAS YET. EACH WI-FI NETWORK THE PHONE ENTERS EARNS ONE; NONE IS NAMED.",
                        "NOCH KEINE EINREISEVISA. JEDES WLAN, DAS DAS TELEFON BETRITT, BRINGT EINES; KEINES WIRD GENANNT.")
                : String.format(locale, t("ENTRY VISAS STAMPED: %d. ONLY THE DATES ARE KEPT; THE NETWORKS ARE NEVER NAMED.",
                        "GESTEMPELTE EINREISEVISA: %d. NUR DIE DATEN WERDEN AUFBEWAHRT; DIE NETZE WERDEN NIE GENANNT."), n));
        note.setLetterSpacing(0.1f);
        note.setPadding(0, 0, 0, dp(10));
        content.addView(note);
        if (n > 0) content.addView(new Passport.Page(this, v, german), fullWidth());
    }

    /** The stamp album: every stamp the state issues, three to a row, the missing ones in outline. */
    private void album() {
        heading(t("STAMP ALBUM", "MARKENALBUM"));
        TextView count = type(11, v.muted, 80, 650);
        count.setText(String.format(locale, t("%d OF %d ISSUED. TAP A STAMP TO SEE IT AWARDED AGAIN.",
                "%d VON %d AUSGEGEBEN. TIPPEN SIE EINE MARKE AN, UM SIE ERNEUT VERLIEHEN ZU SEHEN."),
                Stamps.count(this), Stamps.ALL.length));
        count.setLetterSpacing(0.1f);
        count.setPadding(0, 0, 0, dp(10));
        content.addView(count);
        SimpleDateFormat date = new SimpleDateFormat(t("d MMM yyyy", "d. MMM yyyy"), locale);
        LinearLayout r = null;
        for (int i = 0; i < Stamps.ALL.length; i++) {
            if (i % 3 == 0) {
                r = row();
                r.setPadding(0, 0, 0, dp(12));
                content.addView(r);
            }
            Stamps.Stamp s = Stamps.ALL[i];
            long when = Stamps.issued(this, s.key);
            LinearLayout cell = column();
            cell.setGravity(Gravity.CENTER_HORIZONTAL);
            cell.addView(new StampArt.StampView(this, v, s.glyph, when > 0),
                    new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            TextView name = type(9, when > 0 ? v.ink : v.muted, 70, 850);
            name.setText(Lang.pick(german, s.name));
            name.setGravity(Gravity.CENTER);
            name.setLetterSpacing(0.08f);
            name.setPadding(0, dp(4), 0, 0);
            cell.addView(name);
            TextView issued = type(8, when > 0 ? v.accent : v.rule, 80, 700);
            issued.setText(when > 0 ? date.format(new Date(when)).toUpperCase(locale) : t("NOT YET ISSUED", "NOCH NICHT AUSGEGEBEN"));
            issued.setGravity(Gravity.CENTER);
            issued.setLetterSpacing(0.1f);
            cell.addView(issued);
            if (when > 0) {
                cell.setOnClickListener(view -> Ceremony.show(this, s.glyph, Lang.pick(german, s.name),
                        t("ISSUED ON ", "AUSGEGEBEN AM ") + date.format(new Date(when)).toUpperCase(locale)));
            }
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(i % 3 == 0 ? 0 : dp(6), 0, i % 3 == 2 ? 0 : dp(6), 0);
            r.addView(cell, lp);
        }
        // The last row keeps its columns even when it is short.
        while (r != null && r.getChildCount() % 3 != 0) {
            r.addView(new View(this), new LinearLayout.LayoutParams(0, 0, 1f));
        }
    }

    private void caseFile() {
        boolean ok = Exports.caseFile(this, german);
        if (ok) Record.note(this, "CASE FILE PRINTED BY THE CITIZEN.", "FALLAKTE VOM BÜRGER GEDRUCKT.");
        Toast.makeText(this, ok ? t("Case file saved in Download/Laibach.", "Fallakte in Download/Laibach gespeichert.")
                : t("The case file could not be printed.", "Die Fallakte konnte nicht gedruckt werden."), Toast.LENGTH_LONG).show();
    }

    private void backup() {
        boolean ok = Exports.backup(this);
        Toast.makeText(this, ok ? t("Settings saved in Download/Laibach.", "Einstellungen in Download/Laibach gesichert.")
                : t("The settings could not be saved.", "Die Einstellungen konnten nicht gesichert werden."), Toast.LENGTH_LONG).show();
    }

    private void restore() {
        String latest = Exports.latestBackup(this);
        if (latest == null) {
            Toast.makeText(this, t("No backup made by this launcher was found.",
                    "Keine Sicherung dieses Launchers gefunden."), Toast.LENGTH_LONG).show();
            return;
        }
        StateMenu.dress(new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(t("Restore the settings?", "Einstellungen wiederherstellen?"))
                .setMessage(t("The settings, the record and the lists will be replaced by ",
                        "Einstellungen, Akte und Listen werden ersetzt durch ") + latest + ".")
                .setPositiveButton(t("Restore", "Wiederherstellen"), (d, which) -> {
                    boolean ok = Exports.restore(this);
                    if (ok) {
                        Alarm.schedule(this);
                        Record.note(this, "SETTINGS RESTORED FROM " + latest + ". HISTORY HAS BEEN REVISED.",
                                "EINSTELLUNGEN AUS " + latest + " WIEDERHERGESTELLT. DIE GESCHICHTE WURDE ÜBERARBEITET.");
                        recreate();
                    } else {
                        Toast.makeText(this, t("The backup could not be read.", "Die Sicherung konnte nicht gelesen werden."),
                                Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton(t("Keep the present", "Die Gegenwart behalten"), null)
                .show(), v);
    }

    private void showConstitution() {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < Constitution.ARTICLES.length; i++) {
            if (i > 0) s.append("\n\n");
            s.append(t("Article ", "Artikel ")).append(Constitution.numeral(i + 1)).append(". ")
                    .append(Lang.pick(german, Constitution.ARTICLES[i]));
        }
        StateMenu.dress(new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(t("The Constitution of the Laibach Collective", "Die Verfassung des Laibach-Kollektivs"))
                .setMessage(s)
                .setPositiveButton(t("I agree", "Ich stimme zu"), null)
                .setNegativeButton(t("I do not agree", "Ich stimme nicht zu"), (d, which) -> {
                    Record.note(this, "CITIZEN DID NOT AGREE WITH THE CONSTITUTION. DISAGREEMENT IS PERMITTED.",
                            "BÜRGER STIMMTE DER VERFASSUNG NICHT ZU. WIDERSPRUCH IST ERLAUBT.");
                    Toast.makeText(this, t("Your disagreement has been noted. You agree.",
                            "Ihr Widerspruch wurde vermerkt. Sie stimmen zu."), Toast.LENGTH_LONG).show();
                    fill();
                })
                .show(), v);
    }

    // ---- Helpers ------------------------------------------------------------

    private TextView serif(float sp, int color) {
        TextView t = new TextView(this);
        t.setTypeface(Typeface.create("serif", Typeface.BOLD));
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setLetterSpacing(0.04f);
        t.setLineSpacing(0, 1.15f);
        return t;
    }

    private TextView button(String text, View.OnClickListener click) {
        TextView b = type(14, v.accent, 40, 900);
        b.setText(text);
        b.setLetterSpacing(0.12f);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(12), dp(14), dp(12), dp(14));
        GradientDrawable frame = new GradientDrawable();
        frame.setStroke(Math.max(1, dp(1.5f)), v.accent);
        b.setBackground(frame);
        b.setOnClickListener(click);
        return b;
    }

    private LinearLayout.LayoutParams spaced() {
        LinearLayout.LayoutParams lp = fullWidth();
        lp.topMargin = dp(10);
        return lp;
    }

    /** Bars drawn from the citizen number, in the manner of a shop barcode. */
    private final class Barcode extends View {
        private static final String[] DIGITS = {"0001101", "0011001", "0010011", "0111101", "0100011",
                "0110001", "0101111", "0111011", "0110111", "0001011"};
        private final Paint paint = new Paint();
        private final String bits;

        Barcode(Context context, String number) {
            super(context);
            StringBuilder b = new StringBuilder("101");
            for (char c : number.toCharArray()) if (Character.isDigit(c)) b.append(DIGITS[c - '0']);
            bits = b.append("101").toString();
            paint.setColor(v.ink);
        }

        @Override
        protected void onDraw(Canvas c) {
            float unit = getWidth() / (float) bits.length();
            for (int i = 0; i < bits.length(); i++) {
                if (bits.charAt(i) == '1') c.drawRect(i * unit, 0, (i + 1) * unit + 0.5f, getHeight(), paint);
            }
        }
    }
}
