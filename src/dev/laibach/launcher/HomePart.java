package dev.laibach.launcher;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

/**
 * A part of the home screen in a file of its own: the curfew, the roll call,
 * the decrees, the regulations and the rest. Each part draws with the home
 * screen's tools and in its uniform, and reaches the rest of the screen
 * through {@link #home}.
 */
abstract class HomePart {
    final HomeActivity home;
    final Variant v;
    final boolean german;
    final Locale locale;

    HomePart(HomeActivity home) {
        this.home = home;
        this.v = home.v;
        this.german = home.german;
        this.locale = home.locale;
    }

    String t(String en, String de) { return home.t(en, de); }
    int dp(float value) { return home.dp(value); }
    LinearLayout row() { return home.row(); }
    LinearLayout column() { return home.column(); }
    TextView type(float sp, int color, int width, int weight) { return home.type(sp, color, width, weight); }
    TextView glitch(float sp, int color, int width, int weight) { return home.glitch(sp, color, width, weight); }
    TextView label(String text) { return home.label(text); }
    View rule(int color) { return home.rule(color); }
    LinearLayout.LayoutParams ruleParams(int height, int above, int below) { return home.ruleParams(height, above, below); }
    Drawable pressable() { return home.pressable(); }
    void open(Intent intent) { home.open(intent); }
    void toast(String text) { home.toast(text); }
    static String span(long ms) { return StateActivity.span(ms); }
}
