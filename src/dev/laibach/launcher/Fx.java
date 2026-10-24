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
 * whole screen fails at once: at random, of random kinds and strength,
 * sometimes in a stutter of quick tears and rarely as a long breakdown.
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

    /** What a glitch does. Each one picks a few, so no two fail alike. */
    static final int SPLIT = 1, SLICE = 2, JITTER = 4, BLOCKS = 8, ROLL = 16, SNOW = 32, FLASH = 64;
    private static final int ALL = 127;
    /** The kinds of the current glitch. */
    static int kinds = SPLIT | SLICE;
    /** How hard the current glitch hits, from about 0.3 to 1. */
    static float strength = 1f;
    /** Quick tears still to follow the last one. */
    private static int aftershocks;

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

    static boolean has(int kind) {
        return (kinds & kind) != 0;
    }

    /** Starts a full glitch now, for the given time: the moments the state means it. */
    static void glitchNow(long ms) {
        strike(ms, SPLIT | SLICE | pick(), 1f);
    }

    private static void strike(long ms, int what, float how) {
        glitchStart = now();
        glitchEnd = glitchStart + ms;
        seed = random.nextInt();
        kinds = what;
        strength = how;
        kick();
    }

    /** A few kinds at random; the split and the slice most often. */
    private static int pick() {
        int k = random.nextInt(4) != 0 ? SPLIT : 0;
        if (random.nextInt(2) == 0) k |= SLICE;
        if (random.nextInt(4) == 0) k |= JITTER;
        if (random.nextInt(5) == 0) k |= BLOCKS;
        if (random.nextInt(6) == 0) k |= ROLL;
        if (random.nextInt(6) == 0) k |= SNOW;
        if (random.nextInt(8) == 0) k |= FLASH;
        return k == 0 ? SLICE : k;
    }

    /** The quiet between glitches: memoryless, so the next one cannot be foreseen. */
    private static long calm() {
        return 2_500 + Math.min(30_000, (long) (-Math.log(1 - random.nextDouble()) * 8_000));
    }

    /** Schedules what comes next: a lone tear, a stutter of them, or now and then a breakdown. */
    private static void next(long t) {
        if (aftershocks > 0) {
            aftershocks--;
            long ms = 50 + random.nextInt(130);
            strike(ms, pick(), 0.3f + random.nextFloat() * 0.4f);
            nextGlitch = t + ms + (aftershocks > 0 ? 70 + random.nextInt(420) : calm());
            return;
        }
        int roll = random.nextInt(24);
        long ms;
        if (roll == 0) {
            ms = 600 + random.nextInt(600);
            strike(ms, ALL, 1f);
        } else {
            ms = 90 + random.nextInt(230);
            strike(ms, pick(), 0.35f + random.nextFloat() * 0.6f);
            if (roll < 7) aftershocks = 1 + random.nextInt(3);
        }
        nextGlitch = t + ms + (aftershocks > 0 ? 70 + random.nextInt(420) : calm());
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
            if (t >= nextGlitch) next(t);
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
