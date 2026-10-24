package dev.laibach.launcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import java.util.Random;

/** Over everything: scanlines, a vignette, the refresh band rolling down and the tears of a glitch. */
final class Crt extends Fx.FxView {
    private final Paint scan = new Paint();
    private final Paint vignette = new Paint();
    private final Paint band = new Paint();
    private final Paint tear = new Paint();
    private boolean live;
    private float bandH;

    Crt(Context context, Variant v) {
        super(context, v);
        int step = Math.max(3, Math.round(dp * 1.2f));
        Bitmap lines = Bitmap.createBitmap(1, step, Bitmap.Config.ARGB_8888);
        lines.setPixel(0, 0, v.light ? 0x1A000000 : 0x40000000);
        scan.setShader(new BitmapShader(lines, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT));
    }

    @Override
    public void onVisibilityAggregated(boolean visible) {
        super.onVisibilityAggregated(visible);
        live = visible && Fx.enabled(getContext());
        Fx.glitch(this, live);
    }

    @Override
    protected void onDetachedFromWindow() {
        Fx.glitch(this, false);
        super.onDetachedFromWindow();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        vignette.setShader(new RadialGradient(w / 2f, h / 2f, Math.max(w, h) * 0.72f,
                new int[]{0, 0, v.light ? 0x30000000 : 0x80000000}, new float[]{0f, 0.55f, 1f},
                Shader.TileMode.CLAMP));
        bandH = 140 * dp;
        band.setShader(new LinearGradient(0, 0, 0, bandH,
                new int[]{0, Cyber.alpha(v.glow, 0x12), 0}, null, Shader.TileMode.CLAMP));
    }

    @Override
    protected void onDraw(Canvas c) {
        int w = getWidth(), h = getHeight();
        c.drawRect(0, 0, w, h, scan);
        c.drawRect(0, 0, w, h, vignette);
        if (!live) return;
        float y = (Fx.now() % 7_000) / 7_000f * (h + bandH) - bandH;
        c.save();
        c.translate(0, y);
        c.drawRect(0, 0, w, bandH, band);
        c.restore();
        if (!Fx.glitching()) return;
        Random r = new Random(Fx.seed + Fx.now() / 66);
        int tears = 2 + r.nextInt(5);
        for (int i = 0; i < tears; i++) {
            float top = r.nextFloat() * h, height = (2 + r.nextInt(18)) * dp;
            float x = r.nextFloat() * w * 0.4f;
            tear.setColor(Cyber.alpha(i % 2 == 0 ? v.accent : v.glow, 0x50 + r.nextInt(0x50)));
            c.drawRect(x, top, x + w * (0.3f + r.nextFloat() * 0.7f), top + height, tear);
        }
        if (r.nextInt(3) == 0) c.drawColor(Cyber.alpha(v.glow, 0x0E));
    }
}
