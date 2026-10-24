package dev.laibach.launcher;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Insets;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * The sealed terminal: put in front of the phone's own lock screen whenever the
 * screen goes off. Holding the seal submits the citizen (touching it is enough
 * where it lies over the fingerprint sensor); then the phone's own lock (PIN,
 * fingerprint, face) asks for itself, exactly as before. This is a
 * face in front of the lock, never a replacement for it.
 *
 * Switched off and on in the long-press menu (Public order → the terminal).
 */
public class LockActivity extends StateActivity {
    private static int slogan = (int) (System.currentTimeMillis() / 60_000) % Slogans.LOCK.length;

    private TextView clock, date, power, standing, submissions, curfew, proclamation, caption;
    private Ticker ticker;
    private Seal seal;
    /** Whether the seal lies over the fingerprint sensor. */
    private boolean fingerprint;
    /** When this window last got the input focus, on the uptime clock. */
    private long focusedAt = Long.MAX_VALUE;
    private boolean unsealing;

    /**
     * Where the phone's in-display fingerprint sensor sits, in this window's pixels,
     * or null where the phone does not say (then the seal sits where it likes).
     */
    private Rect sensor() {
        int x, y, w, h;
        try {
            // Most phones keep this from apps; some let them read it.
            String[] xy = systemProperty("persist.vendor.sys.fp.fod.location.X_Y").split(",");
            String[] wh = systemProperty("persist.vendor.sys.fp.fod.size.width_height").split(",");
            x = Integer.parseInt(xy[0].trim());
            y = Integer.parseInt(xy[1].trim());
            w = Integer.parseInt(wh[0].trim());
            h = Integer.parseInt(wh[1].trim());
        } catch (ReflectiveOperationException | RuntimeException e) {
            int[] known = KNOWN_SENSORS.get(Build.DEVICE);
            if (known == null) return null;
            x = known[0];
            y = known[1];
            w = h = known[2];
        }
        // The sensor is given in the panel's own pixels; the screen may run at a lower resolution.
        Display.Mode mode = getDisplay().getMode();
        float scale = getResources().getDisplayMetrics().widthPixels / (float) Math.min(mode.getPhysicalWidth(), mode.getPhysicalHeight());
        return new Rect(Math.round(x * scale), Math.round(y * scale), Math.round((x + w) * scale), Math.round((y + h) * scale));
    }

    /** Fingerprint sensors measured on phones that do not tell: device → left, top, size in panel pixels. */
    private static final java.util.Map<String, int[]> KNOWN_SENSORS = java.util.Map.of(
            "klimt", new int[]{535, 2413, 210});

    private static String systemProperty(String key) throws ReflectiveOperationException {
        Object value = Class.forName("android.os.SystemProperties").getMethod("get", String.class).invoke(null, key);
        return value == null ? "" : value.toString();
    }

    // ---- Lifecycle ----------------------------------------------------------

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setShowWhenLocked(true);

        getWindow().setBackgroundDrawable(new ColorDrawable(v.bg));
        FrameLayout frame = new FrameLayout(this);
        frame.addView(new Backdrop(this, v));
        frame.addView(buildUi());
        if (fingerprint) {
            // Over the whole screen, so that it can be touched wherever the sensor puts it.
            frame.addView(seal, new FrameLayout.LayoutParams(seal.size, seal.size, Gravity.TOP | Gravity.START));
        }
        frame.addView(new Crt(this, v));
        setContentView(frame);
        WindowInsetsController bars = getWindow().getInsetsController();
        bars.hide(WindowInsets.Type.systemBars());
        bars.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Sealed again while already sealed: a new proclamation for the new waking.
        slogan = (slogan + 1) % Slogans.LOCK.length;
    }

    @Override
    protected void onStart() {
        super.onStart();
        Terminal.sealed = true;
        unsealing = false;
        IntentFilter tick = new IntentFilter(Intent.ACTION_TIME_TICK);
        tick.addAction(Intent.ACTION_TIME_CHANGED);
        tick.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        registerReceiver(timeChanged, tick);
        registerReceiver(powerChanged, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        registerReceiver(present, new IntentFilter(Intent.ACTION_USER_PRESENT));
        fill();
    }

    @Override
    protected void onStop() {
        super.onStop();
        seal.release();
        unregisterReceiver(timeChanged);
        unregisterReceiver(powerChanged);
        unregisterReceiver(present);
    }

    @Override
    protected void onDestroy() {
        Terminal.sealed = false;
        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean focused) {
        super.onWindowFocusChanged(focused);
        // A hold only counts in a window the citizen can see and touch.
        if (focused) focusedAt = SystemClock.uptimeMillis();
        else seal.release();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        // There is no way back. There is only through.
        caption.setText(t("THERE IS NO WAY BACK. ONLY THROUGH.", "ES GIBT KEINEN WEG ZURÜCK. NUR HINDURCH."));
    }

    private final BroadcastReceiver timeChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            fillTime();
        }
    };

    /** The phone's own lock opened, by finger, face or PIN: the state accepts that as submission. */
    private final BroadcastReceiver present = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (unsealing) return;
            seal.release();
            submitted(t("FINGERPRINT ACCEPTED. WELCOME BACK, CITIZEN.",
                    "FINGERABDRUCK AKZEPTIERT. WILLKOMMEN ZURÜCK, BÜRGER."), 900);
        }
    };

    private final BroadcastReceiver powerChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            fillPower(intent);
        }
    };

    // ---- Unsealing ----------------------------------------------------------

    private void submitted() {
        submitted(t("IDENTITY CONFIRMED. WELCOME BACK, CITIZEN.",
                "IDENTITÄT BESTÄTIGT. WILLKOMMEN ZURÜCK, BÜRGER."), 380);
    }

    private void submitted(String welcome, long pause) {
        if (unsealing) return;
        unsealing = true;
        int n = Citizen.unseal(this);
        Record.noteDaily(this, "unseal",
                "FIRST SUBMISSION OF THE DAY AT " + time("HH:mm") + ".",
                "ERSTE UNTERWERFUNG DES TAGES UM " + time("HH:mm") + " UHR.");
        submissions.setText(String.valueOf(n));
        caption.setText(welcome);
        caption.setTextColor(v.glow);
        Fx.glitchNow(360);
        caption.postDelayed(this::unseal, Fx.enabled(this) ? pause : 0);
    }

    /** Hands over to the phone's own lock, which asks for the PIN or the finger if it has one. */
    private void unseal() {
        KeyguardManager keyguard = getSystemService(KeyguardManager.class);
        if (!keyguard.isKeyguardLocked()) {
            letIn();
            return;
        }
        keyguard.requestDismissKeyguard(this, new KeyguardManager.KeyguardDismissCallback() {
            @Override
            public void onDismissSucceeded() {
                letIn();
            }

            @Override
            public void onDismissCancelled() {
                unsealing = false;
                caption.setTextColor(v.accent);
                caption.setText(t("SUBMISSION WITHDRAWN. THIS IS NOTED.", "UNTERWERFUNG ZURÜCKGEZOGEN. DAS WIRD VERMERKT."));
            }

            @Override
            public void onDismissError() {
                letIn();
            }
        });
    }

    @SuppressWarnings("deprecation")
    private void letIn() {
        Terminal.sealed = false;
        if (Terminal.onUnseal != null) Terminal.onUnseal.run();
        finish();
        overridePendingTransition(0, android.R.anim.fade_out);
    }

    /** The phone's emergency dialer works above any lock; without one, the phone's own lock screen has it. */
    private void emergency() {
        Intent dial = new Intent("com.android.phone.EmergencyDialer.DIAL").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (dial.resolveActivity(getPackageManager()) != null) {
            startActivity(dial);
        } else {
            unseal();
        }
    }

    // ---- Layout -------------------------------------------------------------

    private View buildUi() {
        LinearLayout root = column();
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            Insets bars = insets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            view.setPadding(bars.left, bars.top + dp(8), bars.right, bars.bottom);
            return WindowInsets.CONSUMED;
        });
        LinearLayout body = column();
        body.setPadding(dp(22), 0, dp(22), 0);

        LinearLayout head = row();
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView sealedText = type(13, v.accent, 40, 950);
        sealedText.setText(t("■ TERMINAL SEALED", "■ TERMINAL VERSIEGELT"));
        sealedText.setLetterSpacing(0.18f);
        head.addView(sealedText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        head.addView(new Eye(this, v), new LinearLayout.LayoutParams(dp(40), dp(24)));
        body.addView(head);
        TextView os = mono(10, v.muted);
        os.setText("LAIBACH OS 2.0\n" + t("CITIZEN", "BÜRGER") + " № " + Citizen.number(this));
        os.setPadding(0, dp(4), 0, 0);
        body.addView(os);
        body.addView(rule(v.accent), ruleParams(dp(3), dp(10), dp(4)));

        clock = new GlitchText(this, v);
        style(clock, 112, v.ink, 25, 900);
        clock.setIncludeFontPadding(false);
        clock.setLetterSpacing(-0.02f);
        body.addView(clock);
        date = type(15, v.ink, 60, 800);
        date.setLetterSpacing(0.16f);
        body.addView(date);

        LinearLayout readout = column();
        readout.setPadding(0, dp(18), 0, dp(4));
        power = readout(readout, t("POWER RATION", "STROMRATION"));
        standing = readout(readout, t("STANDING", "ANSEHEN"));
        submissions = readout(readout, t("SUBMISSIONS TODAY", "UNTERWERFUNGEN HEUTE"));
        curfew = readout(readout, t("CURFEW", "SPERRSTUNDE"));
        TextView watched = readout(readout, t("SURVEILLANCE", "ÜBERWACHUNG"));
        watched.setText(t("ACTIVE", "AKTIV"));
        watched.setTextColor(v.accent);
        body.addView(readout);

        proclamation = new TextView(this);
        proclamation.setTypeface(Typeface.create("serif", Typeface.BOLD));
        proclamation.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        proclamation.setTextColor(v.ink);
        proclamation.setLetterSpacing(0.05f);
        proclamation.setLineSpacing(0, 1.15f);
        proclamation.setPadding(0, dp(12), 0, 0);
        proclamation.setOnClickListener(view -> {
            slogan = (slogan + 1) % Slogans.LOCK.length;
            proclamation.setText(Lang.pick(german, Slogans.LOCK[slogan]));
        });
        body.addView(proclamation);
        root.addView(body, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // The seal, its caption and the one way out that needs no submission.
        ticker = new Ticker(this, v);
        root.addView(ticker, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(20)));
        caption = type(13, v.accent, 40, 950);
        caption.setLetterSpacing(0.16f);
        caption.setGravity(Gravity.CENTER);
        caption.setPadding(dp(16), dp(10), dp(16), dp(6));
        root.addView(caption, fullWidth());

        FrameLayout dock = new FrameLayout(this);
        seal = new Seal(this, v, new Seal.Host() {
            @Override public boolean overSensor() { return fingerprint; }
            @Override public long focusedAt() { return focusedAt; }
            @Override public void holding() {
                caption.setTextColor(v.glow);
                caption.setText(t("SUBMITTING…", "UNTERWERFUNG LÄUFT…"));
            }
            @Override public void hesitated(boolean early) {
                caption.setTextColor(v.accent);
                caption.setText(early
                        ? t("OBEDIENCE TAKES A MOMENT. HOLD.", "GEHORSAM BRAUCHT EINEN MOMENT. HALTEN.")
                        : t("HESITATION HAS BEEN NOTED.", "ZÖGERN WURDE VERMERKT."));
            }
            @Override public void submitted(boolean toSensor) {
                if (toSensor) LockActivity.this.submitted(t("PRESENT YOUR FINGER TO THE STATE.", "LEGEN SIE DEM STAAT IHREN FINGER VOR."), 250);
                else LockActivity.this.submitted();
            }
        });
        seal.setContentDescription(t("Seal. Touch and hold to unlock.", "Siegel. Berühren und halten zum Entsperren."));
        Rect finger = sensor();
        int size = seal.size = dp(finger != null ? 136 : 168);
        if (finger == null) {
            dock.addView(seal, new FrameLayout.LayoutParams(size, size, Gravity.CENTER));
        } else {
            // The seal is the fingerprint sensor: it moves to wherever the sensor is.
            seal.addOnLayoutChangeListener((view, l, t, r, b, ol, ot, or, ob) -> {
                int[] at = new int[2];
                view.getLocationOnScreen(at);
                float x = at[0] - view.getTranslationX() + view.getWidth() / 2f;
                float y = at[1] - view.getTranslationY() + view.getHeight() / 2f;
                view.setTranslationX(finger.exactCenterX() - x);
                view.setTranslationY(finger.exactCenterY() - y);
            });
        }
        TextView sos = type(11, v.muted, 70, 800);
        sos.setText(t("EMERGENCY\n112", "NOTRUF\n112"));
        sos.setLetterSpacing(0.18f);
        sos.setPadding(dp(22), dp(10), dp(22), dp(14));
        sos.setOnClickListener(view -> emergency());
        dock.addView(sos, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.START));
        root.addView(dock, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, size + dp(8)));
        fingerprint = finger != null;
        return root;
    }

    /** A line of the readout, LABEL ........ VALUE; returns the value. */
    private TextView readout(LinearLayout into, String label) {
        LinearLayout line = row();
        line.setGravity(Gravity.BOTTOM);
        line.setPadding(0, dp(3), 0, dp(3));
        TextView name = mono(11, v.muted);
        name.setText(label);
        line.addView(name);
        LinearLayout.LayoutParams dotsAt = new LinearLayout.LayoutParams(0, dp(10), 1f);
        dotsAt.leftMargin = dp(6);
        dotsAt.rightMargin = dp(6);
        dotsAt.bottomMargin = dp(3);
        line.addView(new Dots(this, v.muted), dotsAt);
        TextView value = mono(11, v.ink);
        line.addView(value);
        into.addView(line);
        return value;
    }

    private void fill() {
        fillTime();
        fillPower(registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED)));
        int grade = Citizen.grade(this);
        standing.setText(Lang.pick(german, Citizen.STANDING[grade]));
        standing.setTextColor(grade >= 3 ? v.accent : v.ink);
        submissions.setText(String.valueOf(Citizen.unseals(this)));
        proclamation.setText(Lang.pick(german, Slogans.LOCK[slogan]));
        caption.setTextColor(v.accent);
        caption.setText(fingerprint
                ? t("TOUCH THE SEAL, THEN PRESENT YOUR FINGER", "SIEGEL BERÜHREN, DANN FINGER VORLEGEN")
                : t("HOLD THE SEAL TO SUBMIT", "SIEGEL HALTEN ZUR UNTERWERFUNG"));
        String n = Citizen.number(this);
        ticker.setMessage("   ✚   " + t("TERMINAL SEALED BY ORDER", "TERMINAL AUF ANORDNUNG VERSIEGELT")
                + "   ✚   " + t("CITIZEN № ", "BÜRGER NR. ") + n + t(" IS EXPECTED", " WIRD ERWARTET")
                + "   ✚   " + t("ENTRY IS CONSENT", "EINTRITT IST ZUSTIMMUNG")
                + "   ✚   " + t("THE STATE NEVER SLEEPS. YOU MAY.", "DER STAAT SCHLÄFT NIE. SIE DÜRFEN.")
                + "   ✚   " + t("ALL ACCESS IS LOGGED", "JEDER ZUGRIFF WIRD PROTOKOLLIERT")
                + "   ✚   LAIBACH OS 2.0");
    }

    private void fillTime() {
        clock.setText(time("HH:mm"));
        date.setText(new SimpleDateFormat("EEEE · d MMMM yyyy", locale).format(new Date()).toUpperCase(locale));
        if (!Citizen.curfewOn(this)) {
            curfew.setText(t("ABOLISHED", "ABGESCHAFFT"));
            curfew.setTextColor(v.muted);
        } else {
            int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            boolean on = h >= 23 || h < 6;
            curfew.setText(on ? t("IN FORCE UNTIL 06:00", "IN KRAFT BIS 06:00") : t("FROM 23:00", "AB 23:00"));
            curfew.setTextColor(on ? v.accent : v.ink);
        }
    }

    private void fillPower(Intent battery) {
        if (battery == null) return;
        int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        int status = battery.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        if (level < 0) return;
        int percent = Math.round(100f * level / scale);
        boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
        power.setText(percent + "%" + (charging ? t(" · BEING FED", " · WIRD GESPEIST") : ""));
        power.setTextColor(percent <= 15 && !charging ? v.accent : v.ink);
    }

    // ---- Helpers ------------------------------------------------------------

    private String time(String pattern) {
        return new SimpleDateFormat(pattern, locale).format(new Date());
    }

    private TextView mono(float sp, int color) {
        TextView t = new TextView(this);
        t.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setLetterSpacing(0.06f);
        t.setMaxLines(1);
        return t;
    }
}
