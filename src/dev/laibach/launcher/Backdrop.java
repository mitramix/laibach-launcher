package dev.laibach.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import java.util.Random;

/**
 * Behind the home screen, the scene of the uniform's album: banners in a nave,
 * the market's chart, an anthem's broadcast, the moon, the network, a temple
 * on the mountain, a techno tunnel, the sun rising again, the singing Alps.
 */
final class Backdrop extends Fx.FxView {
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
        Cyber.emblem(wheel, 0, 0, radius, v);
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
        line.setColor(Cyber.alpha(v.accent, a));
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
            fill.setColor(Cyber.alpha(i == banners / 2 ? v.accent : v.ink, i == banners / 2 ? 0x30 : 0x10));
            c.drawPath(shape, fill);
            // A cross on each, cut out of the cloth.
            fill.setColor(Cyber.alpha(v.bg, 0x90));
            float cx = x + bw / 2, cy = len * 0.3f, arm = bw * 0.26f, half = bw * 0.07f;
            c.drawRect(cx - half, cy - arm, cx + half, cy + arm, fill);
            c.drawRect(cx - arm, cy - half, cx + arm, cy + half, fill);
        }
        // The bell: rings going out from the top of the nave, one toll every five seconds.
        line.setStrokeWidth(2 * dp);
        for (int k = 0; k < 3; k++) {
            float age = ((t + k * 600) % 5_000) / 5_000f;
            line.setColor(Cyber.alpha(v.ink, Math.round(0x40 * (1 - age))));
            c.drawCircle(w / 2f, h * 0.18f, w * (0.1f + age * 0.9f), line);
        }
        // Ash.
        for (float[] s : scatter) {
            float y = (s[1] + t / 1000f * (10 + 20 * s[2]) * dp) % h;
            float x = s[0] + (float) Math.sin(t / 1500.0 + s[3] * 6) * 6 * dp;
            fill.setColor(Cyber.alpha(v.ink, Math.round(0x18 + 0x30 * s[2])));
            c.drawCircle(x, y, (0.8f + 1.4f * s[2]) * dp, fill);
        }
    }

    // NOVA AKROPOLA: a temple's columns before the mountain, the sun crossing above.
    private void temple(Canvas c, int w, int h, long t) {
        float base = h * 0.66f;
        float sun = (t % 240_000) / 240_000f;
        float sx = w * (0.1f + 0.8f * sun), sy = h * (0.32f - 0.2f * (float) Math.sin(Math.PI * sun));
        fill.setShader(new RadialGradient(sx, sy, w * 0.3f, Cyber.alpha(v.glow, 0x40), 0, Shader.TileMode.CLAMP));
        c.drawCircle(sx, sy, w * 0.3f, fill);
        fill.setShader(null);
        fill.setColor(Cyber.alpha(v.glow, 0x50));
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
        fill.setColor(Cyber.alpha(v.ink, 0x12));
        c.drawPath(shape, fill);
        line.setColor(Cyber.alpha(v.ink, 0x30));
        line.setStrokeWidth(1.5f * dp);
        c.drawPath(shape, line);
        // The temple, in outline: pediment, architrave, seven fluted columns, steps.
        float left = w * 0.1f, right = w * 0.9f, top = base - h * 0.05f;
        shape.reset();
        shape.moveTo(left - 8 * dp, top);
        shape.lineTo(w / 2f, top - h * 0.07f);
        shape.lineTo(right + 8 * dp, top);
        shape.close();
        fill.setColor(Cyber.alpha(v.accent, 0x30));
        c.drawPath(shape, fill);
        c.drawRect(left - 8 * dp, top, right + 8 * dp, top + 8 * dp, fill);
        line.setColor(Cyber.alpha(v.ink, 0x26));
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
        line.setColor(Cyber.alpha(v.accent, 0x70));
        line.setStrokeWidth(2 * dp);
        c.drawPath(shape, line);
        shape.lineTo(w, mid + amp * 1.6f);
        shape.lineTo(0, mid + amp * 1.6f);
        shape.close();
        fill.setColor(Cyber.alpha(v.accent, 0x10));
        c.drawPath(shape, fill);
        // Volume: one bar a step, against the same tape.
        float barW = w / (float) steps;
        for (int i = 0; i < steps; i++) {
            double k = Math.floor((i * barW + scroll) / barW);
            float vol = (float) Math.abs(Math.sin(k * 12.9898) * 43758.5453 % 1);
            fill.setColor(Cyber.alpha(v.ink, 0x14));
            c.drawRect(i * barW + 1, h * 0.62f - vol * h * 0.06f, (i + 1) * barW - 1, h * 0.62f, fill);
        }
        // The barcode of the citizen as merchandise.
        Random r = new Random(1992);
        float x = w * 0.58f;
        fill.setColor(Cyber.alpha(v.ink, 0x2A));
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
            line.setColor(Cyber.alpha(k % 3 == 0 ? v.accent : v.ink, Math.round(0x48 * z)));
            c.drawRect(cx - s / 2, cy - s / 2, cx + s / 2, cy + s / 2, line);
        }
        int bars = 32;
        float bw = w / (float) bars, floor = h * 0.98f;
        long bar = t / 469;
        for (int i = 0; i < bars; i++) {
            float level = (float) Math.abs(Math.sin((bar * 31 + i * 7) * 12.9898) * 43758.5453 % 1);
            level *= 1 - beat * 0.6f;
            fill.setColor(Cyber.alpha(i % 8 == 0 ? v.accent : v.ink, 0x22));
            c.drawRect(i * bw + dp, floor - level * h * 0.12f, (i + 1) * bw - dp, floor, fill);
        }
        // The clock that everyone is on: a hand, one turn a bar.
        line.setColor(Cyber.alpha(v.ink, 0x30));
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
            fill.setColor(Cyber.alpha(s == 1 ? v.glow : v.ink, s == 1 ? 0x22 : 0x12));
            c.drawPath(shape, fill);
        }
        // Twelve crosses in a ring, every nation the same.
        float cx = w * 0.8f, cy = h * 0.3f, rr = w * 0.3f;
        double turn = (t % 240_000) / 240_000.0 * Math.PI * 2;
        fill.setColor(Cyber.alpha(v.glow, 0x38));
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
        fill.setColor(Cyber.alpha(v.ink, 0x40));
        c.drawPath(shape, fill);
        line.setStrokeWidth(2 * dp);
        for (int k = 0; k < 4; k++) {
            float age = ((t + k * 500) % 2_000) / 2_000f;
            float r = 30 * dp + age * w * 0.5f;
            line.setColor(Cyber.alpha(v.ink, Math.round(0x40 * (1 - age))));
            c.drawArc(lx - r + 22 * dp, ly - r, lx + r + 22 * dp, ly + r, -35, 70, false, line);
        }
    }

    // IRON SKY: stars, the moon and its craters, an orbit, an airship crossing.
    private void lunar(Canvas c, int w, int h, long t) {
        for (float[] s : scatter) {
            float tw = 0.5f + 0.5f * (float) Math.sin(t / 700.0 * (0.5 + s[2]) + s[3] * 6);
            fill.setColor(Cyber.alpha(v.ink, Math.round(0x20 + 0x60 * tw * s[2])));
            c.drawCircle(s[0], s[1] * 0.7f, (0.6f + s[2]) * dp, fill);
        }
        float mx = w * 0.78f, my = h * 0.17f, mr = w * 0.34f;
        fill.setShader(new RadialGradient(mx - mr * 0.3f, my - mr * 0.3f, mr * 1.3f,
                Cyber.alpha(v.ink, 0x30), Cyber.alpha(v.ink, 0x08), Shader.TileMode.CLAMP));
        c.drawCircle(mx, my, mr, fill);
        fill.setShader(null);
        line.setStrokeWidth(1.5f * dp);
        line.setColor(Cyber.alpha(v.ink, 0x26));
        float[][] craters = {{-0.3f, -0.2f, 0.16f}, {0.25f, 0.1f, 0.22f}, {-0.1f, 0.4f, 0.1f}, {0.4f, -0.4f, 0.08f}, {-0.5f, 0.25f, 0.07f}};
        for (float[] k : craters) c.drawCircle(mx + k[0] * mr, my + k[1] * mr, k[2] * mr, line);
        // The dark side, turned away.
        fill.setColor(Cyber.alpha(v.bg, 0x80));
        c.drawCircle(mx + mr * 0.45f, my + mr * 0.2f, mr * 0.9f, fill);
        // An orbit, dashed, with something on it.
        c.save();
        c.translate(mx, my);
        c.rotate(-18);
        dash.setColor(Cyber.alpha(v.glow, 0x40));
        dash.setStrokeWidth(2 * dp);
        c.drawOval(-mr * 1.5f, -mr * 0.4f, mr * 1.5f, mr * 0.4f, dash);
        double a = (t % 30_000) / 30_000.0 * Math.PI * 2;
        fill.setColor(Cyber.alpha(v.glow, 0xA0));
        c.drawCircle((float) (mr * 1.5f * Math.cos(a)), (float) (mr * 0.4f * Math.sin(a)), 3 * dp, fill);
        c.restore();
        // The airship, a minute to cross.
        float p = (t % 60_000) / 60_000f;
        float ax = w * 1.2f - p * w * 1.6f, ay = h * 0.42f, len = w * 0.32f;
        fill.setColor(Cyber.alpha(v.accent, 0x34));
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
        dash.setColor(Cyber.alpha(v.glow, 0x30));
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
                new int[]{Cyber.alpha(v.glow, 0x50), Cyber.alpha(v.accent, 0x20), 0}, new float[]{0f, 0.35f, 1f},
                Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, horizon, fill);
        fill.setShader(null);
        c.save();
        c.translate(cx, sy);
        c.rotate((t % 360_000) / 1000f);
        fill.setColor(Cyber.alpha(v.glow, 0x16));
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
        fill.setColor(Cyber.alpha(v.glow, 0x70));
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
        line.setColor(Cyber.alpha(v.accent, 0x50));
        line.setStrokeWidth(1.5f * dp);
        c.drawPath(shape, line);
        // The eternal return: a ring, and an arrow that keeps coming back round.
        float rr = w * 0.42f, ry = h * 0.3f;
        line.setColor(Cyber.alpha(v.ink, 0x22));
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
        fill.setColor(Cyber.alpha(v.glow, 0x90));
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
            fill.setColor(Cyber.alpha(v.glow, k == 0 ? 0x16 : 0x24));
            c.drawPath(shape, fill);
            line.setColor(Cyber.alpha(v.glow, 0x50));
            line.setStrokeWidth(1.5f * dp);
            c.drawPath(shape, line);
        }
        // The stave, and a tune of the state's own scrolling along it.
        float staff = h * 0.24f, gap = 7 * dp;
        line.setColor(Cyber.alpha(v.ink, 0x26));
        line.setStrokeWidth(Math.max(1f, dp));
        for (int i = 0; i < 5; i++) c.drawLine(0, staff + i * gap, w, staff + i * gap, line);
        float step = 34 * dp, scroll = (t / 1000f * 24 * dp) % step;
        long first = (long) Math.floor(t / 1000f * 24 * dp / step);
        for (int i = -1; i < w / step + 2; i++) {
            long n = first + i;
            int pitch = (int) Math.floorMod(n * 5 + (n / 4) * 3, 9);
            float x = w - (i * step - scroll) - step;
            float y = staff + 4 * gap - pitch * gap / 2f;
            fill.setColor(Cyber.alpha(n % 8 == 0 ? v.accent : v.ink, 0x50));
            c.save();
            c.rotate(-20, x, y);
            c.drawOval(x - 5 * dp, y - 3.5f * dp, x + 5 * dp, y + 3.5f * dp, fill);
            c.restore();
            line.setColor(Cyber.alpha(v.ink, 0x50));
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
        Cyber.emblem(shape, 0, 0, r, flower);
        fill.setColor(Cyber.alpha(v.glow, 0x30));
        c.drawPath(shape, fill);
        fill.setColor(Cyber.alpha(v.accent, 0x50));
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
                glyph.setColor(Cyber.alpha(k == 0 ? head : body, Math.max(0, a)));
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
            line.setColor(Cyber.alpha(v.glow, Math.round(0x40 * z)));
            c.drawLine(0, y, w, y, line);
        }
        line.setColor(Cyber.alpha(v.glow, 0x22));
        for (int k = -8; k <= 8; k++) {
            c.drawLine(w / 2f + k * w * 0.03f, horizon, w / 2f + k * w * 0.2f, h, line);
        }
    }
}
