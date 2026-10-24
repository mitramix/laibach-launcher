package dev.laibach.launcher;

import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.View;

import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

/**
 * The one clock behind every effect, so that all of them move in the same
 * frame: about 30 frames a second, and only while some effect is on screen.
 * It also schedules the glitches, which every glitching view shares, so the
 * whole screen fails at once.
 *
 * Effects stop when the owner pacifies the display (menu → Public order),
 * when Android's animations are turned off, in battery saver, and under
 * austerity (the battery at 15% or less).
 */
final class Fx {
    private static final String KEY_PACIFIED = "pacified";
    private static final long FRAME = 33;

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final Set<View> animated = new LinkedHashSet<>();
    private static final Set<View> glitchers = new LinkedHashSet<>();
    private static final Random random = new Random();
    private static boolean ticking;
    private static long nextGlitch = SystemClock.uptimeMillis() + 3_000;
    private static long glitchStart, glitchEnd;
    private static boolean wasGlitching;
    /** Set by the home screen while the battery is at 15% or less and not charging: no motion, no colour. */
    static boolean austerity;
    /** Changes with every glitch, so each one tears differently. */
    static int seed;

    private Fx() {}

    static boolean pacified(Context c) {
        return c.getSharedPreferences(Variant.PREFS, Context.MODE_PRIVATE).getBoolean(KEY_PACIFIED, false);
    }

    static void setPacified(Context c, boolean on) {
        c.getSharedPreferences(Variant.PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_PACIFIED, on).apply();
    }

    /** Whether things may move now. */
    static boolean enabled(Context c) {
        return !pacified(c) && !austerity && ValueAnimator.areAnimatorsEnabled()
                && !c.getSystemService(PowerManager.class).isPowerSaveMode();
    }

    /** Milliseconds on the shared clock. */
    static long now() {
        return SystemClock.uptimeMillis();
    }

    /** Redraws v on every frame while it is subscribed. */
    static void animate(View v, boolean on) {
        if (on) animated.add(v); else animated.remove(v);
        kick();
    }

    /** Redraws v whenever a glitch starts or ends, and during it. */
    static void glitch(View v, boolean on) {
        if (on) glitchers.add(v); else glitchers.remove(v);
        kick();
    }

    static boolean glitching() {
        long t = now();
        return t >= glitchStart && t < glitchEnd;
    }

    /** How far into the current glitch, from 0 to 1. */
    static float glitchProgress() {
        return Math.min(1f, (now() - glitchStart) / (float) Math.max(1, glitchEnd - glitchStart));
    }

    /** Starts a glitch now, for the given time. */
    static void glitchNow(long ms) {
        glitchStart = now();
        glitchEnd = glitchStart + ms;
        seed = random.nextInt();
        kick();
    }

    private static void kick() {
        if (!ticking && (!animated.isEmpty() || !glitchers.isEmpty())) {
            ticking = true;
            MAIN.postDelayed(TICK, FRAME);
        }
    }

    private static final Runnable TICK = new Runnable() {
        @Override
        public void run() {
            long t = now();
            if (t >= nextGlitch) {
                // Mostly short tears, now and then a long breakdown.
                glitchNow(random.nextInt(8) == 0 ? 420 : 120 + random.nextInt(160));
                nextGlitch = t + 4_000 + random.nextInt(9_000);
            }
            boolean g = glitching();
            for (View v : animated) v.invalidate();
            if (g || wasGlitching) for (View v : glitchers) v.invalidate();
            wasGlitching = g;
            if (animated.isEmpty() && glitchers.isEmpty()) {
                ticking = false;
            } else {
                MAIN.postDelayed(this, FRAME);
            }
        }
    };

    /** A view that is drawn on the shared clock while it is visible and effects are on. */
    abstract static class FxView extends View {
        protected final Variant v;
        protected final float dp;

        FxView(Context context, Variant v) {
            super(context);
            this.v = v;
            dp = context.getResources().getDisplayMetrics().density;
        }

        @Override
        public void onVisibilityAggregated(boolean visible) {
            super.onVisibilityAggregated(visible);
            Fx.animate(this, visible && enabled(getContext()));
        }

        @Override
        protected void onDetachedFromWindow() {
            Fx.animate(this, false);
            super.onDetachedFromWindow();
        }
    }
}
