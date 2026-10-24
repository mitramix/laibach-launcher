package dev.laibach.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/** The day's ration of attention as a bar, the part consumed in the accent. */
final class RationBar extends View {
    final Variant v;
    final Paint paint = new Paint();
    float share;

    RationBar(Context context, Variant v) {
        super(context);
        this.v = v;
    }

    void set(float share) {
        this.share = share;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        paint.setColor(v.rule);
        c.drawRect(0, 0, getWidth(), getHeight(), paint);
        paint.setColor(v.accent);
        c.drawRect(0, 0, getWidth() * Math.min(1f, share), getHeight(), paint);
        // Ticks every half hour of the ration.
        paint.setColor(v.bg);
        float tick = Math.max(1, 1.5f * getResources().getDisplayMetrics().density);
        for (int i = 1; i < 4; i++) {
            float x = getWidth() * i / 4f;
            c.drawRect(x - tick / 2, 0, x + tick / 2, getHeight(), paint);
        }
    }
}
