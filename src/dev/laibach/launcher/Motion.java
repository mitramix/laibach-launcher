package dev.laibach.launcher;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.StateListAnimator;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.AbsListView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

/**
 * How things arrive, leave and give under the finger: one easing, one pace,
 * for every screen. Like the glitches, all of it stops when effects are off
 * (Fx.enabled), and then things simply appear.
 */
final class Motion {
    /** Fast out of the gate, a long settle: the state arrives on time and stands still. */
    static final Interpolator ARRIVE = new PathInterpolator(0.05f, 0.7f, 0.1f, 1f);
    static final Interpolator LEAVE = new PathInterpolator(0.3f, 0f, 0.8f, 0.15f);
    private static final long STEP = 32;
    private static final int STEPS = 12;

    private Motion() {}

    /** The view rises into place. */
    static void enter(View view) {
        if (!Fx.enabled(view.getContext())) return;
        view.setAlpha(0f);
        view.setTranslationY(dp(view, 18));
        view.animate().alpha(1f).translationY(0f).setDuration(320).setInterpolator(ARRIVE).withLayer().start();
    }

    /** The view sinks away, then gone runs; at once when effects are off. */
    static void exit(View view, Runnable gone) {
        if (!Fx.enabled(view.getContext())) {
            gone.run();
            return;
        }
        view.setEnabled(false);
        view.animate().alpha(0f).translationY(dp(view, 12)).setDuration(170).setInterpolator(LEAVE)
                .withLayer().withEndAction(gone).start();
    }

    /** The children report one after another, each rising a little into line. */
    static void reveal(ViewGroup group, long delay) {
        if (!Fx.enabled(group.getContext())) return;
        float rise = dp(group, 10);
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            child.setAlpha(0f);
            child.setTranslationY(rise);
            child.animate().alpha(1f).translationY(0f).setStartDelay(delay + STEP * Math.min(i, STEPS))
                    .setDuration(300).setInterpolator(ARRIVE).start();
        }
    }

    /** Out with the old text, in with the new from the other side. */
    static void retype(TextView t, Runnable change) {
        if (!Fx.enabled(t.getContext())) {
            change.run();
            return;
        }
        float shift = dp(t, 8);
        t.animate().alpha(0f).translationX(-shift).setDuration(110).setInterpolator(LEAVE).withEndAction(() -> {
            change.run();
            t.setTranslationX(shift);
            t.animate().alpha(1f).translationX(0f).setDuration(260).setInterpolator(ARRIVE).start();
        }).start();
    }

    /**
     * Every control under the view gives a little when pressed, wide ones less
     * than small ones. Lists, fields and switches keep their own manner.
     */
    static void press(View view) {
        if (view instanceof AbsListView) return;
        if (view instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) view;
            for (int i = 0; i < g.getChildCount(); i++) press(g.getChildAt(i));
        }
        if (!view.isClickable() || view.getStateListAnimator() != null
                || view instanceof EditText || view instanceof CompoundButton || view.getWidth() == 0) return;
        float give = 1 - Math.max(0.015f, Math.min(0.06f, dp(view, 8) / view.getWidth()));
        StateListAnimator states = new StateListAnimator();
        states.addState(new int[]{android.R.attr.state_pressed}, scale(view, give, 90));
        states.addState(new int[0], scale(view, 1f, 220));
        view.setStateListAnimator(states);
    }

    /** Presses, once the view has been laid out and its controls have widths. */
    static void pressLater(View view) {
        if (!Fx.enabled(view.getContext())) return;
        view.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int l, int t, int r, int b, int ol, int ot, int or, int ob) {
                v.removeOnLayoutChangeListener(this);
                press(v);
            }
        });
    }

    private static AnimatorSet scale(View view, float to, long ms) {
        AnimatorSet set = new AnimatorSet();
        set.playTogether(ObjectAnimator.ofFloat(view, View.SCALE_X, to), ObjectAnimator.ofFloat(view, View.SCALE_Y, to));
        set.setDuration(ms);
        set.setInterpolator(ARRIVE);
        return set;
    }

    private static float dp(View view, float dp) {
        return dp * view.getResources().getDisplayMetrics().density;
    }
}
