package dev.laibach.launcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.widget.TextView;

import java.util.Random;

/**
 * LAIBACH LIFE 2.0, the Synthetic State: the effects that make the home
 * screen look like a ministry's terminal in a bad future. The scene of the
 * uniform's album moves behind everything, a CRT lies over everything, the
 * big type glitches, an eye watches, a pulse is assumed and a ticker
 * broadcasts. All of it runs on the shared clock in {@link Fx}.
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

    private static void polar(Path p, float cx, float cy, Radius radius) {
        for (int i = 0; i < 240; i++) {
            double a = i * Math.PI * 2 / 240;
            float rr = radius.at(a);
            float x = cx + (float) (rr * Math.cos(a)), y = cy + (float) (rr * Math.sin(a));
            if (i == 0) p.moveTo(x, y); else p.lineTo(x, y);
        }
        p.close();
    }

    /** A star of n points, or a polygon when the inner radius is close to the outer. */
    private static void star(Path p, float cx, float cy, float outer, float inner, int n, double turn) {
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
    private static ColorMatrixColorFilter duotone(int dark, int light) {
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

    /**
     * Behind the home screen, the scene of the uniform's album: banners in a nave,
     * the market's chart, an anthem's broadcast, the moon, the network, a temple
     * on the mountain, a techno tunnel, the sun rising again, the singing Alps.
     */
    static final class Backdrop extends Fx.FxView {
        private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint dash = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint glyph = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path wheel = new Path();
        private final Path shape = new Path();
        private final String glyphs;
        private float radius;
        private float[] heads = new float[0];
        private float[] speeds = new float[0];
        /** Fixed scatter for stars, ash, notes and the like: x, y, size, phase. */
        private float[][] scatter = new float[0][];
        private float[] ridge = new float[0];

        Backdrop(Context context, Variant v) {
            super(context, v);
            line.setStyle(Paint.Style.STROKE);
            dash.setStyle(Paint.Style.STROKE);
            glyph.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
            glyph.setTextAlign(Paint.Align.CENTER);
            glyph.setTextSize(11 * dp);
            glyphs = v.motif == Variant.MARKET ? "$€£¥%+−0123456789▲▼" : "LAIBACH1980NSK0110■✚▲ДЕЛОKUNST";
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            radius = w * 0.62f;
            emblem(wheel, 0, 0, radius, v);
            dash.setStrokeWidth(6 * dp);
            dash.setPathEffect(new DashPathEffect(new float[]{2 * dp, 9 * dp}, 0));
            int cols = Math.max(1, (int) (w / (24 * dp)));
            heads = new float[cols];
            speeds = new float[cols];
            Random r = new Random(1980);
            for (int i = 0; i < cols; i++) {
                heads[i] = r.nextFloat() * h;
                speeds[i] = (30 + r.nextInt(110)) * dp;
            }
            scatter = new float[90][];
            for (int i = 0; i < scatter.length; i++) {
                scatter[i] = new float[]{r.nextFloat() * w, r.nextFloat() * h, r.nextFloat(), r.nextFloat()};
            }
            ridge = new float[17];
            for (int i = 0; i < ridge.length; i++) ridge[i] = r.nextFloat();
        }

        @Override
        protected void onDraw(Canvas c) {
            int w = getWidth(), h = getHeight();
            long t = Fx.now();
            line.setPathEffect(null);
            switch (v.motif) {
                case Variant.CATHEDRAL: cathedral(c, w, h, t); break;
                case Variant.MARKET: market(c, w, h, t); break;
                case Variant.ANTHEM: anthem(c, w, h, t); break;
                case Variant.LUNAR: lunar(c, w, h, t); break;
                case Variant.TEMPLE: temple(c, w, h, t); break;
                case Variant.TECHNO: techno(c, w, h, t); break;
                case Variant.DAWN: dawn(c, w, h, t); break;
                case Variant.ALPINE: alpine(c, w, h, t); break;
                default: network(c, w, h, t);
            }
        }

        /** The emblem, huge, turning slowly in a corner; most scenes keep it. */
        private void bigEmblem(Canvas c, float cx, float cy, long t, int a) {
            c.save();
            c.translate(cx, cy);
            c.rotate((t % 180_000) / 500f);
            line.setColor(alpha(v.accent, a));
            line.setStrokeWidth(2 * dp);
            c.drawPath(wheel, line);
            c.drawCircle(0, 0, radius * 0.5f, line);
            c.restore();
        }

        // OPUS DEI: banners hang in a dark nave, ash falls, a bell tolls every few seconds.
        private void cathedral(Canvas c, int w, int h, long t) {
            int banners = 5;
            float gap = w / (float) banners, bw = gap * 0.56f;
            for (int i = 0; i < banners; i++) {
                float sway = (float) Math.sin(t / 2600.0 + i * 1.3) * 3 * dp;
                float x = gap * (i + 0.5f) - bw / 2 + sway, len = h * (0.42f + 0.08f * (i % 2));
                shape.reset();
                shape.moveTo(x, 0);
                shape.lineTo(x + bw, 0);
                shape.lineTo(x + bw, len);
                shape.lineTo(x + bw / 2, len - bw * 0.45f);
                shape.lineTo(x, len);
                shape.close();
                fill.setColor(alpha(i == banners / 2 ? v.accent : v.ink, i == banners / 2 ? 0x30 : 0x10));
                c.drawPath(shape, fill);
                // A cross on each, cut out of the cloth.
                fill.setColor(alpha(v.bg, 0x90));
                float cx = x + bw / 2, cy = len * 0.3f, arm = bw * 0.26f, half = bw * 0.07f;
                c.drawRect(cx - half, cy - arm, cx + half, cy + arm, fill);
                c.drawRect(cx - arm, cy - half, cx + arm, cy + half, fill);
            }
            // The bell: rings going out from the top of the nave, one toll every five seconds.
            line.setStrokeWidth(2 * dp);
            for (int k = 0; k < 3; k++) {
                float age = ((t + k * 600) % 5_000) / 5_000f;
                line.setColor(alpha(v.ink, Math.round(0x40 * (1 - age))));
                c.drawCircle(w / 2f, h * 0.18f, w * (0.1f + age * 0.9f), line);
            }
            // Ash.
            for (float[] s : scatter) {
                float y = (s[1] + t / 1000f * (10 + 20 * s[2]) * dp) % h;
                float x = s[0] + (float) Math.sin(t / 1500.0 + s[3] * 6) * 6 * dp;
                fill.setColor(alpha(v.ink, Math.round(0x18 + 0x30 * s[2])));
                c.drawCircle(x, y, (0.8f + 1.4f * s[2]) * dp, fill);
            }
        }

        // NOVA AKROPOLA: a temple's columns before the mountain, the sun crossing above.
        private void temple(Canvas c, int w, int h, long t) {
            float base = h * 0.66f;
            float sun = (t % 240_000) / 240_000f;
            float sx = w * (0.1f + 0.8f * sun), sy = h * (0.32f - 0.2f * (float) Math.sin(Math.PI * sun));
            fill.setShader(new RadialGradient(sx, sy, w * 0.3f, alpha(v.glow, 0x40), 0, Shader.TileMode.CLAMP));
            c.drawCircle(sx, sy, w * 0.3f, fill);
            fill.setShader(null);
            fill.setColor(alpha(v.glow, 0x50));
            c.drawCircle(sx, sy, w * 0.05f, fill);
            // Three peaks, the middle one highest.
            shape.reset();
            shape.moveTo(0, base);
            shape.lineTo(w * 0.18f, h * 0.44f);
            shape.lineTo(w * 0.32f, h * 0.5f);
            shape.lineTo(w * 0.5f, h * 0.34f);
            shape.lineTo(w * 0.68f, h * 0.49f);
            shape.lineTo(w * 0.82f, h * 0.42f);
            shape.lineTo(w, base);
            shape.close();
            fill.setColor(alpha(v.ink, 0x12));
            c.drawPath(shape, fill);
            line.setColor(alpha(v.ink, 0x30));
            line.setStrokeWidth(1.5f * dp);
            c.drawPath(shape, line);
            // The temple, in outline: pediment, architrave, seven fluted columns, steps.
            float left = w * 0.1f, right = w * 0.9f, top = base - h * 0.05f;
            shape.reset();
            shape.moveTo(left - 8 * dp, top);
            shape.lineTo(w / 2f, top - h * 0.07f);
            shape.lineTo(right + 8 * dp, top);
            shape.close();
            fill.setColor(alpha(v.accent, 0x30));
            c.drawPath(shape, fill);
            c.drawRect(left - 8 * dp, top, right + 8 * dp, top + 8 * dp, fill);
            line.setColor(alpha(v.ink, 0x26));
            line.setStrokeWidth(Math.max(1f, dp));
            float colTop = top + 12 * dp, colBottom = h * 0.95f, span = (right - left) / 6.5f;
            for (int i = 0; i < 7; i++) {
                float x = left + span * (i + 0.25f);
                for (int f = 0; f < 4; f++) {
                    float fx = x + f * span * 0.12f;
                    c.drawLine(fx, colTop, fx, colBottom, line);
                }
            }
            for (int s = 0; s < 3; s++) {
                float y = colBottom + s * 6 * dp;
                c.drawLine(left - (s + 1) * 8 * dp, y, right + (s + 1) * 8 * dp, y, line);
            }
        }

        // KAPITAL: the market's line runs across the screen, volume below, money falling, a barcode.
        private void market(Canvas c, int w, int h, long t) {
            rain(c, w, h, t, v.muted, v.accent, h * 0.5f);
            float mid = h * 0.42f, amp = h * 0.08f, scroll = t / 1000f * 40 * dp;
            shape.reset();
            int steps = 60;
            for (int i = 0; i <= steps; i++) {
                float x = w * i / (float) steps;
                float y = mid - amp * price((x + scroll) / (w / 9f));
                if (i == 0) shape.moveTo(x, y); else shape.lineTo(x, y);
            }
            line.setColor(alpha(v.accent, 0x70));
            line.setStrokeWidth(2 * dp);
            c.drawPath(shape, line);
            shape.lineTo(w, mid + amp * 1.6f);
            shape.lineTo(0, mid + amp * 1.6f);
            shape.close();
            fill.setColor(alpha(v.accent, 0x10));
            c.drawPath(shape, fill);
            // Volume: one bar a step, against the same tape.
            float barW = w / (float) steps;
            for (int i = 0; i < steps; i++) {
                double k = Math.floor((i * barW + scroll) / barW);
                float vol = (float) Math.abs(Math.sin(k * 12.9898) * 43758.5453 % 1);
                fill.setColor(alpha(v.ink, 0x14));
                c.drawRect(i * barW + 1, h * 0.62f - vol * h * 0.06f, (i + 1) * barW - 1, h * 0.62f, fill);
            }
            // The barcode of the citizen as merchandise.
            Random r = new Random(1992);
            float x = w * 0.58f;
            fill.setColor(alpha(v.ink, 0x2A));
            while (x < w * 0.94f) {
                float bar = (1 + r.nextInt(4)) * dp;
                if (r.nextBoolean()) c.drawRect(x, h * 0.05f, x + bar, h * 0.13f, fill);
                x += bar + dp;
            }
        }

        /** The price, a sum of waves, never quite repeating. */
        private static float price(float x) {
            return (float) (0.55 * Math.sin(x * 0.9) + 0.3 * Math.sin(x * 2.3 + 1) + 0.15 * Math.sin(x * 5.7 + 2));
        }

        // WAT: a tunnel of squares rushing out, the equaliser on the beat.
        private void techno(Canvas c, int w, int h, long t) {
            float cx = w / 2f, cy = h * 0.4f;
            float beat = (t % 469) / 469f; // 128 beats a minute
            line.setStrokeWidth(1.5f * dp);
            for (int k = 0; k < 10; k++) {
                float z = (k + (t % 3_752) / 3_752f * 2 % 1) / 10f;
                float s = w * 1.2f * z * z;
                line.setColor(alpha(k % 3 == 0 ? v.accent : v.ink, Math.round(0x48 * z)));
                c.drawRect(cx - s / 2, cy - s / 2, cx + s / 2, cy + s / 2, line);
            }
            int bars = 32;
            float bw = w / (float) bars, floor = h * 0.98f;
            long bar = t / 469;
            for (int i = 0; i < bars; i++) {
                float level = (float) Math.abs(Math.sin((bar * 31 + i * 7) * 12.9898) * 43758.5453 % 1);
                level *= 1 - beat * 0.6f;
                fill.setColor(alpha(i % 8 == 0 ? v.accent : v.ink, 0x22));
                c.drawRect(i * bw + dp, floor - level * h * 0.12f, (i + 1) * bw - dp, floor, fill);
            }
            // The clock that everyone is on: a hand, one turn a bar.
            line.setColor(alpha(v.ink, 0x30));
            c.drawCircle(cx, cy, w * 0.08f, line);
            double a = ((t % 1_875) / 1_875.0) * Math.PI * 2 - Math.PI / 2;
            c.drawLine(cx, cy, cx + (float) (Math.cos(a) * w * 0.07f), cy + (float) (Math.sin(a) * w * 0.07f), line);
        }

        // VOLK: a banner ripples, the ring of nations turns, the loudspeaker broadcasts.
        private void anthem(Canvas c, int w, int h, long t) {
            float top = h * 0.08f, band = h * 0.035f;
            for (int s = 0; s < 3; s++) {
                shape.reset();
                for (int i = 0; i <= 40; i++) {
                    float x = w * i / 40f;
                    float wave = (float) Math.sin(x / (w * 0.22f) - t / 700.0) * 8 * dp * (x / w);
                    float y = top + s * band + wave;
                    if (i == 0) shape.moveTo(x, y); else shape.lineTo(x, y);
                }
                for (int i = 40; i >= 0; i--) {
                    float x = w * i / 40f;
                    float wave = (float) Math.sin(x / (w * 0.22f) - t / 700.0) * 8 * dp * (x / w);
                    shape.lineTo(x, top + (s + 1) * band + wave);
                }
                shape.close();
                fill.setColor(alpha(s == 1 ? v.glow : v.ink, s == 1 ? 0x22 : 0x12));
                c.drawPath(shape, fill);
            }
            // Twelve crosses in a ring, every nation the same.
            float cx = w * 0.8f, cy = h * 0.3f, rr = w * 0.3f;
            double turn = (t % 240_000) / 240_000.0 * Math.PI * 2;
            fill.setColor(alpha(v.glow, 0x38));
            for (int i = 0; i < 12; i++) {
                double a = turn + i * Math.PI / 6;
                float x = cx + (float) (rr * Math.cos(a)), y = cy + (float) (rr * Math.sin(a));
                float arm = 7 * dp, half = 2 * dp;
                c.drawRect(x - half, y - arm, x + half, y + arm, fill);
                c.drawRect(x - arm, y - half, x + arm, y + half, fill);
            }
            // The loudspeaker, low on the left, and what comes out of it.
            float lx = w * 0.08f, ly = h * 0.58f;
            shape.reset();
            shape.moveTo(lx, ly - 8 * dp);
            shape.lineTo(lx + 22 * dp, ly - 22 * dp);
            shape.lineTo(lx + 22 * dp, ly + 22 * dp);
            shape.lineTo(lx, ly + 8 * dp);
            shape.close();
            fill.setColor(alpha(v.ink, 0x40));
            c.drawPath(shape, fill);
            line.setStrokeWidth(2 * dp);
            for (int k = 0; k < 4; k++) {
                float age = ((t + k * 500) % 2_000) / 2_000f;
                float r = 30 * dp + age * w * 0.5f;
                line.setColor(alpha(v.ink, Math.round(0x40 * (1 - age))));
                c.drawArc(lx - r + 22 * dp, ly - r, lx + r + 22 * dp, ly + r, -35, 70, false, line);
            }
        }

        // IRON SKY: stars, the moon and its craters, an orbit, an airship crossing.
        private void lunar(Canvas c, int w, int h, long t) {
            for (float[] s : scatter) {
                float tw = 0.5f + 0.5f * (float) Math.sin(t / 700.0 * (0.5 + s[2]) + s[3] * 6);
                fill.setColor(alpha(v.ink, Math.round(0x20 + 0x60 * tw * s[2])));
                c.drawCircle(s[0], s[1] * 0.7f, (0.6f + s[2]) * dp, fill);
            }
            float mx = w * 0.78f, my = h * 0.17f, mr = w * 0.34f;
            fill.setShader(new RadialGradient(mx - mr * 0.3f, my - mr * 0.3f, mr * 1.3f,
                    alpha(v.ink, 0x30), alpha(v.ink, 0x08), Shader.TileMode.CLAMP));
            c.drawCircle(mx, my, mr, fill);
            fill.setShader(null);
            line.setStrokeWidth(1.5f * dp);
            line.setColor(alpha(v.ink, 0x26));
            float[][] craters = {{-0.3f, -0.2f, 0.16f}, {0.25f, 0.1f, 0.22f}, {-0.1f, 0.4f, 0.1f}, {0.4f, -0.4f, 0.08f}, {-0.5f, 0.25f, 0.07f}};
            for (float[] k : craters) c.drawCircle(mx + k[0] * mr, my + k[1] * mr, k[2] * mr, line);
            // The dark side, turned away.
            fill.setColor(alpha(v.bg, 0x80));
            c.drawCircle(mx + mr * 0.45f, my + mr * 0.2f, mr * 0.9f, fill);
            // An orbit, dashed, with something on it.
            c.save();
            c.translate(mx, my);
            c.rotate(-18);
            dash.setColor(alpha(v.glow, 0x40));
            dash.setStrokeWidth(2 * dp);
            c.drawOval(-mr * 1.5f, -mr * 0.4f, mr * 1.5f, mr * 0.4f, dash);
            double a = (t % 30_000) / 30_000.0 * Math.PI * 2;
            fill.setColor(alpha(v.glow, 0xA0));
            c.drawCircle((float) (mr * 1.5f * Math.cos(a)), (float) (mr * 0.4f * Math.sin(a)), 3 * dp, fill);
            c.restore();
            // The airship, a minute to cross.
            float p = (t % 60_000) / 60_000f;
            float ax = w * 1.2f - p * w * 1.6f, ay = h * 0.42f, len = w * 0.32f;
            fill.setColor(alpha(v.accent, 0x34));
            c.drawOval(ax, ay - len * 0.13f, ax + len, ay + len * 0.13f, fill);
            shape.reset();
            shape.moveTo(ax + len * 0.86f, ay);
            shape.lineTo(ax + len * 1.04f, ay - len * 0.16f);
            shape.lineTo(ax + len * 1.04f, ay + len * 0.16f);
            shape.close();
            c.drawPath(shape, fill);
            c.drawRect(ax + len * 0.38f, ay + len * 0.12f, ax + len * 0.56f, ay + len * 0.18f, fill);
        }

        // SPECTRE: the wheel, the falling data and the grid running into the future.
        private void network(Canvas c, int w, int h, long t) {
            float cx = w * 0.84f, cy = h * 0.16f;
            bigEmblem(c, cx, cy, t, 0x38);
            c.save();
            c.translate(cx, cy);
            c.rotate(-(t % 120_000) / 333f);
            dash.setColor(alpha(v.glow, 0x30));
            dash.setStrokeWidth(6 * dp);
            c.drawCircle(0, 0, radius * 0.66f, dash);
            c.restore();
            rain(c, w, h, t, v.accent, v.glow, h * 0.6f);
            grid(c, w, h, t);
        }

        // ZARATHUSTRA: the sun rises behind the ridge, its rays turn, the ring returns for ever.
        private void dawn(Canvas c, int w, int h, long t) {
            float horizon = h * 0.6f, cx = w / 2f;
            float rise = (float) (0.5 + 0.5 * Math.sin(t / 9_000.0)) * h * 0.04f;
            float sy = horizon - rise;
            fill.setShader(new RadialGradient(cx, sy, w * 0.9f,
                    new int[]{alpha(v.glow, 0x50), alpha(v.accent, 0x20), 0}, new float[]{0f, 0.35f, 1f},
                    Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, horizon, fill);
            fill.setShader(null);
            c.save();
            c.translate(cx, sy);
            c.rotate((t % 360_000) / 1000f);
            fill.setColor(alpha(v.glow, 0x16));
            for (int i = 0; i < 24; i++) {
                c.rotate(15);
                shape.reset();
                shape.moveTo(-w * 0.012f, 0);
                shape.lineTo(0, -h);
                shape.lineTo(w * 0.012f, 0);
                shape.close();
                c.drawPath(shape, fill);
            }
            c.restore();
            fill.setColor(alpha(v.glow, 0x70));
            c.drawCircle(cx, sy, w * 0.13f, fill);
            // The ridge in front of the sun.
            shape.reset();
            shape.moveTo(0, h);
            for (int i = 0; i < ridge.length; i++) {
                float x = w * i / (ridge.length - 1f);
                float peak = (float) Math.exp(-Math.pow((i - ridge.length / 2.5f) / 4f, 2));
                shape.lineTo(x, horizon - (ridge[i] * 0.04f + peak * 0.1f) * h);
            }
            shape.lineTo(w, h);
            shape.close();
            fill.setColor(v.bg);
            c.drawPath(shape, fill);
            line.setColor(alpha(v.accent, 0x50));
            line.setStrokeWidth(1.5f * dp);
            c.drawPath(shape, line);
            // The eternal return: a ring, and an arrow that keeps coming back round.
            float rr = w * 0.42f, ry = h * 0.3f;
            line.setColor(alpha(v.ink, 0x22));
            line.setStrokeWidth(dp);
            c.drawCircle(cx, ry, rr, line);
            double a = (t % 20_000) / 20_000.0 * Math.PI * 2;
            float ax = cx + (float) (rr * Math.cos(a)), ay = ry + (float) (rr * Math.sin(a));
            c.save();
            c.translate(ax, ay);
            c.rotate((float) Math.toDegrees(a) + 90);
            shape.reset();
            shape.moveTo(8 * dp, 0);
            shape.lineTo(-6 * dp, -6 * dp);
            shape.lineTo(-6 * dp, 6 * dp);
            shape.close();
            fill.setColor(alpha(v.glow, 0x90));
            c.drawPath(shape, fill);
            c.restore();
        }

        // SOUND OF MUSIC: the Alps in outline, a stave carrying a tune across, flowers in the meadow.
        private void alpine(Canvas c, int w, int h, long t) {
            float base = h * 0.66f;
            float[][] ranges = {{0.36f, 0.5f, 0.44f, 0.56f}, {0.46f, 0.6f, 0.52f, 0.58f}};
            for (int k = 0; k < ranges.length; k++) {
                float[] r = ranges[k];
                shape.reset();
                shape.moveTo(0, base);
                shape.lineTo(0, h * r[3]);
                shape.lineTo(w * 0.22f, h * r[0]);
                shape.lineTo(w * 0.4f, h * r[2]);
                shape.lineTo(w * 0.62f, h * (r[0] - 0.06f));
                shape.lineTo(w * 0.8f, h * r[1]);
                shape.lineTo(w, h * r[2]);
                shape.lineTo(w, base);
                shape.close();
                fill.setColor(alpha(v.glow, k == 0 ? 0x16 : 0x24));
                c.drawPath(shape, fill);
                line.setColor(alpha(v.glow, 0x50));
                line.setStrokeWidth(1.5f * dp);
                c.drawPath(shape, line);
            }
            // The stave, and a tune of the state's own scrolling along it.
            float staff = h * 0.24f, gap = 7 * dp;
            line.setColor(alpha(v.ink, 0x26));
            line.setStrokeWidth(Math.max(1f, dp));
            for (int i = 0; i < 5; i++) c.drawLine(0, staff + i * gap, w, staff + i * gap, line);
            float step = 34 * dp, scroll = (t / 1000f * 24 * dp) % step;
            long first = (long) Math.floor(t / 1000f * 24 * dp / step);
            for (int i = -1; i < w / step + 2; i++) {
                long n = first + i;
                int pitch = (int) Math.floorMod(n * 5 + (n / 4) * 3, 9);
                float x = w - (i * step - scroll) - step;
                float y = staff + 4 * gap - pitch * gap / 2f;
                fill.setColor(alpha(n % 8 == 0 ? v.accent : v.ink, 0x50));
                c.save();
                c.rotate(-20, x, y);
                c.drawOval(x - 5 * dp, y - 3.5f * dp, x + 5 * dp, y + 3.5f * dp, fill);
                c.restore();
                line.setColor(alpha(v.ink, 0x50));
                c.drawLine(x + 4.5f * dp, y, x + 4.5f * dp, y - 24 * dp, line);
            }
            // Flowers, slowly turning.
            for (int i = 0; i < 7; i++) {
                float[] s = scatter[i];
                float x = s[0], y = base + (h - base) * (0.1f + 0.85f * s[1] / h * 1f);
                c.save();
                c.translate(x, y);
                c.rotate((t / 80f + s[3] * 360) % 360);
                emblemFlower(c, (6 + 6 * s[2]) * dp);
                c.restore();
            }
        }

        private void emblemFlower(Canvas c, float r) {
            Variant flower = v;
            emblem(shape, 0, 0, r, flower);
            fill.setColor(alpha(v.glow, 0x30));
            c.drawPath(shape, fill);
            fill.setColor(alpha(v.accent, 0x50));
            c.drawCircle(0, 0, r * 0.22f, fill);
        }

        /** Falling glyphs over the upper part, thinning out towards the list. */
        private void rain(Canvas c, int w, int h, long t, int body, int head, float limit) {
            float step = glyph.getTextSize() * 1.1f, span = limit + step * 10;
            float colW = w / (float) heads.length;
            for (int i = 0; i < heads.length; i++) {
                float y0 = (heads[i] + speeds[i] * t / 1000f) % span - step * 2;
                for (int k = 0; k < 9; k++) {
                    float y = y0 - k * step;
                    if (y < 0 || y > limit) continue;
                    float fade = 1 - y / limit;
                    int a = Math.round((k == 0 ? 0x70 : 0x46 - k * 5) * fade);
                    glyph.setColor(alpha(k == 0 ? head : body, Math.max(0, a)));
                    char g = glyphs.charAt(Math.floorMod(i * 7 + k * 3 + (int) (t / 160), glyphs.length()));
                    c.drawText(String.valueOf(g), (i + 0.5f) * colW, y, glyph);
                }
            }
        }

        /** The grid: the horizon two thirds down, its lines running towards the citizen. */
        private void grid(Canvas c, int w, int h, long t) {
            float horizon = h * 0.66f, depth = h - horizon;
            line.setStrokeWidth(Math.max(1f, dp));
            float phase = (t % 2400) / 2400f;
            for (int n = 0; n < 12; n++) {
                float z = (n + phase) / 12f;
                float y = horizon + depth * z * z;
                line.setColor(alpha(v.glow, Math.round(0x40 * z)));
                c.drawLine(0, y, w, y, line);
            }
            line.setColor(alpha(v.glow, 0x22));
            for (int k = -8; k <= 8; k++) {
                c.drawLine(w / 2f + k * w * 0.03f, horizon, w / 2f + k * w * 0.2f, h, line);
            }
        }
    }

    // ---- CRT ----------------------------------------------------------------

    /** Over everything: scanlines, a vignette, the refresh band rolling down and the tears of a glitch. */
    static final class Crt extends Fx.FxView {
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
                    new int[]{0, alpha(v.glow, 0x12), 0}, null, Shader.TileMode.CLAMP));
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
                tear.setColor(alpha(i % 2 == 0 ? v.accent : v.glow, 0x50 + r.nextInt(0x50)));
                c.drawRect(x, top, x + w * (0.3f + r.nextFloat() * 0.7f), top + height, tear);
            }
            if (r.nextInt(3) == 0) c.drawColor(alpha(v.glow, 0x0E));
        }
    }

    // ---- Glitching type -----------------------------------------------------

    /** Text that splits into the uniform's two neons and tears into slices whenever the state glitches. */
    static final class GlitchText extends TextView {
        private final Paint left = new Paint();
        private final Paint right = new Paint();
        private final float dp;

        GlitchText(Context context, Variant v) {
            super(context);
            dp = context.getResources().getDisplayMetrics().density;
            left.setColorFilter(new PorterDuffColorFilter(v.glow, PorterDuff.Mode.SRC_IN));
            right.setColorFilter(new PorterDuffColorFilter(v.accent, PorterDuff.Mode.SRC_IN));
        }

        @Override
        public void onVisibilityAggregated(boolean visible) {
            super.onVisibilityAggregated(visible);
            Fx.glitch(this, visible && Fx.enabled(getContext()));
        }

        @Override
        protected void onDetachedFromWindow() {
            Fx.glitch(this, false);
            super.onDetachedFromWindow();
        }

        @Override
        protected void onDraw(Canvas c) {
            if (!Fx.glitching()) {
                super.onDraw(c);
                return;
            }
            Random r = new Random(Fx.seed ^ System.identityHashCode(this) ^ (Fx.now() / 50));
            float dx = (2 + r.nextInt(6)) * dp * (1 - Fx.glitchProgress() * 0.6f);
            c.saveLayer(null, left);
            c.translate(-dx, 0);
            super.onDraw(c);
            c.restore();
            c.saveLayer(null, right);
            c.translate(dx, (r.nextInt(3) - 1) * dp);
            super.onDraw(c);
            c.restore();
            super.onDraw(c);
            int w = getWidth(), h = getHeight();
            int slices = 1 + r.nextInt(3);
            for (int i = 0; i < slices; i++) {
                float top = r.nextFloat() * h, height = h * (0.06f + r.nextFloat() * 0.2f);
                c.save();
                c.clipRect(0, top, w, top + height);
                c.translate((r.nextFloat() - 0.5f) * w * 0.14f, 0);
                super.onDraw(c);
                c.restore();
            }
        }
    }

    // ---- Ticker -------------------------------------------------------------

    /** A band of broadcasts running right to left, endlessly. */
    static final class Ticker extends Fx.FxView {
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

    // ---- Pulse --------------------------------------------------------------

    /** The citizen's heartbeat as the state assumes it: an ECG trace running across. */
    static final class Pulse extends Fx.FxView {
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
            halo.setColor(alpha(v.accent, 0x40));
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

    // ---- Eye ----------------------------------------------------------------

    /** The eye of the state, looking around and blinking now and then; a red light says it records. */
    static final class Eye extends Fx.FxView {
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
            paint.setColor(alpha(v.ink, 0x20));
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

    // ---- Boot ---------------------------------------------------------------

    /**
     * The operating system of the state coming up: lines typed one after the
     * other with their verdicts, then the whole screen glitches away. A tap
     * skips it.
     */
    static final class Boot extends Fx.FxView {
        private static final long LINE = 230, TYPE = 170, HOLD = 450, OUT = 320;
        private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint verdict = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint wheelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path wheel = new Path();
        private String[][] lines = new String[0][];
        private long start;
        private boolean glitched;

        Boot(Context context, Variant v) {
            super(context, v);
            Typeface mono = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD);
            text.setTypeface(mono);
            text.setTextSize(12 * dp);
            text.setColor(v.ink);
            verdict.set(text);
            verdict.setColor(v.accent);
            wheelPaint.setStyle(Paint.Style.STROKE);
            wheelPaint.setStrokeWidth(3 * dp);
            setVisibility(GONE);
            setClickable(true);
            setOnClickListener(view -> finish());
        }

        /** Runs the sequence; each line is {text, verdict}. */
        void run(String[][] lines) {
            this.lines = lines;
            start = Fx.now();
            glitched = false;
            setAlpha(1f);
            setVisibility(VISIBLE);
        }

        private long length() {
            return lines.length * LINE + HOLD + OUT;
        }

        private void finish() {
            setVisibility(GONE);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            emblem(wheel, w / 2f, h * 0.7f, w * 0.28f, v);
        }

        @Override
        protected void onDraw(Canvas c) {
            long t = Fx.now() - start;
            if (t > length()) {
                post(this::finish);
                return;
            }
            long out = t - lines.length * LINE - HOLD;
            if (out > 0 && !glitched) {
                glitched = true;
                Fx.glitchNow(OUT);
            }
            setAlpha(out > 0 ? 1 - out / (float) OUT : 1f);
            c.drawColor(v.bg);
            int w = getWidth(), h = getHeight();

            c.save();
            c.rotate(t / 12f, w / 2f, h * 0.7f);
            wheelPaint.setColor(alpha(v.accent, 0x60));
            c.drawPath(wheel, wheelPaint);
            c.restore();

            float margin = 24 * dp, y = h * 0.2f, lineH = text.getTextSize() * 1.9f;
            int columns = Math.max(20, (int) ((w - 2 * margin) / text.measureText("M")));
            for (int i = 0; i < lines.length; i++) {
                long since = t - i * LINE;
                if (since < 0) break;
                String body = lines[i][0], verdictText = lines[i][1];
                String row = body;
                if (!verdictText.isEmpty()) {
                    int dots = Math.max(2, columns - body.length() - verdictText.length() - 2);
                    row = body + " " + ".".repeat(dots) + " ";
                }
                int shown = (int) Math.min(row.length(), row.length() * since / TYPE);
                c.drawText(row, 0, shown, margin, y, text);
                if (shown == row.length() && !verdictText.isEmpty()) {
                    c.drawText(verdictText, margin + text.measureText(row), y, verdict);
                } else if (since < TYPE + LINE && (t / 120) % 2 == 0) {
                    float x = margin + text.measureText(row, 0, shown);
                    c.drawRect(x, y + text.ascent(), x + text.measureText("M"), y + text.descent(), verdict);
                }
                y += lineH;
            }
        }
    }
}
