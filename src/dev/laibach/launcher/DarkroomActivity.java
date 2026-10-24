package dev.laibach.launcher;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Insets;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Laibach Camera: the darkroom. A photograph comes in from the phone's own
 * camera app (asked for one picture, so this app needs no camera permission),
 * from the photo picker, or from any app's share menu ("Submit to the
 * censor"). It is developed in the manner of a uniform, eyes are blacked out
 * where the phone's face finder sees them, the owner adds bars and paints
 * people out by hand, and the print is framed with a stamp, the seal and a
 * decree. Prints are filed in Pictures/Laibach or shared; they are drawn from
 * pixels alone, so no location or other metadata leaves with them. The
 * photograph itself is kept only while it is on the table.
 */
public class DarkroomActivity extends StateActivity {
    private static final int CAPTURE = 1, PICK = 2;
    private static final int MAX_SIDE = 2048;
    private static final String CAPTURE_FILE = "capture.jpg";
    private static final String KEY_ISSUED = "darkroomIssued";

    /** The uniform the room is dressed in. */
    /** The uniform the print is developed in. */
    private Variant treatment;
    private SharedPreferences prefs;

    private Bitmap source, developed;
    private Censor.Marks marks = new Censor.Marks();
    private boolean painting, busy;
    private int generation;

    private Plate plate;
    private View empty, work;
    private TextView counter, hint, bars, paint, treatmentButton;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        treatment = v;
        prefs = getSharedPreferences(Variant.PREFS, MODE_PRIVATE);

        wear(buildUi());
        DarkroomProvider.sweep(this);
        show();
        receive(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        receive(intent);
    }

    // ---- Layout -------------------------------------------------------------

    private View buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(v.bg);
        LinearLayout chrome = new LinearLayout(this);
        chrome.setOrientation(LinearLayout.VERTICAL);
        root.addView(chrome);

        // Head: back, title, the decree in hand, rule.
        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = type(26, v.accent, 25, 1000);
        back.setText("◀");
        back.setPadding(0, 0, dp(12), 0);
        back.setOnClickListener(view -> finish());
        head.addView(back);
        TextView title = type(30, v.ink, 25, 1000);
        title.setText(t("DARKROOM", "DUNKELKAMMER"));
        title.setLetterSpacing(0.14f);
        title.setSingleLine();
        head.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        counter = type(15, v.accent, 25, 1000);
        counter.setLetterSpacing(0.08f);
        head.addView(counter);
        chrome.addView(head);
        View rule = new View(this);
        rule.setBackgroundColor(v.accent);
        LinearLayout.LayoutParams ruleLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(5));
        ruleLp.topMargin = dp(6);
        chrome.addView(rule, ruleLp);

        FrameLayout table = new FrameLayout(this);
        chrome.addView(table, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // Empty table: what the room does, and three ways to bring it a picture.
        LinearLayout none = new LinearLayout(this);
        none.setOrientation(LinearLayout.VERTICAL);
        none.setGravity(Gravity.CENTER);
        TextView noneTitle = type(34, v.ink, 25, 1000);
        noneTitle.setText(t("THE DARKROOM", "DIE DUNKELKAMMER"));
        noneTitle.setGravity(Gravity.CENTER);
        none.addView(noneTitle);
        TextView noneText = new TextView(this);
        noneText.setTypeface(Typeface.create("serif", Typeface.BOLD));
        noneText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        noneText.setTextColor(v.muted);
        noneText.setGravity(Gravity.CENTER);
        noneText.setLetterSpacing(0.04f);
        noneText.setText(t("EVERY PHOTOGRAPH IS DEVELOPED BY THE STATE.\nFACES ARE BLACKED OUT. COORDINATES ARE REMOVED.\nTHE ORIGINAL IS NOT KEPT.",
                "JEDES FOTO WIRD VOM STAAT ENTWICKELT.\nGESICHTER WERDEN GESCHWÄRZT. KOORDINATEN WERDEN ENTFERNT.\nDAS ORIGINAL WIRD NICHT AUFBEWAHRT."));
        noneText.setPadding(0, dp(12), 0, dp(24));
        none.addView(noneText);
        TextView take = button(t("■ TAKE A PHOTOGRAPH", "■ EIN FOTO AUFNEHMEN"), v.accent, view -> capture());
        take.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        take.setGravity(Gravity.CENTER);
        none.addView(take, full());
        TextView submit = button(t("SUBMIT A PICTURE", "EIN BILD EINREICHEN"), v.ink, view -> pick());
        submit.setGravity(Gravity.CENTER);
        none.addView(submit, full());
        TextView specimen = button(t("SPECIMEN (TEST PICTURE)", "MUSTER (TESTBILD)"), v.muted, view -> specimen());
        specimen.setGravity(Gravity.CENTER);
        none.addView(specimen, full());
        empty = none;
        table.addView(none);

        // The plate on the table, its tools and the foot.
        LinearLayout desk = new LinearLayout(this);
        desk.setOrientation(LinearLayout.VERTICAL);
        plate = new Plate(this, new Plate.Host() {
            @Override public Bitmap developed() { return developed; }
            @Override public Censor.Marks marks() { return marks; }
            @Override public boolean painting() { return painting; }
            @Override public boolean busy() { return busy; }
            @Override public void print(Canvas c) { Censor.draw(c, developed, marks, treatment, stamp(), decree(), foot()); }
        });
        LinearLayout.LayoutParams plateLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        plateLp.topMargin = dp(12);
        plateLp.bottomMargin = dp(8);
        desk.addView(plate, plateLp);
        hint = type(11, v.muted, 50, 700);
        hint.setLetterSpacing(0.06f);
        hint.setGravity(Gravity.CENTER);
        desk.addView(hint, full());
        LinearLayout tools = new LinearLayout(this);
        bars = button(t("■ BARS", "■ BALKEN"), v.accent, view -> mode(false));
        tools.addView(bars, weight());
        paint = button(t("PAINT OUT", "WEGRETUSCHIEREN"), v.muted, view -> mode(true));
        paint.setGravity(Gravity.CENTER_HORIZONTAL);
        tools.addView(paint, weight());
        TextView undo = button(t("UNDO", "ZURÜCK"), v.ink, view -> undo());
        undo.setGravity(Gravity.END);
        tools.addView(undo, weight());
        desk.addView(tools);
        treatmentButton = button("", v.ink, view -> nextTreatment());
        treatmentButton.setPadding(0, 0, 0, dp(12));
        desk.addView(treatmentButton, full());
        View footRule = new View(this);
        footRule.setBackgroundColor(v.accent);
        desk.addView(footRule, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(2)));
        LinearLayout foot = new LinearLayout(this);
        foot.addView(button(t("■ FILE", "■ ABLEGEN"), v.accent, view -> file()), weight());
        TextView share = button(t("SHARE", "TEILEN"), v.ink, view -> share());
        share.setGravity(Gravity.CENTER_HORIZONTAL);
        foot.addView(share, weight());
        TextView again = button(t("NEW", "NEU"), v.muted, view -> clear());
        again.setGravity(Gravity.END);
        foot.addView(again, weight());
        desk.addView(foot);
        work = desk;
        table.addView(desk);

        root.setOnApplyWindowInsetsListener((view, in) -> {
            Insets b = in.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            chrome.setPadding(b.left + dp(20), b.top + dp(6), b.right + dp(20), b.bottom + dp(4));
            return WindowInsets.CONSUMED;
        });
        return root;
    }

    private LinearLayout.LayoutParams weight() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    }

    private LinearLayout.LayoutParams full() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private TextView button(String text, int color, View.OnClickListener click) {
        TextView b = type(13, color, 50, 850);
        b.setText(text);
        b.setLetterSpacing(0.08f);
        b.setPadding(0, dp(14), 0, dp(14));
        b.setOnClickListener(click);
        return b;
    }

    /** Brings the head, the table and the tools in line with what is on the table. */
    private void show() {
        boolean onTable = source != null;
        empty.setVisibility(onTable ? View.INVISIBLE : View.VISIBLE);
        work.setVisibility(onTable ? View.VISIBLE : View.INVISIBLE);
        if (busy) {
            counter.setText(t("DEVELOPING …", "WIRD ENTWICKELT …"));
        } else {
            counter.setText(developed != null ? t("DECREE ", "ERLASS ") + number() : "");
        }
        bars.setTextColor(painting ? v.muted : v.accent);
        bars.setText(painting ? t("BARS", "BALKEN") : t("■ BARS", "■ BALKEN"));
        paint.setTextColor(painting ? v.accent : v.muted);
        paint.setText(painting ? t("■ PAINT OUT", "■ WEGRETUSCHIEREN") : t("PAINT OUT", "WEGRETUSCHIEREN"));
        hint.setText(painting
                ? t("PAINT OUT WHOEVER IS NO LONGER WITH US.", "RETUSCHIEREN SIE WEG, WER NICHT MEHR BEI UNS IST.")
                : t("TAP OR DRAG TO BLACK OUT. TAP A BAR TO LIFT IT.", "TIPPEN ODER ZIEHEN ZUM SCHWÄRZEN. BALKEN ANTIPPEN ZUM ENTFERNEN."));
        treatmentButton.setText(t("TREATMENT · ", "BEHANDLUNG · ") + Lang.pick(german, Censor.treatment(treatment))
                + " (" + treatment.name + ")  ▶");
        plate.invalidate();
    }

    // ---- Bringing a picture -------------------------------------------------

    /** Asks the phone's own camera app for one photograph, written into the darkroom's cache. */
    private void capture() {
        if (busy) return;
        File out = new File(DarkroomProvider.dir(this), CAPTURE_FILE);
        out.delete();
        Uri uri = DarkroomProvider.uri(CAPTURE_FILE);
        Intent take = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        take.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        take.setClipData(ClipData.newRawUri("", uri));
        take.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        try {
            startActivityForResult(take, CAPTURE);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, t("NO CAMERA ON THIS PHONE.", "KEINE KAMERA AUF DIESEM TELEFON."), Toast.LENGTH_SHORT).show();
        }
    }

    private void pick() {
        if (busy) return;
        Intent pick = new Intent(MediaStore.ACTION_PICK_IMAGES);
        pick.setType("image/*");
        try {
            startActivityForResult(pick, PICK);
        } catch (ActivityNotFoundException e) {
            Intent open = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            open.addCategory(Intent.CATEGORY_OPENABLE);
            open.setType("image/*");
            try {
                startActivityForResult(open, PICK);
            } catch (ActivityNotFoundException e2) {
                Toast.makeText(this, t("NO PICTURE PICKER ON THIS PHONE.", "KEINE BILDAUSWAHL AUF DIESEM TELEFON."), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == CAPTURE) {
            if (result == RESULT_OK) admit(DarkroomProvider.uri(CAPTURE_FILE), true);
            else new File(DarkroomProvider.dir(this), CAPTURE_FILE).delete();
        } else if (request == PICK && result == RESULT_OK && data != null && data.getData() != null) {
            admit(data.getData(), false);
        }
    }

    /** A picture shared from another app ("Submit to the censor"). */
    @SuppressWarnings("deprecation")
    private void receive(Intent intent) {
        if (intent == null || !Intent.ACTION_SEND.equals(intent.getAction())) return;
        Uri uri = null;
        ClipData clip = intent.getClipData();
        if (clip != null && clip.getItemCount() > 0) uri = clip.getItemAt(0).getUri();
        if (uri == null) uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        // Only pictures handed over by another app; never a path into this one.
        if (uri == null || !"content".equals(uri.getScheme()) || DarkroomProvider.AUTHORITY.equals(uri.getAuthority())) return;
        intent.setAction(Intent.ACTION_MAIN);
        admit(uri, false);
    }

    /** Reads a picture onto the table, upright and at most MAX_SIDE on its long side. */
    private void admit(Uri uri, boolean capture) {
        busy = true;
        show();
        new Thread(() -> {
            Bitmap picture = upright(uri);
            if (capture) new File(DarkroomProvider.dir(this), CAPTURE_FILE).delete();
            runOnUiThread(() -> {
                busy = false;
                if (picture == null) {
                    show();
                    Toast.makeText(this, t("THE PICTURE COULD NOT BE READ.", "DAS BILD LIESS SICH NICHT LESEN."), Toast.LENGTH_SHORT).show();
                } else {
                    start(picture, null);
                }
            });
        }, "darkroom-read").start();
    }

    private Bitmap upright(Uri uri) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(in, null, bounds);
            }
            if (bounds.outWidth <= 0) return null;
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = 1;
            while (Math.max(bounds.outWidth, bounds.outHeight) / (opts.inSampleSize * 2) >= MAX_SIDE) {
                opts.inSampleSize *= 2;
            }
            Bitmap raw;
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                raw = BitmapFactory.decodeStream(in, null, opts);
            }
            if (raw == null) return null;
            int rotation = 0;
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                int o = new ExifInterface(in).getAttributeInt(ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL);
                rotation = o == ExifInterface.ORIENTATION_ROTATE_90 ? 90
                        : o == ExifInterface.ORIENTATION_ROTATE_180 ? 180
                        : o == ExifInterface.ORIENTATION_ROTATE_270 ? 270 : 0;
            } catch (IOException | RuntimeException e) {
                // No usable EXIF: keep it as decoded.
            }
            float scale = Math.min(1f, (float) MAX_SIDE / Math.max(raw.getWidth(), raw.getHeight()));
            if (scale == 1f && rotation == 0) return raw.copy(Bitmap.Config.ARGB_8888, false);
            Matrix m = new Matrix();
            m.postScale(scale, scale);
            m.postRotate(rotation);
            Bitmap upright = Bitmap.createBitmap(raw, 0, 0, raw.getWidth(), raw.getHeight(), m, true);
            raw.recycle();
            return upright;
        } catch (IOException | RuntimeException | OutOfMemoryError e) {
            return null;
        }
    }

    /** Puts a picture on the table, finds the faces and develops it. */
    private void start(Bitmap picture, List<RectF> preset) {
        source = picture;
        developed = null;
        marks = new Censor.Marks();
        busy = true;
        int run = ++generation;
        Variant in = treatment;
        show();
        new Thread(() -> {
            List<RectF> found;
            try {
                found = Censor.eyes(picture);
            } catch (RuntimeException e) {
                found = new ArrayList<>();
            }
            if (found.isEmpty() && preset != null) found = preset;
            Bitmap done = Censor.develop(picture, in);
            List<RectF> faces = found;
            runOnUiThread(() -> {
                if (run != generation || isDestroyed()) return;
                marks.all.addAll(faces);
                developed = done;
                busy = false;
                show();
                Tones.play(this, Tones.shutter());
                Toast.makeText(this, faces.isEmpty()
                        ? t("NO FACES FOUND. BLACK OUT BY HAND.", "KEINE GESICHTER GEFUNDEN. VON HAND SCHWÄRZEN.")
                        : String.format(locale, t("%d FACE(S) FOUND. THE EYES ARE BLACKED OUT.",
                        "%d GESICHT(ER) GEFUNDEN. DIE AUGEN SIND GESCHWÄRZT."), faces.size()), Toast.LENGTH_SHORT).show();
            });
        }, "darkroom-develop").start();
    }

    private void nextTreatment() {
        if (busy || source == null) return;
        Variant[] all = Variant.ALL;
        treatment = all[(Arrays.asList(all).indexOf(treatment) + 1) % all.length];
        busy = true;
        int run = ++generation;
        Variant in = treatment;
        Bitmap picture = source;
        show();
        new Thread(() -> {
            Bitmap done = Censor.develop(picture, in);
            runOnUiThread(() -> {
                if (run != generation || isDestroyed()) return;
                developed = done;
                busy = false;
                show();
            });
        }, "darkroom-develop").start();
    }

    private void clear() {
        generation++;
        source = developed = null;
        marks = new Censor.Marks();
        busy = false;
        show();
    }

    private void mode(boolean paintOut) {
        painting = paintOut;
        show();
    }

    private void undo() {
        if (!marks.all.isEmpty()) marks.all.remove(marks.all.size() - 1);
        plate.invalidate();
    }

    // ---- The specimen -------------------------------------------------------

    /** The darkroom's own test picture, developed like any other. */
    private void specimen() {
        if (busy) return;
        List<RectF> eyes = new ArrayList<>();
        start(Specimen.draw(eyes), eyes);
    }

    // ---- The print ----------------------------------------------------------

    private String stamp() {
        return marks.empty() ? t("APPROVED", "GENEHMIGT") : t("CENSORED", "ZENSIERT");
    }

    /** The number this print is issued under: the day of the year and the count of prints issued. */
    private String number() {
        return Calendar.getInstance().get(Calendar.DAY_OF_YEAR) + "/" + (prefs.getInt(KEY_ISSUED, 0) + 1);
    }

    private String decree() {
        return t("DECREE ", "ERLASS ") + number() + " · " + Lang.pick(german, new String[]{
                Ministry.ENLIGHTENMENT.en, Ministry.ENLIGHTENMENT.de});
    }

    private String foot() {
        return t("DEPARTMENT OF PHOTOGRAPHY", "ABTEILUNG FÜR FOTOGRAFIE") + " · "
                + Lang.pick(german, Censor.treatment(treatment)) + " · "
                + new SimpleDateFormat("dd.MM.yyyy", locale).format(new Date()) + " · "
                + t("COORDINATES REMOVED", "KOORDINATEN ENTFERNT");
    }

    /** The print at full size: a picture of its own, without the photograph's metadata. */
    private Bitmap print(Bitmap developedNow, Censor.Marks marksNow, Variant v, String stamp, String decree, String foot) {
        int[] size = Censor.size(developedNow.getWidth(), developedNow.getHeight());
        Bitmap out = Bitmap.createBitmap(size[0], size[1], Bitmap.Config.ARGB_8888);
        Censor.draw(new Canvas(out), developedNow, marksNow, v, stamp, decree, foot);
        return out;
    }

    private interface Done {
        void with(Bitmap print);
    }

    /** Draws the print away from the screen's thread, then hands it over on it. */
    private void printThen(Done done) {
        if (busy || developed == null) return;
        busy = true;
        show();
        Bitmap d = developed;
        Censor.Marks m = copy(marks);
        Variant v = treatment;
        String stamp = stamp(), decree = decree(), foot = foot();
        new Thread(() -> {
            Bitmap print;
            try {
                print = print(d, m, v, stamp, decree, foot);
            } catch (OutOfMemoryError e) {
                print = null;
            }
            Bitmap result = print;
            runOnUiThread(() -> {
                busy = false;
                show();
                if (result == null) {
                    Toast.makeText(this, t("THE PRINT COULD NOT BE MADE.", "DER ABZUG LIESS SICH NICHT HERSTELLEN."), Toast.LENGTH_SHORT).show();
                } else {
                    done.with(result);
                }
            });
        }, "darkroom-print").start();
    }

    private static Censor.Marks copy(Censor.Marks from) {
        Censor.Marks to = new Censor.Marks();
        for (Object o : from.all) {
            if (o instanceof RectF) {
                to.all.add(new RectF((RectF) o));
            } else {
                Censor.Stroke s = (Censor.Stroke) o, c = new Censor.Stroke(s.width);
                for (PointF pt : s.points) c.points.add(new PointF(pt.x, pt.y));
                to.all.add(c);
            }
        }
        return to;
    }

    /** Files the print in Pictures/Laibach and notes it in the record. */
    private void file() {
        int barCount = marks.bars().size(), painted = marks.all.size() - barCount;
        printThen(print -> new Thread(() -> {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, "laibach-censored-"
                    + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(new Date()) + ".jpg");
            values.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/" + Exports.FOLDER);
            boolean ok = Exports.write(this, MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                    values, out -> print.compress(Bitmap.CompressFormat.JPEG, 90, out));
            print.recycle();
            runOnUiThread(() -> {
                if (!ok) {
                    Toast.makeText(this, t("THE PRINT COULD NOT BE FILED.", "DER ABZUG LIESS SICH NICHT ABLEGEN."), Toast.LENGTH_SHORT).show();
                    return;
                }
                issued();
                Record.note(this, String.format(Locale.ROOT, "A PHOTOGRAPH WAS DEVELOPED AND FILED. %d BAR(S), %d RETOUCH(ES).", barCount, painted),
                        String.format(Locale.ROOT, "EIN FOTO WURDE ENTWICKELT UND ABGELEGT. %d BALKEN, %d RETUSCHE(N).", barCount, painted));
                Tones.play(this, Tones.stamp());
                Stamps.award(this, "darkroom");
                Toast.makeText(this, t("FILED IN PICTURES/LAIBACH.", "ABGELEGT IN PICTURES/LAIBACH."), Toast.LENGTH_SHORT).show();
            });
        }, "darkroom-file").start());
    }

    /** Shares the print through the share menu, from a copy in the darkroom's cache. */
    private void share() {
        printThen(print -> new Thread(() -> {
            String name = "print-" + System.currentTimeMillis() + ".jpg";
            boolean ok;
            try (OutputStream out = new FileOutputStream(new File(DarkroomProvider.dir(this), name))) {
                ok = print.compress(Bitmap.CompressFormat.JPEG, 90, out);
            } catch (IOException e) {
                ok = false;
            }
            print.recycle();
            boolean written = ok;
            runOnUiThread(() -> {
                if (!written) {
                    Toast.makeText(this, t("THE PRINT COULD NOT BE MADE.", "DER ABZUG LIESS SICH NICHT HERSTELLEN."), Toast.LENGTH_SHORT).show();
                    return;
                }
                issued();
                Uri uri = DarkroomProvider.uri(name);
                Intent send = new Intent(Intent.ACTION_SEND);
                send.setType("image/jpeg");
                send.putExtra(Intent.EXTRA_STREAM, uri);
                send.setClipData(ClipData.newRawUri("", uri));
                send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                Intent chooser = Intent.createChooser(send, t("ISSUE THE PRINT", "DEN ABZUG HERAUSGEBEN"));
                chooser.putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, new ComponentName[]{new ComponentName(this, DarkroomActivity.class)});
                startActivity(chooser);
            });
        }, "darkroom-share").start());
    }

    private void issued() {
        prefs.edit().putInt(KEY_ISSUED, prefs.getInt(KEY_ISSUED, 0) + 1).apply();
        show();
    }
}
