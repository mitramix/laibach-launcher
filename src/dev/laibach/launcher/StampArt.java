package dev.laibach.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.view.View;

/**
 * The glyphs the state's stamps and medals bear, drawn in code, and the two
 * views that carry them: a perforated stamp for the album and a medal on its
 * ribbon for the ceremony.
 */
final class StampArt {
    static final int CHECK = 0, SEVEN = 1, THIRTY = 2, EYE = 3, STAR = 4, MOON = 5, KEY = 6, CROSS = 7,
            ARROW = 8, BARS = 9, SUN = 10, GEAR = 11, EMBLEM = 12, CLOCK = 13, WAVES = 14, LINK = 15, FLAG = 16;

    private StampArt() {}

    /** Draws a glyph centred on (cx, cy) within radius r, in the paint's colour. */
    static void glyph(Canvas c, int glyph, float cx, float cy, float r, Paint fill, Variant v) {
        Paint stroke = new Paint(fill);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(r * 0.16f);
        stroke.setStrokeCap(Paint.Cap.SQUARE);
        stroke.setStrokeJoin(Paint.Join.MITER);
        Path p = new Path();
        switch (glyph) {
            case CHECK:
                p.moveTo(cx - r * 0.6f, cy);
                p.lineTo(cx - r * 0.15f, cy + r * 0.45f);
                p.lineTo(cx + r * 0.65f, cy - r * 0.5f);
                c.drawPath(p, stroke);
                break;
            case SEVEN:
            case THIRTY: {
                Paint text = new Paint(fill);
                text.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
                text.setTextAlign(Paint.Align.CENTER);
                text.setTextSize(r * (glyph == SEVEN ? 1.7f : 1.25f));
                c.drawText(glyph == SEVEN ? "7" : "30", cx, cy + text.getTextSize() * 0.36f, text);
                break;
            }
            case EYE:
                p.moveTo(cx - r * 0.95f, cy);
                p.quadTo(cx, cy - r * 0.95f, cx + r * 0.95f, cy);
                p.quadTo(cx, cy + r * 0.95f, cx - r * 0.95f, cy);
                stroke.setStrokeWidth(r * 0.12f);
                c.drawPath(p, stroke);
                c.drawCircle(cx, cy, r * 0.28f, fill);
                break;
            case STAR:
                Cyber.star(p, cx, cy, r * 0.95f, r * 0.4f, 5, 0);
                c.drawPath(p, fill);
                break;
            case MOON:
                p.addCircle(cx, cy, r * 0.75f, Path.Direction.CW);
                Path bite = new Path();
                bite.addCircle(cx + r * 0.38f, cy - r * 0.2f, r * 0.62f, Path.Direction.CW);
                p.op(bite, Path.Op.DIFFERENCE);
                c.drawPath(p, fill);
                break;
            case KEY:
                stroke.setStrokeWidth(r * 0.14f);
                c.drawCircle(cx - r * 0.4f, cy, r * 0.32f, stroke);
                c.drawLine(cx - r * 0.08f, cy, cx + r * 0.8f, cy, stroke);
                c.drawLine(cx + r * 0.5f, cy, cx + r * 0.5f, cy + r * 0.32f, stroke);
                c.drawLine(cx + r * 0.75f, cy, cx + r * 0.75f, cy + r * 0.25f, stroke);
                break;
            case CROSS:
                c.drawLine(cx - r * 0.55f, cy - r * 0.55f, cx + r * 0.55f, cy + r * 0.55f, stroke);
                c.drawLine(cx + r * 0.55f, cy - r * 0.55f, cx - r * 0.55f, cy + r * 0.55f, stroke);
                break;
            case ARROW:
                stroke.setStrokeWidth(r * 0.12f);
                c.drawRect(cx - r * 0.75f, cy - r * 0.6f, cx + r * 0.15f, cy + r * 0.6f, stroke);
                c.drawLine(cx - r * 0.3f, cy, cx + r * 0.85f, cy, stroke);
                p.moveTo(cx + r * 0.95f, cy);
                p.lineTo(cx + r * 0.55f, cy - r * 0.32f);
                p.lineTo(cx + r * 0.55f, cy + r * 0.32f);
                p.close();
                c.drawPath(p, fill);
                break;
            case BARS:
                for (int i = 0; i < 3; i++) {
                    float y = cy - r * 0.6f + i * r * 0.48f;
                    float w = i == 1 ? 0.55f : 0.8f;
                    c.drawRect(cx - r * 0.8f, y, cx - r * 0.8f + r * 1.6f * w, y + r * 0.3f, fill);
                }
                break;
            case SUN:
                c.drawCircle(cx, cy, r * 0.38f, fill);
                stroke.setStrokeWidth(r * 0.1f);
                for (int i = 0; i < 12; i++) {
                    double a = i * Math.PI / 6;
                    c.drawLine(cx + (float) (Math.cos(a) * r * 0.55f), cy + (float) (Math.sin(a) * r * 0.55f),
                            cx + (float) (Math.cos(a) * r * 0.9f), cy + (float) (Math.sin(a) * r * 0.9f), stroke);
                }
                break;
            case GEAR:
                Cyber.gear(p, cx, cy, r * 0.9f);
                p.addCircle(cx, cy, r * 0.3f, Path.Direction.CCW);
                p.setFillType(Path.FillType.EVEN_ODD);
                c.drawPath(p, fill);
                break;
            case CLOCK:
                stroke.setStrokeWidth(r * 0.12f);
                c.drawCircle(cx, cy, r * 0.78f, stroke);
                c.drawLine(cx, cy, cx, cy - r * 0.5f, stroke);
                c.drawLine(cx, cy, cx + r * 0.38f, cy, stroke);
                break;
            case WAVES:
                // The signal the passport answers to, never its name: three arcs and their source.
                stroke.setStrokeWidth(r * 0.12f);
                stroke.setStrokeCap(Paint.Cap.ROUND);
                for (int i = 1; i <= 3; i++) {
                    float a = r * 0.3f * i;
                    c.drawArc(cx - a, cy + r * 0.45f - a, cx + a, cy + r * 0.45f + a, 225, 90, false, stroke);
                }
                c.drawCircle(cx, cy + r * 0.45f, r * 0.12f, fill);
                break;
            case LINK:
                stroke.setStrokeWidth(r * 0.13f);
                c.drawCircle(cx - r * 0.3f, cy, r * 0.45f, stroke);
                c.drawCircle(cx + r * 0.3f, cy, r * 0.45f, stroke);
                break;
            case FLAG:
                stroke.setStrokeWidth(r * 0.12f);
                c.drawLine(cx - r * 0.6f, cy - r * 0.85f, cx - r * 0.6f, cy + r * 0.85f, stroke);
                p.moveTo(cx - r * 0.6f, cy - r * 0.8f);
                p.lineTo(cx + r * 0.8f, cy - r * 0.5f);
                p.lineTo(cx - r * 0.6f, cy - r * 0.1f);
                p.close();
                c.drawPath(p, fill);
                break;
            default:
                Cyber.emblem(p, cx, cy, r * 0.9f, v);
                c.drawPath(p, fill);
                break;
        }
    }

    /** A postage stamp with perforated edges: in the accent when issued, a dashed outline when not. */
    static final class StampView extends View {
        private final Variant v;
        private final int glyph;
        private final boolean issued;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        StampView(Context c, Variant v, int glyph, boolean issued) {
            super(c);
            this.v = v;
            this.glyph = glyph;
            this.issued = issued;
        }

        @Override
        protected void onMeasure(int w, int h) {
            int width = MeasureSpec.getSize(w);
            setMeasuredDimension(width, Math.round(width * 1.15f));
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight(), hole = w / 26f, inset = hole * 1.4f;
            paint.setStyle(Paint.Style.FILL);
            if (issued) {
                paint.setColor(v.accent);
                c.drawRect(0, 0, w, h, paint);
                // The perforation: holes punched along every edge.
                paint.setColor(v.bg);
                int across = Math.max(4, Math.round(w / (hole * 3)));
                int down = Math.max(4, Math.round(h / (hole * 3)));
                for (int i = 0; i <= across; i++) {
                    float x = w * i / across;
                    c.drawCircle(x, 0, hole, paint);
                    c.drawCircle(x, h, hole, paint);
                }
                for (int i = 0; i <= down; i++) {
                    float y = h * i / down;
                    c.drawCircle(0, y, hole, paint);
                    c.drawCircle(w, y, hole, paint);
                }
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(Math.max(1, hole * 0.35f));
                c.drawRect(inset * 1.6f, inset * 1.6f, w - inset * 1.6f, h - inset * 1.6f, paint);
                paint.setStyle(Paint.Style.FILL);
                glyph(c, glyph, w / 2, h / 2, w * 0.28f, paint, v);
            } else {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(Math.max(1, hole * 0.4f));
                paint.setColor(v.rule);
                paint.setPathEffect(new DashPathEffect(new float[]{hole * 1.5f, hole}, 0));
                c.drawRect(inset, inset, w - inset, h - inset, paint);
                paint.setPathEffect(null);
                paint.setStyle(Paint.Style.FILL);
                glyph(c, glyph, w / 2, h / 2, w * 0.22f, paint, v);
            }
        }
    }

    /** A medal on its ribbon: a striped ribbon, a sunburst, a disc and the glyph. */
    static final class MedalView extends View {
        private final Variant v;
        private final int glyph;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        float turn;

        MedalView(Context c, Variant v, int glyph) {
            super(c);
            this.v = v;
            this.glyph = glyph;
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight();
            float r = Math.min(w / 2, h * 0.36f), cx = w / 2, cy = h - r * 1.05f;
            // The ribbon, five stripes, narrowing into the medal's ring.
            float top = 0, bottom = cy - r * 0.6f, half = r * 0.55f;
            int[] stripes = {v.accent, v.ink, v.accent, v.ink, v.accent};
            for (int i = 0; i < stripes.length; i++) {
                float a = -half + 2 * half * i / stripes.length, b = -half + 2 * half * (i + 1) / stripes.length;
                path.reset();
                path.moveTo(cx + a, top);
                path.lineTo(cx + b, top);
                path.lineTo(cx + b * 0.5f, bottom);
                path.lineTo(cx + a * 0.5f, bottom);
                path.close();
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(stripes[i]);
                c.drawPath(path, paint);
            }
            // The sunburst, turning slowly.
            path.reset();
            Cyber.star(path, cx, cy, r, r * 0.78f, 24, turn);
            paint.setColor(v.accent);
            c.drawPath(path, paint);
            paint.setColor(v.bg);
            c.drawCircle(cx, cy, r * 0.72f, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(r * 0.05f);
            paint.setColor(v.ink);
            c.drawCircle(cx, cy, r * 0.64f, paint);
            paint.setStyle(Paint.Style.FILL);
            glyph(c, glyph, cx, cy, r * 0.42f, paint, v);
        }
    }
}
