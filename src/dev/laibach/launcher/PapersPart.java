package dev.laibach.launcher;

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Locale;

/**
 * A section of the citizen's papers in a file of its own. Each section lays
 * itself into the papers' column with the same headings, figures and buttons.
 */
abstract class PapersPart {
    final PapersActivity papers;
    final Variant v;
    final boolean german;
    final Locale locale;
    /** Where the sections are laid one under another. */
    final LinearLayout content;

    PapersPart(PapersActivity papers) {
        this.papers = papers;
        this.v = papers.v;
        this.german = papers.german;
        this.locale = papers.locale;
        this.content = papers.content;
    }

    String t(String en, String de) { return papers.t(en, de); }
    int dp(float value) { return papers.dp(value); }
    LinearLayout row() { return papers.row(); }
    LinearLayout column() { return papers.column(); }
    TextView type(float sp, int color, int width, int weight) { return papers.type(sp, color, width, weight); }
    View rule(int color) { return papers.rule(color); }
    LinearLayout.LayoutParams ruleParams(int height, int above, int below) { return papers.ruleParams(height, above, below); }
    LinearLayout.LayoutParams fullWidth() { return papers.fullWidth(); }
    static String span(long ms) { return StateActivity.span(ms); }

    final void heading(String text) {
        content.addView(rule(v.accent), ruleParams(dp(2), dp(24), dp(8)));
        TextView h = type(16, v.ink, 25, 1000);
        h.setText(text);
        h.setLetterSpacing(0.24f);
        h.setPadding(0, 0, 0, dp(10));
        content.addView(h);
    }

    final void figures(String l1, String v1, String l2, String v2) {
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

    final TextView serif(float sp, int color) {
        TextView t = new TextView(papers);
        t.setTypeface(Typeface.create("serif", Typeface.BOLD));
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setLetterSpacing(0.04f);
        t.setLineSpacing(0, 1.15f);
        return t;
    }

    final TextView button(String text, View.OnClickListener click) {
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

    final LinearLayout.LayoutParams spaced() {
        LinearLayout.LayoutParams lp = fullWidth();
        lp.topMargin = dp(10);
        return lp;
    }
}
