package dev.laibach.launcher;

import android.app.Activity;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Papers that leave the launcher, each written to the phone's shared storage
 * under a Laibach folder and nowhere else: the citizen card and posters as
 * pictures (overprinted as a specimen, so they are never mistaken for
 * documents), and the writing every export shares. The case file, the
 * settings backup and the state's sounds are written by CaseFile, Backup and
 * Anthems through it. Only the launcher's own files are written or read; no
 * other file is opened.
 */
final class Exports {
    static final String FOLDER = "Laibach";

    private Exports() {}

    static String stamp() {
        return new SimpleDateFormat("yyyyMMdd-HHmm", Locale.ROOT).format(new Date());
    }

    // ---- The card ---------------------------------------------------------

    /** Draws the card as it is shown, overprints it as a specimen and saves it to Pictures/Laibach. */
    static boolean saveCard(Context c, View card, Variant v) {
        Bitmap bmp = specimen(c, card, v);
        if (bmp == null) return false;
        boolean ok = savePicture(c, bmp, "laibach-citizen-card-");
        bmp.recycle();
        return ok;
    }

    /** Saves a picture to Pictures/Laibach as a PNG, its name the prefix and the minute. */
    static boolean savePicture(Context c, Bitmap bmp, String prefix) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, prefix + stamp() + ".png");
        values.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/" + FOLDER);
        return write(c, MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values,
                out -> bmp.compress(Bitmap.CompressFormat.PNG, 100, out));
    }

    /**
     * The specimen as a file in the darkroom's cache, for the share menu: the
     * uri to lend, or null when it could not be drawn. Copies older than a
     * day are swept away.
     */
    static Uri shareCard(Context c, View card, Variant v) {
        Bitmap bmp = specimen(c, card, v);
        if (bmp == null) return null;
        Uri uri = lend(c, bmp, "card-");
        bmp.recycle();
        return uri;
    }

    /** A picture as a JPEG in the darkroom's cache, for the share menu: the uri to lend, or null. */
    static Uri lend(Context c, Bitmap bmp, String prefix) {
        DarkroomProvider.sweep(c);
        String name = prefix + System.currentTimeMillis() + ".jpg";
        boolean ok;
        try (OutputStream out = new FileOutputStream(new File(DarkroomProvider.dir(c), name))) {
            ok = bmp.compress(Bitmap.CompressFormat.JPEG, 92, out);
        } catch (IOException e) {
            ok = false;
        }
        return ok ? DarkroomProvider.uri(name) : null;
    }

    /** Offers a lent picture to the share menu, past the state's own desks, which would only take it in again. */
    static void send(Activity a, Uri uri, String title) {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("image/jpeg");
        send.putExtra(Intent.EXTRA_STREAM, uri);
        send.setClipData(ClipData.newRawUri("", uri));
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        Intent chooser = Intent.createChooser(send, title);
        chooser.putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, new ComponentName[]{
                new ComponentName(a, CensorActivity.class), new ComponentName(a, DarkroomActivity.class)});
        a.startActivity(chooser);
    }

    /** The card drawn as it is shown, with SPECIMEN printed across it and along its foot. */
    private static Bitmap specimen(Context c, View card, Variant v) {
        if (card.getWidth() == 0 || card.getHeight() == 0) return null;
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
        return bmp;
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
}
