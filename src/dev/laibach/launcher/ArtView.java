package dev.laibach.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.view.View;

/**
 * The approved art, centre-cropped, in greyscale and under a veil of the
 * variant's background: thin at the top, where the clock stands, and
 * thick behind the programme list, so every line stays readable.
 */
final class ArtView extends View {
    final Paint picture = new Paint(Paint.FILTER_BITMAP_FLAG);
    final Paint veil = new Paint();
    final Matrix crop = new Matrix();
    final Variant v;
    WallArt.Art current;

    ArtView(Context context, Variant v) {
        super(context);
        this.v = v;
        ColorMatrix m = new ColorMatrix();
        m.setSaturation(0f);
        float contrast = 1.15f, shift = 128 * (1 - contrast);
        m.postConcat(new ColorMatrix(new float[]{
                contrast, 0, 0, 0, shift,
                0, contrast, 0, 0, shift,
                0, 0, contrast, 0, shift,
                0, 0, 0, 1, 0}));
        picture.setColorFilter(new ColorMatrixColorFilter(m));
    }

    void setArt(WallArt.Art a) {
        if (a == null) return;
        current = a;
        fit();
        invalidate();
    }

    void fit() {
        if (current == null || getWidth() == 0) return;
        float bw = current.bitmap.getWidth(), bh = current.bitmap.getHeight();
        float scale = Math.max(getWidth() / bw, getHeight() / bh);
        crop.setScale(scale, scale);
        crop.postTranslate((getWidth() - bw * scale) / 2f, (getHeight() - bh * scale) / 2f);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        fit();
        int bg = v.bg & 0x00FFFFFF;
        veil.setShader(new LinearGradient(0, 0, 0, h,
                new int[]{bg | 0x99000000, bg | 0xB8000000, bg | 0xE6000000},
                new float[]{0f, 0.38f, 0.62f}, Shader.TileMode.CLAMP));
    }

    @Override
    protected void onDraw(Canvas c) {
        c.drawColor(v.bg);
        if (current == null) return;
        c.drawBitmap(current.bitmap, crop, picture);
        c.drawRect(0, 0, getWidth(), getHeight(), veil);
    }
}
