package dev.laibach.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;

/** A band of broadcasts running right to left, endlessly. */
final class Ticker extends Fx.FxView {
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private String message = "";
    private float width;

    Ticker(Context context, Variant v) {
        super(context, v);
        text.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        text.setTextSize(11 * dp);
        text.setLetterSpacing(0.12f);
        text.setColor(v.light ? v.bg : v.bg);
    }

    void setMessage(String m) {
        if (m.equals(message)) return;
        message = m;
        width = Math.max(1, text.measureText(m));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        c.drawColor(v.accent);
        float base = getHeight() / 2f - (text.ascent() + text.descent()) / 2f;
        float x = -((Fx.now() * 42 * dp / 1000f) % width);
        for (; x < getWidth(); x += width) c.drawText(message, x, base, text);
    }
}
