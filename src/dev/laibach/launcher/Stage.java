package dev.laibach.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import java.util.Random;

/**
 * The stage the backdrop's scenes are played on: the shared paints, a fixed
 * scatter of stars, ash and notes, and the props several scenes share (the
 * turning emblem, the emblem in flower, falling glyphs and the grid).
 */
abstract class Stage extends Fx.FxView {
    final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Paint dash = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Paint glyph = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Path wheel = new Path();
    final Path shape = new Path();
    final String glyphs;
    float radius;
    float[] heads = new float[0];
    float[] speeds = new float[0];
    /** Fixed scatter for stars, ash, notes and the like: x, y, size, phase. */
    float[][] scatter = new float[0][];
    float[] ridge = new float[0];

    Stage(Context context, Variant v) {
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

    /** The emblem, huge, turning slowly in a corner; most scenes keep it. */
    void bigEmblem(Canvas c, float cx, float cy, long t, int a) {
        c.save();
        c.translate(cx, cy);
        c.rotate((t % 180_000) / 500f);
        line.setColor(Cyber.alpha(v.accent, a));
        line.setStrokeWidth(2 * dp);
        c.drawPath(wheel, line);
        c.drawCircle(0, 0, radius * 0.5f, line);
        c.restore();
    }

    void emblemFlower(Canvas c, float r) {
        Variant flower = v;
        Cyber.emblem(shape, 0, 0, r, flower);
        fill.setColor(Cyber.alpha(v.glow, 0x30));
        c.drawPath(shape, fill);
        fill.setColor(Cyber.alpha(v.accent, 0x50));
        c.drawCircle(0, 0, r * 0.22f, fill);
    }

    /** Falling glyphs over the upper part, thinning out towards the list. */
    void rain(Canvas c, int w, int h, long t, int body, int head, float limit) {
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
    void grid(Canvas c, int w, int h, long t) {
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
