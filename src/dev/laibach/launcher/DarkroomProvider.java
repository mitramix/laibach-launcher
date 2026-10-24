package dev.laibach.launcher;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Hands single files of the darkroom's cache to other apps, one grant at a
 * time: the phone's camera app writes the photograph it was asked for into
 * capture.jpg, a shared print is read from print-….jpg and a censored
 * memo from memo-….jpg. Nothing else in the app is reachable through it,
 * and it is not exported.
 */
public final class DarkroomProvider extends ContentProvider {
    static final String AUTHORITY = "dev.laibach.launcher.darkroom";

    static Uri uri(String name) {
        return new Uri.Builder().scheme("content").authority(AUTHORITY).appendPath(name).build();
    }

    /** The darkroom's folder in the app's cache. */
    static File dir(Context c) {
        File d = new File(c.getCacheDir(), "darkroom");
        d.mkdirs();
        return d;
    }

    /** Clears prints shared more than a day ago, and any photograph left behind by the camera. */
    static void sweep(Context c) {
        long dayAgo = System.currentTimeMillis() - 24 * 60 * 60_000L;
        File[] files = dir(c).listFiles();
        if (files == null) return;
        for (File f : files) if (f.lastModified() < dayAgo) f.delete();
    }

    private File file(Uri uri) throws FileNotFoundException {
        String name = uri.getLastPathSegment();
        if (uri.getPathSegments().size() != 1 || name == null || !name.matches("[a-z0-9-]+\\.jpg")) {
            throw new FileNotFoundException(uri.toString());
        }
        return new File(dir(getContext()), name);
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        return ParcelFileDescriptor.open(file(uri), ParcelFileDescriptor.parseMode(mode));
    }

    @Override
    public String getType(Uri uri) {
        return "image/jpeg";
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] args, String order) {
        File f;
        try {
            f = file(uri);
        } catch (FileNotFoundException e) {
            return null;
        }
        String[] columns = projection != null ? projection : new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE};
        MatrixCursor cursor = new MatrixCursor(columns, 1);
        Object[] row = new Object[columns.length];
        for (int i = 0; i < columns.length; i++) {
            if (OpenableColumns.DISPLAY_NAME.equals(columns[i])) row[i] = f.getName();
            else if (OpenableColumns.SIZE.equals(columns[i])) row[i] = f.length();
        }
        cursor.addRow(row);
        return cursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] args) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] args) {
        return 0;
    }
}
