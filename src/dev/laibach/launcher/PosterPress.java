package dev.laibach.launcher;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Random;

/**
 * The poster press: a slogan the owner types, set as a poster in the
 * uniform's colours in one of three manners, then saved to Pictures/Laibach
 * or offered to the share menu. The record notes that a poster was printed
 * and in which manner; the slogan itself is not written down.
 */
final class PosterPress {
    static final int CONSTRUCTIVIST = 0, WOODCUT = 1, COLD_WAR = 2;
    private static final String[][] MANNERS = {
            {"CONSTRUCTIVIST", "KONSTRUKTIVISTISCH"}, {"WOODCUT", "HOLZSCHNITT"}, {"COLD WAR", "KALTER KRIEG"}};
    /** The poster's full size, in the proportions of a printed sheet. */
    static final int WIDTH = 1080, HEIGHT = 1350;
    private static final int LONGEST = 60;

    private PosterPress() {}

    /** The manner the uniform suggests: woodcut for the old faiths, cold war for the machines. */
    static int manner(Variant v) {
        switch (v.motif) {
            case Variant.CATHEDRAL:
            case Variant.TEMPLE:
            case Variant.ALPINE:
                return WOODCUT;
            case Variant.LUNAR:
            case Variant.NETWORK:
            case Variant.TECHNO:
                return COLD_WAR;
            default:
                return CONSTRUCTIVIST;
        }
    }

    /** The press: the slogan, the manner and a proof of the poster, which follows every keystroke. */
    static void show(StateActivity a) {
        Variant v = a.v;
        String sample = a.t("THE FUTURE IS PLANNED", "DIE ZUKUNFT IST GEPLANT");
        int[] manner = {manner(v)};

        LinearLayout box = a.column();
        box.setPadding(a.dp(22), a.dp(6), a.dp(22), 0);
        ImageView proof = new ImageView(a);
        proof.setAdjustViewBounds(true);
        proof.setScaleType(ImageView.ScaleType.FIT_CENTER);
        box.addView(proof, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, a.dp(300)));
        EditText field = new EditText(a);
        field.setHint(sample);
        field.setTextColor(v.ink);
        field.setHintTextColor(v.muted);
        field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        field.setFilters(new InputFilter[]{new InputFilter.LengthFilter(LONGEST), new InputFilter.AllCaps()});
        field.setSingleLine(true);
        box.addView(field);
        LinearLayout manners = a.row();
        manners.setPadding(0, a.dp(8), 0, a.dp(4));
        TextView[] chips = new TextView[MANNERS.length];
        Runnable[] redraw = new Runnable[1];
        for (int i = 0; i < MANNERS.length; i++) {
            int m = i;
            TextView chip = a.type(10, v.accent, 60, 850);
            chip.setText(Lang.pick(a.german, MANNERS[i]));
            chip.setLetterSpacing(0.1f);
            chip.setGravity(Gravity.CENTER);
            chip.setMaxLines(1);
            // The longest manner, in the longest language, still fits its chip.
            chip.setAutoSizeTextTypeUniformWithConfiguration(6, 10, 1, TypedValue.COMPLEX_UNIT_SP);
            chip.setPadding(a.dp(4), a.dp(10), a.dp(4), a.dp(10));
            chip.setOnClickListener(x -> {
                manner[0] = m;
                redraw[0].run();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (i > 0) lp.leftMargin = a.dp(6);
            manners.addView(chip, lp);
            chips[i] = chip;
        }
        box.addView(manners);

        redraw[0] = () -> {
            for (int i = 0; i < chips.length; i++) {
                GradientDrawable frame = new GradientDrawable();
                frame.setStroke(Math.max(1, a.dp(1.5f)), v.accent);
                if (i == manner[0]) frame.setColor(v.accent);
                chips[i].setBackground(frame);
                chips[i].setTextColor(i == manner[0] ? v.bg : v.accent);
            }
            proof.setImageBitmap(render(a, slogan(field, sample), manner[0], WIDTH * 2 / 5));
        };
        field.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                redraw[0].run();
            }
        });
        redraw[0].run();

        StateMenu.dress(new AlertDialog.Builder(a, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(a.t("Poster press", "Plakatpresse"))
                .setView(box)
                .setPositiveButton(a.t("Save", "Speichern"), (d, w) -> print(a, slogan(field, sample), manner[0], false))
                .setNeutralButton(a.t("Share", "Teilen"), (d, w) -> print(a, slogan(field, sample), manner[0], true))
                .setNegativeButton(a.t("Cancel", "Abbrechen"), null)
                .show(), v);
    }

    private static String slogan(EditText field, String sample) {
        String s = field.getText().toString().trim();
        return s.isEmpty() ? sample : s;
    }

    /** Prints the poster at full size, to Pictures/Laibach or the share menu. */
    private static void print(StateActivity a, String slogan, int manner, boolean share) {
        Bitmap poster = render(a, slogan, manner, WIDTH);
        boolean ok;
        if (share) {
            Uri uri = Exports.lend(a, poster, "poster-");
            ok = uri != null;
            if (ok) Exports.send(a, uri, a.t("SHARE THE POSTER", "DAS PLAKAT TEILEN"));
        } else {
            ok = Exports.savePicture(a, poster, "laibach-poster-");
            if (ok) a.toast(a.t("Poster saved in Pictures/Laibach.", "Plakat in Pictures/Laibach gespeichert."));
        }
        poster.recycle();
        if (!ok) {
            a.toast(a.t("The press jammed. The poster was not printed.", "Die Presse klemmt. Das Plakat wurde nicht gedruckt."));
            return;
        }
        String[] name = MANNERS[manner];
        Record.note(a, "A POSTER WAS PRINTED, " + name[0] + ". ITS SLOGAN IS NOT RECORDED.",
                "EIN PLAKAT WURDE GEDRUCKT, " + name[1] + ". SEINE LOSUNG WIRD NICHT VERMERKT.");
        Tones.play(a, Tones.stamp());
        Stamps.award(a, "poster");
    }

    // ---- The three manners -----------------------------------------------------

    /** The poster at the given width, in the uniform's paper, ink and accent. */
    static Bitmap render(StateActivity a, String slogan, int manner, int width) {
        Variant v = a.v;
        int height = width * HEIGHT / WIDTH;
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        float k = width / (float) WIDTH;
        c.scale(k, k);
        String text = slogan.toUpperCase(a.locale);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        TextPaint type = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        Typeface condensed = Typeface.create("sans-serif-condensed", Typeface.BOLD);
        String year = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
        switch (manner) {
            case WOODCUT:
                woodcut(c, p, v, text.hashCode());
                type.setTypeface(Typeface.create("serif", Typeface.BOLD));
                type.setColor(v.ink);
                fit(c, text, type, new RectF(110, 860, WIDTH - 110, HEIGHT - 120), Layout.Alignment.ALIGN_CENTER, 0.5f, 0.02f);
                break;
            case COLD_WAR:
                coldWar(c, p, v);
                type.setTypeface(condensed);
                type.setColor(v.bg);
                fit(c, text, type, new RectF(90, 930, WIDTH - 90, HEIGHT - 120), Layout.Alignment.ALIGN_CENTER, 0.5f, 0.12f);
                caption(c, condensed, v.bg, a.t("■ NOTICE TO ALL CITIZENS", "■ MITTEILUNG AN ALLE BÜRGER") + " · " + year,
                        WIDTH / 2f, 92, Paint.Align.CENTER);
                break;
            default:
                constructivist(c, p, v);
                type.setTypeface(condensed);
                type.setColor(v.ink);
                fit(c, text, type, new RectF(80, 760, WIDTH - 80, HEIGHT - 150), Layout.Alignment.ALIGN_NORMAL, 1f, 0.02f);
                caption(c, condensed, v.accent, a.t("■ BY ORDER OF THE STATE", "■ AUF BEFEHL DES STAATES") + " · " + year,
                        80, HEIGHT - 70, Paint.Align.LEFT);
                break;
        }
        return bmp;
    }

    /** A red circle, a black diagonal and a star: the slogan below, set flush left. */
    private static void constructivist(Canvas c, Paint p, Variant v) {
        c.drawColor(v.bg);
        p.setColor(v.accent);
        c.drawCircle(WIDTH * 0.66f, 330, 300, p);
        c.save();
        c.rotate(-24, WIDTH / 2f, 470);
        p.setColor(v.ink);
        c.drawRect(-300, 420, WIDTH + 300, 530, p);
        p.setColor(v.accent);
        c.drawRect(-300, 560, WIDTH * 0.55f, 590, p);
        c.restore();
        Path star = new Path();
        Cyber.star(star, 210, 230, 130, 52, 5, 0);
        p.setColor(v.ink);
        c.drawPath(star, p);
        p.setColor(v.accent);
        c.drawRect(80, 710, 380, 726, p);
    }

    /** A sun over the horizon in cut rays, framed like a block print, with the gouge marks left in. */
    private static void woodcut(Canvas c, Paint p, Variant v, int seed) {
        c.drawColor(v.bg);
        RectF block = new RectF(70, 70, WIDTH - 70, 800);
        c.save();
        c.clipRect(block);
        p.setColor(v.ink);
        c.drawRect(block, p);
        float cx = WIDTH / 2f, cy = 640;
        p.setColor(v.bg);
        Path rays = new Path();
        for (int i = 0; i < 18; i++) {
            double a0 = Math.PI + i * Math.PI / 18, a1 = a0 + Math.PI / 36;
            rays.moveTo(cx, cy);
            rays.lineTo(cx + (float) Math.cos(a0) * 1400, cy + (float) Math.sin(a0) * 1400);
            rays.lineTo(cx + (float) Math.cos(a1) * 1400, cy + (float) Math.sin(a1) * 1400);
            rays.close();
        }
        c.drawPath(rays, p);
        c.drawCircle(cx, cy, 210, p);
        p.setColor(v.ink);
        Path emblem = new Path();
        Cyber.emblem(emblem, cx, cy - 40, 120, v);
        c.drawPath(emblem, p);
        // The ground, cut in short strokes, as the gouge leaves it.
        c.drawRect(70, cy, WIDTH - 70, 800, p);
        Random r = new Random(seed);
        p.setColor(v.bg);
        p.setStrokeCap(Paint.Cap.ROUND);
        for (int i = 0; i < 70; i++) {
            float y = cy + 24 + r.nextFloat() * 120, x = 90 + r.nextFloat() * (WIDTH - 200);
            p.setStrokeWidth(4 + r.nextFloat() * 6);
            c.drawLine(x, y, x + 30 + r.nextFloat() * 90, y + (r.nextFloat() - 0.5f) * 8, p);
        }
        c.restore();
        p.setStyle(Paint.Style.STROKE);
        p.setColor(v.ink);
        p.setStrokeWidth(18);
        c.drawRect(40, 40, WIDTH - 40, HEIGHT - 40, p);
        p.setStrokeWidth(4);
        c.drawRect(70, 70, WIDTH - 70, HEIGHT - 70, p);
        p.setStyle(Paint.Style.FILL);
        p.setColor(v.accent);
        c.drawRect(WIDTH / 2f - 160, 830, WIDTH / 2f + 160, 842, p);
    }

    /** The emblem in the sights of a radar, on the accent; the slogan on a band of ink below. */
    private static void coldWar(Canvas c, Paint p, Variant v) {
        c.drawColor(v.accent);
        float cx = WIDTH / 2f, cy = 500;
        p.setStyle(Paint.Style.STROKE);
        p.setColor(v.bg);
        p.setAlpha(110);
        p.setStrokeWidth(5);
        for (int i = 1; i <= 5; i++) c.drawCircle(cx, cy, 80f * i, p);
        c.drawLine(cx - 440, cy, cx + 440, cy, p);
        c.drawLine(cx, cy - 440, cx, cy + 440, p);
        p.setAlpha(255);
        p.setStyle(Paint.Style.FILL);
        Path emblem = new Path();
        Cyber.emblem(emblem, cx, cy, 190, v);
        c.drawPath(emblem, p);
        p.setColor(v.ink);
        c.drawRect(0, 860, WIDTH, HEIGHT, p);
        p.setColor(v.bg);
        c.drawRect(90, 890, WIDTH - 90, 896, p);
    }

    private static void caption(Canvas c, Typeface face, int color, String text, float x, float y, Paint.Align align) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setTypeface(face);
        p.setColor(color);
        p.setTextSize(30);
        p.setLetterSpacing(0.2f);
        p.setTextAlign(align);
        c.drawText(text, x, y, p);
    }

    /**
     * Sets the slogan as large as the box allows without breaking a word,
     * placed in the box at the given height (0 top, 1 bottom).
     */
    private static void fit(Canvas c, String text, TextPaint type, RectF box, Layout.Alignment align, float place,
                            float spacing) {
        type.setLetterSpacing(spacing);
        int width = (int) box.width();
        StaticLayout layout = null;
        for (float size = 220; size >= 28; size *= 0.94f) {
            type.setTextSize(size);
            boolean words = true;
            for (String w : text.split("\\s+")) {
                if (type.measureText(w) > width) {
                    words = false;
                    break;
                }
            }
            if (!words) continue;
            layout = StaticLayout.Builder.obtain(text, 0, text.length(), type, width)
                    .setAlignment(align).setLineSpacing(0, 0.92f).setIncludePad(false).build();
            if (layout.getHeight() <= box.height()) break;
        }
        if (layout == null) return;
        c.save();
        c.translate(box.left, box.top + (box.height() - layout.getHeight()) * place);
        layout.draw(c);
        c.restore();
    }
}
