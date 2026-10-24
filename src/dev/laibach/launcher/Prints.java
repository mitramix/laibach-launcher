package dev.laibach.launcher;

import android.content.ClipData;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * The darkroom's paperwork: a picture read onto the table upright, the print
 * drawn at full size, filed in Pictures/Laibach or left in the darkroom's
 * cache for the share menu. Prints are drawn from pixels alone, so no
 * location or other metadata leaves with them. Called away from the screen's
 * thread.
 */
final class Prints {
    private Prints() {}

    /** A picture at most maxSide on its long side, turned the way its EXIF says; null if it cannot be read. */
    static Bitmap upright(Context c, Uri uri, int maxSide) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream in = c.getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(in, null, bounds);
            }
            if (bounds.outWidth <= 0) return null;
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = 1;
            while (Math.max(bounds.outWidth, bounds.outHeight) / (opts.inSampleSize * 2) >= maxSide) {
                opts.inSampleSize *= 2;
            }
            Bitmap raw;
            try (InputStream in = c.getContentResolver().openInputStream(uri)) {
                raw = BitmapFactory.decodeStream(in, null, opts);
            }
            if (raw == null) return null;
            int rotation = 0;
            try (InputStream in = c.getContentResolver().openInputStream(uri)) {
                int o = new ExifInterface(in).getAttributeInt(ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL);
                rotation = o == ExifInterface.ORIENTATION_ROTATE_90 ? 90
                        : o == ExifInterface.ORIENTATION_ROTATE_180 ? 180
                        : o == ExifInterface.ORIENTATION_ROTATE_270 ? 270 : 0;
            } catch (IOException | RuntimeException e) {
                // No usable EXIF: keep it as decoded.
            }
            float scale = Math.min(1f, (float) maxSide / Math.max(raw.getWidth(), raw.getHeight()));
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

    /** The print at full size: a picture of its own, without the photograph's metadata. */
    static Bitmap print(Bitmap developedNow, Censor.Marks marksNow, Variant v, String stamp, String decree, String foot) {
        int[] size = Censor.size(developedNow.getWidth(), developedNow.getHeight());
        Bitmap out = Bitmap.createBitmap(size[0], size[1], Bitmap.Config.ARGB_8888);
        Censor.draw(new Canvas(out), developedNow, marksNow, v, stamp, decree, foot);
        return out;
    }

    /** Files the print in Pictures/Laibach; recycles it either way. */
    static boolean file(Context c, Bitmap print) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, "laibach-censored-"
                + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(new Date()) + ".jpg");
        values.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/" + Exports.FOLDER);
        boolean ok = Exports.write(c, MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                values, out -> print.compress(Bitmap.CompressFormat.JPEG, 90, out));
        print.recycle();
        return ok;
    }

    /** Leaves the print in the darkroom's cache for sharing; its file name, or null. Recycles it either way. */
    static String cache(Context c, Bitmap print) {
        String name = "print-" + System.currentTimeMillis() + ".jpg";
        boolean ok;
        try (OutputStream out = new FileOutputStream(new File(DarkroomProvider.dir(c), name))) {
            ok = print.compress(Bitmap.CompressFormat.JPEG, 90, out);
        } catch (IOException e) {
            ok = false;
        }
        print.recycle();
        return ok ? name : null;
    }

    /** The share menu for a cached print, with the darkroom itself left off it. */
    static Intent issue(Context c, String name, String title) {
        Uri uri = DarkroomProvider.uri(name);
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("image/jpeg");
        send.putExtra(Intent.EXTRA_STREAM, uri);
        send.setClipData(ClipData.newRawUri("", uri));
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        Intent chooser = Intent.createChooser(send, title);
        chooser.putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, new ComponentName[]{new ComponentName(c, DarkroomActivity.class)});
        return chooser;
    }
}
