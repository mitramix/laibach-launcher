package dev.laibach.launcher;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;

import java.util.List;

/**
 * The darkroom's test picture, drawn rather than taken: three citizens on a
 * platform before a banner, with bars ready for their eyes in case the face
 * finder does not take them for people.
 */
final class Specimen {
    private Specimen() {}

    /**
     * The picture, with where each citizen's eyes are added to eyes, as
     * fractions of its width and height.
     */
    static Bitmap draw(List<RectF> eyes) {
        int w = 1600, h = 1200;
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setShader(new LinearGradient(0, 0, 0, h * 0.7f, 0xFF6F8FB0, 0xFFD9D2C0, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, p);
        p.setShader(null);
        p.setColor(0xFFF3E7C8);
        c.drawCircle(w * 0.84f, h * 0.17f, h * 0.08f, p);
        // The banner, and the platform.
        p.setColor(0xFFA3202A);
        c.drawRect(w * 0.08f, h * 0.08f, w * 0.62f, h * 0.3f, p);
        p.setColor(0xFFE9DFC6);
        c.drawRect(w * 0.11f, h * 0.17f, w * 0.59f, h * 0.21f, p);
        p.setShader(new LinearGradient(0, h * 0.72f, 0, h, 0xFF4A4038, 0xFF1C1814, Shader.TileMode.CLAMP));
        c.drawRect(0, h * 0.72f, w, h, p);
        p.setShader(null);
        float[][] people = {{0.24f, 0.47f, 1f}, {0.5f, 0.42f, 1.12f}, {0.76f, 0.48f, 0.96f}};
        int[] coats = {0xFF2B2F36, 0xFF3B3226, 0xFF27352E};
        int[] skins = {0xFFD9B08C, 0xFFC99A74, 0xFFE2BC98};
        for (int i = 0; i < people.length; i++) {
            float cx = people[i][0] * w, cy = people[i][1] * h, r = h * 0.11f * people[i][2];
            p.setColor(coats[i]);
            c.drawRoundRect(new RectF(cx - r * 1.9f, cy + r * 1.05f, cx + r * 1.9f, h * 0.95f), r * 0.8f, r * 0.8f, p);
            p.setColor(0xFFE9E4DA);
            c.drawRect(cx - r * 0.35f, cy + r * 0.95f, cx + r * 0.35f, cy + r * 1.5f, p);
            p.setShader(new RadialGradient(cx - r * 0.3f, cy - r * 0.3f, r * 1.4f, skins[i], darker(skins[i]), Shader.TileMode.CLAMP));
            c.drawOval(new RectF(cx - r * 0.82f, cy - r, cx + r * 0.82f, cy + r * 1.05f), p);
            p.setShader(null);
            p.setColor(0xFF2A211B);
            c.drawArc(new RectF(cx - r * 0.86f, cy - r * 1.08f, cx + r * 0.86f, cy + r * 0.2f), 180, 180, true, p);
            float ey = cy + r * 0.05f, ex = r * 0.34f;
            p.setColor(0xFFF6F2EA);
            c.drawOval(new RectF(cx - ex - r * 0.17f, ey - r * 0.08f, cx - ex + r * 0.17f, ey + r * 0.08f), p);
            c.drawOval(new RectF(cx + ex - r * 0.17f, ey - r * 0.08f, cx + ex + r * 0.17f, ey + r * 0.08f), p);
            p.setColor(0xFF1E1A16);
            c.drawCircle(cx - ex, ey, r * 0.07f, p);
            c.drawCircle(cx + ex, ey, r * 0.07f, p);
            c.drawRect(cx - ex - r * 0.2f, ey - r * 0.22f, cx - ex + r * 0.2f, ey - r * 0.16f, p);
            c.drawRect(cx + ex - r * 0.2f, ey - r * 0.22f, cx + ex + r * 0.2f, ey - r * 0.16f, p);
            p.setColor(darker(darker(skins[i])));
            c.drawRect(cx - r * 0.06f, ey + r * 0.1f, cx + r * 0.06f, ey + r * 0.4f, p);
            p.setColor(0xFF6E2E26);
            c.drawRect(cx - r * 0.25f, cy + r * 0.6f, cx + r * 0.25f, cy + r * 0.66f, p);
            eyes.add(new RectF((cx - r * 0.75f) / w, (ey - r * 0.2f) / h, (cx + r * 0.75f) / w, (ey + r * 0.2f) / h));
        }
        return b;
    }

    private static int darker(int c) {
        return 0xFF000000 | (((c >> 16) & 0xFF) * 3 / 4) << 16 | (((c >> 8) & 0xFF) * 3 / 4) << 8 | (c & 0xFF) * 3 / 4;
    }
}
