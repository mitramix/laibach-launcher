package dev.laibach.launcher;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Papers that leave the launcher, each written to the phone's shared storage
 * under a Laibach folder and nowhere else: the citizen card as a picture
 * (overprinted as a specimen, so it is never mistaken for a document), the
 * case file as a PDF, and the settings as a file that can be read back.
 * Only the launcher's own files are written or read; no other file is opened.
 */
final class Exports {
    static final String FOLDER = "Laibach";
    private static final String SETTINGS_PREFIX = "laibach-settings-";
    /** The preference files a backup carries; caches and chosen pictures stay behind. */
    private static final String[] BACKED_UP = {Variant.PREFS, "citizen", "record", "ministries", "tv"};

    private Exports() {}

    private static String stamp() {
        return new SimpleDateFormat("yyyyMMdd-HHmm", Locale.ROOT).format(new Date());
    }

    // ---- The card ---------------------------------------------------------

    /** Draws the card as it is shown, overprints it as a specimen and saves it to Pictures/Laibach. */
    static boolean saveCard(Context c, View card, Variant v) {
        if (card.getWidth() == 0 || card.getHeight() == 0) return false;
        int margin = Math.round(24 * c.getResources().getDisplayMetrics().density);
        Bitmap bmp = Bitmap.createBitmap(card.getWidth() + 2 * margin, card.getHeight() + 2 * margin,
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        canvas.drawColor(v.bg);
        canvas.save();
        canvas.translate(margin, margin);
        card.draw(canvas);
        canvas.restore();

        // The overprint: across the card, and once more along the foot.
        Paint over = new Paint(Paint.ANTI_ALIAS_FLAG);
        over.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        over.setColor(v.accent);
        over.setAlpha(110);
        over.setTextAlign(Paint.Align.CENTER);
        over.setLetterSpacing(0.12f);
        String specimen = "SPECIMEN · NOT A VALID DOCUMENT";
        over.setTextSize(bmp.getWidth() / 14f);
        canvas.save();
        canvas.rotate(-18, bmp.getWidth() / 2f, bmp.getHeight() / 2f);
        canvas.drawText(specimen, bmp.getWidth() / 2f, bmp.getHeight() / 2f + over.getTextSize() / 3, over);
        canvas.restore();
        over.setAlpha(255);
        over.setTextSize(margin * 0.55f);
        canvas.drawText(specimen + " · A WORK OF FICTION · CONFERS NO RIGHTS", bmp.getWidth() / 2f,
                bmp.getHeight() - margin * 0.3f, over);

        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, "laibach-citizen-card-" + stamp() + ".png");
        values.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/" + FOLDER);
        boolean ok = write(c, MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values,
                out -> bmp.compress(Bitmap.CompressFormat.PNG, 100, out));
        bmp.recycle();
        return ok;
    }

    // ---- The case file ----------------------------------------------------

    /** The case file as a PDF in Download/Laibach: the papers, the weekly report and the whole record. */
    static boolean caseFile(Context c, boolean german) {
        Locale locale = Lang.locale(german);
        Pdf pdf = new Pdf();
        pdf.text(t(german, "THE LAIBACH COLLECTIVE · CASE FILE", "DAS LAIBACH-KOLLEKTIV · FALLAKTE"), 20, true, Pdf.RED);
        pdf.text(t(german, "CONFIDENTIAL. A WORK OF FICTION, PRINTED BY THE LAUNCHER FOR ITS OWNER.",
                "VERTRAULICH. EIN WERK DER FIKTION, VOM LAUNCHER FÜR SEINEN BESITZER GEDRUCKT."), 8, true, Pdf.GREY);
        pdf.gap(10);
        pdf.text(t(german, "CITIZEN № ", "BÜRGER NR. ") + Citizen.number(c), 14, true, Pdf.INK);
        pdf.text(t(german, "STANDING: ", "ANSEHEN: ") + Lang.pick(german, Citizen.STANDING[Citizen.grade(c)]), 10, true, Pdf.INK);
        pdf.text(t(german, "PRINTED ", "GEDRUCKT ")
                + new SimpleDateFormat(t(german, "d MMMM yyyy, HH:mm", "d. MMMM yyyy, HH:mm"), locale)
                .format(new Date()).toUpperCase(locale), 10, false, Pdf.INK);
        pdf.gap(14);
        pdf.heading(Report.title(german).toUpperCase(locale));
        for (String line : Report.lines(c, german)) pdf.text(line, 10, false, Pdf.INK);
        pdf.gap(14);
        List<String[]> entries = Record.entries(c);
        pdf.heading(t(german, "PERMANENT RECORD", "STÄNDIGE AKTE") + " (" + entries.size() + ")");
        SimpleDateFormat when = new SimpleDateFormat(t(german, "d MMM yyyy HH:mm", "d. MMM yyyy HH:mm"), locale);
        for (String[] e : entries) {
            String time;
            try {
                time = when.format(new Date(Long.parseLong(e[0]))).toUpperCase(locale);
            } catch (NumberFormatException ex) {
                time = "—";
            }
            pdf.text(time + "   " + (german ? e[2] : Lang.en(e[1])), 9, false, Pdf.INK);
        }
        pdf.gap(14);
        pdf.text(t(german, "THE FILE IS COMPLETE. THE FILE IS NEVER COMPLETE.",
                "DIE AKTE IST VOLLSTÄNDIG. DIE AKTE IST NIE VOLLSTÄNDIG."), 9, true, Pdf.RED);
        PdfDocument doc = pdf.finish();

        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, "laibach-case-file-" + stamp() + ".pdf");
        values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/" + FOLDER);
        boolean ok = write(c, MediaStore.Downloads.EXTERNAL_CONTENT_URI, values, out -> {
            try {
                doc.writeTo(out);
                return true;
            } catch (java.io.IOException e) {
                return false;
            }
        });
        doc.close();
        return ok;
    }

    /** A4 pages filled from the top, a new page when one is full. */
    private static final class Pdf {
        static final int INK = 0xFF111111, RED = 0xFFB3121B, GREY = 0xFF6B6B6B;
        private static final int W = 595, H = 842, MARGIN = 48;
        private final PdfDocument doc = new PdfDocument();
        private final TextPaint paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private PdfDocument.Page page;
        private float y;
        private int number;

        Pdf() {
            newPage();
        }

        private void newPage() {
            if (page != null) doc.finishPage(page);
            page = doc.startPage(new PdfDocument.PageInfo.Builder(W, H, ++number).create());
            y = MARGIN;
            Paint foot = new Paint(Paint.ANTI_ALIAS_FLAG);
            foot.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
            foot.setTextSize(7);
            foot.setColor(GREY);
            foot.setLetterSpacing(0.15f);
            page.getCanvas().drawText("LAIBACH LIFE · " + number, MARGIN, H - MARGIN / 2f, foot);
            Paint rule = new Paint();
            rule.setColor(RED);
            page.getCanvas().drawRect(MARGIN, MARGIN / 2f, W - MARGIN, MARGIN / 2f + 3, rule);
        }

        void text(String s, float size, boolean bold, int color) {
            paint.setTypeface(Typeface.create("sans-serif-condensed", bold ? Typeface.BOLD : Typeface.NORMAL));
            paint.setTextSize(size);
            paint.setColor(color);
            paint.setLetterSpacing(0.04f);
            StaticLayout layout = StaticLayout.Builder.obtain(s, 0, s.length(), paint, W - 2 * MARGIN)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL).setLineSpacing(0, 1.15f).build();
            if (y + layout.getHeight() > H - MARGIN) newPage();
            Canvas canvas = page.getCanvas();
            canvas.save();
            canvas.translate(MARGIN, y);
            layout.draw(canvas);
            canvas.restore();
            y += layout.getHeight() + 2;
        }

        void heading(String s) {
            if (y + 40 > H - MARGIN) newPage();
            Paint rule = new Paint();
            rule.setColor(RED);
            page.getCanvas().drawRect(MARGIN, y, W - MARGIN, y + 1.5f, rule);
            y += 6;
            text(s, 12, true, INK);
            y += 4;
        }

        void gap(float points) {
            y += points;
        }

        PdfDocument finish() {
            doc.finishPage(page);
            page = null;
            return doc;
        }
    }

    // ---- Settings ---------------------------------------------------------

    /** Writes the settings, the record and the lists to Download/Laibach as JSON. */
    static boolean backup(Context c) {
        JSONObject root = new JSONObject();
        try {
            root.put("app", c.getPackageName());
            root.put("version", 1);
            root.put("written", System.currentTimeMillis());
            JSONObject files = new JSONObject();
            for (String name : BACKED_UP) {
                JSONObject prefs = new JSONObject();
                for (Map.Entry<String, ?> e : c.getSharedPreferences(name, Context.MODE_PRIVATE).getAll().entrySet()) {
                    // The dressed lock screen belongs to this phone's wallpaper; it is not carried over.
                    if (e.getKey().startsWith("lock_wallpaper")) continue;
                    Object value = e.getValue();
                    JSONObject typed = new JSONObject();
                    if (value instanceof Boolean) typed.put("b", value);
                    else if (value instanceof Integer) typed.put("i", value);
                    else if (value instanceof Long) typed.put("l", value);
                    else if (value instanceof Float) typed.put("f", ((Float) value).doubleValue());
                    else if (value instanceof String) typed.put("s", value);
                    else if (value instanceof Set) typed.put("set", new JSONArray((Set<?>) value));
                    else continue;
                    prefs.put(e.getKey(), typed);
                }
                files.put(name, prefs);
            }
            root.put("prefs", files);
        } catch (org.json.JSONException e) {
            return false;
        }
        byte[] bytes = root.toString().getBytes(StandardCharsets.UTF_8);
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, SETTINGS_PREFIX + stamp() + ".json");
        values.put(MediaStore.MediaColumns.MIME_TYPE, "application/json");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/" + FOLDER);
        return write(c, MediaStore.Downloads.EXTERNAL_CONTENT_URI, values, out -> {
            try {
                out.write(bytes);
                return true;
            } catch (java.io.IOException e) {
                return false;
            }
        });
    }

    /** The name of the newest backup this launcher wrote, or null. */
    static String latestBackup(Context c) {
        Uri uri = newestBackup(c);
        if (uri == null) return null;
        try (Cursor cur = c.getContentResolver().query(uri,
                new String[]{MediaStore.MediaColumns.DISPLAY_NAME}, null, null, null)) {
            return cur != null && cur.moveToFirst() ? cur.getString(0) : null;
        }
    }

    /**
     * Reads back the newest backup this launcher wrote (only its own files are
     * visible to it) and replaces the settings with it. Returns false if there
     * is none or it cannot be read.
     */
    static boolean restore(Context c) {
        Uri uri = newestBackup(c);
        if (uri == null) return false;
        JSONObject files;
        try (InputStream in = c.getContentResolver().openInputStream(uri)) {
            if (in == null) return false;
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] chunk = new byte[16 * 1024];
            for (int n; (n = in.read(chunk)) > 0; ) {
                buf.write(chunk, 0, n);
                if (buf.size() > 2_000_000) return false;
            }
            JSONObject root = new JSONObject(buf.toString(StandardCharsets.UTF_8.name()));
            if (!c.getPackageName().equals(root.optString("app"))) return false;
            files = root.getJSONObject("prefs");
        } catch (Exception e) {
            return false;
        }
        for (String name : BACKED_UP) {
            JSONObject prefs = files.optJSONObject(name);
            if (prefs == null) continue;
            SharedPreferences p = c.getSharedPreferences(name, Context.MODE_PRIVATE);
            SharedPreferences.Editor e = p.edit();
            for (String key : p.getAll().keySet()) if (!key.startsWith("lock_wallpaper")) e.remove(key);
            for (java.util.Iterator<String> keys = prefs.keys(); keys.hasNext(); ) {
                String key = keys.next();
                if (key.startsWith("lock_wallpaper")) continue;
                JSONObject typed = prefs.optJSONObject(key);
                if (typed == null) continue;
                if (typed.has("b")) e.putBoolean(key, typed.optBoolean("b"));
                else if (typed.has("i")) e.putInt(key, typed.optInt("i"));
                else if (typed.has("l")) e.putLong(key, typed.optLong("l"));
                else if (typed.has("f")) e.putFloat(key, (float) typed.optDouble("f"));
                else if (typed.has("s")) e.putString(key, typed.optString("s"));
                else if (typed.has("set")) {
                    JSONArray a = typed.optJSONArray("set");
                    Set<String> set = new HashSet<>();
                    for (int i = 0; a != null && i < a.length(); i++) set.add(a.optString(i));
                    e.putStringSet(key, set);
                }
            }
            e.commit();
        }
        Lang.reset();
        return true;
    }

    private static Uri newestBackup(Context c) {
        Uri collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
        try (Cursor cur = c.getContentResolver().query(collection, new String[]{MediaStore.MediaColumns._ID},
                MediaStore.MediaColumns.DISPLAY_NAME + " LIKE ? AND " + MediaStore.MediaColumns.OWNER_PACKAGE_NAME + " = ?",
                new String[]{SETTINGS_PREFIX + "%", c.getPackageName()},
                MediaStore.MediaColumns.DATE_ADDED + " DESC")) {
            if (cur == null || !cur.moveToFirst()) return null;
            return android.content.ContentUris.withAppendedId(collection, cur.getLong(0));
        } catch (RuntimeException e) {
            return null;
        }
    }

    // ---- The state's sounds ----------------------------------------------

    /** The uniforms' names as they read on a sound's title. */
    private static String titled(String uniform) {
        StringBuilder out = new StringBuilder();
        for (String word : uniform.toLowerCase(Locale.ROOT).split(" ")) {
            if (out.length() > 0) out.append(' ');
            out.append(word.equals("of") ? word : word.equals("wat") ? "WAT"
                    : Character.toUpperCase(word.charAt(0)) + word.substring(1));
        }
        return out.toString();
    }

    /**
     * Writes the state's sounds as WAV files to Ringtones/Laibach,
     * Notifications/Laibach and Alarms/Laibach, where the phone's sound
     * settings offer them. Earlier copies written by the launcher are replaced.
     * The owner chooses them there; the launcher never sets one itself.
     * Returns how many were written.
     */
    static int sounds(Context c) {
        Map<String, short[]> ringtones = new java.util.LinkedHashMap<>();
        Map<String, short[]> notifications = new java.util.LinkedHashMap<>();
        Map<String, short[]> alarms = new java.util.LinkedHashMap<>();
        ringtones.put("Bell", Tones.repeat(Tones.bell(2.6), 3, 0.4));
        ringtones.put("Anthem", Tones.repeat(Tones.anthem(), 2, 0.8));
        notifications.put("Decree", Tones.decree());
        notifications.put("Stamp", Tones.stamp());
        notifications.put("Charging", Tones.charging());
        for (Variant v : Variant.ALL) {
            short[] call = Tones.signature(v.name);
            ringtones.put(titled(v.name), Tones.repeat(call, 3, 0.7));
            notifications.put(titled(v.name), call);
        }
        alarms.put("Reveille Bell", Tones.repeat(Tones.bell(2.6), 4, 0.2));
        alarms.put("Reveille Anthem", Tones.repeat(Tones.anthem(), 3, 0.6));

        int written = 0;
        written += writeSounds(c, ringtones, Environment.DIRECTORY_RINGTONES, MediaStore.Audio.Media.IS_RINGTONE);
        written += writeSounds(c, notifications, Environment.DIRECTORY_NOTIFICATIONS, MediaStore.Audio.Media.IS_NOTIFICATION);
        written += writeSounds(c, alarms, Environment.DIRECTORY_ALARMS, MediaStore.Audio.Media.IS_ALARM);
        return written;
    }

    private static int writeSounds(Context c, Map<String, short[]> sounds, String folder, String kind) {
        Uri audio = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        String path = folder + "/" + FOLDER + "/";
        int written = 0;
        for (Map.Entry<String, short[]> e : sounds.entrySet()) {
            String name = "Laibach " + e.getKey() + ".wav";
            // Only the launcher's own earlier copy can be seen here, and only that one is removed.
            try {
                c.getContentResolver().delete(audio, MediaStore.MediaColumns.RELATIVE_PATH + "=? AND "
                        + MediaStore.MediaColumns.DISPLAY_NAME + "=?", new String[]{path, name});
            } catch (RuntimeException ignored) {
                // Nothing to replace.
            }
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
            values.put(MediaStore.MediaColumns.TITLE, "Laibach · " + e.getKey());
            values.put(MediaStore.MediaColumns.MIME_TYPE, "audio/x-wav");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, path);
            values.put(kind, 1);
            byte[] wav = Tones.wav(e.getValue());
            if (write(c, audio, values, out -> {
                try {
                    out.write(wav);
                    return true;
                } catch (java.io.IOException x) {
                    return false;
                }
            })) written++;
        }
        return written;
    }

    // ---- Writing ----------------------------------------------------------

    interface Body {
        boolean into(OutputStream out);
    }

    /** Inserts a pending entry, writes it, and publishes it; on failure removes it again. */
    static boolean write(Context c, Uri collection, ContentValues values, Body body) {
        ContentResolver r = c.getContentResolver();
        values.put(MediaStore.MediaColumns.IS_PENDING, 1);
        Uri uri;
        try {
            uri = r.insert(collection, values);
        } catch (RuntimeException e) {
            return false;
        }
        if (uri == null) return false;
        boolean ok;
        try (OutputStream out = r.openOutputStream(uri)) {
            ok = out != null && body.into(out);
        } catch (Exception e) {
            ok = false;
        }
        if (!ok) {
            r.delete(uri, null, null);
            return false;
        }
        ContentValues done = new ContentValues();
        done.put(MediaStore.MediaColumns.IS_PENDING, 0);
        r.update(uri, done, null, null);
        return true;
    }

    private static String t(boolean german, String en, String de) {
        return german ? de : Lang.en(en);
    }
}
