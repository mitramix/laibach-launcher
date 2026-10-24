package dev.laibach.launcher;

import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.media.FaceDetector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The darkroom's censor: develops a photograph in the manner of the uniform,
 * blacks out the eyes the state prefers unseen, paints out whoever is no
 * longer with us, and frames the print with a stamp, the seal and a decree.
 * All of it is drawn here, on the phone. A print is written from pixels
 * alone, so it leaves without the photograph's metadata: no place, no time,
 * no make of camera.
 */
final class Censor {
    private Censor() {}

    /** A stroke of the retoucher's brush, in coordinates from 0 to 1 across the photograph. */
    static final class Stroke {
        final List<PointF> points = new ArrayList<>();
        /** The brush, as a share of the photograph's width. */
        final float width;

        Stroke(float width) {
            this.width = width;
        }
    }

    /** What the censor has marked on a photograph: bars and strokes, from 0 to 1, in the order made. */
    static final class Marks {
        final List<Object> all = new ArrayList<>();

        List<RectF> bars() {
            List<RectF> out = new ArrayList<>();
            for (Object o : all) if (o instanceof RectF) out.add((RectF) o);
            return out;
        }

        boolean empty() {
            return all.isEmpty();
        }

        /** A copy that later marks on the table leave alone. */
        Marks copy() {
            Marks to = new Marks();
            for (Object o : all) {
                if (o instanceof RectF) {
                    to.all.add(new RectF((RectF) o));
                } else {
                    Stroke s = (Stroke) o, c = new Stroke(s.width);
                    for (PointF pt : s.points) c.points.add(new PointF(pt.x, pt.y));
                    to.all.add(c);
                }
            }
            return to;
        }
    }

    // ---- Treatments ---------------------------------------------------------

    /** The name of the uniform's darkroom treatment, in English and German. */
    static String[] treatment(Variant v) {
        switch (v.name) {
            case "OPUS DEI": return new String[]{"WOODCUT", "HOLZSCHNITT"};
            case "NOVA AKROPOLA": return new String[]{"STONE", "STEIN"};
            case "KAPITAL": return new String[]{"NEWSPRINT", "ZEITUNGSDRUCK"};
            case "WAT": return new String[]{"TIMECODE", "ZEITCODE"};
            case "VOLK": return new String[]{"HALFTONE", "RASTERDRUCK"};
            case "IRON SKY": return new String[]{"LUNAR GRAIN", "MONDKORN"};
            case "SPECTRE": return new String[]{"SIGNAL", "SIGNAL"};
            case "ZARATHUSTRA": return new String[]{"DAWN", "MORGENROT"};
            case "SOUND OF MUSIC": return new String[]{"ALPINE", "ALPIN"};
            default: return new String[]{"STARK", "SCHROFF"};
        }
    }

    /** The light of the treatment: what the retoucher paints with. */
    static int paper(Variant v) {
        switch (v.name) {
            case "OPUS DEI": return 0xFFECE6D6;
            case "NOVA AKROPOLA": return 0xFFDCD8CE;
            case "KAPITAL": return 0xFFE8E2D0;
            case "VOLK": return 0xFFF2EBDD;
            case "IRON SKY": return 0xFFB9B6AC;
            case "SPECTRE": return 0xFFE4ECF5;
            case "ZARATHUSTRA": return 0xFFF1E6D0;
            case "SOUND OF MUSIC": return 0xFFEDF1E8;
            default: return 0xFFFFFFFF;
        }
    }

    /** Develops src in the uniform's manner into a new picture of the same size. */
    static Bitmap develop(Bitmap src, Variant v) {
        int w = src.getWidth(), h = src.getHeight();
        int[] px = new int[w * h];
        src.getPixels(px, 0, w, 0, 0, w, h);
        float[] lum = levels(px);
        if (v.name.equals("VOLK")) return halftone(lum, w, h);
        Random grain = new Random(w * 31L + h);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int i = y * w + x;
                float l = lum[i];
                switch (v.name) {
                    case "OPUS DEI": {
                        // Engraved lines, thicker where it is darker.
                        float period = Math.max(4f, w / 180f);
                        double line = 0.5 + 0.5 * Math.sin(2 * Math.PI * (y + 0.25 * x) / period);
                        px[i] = contrast(l, 1.3f) < 0.12 + 0.76 * line ? 0xFF0A0A0A : 0xFFECE6D6;
                        break;
                    }
                    case "NOVA AKROPOLA":
                        px[i] = ramp(vignette(contrast(l, 1.2f), x, y, w, h, 0.45f),
                                0xFF141414, 0xFF5A4630, 0xFFC2A66E, 0xFFDCD8CE);
                        break;
                    case "KAPITAL": {
                        // Ordered dither, as cheap paper takes it.
                        int cell = Math.max(1, w / 500);
                        int bx = (x / cell) & 3, by = (y / cell) & 3;
                        px[i] = contrast(l, 1.15f) > (BAYER[by * 4 + bx] + 0.5f) / 16f ? 0xFFE8E2D0 : 0xFF111111;
                        break;
                    }
                    case "WAT": {
                        float s = contrast(l, 1.8f);
                        if ((y / Math.max(2, h / 400)) % 4 == 0) s *= 0.55f;
                        px[i] = grey(s);
                        break;
                    }
                    case "IRON SKY":
                        px[i] = ramp(contrast(l, 1.15f) + (float) grain.nextGaussian() * 0.06f,
                                0xFF0D1117, 0xFF5E6672, 0xFFD7D2C4);
                        break;
                    case "SPECTRE": {
                        float s = contrast(l, 1.25f);
                        px[i] = s < 0.36f ? 0xFF05070B : s < 0.7f ? 0xFFFF2340 : 0xFFE4ECF5;
                        break;
                    }
                    case "ZARATHUSTRA":
                        px[i] = ramp(contrast(l, 1.1f), 0xFF141024, 0xFF6B2D5C, 0xFFE8752A, 0xFFFFC857, 0xFFF1E6D0);
                        break;
                    case "SOUND OF MUSIC":
                        px[i] = ramp(1 - vignette(1 - (0.15f + 0.8f * l), x, y, w, h, 0.5f),
                                0xFF1F2A24, 0xFF2E7D5B, 0xFFCBD3C7, 0xFFEDF1E8);
                        break;
                    default:
                        px[i] = l < 0.5f ? 0xFF000000 : 0xFFFFFFFF;
                }
            }
        }
        if (v.name.equals("SPECTRE")) tear(px, w, h, grain);
        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        out.setPixels(px, 0, w, 0, 0, w, h);
        return out;
    }

    private static final int[] BAYER = {0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5};

    /** The brightness of each pixel, stretched so the darkest and lightest 1% meet black and white. */
    private static float[] levels(int[] px) {
        float[] lum = new float[px.length];
        int[] histogram = new int[256];
        for (int i = 0; i < px.length; i++) {
            int c = px[i];
            int l = (299 * ((c >> 16) & 0xFF) + 587 * ((c >> 8) & 0xFF) + 114 * (c & 0xFF)) / 1000;
            histogram[l]++;
            lum[i] = l;
        }
        int lo = 0, hi = 255, seen = 0, cut = px.length / 100;
        while (lo < 254 && seen + histogram[lo] <= cut) seen += histogram[lo++];
        seen = 0;
        while (hi > lo + 1 && seen + histogram[hi] <= cut) seen += histogram[hi--];
        float span = Math.max(1, hi - lo);
        for (int i = 0; i < lum.length; i++) lum[i] = clamp((lum[i] - lo) / span);
        return lum;
    }

    /** Dots on paper, bigger where it is darker: the people's newspaper. */
    private static Bitmap halftone(float[] lum, int w, int h) {
        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(out);
        c.drawColor(0xFFF2EBDD);
        Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
        dot.setColor(0xFF2A0A0C);
        float cell = Math.max(6f, w / 110f);
        for (float y = cell / 2; y < h; y += cell) {
            for (float x = cell / 2; x < w; x += cell) {
                float dark = 1 - contrast(lum[Math.min(h - 1, (int) y) * w + Math.min(w - 1, (int) x)], 1.2f);
                if (dark > 0.04f) c.drawCircle(x, y, cell * 0.72f * (float) Math.sqrt(dark), dot);
            }
        }
        return out;
    }

    /** A few bands of the picture slip sideways, as a weak signal tears. */
    private static void tear(int[] px, int w, int h, Random r) {
        int[] row = new int[w];
        for (int band = 0; band < 7; band++) {
            int top = r.nextInt(h), height = Math.max(2, h / 60 + r.nextInt(h / 40 + 1));
            int shift = (r.nextBoolean() ? 1 : -1) * (w / 40 + r.nextInt(w / 25 + 1));
            for (int y = top; y < Math.min(h, top + height); y++) {
                System.arraycopy(px, y * w, row, 0, w);
                for (int x = 0; x < w; x++) px[y * w + x] = row[Math.floorMod(x - shift, w)];
            }
        }
    }

    private static float contrast(float l, float k) {
        return clamp((l - 0.5f) * k + 0.5f);
    }

    private static float vignette(float l, int x, int y, int w, int h, float strength) {
        float dx = (x - w / 2f) / (w / 2f), dy = (y - h / 2f) / (h / 2f);
        return l * (1 - strength * Math.min(1f, (dx * dx + dy * dy) / 2f));
    }

    private static float clamp(float f) {
        return f < 0 ? 0 : f > 1 ? 1 : f;
    }

    private static int grey(float s) {
        int g = Math.round(clamp(s) * 255);
        return 0xFF000000 | g << 16 | g << 8 | g;
    }

    /** The colour at t (0 to 1) along a ramp of evenly spaced colours. */
    private static int ramp(float t, int... stops) {
        float at = clamp(t) * (stops.length - 1);
        int i = Math.min(stops.length - 2, (int) at);
        return mix(stops[i], stops[i + 1], at - i);
    }

    private static int mix(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 0xFF) + (((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * t);
        int g = Math.round(((a >> 8) & 0xFF) + (((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * t);
        int bl = Math.round((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
        return 0xFF000000 | r << 16 | g << 8 | bl;
    }

    // ---- Faces --------------------------------------------------------------

    /** Bars over the eyes of every face Android's own face finder sees, from 0 to 1. Runs on the phone. */
    static List<RectF> eyes(Bitmap src) {
        List<RectF> bars = new ArrayList<>();
        float scale = Math.min(1f, 1024f / src.getWidth());
        int w = Math.round(src.getWidth() * scale) & ~1, h = Math.round(src.getHeight() * scale);
        if (w < 2 || h < 2) return bars;
        Bitmap small = Bitmap.createScaledBitmap(src, w, h, true).copy(Bitmap.Config.RGB_565, false);
        FaceDetector.Face[] faces = new FaceDetector.Face[12];
        int n = new FaceDetector(w, h, faces.length).findFaces(small, faces);
        small.recycle();
        PointF mid = new PointF();
        for (int i = 0; i < n; i++) {
            if (faces[i].confidence() < 0.3f) continue;
            faces[i].getMidPoint(mid);
            float eyes = faces[i].eyesDistance();
            bars.add(new RectF((mid.x - eyes * 1.25f) / w, (mid.y - eyes * 0.38f) / h,
                    (mid.x + eyes * 1.25f) / w, (mid.y + eyes * 0.38f) / h));
        }
        return bars;
    }

    // ---- The print ----------------------------------------------------------

    /** The size of the print around a developed photograph of w × h: a margin and a band for the decree. */
    static int[] size(int w, int h) {
        int margin = margin(w);
        return new int[]{w + 2 * margin, h + 2 * margin + band(w)};
    }

    /** The margin around a developed photograph w pixels wide. */
    static int margin(int w) {
        return Math.round(w * 0.04f);
    }

    private static int band(int w) {
        return Math.round(w * 0.12f);
    }

    /**
     * Draws the print: the developed photograph, the retoucher's strokes, the
     * bars, the stamp, and the band with the seal and the decree. Drawn in the
     * print's own pixels; scale the canvas to show it smaller.
     */
    static void draw(Canvas c, Bitmap developed, Marks marks, Variant v, String stamp, String decree, String foot) {
        int w = developed.getWidth(), h = developed.getHeight();
        int margin = margin(w);
        int[] size = size(w, h);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(v.bg);
        c.drawRect(0, 0, size[0], size[1], p);
        c.drawBitmap(developed, margin, margin, null);

        c.save();
        c.clipRect(margin, margin, margin + w, margin + h);
        c.translate(margin, margin);
        Paint brush = new Paint(Paint.ANTI_ALIAS_FLAG);
        brush.setStyle(Paint.Style.STROKE);
        brush.setStrokeCap(Paint.Cap.ROUND);
        brush.setStrokeJoin(Paint.Join.ROUND);
        brush.setColor(paper(v));
        Paint bar = new Paint();
        bar.setColor(0xFF000000);
        for (Object o : marks.all) {
            if (o instanceof RectF) {
                RectF r = (RectF) o;
                c.drawRect(r.left * w, r.top * h, r.right * w, r.bottom * h, bar);
            } else {
                Stroke s = (Stroke) o;
                if (s.points.isEmpty()) continue;
                float width = s.width * w;
                brush.setStrokeWidth(width);
                brush.setMaskFilter(new BlurMaskFilter(width * 0.3f, BlurMaskFilter.Blur.NORMAL));
                Path path = new Path();
                PointF first = s.points.get(0);
                path.moveTo(first.x * w, first.y * h);
                for (int i = 1; i < s.points.size(); i++) path.lineTo(s.points.get(i).x * w, s.points.get(i).y * h);
                if (s.points.size() == 1) path.lineTo(first.x * w + 0.1f, first.y * h);
                c.drawPath(path, brush);
            }
        }
        rubberStamp(c, stamp, w, h, v);
        c.restore();

        // The band: the seal, the decree and the foot line.
        float top = margin + h, bandH = size[1] - top;
        float r = bandH * 0.3f, cx = margin + r, cy = top + bandH / 2f;
        seal(c, cx, cy, r, v);
        Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        text.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        text.setLetterSpacing(0.08f);
        float left = cx + r * 1.6f, room = margin + w - left;
        text.setColor(v.accent);
        fit(text, decree, room, bandH * 0.2f);
        c.drawText(decree, left, cy - bandH * 0.04f, text);
        text.setColor(v.muted);
        text.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));
        fit(text, foot, room, bandH * 0.14f);
        c.drawText(foot, left, cy + bandH * 0.2f, text);
    }

    /** The stamp in the photograph's lower right: double frame, tilted, the ink not quite even. */
    static void rubberStamp(Canvas c, String word, int w, int h, Variant v) {
        Paint ink = new Paint(Paint.ANTI_ALIAS_FLAG);
        ink.setColor(v.light || v.name.equals("SPECTRE") ? 0xFFB0101A : v.accent);
        ink.setAlpha(215);
        ink.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        ink.setLetterSpacing(0.14f);
        float size = Math.min(w, h) * 0.085f;
        ink.setTextSize(size);
        float tw = ink.measureText(word);
        float pad = size * 0.35f;
        RectF frame = new RectF(-tw / 2 - pad, -size * 0.85f - pad / 2, tw / 2 + pad, size * 0.2f + pad / 2);
        c.save();
        c.translate(w - frame.width() / 2 - w * 0.05f, h - frame.height() - h * 0.04f);
        c.rotate(-9);
        Paint line = new Paint(ink);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(size * 0.09f);
        c.drawRoundRect(frame, size * 0.12f, size * 0.12f, line);
        frame.inset(-size * 0.16f, -size * 0.16f);
        line.setStrokeWidth(size * 0.035f);
        c.drawRoundRect(frame, size * 0.18f, size * 0.18f, line);
        ink.setTextAlign(Paint.Align.CENTER);
        c.drawText(word, 0, 0, ink);
        c.restore();
    }

    /** The uniform's emblem around a disc with the cross, as on the home screen. */
    static void seal(Canvas c, float cx, float cy, float r, Variant v) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        Path gear = new Path();
        Cyber.emblem(gear, cx, cy, r, v);
        p.setColor(v.ink);
        c.drawPath(gear, p);
        p.setColor(v.accent);
        c.drawCircle(cx, cy, r * 0.6f, p);
        p.setColor(v.bg);
        float arm = r * 0.24f, half = r * 0.095f;
        c.drawRect(cx - half, cy - arm, cx + half, cy + arm, p);
        c.drawRect(cx - arm, cy - half, cx + arm, cy + half, p);
    }

    /** Sets the text size to at most max, smaller if needed so text fits in width. */
    static void fit(Paint p, String text, float width, float max) {
        p.setTextSize(max);
        float measured = p.measureText(text);
        if (measured > width) p.setTextSize(max * width / measured);
    }
}
