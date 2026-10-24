package dev.laibach.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.widget.TextView;
import java.util.Random;

/** Text that splits into the uniform's two neons and tears into slices whenever the state glitches. */
final class GlitchText extends TextView {
    private final Paint left = new Paint();
    private final Paint right = new Paint();
    private final float dp;

    GlitchText(Context context, Variant v) {
        super(context);
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
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas c) {
        if (!Fx.glitching()) {
            super.onDraw(c);
            return;
        }
        Random r = new Random(Fx.seed ^ System.identityHashCode(this) ^ (Fx.now() / 50));
        float dx = (2 + r.nextInt(6)) * dp * (1 - Fx.glitchProgress() * 0.6f);
        c.saveLayer(null, left);
        c.translate(-dx, 0);
        super.onDraw(c);
        c.restore();
        c.saveLayer(null, right);
        c.translate(dx, (r.nextInt(3) - 1) * dp);
        super.onDraw(c);
        c.restore();
        super.onDraw(c);
        int w = getWidth(), h = getHeight();
        int slices = 1 + r.nextInt(3);
        for (int i = 0; i < slices; i++) {
            float top = r.nextFloat() * h, height = h * (0.06f + r.nextFloat() * 0.2f);
            c.save();
            c.clipRect(0, top, w, top + height);
            c.translate((r.nextFloat() - 0.5f) * w * 0.14f, 0);
            super.onDraw(c);
            c.restore();
        }
    }
}
