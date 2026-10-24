package dev.laibach.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

/** The eye of the state, looking around and blinking now and then; a red light says it records. */
final class Eye extends Fx.FxView {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path lid = new Path();

    Eye(Context context, Variant v) {
        super(context, v);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        float cy = h / 2f;
        lid.reset();
        lid.moveTo(0, cy);
        lid.quadTo(w / 2f, cy - h * 0.62f, w, cy);
        lid.quadTo(w / 2f, cy + h * 0.62f, 0, cy);
        lid.close();
    }

    @Override
    protected void onDraw(Canvas c) {
        int w = getWidth(), h = getHeight();
        long t = Fx.now();
        float cx = w / 2f, cy = h / 2f;
        // A blink every six seconds or so, 180 ms long.
        long b = t % 6_100;
        float open = b < 180 ? Math.abs(b - 90) / 90f : 1f;
        c.save();
        c.scale(1, Math.max(0.05f, open), cx, cy);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Cyber.alpha(v.ink, 0x20));
        c.drawPath(lid, paint);
        c.save();
        c.clipPath(lid);
        float ix = cx + (float) Math.sin(t / 1_300.0) * w * 0.2f;
        float iy = cy + (float) Math.sin(t / 1_700.0) * h * 0.08f;
        paint.setColor(v.ink);
        c.drawCircle(ix, iy, h * 0.27f, paint);
        paint.setColor(v.accent);
        c.drawCircle(ix, iy, h * 0.13f, paint);
        c.restore();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1f, 1.5f * dp));
        paint.setColor(v.ink);
        c.drawPath(lid, paint);
        c.restore();
        if (t % 1_000 < 600) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(v.accent);
            c.drawCircle(w - 2.5f * dp, 2.5f * dp, 2.5f * dp, paint);
        }
    }
}
