package dev.laibach.launcher;

import android.app.Activity;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.window.OnBackInvokedDispatcher;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Date;

/**
 * The reveille rings: over the lock screen, the clock, the call and two
 * answers. PRESENT stops it and answers the morning roll call; snoozing
 * buys five minutes and is noted in the record.
 */
public class AlarmActivity extends Activity {
    private final Handler main = new Handler(Looper.getMainLooper());
    private Variant v;
    private boolean german;
    private Typeface flex;
    private Tones.Ringer ringer;
    private boolean answered;

    private final Runnable tap = new Runnable() {
        @Override
        public void run() {
            Morse.tap(AlarmActivity.this, true);
            main.postDelayed(this, 8_000);
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        v = Variant.current(this);
        german = Lang.german(this);
        flex = Typeface.create("roboto-flex", Typeface.NORMAL);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setBackgroundDrawable(new ColorDrawable(v.bg));
        FrameLayout frame = new FrameLayout(this);
        frame.addView(new Cyber.Backdrop(this, v));
        frame.addView(buildUi());
        frame.addView(new Cyber.Crt(this, v));
        setContentView(frame);
        getWindow().getInsetsController().hide(WindowInsets.Type.systemBars());
        // Back does not answer a roll call.
        getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT, () -> { });

        Alarm.rang(this);
        Record.note(this, "REVEILLE SOUNDED.", "WECKRUF ERTÖNT.");
        ringer = new Tones.Ringer(Alarm.ANTHEM.equals(Alarm.tone(this)));
        ringer.start();
        if (Morse.on(this)) main.post(tap);
        // Ten minutes without an answer: the citizen is absent, the bell falls silent.
        main.postDelayed(() -> {
            if (!answered) finish();
        }, 10 * 60_000L);
    }

    private View buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            view.setPadding(bars.left + dp(28), bars.top + dp(96), bars.right + dp(28), bars.bottom + dp(48));
            return WindowInsets.CONSUMED;
        });
        root.addView(new EmblemView(this, v), new LinearLayout.LayoutParams(dp(72), dp(72)));

        TextView head = type(15, v.accent, 60, 900);
        head.setLetterSpacing(0.3f);
        head.setText(t("■ REVEILLE ■", "■ WECKRUF ■"));
        head.setPadding(0, dp(24), 0, 0);
        root.addView(head);

        TextView clock = type(96, v.ink, 25, 1000);
        clock.setIncludeFontPadding(false);
        clock.setText(DateFormat.format(DateFormat.is24HourFormat(this) ? "HH:mm" : "h:mm", new Date()));
        root.addView(clock);

        TextView call = type(17, v.ink, 50, 800);
        call.setGravity(Gravity.CENTER);
        call.setLetterSpacing(0.08f);
        call.setText(t("ROLL CALL. THE DAY HAS BEEN ISSUED.\nARE YOU PRESENT, CITIZEN?",
                "APPELL. DER TAG WURDE AUSGEGEBEN.\nSIND SIE ANWESEND, BÜRGER?"));
        call.setPadding(0, dp(20), 0, 0);
        root.addView(call);

        root.addView(new View(this), new LinearLayout.LayoutParams(1, 0, 1f));

        TextView present = button(t("PRESENT", "ANWESEND"), true);
        present.setOnClickListener(view -> answer());
        root.addView(present, wide());
        TextView snooze = button(t("FIVE MORE MINUTES", "FÜNF MINUTEN NOCH"), false);
        snooze.setOnClickListener(view -> snooze());
        LinearLayout.LayoutParams at = wide();
        at.topMargin = dp(14);
        root.addView(snooze, at);
        return root;
    }

    private void answer() {
        answered = true;
        // The roll call opens at 06:00; earlier, the bell is merely silenced.
        if (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) >= 6 && Citizen.rollCallDue(this)) {
            Citizen.answerRollCall(this, true);
            Record.note(this, "ROLL CALL: PRESENT AT REVEILLE.", "APPELL: ANWESEND BEIM WECKRUF.");
        }
        finish();
    }

    private void snooze() {
        answered = true;
        Alarm.snooze(this);
        Record.note(this, "REVEILLE POSTPONED BY FIVE MINUTES. NOTED.", "WECKRUF UM FÜNF MINUTEN VERSCHOBEN. VERMERKT.");
        finish();
    }

    @Override
    protected void onDestroy() {
        main.removeCallbacksAndMessages(null);
        if (ringer != null) ringer.stop();
        super.onDestroy();
    }

    private TextView button(String text, boolean primary) {
        TextView b = type(18, primary ? v.bg : v.accent, 60, 900);
        b.setText(text);
        b.setLetterSpacing(0.2f);
        b.setGravity(Gravity.CENTER);
        b.setPadding(0, dp(18), 0, dp(18));
        GradientDrawable d = new GradientDrawable();
        d.setStroke(dp(2), v.accent);
        if (primary) d.setColor(v.accent);
        b.setBackground(d);
        return b;
    }

    private LinearLayout.LayoutParams wide() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private TextView type(float sp, int color, int width, int weight) {
        TextView t = new TextView(this);
        t.setTypeface(flex);
        if (!t.setFontVariationSettings("'wdth' " + width + ", 'wght' " + weight)) {
            t.setTypeface(Typeface.create("sans-serif-condensed", weight >= 600 ? Typeface.BOLD : Typeface.NORMAL));
        }
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        return t;
    }

    private String t(String en, String de) {
        return german ? de : Lang.en(en);
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
