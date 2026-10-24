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

/**
 * Over everything: scanlines, a vignette, the refresh band rolling down, a
 * flicker now and then, and whatever the glitch brings: tears, macroblocks,
 * a slipping vertical hold, snow or a flash.
 */
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
        float bandY = (Fx.now() % 7_000) / 7_000f * (h + bandH) - bandH;
        c.save();
        c.translate(0, bandY);
        c.drawRect(0, 0, w, bandH, band);
        c.restore();
        if (!Fx.glitching()) {
            // An old set flickers now and then of its own accord.
            if (new Random(Fx.now() / 90).nextInt(110) == 0) c.drawColor(Cyber.alpha(v.glow, 0x08));
            return;
        }
        float k = Fx.strength;
        Random fate = new Random(Fx.seed);
        Random r = new Random(Fx.seed + Fx.now() / (45 + fate.nextInt(50)));
        // A held breath in the middle of the failure.
        if (r.nextInt(7) == 0) return;
        if (Fx.has(Fx.SPLIT) || Fx.has(Fx.SLICE)) {
            int tears = 1 + r.nextInt(2 + Math.round(5 * k));
            for (int i = 0; i < tears; i++) {
                float top = r.nextFloat() * h, height = (2 + r.nextInt(18)) * dp;
                float x = r.nextFloat() * w * 0.4f;
                tear.setColor(Cyber.alpha(i % 2 == 0 ? v.accent : v.glow, 0x40 + r.nextInt(0x60)));
                c.drawRect(x, top, x + w * (0.3f + r.nextFloat() * 0.7f), top + height, tear);
            }
        }
        if (Fx.has(Fx.BLOCKS)) {
            // Macroblocks, clustered where the signal broke, on an 8dp grid.
            float cell = 8 * dp, cx = fate.nextFloat() * w, cy = fate.nextFloat() * h;
            int blocks = 6 + r.nextInt(6 + Math.round(14 * k));
            for (int i = 0; i < blocks; i++) {
                float x = Math.round((cx + (float) r.nextGaussian() * 60 * dp) / cell) * cell;
                float y = Math.round((cy + (float) r.nextGaussian() * 40 * dp) / cell) * cell;
                int pick = r.nextInt(5);
                tear.setColor(pick < 2 ? v.bg : Cyber.alpha(pick == 2 ? v.accent : pick == 3 ? v.glow : v.ink, 0x90));
                c.drawRect(x, y, x + cell * (1 + r.nextInt(3)), y + cell * (1 + r.nextInt(2)), tear);
            }
        }
        if (Fx.has(Fx.ROLL)) {
            // The vertical hold slips: a dark bar with a bright edge runs down the glass.
            float bar = (12 + fate.nextInt(36)) * dp;
            float y = (fate.nextFloat() * 0.5f + Fx.glitchProgress() * 1.2f) % 1f * (h + bar) - bar;
            tear.setColor(0x60000000);
            c.drawRect(0, y, w, y + bar, tear);
            tear.setColor(Cyber.alpha(v.glow, 0x90));
            c.drawRect(0, y, w, y + 1.5f * dp, tear);
        }
        if (Fx.has(Fx.SNOW)) {
            int specks = 60 + Math.round(180 * k);
            for (int i = 0; i < specks; i++) {
                float x = r.nextFloat() * w, y = r.nextFloat() * h;
                tear.setColor(Cyber.alpha(r.nextBoolean() ? v.ink : v.glow, 0x30 + r.nextInt(0x70)));
                c.drawRect(x, y, x + (1 + r.nextInt(3)) * dp, y + dp, tear);
            }
        }
        if (Fx.has(Fx.FLASH) ? r.nextInt(2) == 0 : r.nextInt(5) == 0) {
            c.drawColor(Cyber.alpha(r.nextBoolean() ? v.glow : v.ink, Fx.has(Fx.FLASH) ? 0x1C : 0x0C));
        }
    }
}
