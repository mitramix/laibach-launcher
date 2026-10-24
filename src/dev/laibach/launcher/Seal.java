package dev.laibach.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.PowerManager;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;

/**
 * The seal of the state: the emblem's wheel turning inside a dashed ring.
 * Held down, a ring of light closes around it; once it is closed the
 * citizen has submitted. Let go early and it opens again.
 */
final class Seal extends Fx.FxView {
    /** How long the seal is held before it closes. */
    private static final long HOLD = 1_400;
    /** Touches that began this soon after the screen came to the citizen are not theirs yet. */
    private static final long SETTLE = 600;

    /** The terminal the seal is set in. */
    interface Host {
        /** Whether the seal lies over the fingerprint sensor, so the first touch hands over. */
        boolean overSensor();

        /** When the window last got the input focus, on the uptime clock. */
        long focusedAt();

        /** The seal is being held. */
        void holding();

        /** Let go before it closed: early if it was barely held at all. */
        void hesitated(boolean early);

        /** The seal has closed, or (toSensor) a touch on the sensor hands over to the phone's lock. */
        void submitted(boolean toSensor);
    }

    private final Host host;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path wheel = new Path();
    private final RectF arc = new RectF();
    private long pressedAt;
    private boolean done;
    int size;

    Seal(Context context, Variant v, Host host) {
        super(context, v);
        this.host = host;
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeCap(Paint.Cap.ROUND);
        setClickable(true);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        float r = Math.min(w, h) / 2f;
        Cyber.emblem(wheel, 0, 0, r * 0.62f, v);
        float inset = 4 * dp;
        arc.set(inset, inset, w - inset, h - inset);
    }

    private float progress() {
        return pressedAt == 0 ? 0 : Math.min(1f, (Fx.now() - pressedAt) / (float) HOLD);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                // Pockets, palms and the finger still on the sensor from waking do not submit.
                if (!deliberate(e)) return false;
                if (host.overSensor()) {
                    // Some systems stop the sensor while the seal covers the lock, so
                    // the first touch hands over to the phone's lock, which starts it again.
                    done = true;
                    performHapticFeedback(HapticFeedbackConstants.CONFIRM);
                    host.submitted(true);
                    return true;
                }
                pressedAt = Fx.now();
                done = false;
                performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                host.holding();
                postInvalidateOnAnimation();
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (!done && pressedAt != 0) {
                    host.hesitated(e.getActionMasked() == MotionEvent.ACTION_UP && progress() < 0.25f);
                }
                pressedAt = 0;
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                // Sliding off the seal is letting go.
                if (pressedAt != 0 && !done && (e.getPointerCount() > 1 || !inside(e.getX(), e.getY()))) {
                    host.hesitated(false);
                    release();
                }
                return true;
            case MotionEvent.ACTION_POINTER_DOWN:
                if (!done) release();
                return true;
            default:
                return true;
        }
    }

    private boolean deliberate(MotionEvent e) {
        return hasWindowFocus()
                && getContext().getSystemService(PowerManager.class).isInteractive()
                && e.getDownTime() >= host.focusedAt() + SETTLE
                && inside(e.getX(), e.getY());
    }

    /** Whether a point lies on the seal's disc, not just in its square. */
    private boolean inside(float x, float y) {
        float r = Math.min(getWidth(), getHeight()) / 2f;
        return Math.hypot(x - getWidth() / 2f, y - getHeight() / 2f) <= r * 0.9f;
    }

    /** Lets go of the seal without submitting. */
    void release() {
        if (pressedAt == 0) return;
        pressedAt = 0;
        invalidate();
    }

    @Override
    public boolean performClick() {
        // For accessibility services, a click is a full submission.
        super.performClick();
        host.submitted(false);
        return true;
    }

    @Override
    protected void onDraw(Canvas c) {
        int w = getWidth(), h = getHeight();
        float cx = w / 2f, cy = h / 2f, r = Math.min(w, h) / 2f;
        long t = Fx.now();
        boolean moving = Fx.enabled(getContext());
        float p = progress();

        // The dashed outer ring turns against the wheel.
        ring.setStrokeWidth(2 * dp);
        ring.setPathEffect(new DashPathEffect(new float[]{6 * dp, 5 * dp}, moving ? -t / 40f : 0));
        ring.setColor(Cyber.alpha(v.glow, 0x70));
        c.drawCircle(cx, cy, r * 0.84f, ring);
        ring.setPathEffect(null);

        c.save();
        c.translate(cx, cy);
        c.rotate(moving ? t / 60f % 360 : 0);
        paint.setColor(v.ink);
        c.drawPath(wheel, paint);
        c.restore();

        // The disc glows brighter the longer it is held.
        float beat = moving ? 0.5f + 0.5f * (float) Math.sin(t / 400.0) : 0.5f;
        paint.setColor(Cyber.blend(v.accent, v.glow, p * 0.5f));
        paint.setShadowLayer(r * (0.08f + 0.12f * beat + 0.3f * p), 0, 0, v.glow);
        c.drawCircle(cx, cy, r * 0.4f, paint);
        paint.clearShadowLayer();
        paint.setColor(v.bg);
        float arm = r * 0.16f, half = r * 0.06f;
        c.drawRect(cx - half, cy - arm, cx + half, cy + arm, paint);
        c.drawRect(cx - arm, cy - half, cx + arm, cy + half, paint);

        if (p > 0) {
            ring.setStrokeWidth(5 * dp);
            ring.setColor(v.glow);
            c.drawArc(arc, -90, 360 * p, false, ring);
            if (p >= 1 && !done && hasWindowFocus()) {
                done = true;
                performHapticFeedback(HapticFeedbackConstants.CONFIRM);
                host.submitted(false);
            } else if (p < 1) {
                postInvalidateOnAnimation();
            }
        }
    }
}
