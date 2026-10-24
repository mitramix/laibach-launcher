package dev.laibach.launcher;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;

/**
 * LAIBACH LIFE 2.0, the Synthetic State: the effects that make the home
 * screen look like a ministry's terminal in a bad future. The scene of the
 * uniform's album moves behind everything, a CRT lies over everything, the
 * big type glitches, an eye watches, a pulse is assumed and a ticker
 * broadcasts. All of it runs on the shared clock in {@link Fx}.
 *
 * <p>The effects are views of their own ({@link Backdrop}, {@link Crt},
 * {@link GlitchText}, {@link Eye}, {@link Pulse}, {@link Ticker} and
 * {@link Boot}); this class keeps the drawing they share: colour arithmetic,
 * the gear and the emblem, and the uniform's icon badges.
 */
final class Cyber {
    private Cyber() {}

    static int alpha(int color, int a) {
        return (color & 0x00FFFFFF) | (a << 24);
    }

    /** Mixes a towards b by f, from 0 to 1. */
    static int blend(int a, int b, float f) {
        int r = Math.round(((a >> 16) & 0xFF) * (1 - f) + ((b >> 16) & 0xFF) * f);
        int g = Math.round(((a >> 8) & 0xFF) * (1 - f) + ((b >> 8) & 0xFF) * f);
        int bl = Math.round((a & 0xFF) * (1 - f) + (b & 0xFF) * f);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    /** The emblem's 12-tooth wheel around (cx, cy), teeth reaching to radius r. */
    static void gear(Path gear, float cx, float cy, float r) {
        float root = r * 0.84f, half = (float) (Math.PI / 12 * 0.55);
        gear.reset();
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI / 6;
            double[][] pts = {{a - half * 1.25, root}, {a - half * 0.8, r}, {a + half * 0.8, r}, {a + half * 1.25, root}};
            for (int k = 0; k < pts.length; k++) {
                float x = cx + (float) (pts[k][1] * Math.cos(pts[k][0]));
                float y = cy + (float) (pts[k][1] * Math.sin(pts[k][0]));
                if (i == 0 && k == 0) gear.moveTo(x, y); else gear.lineTo(x, y);
            }
        }
        gear.close();
    }

    /**
     * The uniform's emblem around (cx, cy), reaching to radius r: the cogwheel of
     * the network age, or the shape each album wears instead.
     */
    static void emblem(Path p, float cx, float cy, float r, Variant v) {
        p.reset();
        switch (v.motif) {
            case Variant.CATHEDRAL: {
                // A cross with flared arms.
                float in = r * 0.2f, mid = r * 0.36f;
                for (int i = 0; i < 4; i++) {
                    Path arm = new Path();
                    arm.moveTo(-in, -in);
                    arm.lineTo(-mid, -r);
                    arm.quadTo(0, -r * 0.9f, mid, -r);
                    arm.lineTo(in, -in);
                    arm.close();
                    android.graphics.Matrix m = new android.graphics.Matrix();
                    m.setRotate(90 * i);
                    m.postTranslate(cx, cy);
                    arm.transform(m);
                    p.addPath(arm);
                }
                p.addRect(cx - r * 0.24f, cy - r * 0.24f, cx + r * 0.24f, cy + r * 0.24f, Path.Direction.CW);
                break;
            }
            case Variant.MARKET: {
                // A chip, its pins on every side.
                float s = r * 0.8f, pin = r * 0.1f;
                p.addRect(cx - s, cy - s, cx + s, cy + s, Path.Direction.CW);
                for (int k = -2; k <= 2; k++) {
                    float o = k * s * 0.38f;
                    p.addRect(cx + o - pin / 2, cy - r, cx + o + pin / 2, cy - s, Path.Direction.CW);
                    p.addRect(cx + o - pin / 2, cy + s, cx + o + pin / 2, cy + r, Path.Direction.CW);
                    p.addRect(cx - r, cy + o - pin / 2, cx - s, cy + o + pin / 2, Path.Direction.CW);
                    p.addRect(cx + s, cy + o - pin / 2, cx + r, cy + o + pin / 2, Path.Direction.CW);
                }
                break;
            }
            case Variant.ANTHEM:
                // A rosette of twenty scallops, as pinned on a uniform.
                polar(p, cx, cy, a -> r * (0.86f + 0.14f * (float) Math.abs(Math.sin(a * 10))));
                break;
            case Variant.LUNAR:
                // An iron plate, riveted round.
                p.addCircle(cx, cy, r * 0.88f, Path.Direction.CW);
                for (int i = 0; i < 16; i++) {
                    double a = i * Math.PI / 8;
                    p.addCircle(cx + (float) (r * 0.88f * Math.cos(a)), cy + (float) (r * 0.88f * Math.sin(a)),
                            r * 0.12f, Path.Direction.CW);
                }
                break;
            case Variant.TEMPLE:
                star(p, cx, cy, r, r * 0.97f, 8, Math.PI / 8);
                break;
            case Variant.TECHNO:
                p.addRect(cx - r * 0.86f, cy - r * 0.86f, cx + r * 0.86f, cy + r * 0.86f, Path.Direction.CW);
                break;
            case Variant.DAWN:
                // The sun, twenty-four rays.
                star(p, cx, cy, r, r * 0.8f, 24, 0);
                break;
            case Variant.ALPINE:
                // A mountain flower of six petals.
                polar(p, cx, cy, a -> r * (0.6f + 0.4f * (float) Math.abs(Math.cos(a * 3))));
                break;
            default:
                gear(p, cx, cy, r);
        }
    }

    private interface Radius {
        float at(double angle);
    }

    static void polar(Path p, float cx, float cy, Radius radius) {
        for (int i = 0; i < 240; i++) {
            double a = i * Math.PI * 2 / 240;
            float rr = radius.at(a);
            float x = cx + (float) (rr * Math.cos(a)), y = cy + (float) (rr * Math.sin(a));
            if (i == 0) p.moveTo(x, y); else p.lineTo(x, y);
        }
        p.close();
    }

    /** A star of n points, or a polygon when the inner radius is close to the outer. */
    static void star(Path p, float cx, float cy, float outer, float inner, int n, double turn) {
        for (int i = 0; i < n * 2; i++) {
            double a = turn - Math.PI / 2 + i * Math.PI / n;
            float rr = i % 2 == 0 ? outer : inner;
            float x = cx + (float) (rr * Math.cos(a)), y = cy + (float) (rr * Math.sin(a));
            if (i == 0) p.moveTo(x, y); else p.lineTo(x, y);
        }
        p.close();
    }

    // ---- Icons --------------------------------------------------------------

    /**
     * An app's icon as a badge of the state: in two tones of the uniform,
     * under scanlines, in a disc set in the emblem's cogwheel.
     */
    static Bitmap icon(Drawable d, int px, Variant v) {
        Bitmap b = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        float r = px / 2f;
        Path wheel = new Path();
        emblem(wheel, r, r, r, v);
        p.setColor(v.accent);
        c.drawPath(wheel, p);
        p.setColor(v.light ? v.ink : v.bg);
        c.drawCircle(r, r, r * 0.8f, p);

        c.save();
        Path disc = new Path();
        disc.addCircle(r, r, r * 0.72f, Path.Direction.CW);
        c.clipPath(disc);
        int half = Math.round(r * 0.8f);
        d.setBounds(Math.round(r) - half, Math.round(r) - half, Math.round(r) + half, Math.round(r) + half);
        d.setColorFilter(duotone(v.light ? v.ink : v.bg, v.light ? v.bg : blend(v.ink, v.glow, 0.35f)));
        d.draw(c);
        p.setColor(alpha(v.light ? v.ink : v.bg, 0x66));
        for (int y = 0; y < px; y += 3) c.drawRect(0, y, px, y + 1, p);
        c.restore();

        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(1f, px / 40f));
        p.setColor(alpha(v.glow, 0xB0));
        c.drawCircle(r, r, r * 0.76f, p);
        return b;
    }

    /** Maps brightness onto the line from dark to light, keeping the alpha. */
    static ColorMatrixColorFilter duotone(int dark, int light) {
        float[] m = new float[20];
        int[] lo = {(dark >> 16) & 0xFF, (dark >> 8) & 0xFF, dark & 0xFF};
        int[] hi = {(light >> 16) & 0xFF, (light >> 8) & 0xFF, light & 0xFF};
        for (int ch = 0; ch < 3; ch++) {
            float k = (hi[ch] - lo[ch]) / 255f;
            m[ch * 5] = 0.299f * k;
            m[ch * 5 + 1] = 0.587f * k;
            m[ch * 5 + 2] = 0.114f * k;
            m[ch * 5 + 4] = lo[ch];
        }
        m[18] = 1;
        return new ColorMatrixColorFilter(new ColorMatrix(m));
    }

    // ---- Backdrop -----------------------------------------------------------

    // ---- CRT ----------------------------------------------------------------

    // ---- Glitching type -----------------------------------------------------

    // ---- Ticker -------------------------------------------------------------

    // ---- Pulse --------------------------------------------------------------

    // ---- Eye ----------------------------------------------------------------

    // ---- Boot ---------------------------------------------------------------

}
