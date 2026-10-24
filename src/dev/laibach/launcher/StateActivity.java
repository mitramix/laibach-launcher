package dev.laibach.launcher;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

/**
 * Every screen of the state: it wears the uniform, speaks the citizen's
 * language and sets its type in Roboto Flex. The shared drawing tools live
 * here once, so a screen holds only what is its own.
 */
abstract class StateActivity extends Activity {
    /** The uniform worn. */
    Variant v;
    boolean german;
    Locale locale;
    Typeface flex;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        enlist();
        // With effects off the screens come and go without the uniform's motion.
        if (!Fx.enabled(this) && Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0);
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0);
        }
    }

    /** Reads the uniform and the language again. */
    void enlist() {
        v = Variant.current(this);
        german = Lang.german(this);
        locale = Lang.locale(german);
        flex = Typeface.create("roboto-flex", Typeface.NORMAL);
    }

    /** Shows the content in the uniform's colours, the status bar out of the way. */
    void wear(View content) {
        getWindow().setBackgroundDrawable(new ColorDrawable(v.bg));
        setContentView(content);
        Motion.pressLater(content);
        WindowInsetsController bars = getWindow().getInsetsController();
        bars.hide(WindowInsets.Type.statusBars());
        bars.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        int lightBars = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
        bars.setSystemBarsAppearance(v.light ? lightBars : 0, lightBars);
    }

    /** The English or the German text, by the chosen language. */
    String t(String en, String de) {
        return german ? de : Lang.en(en);
    }

    int dp(float value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                getResources().getDisplayMetrics()));
    }

    LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        return l;
    }

    /** Text in Roboto Flex at the given width and weight axes. */
    TextView type(float sp, int color, int width, int weight) {
        return style(new TextView(this), sp, color, width, weight);
    }

    /** Like {@link #type}, but the text breaks up whenever the state glitches. */
    TextView glitch(float sp, int color, int width, int weight) {
        return style(new GlitchText(this, v), sp, color, width, weight);
    }

    TextView style(TextView t, float sp, int color, int width, int weight) {
        t.setTypeface(flex);
        if (!t.setFontVariationSettings("'wdth' " + width + ", 'wght' " + weight)) {
            t.setTypeface(Typeface.create("sans-serif-condensed", weight >= 600 ? Typeface.BOLD : Typeface.NORMAL));
        }
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        return t;
    }

    /** A small spaced heading in the accent. */
    TextView label(String text) {
        TextView t = type(10, v.accent, 80, 800);
        t.setLetterSpacing(0.22f);
        t.setText(text);
        return t;
    }

    View rule(int color) {
        View r = new View(this);
        r.setBackgroundColor(color);
        return r;
    }

    LinearLayout.LayoutParams ruleParams(int height, int above, int below) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height);
        lp.topMargin = above;
        lp.bottomMargin = below;
        return lp;
    }

    LinearLayout.LayoutParams fullWidth() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    /** A background that ripples in the accent when touched. */
    Drawable pressable() {
        return new RippleDrawable(ColorStateList.valueOf((v.accent & 0x00FFFFFF) | 0x40000000), null,
                new ColorDrawable(0xFFFFFFFF));
    }

    void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }

    /** Opens another screen; one the phone does not have is reported, not thrown. */
    void open(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, t("Not foreseen on this device", "Auf diesem Gerät nicht vorgesehen"),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /** 1H 52M, 23M. */
    static String span(long ms) {
        long minutes = ms / 60_000;
        if (minutes < 60) return minutes + "M";
        return minutes / 60 + "H " + minutes % 60 + "M";
    }
}
