package dev.laibach.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;

/** The citizen's heartbeat as the state assumes it: an ECG trace running across. */
final class Pulse extends Fx.FxView {
    private final Paint trace = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint halo = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private String label = "";
    private int bpm = 72;

    Pulse(Context context, Variant v) {
        super(context, v);
        trace.setStyle(Paint.Style.STROKE);
        trace.setStrokeWidth(1.6f * dp);
        trace.setStrokeJoin(Paint.Join.ROUND);
        trace.setColor(v.accent);
        halo.set(trace);
        halo.setStrokeWidth(5 * dp);
        halo.setColor(Cyber.alpha(v.accent, 0x40));
        text.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        text.setTextSize(10 * dp);
        text.setLetterSpacing(0.14f);
        text.setColor(v.muted);
    }

    void set(String label, int bpm) {
        this.label = label;
        this.bpm = bpm;
        invalidate();
    }

    /** One heartbeat, phase 0 to 1: P wave, the QRS spike and the T wave. */
    private static float beat(float p) {
        if (p > 0.10f && p < 0.18f) return 0.12f * (float) Math.sin((p - 0.10f) / 0.08f * Math.PI);
        if (p > 0.22f && p < 0.25f) return -0.15f * (p - 0.22f) / 0.03f;
        if (p >= 0.25f && p < 0.28f) return -0.15f + 1.15f * (p - 0.25f) / 0.03f;
        if (p >= 0.28f && p < 0.31f) return 1f - 1.4f * (p - 0.28f) / 0.03f;
        if (p >= 0.31f && p < 0.34f) return -0.4f + 0.4f * (p - 0.31f) / 0.03f;
        if (p > 0.42f && p < 0.56f) return 0.25f * (float) Math.sin((p - 0.42f) / 0.14f * Math.PI);
        return 0;
    }

    @Override
    protected void onDraw(Canvas c) {
        int w = getWidth(), h = getHeight();
        float base = h / 2f - (text.ascent() + text.descent()) / 2f;
        c.drawText(label, 0, base, text);
        float x0 = text.measureText(label) + 10 * dp, mid = h * 0.62f, amp = h * 0.5f;
        float beatW = 70 * dp, shift = Fx.now() * beatW / (60_000f / bpm);
        path.reset();
        for (float x = x0; x <= w; x += 1.5f * dp) {
            float p = ((x + shift) / beatW) % 1f;
            float y = mid - beat(p) * amp;
            if (x == x0) path.moveTo(x, y); else path.lineTo(x, y);
        }
        c.drawPath(path, halo);
        c.drawPath(path, trace);
    }
}
