package dev.laibach.launcher;

import android.animation.ValueAnimator;
import android.graphics.Insets;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * The ceremony: a medal on its ribbon across the whole screen, the fanfare,
 * and a few words from the state. It covers whichever screen holds it, and
 * leaves at a tap or after eight seconds.
 */
final class Ceremony {
    private static final long STAY = 8_000;

    private Ceremony() {}

    /** Holds a ceremony over the screen: a glyph from {@link StampArt}, a title and a line beneath it. */
    static void show(StateActivity a, int glyph, String title, String line) {
        Variant v = a.v;
        ViewGroup root = a.findViewById(android.R.id.content);
        if (root == null) return;

        LinearLayout c = a.column();
        c.setBackgroundColor(v.bg);
        c.setGravity(Gravity.CENTER_HORIZONTAL);
        c.setClickable(true);
        c.setOnApplyWindowInsetsListener((view, insets) -> {
            Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            view.setPadding(bars.left + a.dp(28), bars.top + a.dp(10), bars.right + a.dp(28), bars.bottom + a.dp(28));
            return insets;
        });

        TextView by = a.type(16, v.ink, 25, 1000);
        by.setText(a.t("LAIBACH · BY DECREE", "LAIBACH · PER DEKRET"));
        by.setLetterSpacing(0.2f);
        c.addView(by, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        c.addView(a.rule(v.accent), a.ruleParams(a.dp(3), a.dp(6), 0));

        LinearLayout middle = a.column();
        middle.setGravity(Gravity.CENTER);
        StampArt.MedalView medal = new StampArt.MedalView(a, v, glyph);
        middle.addView(medal, new LinearLayout.LayoutParams(a.dp(200), a.dp(260)));
        TextView head = a.glitch(34, v.ink, 25, 1000);
        head.setText(title);
        head.setGravity(Gravity.CENTER);
        head.setLetterSpacing(0.12f);
        head.setPadding(0, a.dp(24), 0, a.dp(10));
        middle.addView(head);
        TextView words = a.type(13, v.accent, 60, 850);
        words.setText(line);
        words.setGravity(Gravity.CENTER);
        words.setLetterSpacing(0.12f);
        words.setLineSpacing(0, 1.2f);
        middle.addView(words);
        c.addView(middle, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        TextView foot = a.type(10, v.muted, 80, 650);
        foot.setText(a.t("THE COLLECTIVE APPLAUDS. BRIEFLY. TAP TO RETURN TO WORK.",
                "DAS KOLLEKTIV APPLAUDIERT. KURZ. TIPPEN, UM ZUR ARBEIT ZURÜCKZUKEHREN."));
        foot.setLetterSpacing(0.12f);
        foot.setGravity(Gravity.CENTER);
        c.addView(foot);

        root.addView(c, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        c.requestApplyInsets();

        // The medal is pinned on, then its sunburst turns until the ceremony ends.
        medal.setScaleX(0.4f);
        medal.setScaleY(0.4f);
        medal.animate().scaleX(1).scaleY(1).setDuration(700).setInterpolator(new OvershootInterpolator(2.2f)).start();
        ValueAnimator turning = ValueAnimator.ofFloat(0, (float) (Math.PI / 6));
        turning.setDuration(STAY);
        turning.setInterpolator(new LinearInterpolator());
        turning.addUpdateListener(an -> {
            medal.turn = (float) an.getAnimatedValue();
            medal.invalidate();
        });
        turning.start();
        Tones.play(a, Tones.anthem());

        Runnable end = new Runnable() {
            boolean done;

            @Override
            public void run() {
                if (done) return;
                done = true;
                turning.cancel();
                c.animate().alpha(0).setDuration(300).withEndAction(() -> root.removeView(c)).start();
            }
        };
        c.setOnClickListener(view -> end.run());
        c.postDelayed(end, STAY);
    }
}
