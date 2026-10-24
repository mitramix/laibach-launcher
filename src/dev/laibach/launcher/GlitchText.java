package dev.laibach.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.widget.TextView;
import java.util.Random;

/**
 * Text that fails whenever the state glitches: it splits into the uniform's
 * two neons, tears into slices, shakes and drops blocks, as the glitch has it.
 * Each text has its own temper, so a weak glitch passes some by and catches
 * others late, and a text can be jolted on its own (the clock, as the minute turns).
 */
final class GlitchText extends TextView {
    private static final Random tempers = new Random();

    private final Paint left = new Paint();
    private final Paint right = new Paint();
    private final Paint block = new Paint();
    private final Variant v;
    private final float dp;
    private final float temper = 0.6f + tempers.nextFloat() * 0.8f;
    private final int id = tempers.nextInt();
    private long joltStart, joltEnd;
    private int joltSeed;

    GlitchText(Context context, Variant v) {
        super(context);
        this.v = v;
        dp = context.getResources().getDisplayMetrics().density;
        left.setColorFilter(new PorterDuffColorFilter(v.glow, PorterDuff.Mode.SRC_IN));
        right.setColorFilter(new PorterDuffColorFilter(v.accent, PorterDuff.Mode.SRC_IN));
    }

    @Override
    public void onVisibilityAggregated(boolean visible) {
        super.onVisibilityAggregated(visible);
        Fx.glitch(this, visible && Fx.enabled(getContext()));
    }

    @Override
    protected void onDetachedFromWindow() {
        Fx.glitch(this, false);
        Fx.animate(this, false);
        super.onDetachedFromWindow();
    }

    /** A glitch of this text alone, for the given time. */
    void jolt(long ms) {
        if (!Fx.enabled(getContext()) || !isAttachedToWindow()) return;
        joltStart = Fx.now();
        joltEnd = joltStart + ms;
        joltSeed = tempers.nextInt();
        Fx.animate(this, true);
        postDelayed(() -> {
            Fx.animate(this, false);
            invalidate();
        }, ms + 40);
    }

    @Override
    protected void onDraw(Canvas c) {
        long t = Fx.now();
        boolean jolted = t < joltEnd;
        if (!jolted && !Fx.glitching()) {
            super.onDraw(c);
            return;
        }
        int seed = jolted ? joltSeed : Fx.seed;
        int kinds = jolted ? Fx.SPLIT | Fx.SLICE | Fx.JITTER : Fx.kinds;
        float strength = jolted ? 0.8f : Fx.strength;
        float progress = jolted ? (t - joltStart) / (float) (joltEnd - joltStart) : Fx.glitchProgress();
        Random fate = new Random(seed ^ id);
        // A weak glitch passes some texts by, and others join it a moment late.
        if (!jolted && (fate.nextFloat() > strength + 0.3f || progress < fate.nextFloat() * 0.3f)) {
            super.onDraw(c);
            return;
        }
        Random r = new Random(seed ^ id ^ (t / (35 + fate.nextInt(45))));
        // Now and then a frame holds still in the middle of it.
        if (r.nextInt(6) == 0) {
            super.onDraw(c);
            return;
        }
        float k = strength * temper * (1 - progress * 0.5f);
        int w = getWidth(), h = getHeight();
        c.save();
        if ((kinds & Fx.JITTER) != 0) c.translate((r.nextFloat() - 0.5f) * 8 * dp * k, (r.nextFloat() - 0.5f) * 4 * dp * k);
        if ((kinds & Fx.SPLIT) != 0) {
            float dx = (1.5f + r.nextFloat() * 5.5f) * dp * k;
            c.saveLayer(null, left);
            c.translate(-dx, (r.nextInt(3) - 1) * dp);
            super.onDraw(c);
            c.restore();
            c.saveLayer(null, right);
            c.translate(dx, (r.nextInt(3) - 1) * dp);
            super.onDraw(c);
            c.restore();
        }
        super.onDraw(c);
        if ((kinds & Fx.SLICE) != 0) {
            int slices = 1 + r.nextInt(1 + Math.round(3 * k));
            for (int i = 0; i < slices; i++) {
                float top = r.nextFloat() * h, height = h * (0.05f + r.nextFloat() * 0.22f);
                c.save();
                c.clipRect(0, top, w, top + height);
                // Half the slices leave a hole behind them, half a ghost.
                if (r.nextBoolean()) c.drawColor(v.bg);
                c.translate((r.nextFloat() - 0.5f) * w * (0.06f + 0.14f * k), 0);
                super.onDraw(c);
                c.restore();
            }
        }
        c.restore();
        if ((kinds & Fx.BLOCKS) != 0) {
            int blocks = 2 + r.nextInt(2 + Math.round(5 * k));
            for (int i = 0; i < blocks; i++) {
                int pick = r.nextInt(4);
                block.setColor(pick < 2 ? v.bg : pick == 2 ? v.accent : v.glow);
                float x = r.nextFloat() * w, y = r.nextFloat() * h;
                c.drawRect(x, y, x + (4 + r.nextInt(22)) * dp, y + (2 + r.nextInt(9)) * dp, block);
            }
        }
    }
}
