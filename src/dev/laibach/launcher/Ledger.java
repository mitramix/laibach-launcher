package dev.laibach.launcher;

import android.content.Intent;
import android.os.SystemClock;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/** The citizen's ledger on the monument: number, standing, ration, service and life force. */
final class Ledger extends HomePart {
    TextView number;
    TextView standing;
    TextView uptimeValue;
    TextView rationLabel;
    TextView rationValue;
    RationBar rationBar;
    TextView rationNote;
    TextView energyLabel;
    TextView energyValue;
    Pulse pulse;

    Ledger(HomeActivity home) {
        super(home);
    }

    /** The citizen's papers: number and standing, then the day's ration of attention and life force. */
    View build() {
        LinearLayout c = column();
        // The number is long, so the standing goes on the line under it.
        LinearLayout papers = row();
        papers.setOrientation(LinearLayout.VERTICAL);
        number = type(11, v.ink, 70, 800);
        number.setLetterSpacing(0.14f);
        number.setSingleLine(true);
        number.setText(t("CITIZEN № ", "BÜRGER NR. ") + Citizen.number(home));
        papers.addView(number);
        standing = type(11, v.ink, 70, 800);
        standing.setLetterSpacing(0.14f);
        papers.addView(standing);
        papers.setPadding(0, 0, 0, dp(8));
        papers.setOnClickListener(view -> home.startActivity(new Intent(home, PapersActivity.class)));
        if (!home.formation.has(Formation.PAPERS)) papers.setVisibility(View.GONE);
        c.addView(papers);

        LinearLayout stats = row();
        uptimeValue = stat(stats, label(t("SERVICE", "DIENSTZEIT")));
        rationLabel = label("");
        rationValue = stat(stats, rationLabel);
        energyLabel = label("");
        energyValue = stat(stats, energyLabel);
        c.addView(stats);
        rationBar = new RationBar(home, v);
        rationBar.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams bar = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(5));
        bar.topMargin = dp(8);
        c.addView(rationBar, bar);
        rationNote = type(10, v.muted, 80, 650);
        rationNote.setLetterSpacing(0.12f);
        rationNote.setPadding(0, dp(4), 0, 0);
        rationNote.setGravity(home.formation.gravity);
        c.addView(rationNote);
        if (!home.formation.has(Formation.STATS)) {
            stats.setVisibility(View.GONE);
            rationBar.setVisibility(View.GONE);
            rationNote.setVisibility(View.GONE);
        }
        pulse = new Pulse(home, v);
        pulse.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams beat = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(22));
        beat.topMargin = dp(6);
        if (!home.formation.has(Formation.PULSE)) pulse.setVisibility(View.GONE);
        c.addView(pulse, beat);
        return c;
    }

    /** Adds a labelled column to the stats row and returns its value view. */
    TextView stat(LinearLayout stats, TextView label) {
        LinearLayout col = column();
        col.addView(label);
        TextView value = type(26, v.ink, 25, 850);
        value.setIncludeFontPadding(false);
        col.addView(value);
        stats.addView(col, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return value;
    }

    void update() {
        long used = Citizen.used(home);
        long left = Citizen.RATION - used;
        boolean over = left < 0;
        rationLabel.setText(over ? t("OVERDRAWN", "ÜBERZOGEN") : t("RATION LEFT", "RESTZUTEILUNG"));
        rationValue.setText((over ? "+" : "") + span(Math.abs(left)));
        rationValue.setTextColor(over ? v.accent : v.ink);
        float share = used / (float) Citizen.RATION;
        rationBar.set(share);
        // The assumed heartbeat rises as the standing falls.
        int bpm = 64 + 9 * Citizen.grade(home) + (over ? 12 : 0);
        pulse.set(String.format(locale, t("♥ %d BPM (ASSUMED)", "♥ %d BPM (ANGENOMMEN)"), bpm), bpm);
        String note = String.format(locale, t("%d%% OF %s ATTENTION CONSUMED", "%d%% VON %s AUFMERKSAMKEIT VERBRAUCHT"),
                Math.round(share * 100), span(Citizen.RATION));
        if (over) {
            Record.noteDaily(home, "overdrawn", "ATTENTION RATION OVERDRAWN. THE STATE EXTENDS CREDIT.",
                    "AUFMERKSAMKEITSRATION ÜBERZOGEN. DER STAAT GEWÄHRT KREDIT.");
        }
        if (!Citizen.usageAccess(home)) note += t(" (ESTIMATED)", " (GESCHÄTZT)");
        else if (over) note += t(" (REPORTED)", " (GEMELDET)");
        rationNote.setText(note);
        rationValue.setContentDescription(rationLabel.getText() + " " + rationValue.getText());
        int grade = Citizen.grade(home);
        SpannableString s = new SpannableString(t("STANDING ", "ANSEHEN ") + Lang.pick(german, Citizen.STANDING[grade]));
        if (grade >= 3) {
            s.setSpan(new ForegroundColorSpan(v.accent), s.length() - Lang.pick(german, Citizen.STANDING[grade]).length(),
                    s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        standing.setText(s);
    }

    String uptime() {
        long minutes = SystemClock.elapsedRealtime() / 60_000;
        long days = minutes / 1440, hours = minutes / 60 % 24, mins = minutes % 60;
        if (days > 0) return days + t("D ", "T ") + hours + "H";
        if (hours > 0) return hours + "H " + mins + "M";
        return mins + " MIN";
    }

    void updateService() {
        uptimeValue.setText(uptime());
    }

    /** Life force: the battery, in the accent when it runs low off the charger. */
    void updateEnergy() {
        String energy = t("LIFE FORCE", "LEBENSKRAFT");
        energyLabel.setText(home.charging ? energy + " ▲" : energy);
        energyValue.setText(home.batteryPct < 0 ? "—" : home.batteryPct + "%");
        energyValue.setContentDescription(energy + " " + (home.batteryPct < 0 ? "—" : home.batteryPct + "%"));
        energyValue.setTextColor(home.batteryPct >= 0 && home.batteryPct <= 15 && !home.charging ? v.accent : v.ink);
    }
}
