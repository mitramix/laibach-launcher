package dev.laibach.launcher;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;
import android.view.animation.LinearInterpolator;

/**
 * The uniform's emblem (a cogwheel, a sun, a rosette...) around a disc with a
 * Suprematist cross, in the variant's colours. The emblem turns once every 24 s and the disc glows with a slow pulse in the
 * uniform's second neon; both stop while out of sight or while the display is pacified.
 */
final class EmblemView extends View {
    private final Variant v;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path gear = new Path();
    private final ValueAnimator spin = ValueAnimator.ofFloat(0f, 360f);
    private float angle;

    EmblemView(Context context, Variant v) {
        super(context);
        this.v = v;
        spin.setDuration(24_000);
        spin.setRepeatCount(ValueAnimator.INFINITE);
        spin.setInterpolator(new LinearInterpolator());
        // The gear advances like machinery: a tooth a second, snapped into place
        // with a little overshoot, then held. Redrawn only in half-degree steps,
        // and not at all while it holds.
        spin.addUpdateListener(a -> {
            float turned = (float) a.getAnimatedValue();
            float tooth = (float) Math.floor(turned / 15f) * 15f;
            float p = Math.min(1f, (turned - tooth) / 15f / 0.3f);
            float eased = 1 + 2.2f * (float) Math.pow(p - 1, 3) + 1.2f * (float) Math.pow(p - 1, 2);
            float step = Math.round((tooth + 15f * eased) * 2) / 2f;
            if (step != angle) {
                angle = step;
                invalidate();
            }
        });
    }

    @Override
    public void onVisibilityAggregated(boolean visible) {
        super.onVisibilityAggregated(visible);
        if (visible && Fx.enabled(getContext())) {
            if (!spin.isStarted()) spin.start();
            else spin.resume();
        } else {
            spin.pause();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        spin.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        float cx = w / 2f, cy = h / 2f, r = Math.min(w, h) / 2f;
        Cyber.emblem(gear, cx, cy, r, v);
    }

    @Override
    protected void onDraw(Canvas c) {
        float cx = getWidth() / 2f, cy = getHeight() / 2f, r = Math.min(getWidth(), getHeight()) / 2f;
        paint.setColor(v.ink);
        c.save();
        c.rotate(angle, cx, cy);
        c.drawPath(gear, paint);
        c.restore();
        // Four pulses a turn, as if the wheel pumped light into the disc.
        float pulse = spin.isRunning() ? 0.5f + 0.5f * (float) Math.sin(Math.toRadians(angle * 4)) : 0f;
        paint.setColor(v.accent);
        paint.setShadowLayer(r * (0.08f + 0.3f * pulse), 0, 0, v.glow);
        c.drawCircle(cx, cy, r * 0.6f, paint);
        paint.clearShadowLayer();
        paint.setColor(v.bg);
        float arm = r * 0.24f, half = r * 0.095f;
        c.drawRect(cx - half, cy - arm, cx + half, cy + arm, paint);
        c.drawRect(cx - arm, cy - half, cx + arm, cy + half, paint);
    }
}
