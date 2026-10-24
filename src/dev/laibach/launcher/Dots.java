package dev.laibach.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/** The dotted leader between a label and its value. */
final class Dots extends View {
    private final float dp;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    Dots(Context context, int color) {
        super(context);
        dp = context.getResources().getDisplayMetrics().density;
        paint.setColor(color);
    }

    @Override
    protected void onDraw(Canvas c) {
        float step = 5 * dp, r = Math.max(1f, Math.round(dp));
        for (float x = r; x < getWidth(); x += step) c.drawCircle(x, getHeight() - r, r, paint);
    }
}
