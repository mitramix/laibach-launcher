package dev.laibach.launcher;

import android.content.Intent;
import android.graphics.Typeface;
import android.provider.AlarmClock;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.format.DateFormat;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * The monument: the clock, the date and plan day, the proclamation of the
 * day, then the parts the home screen stands under it (the dead drop, the
 * ledger, the decree, on air and the art credit).
 */
final class Monument extends HomePart {
    private GlitchText clock;
    private TextView date;
    private TextView slogan;
    private int sloganIndex = Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % Slogans.HOME.length;
    /** The whole of it, stepped aside while a command is typed. */
    View view;

    Monument(HomeActivity home) {
        super(home);
    }

    /** The clock, date and proclamation, with the parts given standing under them in order. */
    View build(View... parts) {
        Formation formation = home.formation;
        LinearLayout mon = column();
        clock = (GlitchText) glitch(formation.clockSp, v.ink, 25, 900);
        if (formation.mono) {
            clock.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        } else {
            clock.setFontVariationSettings("'wdth' 25, 'wght' 900, 'opsz' 144");
        }
        // The obelisk stands the hour on the minute, stone on stone.
        if (formation.stacked) clock.setLineSpacing(0, 0.8f);
        clock.setGravity(formation.gravity);
        clock.setIncludeFontPadding(false);
        clock.setPadding(0, dp(8), 0, 0);
        clock.setOnClickListener(view -> open(new Intent(AlarmClock.ACTION_SHOW_ALARMS)));
        clock.setOnLongClickListener(view -> {
            home.gearMenu.show(view);
            return true;
        });
        mon.addView(clock);
        date = type(15, v.ink, 60, 750);
        date.setLetterSpacing(0.12f);
        date.setGravity(formation.gravity);
        if (formation.mono) {
            // Fixed-width letters are wider; the date keeps to one line.
            date.setTypeface(Typeface.MONOSPACE);
            date.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            date.setLetterSpacing(0.02f);
        }
        mon.addView(date);
        slogan = new TextView(home);
        slogan.setTypeface(Typeface.create("serif", Typeface.BOLD));
        slogan.setTextSize(TypedValue.COMPLEX_UNIT_SP, formation.headlineSp);
        slogan.setTextColor(v.ink);
        // A headline is set tight; a proclamation is spaced out.
        slogan.setLetterSpacing(formation.headlineSp > 16 ? 0.01f : 0.06f);
        slogan.setGravity(formation.gravity);
        if (!formation.has(Formation.SLOGAN)) slogan.setVisibility(View.GONE);
        slogan.setPadding(0, dp(10), 0, dp(14));
        slogan.setOnClickListener(view -> {
            sloganIndex = (sloganIndex + 1) % Slogans.HOME.length;
            Motion.retype(slogan, this::showSlogan);
        });
        showSlogan();
        mon.addView(slogan);
        for (View part : parts) mon.addView(part);
        view = mon;
        return mon;
    }

    void updateClock() {
        Date now = new Date();
        String pattern = DateFormat.is24HourFormat(home) ? "HH:mm" : "h:mm";
        String hhmm = new SimpleDateFormat(pattern, locale).format(now);
        int colon = hhmm.indexOf(':');
        SpannableString time;
        if (home.formation.stacked && colon >= 0) {
            // The hour over the minute, the minute in the accent.
            time = new SpannableString(hhmm.substring(0, colon) + "\n" + hhmm.substring(colon + 1));
            time.setSpan(new ForegroundColorSpan(v.accent), colon + 1, time.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else {
            time = new SpannableString(hhmm);
            if (colon >= 0) {
                time.setSpan(new ForegroundColorSpan(v.accent), colon, colon + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        // The clock tears as the minute turns.
        if (clock.length() > 0 && !time.toString().contentEquals(clock.getText())) clock.jolt(240);
        clock.setText(time);
        int day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        date.setText("■ " + new SimpleDateFormat(t("EEEE, d MMMM", "EEEE, d. MMMM"), locale)
                .format(now).toUpperCase(locale) + t(" · PLAN DAY ", " · PLANTAG ") + day);
    }

    void showSlogan() {
        // A tired phone overrides the proclamation of the day.
        String text = home.batteryPct >= 0 && home.batteryPct <= 15 && !home.charging
                ? t("LIFE FORCE LOW. REPORT TO A CHARGING STATION.", "LEBENSKRAFT NIEDRIG. MELDEN SIE SICH AN EINER LADESTATION.")
                : Lang.pick(german, Slogans.HOME[sloganIndex]);
        SpannableString s = new SpannableString("■ " + text);
        s.setSpan(new ForegroundColorSpan(v.accent), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        slogan.setText(s);
    }
}
